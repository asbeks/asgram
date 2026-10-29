"""ASGRAM ETB-to-Premium order engine. No credentials or funds ship with the app.

Only a dedicated, funded bot may fulfill gifts. Payment redirects/webhooks are
hints: authenticated ArifPay verification decides payment. An ambiguous gift
response is held for human reconciliation, never blindly replayed.
"""
from dataclasses import dataclass
from contextlib import contextmanager
from decimal import Decimal, ROUND_CEILING
import hashlib
import hmac
import json
import secrets
import sqlite3
import time
import urllib.error
import urllib.parse
import urllib.request

PLANS = {3: 1000, 6: 1500, 12: 2500}  # Official Bot API giftPremiumSubscription

class Unavailable(Exception): pass
class VerificationError(Exception): pass
class UncertainDelivery(Exception): pass

@dataclass(frozen=True)
class Config:
    origin: str
    bot_token: str
    arifpay_key: str
    beneficiary_account: str
    beneficiary_bank: str
    star_cost_santim: Decimal
    margin: Decimal = Decimal('0.15')
    payment_fee: Decimal = Decimal('0.035')
    sales_enabled: bool = False

    def price(self, months):
        if months not in PLANS: raise ValueError('Choose 3, 6 or 12 months.')
        if self.star_cost_santim <= 0 or not 0 <= self.margin < Decimal('0.5') or not 0 <= self.payment_fee < Decimal('0.2'):
            raise Unavailable('Verified supplier cost and fee settings are required.')
        # Whole birr, including configured processor fee and gross margin.
        cost = PLANS[months] * self.star_cost_santim / 100
        return int((cost / (1-self.margin-self.payment_fee)).to_integral_value(rounding=ROUND_CEILING))


def telegram_identity(data, bot_token, now=None):
    """Verify Telegram Login Widget proof; never trust an Android-supplied user ID."""
    if not isinstance(data, dict) or len(data) > 12: raise VerificationError('Invalid Telegram proof.')
    signature = data.get('hash', '')
    try:
        at = int(data['auth_date']); uid = int(data['id'])
        if uid <= 0 or not 0 <= (int(now or time.time())-at) <= 300: raise ValueError()
        if any(not isinstance(v, (str, int)) for v in data.values()): raise ValueError()
        content = '\n'.join(f'{k}={v}' for k,v in sorted(data.items()) if k != 'hash')
        expected = hmac.new(hashlib.sha256(bot_token.encode()).digest(), content.encode(), hashlib.sha256).hexdigest()
        if not hmac.compare_digest(expected, signature): raise ValueError()
    except (KeyError, ValueError, TypeError): raise VerificationError('Telegram sign-in expired. Sign in again.') from None
    return uid


def record(payload, session):
    if isinstance(payload, list):
        rows = [r for r in payload if isinstance(r, dict) and (r.get('session') or r.get('sessionId')) == session]
        if len(rows) != 1: raise VerificationError('Ambiguous verification.')
        return rows[0]
    if not isinstance(payload, dict) or payload.get('error', False) is not False:
        raise VerificationError('Provider could not verify payment.')
    data = payload.get('data', payload)
    if not isinstance(data, dict): raise VerificationError('Invalid verification.')
    return data


def assert_paid(responses, order):
    """All three methods must agree; a wrong reference or amount fails closed."""
    if len(responses) != 3: raise VerificationError('Payment verification is incomplete.')
    money_verified = False
    transaction_verified = False
    for payload in responses:
        data = record(payload, order['session'])
        sid = data.get('sessionId') or data.get('uuid') or data.get('session')
        if sid != order['session']: raise VerificationError('Payment session does not match.')
        if data.get('nonce') and data['nonce'] != order['nonce']: raise VerificationError('Payment reference does not match.')
        tx = data.get('transaction') or data
        status = str(tx.get('transactionStatus') or tx.get('status') or '').upper()
        if status not in ('SUCCESS', 'PAID', 'COMPLETED', 'COMPLETE'): raise VerificationError('Payment is not confirmed.')
        tid = tx.get('transactionId') or tx.get('transaction_id')
        if tid:
            if tid != order['transaction_id']: raise VerificationError('Transaction does not match.')
            transaction_verified = True
        amount = data.get('amount', tx.get('amount'))
        currency = data.get('currency', tx.get('currency'))
        if amount is not None:
            if Decimal(str(amount)) != Decimal(order['amount_birr']): raise VerificationError('Payment amount does not match.')
            if currency != 'ETB': raise VerificationError('Payment currency does not match.')
            money_verified = True
    if not money_verified or not transaction_verified: raise VerificationError('Payment evidence is incomplete.')


class Providers:
    def __init__(self, config): self.config = config
    def request(self, base, path, body=None, headers=None):
        encoded = None if body is None else json.dumps(body).encode()
        req = urllib.request.Request(base + path, data=encoded, headers={'Content-Type':'application/json', **(headers or {})})
        # Do not log URLs: Telegram embeds the bot credential in its API prefix.
        try:
            with urllib.request.urlopen(req, timeout=15) as response:
                return json.loads(response.read(1_000_001))
        except urllib.error.HTTPError as error:
            try: return json.loads(error.read(64_000))
            except (ValueError, OSError): raise UncertainDelivery("Provider error response is unknown.") from None
        except (TimeoutError, urllib.error.URLError, json.JSONDecodeError):
            raise UncertainDelivery('Provider response could not be confirmed.') from None
    def bot(self, method, data=None):
        result = self.request('https://api.telegram.org', '/bot'+self.config.bot_token+'/'+method, data or {})
        if result.get('ok') is not True: raise VerificationError(str(result.get('description', 'Telegram rejected the request.'))[:240])
        return result.get('result')
    def stars(self): return int(self.bot('getMyStarBalance')['amount'])
    def recipient(self, uid):
        user = self.bot('getChat', {'chat_id':uid})
        if user.get('type') != 'private' or user.get('id') != uid: raise VerificationError('Recipient is not a Telegram user.')
        return {'id':uid, 'name':str(user.get('first_name','')), 'username':str(user.get('username',''))}
    def gift(self, order):
        return self.bot('giftPremiumSubscription', {'user_id':order['recipient'], 'month_count':order['months'], 'star_count':order['stars']})
    def arif(self, path, body=None):
        return self.request('https://gateway.arifpay.net', path, body, {'x-arifpay-key':self.config.arifpay_key})
    def checkout(self, order, phone):
        url = self.config.origin.rstrip('/') + '/premium/return'
        payload = {
            'phone':phone, 'nonce':order['nonce'],
            'cancelUrl':url, 'errorUrl':url, 'successUrl':url,
            'notifyUrl':self.config.origin.rstrip('/')+'/premium/notify/'+order['id'],
            'expireDate':time.strftime('%Y-%m-%dT%H:%M:%SZ',time.gmtime(order['expires'])),
            'items':[{'name':'Fulbek Telegram Premium gift', 'description':str(order['months'])+' months', 'quantity':1, 'price':order['amount_birr']}],
            'beneficiaries':[{'accountNumber':self.config.beneficiary_account,'bank':self.config.beneficiary_bank,'amount':order['amount_birr']}],
            'lang':'EN'
        }
        # Hosted checkout exposes all merchant-supported banks without invented direct flows.
        result = self.arif('/api/checkout/session',payload)
        data = result.get('data',{})
        if result.get('error',False) is not False: raise VerificationError('ArifPay could not prepare payment.')
        session = data.get('sessionId') or data.get('uuid')
        link = data.get('paymentUrl') or data.get('approvalUrl')
        parsed = urllib.parse.urlparse(link or '')
        if not session or parsed.scheme != 'https' or parsed.hostname not in ('checkout.arifpay.net','gateway.arifpay.net') or parsed.username:
            raise VerificationError('ArifPay checkout could not be verified.')
        return session,link
    def verify(self, order):
        q = urllib.parse.quote
        # First get the authoritative transaction reference; ignore any client-supplied ID.
        details = record(self.arif('/api/checkout/session/'+q(order['session'],safe='')),order['session'])
        tx = details.get('transaction') or {}
        tid = tx.get('transactionId'); method = tx.get('paymentType') or tx.get('paymentMethod') or details.get('paymentType')
        if not tid or not method: raise VerificationError('Waiting for the bank confirmation.')
        snapshot = dict(order); snapshot['transaction_id'] = tid
        replies = [
            self.arif('/api/verify/transaction?uuid='+q(order['session'],safe='')),
            self.arif('/api/checkout/getSessionByNonce/'+q(order['nonce'],safe='')),
            self.arif('/api/checkout/getSessionByTransactionId',{'transactionId':tid,'paymentType':method})
        ]
        assert_paid(replies,snapshot)
        return tid


class Orders:
    def __init__(self, path, config, providers=None):
        self.path=path; self.config=config; self.providers=providers or Providers(config)
        with self.db() as db:
            db.executescript('''PRAGMA journal_mode=WAL;
            CREATE TABLE IF NOT EXISTS orders(
                id TEXT PRIMARY KEY, buyer INTEGER NOT NULL, recipient INTEGER NOT NULL,
                months INTEGER NOT NULL, stars INTEGER NOT NULL, amount_birr INTEGER NOT NULL,
                nonce TEXT UNIQUE NOT NULL, session TEXT UNIQUE, transaction_id TEXT UNIQUE,
                state TEXT NOT NULL, created INTEGER NOT NULL, expires INTEGER NOT NULL,
                idem TEXT NOT NULL, checkout_url TEXT, issue TEXT,
                UNIQUE(buyer,idem));
            CREATE TABLE IF NOT EXISTS events(id INTEGER PRIMARY KEY, order_id TEXT, state TEXT, at INTEGER);
            ''')
    @contextmanager
    def db(self):
        db=sqlite3.connect(self.path,timeout=15);db.row_factory=sqlite3.Row
        try:
            yield db
            db.commit()
        except Exception:
            db.rollback()
            raise
        finally: db.close()
    def event(self,db,oid,state): db.execute('INSERT INTO events(order_id,state,at) VALUES(?,?,?)',(oid,state,int(time.time())))
    def get(self,oid,buyer=None):
        with self.db() as db: row=db.execute('SELECT * FROM orders WHERE id=?',(oid,)).fetchone()
        if not row or buyer is not None and row['buyer'] != buyer: raise VerificationError('Order not found.')
        return dict(row)
    def create(self,buyer,recipient,months,phone,idem,now=None):
        if not self.config.sales_enabled: raise Unavailable('Premium in birr is not available yet.')
        if not self.config.origin.startswith('https://') or not all((self.config.bot_token,self.config.arifpay_key,self.config.beneficiary_account,self.config.beneficiary_bank)):
            raise Unavailable('Shop is not configured.')
        if not isinstance(buyer,int) or buyer<=0 or not isinstance(recipient,int) or recipient<=0: raise ValueError('Invalid recipient.')
        if not isinstance(idem,str) or not 16<=len(idem)<=64 or not idem.isalnum(): raise ValueError('Invalid order key.')
        phone=phone.strip().replace('+','')
        if phone.startswith('0'):phone='251'+phone[1:]
        if len(phone)!=12 or not phone.isdigit() or not phone.startswith(('2519','2517')):raise ValueError('Enter your Ethiopian payment number.')
        at=int(now or time.time());cost=self.config.price(months);balance=self.providers.stars()
        self.providers.recipient(recipient)  # Confirms actual target via Telegram before payment.
        with self.db() as db:
            db.execute('BEGIN IMMEDIATE')
            existing=db.execute('SELECT id,recipient,months FROM orders WHERE buyer=? AND idem=?',(buyer,idem)).fetchone()
            if existing:
                if existing['recipient']!=recipient or existing['months']!=months:raise VerificationError('Order key has different details.')
                return self.get(existing['id'],buyer)
            db.execute("UPDATE orders SET state='expired' WHERE state='draft' AND expires<?",(at,))
            reserved=db.execute("SELECT COALESCE(SUM(stars),0) FROM orders WHERE state IN ('draft','creating','pending','paid','sending','needs_review','refund_required')").fetchone()[0]
            if balance-reserved<PLANS[months]:raise Unavailable('Premium gifts are temporarily sold out. No payment was started.')
            oid=secrets.token_hex(16);nonce='fulbek-prem-'+oid
            db.execute('INSERT INTO orders(id,buyer,recipient,months,stars,amount_birr,nonce,state,created,expires,idem) VALUES(?,?,?,?,?,?,?,?,?,?,?)',(oid,buyer,recipient,months,PLANS[months],cost,nonce,'creating',at,at+1800,idem))
            self.event(db,oid,'creating')
        order=self.get(oid)
        try:session,url=self.providers.checkout(order,phone)
        except Exception:
            # Session creation may have succeeded upstream. Hold funds/reference until reconciled.
            with self.db() as db:db.execute("UPDATE orders SET state='needs_review',issue='checkout_response_uncertain' WHERE id=?",(oid,));self.event(db,oid,'needs_review')
            raise
        with self.db() as db:
            db.execute("UPDATE orders SET state='pending',session=?,checkout_url=? WHERE id=?",(session,url,oid));self.event(db,oid,'pending')
        return self.get(oid,buyer)
    def verify(self,oid):
        order=self.get(oid)
        if order['state'] not in ('pending','paid'):return order
        tid=self.providers.verify(order)
        with self.db() as db:
            db.execute("UPDATE orders SET state='paid',transaction_id=? WHERE id=? AND state='pending'",(tid,oid));self.event(db,oid,'paid')
        return self.get(oid)
    def fulfill(self,oid):
        with self.db() as db:
            db.execute('BEGIN IMMEDIATE')
            row=db.execute("SELECT * FROM orders WHERE id=? AND state='paid'",(oid,)).fetchone()
            if not row:return self.get(oid)
            order=dict(row)
            if self.providers.stars()<order['stars']:raise Unavailable('Waiting for gift stock. Payment is preserved.')
            db.execute("UPDATE orders SET state='sending' WHERE id=?",(oid,));self.event(db,oid,'sending')
        try:
            success=self.providers.gift(order)
            if success is not True:raise UncertainDelivery('Delivery result is unknown.')
        except VerificationError:
            state='refund_required'
        except Exception:
            state='needs_review'
        else:state='delivered'
        with self.db() as db:db.execute('UPDATE orders SET state=? WHERE id=?',(state,oid));self.event(db,oid,state)
        return self.get(oid)
    def recover_interrupted(self):
        # A crashed worker must not resend an already debited Telegram gift.
        with self.db() as db:db.execute("UPDATE orders SET state='needs_review',issue='interrupted_provider_call' WHERE state IN ('creating','sending')")
