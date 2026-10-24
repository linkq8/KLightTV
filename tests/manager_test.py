import importlib.util,pathlib,tempfile,unittest,json
ROOT=pathlib.Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('manager',ROOT/'tools/tv-manager/server.py');mod=importlib.util.module_from_spec(spec);spec.loader.exec_module(mod)
class Fake(mod.Manager):
 def __init__(self,path):
  super().__init__('adb','test-device',history=path);self.calls=[];self.states={'com.google.android.play.games':0,'com.google.android.videos':3};self.ops={p:'default' for p in self.states};self.items={p:dict(protected=False,system=True)for p in self.states};self.identity={'serial':'test-device','fingerprint':'test'};self.revision='rev';self.uid=10001
 def enabled(self,p):return self.states[p],self.uid
 def sh(self,*a):
  self.calls.append(a)
  if a[0]=='getprop':return 'test'
  if a[:3]==('cmd','appops','get'):return 'RUN_ANY_IN_BACKGROUND: '+self.ops[a[3]]
  if a[:3]==('cmd','appops','set'):self.ops[a[3]]=a[5];return ''
  if a[0]=='pm':self.states[a[-1]]={'enable':1,'default-state':0,'disable-user':3,'disable':2,'disable-until-used':4}[a[1]];return 'new state'
  raise AssertionError(a)
REAL_GATE=mod.safety.gate
class Tests(unittest.TestCase):
 def setUp(self):
  from unittest.mock import patch
  # Existing execution/rollback unit tests isolate the independently tested policy boundary.
  self.gate=patch.object(mod.safety,"gate",return_value=pathlib.Path("test-policy-record")).start();patch.object(mod.safety,"finish").start();self.addCleanup(patch.stopall)
 def test_missing_policy_prevents_all_mutations(self):
  from unittest.mock import patch
  with tempfile.TemporaryDirectory() as d,patch.object(mod.safety,"gate",side_effect=REAL_GATE):
   m=Fake(d)
   with self.assertRaises(ValueError):m.execute([dict(package="com.google.android.play.games",action="disable")],"rev")
   self.assertFalse(any(c[0]=="pm" or c[:3]==("cmd","appops","set") for c in m.calls))

 def test_protected_and_injection(self):
  with tempfile.TemporaryDirectory() as d:
   m=Fake(d)
   for p in ['android','com.google.android.katniss','com.x;reboot','com.unknown']:
    with self.assertRaises(ValueError):m.execute([dict(package=p,action='disable')],'rev')
   self.assertEqual(m.calls,[])
 def test_input_packages_are_protected_even_with_stale_inventory(self):
  with tempfile.TemporaryDirectory() as d:
   m=Fake(d)
   for p in ['com.smartdevice.livetv','com.mediatek.tvinput','com.mediatek.tvinputservice.arbitratorservice','com.mediatek.hotkey.dispatcher','com.mediatek.tv.agent','com.android.providers.tv']:
    m.items[p]=dict(protected=False,system=True)
    for action in ['disable','background','uninstall']:
     with self.assertRaises(ValueError):m.execute([dict(package=p,action=action)],'rev')
   self.assertEqual(m.calls,[])
 def test_roundtrip_preserves_default_and_disabled(self):
  with tempfile.TemporaryDirectory() as d:
   m=Fake(d);m.execute([dict(package=p,action='disable')for p in m.states],'rev');self.assertEqual(list(m.states.values()),[3,3]);m.restore();self.assertEqual(list(m.states.values()),[0,3])
 def test_shared_uid(self):
  with tempfile.TemporaryDirectory() as d:
   m=Fake(d);m.uid=1000
   with self.assertRaises(ValueError):m.execute([dict(package='com.google.android.play.games',action='disable')],'rev')
   self.assertFalse(any(x[0]=='pm' for x in m.calls))
 def test_revision_and_duplicate(self):
  with tempfile.TemporaryDirectory() as d:
   m=Fake(d);c=dict(package='com.google.android.play.games',action='disable')
   with self.assertRaises(ValueError):m.execute([c],'stale')
   with self.assertRaises(ValueError):m.execute([c,c],'rev')
 def test_background_restore(self):
  with tempfile.TemporaryDirectory() as d:
   m=Fake(d);m.execute([dict(package='com.google.android.play.games',action='background')],'rev');self.assertEqual(m.ops['com.google.android.play.games'],'ignore');m.restore();self.assertEqual(m.ops['com.google.android.play.games'],'default')
if __name__=='__main__':unittest.main()
