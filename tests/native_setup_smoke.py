"""Explicit emulator-only UI smoke test. Resets KLightTV launcher preferences on that emulator."""
import os,subprocess,time,xml.etree.ElementTree as ET,re,pathlib,json
ADB=os.environ.get('ADB','/Users/essa/Library/Android/sdk/platform-tools/adb')
SERIAL=os.environ.get('SERIAL','emulator-5556')
if not SERIAL.startswith('emulator-'):raise SystemExit('Emulator required')
OUT=pathlib.Path('outputs/native-setup');OUT.mkdir(parents=True,exist_ok=True)
def adb(*a):return subprocess.check_output([ADB,'-s',SERIAL,*a],timeout=30).decode(errors='replace')
def key(*codes):
 for code in codes:adb('shell','input','keyevent',str(code));time.sleep(.5)
def dump():
 adb('shell','rm','-f','/sdcard/klight-ui.xml')
 result=adb('shell','uiautomator','dump','/sdcard/klight-ui.xml')
 if 'dumped' not in result:raise AssertionError('UI not ready: '+result)
 return ET.fromstring(adb('shell','cat','/sdcard/klight-ui.xml'))
def click(label):
 r=dump();nodes=[n for n in r.iter('node') if n.get('text')==label]
 assert nodes, label+' not visible: '+str([n.get('text') for n in r.iter('node') if n.get('text')])
 n=nodes[-1];v=list(map(int,re.findall(r'\d+',n.get('bounds'))));adb('shell','input','tap',str((v[0]+v[2])//2),str((v[1]+v[3])//2));time.sleep(.5)
 # Focusable-in-touch-mode controls first receive focus; OK activates them.
 if any(n.get('text')==label and n.get('focused')=='true' for n in dump().iter('node')):key(23)
def contains(label):assert any(label in n.get('text','') for n in dump().iter('node')),label

def hardware_button():
 commands=[{'id':1,'command':'register','name':'KLightTestKeyboard','vid':1,'pid':1,'bus':'usb','configuration':[{'type':100,'data':[1]},{'type':101,'data':[30,256]}]},{'id':1,'command':'delay','duration':1500},{'id':1,'command':'inject','events':[1,256,1,0,0,0,1,256,0,0,0,0]},{'id':1,'command':'delay','duration':500}]
 subprocess.run([ADB,'-s',SERIAL,'shell','uinput','-'],input='\n'.join(json.dumps(x) for x in commands),text=True,check=True,timeout=10)

def shot(name):
 time.sleep(.4)
 with (OUT/name).open('wb') as f:subprocess.run([ADB,'-s',SERIAL,'exec-out','screencap','-p'],stdout=f,check=True)
adb('root');adb('wait-for-device')
adb('shell','input','keyevent','KEYCODE_WAKEUP');adb('shell','wm','dismiss-keyguard');adb('shell','svc','power','stayon','true')
adb('shell','pm','clear','kw.urtv.home')
adb('shell','am','start','-n','kw.urtv.home/.MainActivity');time.sleep(2)
contains('أين أنت؟');shot('01-location.png')
# DPAD enters the optional location editor; city survives the two-page setup.
key(23);contains('مدينتك ومواقيت الصلاة');click(next(n.get('text') for n in dump().iter('node') if n.get('text','').startswith('الدولة · ')));click('الكويت');click('مناطق الكويت');click('الخيران');click('اعتماد المدينة');contains('الدولة والمدينة · الخيران')
click('التالي');contains('كيف تحب شاشتك؟')
click(next(n.get('text') for n in dump().iter('node') if n.get('text','').startswith('مظهر التطبيق · ')));click('فاتح');shot('02-options-light.png')
click('تخصيص التطبيقات والريموت · اختياري');contains('على طريقتك');shot('03-optional.png')
click('التطبيقات · إظهار وإخفاء وإدارة');time.sleep(1);contains('مركز التطبيقات');shot('04-apps.png');key(4)
click('الريموت · اختر الزر والتطبيق');contains('الأزرار الأصلية');shot('05-remote-original.png')
click('إضافة زر');key(188);contains('اختر التطبيق')
r=dump();nodes=[n for n in r.iter('node') if n.get('text')=='Chrome']
assert nodes,'Chrome required in emulator image'
click('Chrome');contains('BUTTON_1');shot('06-remote-mapping.png')
# A mapping alone must not enable accessibility.
assert 'kw.urtv.home' not in adb('shell','settings','get','secure','enabled_accessibility_services')
old=adb('shell','settings','get','secure','enabled_accessibility_services').strip()
old_enabled=adb('shell','settings','get','secure','accessibility_enabled').strip()
try:
 component='kw.urtv.home/kw.urtv.home.RemoteButtonService'
 adb('shell','settings','put','secure','enabled_accessibility_services',component)
 adb('shell','settings','put','secure','accessibility_enabled','1');time.sleep(1)
 key(4,4) # Back to setup; service ignores keys only while remote editor is foreground.
 contains('كيف تحب شاشتك؟')
 hardware_button();time.sleep(1)
 assert 'com.android.chrome' in adb('shell','dumpsys','activity','activities').split('topResumedActivity=')[-1].split('\n')[0]
 key(4);time.sleep(.3)
finally:
 if old in ('','null'):adb('shell','settings','delete','secure','enabled_accessibility_services')
 else:adb('shell','settings','put','secure','enabled_accessibility_services',old)
 adb('shell','settings','put','secure','accessibility_enabled',old_enabled if old_enabled!='null' else '0')
# Starting Home may resume the existing setup or recreate it after Chrome.
r=dump()
if any('أين أنت؟'==n.get('text') for n in r.iter('node')):click('التالي')
contains('كيف تحب شاشتك؟')
click('تخصيص التطبيقات والريموت · اختياري');click('الريموت · اختر الزر والتطبيق');click('استعادة الأزرار الأصلية')
assert 'BUTTON_1' not in ET.tostring(dump(),encoding='unicode')
key(4,4);click('ابدأ');time.sleep(2)
assert 'setup_complete' in adb('shell','run-as','kw.urtv.home','cat','shared_prefs/MainActivity.xml')
assert 'الخيران' in adb('shell','run-as','kw.urtv.screensaver','cat','shared_prefs/kuwait_settings.xml')
shot('07-home.png')
adb('shell','am','force-stop','kw.urtv.home');adb('shell','am','start','-n','kw.urtv.home/.MainActivity');time.sleep(1)
assert 'SetupActivity' not in adb('shell','dumpsys','activity','activities').split('topResumedActivity=')[-1].split('\n')[0]
assert 'FATAL EXCEPTION' not in adb('logcat','-d','-s','AndroidRuntime:E')
print('PASS native setup: two pages, DPAD, region persisted, optional app inventory, explicit remote mapping, original default, restore, completion and restart skip')
