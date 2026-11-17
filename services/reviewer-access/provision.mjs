import { createHash, randomBytes } from 'node:crypto';
import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { execFileSync } from 'node:child_process';
import { createInterface } from 'node:readline/promises';
import QRCode from 'qrcode';
import { Api, TelegramClient } from 'teleproto';
import { StringSession } from 'teleproto/sessions';

const root = new URL('../../', import.meta.url);
const privateDir = new URL('.asgram/reviewer-access/', root);
const runtime = new URL('runtime.env', privateDir);
const reviewerCodeFile = new URL('review-code.txt', privateDir);

async function prompt(message, { hidden = false } = {}) {
  const input = createInterface({ input: process.stdin, terminal: false });
  if (hidden) execFileSync('stty', ['-echo'], { stdio: ['inherit', 'ignore', 'ignore'] });
  try {
    process.stdout.write(message);
    return (await input.question('')).trim();
  } finally {
    input.close();
    if (hidden) {
      execFileSync('stty', ['echo'], { stdio: ['inherit', 'ignore', 'ignore'] });
      process.stdout.write('\n');
    }
  }
}

async function provision() {
  if (!process.stdin.isTTY || !process.stdout.isTTY) {
    throw new Error('Open this setup in Terminal to scan the QR with the dedicated Telegram account.');
  }
  if (existsSync(runtime) || existsSync(reviewerCodeFile)) {
    throw new Error('A reviewer setup already exists. No files were replaced.');
  }
  const configPath = new URL('asgram.local.properties', root);
  const config = existsSync(configPath) ? readFileSync(configPath, 'utf8') : '';
  const apiId = /^ASGRAM_API_ID\s*=\s*([0-9]+)\s*$/m.exec(config)?.[1] || process.env.ASGRAM_API_ID;
  const apiHash = /^ASGRAM_API_HASH\s*=\s*([a-f\d]{32})\s*$/im.exec(config)?.[1] || process.env.ASGRAM_API_HASH;
  if (!/^[1-9]\d*$/.test(apiId || '') || !/^[a-f\d]{32}$/i.test(apiHash || '')) {
    throw new Error('Configure the registered ASGRAM Telegram application before provisioning.');
  }

  const expectedPhone = process.env.REVIEW_TEST_PHONE || await prompt('Enter the dedicated test account phone number: ');
  if (!/^\+?[1-9][0-9]{6,14}$/.test(expectedPhone)) throw new Error('Enter an international phone number.');
  const session = new StringSession('');
  const client = new TelegramClient(session, Number(apiId), apiHash, {
    connectionRetries: 2, requestRetries: 1, floodSleepThreshold: 0, deviceModel: 'ASGRAM review access',
  });
  client.setLogLevel('none');
  try {
    await client.connect();
    await client.signInUserWithQrCode({ apiId: Number(apiId), apiHash }, {
      abortSignal: AbortSignal.timeout(180_000),
      qrCode: async ({ token }) => {
        process.stdout.write('\x1b[2J\x1b[H');
        process.stdout.write('In the dedicated Telegram account, open Settings → Devices → Link Desktop Device and scan.\n');
        process.stdout.write(await QRCode.toString('tg://login?token=' + Buffer.from(token).toString('base64url'), {
          type: 'terminal', small: true,
        }));
      },
      password: () => prompt('Enter this test account’s Telegram password: ', { hidden: true }),
      onError: () => true,
    });

    const account = await client.getMe();
    if (account?.bot || account?.phone !== expectedPhone.replace(/^\+/, '')) {
      await client.invoke(new Api.auth.LogOut());
      throw new Error('The scanned account did not match the dedicated test number. Its setup session was revoked.');
    }

    const code = randomBytes(32).toString('base64url');
    const digest = createHash('sha256').update(code).digest('hex');
    mkdirSync(privateDir, { recursive: true, mode: 0o700 });
    writeFileSync(runtime, [
      'ASGRAM_API_ID=' + apiId,
      'ASGRAM_API_HASH=' + apiHash,
      'REVIEW_TELEGRAM_USER_ID=' + account.id.toString(),
      'REVIEW_TELEGRAM_SESSION=' + session.save(),
      'REVIEW_CODE_SHA256=' + digest,
    ].join('\n') + '\n', { mode: 0o600, flag: 'wx' });
    writeFileSync(reviewerCodeFile, code + '\n', { mode: 0o600, flag: 'wx' });
    process.stdout.write('Dedicated reviewer sign-in prepared in ' + fileURLToPath(privateDir) + '\n');
  } finally {
    await client.disconnect();
  }
}

try {
  await provision();
} catch (error) {
  const safeErrors = ['Open this setup', 'A reviewer setup', 'Configure the registered', 'Enter an international', 'The scanned account'];
  const message = typeof error?.message === 'string' ? error.message : '';
  process.stderr.write((safeErrors.some((prefix) => message.startsWith(prefix)) ? message : 'Reviewer setup could not finish. Reopen it and scan a fresh QR.') + '\n');
  process.exitCode = 1;
}
