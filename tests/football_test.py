"""Production normalization and geographic exclusion, no Android or network required.
Set JSON_JAR to an org.json Java implementation (Maven org.json:json:20240303).
"""
import os,pathlib,tempfile,subprocess
r=pathlib.Path(__file__).resolve().parents[1]
jar=pathlib.Path(os.environ.get('JSON_JAR','/tmp/klight-test-json.jar'))
assert jar.exists(),'Set JSON_JAR to org.json jar'
jdk=pathlib.Path('/opt/homebrew/opt/openjdk@17/bin')
with tempfile.TemporaryDirectory() as tmp:
 p=pathlib.Path(tmp)
 (p/'Test.java').write_text('''package kw.urtv.home;
import kw.urtv.shared.ContentPolicy;import org.json.*;import java.time.*;import java.nio.file.*;
public class Test{static void ok(boolean b,String s){if(!b)throw new AssertionError(s);}public static void main(String[] args)throws Exception{
 ok(!ContentPolicy.country("IL")&&ContentPolicy.country("KW")&&ContentPolicy.country("PS"),"country filter");
 ok(!ContentPolicy.text("إِسْرَائِيل")&&!ContentPolicy.text("Israël")&&!ContentPolicy.team("461","neutral"),"normalized aliases and ID");
 JSONObject recent=new JSONObject(Files.readString(Path.of(args[0]))),next=new JSONObject(Files.readString(Path.of(args[1])));Instant now=Instant.parse("2026-10-22T12:00:00Z");JSONArray rows=FootballResults.merge(recent,next,now);ok(rows.length()>1&&rows.length()<=6,"bounded results and fixtures");
 JSONObject e=new JSONObject(recent.getJSONArray("events").getJSONObject(0).toString());e.put("date","2026-10-21T19:00:00Z");JSONObject c=e.getJSONArray("competitions").getJSONObject(0);JSONObject side=c.getJSONArray("competitors").getJSONObject(0);side.remove("score");JSONObject one=new JSONObject().put("events",new JSONArray().put(e));JSONArray missing=FootballResults.merge(one,one,now);ok(missing.length()==1,"dedup");ok(missing.getJSONObject(0).getString("homeScore").equals("—"),"no invented zero");side.getJSONObject("team").put("id","524");ok(FootballResults.merge(one,new JSONObject(),now).length()==0,"entire excluded match removed");
 JSONObject stale=rows.getJSONObject(0);stale.put("awayId","461");ok(!FootballResults.allowed(stale),"cache filtered");ok(FootballResults.merge(new JSONObject(),new JSONObject(),now).length()==0,"empty feed");System.out.println("PASS country aliases, national/club IDs, cached rows, whole-match exclusion, dedup, missing score, bounded feed");}}
''')
 subprocess.run([str(jdk/'javac'),'-cp',str(jar),'-d',tmp,str(p/'Test.java'),str(r/'android/shared/src/main/java/kw/urtv/shared/ContentPolicy.java'),str(r/'android/launcher/src/main/java/kw/urtv/home/FootballResults.java')],check=True)
 subprocess.run([str(jdk/'java'),'-cp',tmp+':'+str(jar),'kw.urtv.home.Test',str(r/'tests/fixtures/football/recent.json'),str(r/'tests/fixtures/football/upcoming.json')],check=True)
