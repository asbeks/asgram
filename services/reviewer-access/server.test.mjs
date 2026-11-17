import { test } from 'node:test';
import assert from 'node:assert/strict';
import { createHash, randomBytes } from 'node:crypto';
import { createServer } from 'node:http';
import { once } from 'node:events';
import { reviewHandler } from './server.mjs';

async function harness(t, approve, now) {
  const code = randomBytes(32).toString('base64url');
  const codeHash = createHash('sha256').update(code).digest('hex');
  const server = createServer(reviewHandler({ codeHash, approve, now }));
  server.listen(0, '127.0.0.1');
  await once(server, 'listening');
  t.after(() => new Promise((resolve) => server.close(resolve)));
  const endpoint = 'http://127.0.0.1:' + server.address().port + '/v1/asgram/reviewer/authorize';
  return (token, authorization = code, body) => fetch(endpoint, {
    method: 'POST',
    headers: { Authorization: 'Bearer ' + authorization, 'Content-Type': 'application/json' },
    body: body ?? JSON.stringify({ token }),
  });
}

test('only the private reviewer code can approve a Telegram token, once', async (t) => {
  const seen = [];
  const post = await harness(t, async (token) => seen.push(token));
  const token = randomBytes(32);
  const encoded = token.toString('base64url');
  assert.equal((await post(encoded, randomBytes(32).toString('base64url'))).status, 401);
  assert.equal((await post(encoded)).status, 200);
  assert.equal((await post(encoded)).status, 409);
  assert.deepEqual(seen, [token]);
});

test('malformed or oversized requests grant no access', async (t) => {
  let calls = 0;
  const post = await harness(t, async () => { calls++; });
  assert.equal((await post('!not-a-token!')).status, 400);
  assert.equal((await post(null, undefined, '{')).status, 400);
  assert.equal((await post('A'.repeat(2048))).status, 413);
  assert.equal(calls, 0);
});

test('provider failures leave the request unapproved and rate limited', async (t) => {
  let now = 1000;
  const post = await harness(t, async () => { throw new Error('provider failure'); }, () => now);
  const token = randomBytes(32).toString('base64url');
  assert.equal((await post(token)).status, 502);
  assert.equal((await post(token)).status, 409);
  for (let i = 1; i < 30; i++) assert.equal((await post(randomBytes(32).toString('base64url'))).status, 502);
  assert.equal((await post(randomBytes(32).toString('base64url'))).status, 429);
  now += 3_600_001;
  assert.equal((await post(token)).status, 502);
});
