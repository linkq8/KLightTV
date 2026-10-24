package kw.urtv.home;
import android.content.*;import android.database.Cursor;import android.net.Uri;import android.os.ParcelFileDescriptor;import java.io.*;
/** A private, read-only URI for the single validated installation artifact. */
public class UpdateFiles extends ContentProvider {
 public boolean onCreate(){return true;}
 public String getType(Uri u){return "application/vnd.android.package-archive";}
 public ParcelFileDescriptor openFile(Uri u,String mode)throws FileNotFoundException{if(!"r".equals(mode)||!"/verified.apk".equals(u.getPath()))throw new FileNotFoundException();return ParcelFileDescriptor.open(new File(getContext().getCacheDir(),"verified.apk"),ParcelFileDescriptor.MODE_READ_ONLY);}
 public Cursor query(Uri u,String[] p,String s,String[] a,String o){return null;}public int delete(Uri u,String s,String[] a){throw new UnsupportedOperationException();}public int update(Uri u,ContentValues v,String s,String[] a){throw new UnsupportedOperationException();}public Uri insert(Uri u,ContentValues v){throw new UnsupportedOperationException();}
}
