package kw.urtv.home;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.Toast;
import kw.urtv.shared.*;
import org.json.*;
import java.util.*;
import java.util.concurrent.*;
import java.net.*;
import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;

/** On-demand football: bounded payloads, one worker, no alarm/service/live polling. */
public final class FootballActivity extends TvPage {
 private static final String BASE="https://site.api.espn.com/apis/site/v2/sports/soccer/";
 private static final String[][] LEAGUES={
  {"eng.1","الدوري الإنجليزي"},{"esp.1","الدوري الإسباني"},{"ita.1","الدوري الإيطالي"},
  {"ger.1","الدوري الألماني"},{"fra.1","الدوري الفرنسي"},{"ksa.1","الدوري السعودي"},
  {"fifa.worldq.afc","منتخبات آسيا"},{"fifa.worldq.caf","منتخبات أفريقيا"},
  {"fifa.worldq.uefa","منتخبات أوروبا"},{"fifa.worldq.conmebol","منتخبات أمريكا الجنوبية"},
  {"fifa.worldq.concacaf","منتخبات أمريكا الشمالية"},{"fifa.worldq.ofc","منتخبات أوقيانوسيا"}};
 private final ExecutorService worker=Executors.newSingleThreadExecutor();
 private Future<?> task;private volatile HttpURLConnection connection;private int generation;private boolean active;
 private android.content.SharedPreferences prefs;private JSONArray favorites;private String message="";
 @Override public void onCreate(Bundle b){super.onCreate(b);prefs=getSharedPreferences("football",0);favorites=clean(read(prefs.getString("favorites","[]")));save();purgeCache();render();}
 @Override protected void onResume(){super.onResume();active=true;}
 @Override protected void onStop(){active=false;cancel();super.onStop();}
 @Override protected void onDestroy(){cancel();worker.shutdownNow();super.onDestroy();}
 private static JSONArray read(String raw){try{return new JSONArray(raw);}catch(Exception e){return new JSONArray();}}
 static JSONArray clean(JSONArray input){JSONArray out=new JSONArray();Set<String> ids=new HashSet<>();for(int i=0;i<input.length()&&out.length()<6;i++){JSONObject t=input.optJSONObject(i);if(t!=null&&ContentPolicy.team(t.optString("id"),t.toString())&&ids.add(t.optString("id")))out.put(t);}return out;}
 private void save(){prefs.edit().putString("favorites",favorites.toString()).apply();}
 private void purgeCache(){Set<String> ids=new HashSet<>();for(int i=0;i<favorites.length();i++)ids.add(favorites.optJSONObject(i).optString("id"));android.content.SharedPreferences.Editor editor=prefs.edit();for(String key:prefs.getAll().keySet()){if(key.startsWith("cache:")){String id=key.substring(6);if(!ids.contains(id)){editor.remove(key).remove("updated:"+id);continue;}JSONArray rows=read(prefs.getString(key,"[]")),safe=new JSONArray();for(int i=0;i<rows.length();i++)if(FootballResults.allowed(rows.optJSONObject(i)))safe.put(rows.optJSONObject(i));editor.putString(key,safe.toString());}}editor.apply();}
 private boolean enabled(){return prefs.getBoolean("enabled",false);}
 private void render(){title("كرة القدم","أنديتك ومنتخباتك · الجداول والنتائج عند الطلب");
  addButton(enabled()?"المتابعة مفعّلة · إيقاف":"تفعيل متابعة كرة القدم",()->{cancel();prefs.edit().putBoolean("enabled",!enabled()).apply();message="";render();});
  if(enabled()){
   addButton("إضافة نادٍ أو منتخب · "+favorites.length()+" / ٦",this::chooseLeague);
   for(int i=0;i<favorites.length();i++){JSONObject t=favorites.optJSONObject(i);final int index=i;addButton("✓  "+TeamNames.arabic(this,t.optString("name"))+"  · إزالة من المفضلة",()->{cancel();String id=t.optString("id");favorites.remove(index);save();prefs.edit().remove("cache:"+id).remove("updated:"+id).apply();render();});}
   if(favorites.length()>0)addButton("تحديث المباريات والنتائج",this::refresh);
   if(!message.isEmpty())note(message);
   for(int i=0;i<favorites.length();i++)showCached(favorites.optJSONObject(i));
   if(favorites.length()==0)note("اختر الفرق التي تهمك لعرض مبارياتها هنا.");
   note("البيانات: ESPN · قد تتأخر النتائج أو لا تتوفر بعض المسابقات. لا يوجد تحديث مستمر في الخلفية.");
  }
  addButton("رجوع",this::finish);
 }
 private void showCached(JSONObject t){String id=t.optString("id");JSONArray rows=read(prefs.getString("cache:"+id,"[]"));note(TeamNames.arabic(this,t.optString("name")));long updated=prefs.getLong("updated:"+id,0);
  if(updated==0){note("اضغط تحديث لعرض المباريات.");return;}
  note("آخر تحديث · "+format(Instant.ofEpochMilli(updated))+(System.currentTimeMillis()-updated>900000?" · بيانات محفوظة":""));
  int shown=0;for(int i=0;i<rows.length();i++){JSONObject e=rows.optJSONObject(i);if(!FootballResults.allowed(e))continue;android.widget.TextView card=button(localizedLabel(e),()->{});card.setClickable(false);card.setTextSize(19);android.widget.LinearLayout.LayoutParams layout=new android.widget.LinearLayout.LayoutParams(-1,-2);layout.bottomMargin=dp(8);list.addView(card,layout);shown++;}
  if(shown==0)note("لا توجد مباريات متاحة لهذا الفريق.");
 }
 private String format(Instant instant){return DateTimeFormatter.ofPattern("d MMM · HH:mm",new Locale("ar")).withZone(Place.load(this).zoneId()).format(instant);}
 private void chooseLeague(){if(favorites.length()>=6){Toast.makeText(this,"الحد الأقصى ستة فرق",Toast.LENGTH_SHORT).show();return;}String[] labels=new String[LEAGUES.length];for(int i=0;i<labels.length;i++)labels[i]=LEAGUES[i][1];new AlertDialog.Builder(this).setTitle("الأندية والمنتخبات").setItems(labels,(d,i)->catalog(LEAGUES[i][0])).show();}
 private void catalog(String league){cancel();final int token=generation;message="جارٍ تحميل قائمة الفرق…";render();task=worker.submit(()->{try{JSONObject root=get(league+"/teams?limit=200");JSONArray source=root.getJSONArray("sports").getJSONObject(0).getJSONArray("leagues").getJSONObject(0).getJSONArray("teams");ArrayList<JSONObject> teams=new ArrayList<>();for(int i=0;i<source.length();i++){JSONObject t=source.getJSONObject(i).getJSONObject("team");if(ContentPolicy.team(t.optString("id"),t.toString()))teams.add(new JSONObject().put("id",t.getString("id")).put("name",t.getString("displayName")));}teams.sort(Comparator.comparing(t->TeamNames.arabic(this,t.optString("name"))));runOnUiThread(()->{if(!current(token))return;message="";render();if(teams.isEmpty()){note("لا تتوفر قائمة لهذه المسابقة حاليًا.");return;}String[] names=new String[teams.size()];for(int i=0;i<names.length;i++)names[i]=TeamNames.arabic(this,teams.get(i).optString("name"));new AlertDialog.Builder(this).setTitle("اختر فريقًا").setItems(names,(d,i)->{favorites.put(teams.get(i));favorites=clean(favorites);save();render();}).show();});}catch(Exception e){error(token,"تعذر تحميل الفرق. حاول لاحقًا.");}});}
 private void refresh(){cancel();final int token=generation;final JSONArray selected=read(favorites.toString());message="جارٍ تحديث المباريات…";render();task=worker.submit(()->{int failures=0;for(int i=0;i<selected.length();i++){if(Thread.currentThread().isInterrupted())return;try{String id=selected.getJSONObject(i).getString("id");JSONObject recent=get("all/teams/"+id+"/schedule"),upcoming=get("all/teams/"+id+"/schedule?fixture=true");if(recent.optJSONArray("events")==null||upcoming.optJSONArray("events")==null)throw new IOException("Invalid schedule");JSONArray rows=FootballResults.merge(recent,upcoming,Instant.now());JSONArray verified=new JSONArray();for(int k=0;k<rows.length();k++){JSONObject row=rows.getJSONObject(k);if(verifyTeam(row.getString("homeId"))&&verifyTeam(row.getString("awayId")))verified.put(row);}rows=verified;synchronized(this){if(token!=generation)return;prefs.edit().putString("cache:"+id,rows.toString()).putLong("updated:"+id,System.currentTimeMillis()).apply();}}catch(Exception e){failures++;}}final int failed=failures;runOnUiThread(()->{if(!current(token))return;message=failed==0?"تم التحديث":"تعذر تحديث بعض الفرق؛ تظهر آخر بيانات محفوظة إن توفرت.";render();});});}
 private final Map<String,Boolean> verifiedTeams=new HashMap<>();
 private boolean verifyTeam(String id)throws Exception{if(verifiedTeams.containsKey(id))return verifiedTeams.get(id);JSONObject t=get("all/teams/"+id).getJSONObject("team");boolean allowed=id.equals(t.optString("id"))&&t.has("defaultLeague")&&ContentPolicy.team(id,t.toString());if(allowed)cacheLogo(id,t);verifiedTeams.put(id,allowed);return allowed;}
 private String localizedLabel(JSONObject e){try{JSONObject row=new JSONObject(e.toString());row.put("home",TeamNames.arabic(this,e.optString("home"))).put("away",TeamNames.arabic(this,e.optString("away")));return FootballResults.label(row,Place.load(this).zoneId());}catch(Exception ex){return "موعد غير متاح";}}
 private void cacheLogo(String id,JSONObject team){try{File dir=new File(getCacheDir(),"football-logos");dir.mkdirs();File file=new File(dir,id+".png");if(file.isFile()&&System.currentTimeMillis()-file.lastModified()<7*86400000L)return;JSONArray logos=team.optJSONArray("logos");if(logos==null||logos.length()==0)return;URL url=new URL(logos.getJSONObject(0).getString("href"));if(!url.getProtocol().equals("https")||!url.getHost().equals("a.espncdn.com"))return;HttpURLConnection c=(HttpURLConnection)url.openConnection();connection=c;c.setInstanceFollowRedirects(false);c.setConnectTimeout(4000);c.setReadTimeout(4000);byte[] bytes;try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1){if(Thread.currentThread().isInterrupted())return;out.write(b,0,n);if(out.size()>262144)return;}bytes=out.toByteArray();}finally{c.disconnect();if(connection==c)connection=null;}
 android.graphics.BitmapFactory.Options options=new android.graphics.BitmapFactory.Options();options.inJustDecodeBounds=true;android.graphics.BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);if(options.outWidth<1||options.outWidth>2048||options.outHeight>2048)return;options.inJustDecodeBounds=false;options.inSampleSize=Math.max(1,Math.max(options.outWidth,options.outHeight)/128);android.graphics.Bitmap bitmap=android.graphics.BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);if(bitmap==null)return;android.graphics.Bitmap scaled=android.graphics.Bitmap.createScaledBitmap(bitmap,Math.max(1,96*bitmap.getWidth()/Math.max(bitmap.getWidth(),bitmap.getHeight())),Math.max(1,96*bitmap.getHeight()/Math.max(bitmap.getWidth(),bitmap.getHeight())),true);try(FileOutputStream out=new FileOutputStream(file)){scaled.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}if(scaled!=bitmap)bitmap.recycle();scaled.recycle();File[] files=dir.listFiles();if(files!=null&&files.length>48){Arrays.sort(files,Comparator.comparingLong(File::lastModified));for(int i=0;i<files.length-48;i++)files[i].delete();}
 }catch(Exception ignored){}}
 private boolean current(int token){return active&&token==generation&&!isFinishing();}
 private void error(int token,String text){runOnUiThread(()->{if(current(token)){message=text;render();}});}
 private synchronized void cancel(){generation++;if(task!=null)task.cancel(true);HttpURLConnection c=connection;if(c!=null)c.disconnect();connection=null;}
 private JSONObject get(String path)throws Exception{if(Thread.currentThread().isInterrupted())throw new InterruptedException();HttpURLConnection c=(HttpURLConnection)new URL(BASE+path).openConnection();connection=c;c.setConnectTimeout(6000);c.setReadTimeout(6000);try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(Thread.currentThread().isInterrupted())throw new InterruptedException();out.write(b,0,n);if(out.size()>2097152)throw new IOException("Response too large");}return new JSONObject(out.toString("UTF-8"));}finally{c.disconnect();if(connection==c)connection=null;}}
}
