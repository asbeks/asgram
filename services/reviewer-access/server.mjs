import { createHash, timingSafeEqual } from 'node:crypto';
import { createServer } from 'node:http';
import { pathToFileURL } from 'node:url';

const MAX_BODY_BYTES = 1024;
const WINDOW_MS = 3_600_000;
const TOKEN_TTL_MS = 90_000;
const MAX_APPROVALS = 30;
const REVIEW_CODE = /^[A-Za-z0-9_-]{32,64}$/;
const QR_TOKEN = /^[A-Za-z0-9_-]{16,512}$/;

function digest(value) {
  return createHash('sha256').update(value).digest();
}

export function reviewHandler({ codeHash, approve, now = Date.now }) {
  if (!/^[a-f\d]{64}$/i.test(codeHash)) throw new Error('Set a SHA-256 digest of the randomly generated reviewer code.');
  const expectedCode = Buffer.from(codeHash, 'hex');
  const attempts = new Map();
  let approvals = [];
  let inFlight = false;

  return async (req, res) => {
    const send = (status, body) => {
      res.writeHead(status, { 'Content-Type': 'application/json; charset=utf-8', 'Cache-Control': 'no-store' });
      res.end(JSON.stringify(body));
    };

    if (req.url !== '/v1/asgram/reviewer/authorize' || req.method !== 'POST') {
      return send(404, { error: 'unavailable' });
    }
    const code = /^Bearer ([A-Za-z0-9_-]+)$/.exec(req.headers.authorization || '')?.[1] || '';
    if (!REVIEW_CODE.test(code) || !timingSafeEqual(digest(code), expectedCode)) {
      return send(401, { error: 'invalid_code' });
    }
    if (!/^application\/json(?:;|$)/i.test(req.headers['content-type'] || '')) {
      return send(415, { error: 'invalid_request' });
    }
    if (Number(req.headers['content-length'] || 0) > MAX_BODY_BYTES) {
      return send(413, { error: 'invalid_request' });
    }

    let token;
    try {
      const chunks = [];
      let limit = MAX_BODY_BYTES;
      for await (const chunk of req) {
        limit -= chunk.length;
        if (limit < 0) return send(413, { error: 'invalid_request' });
        chunks.push(chunk);
      }
      const body = JSON.parse(Buffer.concat(chunks).toString('utf8'));
      if (!body || typeof body.token !== 'string' || !QR_TOKEN.test(body.token)) {
        return send(400, { error: 'invalid_token' });
      }
      token = Buffer.from(body.token, 'base64url');
      if (token.toString('base64url') !== body.token) return send(400, { error: 'invalid_token' });
    } catch {
      return send(400, { error: 'invalid_request' });
    }

    const time = now();
    const key = digest(token).toString('hex');
    for (const [hash, expiry] of attempts) if (expiry <= time) attempts.delete(hash);
    approvals = approvals.filter((at) => at > time - WINDOW_MS);
    if (attempts.has(key)) return send(409, { error: 'already_used' });
    if (inFlight || approvals.length >= MAX_APPROVALS) return send(429, { error: 'try_again_later' });

    // Mark before calling Telegram. An interrupted client must request a new token.
    attempts.set(key, time + TOKEN_TTL_MS);
    approvals.push(time);
    inFlight = true;
    try {
      await approve(token);
      return send(200, { ok: true });
    } catch {
      return send(502, { error: 'telegram_unavailable' });
    } finally {
      inFlight = false;
    }
  };
}

async function start() {
  const {
    ASGRAM_API_ID, ASGRAM_API_HASH, REVIEW_TELEGRAM_USER_ID,
    REVIEW_TELEGRAM_SESSION, REVIEW_CODE_SHA256, PORT = '8098',
  } = process.env;
  if (!/^\d+$/.test(ASGRAM_API_ID || '') || !/^[a-f\d]{32}$/i.test(ASGRAM_API_HASH || '')
      || !/^[1-9]\d{0,18}$/.test(REVIEW_TELEGRAM_USER_ID || '') || !REVIEW_TELEGRAM_SESSION) {
    throw new Error('Configure the dedicated reviewer account before starting.');
  }

  const { Api, TelegramClient } = await import('teleproto');
  const { StringSession } = await import('teleproto/sessions');
  const telegram = new TelegramClient(new StringSession(REVIEW_TELEGRAM_SESSION), Number(ASGRAM_API_ID), ASGRAM_API_HASH, {
    connectionRetries: 2, requestRetries: 1, floodSleepThreshold: 0, deviceModel: 'ASGRAM review access',
  });
  telegram.setLogLevel('none');
  await telegram.connect();
  const identity = await telegram.getMe();
  if (identity?.bot || identity?.id?.toString() !== REVIEW_TELEGRAM_USER_ID) {
    await telegram.disconnect();
    throw new Error('The Telegram session does not match the dedicated reviewer account.');
  }

  const server = createServer(reviewHandler({
    codeHash: REVIEW_CODE_SHA256 || '',
    approve: (token) => telegram.invoke(new Api.auth.AcceptLoginToken({ token })),
  }));
  server.requestTimeout = 15_000;
  server.headersTimeout = 8_000;
  server.listen(Number(PORT), process.env.BIND_HOST || '127.0.0.1', () => process.stdout.write('ASGRAM reviewer access ready\n'));
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  start().catch(() => {
    process.stderr.write('ASGRAM reviewer access could not start. Verify the dedicated account and private configuration.\n');
    process.exit(1);
  });
}
