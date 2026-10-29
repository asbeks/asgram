import concurrent.futures
from decimal import Decimal
import hashlib,hmac,json,os,sqlite3,tempfile,time,unittest
from premium import Config,Orders,Unavailable,VerificationError,UncertainDelivery,assert_paid,telegram_identity

class Fake:
    balance=5000
    gifts=0
    requests=0
    fail=False
    uncertain=False
    paid=True
    def stars(self):return self.balance
    def recipient(self,uid):return {'id':uid,'name':'Verified user','username':'test_user'}
    def checkout(self,o,p):self.requests+=1;return 'session-'+o['id'],'https://checkout.arifpay.net/'+o['id']
    def verify(self,o):
        if not self.paid:raise VerificationError('Waiting')
        return 'tx-'+o['id']
    def gift(self,o):
        self.gifts+=1
        if self.uncertain:raise UncertainDelivery('Timeout after submission')
        if self.fail:raise VerificationError('Recipient cannot receive this gift')
        self.balance-=o['stars'];return True

class Tests(unittest.TestCase):
    def setUp(self):
        self.tmp=tempfile.TemporaryDirectory();self.fake=Fake()
        self.cfg=Config('https://shop.example','test-token','test-key','merchant','BANK',Decimal('200'),sales_enabled=True)
        self.orders=Orders(os.path.join(self.tmp.name,'orders.db'),self.cfg,self.fake)
    def tearDown(self):self.tmp.cleanup()
    def create(self,key='abcdefghijklmnop',recipient=23,months=3):return self.orders.create(11,recipient,months,'0912345678',key)
    def test_unfunded_never_starts_payment(self):
        self.fake.balance=999
        with self.assertRaises(Unavailable):self.create()
        self.assertEqual(self.fake.requests,0)
    def test_sales_disabled_by_default(self):
        cfg=Config('https://shop.example','t','k','m','b',Decimal('200'))
        orders=Orders(os.path.join(self.tmp.name,'disabled.db'),cfg,self.fake)
        with self.assertRaises(Unavailable):orders.create(11,23,3,'0912345678','abcdefghijklmnop')
    def test_quote_includes_cost_fee_and_margin(self):
        amount=self.cfg.price(3);net=Decimal(amount)*(1-self.cfg.payment_fee)
        self.assertGreaterEqual(net-Decimal(2000),Decimal(amount)*self.cfg.margin)
    def test_idempotent_checkout(self):
        first=self.create();second=self.create();self.assertEqual(first['id'],second['id']);self.assertEqual(self.fake.requests,1)
    def test_order_key_cannot_change_recipient(self):
        self.create()
        with self.assertRaises(VerificationError):self.create(recipient=99)
    def test_another_buyer_cannot_read_order(self):
        order=self.create()
        with self.assertRaises(VerificationError):self.orders.get(order['id'],99)
    def test_unverified_payment_never_delivers(self):
        o=self.create();self.fake.paid=False
        with self.assertRaises(VerificationError):self.orders.verify(o['id'])
        self.orders.fulfill(o['id']);self.assertEqual(self.fake.gifts,0)
    def test_verified_order_delivers_once_under_concurrent_workers(self):
        o=self.create();self.orders.verify(o['id'])
        with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:list(pool.map(self.orders.fulfill,[o['id']]*8))
        self.assertEqual(self.fake.gifts,1);self.assertEqual(self.orders.get(o['id'])['state'],'delivered')
    def test_unknown_delivery_must_not_be_replayed(self):
        o=self.create();self.orders.verify(o['id']);self.fake.uncertain=True
        self.assertEqual(self.orders.fulfill(o['id'])['state'],'needs_review');self.orders.fulfill(o['id']);self.assertEqual(self.fake.gifts,1)
    def test_definitive_delivery_failure_is_refund_required(self):
        o=self.create();self.orders.verify(o['id']);self.fake.fail=True
        self.assertEqual(self.orders.fulfill(o['id'])['state'],'refund_required')
    def test_stock_reservations_prevent_overselling(self):
        self.fake.balance=1500;self.create()
        with self.assertRaises(Unavailable):self.create(key='abcdefghijklmnop2')
        self.assertEqual(self.fake.requests,1)
    def test_crashed_send_stays_held(self):
        o=self.create();self.orders.verify(o['id'])
        with self.orders.db() as db:db.execute("UPDATE orders SET state='sending' WHERE id=?",(o['id'],))
        self.orders.recover_interrupted();self.orders.fulfill(o['id']);self.assertEqual(self.fake.gifts,0);self.assertEqual(self.orders.get(o['id'])['state'],'needs_review')
    def proof(self):
        data={'id':11,'first_name':'Test','auth_date':int(time.time())}
        content='\n'.join(f'{k}={v}' for k,v in sorted(data.items()))
        data['hash']=hmac.new(hashlib.sha256(b'test-token').digest(),content.encode(),hashlib.sha256).hexdigest();return data
    def test_signed_telegram_proof_required(self):
        data=self.proof();self.assertEqual(telegram_identity(data,'test-token'),11)
        data['id']=99
        with self.assertRaises(VerificationError):telegram_identity(data,'test-token')
    def test_expired_identity_is_rejected(self):
        with self.assertRaises(VerificationError):telegram_identity(self.proof(),'test-token',int(time.time())+600)
    def evidence(self):
        order={'session':'sid','nonce':'nonce','transaction_id':'tx','amount_birr':250}
        row={'session':'sid','nonce':'nonce','transactionId':'tx','transactionStatus':'SUCCESS','amount':250,'currency':'ETB'}
        return order,[[dict(row)],{'error':False,'data':dict(row)},{'data':dict(row)}]
    def test_three_verification_shapes_confirm(self):
        o,r=self.evidence();assert_paid(r,o)
    def test_wrong_amount_currency_reference_or_failed_status_cannot_deliver(self):
        for key,value in [('amount',1),('currency','USD'),('session','other'),('nonce','other'),('transactionId','other'),('transactionStatus','FAILED')]:
            with self.subTest(key=key):
                o,r=self.evidence();r[1]['data'][key]=value
                with self.assertRaises(VerificationError):assert_paid(r,o)
    def test_missing_and_ambiguous_evidence_are_not_success(self):
        o,r=self.evidence()
        with self.assertRaises(VerificationError):assert_paid(r[:2],o)
        r[0].append(r[0][0])
        with self.assertRaises(VerificationError):assert_paid(r,o)

if __name__=='__main__':unittest.main()
