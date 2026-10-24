package kw.urtv.home;
import org.json.*;import kw.urtv.shared.*;
/** Real unsaved setup model: mandatory single city, optional advanced controls, restore. */
public class QuickSetupRegression {
 static void ok(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] args)throws Exception{
  Place sa=new Place("SA","Riyadh",24.7136,46.6753,"Asia/Riyadh","UMM_AL_QURA",false,true,false);
  Place london=new Place("GB","London",51.5085,-.1257,"Europe/London","MUSLIM_WORLD_LEAGUE",false);
  String original=new JSONObject().put("prayersEnabled",false).put("weatherEnabled",true).put("theme","light").put("reading",2).put("futureOption","keep").toString();
  SetupDraft d=new SetupDraft(original,sa);d.prayerPlace=null;d.country="";
  try{d.payload();throw new AssertionError("missing country allowed");}catch(IllegalArgumentException expected){}
  d.country("SA");ok(!d.prayers&&d.weather,"country must not change content choices");
  try{d.payload();throw new AssertionError("country without city allowed");}catch(IllegalArgumentException expected){}
  d.city(sa);JSONObject oneCity=new JSONObject(d.payload());ok(oneCity.getJSONObject("display").getJSONObject("weatherPlace").getString("name").equals("Riyadh"),"single city shared without second weather question");ok(!d.separateWeather,"default one city");
  ok(oneCity.getJSONObject("display").getInt("reading")==2&&oneCity.getJSONObject("display").getString("futureOption").equals("keep"),"retain unrelated preferences");
  d.separateWeather=true;d.weatherPlace=london;d.prayers=true;d.language="en";d.advanced=true;d.step=1;d.surface="focus";
  SetupDraft restored=SetupDraft.restore(d.json());ok(restored.step==1&&restored.advanced&&restored.focused()&&restored.language.equals("en"),"restore draft review/style/language");ok(restored.payload().equals(d.payload()),"recreation does not lose draft");
  JSONObject saved=new JSONObject(restored.payload());ok(saved.getJSONObject("display").getBoolean("latin"),"simple style uses English numerals");ok(saved.getJSONObject("place").getString("method").equals("UMM_AL_QURA"),"manual method retained");ok(saved.getJSONObject("display").getJSONObject("weatherPlace").getString("zone").equals("Europe/London"),"advanced separate weather retained");
  restored.country("EG");try{restored.payload();throw new AssertionError("city from different country allowed");}catch(IllegalArgumentException expected){}
  restored.country("SA");restored.separateWeather=false;restored.city(sa);ok(restored.weatherPlace.json().equals(sa.json()),"returning to one city syncs both");restored.surface="scenic";ok(!new JSONObject(restored.payload()).getJSONObject("display").getBoolean("latin"),"photo style returns Arabic numerals");
  restored.prayers=false;ok(!new JSONObject(restored.payload()).getJSONObject("display").getBoolean("prayersEnabled"),"prayer remains optional in advanced settings");
  try{restored.country("INVALID");throw new AssertionError("invalid country");}catch(IllegalArgumentException expected){}
  System.out.println("Quick setup: required country/city, shared default city, advanced overrides, styles, retained preferences and draft restoration passed");
 }
}
