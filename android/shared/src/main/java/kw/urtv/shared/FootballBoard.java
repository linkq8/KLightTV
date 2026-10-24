package kw.urtv.shared;
import android.content.*;import android.database.Cursor;import android.graphics.*;import android.net.Uri;import android.os.*;import android.view.View;import org.json.*;import java.util.*;import java.util.concurrent.*;import java.time.*;import java.time.format.DateTimeFormatter;

/** Two quiet, shared match cards. Reads local IPC/cache only; no network or resident service. */
public final class FootballBoard {
 private static final Uri URI=Uri.parse("content://kw.urtv.home.football");
 private final Context context;private final Runnable changed;private final Handler main=new Handler(Looper.getMainLooper());private final ExecutorService worker=Executors.newSingleThreadExecutor();
 private final Paint paint=new Paint(3);private final Typeface face;private final DisplayOptions options;private final Palette palette;private final ZoneId zone;
 private JSONArray rows=new JSONArray();private Map<String,Bitmap> logos=new HashMap<>();private boolean closed,loading;private int page;
 public FootballBoard(Context c,Runnable update){context=c;changed=update;options=DisplayOptions.load(c);palette=new Palette(c,options);zone=Place.load(c).zoneId();face=Typeface.createFromAsset(c.getAssets(),"fonts/Thmanyah-Medium.otf");}
 public void refresh(){if(closed||loading)return;loading=true;worker.submit(()->{JSONArray safe=new JSONArray();Map<String,Bitmap> images=new HashMap<>();try(Cursor cursor=context.getContentResolver().query(URI,null,null,null,null)){if(cursor!=null&&cursor.moveToFirst()){JSONArray source=new JSONArray(cursor.getString(0));for(int i=0;i<Math.min(12,source.length());i++){JSONObject e=source.getJSONObject(i);if(!ContentPolicy.team(e.optString("homeId"),e.toString())||!ContentPolicy.team(e.optString("awayId"),e.toString()))continue;Instant when=Instant.parse(e.getString("date"));if(!MatchVisibility.visible(e.optString("state"),e.optString("status"),when,Instant.now()))continue;e.put("homeAr",TeamNames.arabic(context,e.optString("home"))).put("awayAr",TeamNames.arabic(context,e.optString("away")));safe.put(e);for(String key:new String[]{"homeId","awayId"}){String id=e.getString(key);if(images.containsKey(id))continue;try(java.io.InputStream in=context.getContentResolver().openInputStream(URI.buildUpon().appendPath(id).build())){Bitmap b=BitmapFactory.decodeStream(in);if(b!=null)images.put(id,b);}catch(Exception ignored){}}}}}catch(Exception ignored){}final JSONArray result=safe;main.post(()->{loading=false;if(closed){recycle(images);return;}recycle(logos);logos=images;rows=result;page=(int)(System.currentTimeMillis()/60000)%Math.max(1,(rows.length()+1)/2);changed.run();});});}
 private static void recycle(Map<String,Bitmap> values){for(Bitmap b:values.values())b.recycle();values.clear();}
 public void close(){closed=true;worker.shutdownNow();recycle(logos);}
 public boolean hasMatches(){return rows.length()>0;}
 public String description(){StringBuilder s=new StringBuilder();for(int i=0;i<rows.length();i++){JSONObject e=rows.optJSONObject(i);s.append(e.optString("homeAr")).append(" · ").append(e.optString("awayAr")).append(". ");}return s.toString();}
 public void draw(Canvas c,float x,float y,float width,float height){if(rows.length()==0)return;float scale=Math.min(width/720,height/138),cardWidth=(width/scale-16)/2;c.save();c.translate(x,y);c.scale(scale,scale);for(int slot=0;slot<Math.min(2,rows.length());slot++){int index=(page*2+slot)%rows.length();JSONObject e=rows.optJSONObject(index);c.save();c.translate(slot*(cardWidth+16),0);paint.setColor(palette.panel);c.drawRoundRect(new RectF(0,0,cardWidth,138),12,12,paint);card(c,e,cardWidth);c.restore();}c.restore();}
 private void card(Canvas c,JSONObject e,float width){boolean wide=width>500;float center=width/2,home=width*.80f,away=width*.20f;String state=e.optString("state"),status=e.optString("status");String tag=state.equals("post")?"انتهت":state.equals("in")?"جارية · آخر نتيجة":"قادمة";if(status.contains("POSTPONED"))tag="مؤجلة";if(status.contains("CANCEL"))tag="ملغاة";String date="";try{if(state.equals("pre")&&Instant.parse(e.getString("date")).isBefore(Instant.now()))tag="بانتظار التحديث";date=DateTimeFormatter.ofPattern("EEE d/M",new Locale("ar")).withZone(zone).format(Instant.parse(e.getString("date")));}catch(Exception ignored){}
 text(c,tag+" · "+options.numbers(date),center,24,wide?20:16,palette.accent,width-32);
 logo(c,e.optString("homeId"),home,63,wide?64:48);logo(c,e.optString("awayId"),away,63,wide?64:48);
 String value=e.optString("homeScore","—")+" — "+e.optString("awayScore","—");if(state.equals("pre")){try{value=e.optBoolean("timeValid",true)?DateTimeFormatter.ofPattern("HH:mm").withZone(zone).format(Instant.parse(e.getString("date"))):"لم يحدد";}catch(Exception ignored){value="—";}}
 if(state.equals("pre"))text(c,options.numbers(value),center,80,wide?40:28,palette.ink,width*.28f);else{ text(c,options.numbers(e.optString("homeScore","—")),center+32,80,wide?40:28,palette.ink,55);text(c,"—",center,80,24,palette.muted,26);text(c,options.numbers(e.optString("awayScore","—")),center-32,80,wide?40:28,palette.ink,55);}
 text(c,e.optString("homeAr"),home,116,wide?24:18,palette.ink,width*.34f);text(c,e.optString("awayAr"),away,116,wide?24:18,palette.ink,width*.34f);
 if(System.currentTimeMillis()-e.optLong("updated")>900000)text(c,"نتيجة محفوظة",center,134,13,palette.muted,width-32);
 }
 private void logo(Canvas c,String id,float x,float y,float size){
  Bitmap b=logos.get(id);
  if(b!=null&&!b.isRecycled()){
   paint.setColor(Color.WHITE);paint.setStyle(Paint.Style.FILL);
   float scale=size/Math.max(b.getWidth(),b.getHeight()),w=b.getWidth()*scale,h=b.getHeight()*scale;
   c.drawBitmap(b,null,new RectF(x-w/2,y-h/2,x+w/2,y+h/2),paint);
  }else{
   // Keep the team name below; no fabricated badge or circular placeholder.
   paint.setColor(palette.muted);paint.setStrokeWidth(2);c.drawLine(x-8,y,x+8,y,paint);
  }
 }

 private void text(Canvas c,String s,float x,float baseline,float size,int color,float max){paint.setTypeface(face);paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(size* Math.min(1.12f,options.textScale()));paint.setColor(color);if(paint.measureText(s)>max)paint.setTextSize(Math.max(size*.8f,paint.getTextSize()*max/paint.measureText(s)));if(paint.measureText(s)>max){while(s.length()>1&&paint.measureText(s+"…")>max)s=s.substring(0,s.length()-1);s+="…";}c.drawText(s,x,baseline,paint);}
 public static final class Surface extends View {
  private final FootballBoard board;
  public Surface(Context c){super(c);board=new FootballBoard(c,()->{updateVisibility();setContentDescription(boardDescription());invalidate();});setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);}
  private void updateVisibility(){setVisibility(board.hasMatches()?VISIBLE:GONE);}
  private String boardDescription(){return board.description();}
  public void refresh(){board.refresh();}
  public void close(){board.close();}
  @Override protected void onDraw(Canvas c){super.onDraw(c);board.draw(c,0,0,getWidth(),getHeight());}
 }
}
