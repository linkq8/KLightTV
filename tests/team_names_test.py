"""Run the production local name resolver with file-backed Android asset stubs."""
import pathlib,tempfile,subprocess,os
r=pathlib.Path(__file__).resolve().parents[1];jdk=pathlib.Path('/opt/homebrew/opt/openjdk@17/bin');jar=os.environ.get('JSON_JAR','/tmp/klight-test-json.jar')
with tempfile.TemporaryDirectory() as d:
 p=pathlib.Path(d)
 (p/'Context.java').write_text('package android.content; public class Context {public android.content.res.AssetManager getAssets(){return new android.content.res.AssetManager();}}')
 (p/'AssetManager.java').write_text('package android.content.res; public class AssetManager {public java.io.InputStream open(String path)throws Exception{return new java.io.FileInputStream(System.getProperty("assets")+"/"+path);}}')
 (p/'NamesTest.java').write_text('''import kw.urtv.shared.*;public class NamesTest {static void ok(boolean b){if(!b)throw new AssertionError();} public static void main(String[] args){android.content.Context c=new android.content.Context();ok(TeamNames.arabic(c,"Arsenal").equals("آرسنال"));ok(TeamNames.arabic(c,"Australia").equals("أستراليا"));ok(TeamNames.arabic(c,"Bayern Munich").equals("بايرن ميونخ"));ok(TeamNames.arabic(c,"France").equals("فرنسا"));ok(!TeamNames.arabic(c,"Unknown United").matches(".*[a-zA-Z].*"));ok(TeamNames.arabic(c,"Israel").isEmpty());System.out.println("PASS club dictionary, country labels, Arabic fallback, content exclusion");}}''')
 subprocess.run([str(jdk/'javac'),'-cp',jar,'-d',d,*map(str,p.glob('*.java')),str(r/'android/shared/src/main/java/kw/urtv/shared/TeamNames.java'),str(r/'android/shared/src/main/java/kw/urtv/shared/ContentPolicy.java')],check=True)
 subprocess.run([str(jdk/'java'),'-Dassets='+str(r/'android/shared/src/main/assets'),'-cp',d+':'+jar,'NamesTest'],check=True)
