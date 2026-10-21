package kw.urtv.home;

import android.app.Activity;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Native prayer/weather views. No browser renderer; no polling while another app is in front. */
final class HomeCard {
    private final kw.urtv.shared.CalendarData calendar;
    private boolean official;
    private TextView nextPrayer;
    private static final ZoneId KUWAIT = ZoneId.of("Asia/Kuwait");
    private static final int SAND=0xFFF6EFE0, GOLD=0xFFDCB86F;
    private final Activity activity;
    private final FrameLayout surface;
    private final LinearLayout table, weather;
    private final TextView heading, condition, weatherDetails;
    private final TextView[] labels = new TextView[6], values = new TextView[6];
    private final Typeface medium, bold;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final SharedPreferences cache;
    private Future<?> pending;
    private volatile HttpURLConnection connection;
    private volatile boolean active;
    private boolean destroyed;
    private int generation;
    private String city="مدينة الكويت", panel="prayers", identity="", day="";
    private double latitude=29.3759, longitude=47.9774;
    private int[] prayers;
    private JSONObject weatherData;
    private long lastPrayer, lastWeather, nextAttempt;
    private final Runnable minute = new Runnable() {
        @Override public void run() {
            if (!active) return;
            refreshDay(); render(); fetchIfNeeded();
            main.postDelayed(this, 60000 - System.currentTimeMillis()%60000 + 25);
        }
    };

    HomeCard(Activity activity, ViewGroup parent, FrameLayout.LayoutParams params) {
        this.activity=activity;
        calendar=new kw.urtv.shared.CalendarData(activity);
        cache=activity.getSharedPreferences("native_home_data", Activity.MODE_PRIVATE);
        medium=Typeface.createFromAsset(activity.getAssets(), "fonts/Thmanyah-Medium.otf");
        bold=Typeface.create(medium, Typeface.BOLD);
        surface=new FrameLayout(activity);
        surface.setFocusable(false);
        surface.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        parent.addView(surface, params);
        table=new LinearLayout(activity); table.setOrientation(LinearLayout.VERTICAL);
        table.setPadding(dp(14),dp(8),dp(14),dp(8));
        GradientDrawable background=new GradientDrawable();
        background.setColor(0xD9091810); background.setCornerRadius(dp(12)); background.setStroke(dp(1),0x29DCB86F);
        table.setBackground(background);
        surface.addView(table,new FrameLayout.LayoutParams(1,dp(234),Gravity.LEFT|Gravity.TOP));
        heading=text(16,GOLD); heading.setText("مواقيت اليوم");
        table.addView(heading,new LinearLayout.LayoutParams(-1,dp(25)));
        for(int i=0;i<6;i++) {
            FrameLayout row=new FrameLayout(activity);
            labels[i]=text(20,SAND); values[i]=text(23,0xFFFFFDF6);
            values[i].setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);
            row.addView(labels[i],new FrameLayout.LayoutParams(-1,-1));
            row.addView(values[i],new FrameLayout.LayoutParams(-1,-1));
            if(i<5) { View line=new View(activity); line.setBackgroundColor(0x21DCB86F);
                row.addView(line,new FrameLayout.LayoutParams(-1,dp(1),Gravity.BOTTOM)); }
            table.addView(row,new LinearLayout.LayoutParams(-1,dp(32)));
        }
        weather=new LinearLayout(activity);weather.setOrientation(LinearLayout.VERTICAL);
        condition=text(28,SAND); condition.setSingleLine(false);
        weatherDetails=text(18,0xFFE2D7C2);weatherDetails.setTypeface(medium);weatherDetails.setSingleLine(false);
        weather.addView(condition,new LinearLayout.LayoutParams(-1,-2));
        LinearLayout.LayoutParams detailParams=new LinearLayout.LayoutParams(-1,-2); detailParams.topMargin=dp(8);
        weather.addView(weatherDetails,detailParams);
        nextPrayer=text(18,GOLD);nextPrayer.setSingleLine(false);nextPrayer.setPadding(0,dp(18),0,0);weather.addView(nextPrayer,new LinearLayout.LayoutParams(-1,-2));
        FrameLayout.LayoutParams wx=new FrameLayout.LayoutParams(1,-2,Gravity.RIGHT|Gravity.TOP);wx.topMargin=dp(80);
        surface.addView(weather,wx);
        surface.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)-> {
            int width=r-l;
            if(width!=or-ol) {
                ViewGroup.LayoutParams p=table.getLayoutParams();p.width=Math.round(width*.53f);table.setLayoutParams(p);
                p=weather.getLayoutParams();p.width=Math.round(width*.43f);weather.setLayoutParams(p);
            }
        });
    }

    void refresh() {
        if(destroyed) return;
        active=true;
        try(Cursor c=activity.getContentResolver().query(Uri.parse("content://kw.urtv.screensaver.config"),null,null,null,null)) {
            if(c!=null && c.moveToFirst()) {
                Uri config=Uri.parse("https://local/?"+c.getString(0));
                String newCity=config.getQueryParameter("city");
                double lat=Double.parseDouble(config.getQueryParameter("lat")), lon=Double.parseDouble(config.getQueryParameter("lon"));
                if(newCity!=null && Double.isFinite(lat) && Double.isFinite(lon) && Math.abs(lat)<=90 && Math.abs(lon)<=180) {
                    city=newCity;latitude=lat;longitude=lon;
                }
                panel=c.getString(1);
            }
        } catch(Exception ignored) { }
        String key=city+":"+latitude+":"+longitude;
        if(!key.equals(identity)) {
            cancel(); identity=key; day=""; weatherData=null;lastPrayer=0;lastWeather=0;nextAttempt=0;
            try {
                weatherData=new JSONObject(cache.getString(identity+":weather",""));
                lastWeather=cache.getLong(identity+":weatherAt",0);
            } catch(Exception ignored) { }
        }
        main.removeCallbacks(minute); minute.run();
    }
    void pause() { active=false; main.removeCallbacks(minute); cancel(); }
    void destroy() { pause();destroyed=true;executor.shutdownNow();((ViewGroup)surface.getParent()).removeView(surface); }
    private void cancel() {
        generation++;
        if(pending!=null&&!pending.isDone())nextAttempt=0;
        if(pending!=null) pending.cancel(true);
        HttpURLConnection c=connection;if(c!=null)c.disconnect();
        pending=null;
    }
    private void refreshDay() {
        String today=LocalDate.now(KUWAIT).toString();
        if(today.equals(day))return;
        day=today;lastPrayer=0;official=false;
        prayers=PrayerTimes.calculate(LocalDate.parse(day),latitude,longitude);
        try {
            JSONArray saved=new JSONArray(cache.getString(identity+":"+day+":prayers",""));
            int[] valid=new int[6];for(int i=0;i<6;i++){valid[i]=saved.getInt(i);if(valid[i]<0||valid[i]>=1440)throw new IllegalArgumentException();}
            prayers=valid;lastPrayer=cache.getLong(identity+":"+day+":prayerAt",0);
        } catch(Exception ignored) { }
        kw.urtv.shared.CalendarData.Day verified=calendar.get(LocalDate.parse(day));
        if(kw.urtv.shared.CalendarData.isKuwaitCity(latitude,longitude)&&verified!=null){prayers=verified.times;official=true;lastPrayer=System.currentTimeMillis();}
    }
    private void fetchIfNeeded() {
        long now=System.currentTimeMillis();
        if(!active || now<nextAttempt || pending!=null&&!pending.isDone())return;
        boolean needPrayer="prayers".equals(panel)&&(lastPrayer==0||now-lastPrayer>=6*3600000L);
        boolean needWeather=weatherData==null||now-lastWeather>=1800000L||now<lastWeather;
        if(!needPrayer&&!needWeather)return;
        nextAttempt=now+300000; // failure backoff, not an every-minute network loop
        final int token=generation;final String wantedDay=day,wantedIdentity=identity;
        final String query="latitude="+latitude+"&longitude="+longitude+"&timezone=Asia%2FKuwait";
        pending=executor.submit(()-> {
            if(needPrayer)try {
                if(kw.urtv.shared.CalendarData.isKuwaitCity(latitude,longitude)){
                    kw.urtv.shared.CalendarData.Day verified=calendar.fetch(LocalDate.parse(wantedDay));
                    main.post(()->{if(active&&token==generation&&wantedDay.equals(day)){prayers=verified.times;official=true;lastPrayer=System.currentTimeMillis();render();}});
                    if(calendar.get(LocalDate.parse(wantedDay).plusDays(1))==null)calendar.fetch(LocalDate.parse(wantedDay).plusDays(1));
                }
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
                    weatherData=data;lastWeather=System.currentTimeMillis();cache.edit().putString(wantedIdentity+":weather",data.toString()).putLong(wantedIdentity+":weatherAt",lastWeather).apply();render();
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
    private void render() {
        table.setVisibility("none".equals(panel)?View.GONE:View.VISIBLE);
        heading.setText("forecast".equals(panel)?"الأيام القادمة":official?"مواقيت اليوم · العجيري":"مواقيت اليوم · محسوبة");
        String[] names={"الفجر","الشروق",LocalDate.now(KUWAIT).getDayOfWeek().getValue()==5?"الجمعة":"الظهر","العصر","المغرب","العشاء"};
        ZonedDateTime now=ZonedDateTime.now(KUWAIT);int minute=now.getHour()*60+now.getMinute();boolean highlighted=false;
        for(int i=0;i<6;i++) {
            View row=(View)labels[i].getParent(); row.setVisibility(View.VISIBLE);
            if("forecast".equals(panel)) {
                if(i==5){row.setVisibility(View.GONE);continue;}
                try {
                    JSONObject daily=weatherData.getJSONObject("daily");
                    LocalDate d=LocalDate.parse(daily.getJSONArray("time").getString(i));
                    labels[i].setText(i==0?"اليوم":i==1?"غداً":d.format(DateTimeFormatter.ofPattern("EEEE",new Locale("ar"))));
                    values[i].setText(ar(Math.round(daily.getJSONArray("temperature_2m_max").getDouble(i))+"° / "+Math.round(daily.getJSONArray("temperature_2m_min").getDouble(i))+"°"));
                }catch(Exception e){labels[i].setText(i==0?"جارٍ تحميل الطقس…":"");values[i].setText("");}
                labels[i].setTextColor(SAND);values[i].setTextColor(SAND);
            } else {
                labels[i].setText(names[i]);values[i].setText(prayers==null?"—":time(prayers[i]));
                boolean next=prayers!=null&&i==kw.urtv.shared.CalendarData.next(prayers,minute);
                row.setBackgroundColor(next?0x382F7052:android.graphics.Color.TRANSPARENT);
                if(next)highlighted=true;
                labels[i].setTextColor(next?0xFFFFE2A0:SAND);values[i].setTextColor(next?0xFFFFE2A0:0xFFFFFDF6);
            }
        }
        nextPrayer.setText("");
        if(prayers!=null){int n=kw.urtv.shared.CalendarData.next(prayers,minute);int target=prayers[n];boolean tomorrow=n==0&&minute>prayers[5];boolean estimated=!official;
            if(tomorrow){kw.urtv.shared.CalendarData.Day d=calendar.get(now.toLocalDate().plusDays(1));target=d==null?PrayerTimes.calculate(now.toLocalDate().plusDays(1),latitude,longitude)[0]:d.times[0];estimated=d==null;}
            int left=target+(tomorrow?1440:0)-minute;
            nextPrayer.setText((tomorrow?"غداً · ":"القادمة · ")+names[n]+" "+time(target)+(estimated?" · تقديري":"")+"\n"+ar("متبقي "+(left/60)+" س و "+(left%60)+" د"));
        }
        weatherDetails.setText(city);
        try {
            JSONObject current=weatherData.getJSONObject("current"),daily=weatherData.getJSONObject("daily");
            double dust=weatherData.optDouble("dust",0);
            String description=dust>=400?"غبار كثيف":dust>=150?"غبار":weatherName(current.getInt("weather_code"));
            String temperature="\u2066"+ar(Math.round(current.getDouble("temperature_2m"))+"°")+"\u2069";
            SpannableString summary=new SpannableString(temperature+"   "+description);
            summary.setSpan(new ForegroundColorSpan(SAND),0,temperature.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            condition.setText(summary);
            condition.setTextColor(dust>=150?GOLD:SAND);
            weatherDetails.setText(city+"  ·  "+ar(Math.round(daily.getJSONArray("temperature_2m_max").getDouble(0))+"° / "+Math.round(daily.getJSONArray("temperature_2m_min").getDouble(0))+"°"));
            if(lastWeather>0&&System.currentTimeMillis()-lastWeather>3600000L)weatherDetails.append("\n"+ar("آخر تحديث قبل "+((System.currentTimeMillis()-lastWeather)/3600000L)+" ساعة"));
        }catch(Exception e){condition.setText("الطقس غير متاح حالياً");}
    }
    private static String weatherName(int code) {
        switch(code){case 0:return "صافي";case 1:return "صافي غالباً";case 2:return "غائم جزئياً";case 3:return "غائم";case 45:case 48:return "ضباب";case 51:case 53:case 55:case 56:case 57:return "رذاذ";case 61:case 80:return "مطر خفيف";case 63:case 81:return "مطر";case 65:case 82:return "مطر غزير";case 66:case 67:return "مطر متجمد";case 71:case 73:case 75:case 77:case 85:case 86:return "ثلج";case 95:case 96:case 99:return "عواصف رعدية";default:return "";}
    }
    private static String time(int minutes) {int h=minutes/60;return ar((h%12==0?12:h%12)+":"+String.format(Locale.ROOT,"%02d",minutes%60)+" ")+(h<12?"ص":"م");}
    private static String ar(String s) {StringBuilder b=new StringBuilder();for(char c:s.toCharArray())b.append(c>='0'&&c<='9'?(char)('٠'+c-'0'):c);return b.toString();}
    private TextView text(float size,int color) {TextView t=new TextView(activity);t.setTextSize(size);t.setTextColor(color);t.setTypeface(bold);t.setIncludeFontPadding(false);t.setSingleLine(true);t.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);t.setTextDirection(View.TEXT_DIRECTION_RTL);return t;}
    private int dp(float n){return Math.round(n*activity.getResources().getDisplayMetrics().density);}
}
