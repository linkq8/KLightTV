#!/usr/bin/env python3
"""Fail-closed device-change policy. Research is a reviewable human record, not inferred safety."""
import argparse,os,hashlib,json,pathlib,re,shlex,subprocess,time,urllib.parse
VERSION=1
CORE={'android','com.smartdevice.livetv','com.mediatek.tvinput','com.mediatek.tvinputservice.arbitratorservice','com.mediatek.hotkey.dispatcher','com.mediatek.tv.agent','com.android.providers.tv','com.google.android.katniss','com.mediatek.partner.netflix','com.google.android.apps.mediashell','com.google.android.gms','com.google.android.gsf','com.android.vending','com.android.systemui','com.android.bluetooth','com.android.tv.settings','com.google.android.tv.remote.service','com.google.android.packageinstaller','com.android.packageinstaller','com.android.permissioncontroller','com.google.android.permissioncontroller','com.google.android.inputmethod.latin','kw.urtv.home','kw.urtv.screensaver'}
CORE_PATTERN=re.compile(r'(?:tvinput|livetv|live\.tv|hdmi|tuner|cec|bluetooth|remote|hotkey|keymap|inputmethod|audio|microphone|voice|telecom|network|wifi|wlan|ethernet|mediashell|drm|widevine|launcher)',re.I)
FEATURES=('channels','hdmi','remote','audio','network','voice','streaming')
STORE=pathlib.Path.home()/'.klighttv'/'device-safety'
def digest(v):return hashlib.sha256(json.dumps(v,sort_keys=True,separators=(',',':')).encode()).hexdigest()
def protected(package,dynamic=()):return package in CORE or package in dynamic or bool(CORE_PATTERN.search(package))
def read(path):
 p=pathlib.Path(path)
 if p.is_symlink() or p.stat().st_size>4*1048576:raise ValueError('Invalid policy file')
 return json.loads(p.read_text())
def write(path,value):
 p=pathlib.Path(path);p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(value,ensure_ascii=False,indent=2));p.chmod(0o600)
def tv_recovery_evidence(shell,identity):
 # Deliberately narrow: only the firmware family's original Live TV host, never arbitrary OEM packages.
 if identity.get('manufacturer','').lower()!='skyworth' or identity.get('model')!='SWTV-20AE':return {}
 raw=shell('dumpsys','package','com.smartdevice.livetv')
 user=re.search(r'User 0:.*?installed=(true|false).*?enabled=(\d+)',raw)
 path=re.search(r'codePath=(\S+)',raw)
 flags=re.search(r'pkgFlags=\[([^]]+)\]',raw)
 if not user or not path or not flags:return {}
 return dict(package='com.smartdevice.livetv',code_path=path[1],installed=user[1]=='true',enabled=int(user[2]),system='SYSTEM' in flags[1].split(),tv_handler='LiveTvScreenActivity' in raw and 'vnd.android.cursor.item/channel' in raw)

def inspect(shell,serial):
 props={k:shell('getprop',k).strip() for k in ('ro.product.manufacturer','ro.product.model','ro.product.device','ro.build.fingerprint','ro.build.version.release','ro.build.version.sdk','ro.product.cpu.abilist')}
 if any(not props[k] for k in ('ro.product.manufacturer','ro.product.model','ro.build.fingerprint','ro.build.version.sdk')):raise ValueError('تعذر تحديد الجهاز والنظام بدقة')
 identity=dict(serial=serial,manufacturer=props['ro.product.manufacturer'],model=props['ro.product.model'],device=props['ro.product.device'],fingerprint=props['ro.build.fingerprint'],android_id=shell('settings','get','secure','android_id').strip())
 if identity['android_id'] in ('','null'):raise ValueError('تعذر تحديد هوية الجهاز')
 features=shell('pm','list','features')
 if 'android.software.leanback' not in features and 'android.hardware.type.television' not in features and not serial.startswith('emulator-'):raise ValueError('الجهاز لا يعلن دعم Android TV')
 packages=sorted(set(re.findall(r'^package:([^\s]+)',shell('pm','list','packages','--user','0'),re.M)))
 if not packages:raise ValueError('تعذر قراءة حزم الجهاز')
 # Discover live input services, bound voice/keyboard/accessibility and HOME rather than model-name guessing.
 inputs=shell('dumpsys','tv_input');bindings='\n'.join(shell('settings','get','secure',k) for k in ('voice_interaction_service','voice_recognition_service','default_input_method','enabled_accessibility_services'))
 home=shell('cmd','package','resolve-activity','--brief','-a','android.intent.action.MAIN','-c','android.intent.category.HOME')
 dynamic=sorted(set(re.findall(r'([A-Za-z][\w]*(?:\.[\w]+)+)/[\w.$]+',inputs+'\n'+bindings+'\n'+home)))
 uid_text=shell('pm','list','packages','-U','--user','0');uids={p:int(uid) for p,uid in re.findall(r'package:([^\s]+)\s+uid:(\d+)',uid_text)}
 baseline={p:shell('dumpsys','package',p) for p in packages if protected(p,dynamic)}
 enabled={p:(re.search(r'User 0:.*?\benabled=(\d+)',raw).group(1) if re.search(r'User 0:.*?\benabled=(\d+)',raw) else 'unknown') for p,raw in baseline.items()}
 audit=dict(policy=VERSION,created=time.time(),identity=identity,properties=props,features=features,packages=packages,uids=uids,dynamic_protected=dynamic,core_states=enabled,input_services=sorted(set(re.findall(r'([\w.]+/[\w.$]+)',inputs))),memory=shell('cat','/proc/meminfo'),display=shell('wm','size'),remote_bindings=bindings)
 audit['tv_recovery']=tv_recovery_evidence(shell,identity)
 audit['id']=digest(audit);return audit

def template(audit,actions):
 return dict(policy=VERSION,audit_id=audit['id'],identity=audit['identity'],reviewed_at=0,reviewer='',sources=[],baseline_checks={k:False for k in FEATURES},findings='',actions=[dict(operation=a,rationale='',dependencies_checked=False,rollback='') for a in actions])
def operation_allowed(op,audit):
 if not isinstance(op,dict):raise ValueError('Invalid operation')
 kind=op.get('kind')
 if kind=='package':
  p=op.get('package','')
  if not re.fullmatch(r'[A-Za-z0-9_]+(?:\.[A-Za-z0-9_]+)+',p) or p not in audit['packages'] or protected(p,audit['dynamic_protected']):raise ValueError('حزمة أساسية أو غير معروفة محمية: '+p)
  uid=audit.get('uids',{}).get(p,-1)
  if uid<10000 or sum(v==uid for v in audit.get('uids',{}).values())!=1:raise ValueError('هوية تطبيق مجهولة أو مشتركة؛ التعديل محظور')
  if op.get('action') not in ('enable','disable','background','uninstall'):raise ValueError('عملية حزم غير مسموحة')
 elif kind=='restore-tv-package':
  evidence=audit.get('tv_recovery',{})
  if set(op)!={'kind','package','evidence_sha256'} or op.get('package')!='com.smartdevice.livetv' or op.get('evidence_sha256')!=digest(evidence):raise ValueError('Restoration must match the inspected original TV package')
  if audit['identity'].get('manufacturer','').lower()!='skyworth' or audit['identity'].get('model')!='SWTV-20AE' or evidence.get('package')!=op['package'] or evidence.get('code_path')!='/system/product/app/OceanusLiveTv' or evidence.get('installed') is not False or evidence.get('enabled')!=0 or evidence.get('system') is not True or evidence.get('tv_handler') is not True:raise ValueError('Original system TV host restoration prerequisites not met')
 elif kind=='install':
  if op.get('package') not in ('kw.urtv.home','kw.urtv.screensaver') or not re.fullmatch('[a-f0-9]{64}',op.get('sha256','')):raise ValueError('خطة التثبيت غير صالحة')
 elif kind not in ('set-home','set-screensaver','restore-install','restore-packages'):raise ValueError('العملية غير مسموحة؛ لا تعيين تلقائي لأزرار الريموت')

def validate(audit,review,actions,now=None):
 now=time.time() if now is None else now
 body={k:v for k,v in audit.items() if k!='id'}
 if any(v=='unknown' for v in audit.get('core_states',{}).values()):raise ValueError('حالة خدمة أساسية مجهولة؛ لا تعديل حتى استكمال الفحص')
 if audit.get('policy')!=VERSION or audit.get('id')!=digest(body) or not 0<=now-audit['created']<=3600:raise ValueError('الفحص مفقود أو تغير أو مضى عليه أكثر من ساعة؛ أعد الفحص والمراجعة')
 if review.get('policy')!=VERSION or review.get('audit_id')!=audit['id'] or review.get('identity')!=audit['identity']:raise ValueError('المراجعة لا تطابق الجهاز وإصدار نظامه')
 if not 0<=now-review.get('reviewed_at',0)<=3600 or not str(review.get('reviewer','')).strip():raise ValueError('تحتاج مراجعة بحث حديثة باسم المراجع')
 sources=review.get('sources',[]);hosts=set()
 for source in sources:
  url=urllib.parse.urlparse(source.get('url',''))
  if url.scheme!='https' or not url.hostname or len(source.get('finding','').strip())<20 or source.get('model')!=audit['identity']['model'] or source.get('firmware')!=audit['identity']['fingerprint']:raise ValueError('وثّق صلة كل مصدر بالموديل وإصدار النظام ونتيجته')
  hosts.add(url.hostname.lower())
 if len(hosts)<2 or len(review.get('findings','').strip())<30:raise ValueError('يلزم مصدران مستقلان وتحليل للتوافق والمخاطر؛ وجود روابط وحده لا يثبت الأمان')
 repair_only=len(actions)==1 and actions[0].get('kind')=='restore-tv-package'
 if repair_only:
  # A broken input cannot honestly pass the normal baseline. Unknown observations stay unknown.
  observations=review.get('baseline_observations',{})
  for feature in FEATURES:
   observation=observations.get(feature,{})
   if observation.get('status') not in ('working','failed','not-tested') or len(observation.get('evidence','').strip())<20:raise ValueError('Document each baseline observation, including untested functions')
  if observations['hdmi']['status']!='failed':raise ValueError('This recovery is only for a documented HDMI failure')
 elif any(review.get('baseline_checks',{}).get(k) is not True for k in FEATURES):raise ValueError('تحقق من الوظائف الأساسية قبل التعديل؛ الجهاز غير المختبر لا يُعد آمنًا')
 approved=review.get('actions',[])
 if not actions:raise ValueError('لا توجد خطة تغييرات')
 for op in actions:
  operation_allowed(op,audit)
  matches=[a for a in approved if a.get('operation')==op]
  if len(matches)!=1 or matches[0].get('dependencies_checked') is not True or len(matches[0].get('rationale','').strip())<20 or len(matches[0].get('rollback','').strip())<20:raise ValueError('كل تغيير يحتاج تحليل اعتماديات وسببًا وخطة تراجع مطابقة')

def gate(shell,serial,audit_path,review_path,actions,store=None,recovery=False):
 if not audit_path or not review_path:raise ValueError('التعديل محظور: نفّذ device_safety.py inspect ثم أكمل ملف المراجعة ومرّر --audit و--review')
 audit,review=read(audit_path),read(review_path);validate(audit,review,actions)
 fresh=inspect(shell,serial)
 if any(fresh[k]!=audit[k] for k in ('identity','packages','uids','dynamic_protected','input_services','remote_bindings','core_states','tv_recovery')):raise ValueError('تغير الجهاز أو خدماته أو حزمه منذ الفحص؛ أعد الفحص والمراجعة')
 root=pathlib.Path(store or STORE)
 root.mkdir(parents=True,exist_ok=True);lock=root/'.gate-lock'
 try:fd=os.open(lock,os.O_CREAT|os.O_EXCL|os.O_WRONLY,0o600)
 except FileExistsError:raise ValueError('يوجد فحص تنفيذ آخر؛ لا تبدأ عمليتين معًا')
 try:
  if not recovery:
   for path in root.glob('*.json'):
    record=read(path)
    if record.get('identity')==fresh['identity'] and not record.get('verified'):raise ValueError('يوجد تعديل سابق لم يُفحص بعد؛ تحقق من الوظائف أو تراجع قبل تعديل جديد')
  record=dict(identity=fresh['identity'],created=time.time(),audit=fresh,review=review,actions=actions,verified=False,results=[])
  path=root/(str(time.time_ns())+'.json');write(path,record);return path
 finally:os.close(fd);lock.unlink()

def finish(path,results):
 record=read(path);record['results']=results;write(path,record)
def verify_input_services(before,after,actions):
 # Restoring this host registers its own TV input service. No removed/replaced input is acceptable.
 added=set(after)-set(before);removed=set(before)-set(after)
 expected={'com.smartdevice.livetv/skyworth.platformsupport.componentSupport.AndroidTvSupportTvinputService'} if len(actions)==1 and actions[0].get('kind')=='restore-tv-package' else set()
 if removed or not added.issubset(expected):raise ValueError('تغيرت خدمات المداخل خارج أثر الاستعادة المراجع')
def verify(shell,serial,path,checks):
 record=read(path);fresh=inspect(shell,serial);before=record['audit']
 if fresh['identity']!=record['identity']:raise ValueError('سجل التحقق لجهاز آخر')
 verify_input_services(before['input_services'],fresh['input_services'],record['actions'])
 if fresh['remote_bindings']!=before['remote_bindings']:raise ValueError('تغيرت ارتباطات الصوت/الريموت؛ يلزم التحقيق والتراجع')
 for package,value in before['core_states'].items():
  match=re.search(r'User 0:.*?\benabled=(\d+)',shell('dumpsys','package',package))
  if not match or match[1]!=value:raise ValueError('تغيرت خدمة أساسية')
 for action in record['actions']:
  if action['kind']=='restore-tv-package':
   evidence=fresh.get('tv_recovery',{})
   if evidence.get('installed') is not True or evidence.get('enabled')!=0 or not evidence.get('system') or evidence.get('code_path')!=before['tv_recovery']['code_path']:raise ValueError('Original TV host restoration is not confirmed')
 if set(checks)!=set(FEATURES):raise ValueError('يجب اختبار القنوات وHDMI والريموت والصوت والشبكة والميكروفون والبث فعليًا')
 record['verified']=True;record['verified_at']=time.time();record['checks']=list(checks);write(path,record)
 if any(a['kind'] in ('restore-install','restore-packages','restore-tv-package') for a in record['actions']):
  for previous in pathlib.Path(path).parent.glob('*.json'):
   old=read(previous)
   if old.get('identity')==record['identity'] and old.get('created',0)<record['created'] and not old.get('verified'):
    old['verified']=True;old['closed_by_verified_recovery']=str(path);write(previous,old)

def readonly_command(command):
 # Legacy arbitrary shell/batch cannot bypass the policy by quoting, pipelines or substitutions.
 if any(c in command for c in ';|&`$><\n\r'):return False
 try:a=shlex.split(command)
 except ValueError:return False
 if not a:return False
 if a[0]=='getprop':return len(a)<=2
 if a[0]=='dumpsys':return (len(a)==2 and a[1] in ('tv_input','package','meminfo','input','audio','wifi','connectivity','window','power','display')) or (len(a)==3 and a[1] in ('package','meminfo') and re.fullmatch(r'[A-Za-z][\w]*(?:\.[\w]+)+',a[2]) is not None) or a==['dumpsys','activity','activities']
 if a[:2] in (['pm','list'],['pm','path']):return all(re.fullmatch(r'[\w.=-]+',x) for x in a)
 if a[:2]==['settings','get']:return len(a)==4 and a[2] in ('secure','global','system') and re.fullmatch(r'\w+',a[3]) is not None
 return a in (['wm','size'],['wm','density'],['cat','/proc/meminfo'],['cat','/proc/uptime'],['df','-k','/data'],['screencap','-p','/sdcard/_shot.png'],['rm','/sdcard/_shot.png'])

def main():
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('command',choices=['inspect','verify']);p.add_argument('--adb',default='adb');p.add_argument('--serial',required=True);p.add_argument('--out',type=pathlib.Path,default=pathlib.Path('device-audit.json'));p.add_argument('--plan',type=pathlib.Path);p.add_argument('--record',type=pathlib.Path);p.add_argument('--checked',nargs='+',default=[]);a=p.parse_args()
 def shell(*args):return subprocess.check_output([a.adb,'-s',a.serial,'shell',shlex.join(args)],text=True,timeout=30).strip()
 if a.command=='inspect':
  audit=inspect(shell,a.serial);write(a.out,audit);review=a.out.with_name(a.out.stem+'-review.json');write(review,template(audit,read(a.plan) if a.plan else []));print(str(a.out)+'\n'+str(review)+'\nRead-only inspection complete. Research the exact model/firmware before completing the review.')
 else:
  if not a.record:p.error('--record is required')
  verify(shell,a.serial,a.record,a.checked);print('Functional verification recorded; no device setting changed.')
if __name__=='__main__':main()
