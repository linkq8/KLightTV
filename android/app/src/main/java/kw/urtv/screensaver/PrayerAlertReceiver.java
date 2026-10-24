package kw.urtv.screensaver;
import android.content.*;import android.os.*;import android.graphics.PixelFormat;import android.graphics.Typeface;import android.graphics.drawable.GradientDrawable;import android.provider.Settings;import android.view.*;import android.widget.*;
/** No service: a bounded 7-second overlay during a pending broadcast, then release all views. */
public class PrayerAlertReceiver extends BroadcastReceiver {
 static View active;static WindowManager window;
 @Override public void onReceive(Context c,Intent i){
  if(!PrayerAlerts.FIRE.equals(i.getAction())){PrayerAlerts.schedule(c);return;}
  long at=i.getLongExtra("at",0);int mode=PrayerAlerts.mode(c);PrayerAlerts.schedule(c);
  Prefs prefs=new Prefs(c);String profile=kw.urtv.shared.PrayerCorrections.key(prefs.place())+":"+kw.urtv.shared.PrayerCorrections.encode(prefs.adjustments());
  if(!profile.equals(i.getStringExtra("profile"))||mode<0||(i.getIntExtra("lead",-2)!=0&&mode!=i.getIntExtra("lead",-2))||Math.abs(System.currentTimeMillis()-at)>90000||!c.getSystemService(PowerManager.class).isInteractive()||!Settings.canDrawOverlays(c))return;
  PendingResult pending=goAsync();show(c,i.getIntExtra("index",0),i.getIntExtra("time",0),i.getIntExtra("lead",0),i.getStringExtra("city"),pending::finish);
 }
 static void show(Context c,int index,int time,int lead,String city,Runnable done){
  Handler h=new Handler(Looper.getMainLooper());try{
   if(active!=null){done.run();return;}
   window=c.getSystemService(WindowManager.class);LinearLayout box=new LinearLayout(c);box.setOrientation(android.widget.LinearLayout.VERTICAL);int pad=Math.round(20*c.getResources().getDisplayMetrics().density);box.setPadding(pad,pad,pad,pad);
   GradientDrawable bg=new GradientDrawable();kw.urtv.shared.Palette palette=new kw.urtv.shared.Palette(c,kw.urtv.shared.DisplayOptions.load(c));bg.setColor(palette.panel);bg.setCornerRadius(pad*.7f);box.setBackground(bg);
   String[] names={"الفجر","الشروق","الظهر","العصر","المغرب","العشاء"};index=Math.max(0,Math.min(5,index));
   String title=lead==10?"تبقّت عشر دقائق على صلاة "+names[index]:lead==5?"تبقّت خمس دقائق على صلاة "+names[index]:"حان وقت صلاة "+names[index];
   for(String s:new String[]{title,kw.urtv.shared.DisplayOptions.load(c).time(time)+"  ·  "+city}){TextView t=new TextView(c);t.setText(s);t.setTextSize(s.equals(title)?23:17);t.setTextColor(s.equals(title)?palette.ink:palette.muted);t.setTypeface(Typeface.createFromAsset(c.getAssets(),"fonts/Thmanyah-Medium.otf"));t.setGravity(Gravity.RIGHT);box.addView(t);}
   WindowManager.LayoutParams p=new WindowManager.LayoutParams(Math.min(c.getResources().getDisplayMetrics().widthPixels-pad*2,Math.round(470*c.getResources().getDisplayMetrics().density)),-2,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,PixelFormat.TRANSLUCENT);
   p.gravity=Gravity.TOP|Gravity.RIGHT;p.x=pad;p.y=pad;window.addView(box,p);active=box;
   h.postDelayed(()->{try{window.removeView(box);}catch(Exception ignored){}finally{active=null;window=null;done.run();}},7000);
  }catch(Exception failure){active=null;window=null;done.run();}
 }
}
