package kw.urtv.shared;
import java.time.*;
public final class PrayerCountdown {
 public static int minutes(ZonedDateTime now,int target,boolean tomorrow){ZonedDateTime end=now.toLocalDate().plusDays(tomorrow?1:0).atTime(target/60,target%60).atZone(now.getZone());return (int)Math.max(0,Duration.between(now.withSecond(0).withNano(0),end).toMinutes());}
}
