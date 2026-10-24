package kw.urtv.shared;
import android.content.*;import android.database.Cursor;import org.json.*;import java.time.*;
/** Only the selected city is stored. No global database or background location tracking. */
public final class Place {
 public final String country,name,zone,method;public final double lat,lon;public final boolean hanafi,manualMethod,needsConfirmation;
 public Place(String c,String n,double a,double o,String z,String m,boolean h){this(c,n,a,o,z,m,h,false,false);}
 public Place(String c,String n,double a,double o,String z,String m,boolean h,boolean manual,boolean confirm){if(!ContentPolicy.country(c)||!ContentPolicy.text(n)||!ContentPolicy.text(z)||n==null||n.trim().isEmpty()||n.length()>160||!Double.isFinite(a)||!Double.isFinite(o)||Math.abs(a)>90||Math.abs(o)>180)throw new IllegalArgumentException("Invalid city");ZoneId.of(z);if(!java.util.Arrays.asList(METHODS).contains(m))throw new IllegalArgumentException("Invalid method");country=c;name=n;lat=a;lon=o;zone=z;method=m;hanafi=h;manualMethod=manual;needsConfirmation=confirm;}
 public static final String[] METHODS={"KUWAIT","UMM_AL_QURA","EGYPTIAN","MUSLIM_WORLD_LEAGUE","KARACHI","NORTH_AMERICA","DUBAI","QATAR","SINGAPORE"};
 public static final String[] LABELS={"الكويت","أم القرى","الهيئة المصرية للمساحة","رابطة العالم الإسلامي","كراتشي","أمريكا الشمالية","دبي","قطر","سنغافورة"};
 public static final String[] EN_LABELS={"Kuwait","Umm al-Qura","Egyptian Survey Authority","Muslim World League","Karachi","North America","Dubai","Qatar","Singapore"};
 public String label(boolean english){return english?EN_LABELS[java.util.Arrays.asList(METHODS).indexOf(method)]:label();}
 public String label(){return LABELS[java.util.Arrays.asList(METHODS).indexOf(method)];}
 public static String methodFor(String c){switch(c){case "KW":return "KUWAIT";case "SA":return "UMM_AL_QURA";case "EG":return "EGYPTIAN";case "AE":return "DUBAI";case "QA":return "QATAR";case "PK":case "BD":return "KARACHI";case "US":case "CA":return "NORTH_AMERICA";case "SG":return "SINGAPORE";default:return "MUSLIM_WORLD_LEAGUE";}}
 public static Place kuwait(){return region(0);}
 public static Place region(int i){RegionCatalog.Region r=RegionCatalog.ALL[i];return new Place("KW",r.name,r.lat,r.lon,"Asia/Kuwait","KUWAIT",false);}
 public ZoneId zoneId(){return ZoneId.of(zone);}
 public String json(){try{return new JSONObject().put("country",country).put("name",name).put("lat",lat).put("lon",lon).put("zone",zone).put("method",method).put("hanafi",hanafi).put("manualMethod",manualMethod).put("needsConfirmation",needsConfirmation).toString();}catch(Exception e){throw new IllegalStateException(e);}}
 public static Place parse(String s){try{JSONObject j=new JSONObject(s);return new Place(j.getString("country"),j.getString("name"),j.getDouble("lat"),j.getDouble("lon"),j.getString("zone"),j.getString("method"),j.optBoolean("hanafi"),j.optBoolean("manualMethod",true),j.optBoolean("needsConfirmation"));}catch(Exception e){throw new IllegalArgumentException("Invalid location",e);}}
 /** Legacy country inference is limited to known city anchors, never a country's bounding rectangle. */
 public static Place legacy(String name,double lat,double lon){
  for(int i=0;i<RegionCatalog.ALL.length;i++){RegionCatalog.Region r=RegionCatalog.ALL[i];if(distance(lat,lon,r.lat,r.lon)<5)return new Place("KW",name,lat,lon,"Asia/Kuwait","KUWAIT",false);}
  double[][] sa={{21.4858,39.1925},{21.4225,39.8262},{24.7136,46.6753}};
  for(double[] a:sa)if(distance(lat,lon,a[0],a[1])<25)return new Place("SA",name,lat,lon,"Asia/Riyadh","UMM_AL_QURA",false);
  return new Place("ZZ",name,lat,lon,"UTC","MUSLIM_WORLD_LEAGUE",false,false,true);
 }
 private static double distance(double a,double o,double b,double p){double x=Math.toRadians(b-a),y=Math.toRadians(p-o);double h=Math.sin(x/2)*Math.sin(x/2)+Math.cos(Math.toRadians(a))*Math.cos(Math.toRadians(b))*Math.sin(y/2)*Math.sin(y/2);return 6371*2*Math.atan2(Math.sqrt(h),Math.sqrt(Math.max(0,1-h)));}
 public static Place local(Context c){
  android.content.SharedPreferences prefs=c.getSharedPreferences("kuwait_settings",0);String raw=prefs.getString("place","");
  if(!raw.isEmpty())try{
   JSONObject json=new JSONObject(raw);Place p=parse(raw);
   if(!json.has("manualMethod")&&p.country.equals("KW")&&p.method.equals("KUWAIT")){
    Place inferred=legacy(p.name,p.lat,p.lon);
    if(!inferred.country.equals("KW")){prefs.edit().putString("place",inferred.json()).apply();return inferred;}
   }
   return p;
  }catch(Exception invalid){prefs.edit().putBoolean("locationNeedsConfirmation",true).apply();return new Place("ZZ","أعد اختيار المدينة",0,0,"UTC","MUSLIM_WORLD_LEAGUE",false,false,true);}
  if(!prefs.contains("cityLat")&&!prefs.contains("cityLon"))return kuwait();
  try{Place p=legacy(prefs.getString("cityName","مدينتك"),Double.parseDouble(prefs.getString("cityLat","")),Double.parseDouble(prefs.getString("cityLon","")));prefs.edit().putString("place",p.json()).apply();return p;}
  catch(Exception invalid){return new Place("ZZ","أعد اختيار المدينة",0,0,"UTC","MUSLIM_WORLD_LEAGUE",false,false,true);}
 }
 public Place withMethod(String m,boolean manual){return new Place(country,name,lat,lon,zone,m,hanafi,manual,needsConfirmation);}
 public static Place load(Context c){if(c.getPackageName().equals("kw.urtv.screensaver"))return local(c);try(Cursor q=c.getContentResolver().query(DisplayOptions.URI,null,null,null,null)){if(q!=null&&q.moveToFirst())return parse(q.getString(q.getColumnIndexOrThrow("place")));}catch(Exception ignored){}return kuwait();}
 public boolean officialEligible(){return country.equals("KW")&&method.equals("KUWAIT")&&!hanafi&&RegionCatalog.usesCityRecord(lat,lon);}
}
