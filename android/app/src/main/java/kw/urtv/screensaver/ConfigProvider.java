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
        MatrixCursor c = new MatrixCursor(new String[] {"fragment", "home_panel", "display"});
        c.addRow(new Object[] {p.fragment(), p.homePanel(), kw.urtv.shared.DisplayOptions.local(getContext()).json()});
        return c;
    }

    @Override public String getType(Uri uri) { return null; }
    @Override public Uri insert(Uri uri, ContentValues v) { return null; }
    @Override public int delete(Uri uri, String s, String[] a) { return 0; }
    @Override public int update(Uri uri, ContentValues v, String s, String[] a) { if(v==null||!v.containsKey("display"))return 0; kw.urtv.shared.DisplayOptions.decode(v.getAsString("display")).save(getContext());getContext().getContentResolver().notifyChange(uri,null);return 1; }
}
