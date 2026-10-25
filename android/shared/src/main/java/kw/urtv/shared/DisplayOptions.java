package kw.urtv.shared;

import android.app.ActivityManager;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import org.json.JSONObject;
import java.util.Locale;

/** Same-signature configuration shared through the screensaver; local fallback if absent. */
public final class DisplayOptions {
    public int reading=0,prayer=0,next=0,dim=0;public boolean saverMatches=true,prayersEnabled=true,weatherEnabled=true;public Place weatherPlace;public String setupLanguage="ar",setupCountry="";public boolean heavy=true,contrast=false,latin=false,hour24=false;
    public String performance="auto",theme="dark",surface="scenic",focusColor="blue";
    public static volatile boolean memoryPressure;
    public static final Uri URI=Uri.parse("content://kw.urtv.screensaver.config");
    public static DisplayOptions decode(String s){DisplayOptions o=new DisplayOptions();try{JSONObject j=new JSONObject(s);o.prayersEnabled=j.optBoolean("prayersEnabled",true);o.weatherEnabled=j.optBoolean("weatherEnabled",true);o.setupLanguage="en".equals(j.optString("setupLanguage"))?"en":"ar";String country=j.optString("setupCountry","");o.setupCountry=ContentPolicy.country(country)?country:"";if(j.has("weatherPlace")&&!j.isNull("weatherPlace"))try{o.weatherPlace=Place.parse(j.getJSONObject("weatherPlace").toString());}catch(Exception ignored){}o.saverMatches=j.optBoolean("saverMatches",true);String fc=j.optString("focusColor","blue");o.focusColor=fc.equals("white")||fc.equals("black")?fc:"blue";o.surface="focus".equals(j.optString("surface"))?"focus":"scenic";String theme=j.optString("theme","dark");if(theme.equals("light")||theme.equals("auto"))o.theme=theme;o.reading=range(j.optInt("reading"),0,2);o.prayer=range(j.optInt("prayer"),0,2);o.next=range(j.optInt("next"),0,2);o.dim=range(j.optInt("dim"),0,2);o.heavy=j.optBoolean("heavy",true);o.contrast=j.optBoolean("contrast");o.latin=j.optBoolean("latin");o.hour24=j.optBoolean("hour24");String p=j.optString("performance","auto");if(p.equals("normal")||p.equals("lite"))o.performance=p;}catch(Exception ignored){}return o;}
    private static int range(int n,int a,int b){return Math.max(a,Math.min(b,n));}
    public String json(){try{return new JSONObject().put("prayersEnabled",prayersEnabled).put("weatherEnabled",weatherEnabled).put("weatherPlace",weatherPlace==null?JSONObject.NULL:new JSONObject(weatherPlace.json())).put("setupLanguage",setupLanguage).put("setupCountry",setupCountry).put("saverMatches",saverMatches).put("focusColor",focusColor).put("surface",surface).put("theme",theme).put("reading",reading).put("prayer",prayer).put("next",next).put("dim",dim).put("heavy",heavy).put("contrast",contrast).put("latin",latin).put("hour24",hour24).put("performance",performance).toString();}catch(Exception e){return "{}";}}
    public static DisplayOptions local(Context c){
        android.content.SharedPreferences prefs=c.getSharedPreferences("kuwait_settings",0);
        String raw=prefs.getString("display_options_v2",null);if(raw!=null)return decode(raw);
        raw=c.getSharedPreferences("klight_display",0).getString("options",null);if(raw!=null)return decode(raw);
        DisplayOptions defaults=new DisplayOptions();boolean legacy=prefs.contains("place")||prefs.contains("cityLat");
        try{android.content.pm.PackageInfo info=c.getPackageManager().getPackageInfo(c.getPackageName(),0);legacy|=info.lastUpdateTime-info.firstInstallTime>1000;}catch(Exception ignored){}
        // Preserve pre-feature installs. Fresh installs opt in to each optional content type.
        defaults.prayersEnabled=legacy;defaults.weatherEnabled=legacy;
        prefs.edit().putString("display_options_v2",defaults.json()).apply();return defaults;
    }
    public static DisplayOptions load(Context c){if(!c.getPackageName().equals("kw.urtv.screensaver"))try(Cursor cur=c.getContentResolver().query(URI,null,null,null,null)){if(cur!=null&&cur.moveToFirst()){int i=cur.getColumnIndex("display");if(i>=0)return decode(cur.getString(i));}}catch(Exception ignored){}return local(c);}
    public void save(Context c){c.getSharedPreferences(c.getPackageName().equals("kw.urtv.screensaver")?"kuwait_settings":"klight_display",0).edit().putString(c.getPackageName().equals("kw.urtv.screensaver")?"display_options_v2":"options",json()).apply();if(!c.getPackageName().equals("kw.urtv.screensaver")){ContentValues v=new ContentValues();v.put("display",json());try{c.getContentResolver().update(URI,v,null,null);}catch(Exception ignored){}}}
    public java.time.ZoneId clockZone(Context c){if(prayersEnabled)return Place.load(c).zoneId();if(weatherEnabled&&weatherPlace!=null)return weatherPlace.zoneId();return java.time.ZoneId.systemDefault();}
    public Place weatherLocation(Context c){return weatherPlace==null?Place.load(c):weatherPlace;}
    public String focusColorName(){return focusColor.equals("white")?"أبيض":focusColor.equals("black")?"أسود":"أزرق";}
    public void cycleFocusColor(){focusColor=focusColor.equals("white")?"black":focusColor.equals("black")?"blue":"white";}
    public boolean showsWeather(){return weatherEnabled&&!focused();}
    public boolean showsMatches(){return !focused();}
    public boolean focused(){return "focus".equals(surface);}
    public boolean usesLatin(){return focused()||latin;}
    public String numbers(String s){StringBuilder b=new StringBuilder();for(char ch:s.toCharArray()){char a=ch>='٠'&&ch<='٩'?(char)('0'+ch-'٠'):ch;b.append(!usesLatin()&&a>='0'&&a<='9'?(char)('٠'+a-'0'):a);}return b.toString();}
    public String time(int m){int h=m/60;return numbers((hour24?String.format(Locale.ROOT,"%02d",h):""+(h%12==0?12:h%12))+":"+String.format(Locale.ROOT,"%02d",m%60))+(hour24?"":" "+(h<12?"ص":"م"));}
    public float textScale(){return 1f+reading*.12f;}
    public float prayerScale(){return 1f+Math.max(reading,prayer)*.12f;}
    public float nextScale(){return 1f+Math.max(reading,next)*.12f;}
    public int overlayAlpha(){return dim*24+(contrast?28:0);}
    public static final class Device {
        public final long ram;public final int heap,edge;public final boolean low,lite;public final String reason;
        Device(long r,int h,boolean l,boolean use,int e,String why){ram=r;heap=h;low=l;lite=use;edge=e;reason=why;}
    }
    public Device device(Context c){ActivityManager a=c.getSystemService(ActivityManager.class);ActivityManager.MemoryInfo m=new ActivityManager.MemoryInfo();a.getMemoryInfo(m);int h=a.getMemoryClass();boolean l=a.isLowRamDevice(),use=HardwarePolicy.useLite(performance,l,m.totalMem,h,memoryPressure);String why=memoryPressure?"ضغط ذاكرة":performance.equals("auto")?(use?"ذاكرة الجهاز أو حد التطبيق محدود":"ذاكرة مناسبة للعرض العادي"):performance.equals("normal")&&h<96?"حد ذاكرة التطبيق يتطلب الوضع الخفيف":"اختيارك اليدوي";return new Device(m.totalMem,h,l,use,memoryPressure?1280:HardwarePolicy.imageEdge(use,h),why);}
    public String size(int n){return new String[]{"عادي","كبير","كبير جدًا"}[n];}
}
