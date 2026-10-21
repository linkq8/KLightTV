#!/usr/bin/env python3
"""Isha+30 rollover, date boundaries, and table/countdown agreement."""
import pathlib,subprocess,tempfile,os
r=pathlib.Path(__file__).resolve().parents[1]
j=pathlib.Path(os.environ.get('JAVA_HOME','/opt/homebrew/opt/openjdk@17'))/'bin'
with tempfile.TemporaryDirectory() as temp:
 h=pathlib.Path(temp)/'TableCheck.java'
 h.write_text('''package kw.urtv.screensaver;
import java.time.LocalDate;
import kw.urtv.shared.PrayerTableDay;
public class TableCheck {
 static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
 public static void main(String[] args){
  LocalDate d=LocalDate.of(2026,10,21);int isha=1109;
  check(PrayerTableDay.select(d,isha+29,isha).equals(d),"29 minutes still today");
  check(PrayerTableDay.select(d,isha+30,isha).equals(d.plusDays(1)),"30 minutes exactly tomorrow");
  check(PrayerTableDay.select(d,1439,isha).equals(d.plusDays(1)),"23:59 tomorrow");
  check(PrayerTableDay.select(d.plusDays(1),0,1108).equals(d.plusDays(1)),"00:00 same table date now today");
  check(PrayerTableDay.heading(d,d.plusDays(1)).contains("غدًا"),"tomorrow heading");
  check(PrayerTableDay.heading(d.plusDays(1),d.plusDays(1)).contains("اليوم"),"midnight heading");
  check(PrayerTableDay.dateLabel(d.plusDays(1)).contains("الخميس"),"weekday follows displayed date");
  check(PrayerTableDay.select(d,1439,1420).equals(d),"late Isha does not wrap cutoff into same evening");
  for(String date:new String[]{"2026-12-31","2028-02-28","2028-02-29","2026-10-22"}){
   LocalDate day=LocalDate.parse(date);int[] today=PrayerTimes.calculate(day,29.3759,47.9774),tomorrow=PrayerTimes.calculate(day.plusDays(1),29.3759,47.9774);
   LocalDate shown=PrayerTableDay.select(day,today[5]+30,today[5]);check(shown.equals(day.plusDays(1)),"calendar rollover "+date);
   int[] table=PrayerTimes.calculate(shown,29.3759,47.9774),next=PrayerClock.next(today,tomorrow,today[5]+30);
   check(table[0]==next[1]&&next[0]==0,"displayed Fajr agrees with next prayer "+date);
   check(PrayerTableDay.select(shown,0,tomorrow[5]).equals(shown),"midnight retains displayed calendar date "+date);
   if(date.equals("2026-10-22"))check(shown.getDayOfWeek().getValue()==5&&PrayerTableDay.dateLabel(shown).contains("الجمعة"),"Friday table weekday");
  }
  System.out.println("PASS table rollover: Isha+29/+30, 23:59/00:00, month/year/leap day, Friday and next Fajr agreement");
 }
}''')
 src=r/'android/app/src/main/java/kw/urtv/screensaver'
 subprocess.run([str(j/'javac'),'-encoding','UTF-8','-d',temp,str(h),str(src/'PrayerTimes.java'),str(src/'PrayerClock.java'),str(r/'android/shared/src/main/java/kw/urtv/shared/PrayerTableDay.java')],check=True)
 subprocess.run([str(j/'java'),'-cp',temp,'kw.urtv.screensaver.TableCheck'],check=True)
