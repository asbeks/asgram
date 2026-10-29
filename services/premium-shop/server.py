"""Run behind a TLS reverse proxy; port is loopback-only by default.
No app-facing shop is enabled until credentials, funding and distribution policy
are configured. Provider callbacks never set payment/delivery state themselves.
"""
from decimal import Decimal
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import argparse,json,os,re,threading,time
from premium import Config,Orders,Unavailable,VerificationError,telegram_identity

RATE={};LOCK=threading.Lock()

def config():
    env=os.environ
    return Config(env.get('PUBLIC_ORIGIN',''),env.get('PREMIUM_BOT_TOKEN',''),env.get('ARIFPAY_API_KEY',''),env.get('ARIFPAY_BENEFICIARY_ACCOUNT',''),env.get('ARIFPAY_BENEFICIARY_BANK',''),Decimal(env.get('STAR_COST_SANTIM','0')),Decimal(env.get('GROSS_MARGIN','0.15')),Decimal(env.get('PAYMENT_FEE','0.035')),env.get('LIVE_SALES_ENABLED')=='true')

def public(order):
    return {key:order.get(key) for key in ('id','recipient','months','amount_birr','state','checkout_url','created','expires')}

def handler(orders):
    class Handler(BaseHTTPRequestHandler):
        server_version='ASGRAM'
        def log_message(self,*args):pass  # Avoid request bodies, phones, proofs and payment links in logs.
        def respond(self,code,value):
            data=json.dumps(value).encode();self.send_response(code)
            self.send_header('Content-Type','application/json');self.send_header('Content-Length',str(len(data)));self.send_header('Cache-Control','no-store');self.send_header('X-Content-Type-Options','nosniff');self.end_headers();self.wfile.write(data)
        def do_GET(self):
            if self.path=='/health':return self.respond(200,{'ok':True,'sales_enabled':orders.config.sales_enabled})
            return self.respond(404,{'error':'Not found'})
        def do_POST(self):
            now=int(time.time());ip=self.client_address[0]
            with LOCK:
                bucket=RATE.setdefault(ip,[]);bucket[:]=[at for at in bucket if now-at<60]
                if len(bucket)>=40:return self.respond(429,{'error':'Wait a moment and try again.'})
                bucket.append(now)
            try:
                length=int(self.headers.get('Content-Length','0'))
                if not 0<length<=16_384:raise ValueError('Invalid request.')
                if self.headers.get('Content-Type','').split(';')[0]!='application/json':raise ValueError('JSON request required.')
                body=json.loads(self.rfile.read(length))
                if not isinstance(body,dict):raise ValueError('Invalid request.')
                # Webhook body is untrusted; it merely requests authoritative reconciliation.
                match=re.fullmatch(r'/premium/notify/([a-f0-9]{32})',self.path)
                if match:
                    oid=match[1];orders.verify(oid);orders.fulfill(oid)
                    return self.respond(200,{'received':True})
                buyer=telegram_identity(body.get('telegram'),orders.config.bot_token)
                if self.path=='/premium/orders':
                    recipient=body.get('recipient_id',buyer)
                    order=orders.create(buyer,recipient,body.get('months'),body.get('phone',''),body.get('idempotency_key',''))
                    return self.respond(201,public(order))
                if self.path in ('/premium/order','/premium/sync'):
                    oid=body.get('id','');order=orders.get(oid,buyer)
                    if self.path.endswith('/sync'):
                        orders.verify(oid);orders.fulfill(oid);order=orders.get(oid,buyer)
                    return self.respond(200,public(order))
                self.respond(404,{'error':'Not found'})
            except Unavailable as e:self.respond(503,{'error':str(e)})
            except (ValueError,VerificationError) as e:self.respond(400,{'error':str(e)[:240]})
            except Exception:self.respond(503,{'error':'Confirmation is pending. Check your order before paying again.'})
    return Handler

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--db',required=True);parser.add_argument('--port',type=int,default=8294);args=parser.parse_args()
    service=Orders(args.db,config());service.recover_interrupted()
    ThreadingHTTPServer(('127.0.0.1',args.port),handler(service)).serve_forever()
