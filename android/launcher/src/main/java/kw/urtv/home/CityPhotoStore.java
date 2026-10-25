package kw.urtv.home;
import android.content.Context;
import android.graphics.BitmapFactory;
import java.io.*;
import java.net.*;
import org.json.JSONObject;
import kw.urtv.shared.*;
/** One selected-city photo, shared by both apps. No service, scheduler or global image pack. */
final class CityPhotoStore {
 private static final long LIMIT=20L*1024*1024;
 private static String failed="";private static long retryAfter;
 static synchronized File current(Context c,String requested)throws IOException{
  Place p=Place.load(c);String identity=CityWallpaper.identity(p);
  if(!identity.equals(requested))throw new IOException("City changed");
  File dir=new File(c.getCacheDir(),"city-photo");if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("Cache unavailable");
  JSONObject e=CityWallpaper.entry(c,p);String name=e==null?"none":e.optString("id")+"-v1.jpg";
  File[] files=dir.listFiles();if(files!=null)for(File f:files)if(!f.getName().equals(name)&&!f.delete())throw new IOException("Cannot clear previous city");
  if(e==null||DisplayOptions.load(c).focused())throw new IOException("Neutral background");
  File target=new File(dir,name);if(target.isFile()&&valid(target))return target;
  if(target.exists()&&!target.delete())throw new IOException("Invalid cache cannot be cleared");
  if(identity.equals(failed)&&android.os.SystemClock.elapsedRealtime()<retryAfter)throw new IOException("Retry deferred");
  File pending=new File(dir,"download.part");HttpURLConnection connection=null;
  try{
   URL url=new URL(e.getString("url"));if(!url.getProtocol().equals("https")||!(url.getHost().equals("upload.wikimedia.org")||url.getHost().equals("thumb.wikimedia.org")||url.getHost().equals("raw.githubusercontent.com")))throw new IOException("Untrusted image host");
   connection=(HttpURLConnection)url.openConnection();connection.setInstanceFollowRedirects(false);connection.setConnectTimeout(12000);connection.setReadTimeout(15000);connection.setRequestProperty("User-Agent","KLightTV/2.0 (https://github.com/linkq8/KLightTV)");
   if(connection.getResponseCode()!=200||connection.getContentLengthLong()>LIMIT)throw new IOException("Image response rejected");
   String type=connection.getContentType();if(type==null||!type.toLowerCase(java.util.Locale.ROOT).startsWith("image/"))throw new IOException("Not an image");
   long total=0,deadline=android.os.SystemClock.elapsedRealtime()+60000;
   try(InputStream in=connection.getInputStream();FileOutputStream out=new FileOutputStream(pending)){
    byte[] b=new byte[32768];int n;while((n=in.read(b))!=-1){total+=n;if(total>LIMIT||Thread.currentThread().isInterrupted()||android.os.SystemClock.elapsedRealtime()>deadline||!identity.equals(CityWallpaper.identity(Place.load(c))))throw new IOException("Cancelled or oversized photo");out.write(b,0,n);}out.getFD().sync();
   }
   if(!valid(pending)||!identity.equals(CityWallpaper.identity(Place.load(c))))throw new IOException("Invalid or stale photo");
   if(!pending.renameTo(target))throw new IOException("Cache commit failed");failed="";return target;
  }catch(Exception error){failed=identity;retryAfter=android.os.SystemClock.elapsedRealtime()+300000;throw new IOException("City photo unavailable",error);}
  finally{if(connection!=null)connection.disconnect();if(pending.exists())pending.delete();}
 }
 private static boolean valid(File f){if(f.length()<1024||f.length()>LIMIT)return false;BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeFile(f.getAbsolutePath(),o);return o.outWidth>=1600&&o.outHeight>=900&&o.outWidth<=10000&&o.outHeight<=10000&&(long)o.outWidth*o.outHeight<=40000000;}
}
