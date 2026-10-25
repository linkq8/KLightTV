package kw.urtv.shared;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.*;
import java.io.*;
import java.net.*;
import java.time.*;
import java.util.*;
import java.util.regex.*;

/** Small shared official-date store. Reject missing or malformed official data; never relabel a calculation. */
public final class CalendarData {
    public static final String URL="https://www.alojeiri.com/ar/ojeiri-calendar";
    public static final ZoneId ZONE=ZoneId.of("Asia/Kuwait");
    private final SharedPreferences cache;
    private JSONObject bundled=new JSONObject();
    public static final class Day {
        public final int[] times;public final String hijri,date;
        Day(String date,JSONObject o)throws Exception {
            this.date=date;hijri=o.optString("hijri");JSONArray a=o.getJSONArray("times");
            if(a.length()!=6)throw new IOException("Missing times");times=new int[6];
            for(int i=0;i<6;i++){String s=a.getString(i);if(!s.matches("[0-2][0-9]:[0-5][0-9]"))throw new IOException("Invalid time");times[i]=Integer.parseInt(s.substring(0,2))*60+Integer.parseInt(s.substring(3));if(times[i]>=1440||(i>0&&times[i]<=times[i-1]))throw new IOException("Invalid order");}
            if(times[0]<180||times[0]>420||times[2]<600||times[2]>840||times[4]<900||times[4]>1260)throw new IOException("Invalid range");
        }
        public boolean ramadan(){return hijri.contains("رمضان");}
    }
    public CalendarData(Context c){cache=c.getSharedPreferences("alojeiri_dates",0);try(InputStream in=c.getAssets().open("calendar/alojeiri.json")){bundled=new JSONObject(read(in)).getJSONObject("days");}catch(Exception ignored){}}
    public synchronized Day get(LocalDate date){String key=date.toString();try {String saved=cache.getString(key,null);JSONObject obj=saved==null?bundled.optJSONObject(key):new JSONObject(saved);return obj==null?null:new Day(key,obj);}catch(Exception e){return null;}}
    public RegionSchedule.Result resolve(LocalDate date,double lat,double lon,int[] adjustments,int[] calculated){Day d=get(date);return RegionSchedule.resolve(lat,lon,d==null?null:d.times,calculated,adjustments);}
    public static boolean isKuwaitCity(double lat,double lon){return Math.abs(lat-29.3759)<.015&&Math.abs(lon-47.9774)<.015;}
    public static Day parse(String payload,LocalDate date)throws Exception {
        String h=new JSONObject(payload).getString("html");
        if(!Pattern.compile("id=\"datepicker\"\\s+value=\""+date+"\"").matcher(h).find())throw new IOException("Date mismatch");
        Matcher m=Pattern.compile("class=\"item-prayer prayer_time\"\\s+data-time=\"(\\d\\d:\\d\\d):\\d\\d\"").matcher(h);JSONArray values=new JSONArray();while(m.find())values.put(m.group(1));
        Matcher labels=Pattern.compile("<p>(.*?)</p>").matcher(h.split("class=\"ls-prayer")[0]);String hijri="";for(int i=0;i<3&&labels.find();i++)hijri=labels.group(1).trim();
        return new Day(date.toString(),new JSONObject().put("times",values).put("hijri",hijri));
    }
    public Day fetch(LocalDate date)throws Exception {
        if(Thread.currentThread().isInterrupted())throw new InterruptedException();
        HttpURLConnection c=(HttpURLConnection)new java.net.URL(URL+"?country_id=1&date="+date).openConnection();c.setConnectTimeout(7000);c.setReadTimeout(7000);c.setRequestProperty("X-Requested-With","XMLHttpRequest");c.setRequestProperty("User-Agent","KLightTV/1.10.0");
        try(InputStream in=c.getInputStream()){
            Day result=parse(read(in),date);if(Thread.currentThread().isInterrupted())throw new InterruptedException();JSONArray times=new JSONArray();for(int n:result.times)times.put(String.format(Locale.ROOT,"%02d:%02d",n/60,n%60));
            SharedPreferences.Editor edit=cache.edit();for(String key:cache.getAll().keySet())try{if(LocalDate.parse(key).isBefore(date.minusDays(45))||LocalDate.parse(key).isAfter(date.plusDays(45)))edit.remove(key);}catch(Exception ignored){}
            edit.putString(date.toString(),new JSONObject().put("times",times).put("hijri",result.hijri).toString()).apply();return result;
        }finally{c.disconnect();}
    }
    private static String read(InputStream in)throws Exception{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1){if(Thread.currentThread().isInterrupted())throw new InterruptedException();out.write(b,0,n);if(out.size()>262144)throw new IOException("Too large");}return out.toString("UTF-8");}
    public static String ar(String s){StringBuilder b=new StringBuilder();for(char c:s.toCharArray())b.append(c>='0'&&c<='9'?(char)('٠'+c-'0'):c);return b.toString();}
    public static String time(int n){return ar(String.format(Locale.ROOT,"%02d:%02d",n/60,n%60));}
    public static int next(int[] times,int minute){for(int i=0;i<6;i++)if(i!=1&&times[i]>minute)return i;return 0;}
}
