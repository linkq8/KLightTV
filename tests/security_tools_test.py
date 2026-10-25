#!/usr/bin/env python3
"""Regression: signing helper must never log credentials or overwrite keys/symlinks."""
import ast, base64, contextlib, io, os, pathlib, secrets, shutil, stat, subprocess, sys, tempfile, unittest
from unittest.mock import patch
ROOT=pathlib.Path(__file__).resolve().parents[1]
# Load only the helper's real main function, without invoking crypto generation or GitHub.
tree=ast.parse((ROOT/'.github/tools/make_signing_key.py').read_text())
main=next(n for n in tree.body if isinstance(n,ast.FunctionDef) and n.name=='main')
space=dict(Path=pathlib.Path,os=os,sys=sys,base64=base64,secrets=secrets,subprocess=subprocess,REPO='linkq8/KLightTV',gh_ready=lambda:None,make=lambda _:b'test-keystore-bytes')
exec(compile(ast.Module(body=[main],type_ignores=[]),'signing-helper','exec'),space)
class SigningSecurity(unittest.TestCase):
 def run_main(self,home):
  output=io.StringIO()
  with patch.object(pathlib.Path,'home',return_value=home),patch.object(secrets,'token_urlsafe',return_value='sentinel-private-password'),contextlib.redirect_stdout(output):space['main']()
  return output.getvalue()
 def test_no_secret_output_private_files_and_no_overwrite(self):
  with tempfile.TemporaryDirectory() as directory:
   home=pathlib.Path(directory);text=self.run_main(home)
   self.assertNotIn('sentinel-private-password',text);self.assertNotIn(base64.b64encode(b'test-keystore-bytes').decode(),text)
   for name in ['urtv-signing-key.p12','urtv-signing-key-password.txt','urtv-keystore-base64.txt']:
    self.assertEqual(stat.S_IMODE((home/name).stat().st_mode),0o600)
   with self.assertRaises(SystemExit):self.run_main(home)
   self.assertEqual((home/'urtv-signing-key.p12').read_bytes(),b'test-keystore-bytes')
 def test_refuse_dangling_symlink(self):
  with tempfile.TemporaryDirectory() as directory:
   home=pathlib.Path(directory);target=home/'outside';(home/'urtv-signing-key.p12').symlink_to(target)
   with self.assertRaises(FileExistsError):self.run_main(home)
   self.assertFalse(target.exists())
if __name__=='__main__':unittest.main()
