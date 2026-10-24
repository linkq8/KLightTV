"""Portable preferences and an explicit, read-only installation plan. No device writes."""
import math,re,json,unicodedata
from zoneinfo import ZoneInfo,ZoneInfoNotFoundError
FORMAT='klighttv-profile-1'
METHODS={'kuwait','mwl','isna','egypt','umm-al-qura','karachi','local'}
def allowed_text(value):
 text=''.join(c for c in unicodedata.normalize('NFD',str(value)).lower() if unicodedata.category(c)!='Mn').replace('ـ','')
 return not any(x in text for x in ('israel','اسرائيل','اسراييل','ישראל','isr.'))
def default_profile():
 return {'format':FORMAT,'language':'ar','location':{'country':'KW','city':'مدينة الكويت','latitude':29.3759,'longitude':47.9774,'timezone':'Asia/Kuwait'},'appearance':{'theme':'auto','reading':0,'performance':'auto'},'content':{'clock':True,'prayers':True,'weather':True},'prayer':{'method':'kuwait','asr':'standard','high_latitude':'middle-night','corrections':[0]*6,'reminder':-1},'wallpapers':{'category':'city','max_images':3,'budget_mib':24},'remote':{'mode':'original','mappings':[]},'applications':[]}
def validate(p):
 def require(ok,msg):
  if not ok:raise ValueError(msg)
 require(isinstance(p,dict),'ملف إعداد غير صالح')
 expected=set(default_profile());require(set(p)==expected,'حقول الملف غير مكتملة أو غير معروفة')
 require(p['format']==FORMAT,'نسخة ملف غير مدعومة');require(p['language'] in ('ar','en'),'لغة غير مدعومة')
 def section(k,keys):
  d=p[k];require(isinstance(d,dict) and set(d)==set(keys),f'حقول {k} غير صالحة');return d
 l=section('location',['country','city','latitude','longitude','timezone'])
 require(isinstance(l['country'],str) and re.fullmatch('[A-Z]{2}',l['country']) and l['country']!='IL','رمز الدولة غير صالح')
 require(allowed_text(l['city']) and allowed_text(l['timezone']) and isinstance(l['city'],str) and 1<=len(l['city'].strip())<=100 and not any(ord(x)<32 for x in l['city']),'اسم المدينة غير صالح')
 for key,limit in [('latitude',90),('longitude',180)]:require(type(l[key]) in (float,int) and math.isfinite(l[key]) and abs(l[key])<=limit,'إحداثيات غير صالحة')
 try:ZoneInfo(l['timezone'])
 except (ZoneInfoNotFoundError,TypeError,ValueError):raise ValueError('منطقة زمنية غير صالحة')
 a=section('appearance',['theme','reading','performance']);require(a['theme'] in ('light','dark','auto') and type(a['reading']) is int and 0<=a['reading']<=2 and a['performance'] in ('auto','lite','normal'),'خيارات المظهر غير صالحة')
 c=section('content',['clock','prayers','weather']);require(all(type(v) is bool for v in c.values()),'خيارات المحتوى غير صالحة')
 q=section('prayer',['method','asr','high_latitude','corrections','reminder']);require(q['method'] in METHODS and q['asr'] in ('standard','hanafi') and q['high_latitude'] in ('middle-night','seventh-night','angle'),'طريقة الصلاة غير صالحة');require(type(q['reminder']) is int and q['reminder'] in (-1,0,5),'توقيت التنبيه غير صالح');require(isinstance(q['corrections'],list) and len(q['corrections'])==6 and all(type(n) is int and -30<=n<=30 for n in q['corrections']),'تصحيح المواقيت غير صالح')
 w=section('wallpapers',['category','max_images','budget_mib']);require(w['category'] in ('city','nature','personal','none') and type(w['max_images']) is int and 0<=w['max_images']<=5 and type(w['budget_mib']) is int and 8<=w['budget_mib']<=64,'حدود الصور غير صالحة')
 r=section('remote',['mode','mappings']);require(r['mode'] in ('original','custom') and isinstance(r['mappings'],list) and len(r['mappings'])<=8,'خيارات الريموت غير صالحة');require(r['mode']!='original' or not r['mappings'],'الريموت الأصلي لا يقبل تعيينات')
 for m in r['mappings']:require(isinstance(m,dict) and set(m)=={'key','package'} and type(m['key']) is int and 1<=m['key']<=300 and isinstance(m['package'],str) and re.fullmatch(r'[A-Za-z0-9_]+(?:\.[A-Za-z0-9_]+)+',m['package']),'تعيين زر غير صالح')
 require(isinstance(p['applications'],list) and len(p['applications'])<=300,'قائمة التطبيقات غير صالحة');seen=set()
 for x in p['applications']:
  require(isinstance(x,dict) and set(x)=={'package','action'},'اختيار تطبيق غير صالح');pkg=x['package'];require(isinstance(pkg,str) and re.fullmatch(r'[A-Za-z0-9_]+(?:\.[A-Za-z0-9_]+)+',pkg) and pkg not in seen,'حزمة غير صالحة أو مكررة');seen.add(pkg);require(x['action'] in ('keep','hide','disable','remove'),'إجراء غير صالح')
 require(len(json.dumps(p))<32768,'الملف كبير جدًا');return p

def plan(p,device=None):
 validate(p);d=device or {};ram=d.get('ram_mib',0);heap=d.get('heap_mib',0);selected=p['appearance']['performance'];lite=selected=='lite' or (selected=='auto' and (not ram or ram<=2304 or not heap or heap<=128)) or (heap and heap<96)
 blockers=[]
 if d.get('sdk',26)<26:blockers.append('يلزم Android 8 أو أحدث')
 if d.get('television') is False:blockers.append('لم يعلن الجهاز دعم Android TV؛ يلزم التحقق قبل التثبيت')
 # Current native implementation is Kuwait-only. Never silently apply a global city to it.
 pending=['ربط الملف بالتطبيق بعد اكتمال محرك المدن العالمي','تنزيل حزمة خلفيات الدولة عند الطلب بعد تجهيز المكتبة']
 if p['location']['country']!='KW' or p['location']['timezone']!='Asia/Kuwait':pending.append('الموقع خارج الكويت: لا تطبّق حساب الكويت؛ ينتظر اختبار طريقة الصلاة والمنطقة الزمنية')
 if p['remote']['mode']=='custom':pending.append('تخصيص الأزرار يحتاج فحص إشارة الريموت وموافقة مستقلة')
 changes=[x for x in p['applications'] if x['action']!='keep']
 if changes:pending.append('اختيارات التطبيقات تنتظر جردًا حيًا ومراجعة الحماية؛ الملف لا ينفّذها')
 return {'format':'klighttv-install-plan-1','ready_to_apply':False,'mode':'lite' if lite else 'normal','reason':'اختيار محافظ عند نقص بيانات الذاكرة' if not ram or not heap else 'حسب الذاكرة وحد التطبيق واختيار المستخدم','image_edge':1280 if heap and heap<96 else 1920 if lite else 3840,'location':p['location'],'theme':p['appearance']['theme'],'remote_unchanged':True,'requested_app_changes':changes,'automatic_app_changes':[],'blockers':blockers,'pending':pending}
