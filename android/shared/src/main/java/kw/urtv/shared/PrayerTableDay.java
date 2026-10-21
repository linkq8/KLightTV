package kw.urtv.shared;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Display-day policy only; the current-day schedule still drives the next-prayer countdown. */
public final class PrayerTableDay {
    private static final DateTimeFormatter DATE=DateTimeFormatter.ofPattern("EEEE، d MMMM yyyy",new Locale("ar"));
    private PrayerTableDay(){}
    public static LocalDate select(LocalDate today,int minute,int isha){
        return minute>=isha+30?today.plusDays(1):today;
    }
    public static String heading(LocalDate today,LocalDate shown){return shown.isAfter(today)?"مواقيت الصلاة غدًا":"مواقيت الصلاة اليوم";}
    public static String dateLabel(LocalDate shown){return shown.format(DATE);}
}
