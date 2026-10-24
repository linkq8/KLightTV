package kw.urtv.screensaver;
import kw.urtv.shared.*;import java.time.*;import java.time.chrono.*;import java.util.*;
public class Regression {
 static void ok(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] args)throws Exception{
  android.content.Context ctx=new android.content.Context();android.content.SharedPreferences sp=ctx.getSharedPreferences("kuwait_settings",0);
  sp.edit().putString("cityName","جدة").putString("cityLat","21.4858").putString("cityLon","39.1925").putString("adjust:21.4858:39.1925","-2,0,0,0,0,18").apply();
  Prefs prefs=new Prefs(ctx);Place j=prefs.place();ok(j.country.equals("SA")&&j.method.equals("UMM_AL_QURA"),"legacy Jeddah migrated to Saudi Umm al Qura");ok(Arrays.equals(prefs.adjustments(),new int[6]),"legacy compensation not stacked");ok(prefs.place().json().equals(j.json()),"migration stable on reread");
  int[] correction={-30,-20,-10,10,20,30};prefs.setAdjustments(correction);ok(Arrays.equals(new Prefs(ctx).adjustments(),correction),"corrections retained across Prefs restart");
  prefs.setPlace(j.withMethod("MUSLIM_WORLD_LEAGUE",true));ok(Arrays.equals(prefs.adjustments(),new int[6]),"new method does not inherit corrections");ok(prefs.place().manualMethod,"manual provenance persisted");prefs.setPlace(j);ok(Arrays.equals(prefs.adjustments(),correction),"return to same context restores its own corrections");
  Place hanafi=new Place(j.country,j.name,j.lat,j.lon,j.zone,j.method,true);prefs.setPlace(hanafi);ok(Arrays.equals(prefs.adjustments(),new int[6]),"Asr setting isolates correction context");
  for(String raw:new String[]{"31,0,0,0,0,0","-31,0,0,0,0,0","0,0","0,0,0,0,0,0,0"})try{PrayerCorrections.parse(raw);throw new AssertionError("bad correction accepted");}catch(IllegalArgumentException good){}
  ok(Arrays.equals(PrayerCorrections.parse(PrayerCorrections.encode(correction)),correction),"backup round trip ±30");
  Place manual=new Place("SA","جدة",j.lat,j.lon,j.zone,"KUWAIT",false,true,false);prefs.setPlace(manual);ok(prefs.place().method.equals("KUWAIT")&&prefs.place().manualMethod,"explicit manual choice never overridden");
  prefs.setPlace(Place.region(0));ok(Arrays.equals(prefs.adjustments(),new int[6]),"different city resets active correction");
  android.content.Context oldJson=new android.content.Context();org.json.JSONObject legacyJson=new org.json.JSONObject(new Place("KW","جدة",j.lat,j.lon,"Asia/Kuwait","KUWAIT",false).json());legacyJson.remove("manualMethod");oldJson.getSharedPreferences("kuwait_settings",0).edit().putString("place",legacyJson.toString()).apply();ok(Place.local(oldJson).country.equals("SA")&&Place.local(oldJson).method.equals("UMM_AL_QURA"),"old serialized forced Kuwait migrated too");
  android.content.Context unknown=new android.content.Context();unknown.getSharedPreferences("kuwait_settings",0).edit().putString("cityName","موقع قديم").putString("cityLat","1").putString("cityLon","1").apply();Place u=Place.local(unknown);ok(u.needsConfirmation,"unknown country needs reapproval");ok(WorldPrayers.resolve(new CalendarData(),LocalDate.of(2026,10,24),u,new int[6]).times==null,"unknown location does not invent Kuwait prayers");
  Place[] places={j,new Place("SA","مكة",21.4225,39.8262,"Asia/Riyadh","UMM_AL_QURA",false),new Place("SA","الرياض",24.7136,46.6753,"Asia/Riyadh","UMM_AL_QURA",false),Place.kuwait()};
  int cases=0;
  for(Place p:places)for(LocalDate date:new LocalDate[]{LocalDate.of(2026,3,20),LocalDate.of(2026,6,21),LocalDate.of(2026,9,22),LocalDate.of(2026,12,21),LocalDate.of(2027,2,18)}){
   int[] t=WorldPrayers.calculate(date,p);for(int i=1;i<6;i++)ok(t[i]>t[i-1],"seasonal order");
   if(p.country.equals("SA"))ok(Math.abs(t[5]-t[4]-(WorldPrayers.ramadan(date)?120:90))<=1,"Umm al Qura isha interval");
   int[] shifted=WorldPrayers.resolve(new CalendarData(),date,p,correction).times;for(int i=0;i<6;i++)ok(shifted[i]==t[i]+correction[i],"correction applied once");
   ZonedDateTime now=date.atTime(0,1).atZone(p.zoneId());int[] tomorrow=WorldPrayers.calculate(date.plusDays(1),p);
   for(int lead:new int[]{0,5,10}){PrayerAlertPlan.Event event=PrayerAlertPlan.next(now,lead,t,tomorrow);ok(event.time==t[0],"alert uses exact same Fajr as both UI schedules");ok(Instant.ofEpochMilli(event.at).atZone(p.zoneId()).toLocalTime().equals(LocalTime.of(t[0]/60,t[0]%60).minusMinutes(lead)),"alert lead");}
   System.out.println(p.name+","+date+","+Arrays.toString(t));cases++;
  }
  for(int year:new int[]{1447,1448}){
   LocalDate first=LocalDate.from(HijrahDate.of(year,9,1)),last=LocalDate.from(HijrahDate.of(year,10,1)).minusDays(1);
   ok(!WorldPrayers.ramadan(first.minusDays(1))&&WorldPrayers.ramadan(first)&&WorldPrayers.ramadan(last)&&!WorldPrayers.ramadan(last.plusDays(1)),"HijrahDate Ramadan boundaries");
   for(LocalDate day:new LocalDate[]{first.minusDays(1),first,last,last.plusDays(1)})for(Place p:places)if(p.country.equals("SA")){int[] t=WorldPrayers.calculate(day,p);ok(Math.abs(t[5]-t[4]-(WorldPrayers.ramadan(day)?120:90))<=1,"boundary Isha");}
  }
  com.batoulapps.adhan.CalculationParameters params=com.batoulapps.adhan.CalculationMethod.UMM_AL_QURA.getParameters();ok(params.fajrAngle==18.5&&params.ishaInterval==90,"Adhan upstream parameters");
  Place ref=new Place("US","Raleigh",35.7750,-78.6336,"America/New_York","NORTH_AMERICA",true);ok(Arrays.equals(WorldPrayers.calculate(LocalDate.of(2015,7,12),ref),new int[]{282,368,801,1102,1232,1317}),"upstream Adhan published regression fixture");
  org.json.JSONArray refs=new org.json.JSONObject(java.nio.file.Files.readString(java.nio.file.Path.of(args[0]))).getJSONArray("cases");int maxDelta=0;
  for(int n=0;n<refs.length();n++){org.json.JSONObject refCase=refs.getJSONObject(n);String[] date=refCase.getString("date").split("-");Place cityRef=new Place("SA",refCase.getString("city"),refCase.getDouble("lat"),refCase.getDouble("lon"),"Asia/Riyadh","UMM_AL_QURA",false);LocalDate d=LocalDate.of(Integer.parseInt(date[2]),Integer.parseInt(date[1]),Integer.parseInt(date[0]));int[] a=WorldPrayers.calculate(d,cityRef);String[] names={"Fajr","Sunrise","Dhuhr","Asr","Maghrib","Isha"};for(int i=0;i<6;i++){String[] t=refCase.getJSONObject("times").getString(names[i]).substring(0,5).split(":");int expected=Integer.parseInt(t[0])*60+Integer.parseInt(t[1]);int delta=Math.abs(a[i]-expected);maxDelta=Math.max(maxDelta,delta);ok(delta<=2,"AlAdhan reference "+cityRef.name+" "+d+" "+names[i]+" delta "+delta);}}
  System.out.println("Reference cross-check: 9 Saudi city/date cases, max delta "+maxDelta+" min (API, not official timetable)");
  int[] jt=WorldPrayers.calculate(LocalDate.of(2026,10,22),j),mt=WorldPrayers.calculate(LocalDate.of(2026,10,22),places[1]);ok(!Arrays.equals(jt,mt),"Saudi cities use their own coordinates, not Makkah clone");
  System.out.println("PASS migration, unknown location, manual provenance, context profiles, ±30, "+cases+" city-season cases, 2 Ramadan boundaries, alerts and upstream reference");
 }
}
