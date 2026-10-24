"""Run real Place/Prefs/WorldPrayers with in-memory Android storage, no TV mutation."""
import pathlib,tempfile,subprocess,os
root=pathlib.Path(__file__).resolve().parents[1]
jar=next((pathlib.Path.home()/'.gradle/caches/modules-2/files-2.1/com.batoulapps.adhan/adhan/1.2.1').rglob('*.jar'))
jsonjar=pathlib.Path(os.environ.get('JSON_JAR','/tmp/klight-test-json.jar'))
jdk=pathlib.Path(os.environ.get('JAVA_HOME','/opt/homebrew/opt/openjdk@17'))/'bin'
stubs={
'android/content/SharedPreferences.java':'''package android.content;import java.util.*;public class SharedPreferences{Map<String,String> data=new HashMap<>();public String getString(String k,String d){return data.getOrDefault(k,d);}public boolean contains(String k){return data.containsKey(k);}public Editor edit(){return new Editor();}public class Editor{public Editor putString(String k,String v){data.put(k,v);return this;}public Editor putBoolean(String k,boolean v){return putString(k,String.valueOf(v));}public void apply(){}}}''',
'android/content/Context.java':'''package android.content;public class Context{public static final int MODE_PRIVATE=0;java.util.Map<String,SharedPreferences> prefs=new java.util.HashMap<>();public SharedPreferences getSharedPreferences(String n,int mode){return prefs.computeIfAbsent(n,k->new SharedPreferences());}public Context getApplicationContext(){return this;}public String getPackageName(){return "kw.urtv.screensaver";}public ContentResolver getContentResolver(){return new ContentResolver();}}''',
'android/content/ContentResolver.java':'''package android.content;public class ContentResolver{public android.database.Cursor query(android.net.Uri u,Object a,Object b,Object c,Object d){return null;}}''',
'android/database/Cursor.java':'''package android.database;public interface Cursor extends AutoCloseable{boolean moveToFirst();String getString(int i);int getColumnIndexOrThrow(String s);void close();}''',
'android/net/Uri.java':'''package android.net;public class Uri{public static String encode(String s){return s;}}''',
'kw/urtv/shared/DisplayOptions.java':'''package kw.urtv.shared;public class DisplayOptions{public static final android.net.Uri URI=new android.net.Uri();}''',
'kw/urtv/shared/CalendarData.java':'''package kw.urtv.shared;import java.time.*;public class CalendarData{static class Day{int[] times;}Day get(LocalDate d){return null;}RegionSchedule.Result resolve(LocalDate d,double a,double o,int[] c,int[] t){return RegionSchedule.resolve(a,o,null,t,c);}}''',
'kw/urtv/screensaver/PrayerAlerts.java':'''package kw.urtv.screensaver;class PrayerAlerts{static void schedule(android.content.Context c){}}'''
}
with tempfile.TemporaryDirectory() as d:
 p=pathlib.Path(d)
 for name,source in stubs.items():
  f=p/name;f.parent.mkdir(parents=True,exist_ok=True);f.write_text(source)
 sources=list(p.rglob('*.java'))+[root/'tests/java/prayer/Regression.java']+[root/f'android/shared/src/main/java/kw/urtv/shared/{n}.java' for n in ['Place','ContentPolicy','RegionCatalog','RegionSchedule','PrayerLimits','PrayerCorrections','WorldPrayers']]+[root/f'android/app/src/main/java/kw/urtv/screensaver/{n}.java' for n in ['Prefs','PrayerAlertPlan']]
 cp=str(jar)+':'+str(jsonjar)
 subprocess.run([str(jdk/'javac'),'-cp',cp,'-d',d,*map(str,sources)],check=True)
 subprocess.run([str(jdk/'java'),'-cp',d+':'+cp,'kw.urtv.screensaver.Regression',str(root/'tests/fixtures/saudi-method4.json')],check=True)
