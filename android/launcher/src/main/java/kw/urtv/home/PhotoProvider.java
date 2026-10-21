package kw.urtv.home;
import android.content.*;
import android.database.*;
import android.net.Uri;
/** Signature-protected read-only wallpaper choice, shared with our screensaver. */
public class PhotoProvider extends ContentProvider {
 public boolean onCreate(){return true;}
 public Cursor query(Uri uri,String[] projection,String selection,String[] args,String sort){MatrixCursor c=new MatrixCursor(new String[]{"pinned","file"});String file="";try{String[] files=getContext().getAssets().list("wallpapers");java.util.Arrays.sort(files);if(files.length>0)file=files[Math.floorMod(Dock.prefs(getContext()).getInt("wallpaper",0),files.length)];}catch(Exception ignored){}c.addRow(new Object[]{Dock.prefs(getContext()).getBoolean("wallpaper_pinned",false)?1:0,file});return c;}
 public String getType(Uri u){return null;}public Uri insert(Uri u,ContentValues v){throw new UnsupportedOperationException();}public int delete(Uri u,String s,String[] a){throw new UnsupportedOperationException();}public int update(Uri u,ContentValues v,String s,String[] a){throw new UnsupportedOperationException();}
}
