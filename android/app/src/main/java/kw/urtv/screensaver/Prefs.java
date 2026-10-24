package kw.urtv.screensaver;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
final class Prefs {
    static final String TZ = "Asia/Kuwait";
    static final String[][] PANELS = {{"prayers", "مواقيت الصلاة اليوم"}, {"forecast", "طقس الأيام القادمة"}, {"none", "لا شيء"}};
    private final SharedPreferences sp; private final Context context;
    Prefs(Context c) { context=c.getApplicationContext(); sp = c.getSharedPreferences("kuwait_settings", Context.MODE_PRIVATE); }
    kw.urtv.shared.Place place(){return kw.urtv.shared.Place.local(context);}
    void setPlace(kw.urtv.shared.Place p){setPlace(p,null);}
    void setPlace(kw.urtv.shared.Place p,int[] corrections){String encoded=corrections==null?null:kw.urtv.shared.PrayerCorrections.encode(corrections);SharedPreferences.Editor edit=sp.edit().putString("place",p.json()).putString("cityName",p.name).putString("cityLat",String.valueOf(p.lat)).putString("cityLon",String.valueOf(p.lon));if(encoded!=null)edit.putString(kw.urtv.shared.PrayerCorrections.key(p),encoded);edit.apply();PrayerAlerts.schedule(context);}
    void applyQuickSetup(kw.urtv.shared.Place p,kw.urtv.shared.DisplayOptions display){SharedPreferences.Editor edit=sp.edit().putString("display_options_v2",display.json());if(p!=null)edit.putString("place",p.json()).putString("cityName",p.name).putString("cityLat",String.valueOf(p.lat)).putString("cityLon",String.valueOf(p.lon));if(!edit.commit())throw new IllegalStateException("Settings could not be saved");PrayerAlerts.schedule(context);}
    String[] city() { kw.urtv.shared.Place p=place();return new String[]{p.name,String.valueOf(p.lat),String.valueOf(p.lon)}; }
    void setCity(String name,String lat,String lon){setPlace(kw.urtv.shared.Place.legacy(name,Double.parseDouble(lat),Double.parseDouble(lon)));}
    int[] adjustments(){try{return kw.urtv.shared.PrayerCorrections.parse(sp.getString(kw.urtv.shared.PrayerCorrections.key(place()),"0,0,0,0,0,0"));}catch(Exception invalid){return new int[6];}}
    void setAdjustments(int[] a){sp.edit().putString(kw.urtv.shared.PrayerCorrections.key(place()),kw.urtv.shared.PrayerCorrections.encode(a)).apply();PrayerAlerts.schedule(context);}
    static boolean isPanel(String v) { for (String[] p : PANELS) if (p[0].equals(v)) return true; return false; }
    String panel() { String v = sp.getString("panel", "prayers"); return isPanel(v) ? v : "prayers"; }
    void setPanel(String v) { sp.edit().putString("panel", v).apply(); }
    String homePanel() { String v = sp.getString("homePanel", "prayers"); return isPanel(v) ? v : "none"; }
    void setHomePanel(String v) { sp.edit().putString("homePanel", v).apply(); }
    static String panelLabel(String v) { for (String[] p : PANELS) if(p[0].equals(v)) return p[1]; return PANELS[0][1]; }
    String fragment() { String[] city = city(); return "city=" + Uri.encode(city[0]) + "&lat=" + city[1] + "&lon=" + city[2] + "&tz=" + place().zone + "&panel=" + panel()+"&adjust="+android.net.Uri.encode(java.util.Arrays.toString(adjustments()).replaceAll("[\\[\\] ]", "")); }
}
