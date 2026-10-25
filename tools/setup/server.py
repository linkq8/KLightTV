#!/usr/bin/env python3
"""Local setup planner: reads optional ADB device, searches cities only on request, exports preferences. Never mutates TV."""
import argparse,importlib.util,json,pathlib,secrets,subprocess,urllib.request,urllib.parse,re,webbrowser
from http.server import BaseHTTPRequestHandler,ThreadingHTTPServer
from profile import default_profile,validate,plan,allowed_text
ROOT=pathlib.Path(__file__).resolve().parent
TOKEN=secrets.token_urlsafe(24)
def inspect_device(adb,serial):
 if not serial:return {'connected':False,'note':'لم يتم فحص جهاز؛ إعداد ملف فقط'}
 def run(*args):
  p=subprocess.run([adb,'-s',serial,'shell',*args],capture_output=True,text=True,timeout=15)
  if p.returncode:raise ValueError('تعذر فحص الجهاز؛ تحقق من اتصال ADB والتصريح')
  return p.stdout.strip()
 props=run('getprop');values=dict(re.findall(r'^\[([^]]+)\]: \[(.*)\]$',props,re.M));memory=run('cat','/proc/meminfo');m=re.search(r'MemTotal:\s+(\d+)',memory)
 features=run('pm','list','features');heap=values.get('dalvik.vm.heapgrowthlimit','0m');h=re.fullmatch(r'(\d+)[mM]',heap)
 return {'connected':True,'model':values.get('ro.product.model','غير معروف'),'manufacturer':values.get('ro.product.manufacturer',''),'sdk':int(values.get('ro.build.version.sdk','0')),'ram_mib':int(m[1])//1024 if m else 0,'heap_mib':int(h[1]) if h else 0,'television':'android.software.leanback' in features or 'android.hardware.type.television' in features,'display':run('wm','size'),'abis':values.get('ro.product.cpu.abilist',''),'storage':run('df','-k','/data')}
class Handler(BaseHTTPRequestHandler):
 def log_message(self,*a):pass
 def send(self,data,status=200,kind='application/json; charset=utf-8'):
  body=json.dumps(data,ensure_ascii=False).encode() if kind.startswith('application/json') else data
  self.send_response(status);self.send_header('Content-Type',kind);self.send_header('Content-Length',str(len(body)));self.send_header('Cache-Control','no-store');self.send_header('X-Content-Type-Options','nosniff');self.end_headers();self.wfile.write(body)
 def trusted(self):
  return self.headers.get('Host')==f'127.0.0.1:{self.server.server_port}' and self.headers.get('X-Setup-Token')==TOKEN
 def do_GET(self):
  path=urllib.parse.urlsplit(self.path).path
  if path in ('/','/app.js','/style.css'):
   name={'/':'index.html','/app.js':'app.js','/style.css':'style.css'}[path];kind={'/':'text/html; charset=utf-8','/app.js':'text/javascript; charset=utf-8','/style.css':'text/css; charset=utf-8'}[path];return self.send((ROOT/name).read_bytes(),kind=kind)
  if not self.trusted():return self.send({'error':'جلسة غير صالحة'},403)
  try:
   if path=='/api/default':return self.send(default_profile())
   if path=='/api/device':return self.send(inspect_device(self.server.adb,self.server.serial))
   if path=='/api/apps':
    if not self.server.serial:return self.send({'apps':[],'note':'وصّل جهازًا وافتح المعالج مع --serial لقراءة تطبيقاته؛ لا قائمة حذف افتراضية.'})
    spec=importlib.util.spec_from_file_location('tv_manager',ROOT.parent/'tv-manager/server.py');module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
    return self.send(module.Manager(self.server.adb,self.server.serial).inventory())
   if path=='/api/cities':
    q=urllib.parse.parse_qs(urllib.parse.urlsplit(self.path).query);name=q.get('name',[''])[0].strip();country=q.get('country',[''])[0].upper()
    if not 2<=len(name)<=100 or (not re.fullmatch('[A-Z]{2}',country) or country=='IL'):raise ValueError('أدخل اسم مدينة ورمز دولة من حرفين')
    url='https://geocoding-api.open-meteo.com/v1/search?'+urllib.parse.urlencode({'name':name,'countryCode':country,'count':10,'language':'ar','format':'json'})
    with urllib.request.urlopen(url,timeout=10) as r:body=r.read(131073)
    if len(body)>131072:raise ValueError('استجابة غير متوقعة')
    data=json.loads(body);return self.send({'results':[item for item in data.get('results',[]) if item.get('country_code')==country and item.get('country_code')!='IL' and allowed_text(json.dumps(item,ensure_ascii=False))],'source':'Open-Meteo / GeoNames'})
   self.send({'error':'غير موجود'},404)
  except Exception as e:self.send({'error':str(e)},400)
 def do_POST(self):
  if not self.trusted():return self.send({'error':'جلسة غير صالحة'},403)
  try:
   if self.path!='/api/plan':return self.send({'error':'غير موجود'},404)
   n=int(self.headers.get('Content-Length','0'))
   if not 0<n<=32768:raise ValueError('حجم ملف غير صالح')
   p=validate(json.loads(self.rfile.read(n)));return self.send({'profile':p,'plan':plan(p,inspect_device(self.server.adb,self.server.serial))})
  except Exception as e:self.send({'error':str(e)},400)
def main():
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--adb',default='adb');p.add_argument('--serial');p.add_argument('--port',type=int,default=0);p.add_argument('--no-browser',action='store_true');a=p.parse_args();s=ThreadingHTTPServer(('127.0.0.1',a.port),Handler);s.adb=a.adb;s.serial=a.serial;url=f'http://127.0.0.1:{s.server_port}/#{TOKEN}';print(url,flush=True)
 if not a.no_browser:webbrowser.open(url)
 s.serve_forever()
if __name__=='__main__':main()
