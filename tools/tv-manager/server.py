#!/usr/bin/env python3
"""Local, on-demand ADB checklist. No shell interpolation; core system packages are protected."""
import argparse,json,pathlib,re,secrets,subprocess,threading,time,urllib.parse,webbrowser
from http.server import ThreadingHTTPServer,BaseHTTPRequestHandler
ROOT=pathlib.Path(__file__).resolve().parent
OPTIONAL={
 'com.google.android.play.games':('Play Games','ألعاب Google','optional'),
 'com.google.android.videos':('Play Movies / Google TV','متجر الأفلام','optional'),
 'com.android.printspooler':('خدمة الطباعة','الطباعة من Android','optional'),
 'com.google.android.feedback':('Google Feedback','إرسال ملاحظات وتشخيص اختياري','privacy'),
 'com.google.android.tv.bugreportsender':('TV Bug Report Sender','إرسال تقارير الأعطال','privacy'),
 'com.google.android.tvrecommendations':('توصيات Google TV','اقتراحات محتوى الرئيسية','privacy'),
 'com.smartdevice.acr.recommendation':('Skyworth ACR','التعرّف التلقائي على المحتوى والتوصيات','privacy'),
 'com.smartdevice.recommendation':('DailyPlay','توصيات الشركة','privacy'),
 'com.smartdevice.epopplayer':('EPOP','عروض المتجر والإعلانات','privacy'),
 'fusion.android.tv.demo':('Retail Display','وضع العرض في المتجر','privacy'),
 'com.miui.tv.analytics':('Xiaomi Analytics','تحليلات الشركة','privacy'),
 'com.smartdevice.dtv.icast':('CastPlay','بث مكرر؛ Chromecast مستقل','optional'),
 'com.netflix.ninja':('Netflix','تطبيق بث','streaming'),
 'com.amazon.amazonvideo.livingroom':('Prime Video','تطبيق بث','streaming'),
 'com.smartdevice.livetv':('Live TV','قنوات الموالف','optional'),
 'com.Smartdevice.emanual':('E-Manual','دليل التلفزيون','optional'),
 'com.facebook.katana':('Facebook Watch','فيديو Facebook','optional'),
 'com.security.SwaiotTV':('Swaiot Home','منزل ذكي من الشركة','optional'),
 'com.qjy.browser':('QJY Browser','متصفح الشركة','optional'),
 'com.google.android.youtube.tvmusic':('YouTube Music','موسيقى','optional'),
 'com.mstar.android.tv.disclaimercustomization':('Disclaimer Customization','واجهة إقرار الشركة','optional'),
 'com.mstar.netflixobserver':('Netflix Observer','مراقب Netflix؛ ليس خدمة Bluetooth','privacy'),
 'com.google.android.youtube.tv':('YouTube','تطبيق بث','streaming'),
 'net.mbc.shahidTV':('شاهد','تطبيق بث','streaming'),
 'com.thmanyah.shasha.tv':('ثمانية','تطبيق بث','streaming'),
 'com.shasha.tv':('شاشا','تطبيق بث خليجي','streaming'),
 'com.shasha.app':('شاشا للموبايل','تطبيق بث خليجي','streaming'),
 'com.todtv.tod':('TOD','تطبيق بث','streaming'),
 'com.crunchyroll.crunchyroid':('Crunchyroll','تطبيق بث','streaming'),
 'tv.twitch.android.app':('Twitch','تطبيق بث','streaming'),
 'com.FDGEntertainment.redball4.gp':('Red Ball 4','لعبة','optional'),
}
PROTECTED={'com.google.android.katniss','com.mediatek.partner.netflix','com.google.android.apps.mediashell','com.google.android.gms','com.google.android.gsf','com.android.vending','kw.urtv.home','kw.urtv.screensaver','com.google.android.packageinstaller','com.android.packageinstaller','com.android.permissioncontroller','com.google.android.permissioncontroller','com.android.systemui','android','com.android.bluetooth','com.android.tv.settings','com.google.android.tv.remote.service','com.liskovsoft.leankeyboard','com.google.android.inputmethod.latin'}
LABELS={'com.google.android.katniss':'الميكروفون والبحث الصوتي','com.mediatek.partner.netflix':'MediaTek · خدمة Bluetooth مشتركة','com.google.android.apps.mediashell':'Chromecast','kw.urtv.home':'KLightTV · الرئيسية','kw.urtv.screensaver':'KLightTV · شاشة التوقف','com.android.vending':'متجر Google Play','com.google.android.gms':'خدمات Google Play'}
PKG=re.compile(r'[A-Za-z0-9_]+(?:\.[A-Za-z0-9_]+)+\Z')
class Manager:
 def __init__(self,adb,serial,snapshot=None,history=None):
  self.adb=adb;self.serial=serial;self.snapshot=snapshot;self.history=pathlib.Path(history or pathlib.Path.home()/'.urtv-manager');self.lock=threading.Lock();self.items={};self.revision='';self.device='';self.identity={}
 def run(self,*args):
  if self.snapshot:raise ValueError('وضع المعاينة لا يغيّر أي جهاز')
  p=subprocess.run([self.adb,'-s',self.serial,*args],capture_output=True,text=True,timeout=30)
  if p.returncode:raise RuntimeError(p.stderr.strip() or p.stdout.strip() or 'ADB failed')
  return p.stdout.strip()
 def sh(self,*args):return self.run('shell',*args)
 def enabled(self,pkg):
  s=self.sh('dumpsys','package',pkg);m=re.search(r'User 0:.*?\benabled=(\d+)',s);u=re.search(r'\b(?:userId|appId)=(\d+)',s)
  if not m:raise ValueError('تعذرت قراءة حالة التطبيق')
  return int(m[1]),int(u[1]) if u else -1
 def inventory(self):
  if self.snapshot:
   d=json.loads(pathlib.Path(self.snapshot).read_text());packages=[p.removeprefix('package:') for p in d['packages'].splitlines()];disabled=set(p.removeprefix('package:') for p in d.get('disabled','').splitlines()) if isinstance(d.get('disabled'),str) else set(d.get('disabled',[]));third=set();self.device=d.get('model','Skyworth')+' · سجل محفوظ، ليس اتصالاً حياً';self.identity={'snapshot':True}
  else:
   self.device=self.sh('getprop','ro.product.model');self.identity={'serial':self.serial,'model':self.device,'fingerprint':self.sh('getprop','ro.build.fingerprint')}
   packages=[p[8:] for p in self.sh('pm','list','packages','--user','0').splitlines() if p.startswith('package:')];disabled={p[8:] for p in self.sh('pm','list','packages','-d','--user','0').splitlines()};third={p[8:] for p in self.sh('pm','list','packages','-3','--user','0').splitlines()}
  out=[]
  for p in sorted(set(packages)):
   info=OPTIONAL.get(p);protected=p in PROTECTED or (not info and p not in third)
   name,description,kind=info or (LABELS.get(p,p),'خدمة نظام غير مصنفة؛ محمية من التغيير' if protected else 'تطبيق مستخدم','protected' if protected else 'user')
   out.append(dict(package=p,name=name,description=description,kind=kind,enabled=p not in disabled,protected=protected,system=p not in third))
  out.sort(key=lambda x:({'streaming':0,'optional':1,'privacy':2,'user':3,'protected':4}.get(x['kind'],4),x['name']))
  self.items={x['package']:x for x in out};self.revision=secrets.token_hex(16)
  return dict(device=self.device,serial=self.serial,preview=bool(self.snapshot),apps=out,revision=self.revision)
 def validate(self,changes,revision):
  if self.snapshot:raise ValueError('المعاينة فقط؛ اتصل بالجهاز للتنفيذ')
  if revision!=self.revision:raise ValueError('تغيرت القائمة؛ أعد الفحص قبل التنفيذ')
  if not isinstance(changes,list) or not 1<=len(changes)<=60:raise ValueError('اختر من ١ إلى ٦٠ تطبيقاً')
  seen=set()
  for c in changes:
   p=c.get('package','');action=c.get('action');item=self.items.get(p)
   if not PKG.fullmatch(p) or p in seen or not item or item['protected']:raise ValueError('حزمة محمية أو غير معروفة')
   seen.add(p)
   if action not in ['enable','disable','background','uninstall']:raise ValueError('عملية غير معروفة')
   if action=='uninstall' and (p not in OPTIONAL or not item['system']):raise ValueError('الإزالة هنا لتطبيقات النظام الاختيارية فقط؛ تطبيق المستخدم يُزال من تأكيد Android على التلفزيون')
 def execute(self,changes,revision):
  with self.lock:
   self.validate(changes,revision)
   if self.sh('getprop','ro.build.fingerprint')!=self.identity['fingerprint']:raise ValueError('تغير الجهاز؛ أعد الاتصال')
   # Snapshot every selected state before the first mutation, including application-level UID protection.
   states=[]
   for c in changes:
    state,uid=self.enabled(c['package'])
    if uid<10000:raise ValueError('خدمة ذات هوية نظام مشتركة محمية: '+c['package'])
    op=self.sh('cmd','appops','get',c['package'],'RUN_ANY_IN_BACKGROUND')
    mode=re.search(r'RUN_ANY_IN_BACKGROUND:\s*(allow|ignore|deny|default|foreground)',op)
    states.append(dict(package=c['package'],action=c['action'],enabled=state,appop=mode[1] if mode else 'default'))
   record=dict(device=self.identity,time=time.time(),states=states,results=[],restored=False);self.history.mkdir(parents=True,exist_ok=True);path=self.history/(str(time.time_ns())+'.json');path.write_text(json.dumps(record,ensure_ascii=False,indent=2));self.revision=''
   for s in states:
    p=s['package'];a=s['action']
    try:
     if a=='disable':reply=self.sh('pm','disable-user','--user','0',p)
     elif a=='enable':reply=self.sh('pm','enable','--user','0',p)
     elif a=='background':reply=self.sh('cmd','appops','set',p,'RUN_ANY_IN_BACKGROUND','ignore')
     else:
      reply=self.sh('pm','uninstall','-k','--user','0',p)
      if 'Success' not in reply:raise ValueError(reply)
     if a in ['enable','disable']:
      actual,_=self.enabled(p)
      if actual!=(1 if a=='enable' else 3):raise ValueError('لم يثبت تغيير الحالة')
     if a=='background' and 'ignore' not in self.sh('cmd','appops','get',p,'RUN_ANY_IN_BACKGROUND'):raise ValueError('لم يثبت تقييد الخلفية')
     record['results'].append(dict(package=p,ok=True,reply=reply))
    except Exception as e:record['results'].append(dict(package=p,ok=False,error=str(e)))
    path.write_text(json.dumps(record,ensure_ascii=False,indent=2))
   return dict(results=record['results'],backup=path.name)
 def restore(self):
  with self.lock:
   if self.snapshot:raise ValueError('معاينة فقط')
   files=sorted(self.history.glob('*.json'),reverse=True);record=None
   fingerprint=self.sh('getprop','ro.build.fingerprint')
   for path in files:
    r=json.loads(path.read_text())
    if r['device'].get('serial')==self.serial and r['device'].get('fingerprint')==fingerprint and not r.get('restored'):record=r;break
   if record is None:raise ValueError('لا توجد عملية قابلة للاستعادة لهذا الجهاز')
   results=[]
   # Restores all snapshotted entries, also covers a crash after mutation but before result logging.
   for s in reversed(record['states']):
    p=s['package']
    if p in PROTECTED or not PKG.fullmatch(p):raise ValueError('حزمة محمية في سجل الاستعادة')
    try:
     if s['action']=='uninstall':
      reply=self.sh('cmd','package','install-existing','--user','0',p)
      if 'installed' not in reply.lower():raise ValueError(reply)
     command={0:'default-state',1:'enable',2:'disable',3:'disable-user',4:'disable-until-used'}[s['enabled']]
     self.sh('pm',command,'--user','0',p);self.sh('cmd','appops','set',p,'RUN_ANY_IN_BACKGROUND',s['appop'])
     if self.enabled(p)[0]!=s['enabled']:raise ValueError('تعذر التحقق من الاستعادة')
     results.append(dict(package=p,ok=True))
    except Exception as e:results.append(dict(package=p,ok=False,error=str(e)))
   record['restored']=all(x['ok'] for x in results);record['restore_results']=results;path.write_text(json.dumps(record,ensure_ascii=False,indent=2));self.revision='';return dict(results=results)

def main():
 p=argparse.ArgumentParser();p.add_argument('--serial',required=True);p.add_argument('--adb',default='adb');p.add_argument('--snapshot');p.add_argument('--port',type=int,default=0);p.add_argument('--no-browser',action='store_true');a=p.parse_args()
 m=Manager(a.adb,a.serial,a.snapshot);token=secrets.token_urlsafe(32)
 class Handler(BaseHTTPRequestHandler):
  def log_message(self,*args):pass
  def send(self,status,body,kind='application/json'):
   if not isinstance(body,bytes):body=json.dumps(body,ensure_ascii=False).encode()
   self.send_response(status);self.send_header('Content-Type',kind+'; charset=utf-8');self.send_header('Content-Length',str(len(body)));self.send_header('Cache-Control','no-store');self.send_header('X-Content-Type-Options','nosniff');self.send_header('Content-Security-Policy',"default-src 'self'; script-src 'self'; style-src 'self'; font-src 'self'; img-src 'self' data:; frame-ancestors 'none'");self.end_headers();self.wfile.write(body)
  def auth(self):return self.headers.get('X-UrTV-Token')==token and self.headers.get('Host')==f'127.0.0.1:{self.server.server_port}'
  def do_GET(self):
   path=urllib.parse.urlparse(self.path).path
   if path=='/api/inventory':
    if not self.auth():return self.send(403,{'error':'Unauthorized'})
    try:
     with m.lock:result=m.inventory()
     return self.send(200,result)
    except Exception as e:return self.send(400,{'error':str(e)})
   files={'/':('index.html','text/html'),'/app.js':('app.js','text/javascript'),'/style.css':('style.css','text/css'),'/font.otf':('../../android/launcher/src/main/assets/fonts/Thmanyah-Regular.otf','font/otf')}
   if path not in files:return self.send(404,{})
   file,kind=files[path];self.send(200,(ROOT/file).read_bytes(),kind)
  def do_POST(self):
   if not self.auth():return self.send(403,{'error':'Unauthorized'})
   try:
    length=int(self.headers.get('Content-Length','0'))
    if not 0<length<=65536:raise ValueError('Invalid request size')
    body=json.loads(self.rfile.read(length))
    if self.path=='/api/apply':result=m.execute(body.get('changes'),body.get('revision'))
    elif self.path=='/api/restore':result=m.restore()
    else:return self.send(404,{})
    self.send(200,result)
   except Exception as e:self.send(400,{'error':str(e)})
 server=ThreadingHTTPServer(('127.0.0.1',a.port),Handler);url=f'http://127.0.0.1:{server.server_port}/#'+token
 print(url,flush=True)
 if not a.no_browser:webbrowser.open(url)
 try:server.serve_forever()
 except KeyboardInterrupt:pass
 finally:server.server_close()
if __name__=='__main__':main()
