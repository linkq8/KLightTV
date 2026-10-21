package kw.urtv.screensaver;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
public class SaverReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent i) {
        Prefs p = new Prefs(c);
        String a = String.valueOf(i.getAction());
        if (a.endsWith(".GET")) { setResultData(describe(p)); return; }
        if (a.endsWith(".CITIES")) {
            StringBuilder b = new StringBuilder();
            for (Cities.Region r : Cities.REGIONS) for (String[] x : r.cities) b.append(x[0]).append("\n");
            setResultData(b.toString()); return;
        }
        if (!a.endsWith(".SET")) { setResultData("error: unknown action"); return; }
        String name = i.getStringExtra("city"), lat = i.getStringExtra("lat"), lon = i.getStringExtra("lon");
        String panel = i.getStringExtra("panel"), home = i.getStringExtra("home_panel");
        if ((panel != null && !Prefs.isPanel(panel)) || (home != null && !Prefs.isPanel(home))) { setResultData("error: panels: prayers forecast none"); return; }
        if (name != null) {
            if (lat == null && lon == null) for(Cities.Region r : Cities.REGIONS) for(String[] x : r.cities) if(x[0].equals(name)) { lat=x[1]; lon=x[2]; }
            try {
                double la=Double.parseDouble(lat), lo=Double.parseDouble(lon);
                if (Double.isNaN(la) || Double.isInfinite(la) || Double.isNaN(lo) || Double.isInfinite(lo) || Math.abs(la)>90 || Math.abs(lo)>180 || name.trim().isEmpty()) throw new IllegalArgumentException();
            } catch(Exception e) { setResultData("error: supply a listed city or valid name, latitude and longitude"); return; }
        } else if(lat != null || lon != null) { setResultData("error: city name required"); return; }
        if(name != null) p.setCity(name, lat, lon);
        if(panel != null) p.setPanel(panel);
        if(home != null) p.setHomePanel(home);
        setResultData("saved\n" + describe(p));
    }
    private static String describe(Prefs p) { String[] x=p.city(); return "city: " + x[0] + " (" + x[1] + ", " + x[2] + ")\npanel: " + p.panel() + "\nhome_panel: " + p.homePanel(); }
}
