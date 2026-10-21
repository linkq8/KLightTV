package kw.urtv.screensaver;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
final class Prefs {
    static final String TZ = "Asia/Kuwait";
    static final String[][] PANELS = {{"prayers", "مواقيت الصلاة اليوم"}, {"forecast", "طقس الأيام القادمة"}, {"none", "لا شيء"}};
    private final SharedPreferences sp;
    Prefs(Context c) { sp = c.getSharedPreferences("kuwait_settings", Context.MODE_PRIVATE); }
    String[] city() { return new String[]{sp.getString("cityName", "مدينة الكويت"), sp.getString("cityLat", "29.3759"), sp.getString("cityLon", "47.9774")}; }
    void setCity(String name, String lat, String lon) { sp.edit().putString("cityName", name).putString("cityLat", lat).putString("cityLon", lon).apply(); }
    static boolean isPanel(String v) { for (String[] p : PANELS) if (p[0].equals(v)) return true; return false; }
    String panel() { String v = sp.getString("panel", "prayers"); return isPanel(v) ? v : "prayers"; }
    void setPanel(String v) { sp.edit().putString("panel", v).apply(); }
    String homePanel() { String v = sp.getString("homePanel", "none"); return isPanel(v) ? v : "none"; }
    void setHomePanel(String v) { sp.edit().putString("homePanel", v).apply(); }
    static String panelLabel(String v) { for (String[] p : PANELS) if(p[0].equals(v)) return p[1]; return PANELS[0][1]; }
    String fragment() { String[] city = city(); return "city=" + Uri.encode(city[0]) + "&lat=" + city[1] + "&lon=" + city[2] + "&tz=" + TZ + "&panel=" + panel(); }
}
