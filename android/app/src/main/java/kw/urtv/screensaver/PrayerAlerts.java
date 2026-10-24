package kw.urtv.screensaver;
import android.app.*;import android.content.*;import android.os.*;import android.provider.Settings;import java.time.*;import kw.urtv.shared.CalendarData;
final class PrayerAlerts {
 static final String FIRE="kw.urtv.screensaver.PRAYER_ALERT";
 static int mode(Context c){if(!kw.urtv.shared.DisplayOptions.local(c).prayersEnabled)return -1;return c.getSharedPreferences("prayer_alerts",0).getInt("lead",-1);}
 static void set(Context c,int lead){c.getSharedPreferences("prayer_alerts",0).edit().putInt("lead",lead).apply();schedule(c);}
 static boolean precise(Context c){return Build.VERSION.SDK_INT<31||c.getSystemService(AlarmManager.class).canScheduleExactAlarms();}
 static PendingIntent pending(Context c,Intent i){return PendingIntent.getBroadcast(c,720,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
 static synchronized void schedule(Context c){
  AlarmManager am=c.getSystemService(AlarmManager.class);Intent intent=new Intent(c,PrayerAlertReceiver.class).setAction(FIRE);
  am.cancel(pending(c,intent));int lead=mode(c);
  c.getSharedPreferences("prayer_alerts",0).edit().remove("next_at").apply();
  if(lead<0||!Settings.canDrawOverlays(c)||!precise(c))return;
  Prefs p=new Prefs(c);String[] city=p.city();double lat=Double.parseDouble(city[1]),lon=Double.parseDouble(city[2]);
  ZonedDateTime now=ZonedDateTime.now(p.place().zoneId());CalendarData cal=new CalendarData(c);LocalDate day=now.toLocalDate();
  int[] today=kw.urtv.shared.WorldPrayers.resolve(cal,day,p.place(),p.adjustments()).times;
  day=day.plusDays(1);int[] tomorrow=kw.urtv.shared.WorldPrayers.resolve(cal,day,p.place(),p.adjustments()).times;
  PrayerAlertPlan.Event e=PrayerAlertPlan.next(now,lead,today,tomorrow);if(e==null)return;
  intent.putExtra("profile",kw.urtv.shared.PrayerCorrections.key(p.place())+":"+kw.urtv.shared.PrayerCorrections.encode(p.adjustments()));
  intent.putExtra("at",e.at).putExtra("index",e.index).putExtra("time",e.time).putExtra("lead",e.lead).putExtra("city",city[0]);
  try {am.setExact(AlarmManager.RTC,e.at,pending(c,intent));c.getSharedPreferences("prayer_alerts",0).edit().putLong("next_at",e.at).apply();}
  catch(SecurityException denied){c.getSharedPreferences("prayer_alerts",0).edit().remove("next_at").apply();}
 }
}
