#!/usr/bin/env python3
"""KLightTV guided, reversible ADB setup. No OEM package is disabled or removed."""
from __future__ import annotations
import argparse,datetime,hashlib,ipaddress,json,os,pathlib,re,shlex,shutil,subprocess,sys,urllib.request
REPO='linkq8/KLightTV';VERSION='1.10.3'
FILES=('KLightTV-screensaver-android-tv.apk','KLightTV-home-android-tv.apk')
DREAM_KEYS=('screensaver_enabled','screensaver_components','screensaver_default_component')

class ADB:
 def __init__(self,executable,serial):self.executable,self.serial=executable,serial
 def call(self,*args,timeout=45):
  p=subprocess.run([self.executable,'-s',self.serial,*args],text=True,capture_output=True,timeout=timeout)
  if p.returncode:raise RuntimeError((p.stderr or p.stdout).strip())
  return p.stdout.strip()
 def shell(self,*args):return self.call('shell',shlex.join(args))

def address(value):
 host,sep,port=value.partition(':')
 try:ipaddress.IPv4Address(host);n=int(port) if sep else 5555
 except ValueError:raise argparse.ArgumentTypeError('Use TV IPv4:port, or --serial for an emulator / USB device')
 if not 1<=n<=65535:raise argparse.ArgumentTypeError('Invalid port')
 return f'{host}:{n}'

def download_installers(directory,version):
 if not re.fullmatch(r'[0-9]{1,9}\.[0-9]{1,9}\.[0-9]{1,9}',version):raise ValueError('Invalid version')
 directory.mkdir(parents=True,exist_ok=True)
 base=f'https://github.com/{REPO}/releases/download/v{version}/'
 with urllib.request.urlopen(base+f'KLightTV-{version}-SHA256SUMS.txt',timeout=30) as response:sums=response.read(16384).decode()
 hashes={name:digest for digest,name in (line.split('  ',1) for line in sums.splitlines() if '  ' in line)}
 for name in FILES:
  digest=hashes.get(name,'')
  if not re.fullmatch('[a-f0-9]{64}',digest):raise ValueError('Missing checksum for '+name)
  target=directory/name
  if target.exists() and not target.is_symlink() and hashlib.sha256(target.read_bytes()).hexdigest()==digest:continue
  temp=directory/(name+'.download')
  if temp.is_symlink() or target.is_symlink():raise ValueError('Symlinks not accepted')
  h=hashlib.sha256();size=0
  try:
   with urllib.request.urlopen(base+name,timeout=60) as response,temp.open('wb') as output:
    while chunk:=response.read(262144):
     size+=len(chunk)
     if size>50*1048576:raise ValueError('Unexpected installer size')
     h.update(chunk);output.write(chunk)
   if not size or h.hexdigest()!=digest:raise ValueError('Checksum mismatch for '+name)
   temp.replace(target)
  finally:
   if temp.exists():temp.unlink()
 return [directory/name for name in FILES]

def inspect(adb):
 if adb.call('get-state')!='device':raise RuntimeError('Authorize debugging on the TV first')
 sdk=int(adb.shell('getprop','ro.build.version.sdk'))
 if sdk<26:raise RuntimeError('Android8+ is required')
 features=adb.shell('pm','list','features')
 if not adb.serial.startswith('emulator-') and 'android.software.leanback' not in features and 'android.hardware.type.television' not in features:raise RuntimeError('This device does not report Android TV support; check the selected device')
 memory=adb.shell('cat','/proc/meminfo');match=re.search(r'MemTotal:\s+(\d+)',memory);ram=int(match[1])//1024 if match else 0
 home=adb.shell('cmd','package','resolve-activity','--brief','-a','android.intent.action.MAIN','-c','android.intent.category.HOME').splitlines()[-1]
 if not re.fullmatch(r'[\w.]+/[\w.$]+',home):home=None
 return dict(serial=adb.serial,sdk=sdk,model=adb.shell('getprop','ro.product.model'),ram_mib=ram,
  fingerprint=adb.shell('getprop','ro.build.fingerprint'),android_id=adb.shell('settings','get','secure','android_id'),
  home=home,dream={key:adb.shell('settings','get','secure',key) for key in DREAM_KEYS},
  installed={p:adb.shell('pm','path',p) for p in ('kw.urtv.home','kw.urtv.screensaver')})

def save_snapshot(state,root=None):
 root=pathlib.Path(root or pathlib.Path.home()/'.klighttv'/'installs');root.mkdir(parents=True,exist_ok=True);root.chmod(0o700)
 stamp=datetime.datetime.now(datetime.timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');key=hashlib.sha256(state['android_id'].encode()).hexdigest()[:10]
 path=root/f'{stamp}-{key}.json';path.write_text(json.dumps(state,ensure_ascii=False,indent=2));path.chmod(0o600);return path

def restore(adb,state):
 if state.get('home') and not re.fullmatch(r'[\w.]+/[\w.$]+',state['home']):raise ValueError('Invalid Home component')
 for key,value in state['dream'].items():
  if key not in DREAM_KEYS or (value!='null' and not re.fullmatch(r'[\w.,/$]+',value)):raise ValueError('Invalid screensaver value')
 if adb.shell('getprop','ro.build.fingerprint')!=state['fingerprint'] or adb.shell('settings','get','secure','android_id')!=state['android_id']:raise RuntimeError('Snapshot belongs to a different device or system')
 if state['home']:adb.shell('cmd','package','set-home-activity','--user','0',state['home'])
 for key,value in state['dream'].items():
  if key not in DREAM_KEYS:raise ValueError('Invalid settings key')
  adb.shell('settings','delete','secure',key) if value=='null' else adb.shell('settings','put','secure',key,value)

def install(adb,files,state,home=False,dream=False):
 for path in files:
  if path.is_symlink() or not path.is_file() or not path.stat().st_size:raise ValueError('Missing or invalid APK: '+str(path))
 for path in files:
  output=adb.call('install','-r',str(path.resolve()),timeout=180)
  if not re.search(r'^Success\s*$',output,re.M):raise RuntimeError('Install did not succeed: '+output)
 if dream:
  expected='kw.urtv.screensaver/.ScreenDream'
  for key,value in zip(DREAM_KEYS,('1',expected,expected)):
   adb.shell('settings','put','secure',key,value)
   if adb.shell('settings','get','secure',key)!=value:raise RuntimeError('Firmware rejected screensaver configuration; use its Settings menu')
 if home:
  if not state['home']:raise RuntimeError('Cannot safely switch Home without an original Home component')
  adb.shell('cmd','package','set-home-activity','--user','0','kw.urtv.home/.MainActivity')
  adb.shell('input','keyevent','KEYCODE_HOME')
  import time
  front=''
  for _ in range(3):
   time.sleep(1);front=adb.shell('dumpsys','activity','activities')
   resumed='\n'.join(line for line in front.splitlines() if 'ResumedActivity' in line or 'mResumedActivity' in line)
   if 'kw.urtv.home' in resumed:break
  else:
   adb.shell('cmd','package','set-home-activity','--user','0',state['home'])
   raise RuntimeError('Firmware kept another Home; original Home restored. APKs are installed.')
 adb.shell('am','start','-n','kw.urtv.screensaver/.SettingsActivity')

def main():
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--serial');p.add_argument('--connect',type=address);p.add_argument('--pair',type=address);p.add_argument('--adb');p.add_argument('--apk-dir',type=pathlib.Path);p.add_argument('--version',default=VERSION);p.add_argument('--set-home',action='store_true');p.add_argument('--set-screensaver',action='store_true');p.add_argument('--yes',action='store_true');p.add_argument('--inspect',action='store_true');p.add_argument('--restore',type=pathlib.Path);a=p.parse_args()
 candidates=[a.adb,os.environ.get('ADB'),shutil.which('adb'),str(pathlib.Path.home()/'Library/Android/sdk/platform-tools/adb'),str(pathlib.Path.home()/'AppData/Local/Android/Sdk/platform-tools/adb.exe')]
 binary=next((x for x in candidates if x and pathlib.Path(x).is_file()),None)
 if not binary:raise RuntimeError('Install Android SDK Platform-Tools and add adb to PATH')
 if a.pair:
  import getpass
  code=getpass.getpass('Pairing code shown on TV: ')
  subprocess.run([binary,'pair',a.pair,code],check=True)
 serial=a.serial or a.connect
 if not serial:serial=address(input('TV IPv4:port / عنوان التلفزيون: ').strip())
 if not a.serial:
  subprocess.run([binary,'connect',serial],check=True)
  if not a.yes:input('Allow debugging on the TV, then press Enter: ')
 adb=ADB(binary,serial);state=inspect(adb)
 print(f"{state['model']} · Android API{state['sdk']} · RAM {state['ram_mib']}MiB")
 print('Display mode: automatically selected on TV using RAM and app heap limits.')
 if a.inspect:return
 if a.restore:
  restore(adb,json.loads(a.restore.read_text()));print('Home/screensaver settings restored; apps and their data preserved.');return
 directory=a.apk_dir or pathlib.Path.home()/'.klighttv'/'downloads'/a.version
 files=[directory/name for name in FILES] if a.apk_dir else download_installers(directory,a.version)
 if not a.yes and input('Install both APKs and requested Home/screensaver settings? [y/N] ').lower()!='y':return
 snapshot=save_snapshot(state);print('Restore snapshot: '+str(snapshot))
 try:install(adb,files,state,a.set_home,a.set_screensaver)
 except Exception:
  print('Setup incomplete. Keep the snapshot; restore with --serial DEVICE --restore FILE.');raise
 print('KLightTV installed. Select the region and reading preferences on the TV.')

if __name__=='__main__':
 try:main()
 except (Exception,KeyboardInterrupt) as e:print(str(e),file=sys.stderr);sys.exit(1)
