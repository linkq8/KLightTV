#!/usr/bin/env python3
import unittest,pathlib,importlib.util,copy,sys,threading,urllib.request,urllib.error,json
root=pathlib.Path(__file__).resolve().parents[1];sys.path.insert(0,str(root/'tools/setup'))
spec=importlib.util.spec_from_file_location('setup_profile',root/'tools/setup/profile.py');p=importlib.util.module_from_spec(spec);spec.loader.exec_module(p)
class Tests(unittest.TestCase):
 def test_defaults_have_no_mutations(self):
  x=p.default_profile();plan=p.plan(x);self.assertEqual(plan['automatic_app_changes'],[]);self.assertTrue(plan['remote_unchanged']);self.assertFalse(plan['ready_to_apply']);self.assertEqual(x['prayer']['reminder'],-1)
 def test_global_location_does_not_use_kuwait_runtime(self):
  x=p.default_profile();x['location']={'country':'GB','city':'London','latitude':51.5,'longitude':-.12,'timezone':'Europe/London'};result=p.plan(x);self.assertFalse(result['ready_to_apply']);self.assertTrue(any('خارج الكويت' in v for v in result['pending']))
 def test_bad_inputs_rejected(self):
  for key,value in [('latitude',float('nan')),('longitude',999),('timezone','wrong/zone'),('city','bad\nname'),('country','KW;reboot'),('country','IL'),('city','إِسْرَائِيل'),('city','Israël')]:
   x=p.default_profile();x['location'][key]=value
   with self.assertRaises(ValueError):p.validate(x)
  x=p.default_profile();x['applications']=[{'package':'android;reboot','action':'remove'}]
  with self.assertRaises(ValueError):p.validate(x)
  x=p.default_profile();x['remote']['mappings']=[{'key':1,'package':'com.example.app'}]
  with self.assertRaises(ValueError):p.validate(x)
 def test_low_memory_even_when_normal_requested(self):
  x=p.default_profile();x['appearance']['performance']='normal';v=p.plan(x,{'ram_mib':4096,'heap_mib':64,'sdk':34});self.assertEqual(v['mode'],'lite');self.assertEqual(v['image_edge'],1280)
 def test_selected_removal_remains_plan_only(self):
  x=p.default_profile();x['applications']=[{'package':'com.example.app','action':'remove'}];v=p.plan(x);self.assertEqual(len(v['requested_app_changes']),1);self.assertEqual(v['automatic_app_changes'],[])
 def test_hardware_compatibility_is_explicit(self):
  v=p.plan(p.default_profile(),{'sdk':25,'television':False});self.assertEqual(len(v['blockers']),2)
if __name__=='__main__':unittest.main()
