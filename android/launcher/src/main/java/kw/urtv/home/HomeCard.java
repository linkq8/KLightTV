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
    private String prayerSource="محسوبة",correctionIdentity="";
    private int[] adjustments=new int[6];
    private kw.urtv.shared.DisplayOptions options;
    private TextView nextPrayer,remainingLabel;
    private final kw.urtv.shared.TimeTiles.TileView[] tiles=new kw.urtv.shared.TimeTiles.TileView[6];
    private kw.urtv.shared.TimeTiles.TileView nextTiles,nextTimeTiles;
    private kw.urtv.shared.Palette palette;private kw.urtv.shared.Place place,weatherPlace;private String weatherIdentity="";private boolean prayersWereEnabled;
    private ZoneId zone = ZoneId.of("Asia/Kuwait");
    private static final int SAND=0xFFF1F5FA, GOLD=0xFF8EDBEA;
    private final Activity activity;
    private final FrameLayout surface;
    private final LinearLayout table, weather, clocks;
    private final kw.urtv.shared.FootballBoard.Surface football;
    private final TextView heading, tableDate, condition, weatherDetails;
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
            refreshDay(); render(); fetchIfNeeded(); if(options.showsMatches())football.refresh();else football.setVisibility(View.GONE);
            main.postDelayed(this, 60000 - System.currentTimeMillis()%60000 + 25);
        }
    };

    // Home: timetable left; clock, weather, next prayer and matches right.
    private final LinearLayout rightStack;
    private View simpleView;private kw.urtv.shared.SimplePrayerFace simpleFace;
    private final kw.urtv.shared.WeatherSymbol.SymbolView weatherSymbol;
    HomeCard(Activity activity, ViewGroup parent, FrameLayout.LayoutParams params) {
        this.activity=activity;calendar=new kw.urtv.shared.CalendarData(activity);cache=activity.getSharedPreferences("native_home_data",0);
        medium=Typeface.createFromAsset(activity.getAssets(),"fonts/Thmanyah-Medium.otf");bold=Typeface.create(medium,Typeface.BOLD);
        options=kw.urtv.shared.DisplayOptions.load(activity);
        int column=options.focused()?(activity.getResources().getDisplayMetrics().widthPixels-dp(104))/2:dp(280);
        surface=new FrameLayout(activity);surface.setFocusable(false);surface.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);parent.addView(surface,params);
        table=new LinearLayout(activity);table.setOrientation(android.widget.LinearLayout.VERTICAL);table.setPadding(dp(8),dp(options.focused()?6:10),dp(8),dp(options.focused()?6:10));
        GradientDrawable bg=new GradientDrawable();bg.setColor(0xED101D2D);bg.setCornerRadius(dp(12));table.setBackground(bg);
        FrameLayout.LayoutParams tp=new FrameLayout.LayoutParams(column,-2,Gravity.LEFT|Gravity.TOP);tp.topMargin=dp(8);surface.addView(table,tp);
        heading=text(14,GOLD);tableDate=text(12,0xFFB9CADA);
        table.addView(heading,new LinearLayout.LayoutParams(-1,dp(options.focused()?22:25)));
        LinearLayout.LayoutParams dateParams=new LinearLayout.LayoutParams(-1,dp(options.focused()?18:23));dateParams.bottomMargin=dp(options.focused()?4:8);table.addView(tableDate,dateParams);
        LinearLayout route=new LinearLayout(activity);route.setOrientation(LinearLayout.VERTICAL);table.addView(route,new LinearLayout.LayoutParams(-1,-2));
        for(int i=0;i<6;i++){
            FrameLayout row=new FrameLayout(activity);row.setPadding(dp(10),dp(options.focused()?2:4),dp(10),dp(options.focused()?2:4));
            LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,dp(options.focused()?43:40));if(i>0)rp.topMargin=dp(2);route.addView(row,rp);
            labels[i]=text(17,SAND);if(options.focused())labels[i].setGravity(Gravity.CENTER);row.addView(labels[i],new FrameLayout.LayoutParams(dp(options.focused()?156:78),-1,Gravity.RIGHT|Gravity.CENTER_VERTICAL));
            values[i]=text(17,SAND);values[i].setGravity(Gravity.CENTER);row.addView(values[i],new FrameLayout.LayoutParams(dp(options.focused()?200:150),-1,Gravity.LEFT|Gravity.CENTER_VERTICAL));
            tiles[i]=new kw.urtv.shared.TimeTiles.TileView(activity);tiles[i].alignCentered();
            FrameLayout.LayoutParams timeParams=new FrameLayout.LayoutParams(dp(options.focused()?190:150),-1,Gravity.LEFT|Gravity.CENTER_VERTICAL);row.addView(tiles[i],timeParams);
        }
        rightStack=new LinearLayout(activity);rightStack.setOrientation(LinearLayout.VERTICAL);
        FrameLayout.LayoutParams stackParams=new FrameLayout.LayoutParams(column,-2,Gravity.RIGHT|Gravity.TOP);stackParams.topMargin=dp(126);surface.addView(rightStack,stackParams);
        weather=new LinearLayout(activity);weather.setOrientation(android.widget.LinearLayout.VERTICAL);weather.setGravity(Gravity.CENTER_HORIZONTAL);
        condition=text(24,SAND);weatherDetails=text(13,0xFFB9CADA);
        LinearLayout weatherLine=new LinearLayout(activity);weatherLine.setGravity(Gravity.CENTER);weatherLine.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        weatherLine.addView(condition,new LinearLayout.LayoutParams(-2,dp(32)));
        weatherSymbol=new kw.urtv.shared.WeatherSymbol.SymbolView(activity);weatherSymbol.setVisibility(View.GONE);
        LinearLayout.LayoutParams iconParams=new LinearLayout.LayoutParams(dp(30),dp(30));iconParams.leftMargin=dp(12);weatherLine.addView(weatherSymbol,iconParams);
        weather.addView(weatherLine,new LinearLayout.LayoutParams(-1,-2));
        weatherDetails.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams detailsParams=new LinearLayout.LayoutParams(-1,-2);detailsParams.topMargin=dp(2);weather.addView(weatherDetails,detailsParams);

        clocks=new LinearLayout(activity);clocks.setOrientation(android.widget.LinearLayout.HORIZONTAL);clocks.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);clocks.setPadding(dp(12),dp(10),dp(12),dp(10));
        GradientDrawable clockBg=new GradientDrawable();clockBg.setColor(0xE8101D2D);clockBg.setCornerRadius(dp(12));clocks.setBackground(clockBg);
        LinearLayout nextColumn=new LinearLayout(activity),remainingColumn=new LinearLayout(activity);nextColumn.setOrientation(android.widget.LinearLayout.VERTICAL);remainingColumn.setOrientation(android.widget.LinearLayout.VERTICAL);
        clocks.addView(nextColumn,new LinearLayout.LayoutParams(0,-2,1));clocks.addView(remainingColumn,new LinearLayout.LayoutParams(0,-2,1));
        nextPrayer=text(15,GOLD);remainingLabel=text(13,0xFFB9CADA);remainingLabel.setText("الوقت المتبقي");nextPrayer.setGravity(Gravity.CENTER);remainingLabel.setGravity(Gravity.CENTER);
        nextColumn.addView(nextPrayer,new LinearLayout.LayoutParams(-1,dp(30)));remainingColumn.addView(remainingLabel,new LinearLayout.LayoutParams(-1,dp(30)));
        nextTimeTiles=new kw.urtv.shared.TimeTiles.TileView(activity);nextTiles=new kw.urtv.shared.TimeTiles.TileView(activity);nextTimeTiles.alignCentered();nextTiles.alignCentered();
        nextColumn.addView(nextTimeTiles,new LinearLayout.LayoutParams(-1,dp(42)));remainingColumn.addView(nextTiles,new LinearLayout.LayoutParams(-1,dp(42)));
        rightStack.addView(weather,new LinearLayout.LayoutParams(-1,-2));
        LinearLayout.LayoutParams clockParams=new LinearLayout.LayoutParams(-1,-2);clockParams.topMargin=dp(10);rightStack.addView(clocks,clockParams);
        football=new kw.urtv.shared.FootballBoard.Surface(activity);
        LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(-1,dp(54));fp.topMargin=dp(12);rightStack.addView(football,fp);
        if(options.focused()){
            simpleFace=new kw.urtv.shared.SimplePrayerFace(activity);
            simpleView=new View(activity){@Override protected void onDraw(android.graphics.Canvas c){ZonedDateTime n=ZonedDateTime.now(zone);LocalDate d=n.toLocalDate(),shown=prayers==null?d:kw.urtv.shared.PrayerTableDay.select(d,n.getHour()*60+n.getMinute(),prayers[5]);simpleFace.draw(c,getWidth(),getHeight(),options,palette,n,city,prayers,shown.equals(d)?prayers:schedule(shown).times,shown,prayers==null?null:schedule(d.plusDays(1)).times);}};
            surface.addView(simpleView,new FrameLayout.LayoutParams(-1,-1));
        }
        table.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->surface.post(this::fitHero));
        rightStack.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->surface.post(this::fitHero));
    }
    void alignBelowClock(int clockBottom){
        if(options.focused())return;
        FrameLayout.LayoutParams p=(FrameLayout.LayoutParams)rightStack.getLayoutParams();
        p.topMargin=Math.max(dp(options.focused()?92:116),clockBottom-dp(options.focused()?56:80)+dp(10));rightStack.setLayoutParams(p);

    }
    private void fitHero(){
        if(options.focused())return;
        // Large reading modes may scroll slightly; never cover prayer/football content with the app dock.
        int base=dp(options.focused()?56:80);
        int bottom=Math.max(table.getBottom(),rightStack.getBottom());
        View hero=(View)surface.getParent();int height=Math.max(activity.getResources().getDisplayMetrics().heightPixels,base+bottom+dp(120));
        if(hero.getLayoutParams().height!=height){ViewGroup.LayoutParams hp=hero.getLayoutParams();hp.height=height;hero.setLayoutParams(hp);ViewGroup.LayoutParams sp=surface.getLayoutParams();sp.height=height-base;surface.setLayoutParams(sp);}

    }

    void refresh() {
        if(destroyed) return;
        active=true;
        options=kw.urtv.shared.DisplayOptions.load(activity);palette=new kw.urtv.shared.Palette(activity,options);place=kw.urtv.shared.Place.load(activity);zone=options.clockZone(activity);weatherPlace=options.weatherLocation(activity);if(prayersWereEnabled!=options.prayersEnabled){day="";prayersWereEnabled=options.prayersEnabled;}
        applyReading();
        try(Cursor c=activity.getContentResolver().query(Uri.parse("content://kw.urtv.screensaver.config"),null,null,null,null)) {
            if(c!=null && c.moveToFirst()) {
                Uri config=Uri.parse("https://local/?"+c.getString(0));
                String newCity=config.getQueryParameter("city");
                double lat=Double.parseDouble(config.getQueryParameter("lat")), lon=Double.parseDouble(config.getQueryParameter("lon"));
                if(newCity!=null && Double.isFinite(lat) && Double.isFinite(lon) && Math.abs(lat)<=90 && Math.abs(lon)<=180) {
                    city=newCity;latitude=lat;longitude=lon;
                }
                panel=c.getString(1);
                String raw=config.getQueryParameter("adjust");adjustments=new int[6];if(raw!=null)adjustments=kw.urtv.shared.PrayerCorrections.parse(raw);
            }
        } catch(Exception ignored) { }
        if(options.focused())panel="prayers";
        String corrections=kw.urtv.shared.PrayerCorrections.key(place)+java.util.Arrays.toString(adjustments);if(!corrections.equals(correctionIdentity)){correctionIdentity=corrections;day="";lastPrayer=0;}
        String key=place.json();
        if(!key.equals(identity)) {
            cancel(); identity=key; day="";lastPrayer=0;nextAttempt=0;
        }
        String weatherKey=weatherPlace.json();
        if(!weatherKey.equals(weatherIdentity)){cancel();weatherIdentity=weatherKey;weatherData=null;lastWeather=0;nextAttempt=0;
            try {
                weatherData=new JSONObject(cache.getString(weatherIdentity+":weather",""));
                lastWeather=cache.getLong(weatherIdentity+":weatherAt",0);
            } catch(Exception ignored) { }
        }
        main.removeCallbacks(minute); minute.run();
    }
    void pause() { active=false; main.removeCallbacks(minute); cancel(); }
    void destroy() { pause();football.close();destroyed=true;executor.shutdownNow();((ViewGroup)surface.getParent()).removeView(surface); }
    private void cancel() {
        generation++;
        if(pending!=null&&!pending.isDone())nextAttempt=0;
        if(pending!=null) pending.cancel(true);
        HttpURLConnection c=connection;if(c!=null)c.disconnect();
        pending=null;
    }
    private void refreshDay() {
        String today=LocalDate.now(zone).toString();
        if(today.equals(day))return;
        day=today;lastPrayer=0;official=false;
        applySchedule(LocalDate.parse(day));
    }
    private kw.urtv.shared.RegionSchedule.Result schedule(LocalDate d){return kw.urtv.shared.WorldPrayers.resolve(calendar,d,place,adjustments);}
    private void applySchedule(LocalDate d){if(!options.prayersEnabled){prayers=null;official=false;return;}kw.urtv.shared.RegionSchedule.Result result=schedule(d);prayers=result.times;official=result.verified;prayerSource=result.source;if(official)lastPrayer=System.currentTimeMillis();}
    private void fetchIfNeeded() {
        long now=System.currentTimeMillis();
        if(!active || now<nextAttempt || pending!=null&&!pending.isDone())return;
        boolean needPrayer=options.prayersEnabled&&place.officialEligible()&&(lastPrayer==0||now-lastPrayer>=6*3600000L);
        boolean needWeather=options.showsWeather()&&(weatherData==null||now-lastWeather>=1800000L||now<lastWeather);
        if(!needPrayer&&!needWeather)return;
        nextAttempt=now+300000; // failure backoff, not an every-minute network loop
        final int token=generation;final String wantedDay=day,wantedIdentity=weatherIdentity;
        final String query="latitude="+weatherPlace.lat+"&longitude="+weatherPlace.lon+"&timezone="+android.net.Uri.encode(weatherPlace.zone);
        pending=executor.submit(()-> {
            if(needPrayer)try {
                if(place.officialEligible()){
                    kw.urtv.shared.CalendarData.Day verified=calendar.fetch(LocalDate.parse(wantedDay));
                    main.post(()->{if(active&&token==generation&&wantedDay.equals(day)){applySchedule(LocalDate.parse(wantedDay));render();}});
                    if(calendar.get(LocalDate.parse(wantedDay).plusDays(1))==null)calendar.fetch(LocalDate.parse(wantedDay).plusDays(1));
                }
            } catch(Exception ignored) { }
            if(needWeather)try {
                JSONObject data=get("https://api.open-meteo.com/v1/forecast?"+query+"&current=temperature_2m,weather_code,is_day&daily=temperature_2m_max,temperature_2m_min,weather_code&forecast_days=5");
                if(!Double.isFinite(data.getJSONObject("current").getDouble("temperature_2m")))throw new IllegalArgumentException();
                if(!Double.isFinite(data.getJSONObject("daily").getJSONArray("temperature_2m_max").getDouble(0))
                        || !Double.isFinite(data.getJSONObject("daily").getJSONArray("temperature_2m_min").getDouble(0)))
                    throw new IllegalArgumentException();
                try { JSONObject air=get("https://air-quality-api.open-meteo.com/v1/air-quality?"+query+"&current=pm10,dust").getJSONObject("current");
                    data.put("dust",Math.max(air.optDouble("pm10",0),air.optDouble("dust",0))); } catch(Exception ignored) { }
                main.post(()->{if(active&&token==generation) {
                    weatherData=data;lastWeather=System.currentTimeMillis();cache.edit().clear().putString(wantedIdentity+":weather",data.toString()).putLong(wantedIdentity+":weatherAt",lastWeather).apply();render();
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
    private void applyReading(){
        heading.setTextColor(palette.accent);tableDate.setTextColor(palette.muted);weatherDetails.setTextColor(palette.muted);nextPrayer.setTextColor(palette.accent);remainingLabel.setTextColor(palette.muted);((GradientDrawable)((View)nextPrayer.getParent().getParent()).getBackground()).setColor(palette.panel);
        for(int i=0;i<6;i++){View row=(View)labels[i].getParent();ViewGroup.LayoutParams rp=row.getLayoutParams();rp.height=dp((options.focused()?43:40)+Math.max(options.reading,options.prayer)*3);row.setLayoutParams(rp);labels[i].setTextSize(16*options.prayerScale());values[i].setTextSize(17*options.prayerScale());labels[i].setTypeface(medium,options.heavy?Typeface.BOLD:Typeface.NORMAL);}
        condition.setTextSize(20*options.textScale());weatherDetails.setTextSize(13*options.textScale());nextPrayer.setTextSize(14*options.nextScale());remainingLabel.setTextSize(13*options.nextScale());
        int glass=options.focused()?android.graphics.Color.TRANSPARENT:options.contrast?palette.panel:(palette.light?0xD0F5F8FA:0x70101D2D);
        ((GradientDrawable)table.getBackground()).setColor(glass);
        ((GradientDrawable)((View)nextPrayer.getParent().getParent()).getBackground()).setColor(glass);
        for(kw.urtv.shared.TimeTiles.TileView tile:tiles){tile.setGlassTiles(palette.light,options.contrast);tile.setDigitScale(.68f*options.prayerScale());}
        nextTimeTiles.setGlassTiles(palette.light,options.contrast);nextTiles.setGlassTiles(palette.light,options.contrast);
        nextTimeTiles.setDigitScale(.68f*options.nextScale());nextTiles.setDigitScale(.68f*options.nextScale());
    }
    private void render() {
        if(options.focused()&&simpleView!=null){table.setVisibility(View.GONE);rightStack.setVisibility(View.GONE);simpleView.invalidate();simpleView.setContentDescription("الساعة ومواقيت الصلاة · "+city+" · "+options.time(ZonedDateTime.now(zone).getHour()*60+ZonedDateTime.now(zone).getMinute())+(prayers==null?"":java.util.stream.IntStream.range(0,6).mapToObj(i->new String[]{"الفجر","الشروق","الظهر","العصر","المغرب","العشاء"}[i]+" "+options.time(prayers[i])).collect(java.util.stream.Collectors.joining(" · "))));return;}

        table.setVisibility("none".equals(panel)||("forecast".equals(panel)?!options.showsWeather():!options.prayersEnabled)?View.GONE:View.VISIBLE);weather.setVisibility(options.showsWeather()?View.VISIBLE:View.GONE);clocks.setVisibility(options.prayersEnabled?View.VISIBLE:View.GONE);
        ZonedDateTime now=ZonedDateTime.now(zone);int minute=now.getHour()*60+now.getMinute();
        LocalDate today=now.toLocalDate(),shown=prayers==null?today:kw.urtv.shared.PrayerTableDay.select(today,minute,prayers[5]);
        int[] tableTimes=shown.equals(today)?prayers:schedule(shown).times;
        heading.setText("forecast".equals(panel)?"الأيام القادمة":kw.urtv.shared.PrayerTableDay.heading(today,shown)+" · "+city);
        tableDate.setText(options.numbers(kw.urtv.shared.PrayerTableDay.dateLabel(shown)));tableDate.setVisibility("forecast".equals(panel)?View.GONE:View.VISIBLE);
        String[] names={"الفجر","الشروق",shown.getDayOfWeek().getValue()==5?"الجمعة":"الظهر","العصر","المغرب","العشاء"};
        boolean highlighted=false;
        for(int i=0;i<6;i++) {
            View row=(View)labels[i].getParent(); row.setVisibility(View.VISIBLE);
            boolean forecast="forecast".equals(panel);tiles[i].setVisibility(forecast?View.GONE:View.VISIBLE);values[i].setVisibility(forecast?View.VISIBLE:View.GONE);
            if("forecast".equals(panel)) {
                row.setBackground(null);
                if(i==5){row.setVisibility(View.GONE);continue;}
                try {
                    JSONObject daily=weatherData.getJSONObject("daily");
                    LocalDate d=LocalDate.parse(daily.getJSONArray("time").getString(i));
                    labels[i].setText(i==0?"اليوم":i==1?"غداً":d.format(DateTimeFormatter.ofPattern("EEEE",new Locale("ar"))));
                    values[i].setText(ar(Math.round(daily.getJSONArray("temperature_2m_max").getDouble(i))+"° / "+Math.round(daily.getJSONArray("temperature_2m_min").getDouble(i))+"°"));
                }catch(Exception e){labels[i].setText(i==0?"جارٍ تحميل الطقس…":"");values[i].setText("");}
                labels[i].setTextColor(palette.ink);values[i].setTextColor(palette.ink);
            } else {
                labels[i].setText(names[i]);values[i].setText(tableTimes==null?"—":time(tableTimes[i]));
                if(tableTimes!=null)tiles[i].update(kw.urtv.shared.TimeTiles.clock(tableTimes[i],options),kw.urtv.shared.TimeTiles.period(tableTimes[i],options),bold,palette.ink,names[i]+" "+time(tableTimes[i]));
                boolean next=tableTimes!=null&&(shown.isAfter(today)||minute<prayers[5])&&i==(shown.isAfter(today)?0:kw.urtv.shared.CalendarData.next(prayers,minute));
                if(next){GradientDrawable highlight=new GradientDrawable();highlight.setColor(palette.selected);highlight.setCornerRadius(dp(8));row.setBackground(highlight);}else row.setBackground(null);
                if(next)highlighted=true;
                labels[i].setTextColor(palette.ink);values[i].setTextColor(palette.ink);
            }
        }
        nextPrayer.setText(prayers==null?prayerSource:"");nextTiles.setVisibility(prayers==null?View.GONE:View.VISIBLE);nextTimeTiles.setVisibility(prayers==null?View.GONE:View.VISIBLE);
        if(prayers!=null){int n=kw.urtv.shared.CalendarData.next(prayers,minute);int target=prayers[n];boolean tomorrow=n==0&&minute>prayers[5];boolean estimated=!official;
            if(tomorrow){kw.urtv.shared.RegionSchedule.Result d=schedule(now.toLocalDate().plusDays(1));if(d.times==null){nextPrayer.setText("المواقيت التالية غير متاحة");nextTimeTiles.setVisibility(View.GONE);nextTiles.setVisibility(View.GONE);return;}target=d.times[0];estimated=!d.verified;}
            int left=kw.urtv.shared.PrayerCountdown.minutes(now,target,tomorrow);
            nextPrayer.setText("التالي · "+names[n]);
            nextTimeTiles.update(kw.urtv.shared.TimeTiles.clock(target,options),kw.urtv.shared.TimeTiles.period(target,options),bold,palette.ink,"موعد "+time(target));
            nextTiles.update(kw.urtv.shared.TimeTiles.remaining(left,options),"",bold,palette.ink,"متبقي "+left+" دقيقة");
        }
        weatherDetails.setText(weatherPlace.name);
        try {
            JSONObject current=weatherData.getJSONObject("current"),daily=weatherData.getJSONObject("daily");
            double dust=weatherData.optDouble("dust",0);
            weatherSymbol.update(current.getInt("weather_code"),dust,current.optInt("is_day",1)==0,palette.ink,palette.light?0xFF8A611E:0xFFE8C780);
            String description=dust>=400?"غبار كثيف":dust>=150?"غبار":weatherName(current.getInt("weather_code"));
            String temperature="\u2066"+ar(Math.round(current.getDouble("temperature_2m"))+"°")+"\u2069";
            SpannableString summary=new SpannableString(temperature+"   "+description);
            summary.setSpan(new ForegroundColorSpan(palette.ink),0,temperature.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            condition.setText(summary);
            condition.setTextColor(dust>=150?palette.accent:palette.ink);
            weatherDetails.setText(weatherPlace.name+"  ·  "+ar(Math.round(daily.getJSONArray("temperature_2m_max").getDouble(0))+"° / "+Math.round(daily.getJSONArray("temperature_2m_min").getDouble(0))+"°"));
            if(lastWeather>0&&System.currentTimeMillis()-lastWeather>3600000L)weatherDetails.append("\n"+ar("آخر تحديث قبل "+((System.currentTimeMillis()-lastWeather)/3600000L)+" ساعة"));
        }catch(Exception e){weatherSymbol.setVisibility(View.GONE);condition.setText("الطقس غير متاح حالياً");}
        for(int i=0;i<6;i++)values[i].setText(options.numbers(values[i].getText().toString()));
        nextPrayer.setText(options.numbers(nextPrayer.getText().toString()));condition.setText(options.numbers(condition.getText().toString()));weatherDetails.setText(options.numbers(weatherDetails.getText().toString()));
    }
    private static String weatherName(int code) {
        switch(code){case 0:return "صافي";case 1:return "صافي غالباً";case 2:return "غائم جزئياً";case 3:return "غائم";case 45:case 48:return "ضباب";case 51:case 53:case 55:case 56:case 57:return "رذاذ";case 61:case 80:return "مطر خفيف";case 63:case 81:return "مطر";case 65:case 82:return "مطر غزير";case 66:case 67:return "مطر متجمد";case 71:case 73:case 75:case 77:case 85:case 86:return "ثلج";case 95:case 96:case 99:return "عواصف رعدية";default:return "";}
    }
    private String time(int minutes){return options.time(minutes);}

    private static String ar(String s) {StringBuilder b=new StringBuilder();for(char c:s.toCharArray())b.append(c>='0'&&c<='9'?(char)('٠'+c-'0'):c);return b.toString();}
    private TextView text(float size,int color) {TextView t=new TextView(activity);t.setTextSize(size);t.setTextColor(color);t.setTypeface(bold);t.setIncludeFontPadding(false);t.setSingleLine(true);t.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);t.setTextDirection(View.TEXT_DIRECTION_RTL);return t;}
    private int dp(float n){return Math.round(n*activity.getResources().getDisplayMetrics().density);}
}
