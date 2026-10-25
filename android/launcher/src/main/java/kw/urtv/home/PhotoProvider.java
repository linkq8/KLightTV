package kw.urtv.home;
import android.content.*;
import android.database.*;
import android.net.Uri;
/** Signature-protected read-only wallpaper choice, shared with our screensaver. */
public class PhotoProvider extends ContentProvider {
 public boolean onCreate(){return true;}
 public Cursor query(Uri uri,String[] projection,String selection,String[] args,String sort){MatrixCursor c=new MatrixCursor(new String[]{"pinned","file"});c.addRow(new Object[]{1,""});return c;}
 public android.os.ParcelFileDescriptor openFile(Uri uri,String mode)throws java.io.FileNotFoundException{
  if(!"r".equals(mode)||!"/current".equals(uri.getPath()))throw new java.io.FileNotFoundException();
  try{return android.os.ParcelFileDescriptor.open(CityPhotoStore.current(getContext(),uri.getQueryParameter("city")),android.os.ParcelFileDescriptor.MODE_READ_ONLY);}catch(java.io.IOException e){throw new java.io.FileNotFoundException(e.getMessage());}
 }
 public String getType(Uri u){return null;}public Uri insert(Uri u,ContentValues v){throw new UnsupportedOperationException();}public int delete(Uri u,String s,String[] a){throw new UnsupportedOperationException();}public int update(Uri u,ContentValues v,String s,String[] a){throw new UnsupportedOperationException();}
}
