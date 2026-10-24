import pathlib,subprocess,tempfile
root=pathlib.Path(__file__).resolve().parents[1]
jdk=pathlib.Path('/opt/homebrew/opt/openjdk@17/bin')
with tempfile.TemporaryDirectory() as d:
 p=pathlib.Path(d)/'Test.java'
 p.write_text('import java.time.*;import kw.urtv.shared.MatchVisibility;\npublic class Test {\n static Instant now=Instant.parse("2026-10-23T12:00:00Z");\n static void check(String state,String status,long offset,boolean expected){if(MatchVisibility.visible(state,status,now.plusSeconds(offset),now)!=expected)throw new AssertionError(state+" "+status+" "+offset);}\n public static void main(String[] a){check("pre","",259200,true);check("pre","",259201,false);check("pre","",0,true);check("pre","",-3601,false);check("post","",-86400,true);check("post","",-86401,false);check("post","",1,false);check("in","",-7200,true);check("in","",-21601,false);check("pre","STATUS_POSTPONED",3600,false);check("pre","STATUS_CANCELED",3600,false);check("in","STATUS_SUSPENDED",-3600,false);check("unknown","",0,false);System.out.println("PASS 13 boundary, stale and status cases");}}\n')
 subprocess.run([str(jdk/'javac'),'-d',d,str(p),str(root/'android/shared/src/main/java/kw/urtv/shared/MatchVisibility.java')],check=True)
 subprocess.run([str(jdk/'java'),'-cp',d,'Test'],check=True)
