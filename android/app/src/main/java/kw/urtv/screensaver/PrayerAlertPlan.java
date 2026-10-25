package kw.urtv.screensaver;
import java.time.*;
public final class PrayerAlertPlan {
 public static final class Event {public final long at;public final int index,time,lead;Event(long a,int i,int t,int l){at=a;index=i;time=t;lead=l;}}
 public static Event next(ZonedDateTime now,int lead,int[] today,int[] tomorrow){if(lead<0)return null;Event best=null;for(int d=0;d<2;d++){int[] times=d==0?today:tomorrow;if(times==null)continue;for(int i:new int[]{0,2,3,4,5})for(int advance:lead==5||lead==10?new int[]{lead,0}:new int[]{0}){long at=now.toLocalDate().plusDays(d).atTime(times[i]/60,times[i]%60).atZone(now.getZone()).minusMinutes(advance).toInstant().toEpochMilli();if(at>now.toInstant().toEpochMilli()&&(best==null||at<best.at))best=new Event(at,i,times[i],advance);}}return best;}
}
