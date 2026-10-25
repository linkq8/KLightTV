#!/usr/bin/env python3
import pathlib,subprocess,tempfile,os
root=pathlib.Path(__file__).resolve().parents[1];jdk=pathlib.Path(os.environ.get('JAVA_HOME','/opt/homebrew/opt/openjdk@17'))/'bin'
with tempfile.TemporaryDirectory() as tmp:
 p=pathlib.Path(tmp)/'AlertCheck.java'
 p.write_text('''package kw.urtv.screensaver;
import java.time.*;
public class AlertCheck {
 static void ok(boolean v,String m){if(!v)throw new AssertionError(m);}
 public static void main(String[] x){ZoneId z=ZoneId.of("Asia/Kuwait");LocalDate day=LocalDate.of(2026,12,31);int[] a={270,360,700,900,1050,1140},b={271,361,700,900,1050,1140};
 ZonedDateTime now=day.atStartOfDay(z).plusMinutes(264);
 PrayerAlertPlan.Event e=PrayerAlertPlan.next(now,5,a,b);ok(e.index==0&&e.at==day.atStartOfDay(z).plusMinutes(265).toInstant().toEpochMilli(),"five minutes before Fajr");
 e=PrayerAlertPlan.next(now.plusMinutes(2),5,a,b);ok(e.index==0&&e.lead==0,"at-time still fires after pre-alert"); e=PrayerAlertPlan.next(now.plusMinutes(7),5,a,b);ok(e.index==2&&e.lead==5,"skip sunrise"); e=PrayerAlertPlan.next(now.minusMinutes(5),10,a,b);ok(e.index==0&&e.lead==10,"ten minute reminder");ok(PrayerAlertPlan.next(now,-1,a,b)==null,"disabled");ok(PrayerAlertPlan.next(now,10,null,null)==null,"missing data");ZoneId ny=ZoneId.of("America/New_York");ZonedDateTime dst=LocalDate.of(2026,3,8).atStartOfDay(ny);e=PrayerAlertPlan.next(dst,0,a,b);ok(Instant.ofEpochMilli(e.at).atZone(ny).getHour()==4,"DST wall clock hour");
 e=PrayerAlertPlan.next(now,0,a,b);ok(e.time==270&&e.at==day.atStartOfDay(z).plusMinutes(270).toInstant().toEpochMilli(),"at prayer");
 e=PrayerAlertPlan.next(day.atStartOfDay(z).plusMinutes(1439),5,a,b);ok(e.index==0&&e.time==271&&Instant.ofEpochMilli(e.at).atZone(z).toLocalDate().equals(day.plusDays(1)),"next day and year use tomorrow's Fajr");
 int[] adjusted=a.clone();adjusted[0]+=6;e=PrayerAlertPlan.next(now,5,adjusted,b);ok(e.time==276&&e.at==day.atStartOfDay(z).plusMinutes(271).toInstant().toEpochMilli(),"regional adjustment retained");
 ok(kw.urtv.shared.PrayerCountdown.minutes(LocalDate.of(2026,3,7).atTime(23,0).atZone(ny),270,true)==270,"countdown spring forward");ok(kw.urtv.shared.PrayerCountdown.minutes(LocalDate.of(2026,10,31).atTime(23,0).atZone(ny),270,true)==390,"countdown fall back");
 System.out.println("PASS alert lead time, sunrise exclusion, elapsed event, year rollover, corrected schedule");
 }
}''')
 subprocess.run([str(jdk/'javac'),'-d',tmp,str(p),str(root/'android/app/src/main/java/kw/urtv/screensaver/PrayerAlertPlan.java'),str(root/'android/shared/src/main/java/kw/urtv/shared/PrayerCountdown.java')],check=True)
 subprocess.run([str(jdk/'java'),'-cp',tmp,'kw.urtv.screensaver.AlertCheck'],check=True)
