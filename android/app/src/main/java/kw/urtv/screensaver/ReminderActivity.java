package kw.urtv.screensaver;
import android.os.*;import android.content.*;import android.provider.Settings;import android.net.Uri;import kw.urtv.shared.TvPage;
public class ReminderActivity extends TvPage {
 @Override public void onCreate(Bundle b){super.onCreate(b);}
 @Override public void onResume(){super.onResume();PrayerAlerts.schedule(this);render();}
 void render(){int mode=PrayerAlerts.mode(this);title("تذكير الصلاة","نافذة صامتة أثناء المشاهدة · تختفي بعد ٧ ثوانٍ");
 if(!kw.urtv.shared.DisplayOptions.local(this).prayersEnabled){note("مواقيت الصلاة متوقفة. فعّلها واختر مدينتك من الإعداد السريع أولًا.");addButton("رجوع",this::finish);return;}
 note("الحالة: "+(mode<0?"متوقف":mode>0?"عند الأذان وقبله بـ"+kw.urtv.shared.CalendarData.ar(""+mode)+" دقائق":"عند دخول الوقت"));
 addButton((mode==5?"✓  ":"")+"عند الأذان + قبل الصلاة بخمس دقائق",()->{PrayerAlerts.set(this,5);render();});
 addButton((mode==10?"✓  ":"")+"عند الأذان + قبل الصلاة بعشر دقائق",()->{PrayerAlerts.set(this,10);render();});
 addButton((mode==0?"✓  ":"")+"عند دخول وقت الصلاة",()->{PrayerAlerts.set(this,0);render();});
 addButton("إيقاف التنبيهات",()->{PrayerAlerts.set(this,-1);render();});
 if(!Settings.canDrawOverlays(this))addButton("السماح بالظهور فوق تطبيقات المشاهدة",()->open(Settings.ACTION_MANAGE_OVERLAY_PERMISSION));
 if(!PrayerAlerts.precise(this)&&Build.VERSION.SDK_INT>=31)addButton("السماح بالمواعيد الدقيقة",()->open(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM));
 boolean ready=Settings.canDrawOverlays(this)&&PrayerAlerts.precise(this);
 note(ready?"الصلاحيات جاهزة. لا صوت، ولا إيقاظ للشاشة أو إيقاف للفيديو.":"لن يعمل التذكير حتى تمنح الصلاحيات المطلوبة. بعض تطبيقات البث تحجب النوافذ العائمة.");
 long next=getSharedPreferences("prayer_alerts",0).getLong("next_at",0);if(next>0)note("التنبيه التالي: "+kw.urtv.shared.CalendarData.ar(java.time.Instant.ofEpochMilli(next).atZone(new Prefs(this).place().zoneId()).format(java.time.format.DateTimeFormatter.ofPattern("EEE d MMM · HH:mm",new java.util.Locale("ar")))));
 addButton("معاينة بعد خمس ثوانٍ",()->{if(Settings.canDrawOverlays(this)){Context c=getApplicationContext();int lead=Math.max(0,PrayerAlerts.mode(c));kw.urtv.shared.Place place=new Prefs(c).place();java.time.ZonedDateTime now=java.time.ZonedDateTime.now(place.zoneId());int time=now.getHour()*60+now.getMinute()+lead;new Handler(Looper.getMainLooper()).postDelayed(()->PrayerAlertReceiver.show(c,0,time%1440,lead,"معاينة · "+place.name,()->{}),5000);note("ستظهر معاينة صامتة خلال خمس ثوانٍ؛ يمكنك الانتقال إلى تطبيق آخر.");}else open(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);});
 addButton("رجوع",this::finish);
 }
 void open(String action){try{startActivity(new Intent(action,Uri.parse("package:"+getPackageName())));}catch(Exception e){note("هذا التلفزيون لا يعرض هذه الصفحة. يمكن منح الصلاحية من إعدادات التطبيقات أو بواسطة ADB.");}}
}
