package kw.urtv.screensaver;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Bounded native data fetches with offline prayers and a city/day-specific disk cache. */
final class ScreenData {
    final kw.urtv.shared.CalendarData calendar;
    boolean official;
    String prayerSource="محسوبة";
    final int[] adjustments;
    String hijri="";
    static final ZoneId KUWAIT=ZoneId.of("Asia/Kuwait");
    final String city, panel;
    final double latitude, longitude;
    final String identity;
    String day="";
    int[] prayers;
    JSONObject weatherData;
    private final SharedPreferences cache;
    private final Runnable changed;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService executor=Executors.newSingleThreadExecutor();
    private Future<?> pending;
    private volatile HttpURLConnection connection;
    private volatile boolean active;
    private int generation;
    private long lastPrayer,lastWeather,nextAttempt;

    ScreenData(Context context,Runnable changed) {
        calendar=new kw.urtv.shared.CalendarData(context);
        Prefs prefs=new Prefs(context);
        adjustments=prefs.adjustments();
        String[] location=prefs.city();
        city=location[0];latitude=Double.parseDouble(location[1]);longitude=Double.parseDouble(location[2]);
        panel=prefs.panel();identity=city+":"+latitude+":"+longitude;
        this.changed=changed;cache=context.getSharedPreferences("native_saver_data",Context.MODE_PRIVATE);
        try {
            weatherData=new JSONObject(cache.getString(identity+":weather",""));
            lastWeather=cache.getLong(identity+":weatherAt",0);
        }catch(Exception ignored){}
    }
    void tick() {active=true;refreshDay();fetchIfNeeded();}
    void destroy() {
        active=false;generation++;
        if(pending!=null)pending.cancel(true);
        HttpURLConnection c=connection;if(c!=null)c.disconnect();
        executor.shutdownNow();main.removeCallbacksAndMessages(null);
    }
    private void refreshDay() {
        String today=LocalDate.now(KUWAIT).toString();
        if(today.equals(day))return;
        day=today;lastPrayer=0;official=false;hijri="";
        applySchedule(LocalDate.parse(day));
    }
    private kw.urtv.shared.RegionSchedule.Result schedule(LocalDate d){return calendar.resolve(d,latitude,longitude,adjustments,PrayerTimes.calculate(d,latitude,longitude));}
    private void applySchedule(LocalDate d){kw.urtv.shared.RegionSchedule.Result result=schedule(d);prayers=result.times;official=result.verified;prayerSource=result.source;kw.urtv.shared.CalendarData.Day base=calendar.get(d);hijri=base==null?"":base.hijri;if(official)lastPrayer=System.currentTimeMillis();}
    int[] tableTimes(LocalDate shown){return prayers==null?null:shown.toString().equals(day)?prayers:schedule(shown).times;}
    int[] tomorrow(){return schedule(LocalDate.parse(day).plusDays(1)).times;}
    boolean tomorrowOfficial(){return schedule(LocalDate.parse(day).plusDays(1)).verified;}
    boolean weatherStale(){return lastWeather>0&&System.currentTimeMillis()-lastWeather>3600000L;}
    private void fetchIfNeeded() {
        long now=System.currentTimeMillis();
        if(!active || now<nextAttempt || pending!=null&&!pending.isDone())return;
        boolean needPrayer=kw.urtv.shared.RegionCatalog.usesCityRecord(latitude,longitude)&&(lastPrayer==0||now-lastPrayer>=6*3600000L);
        boolean needWeather=weatherData==null||now-lastWeather>=1800000L||now<lastWeather;
        if(!needPrayer&&!needWeather)return;
        nextAttempt=now+300000; // failure backoff, not an every-minute network loop
        final int token=generation;final String wantedDay=day,wantedIdentity=identity;
        final String query="latitude="+latitude+"&longitude="+longitude+"&timezone=Asia%2FKuwait";
        pending=executor.submit(()-> {
            if(needPrayer&&kw.urtv.shared.RegionCatalog.usesCityRecord(latitude,longitude))try {
                kw.urtv.shared.CalendarData.Day verified=calendar.fetch(LocalDate.parse(wantedDay));
                main.post(()->{if(active&&token==generation&&wantedDay.equals(day)){applySchedule(LocalDate.parse(wantedDay));changed.run();}});
                if(calendar.get(LocalDate.parse(wantedDay).plusDays(1))==null)calendar.fetch(LocalDate.parse(wantedDay).plusDays(1));
            } catch(Exception ignored) { }
            if(needWeather)try {
                JSONObject data=get("https://api.open-meteo.com/v1/forecast?"+query+"&current=temperature_2m,weather_code&daily=temperature_2m_max,temperature_2m_min,weather_code&forecast_days=5");
                if(!Double.isFinite(data.getJSONObject("current").getDouble("temperature_2m")))throw new IllegalArgumentException();
                if(!Double.isFinite(data.getJSONObject("daily").getJSONArray("temperature_2m_max").getDouble(0))
                        || !Double.isFinite(data.getJSONObject("daily").getJSONArray("temperature_2m_min").getDouble(0)))
                    throw new IllegalArgumentException();
                try { JSONObject air=get("https://air-quality-api.open-meteo.com/v1/air-quality?"+query+"&current=pm10,dust").getJSONObject("current");
                    data.put("dust",Math.max(air.optDouble("pm10",0),air.optDouble("dust",0))); } catch(Exception ignored) { }
                main.post(()->{if(active&&token==generation) {
                    weatherData=data;lastWeather=System.currentTimeMillis();cache.edit().putString(wantedIdentity+":weather",data.toString()).putLong(wantedIdentity+":weatherAt",lastWeather).apply();changed.run();
                }});
            }catch(Exception ignored) { }
        });
    }
    private JSONObject get(String url) throws Exception {
        if(!active||Thread.currentThread().isInterrupted())throw new InterruptedException();
        HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();connection=c;
        c.setConnectTimeout(10000);c.setReadTimeout(10000);
        try {
            if(!active||Thread.currentThread().isInterrupted())throw new InterruptedException();
            if(c.getResponseCode()!=200)throw new java.io.IOException("HTTP "+c.getResponseCode());
            try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()) {
                byte[] buffer=new byte[4096];int n;
                while((n=in.read(buffer))!=-1) {if(!active||Thread.currentThread().isInterrupted())throw new InterruptedException();out.write(buffer,0,n);if(out.size()>262144)throw new java.io.IOException("Response too large");}
                return new JSONObject(out.toString("UTF-8"));
            }
        } finally { c.disconnect();if(connection==c)connection=null; }
    }
    static String weatherName(int code) {
        switch(code){case 0:return "صافي";case 1:return "صافي غالباً";case 2:return "غائم جزئياً";case 3:return "غائم";case 45:case 48:return "ضباب";case 51:case 53:case 55:case 56:case 57:return "رذاذ";case 61:case 80:return "مطر خفيف";case 63:case 81:return "مطر";case 65:case 82:return "مطر غزير";case 66:case 67:return "مطر متجمد";case 71:case 73:case 75:case 77:case 85:case 86:return "ثلج";case 95:case 96:case 99:return "عواصف رعدية";default:return "";}
    }
}
