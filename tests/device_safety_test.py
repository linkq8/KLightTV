import sys,pathlib,tempfile,time,unittest,copy
from unittest.mock import patch
ROOT=pathlib.Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'tools'));import device_safety as s
class Device:
 def __init__(self):self.fingerprint='maker/build:34';self.calls=[];self.extra=False
 def shell(self,*a):
  self.calls.append(a)
  if a[0]=='getprop':return {'ro.product.manufacturer':'Maker','ro.product.model':'TV 1','ro.product.device':'tv1','ro.build.fingerprint':self.fingerprint,'ro.build.version.release':'14','ro.build.version.sdk':'34','ro.product.cpu.abilist':'arm64-v8a'}.get(a[1],'')
  if a[:3]==('settings','get','secure'):return {'android_id':'device-one','default_input_method':'vendor.keyboard/.Ime','voice_interaction_service':'vendor.voice/.Voice'}.get(a[3],'null')
  if a[:3]==('pm','list','features'):return 'feature:android.software.leanback'
  if a[:3]==('pm','list','packages'):
   names=['com.example.optional','vendor.input','vendor.keyboard','vendor.voice','vendor.home']+(['com.new.app'] if self.extra else [])
   return '\n'.join('package:'+p+(' uid:'+str(10001+i) if '-U' in a else '') for i,p in enumerate(names))
  if a[:2]==('dumpsys','tv_input'):return 'Service: vendor.input/.Input'
  if a[:2]==('dumpsys','package'):return 'User 0: installed=true enabled=0'
  if a[:3]==('cmd','package','resolve-activity'):return 'vendor.home/.Home'
  if a[:2]==('cat','/proc/meminfo'):return 'MemTotal: 2097152 kB'
  if a==('wm','size'):return 'Physical size: 1920x1080'
  raise AssertionError('Unexpected or mutating command '+str(a))
def reviewed(audit,op):
 r=s.template(audit,[op]);r.update(reviewed_at=time.time(),reviewer='test reviewer',findings='Reviewed exact test model firmware and dependencies.',baseline_checks={k:True for k in s.FEATURES},sources=[dict(url='https://'+host+'/evidence',model=audit['identity']['model'],firmware=audit['identity']['fingerprint'],finding='Fixture evidence for compatibility and limitations.') for host in ('maker.example','community.example')]);r['actions'][0].update(rationale='This optional package has no core dependencies.',rollback='Restore the original package state from recorded backup.',dependencies_checked=True);return r
class Tests(unittest.TestCase):
 def setUp(self):self.device=Device();self.audit=s.inspect(self.device.shell,'test-device');self.op=dict(kind='package',package='com.example.optional',action='disable');self.review=reviewed(self.audit,self.op)
 def test_valid_and_required_research(self):
  s.validate(self.audit,self.review,[self.op])
  for key,value in [('sources',[]),('findings',''),('baseline_checks',{}),('reviewer',''),('reviewed_at',0),('audit_id','different'),('actions',[])]:
   r=copy.deepcopy(self.review);r[key]=value
   with self.assertRaises(ValueError):s.validate(self.audit,r,[self.op])
 def test_expiry_wrong_model_and_plan(self):
  with self.assertRaises(ValueError):s.validate(self.audit,self.review,[self.op],now=time.time()+3601)
  r=copy.deepcopy(self.review);r['identity']['model']='Other'
  with self.assertRaises(ValueError):s.validate(self.audit,r,[self.op])
  with self.assertRaises(ValueError):s.validate(self.audit,self.review,[dict(self.op,action='uninstall')])
 def test_core_roles_shared_uid_and_remote_are_blocked(self):
  for package in ['vendor.input','vendor.voice','vendor.keyboard','vendor.home','com.smartdevice.livetv','com.android.providers.tv','vendor.hdmi','vendor.remote']:
   op=dict(self.op,package=package)
   with self.assertRaises(ValueError):s.operation_allowed(op,self.audit)
  a=copy.deepcopy(self.audit);a['uids']['vendor.input']=a['uids']['com.example.optional']
  with self.assertRaises(ValueError):s.operation_allowed(self.op,a)
  with self.assertRaises(ValueError):s.operation_allowed({'kind':'remote-remap'},self.audit)
 def test_unknown_core_state_is_not_assumed_safe(self):
  audit=copy.deepcopy(self.audit);audit['core_states']['vendor.input']='unknown';audit['id']=s.digest({k:v for k,v in audit.items() if k!='id'})
  with self.assertRaises(ValueError):s.validate(audit,reviewed(audit,self.op),[self.op])
 def test_repair_requires_original_removed_system_host_and_honest_baseline(self):
  audit=copy.deepcopy(self.audit);audit['identity'].update(manufacturer='skyworth',model='SWTV-20AE')
  evidence=dict(package='com.smartdevice.livetv',code_path='/system/product/app/OceanusLiveTv',installed=False,enabled=0,system=True,tv_handler=True)
  audit['tv_recovery']=evidence;audit['id']=s.digest({k:v for k,v in audit.items() if k!='id'})
  op=dict(kind='restore-tv-package',package=evidence['package'],evidence_sha256=s.digest(evidence))
  review=reviewed(audit,op);review['baseline_checks']={}
  with self.assertRaises(ValueError):s.validate(audit,review,[op])
  review['baseline_observations']={k:dict(status='failed' if k=='hdmi' else 'not-tested',evidence='User confirmed HDMI failure; other functions remain untested.') for k in s.FEATURES}
  s.validate(audit,review,[op])
  for key,value in [('installed',True),('enabled',3),('system',False),('tv_handler',False),('code_path','/data/app/replacement')]:
   a=copy.deepcopy(audit);a['tv_recovery'][key]=value
   with self.assertRaises(ValueError):s.operation_allowed(dict(op,evidence_sha256=s.digest(a['tv_recovery'])),a)
  with self.assertRaises(ValueError):s.validate(audit,review,[op,self.op])
  with self.assertRaises(ValueError):s.operation_allowed(dict(op,action='uninstall'),audit)
 def test_recovery_allows_only_original_host_service_addition(self):
  old=['vendor.input/.Hdmi'];new=old+['com.smartdevice.livetv/skyworth.platformsupport.componentSupport.AndroidTvSupportTvinputService']
  repair=[dict(kind='restore-tv-package')]
  s.verify_input_services(old,new,repair)
  for before,after,actions in [(old,new,[self.op]),(old,[],repair),(old,new+['unknown/.Input'],repair)]:
   with self.assertRaises(ValueError):s.verify_input_services(before,after,actions)
 def test_live_recheck_journal_and_verification(self):
  with tempfile.TemporaryDirectory() as d:
   root=pathlib.Path(d);a=root/'audit';r=root/'review';s.write(a,self.audit);s.write(r,self.review)
   with self.assertRaises(ValueError):s.gate(self.device.shell,'test-device',None,None,[self.op],store=root/'journal')
   self.device.extra=True
   with self.assertRaises(ValueError):s.gate(self.device.shell,'test-device',a,r,[self.op],store=root/'journal')
   self.device.extra=False;record=s.gate(self.device.shell,'test-device',a,r,[self.op],store=root/'journal');self.assertFalse(s.read(record)['verified'])
   with self.assertRaises(ValueError):s.gate(self.device.shell,'test-device',a,r,[self.op],store=root/'journal')
   with self.assertRaises(ValueError):s.verify(self.device.shell,'test-device',record,['hdmi'])
   s.verify(self.device.shell,'test-device',record,s.FEATURES);self.assertTrue(s.read(record)['verified'])
 def test_legacy_mutations_and_injection_denied(self):
  for command in ['pm disable-user com.x','pm uninstall com.x','settings put secure x 1','getprop; reboot','getprop $(reboot)','getprop | sh','input keyevent 3','am start -n com.x/.Y','cmd appops set com.x OP ignore','wm size 640x480','dumpsys battery unplug','dumpsys deviceidle force-idle']:
   self.assertFalse(s.readonly_command(command),command)
  for command in ['getprop ro.product.model','dumpsys tv_input','pm list packages -U','settings get secure android_id','wm size']:self.assertTrue(s.readonly_command(command),command)
 def test_legacy_entrypoints_exit_before_actions(self):
  import subprocess
  for name in ['run','install','disable','tweaks','keep','reset','restore','deadman','saver','dock','type','start-saver']:
   result=subprocess.run(['bash',str(ROOT/'optimize'/f'{name}.sh')],capture_output=True,text=True);self.assertEqual(result.returncode,2,name);self.assertIn('blocked',result.stderr)
if __name__=='__main__':unittest.main()
