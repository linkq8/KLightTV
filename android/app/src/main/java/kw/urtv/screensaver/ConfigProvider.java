package kw.urtv.screensaver;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

/**
 * Signature-protected settings: hands the launcher (same signing key, see READ_CONFIG in the manifest) what its home-screen
 * card needs: the page settings as a URL fragment and which card to show ("none" = off).
 */
public class ConfigProvider extends ContentProvider {
    @Override public boolean onCreate() { return true; }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] args, String sort) {
        Prefs p = new Prefs(getContext());
        MatrixCursor c = new MatrixCursor(new String[] {"fragment", "home_panel", "display", "place", "location_configured"});
        c.addRow(new Object[] {p.fragment(), p.homePanel(), kw.urtv.shared.DisplayOptions.local(getContext()).json(), p.place().json(), getContext().getSharedPreferences("kuwait_settings",0).contains("place")||getContext().getSharedPreferences("kuwait_settings",0).contains("cityLat")?1:0});
        return c;
    }

    @Override public String getType(Uri uri) { return null; }
    @Override public Uri insert(Uri uri, ContentValues v) { return null; }
    @Override public int delete(Uri uri, String s, String[] a) { return 0; }
    @Override public int update(Uri uri, ContentValues v, String s, String[] a) { if(v==null)return 0;if(v.containsKey("quickSetup")){try{org.json.JSONObject payload=new org.json.JSONObject(v.getAsString("quickSetup"));org.json.JSONObject options=payload.getJSONObject("display");if(!(options.get("prayersEnabled") instanceof Boolean)||!(options.get("weatherEnabled") instanceof Boolean))return 0;kw.urtv.shared.DisplayOptions display=kw.urtv.shared.DisplayOptions.decode(options.toString());kw.urtv.shared.Place place=payload.has("place")?kw.urtv.shared.Place.parse(payload.getJSONObject("place").toString()):null;if(place!=null&&place.needsConfirmation)return 0;if(options.has("weatherPlace")&&!options.isNull("weatherPlace"))kw.urtv.shared.Place.parse(options.getJSONObject("weatherPlace").toString());if(display.weatherEnabled&&(display.weatherPlace==null||display.weatherPlace.needsConfirmation))return 0;if(display.prayersEnabled&&(place==null?new Prefs(getContext()).place():place).needsConfirmation)return 0;new Prefs(getContext()).applyQuickSetup(place,display);getContext().getContentResolver().notifyChange(uri,null);return 1;}catch(Exception e){return 0;}} if(v.containsKey("place")){try{kw.urtv.shared.Place place=kw.urtv.shared.Place.parse(v.getAsString("place"));int[] corrections=v.containsKey("corrections")?kw.urtv.shared.PrayerCorrections.parse(v.getAsString("corrections")):null;Prefs prefs=new Prefs(getContext());prefs.setPlace(place,corrections);getContext().getContentResolver().notifyChange(uri,null);return 1;}catch(Exception e){return 0;}} if(v.containsKey("region")){Integer index=v.getAsInteger("region");if(index==null||index<0||index>=kw.urtv.shared.RegionCatalog.ALL.length)return 0;kw.urtv.shared.RegionCatalog.Region r=kw.urtv.shared.RegionCatalog.ALL[index];new Prefs(getContext()).setCity(r.name,String.valueOf(r.lat),String.valueOf(r.lon));getContext().getContentResolver().notifyChange(uri,null);return 1;} if(!v.containsKey("display"))return 0; kw.urtv.shared.DisplayOptions.decode(v.getAsString("display")).save(getContext());getContext().getContentResolver().notifyChange(uri,null);return 1; }
}
