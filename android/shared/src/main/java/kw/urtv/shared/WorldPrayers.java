package kw.urtv.shared;
import java.time.*;import java.time.chrono.HijrahDate;import java.util.Date;import com.batoulapps.adhan.*;import com.batoulapps.adhan.data.DateComponents;
/** Offline Adhan engine; an adopted calculation method is not a downloaded official timetable. */
public final class WorldPrayers {
 public static int[] calculate(LocalDate day,Place p){
  if(p.needsConfirmation)throw new IllegalArgumentException("أعد اعتماد الدولة والمدينة");
  CalculationParameters params=CalculationMethod.valueOf(p.method).getParameters();params.madhab=p.hanafi?Madhab.HANAFI:Madhab.SHAFI;params.highLatitudeRule=Math.abs(p.lat)>48?HighLatitudeRule.SEVENTH_OF_THE_NIGHT:HighLatitudeRule.MIDDLE_OF_THE_NIGHT;
  if(p.method.equals("UMM_AL_QURA")&&ramadan(day))params.ishaInterval=120;
  com.batoulapps.adhan.PrayerTimes t=new com.batoulapps.adhan.PrayerTimes(new Coordinates(p.lat,p.lon),new DateComponents(day.getYear(),day.getMonthValue(),day.getDayOfMonth()),params);
  Date[] dates={t.fajr,t.sunrise,t.dhuhr,t.asr,t.maghrib,t.isha};int[] out=new int[6];
  for(int i=0;i<6;i++){if(dates[i]==null)throw new IllegalArgumentException("لا تتوفر مواقيت شمسية لهذه المدينة في هذا اليوم؛ راجع الجهة المحلية");ZonedDateTime z=dates[i].toInstant().atZone(p.zoneId());out[i]=(int)(java.time.temporal.ChronoUnit.DAYS.between(day,z.toLocalDate())*1440+z.getHour()*60+z.getMinute());if(out[i]<0||out[i]>=1440||(i>0&&out[i]<=out[i-1]))throw new IllegalArgumentException("يتطلب هذا الموقع جدولًا محليًا معتمدًا");}return out;
 }
 public static boolean ramadan(LocalDate day){return HijrahDate.from(day).get(java.time.temporal.ChronoField.MONTH_OF_YEAR)==9;}
 public static RegionSchedule.Result resolve(CalendarData calendar,LocalDate day,Place p,int[] corrections){
  try {PrayerCorrections.encode(corrections);int[] calculated=calculate(day,p);if(p.officialEligible())return calendar.resolve(day,p.lat,p.lon,corrections,calculated);
   for(int i=0;i<6;i++){calculated[i]+=corrections[i];if(calculated[i]<0||calculated[i]>=1440||(i>0&&calculated[i]<=calculated[i-1]))throw new IllegalArgumentException();}
   return new RegionSchedule.Result(calculated,false,java.util.Arrays.stream(corrections).anyMatch(n->n!=0),"حساب · "+p.label());
  }catch(Exception e){try{if(p.officialEligible()&&calendar.get(day)!=null)return calendar.resolve(day,p.lat,p.lon,corrections,calendar.get(day).times);}catch(Exception invalid){}return new RegionSchedule.Result(null,false,false,"راجع الجدول المحلي؛ المواقيت غير متاحة");}
 }
}
