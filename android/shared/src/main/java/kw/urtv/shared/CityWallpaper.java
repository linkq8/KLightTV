package kw.urtv.shared;
import android.content.Context;
import android.graphics.*;
import android.net.Uri;
import org.json.*;
import java.io.*;
/** Curated geographic matches only. No country-wide or unrelated-city photo fallback. */
public final class CityWallpaper {
 public static String identity(Place p){return p.country+":"+p.lat+":"+p.lon+":"+p.name;}
 public static JSONObject entry(Context c,Place p){
  try(InputStream in=c.getAssets().open("city-wallpapers.json")){ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] buf=new byte[4096];int n;while((n=in.read(buf))!=-1)b.write(buf,0,n);JSONArray a=new JSONArray(b.toString("UTF-8"));
   for(int i=0;i<a.length();i++){JSONObject e=a.getJSONObject(i);JSONArray names=e.getJSONArray("aliases");String[] aliases=new String[names.length()];for(int j=0;j<aliases.length;j++)aliases[j]=names.getString(j);if(CityPhotoMatch.nameMatches(p.name,aliases)&&matches(p.country,p.lat,p.lon,e))return e;}
  }catch(Exception ignored){}return null;
 }
 public static boolean matches(String country,double lat,double lon,JSONObject e)throws JSONException{
  return CityPhotoMatch.matches(country,lat,lon,e.getString("country"),e.getDouble("lat"),e.getDouble("lon"),e.getDouble("radiusKm"));
 }
 public static Uri uri(Place p){return Uri.parse("content://kw.urtv.home.photos/current").buildUpon().appendQueryParameter("city",identity(p)).build();}
 /** Off-main-thread only; provider owns the single shared on-disk copy. */
 public static Bitmap decode(Context c,Place p,int edge)throws IOException{
  edge=Math.min(edge,Math.max(c.getResources().getDisplayMetrics().widthPixels,c.getResources().getDisplayMetrics().heightPixels));
  Uri u=uri(p);BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;
  try(InputStream in=c.getContentResolver().openInputStream(u)){BitmapFactory.decodeStream(in,null,o);}
  if(o.outWidth<=0||o.outHeight<=0)return null;
  int sample=1;while(Math.max(o.outWidth,o.outHeight)/(sample*2)>=edge)sample*=2;
  o.inJustDecodeBounds=false;o.inSampleSize=sample;o.inPreferredConfig=Bitmap.Config.RGB_565;
  Bitmap bm;try(InputStream in=c.getContentResolver().openInputStream(u)){bm=BitmapFactory.decodeStream(in,null,o);}
  if(bm==null)return null;float factor=Math.min(1f,edge/(float)Math.max(bm.getWidth(),bm.getHeight()));
  if(factor==1)return bm;Bitmap scaled=Bitmap.createScaledBitmap(bm,Math.max(1,Math.round(bm.getWidth()*factor)),Math.max(1,Math.round(bm.getHeight()*factor)),true);if(scaled!=bm)bm.recycle();return scaled;
 }
 private CityWallpaper(){}
}
