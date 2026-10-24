#!/usr/bin/env python3
import importlib.util,pathlib,tempfile,unittest,json,zipfile
from unittest.mock import patch
r=pathlib.Path(__file__).resolve().parents[1];spec=importlib.util.spec_from_file_location('installer',r/'tools/install.py');m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
class Fake:
 serial='emulator-5554'
 def __init__(self):self.calls=[];self.settings={k:'null' for k in m.DREAM_KEYS};self.fingerprint='test-build';self.android_id='test-device'
 def call(self,*args,**kwargs):self.calls.append(args);return 'device' if args==('get-state',) else 'Success'
 def shell(self,*args):
  self.calls.append(args)
  if args[:2]==('getprop','ro.build.version.sdk'):return '34'
  if args[:2]==('getprop','ro.build.fingerprint'):return self.fingerprint
  if args==('settings','get','secure','android_id'):return self.android_id
  if args[:3]==('settings','get','secure'):return self.settings[args[3]]
  if args[:3]==('settings','put','secure'):self.settings[args[3]]=args[4];return ''
  if args[:3]==('settings','delete','secure'):self.settings[args[3]]='null';return ''
  if args[:3]==('cmd','package','resolve-activity'):return 'com.example.home/.Home'
  if args[:2]==('cat','/proc/meminfo'):return 'MemTotal: 2097152 kB'
  if args[:2]==('pm','path'):return ''
  if args[:2]==('pm','list'):return 'feature:android.software.leanback'
  if args[:2]==('dumpsys','activity'):return 'mResumedActivity: kw.urtv.home/.MainActivity'
  return 'test'
REAL_GATE=m.safety.gate
REAL_BACKUP=m.prepare_backup
class Tests(unittest.TestCase):
 def setUp(self):
  # Installer behavior is tested independently from the production safety validator.
  self.backup=patch.object(m,"prepare_backup",return_value=pathlib.Path("verified-test-backup")).start()
  self.gate=patch.object(m.safety,"gate",return_value=pathlib.Path("test-policy-record")).start();patch.object(m.safety,"finish").start();self.addCleanup(patch.stopall)
 def test_backup_failure_blocks_every_mutation(self):
  f=Fake();state=m.inspect(f)
  with tempfile.TemporaryDirectory() as d:
   files=[pathlib.Path(d)/n for n in m.FILES]
   for file in files:file.write_bytes(b'test')
   self.backup.side_effect=OSError('disk full')
   with self.assertRaises(OSError):m.install(f,files,state,True,True)
   self.assertFalse(any(c[0]=='install' or c[:2]==('settings','put') or c[:3]==('cmd','package','set-home-activity') for c in f.calls))
   self.gate.assert_not_called()
 def test_backup_failure_blocks_restore(self):
  f=Fake();state=m.inspect(f);self.backup.side_effect=OSError('backup unavailable')
  with self.assertRaises(OSError):m.restore(f,state)
  self.assertFalse(any(c[:2]==('settings','put') or c[:2]==('settings','delete') or c[:3]==('cmd','package','set-home-activity') for c in f.calls))
  self.gate.assert_not_called()
 def test_original_split_apks_are_pulled_and_verified(self):
  # Call the unpatched production function stored before setUp.
  class Original(Fake):
   def shell(self,*args):
    if args[:2]==('pm','path'):return 'package:/data/app/base.apk\npackage:/data/app/split_config.apk' if args[2]=='kw.urtv.home' else ''
    return super().shell(*args)
   def call(self,*args,**kwargs):
    if args[0]=='pull':
     with zipfile.ZipFile(args[2],'w') as z:z.writestr('AndroidManifest.xml','test fixture')
     return 'pulled'
    return super().call(*args,**kwargs)
  f=Original();state=m.inspect(f)
  with tempfile.TemporaryDirectory() as d:
   path=REAL_BACKUP(f,state,pathlib.Path(d));saved=json.loads(path.read_text())
   self.assertEqual(len(saved['apks']),2);self.assertFalse(saved['private_app_data'])
   self.assertTrue(all((path.parent/e['path']).is_file() for e in saved['apks']))
   self.assertFalse(any(c[0]=='install' for c in f.calls))
 def test_missing_policy_blocks_install_even_with_files(self):
  f=Fake();state=m.inspect(f)
  with tempfile.TemporaryDirectory() as d,patch.object(m.safety,"gate",side_effect=REAL_GATE):
   files=[pathlib.Path(d)/n for n in m.FILES]
   for file in files:file.write_bytes(b"test")
   with self.assertRaises(ValueError):m.install(f,files,state)
   self.assertFalse(any(c[0]=="install" or c[:2]==("settings","put") for c in f.calls))

 def test_explicit_device_snapshot_and_restore(self):
  f=Fake();state=m.inspect(f)
  with tempfile.TemporaryDirectory() as d:
   files=[pathlib.Path(d)/name for name in m.FILES]
   for p in files:p.write_bytes(b'local test installer')
   snapshot=m.save_snapshot(state,pathlib.Path(d)/'history');self.assertEqual(json.loads(snapshot.read_text()),state)
   with patch('time.sleep'):m.install(f,files,state,True,True)
   self.assertEqual(len([c for c in f.calls if c[0]=='install']),2)
   self.assertFalse(any('disable-user' in c or 'uninstall' in c for c in f.calls))
   m.restore(f,state);self.assertTrue(all(v=='null' for v in f.settings.values()))
   f.android_id='another'
   with self.assertRaises(RuntimeError):m.restore(f,state)
 def test_invalid_restore_before_mutation(self):
  f=Fake();state=m.inspect(f);state['home']='bad;reboot';before=len(f.calls)
  with self.assertRaises(ValueError):m.restore(f,state)
  self.assertEqual(before,len(f.calls))
 def test_input_and_missing_apk(self):
  self.assertEqual(m.address('192.0.2.50'),'192.0.2.50:5555')
  for value in ['localhost;reboot','192.0.2.50:99999','wrong']:
   with self.assertRaises(Exception):m.address(value)
  with self.assertRaises(ValueError):m.install(Fake(),[pathlib.Path('/missing.apk')],{})
 def test_home_rejection_restores_previous_component(self):
  class RefusesHome(Fake):
   def shell(self,*args):
    if args[:2]==('dumpsys','activity'):return 'mResumedActivity: com.example.home/.Home'
    return super().shell(*args)
  f=RefusesHome();state=m.inspect(f)
  with tempfile.TemporaryDirectory() as d:
   files=[pathlib.Path(d)/n for n in m.FILES]
   for p in files:p.write_bytes(b'local test')
   with patch('time.sleep'),self.assertRaises(RuntimeError):m.install(f,files,state,True,False)
   self.assertIn(('cmd','package','set-home-activity','--user','0',state['home']),f.calls)
 def test_download_integrity_and_corrupt_file_cleanup(self):
  import io,hashlib
  body=b'example installer';digest=hashlib.sha256(body).hexdigest();manifest=''.join(digest+'  '+n+'\n' for n in m.FILES).encode()
  def response(url,**kwargs):return io.BytesIO(manifest if url.endswith('.txt') else body)
  with tempfile.TemporaryDirectory() as d,patch.object(m.urllib.request,'urlopen',side_effect=response):
   files=m.download_installers(pathlib.Path(d),'1.10.0');self.assertTrue(all(p.read_bytes()==body for p in files))
  def corrupt(url,**kwargs):return io.BytesIO(manifest if url.endswith('.txt') else b'corrupt')
  with tempfile.TemporaryDirectory() as d,patch.object(m.urllib.request,'urlopen',side_effect=corrupt):
   with self.assertRaises(ValueError):m.download_installers(pathlib.Path(d),'1.10.0')
   self.assertFalse(list(pathlib.Path(d).glob('*.download')))
   self.assertFalse(list(pathlib.Path(d).glob('*.apk')))
if __name__=='__main__':unittest.main()
