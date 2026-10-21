package kw.urtv.screensaver;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextDirectionHeuristics;
import android.text.TextPaint;
import android.view.View;
import org.json.JSONObject;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.chrono.HijrahDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** A native, minute-driven display. One decoded photo; no WebView or browser subprocess. */
final class ScreenView extends View {
    private static final int SAND=0xFFF6EFE0,GOLD=0xFFDCB86F,WHITE=0xFFFFFDF6;
    private static final Locale AR=new Locale("ar","KW");
    private static final String[] BACKGROUNDS={"composed.jpg","water.jpg","dusk.jpg","sunset.jpg"};
    private static final int[][] SHIFTS={{0,0},{3,2},{0,4},{-3,2},{-3,-2},{0,-4},{3,-2}};
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final Typeface medium,regular,display,bold;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final ExecutorService photos=Executors.newSingleThreadExecutor();
    private final Map<String,StaticLayout> layouts=new HashMap<>();
    private final ScreenData data;
    private final Shader veil=new LinearGradient(0,0,0,1080,
            new int[]{0x9E030A07,0x4D030A07,0x1F030A07,0x6B030A07,0xE0020604},
            new float[]{0,.22f,.46f,.74f,1},Shader.TileMode.CLAMP);
    private final Shader bottomVeil=new LinearGradient(0,756,0,1080,
            new int[]{0x00020704,0x8C020704,0xDB020604},new float[]{0,.42f,1},Shader.TileMode.CLAMP);
    private final Shader radial=ellipse(422,367,2304,918,new int[]{0x6B030E09,0x00030E09},new float[]{0,.58f});
    private final Shader topVeil=ellipse(0,0,1290,367,new int[]{0xD1020906,0x6B020906,0x00020906},new float[]{0,.45f,.76f});
    private String pinnedPhoto="";
    private Bitmap background;
    private long photoSlot=Long.MIN_VALUE;
    private boolean destroyed,attached;
    private ZonedDateTime now;
    private String hijri="",gregorian="";
    private final Runnable minute=new Runnable() {
        @Override public void run() {
            if(destroyed||!attached)return;
            now=ZonedDateTime.now(ScreenData.KUWAIT);
            LocalDate today=now.toLocalDate();
            gregorian=PrayerClock.ar(today.format(DateTimeFormatter.ofPattern("EEEE، d MMMM yyyy",AR)));
            hijri=PrayerClock.ar(today.format(DateTimeFormatter.ofPattern("EEEE، ",AR))
                    +HijrahDate.from(today).format(DateTimeFormatter.ofPattern("d MMMM yyyy",AR)))+" هـ";
            layouts.clear();
            updatePhoto();data.tick();invalidate();
            handler.postDelayed(this,60000-System.currentTimeMillis()%60000+30);
        }
    };

    static ScreenView create(Context context){return new ScreenView(context);}
    private ScreenView(Context context) {
        super(context);
        medium=Typeface.createFromAsset(context.getAssets(),"fonts/Thmanyah-Medium.otf");
        regular=Typeface.createFromAsset(context.getAssets(),"fonts/Thmanyah-Regular.otf");
        display=Typeface.createFromAsset(context.getAssets(),"fonts/ThmanyahDisplay-Bold.otf");
        bold=Typeface.create(medium,Typeface.BOLD);
        try(android.database.Cursor c=context.getContentResolver().query(android.net.Uri.parse("content://kw.urtv.home.photos"),null,null,null,null)){if(c!=null&&c.moveToFirst()&&c.getInt(0)==1){String f=c.getString(1);if(java.util.Arrays.asList(BACKGROUNDS).contains(f))pinnedPhoto=f;}}catch(Exception ignored){}
        data=new ScreenData(context,()->{if(!destroyed){layouts.clear();invalidate();}});
        setFocusable(false);setBackgroundColor(0xFF040E09);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();attached=true;minute.run();}
    @Override protected void onDetachedFromWindow(){destroy();super.onDetachedFromWindow();}
    @Override protected void onSizeChanged(int w,int h,int ow,int oh){photoSlot=Long.MIN_VALUE;if(attached)updatePhoto();}
    void destroy() {
        if(destroyed)return;
        destroyed=true;attached=false;handler.removeCallbacksAndMessages(null);
        data.destroy();photos.shutdownNow();layouts.clear();
        if(background!=null){background.recycle();background=null;}
    }
    private static Shader ellipse(float x,float y,float rx,float ry,int[] colors,float[] stops) {
        Shader shader=new RadialGradient(0,0,1,colors,stops,Shader.TileMode.CLAMP);
        Matrix matrix=new Matrix();matrix.setScale(rx,ry);matrix.postTranslate(x,y);shader.setLocalMatrix(matrix);return shader;
    }
    private void updatePhoto() {
        if(getWidth()==0||getHeight()==0||destroyed)return;
        long slot=pinnedPhoto.isEmpty()?System.currentTimeMillis()/1800000L:0;
        if(slot==photoSlot)return;
        photoSlot=slot;
        String name=pinnedPhoto.isEmpty()?BACKGROUNDS[(int)Math.floorMod(slot,BACKGROUNDS.length)]:pinnedPhoto;
        int width=getWidth(),height=getHeight();
        photos.execute(()-> {
            Bitmap decoded=null;
            try {
                BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;
                try(InputStream stream=getContext().getAssets().open("backgrounds/"+name)){BitmapFactory.decodeStream(stream,null,options);}
                float target=Math.max(width*1.03f/options.outWidth,height*1.03f/options.outHeight);
                int sample=1;while(sample*2<=1/target)sample*=2;
                options.inJustDecodeBounds=false;options.inSampleSize=sample;options.inPreferredConfig=Bitmap.Config.ARGB_8888;
                try(InputStream stream=getContext().getAssets().open("backgrounds/"+name)){decoded=BitmapFactory.decodeStream(stream,null,options);}
                if(decoded==null)return;
                float factor=Math.max(width*1.03f/decoded.getWidth(),height*1.03f/decoded.getHeight());
                Bitmap sized=Bitmap.createScaledBitmap(decoded,Math.round(decoded.getWidth()*factor),Math.round(decoded.getHeight()*factor),true);
                if(sized!=decoded)decoded.recycle();decoded=null;
                handler.post(()-> {
                    if(destroyed||photoSlot!=slot){sized.recycle();return;}
                    Bitmap old=background;background=sized;invalidate();if(old!=null)old.recycle();
                });
            }catch(Exception ignored){if(decoded!=null)decoded.recycle();}
        });
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(destroyed||now==null)return;
        float scale=Math.min(getWidth()/1920f,getHeight()/1080f);
        canvas.save();canvas.translate((getWidth()-1920*scale)/2,(getHeight()-1080*scale)/2);canvas.scale(scale,scale);
        canvas.drawColor(0xFF040E09);
        paint.setShader(null);paint.setColor(Color.WHITE);
        if(background!=null) {
            float w=background.getWidth()/scale,h=background.getHeight()/scale;
            canvas.drawBitmap(background,null,new RectF((1920-w)/2,(1080-h)*.26f,(1920+w)/2,(1080-h)*.26f+h),paint);
            paint.setColor(0x0A000000);canvas.drawRect(0,0,1920,1080,paint);
        }
        for(Shader shader:new Shader[]{veil,radial,topVeil,bottomVeil}){paint.setShader(shader);paint.setColor(Color.WHITE);canvas.drawRect(0,0,1920,1080,paint);}
        paint.setShader(null);
        int[] shift=SHIFTS[(int)(System.currentTimeMillis()/240000L%SHIFTS.length)];
        canvas.save();canvas.translate(shift[0],shift[1]);
        text(canvas,data.official?kw.urtv.shared.CalendarData.ar(data.hijri)+" هـ":hijri,1040,34,812,36,medium,SAND,Layout.Alignment.ALIGN_NORMAL);
        text(canvas,gregorian,1050,90,802,23,regular,0xFFC3B8A2,Layout.Alignment.ALIGN_NORMAL);
        line(canvas,1678,137,1852,137,0xFFDCB86F,1);
        boolean table=!"none".equals(data.panel);
        if(table)drawTable(canvas);
        float center=table?1467:960;
        drawClock(canvas,center);
        text(canvas,"بتوقيت "+data.city,center-390,545,780,23,regular,0xFFD8CDB6,Layout.Alignment.ALIGN_CENTER);
        paint.setColor(GOLD);canvas.drawCircle(center+160,558,5,paint);
        drawWeather(canvas,center);
        drawNextPrayer(canvas);
        canvas.restore();
        double hour=now.getHour()+now.getMinute()/60.0;
        double opacity=hour>=23||hour<5?.46:hour>=21?.46*(hour-21)/2:hour<7?.46*(7-hour)/2:0;
        if(opacity>0){paint.setColor(Color.argb((int)Math.round(Math.min(.46,opacity)*255),0,0,0));canvas.drawRect(0,0,1920,1080,paint);}
        canvas.restore();
    }
    private void drawTable(Canvas canvas) {
        RectF box=new RectF(76,181,996,773);
        paint.setStyle(Paint.Style.FILL);paint.setColor(0xD9091810);canvas.drawRoundRect(box,24,24,paint);
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2);paint.setColor(0x29DCB86F);canvas.drawRoundRect(box,24,24,paint);paint.setStyle(Paint.Style.FILL);
        boolean forecast="forecast".equals(data.panel);
        text(canvas,forecast?"الأيام القادمة":data.official?"مواقيت اليوم · العجيري":"مواقيت اليوم · محسوبة",350,203,618,32,bold,0xFFD6AF6B,Layout.Alignment.ALIGN_NORMAL);
        line(canvas,104,244,968,244,0x80DCB86F,1);
        String[] names=prayerNames();int minute=now.getHour()*60+now.getMinute();boolean highlighted=false;
        for(int i=0;i<(forecast?5:6);i++) {
            float top=264+i*83.5f;
            String label=names[i],value=data.prayers==null?"—":PrayerClock.time(data.prayers[i]);
            boolean next=!forecast&&data.prayers!=null&&i==kw.urtv.shared.CalendarData.next(data.prayers,minute);
            if(next){paint.setColor(0x383F8D65);canvas.drawRoundRect(new RectF(90,top-4,982,top+66),12,12,paint);}
            if(next)highlighted=true;
            int color=next?0xFFFFE2A0:SAND;
            if(forecast) {
                try {
                    JSONObject daily=data.weatherData.getJSONObject("daily");
                    LocalDate date=LocalDate.parse(daily.getJSONArray("time").getString(i));
                    label=i==0?"اليوم":i==1?"غداً":date.format(DateTimeFormatter.ofPattern("EEEE",AR));
                    value=PrayerClock.ar(Math.round(daily.getJSONArray("temperature_2m_max").getDouble(i))+"° / "+Math.round(daily.getJSONArray("temperature_2m_min").getDouble(i))+"°");
                    text(canvas,ScreenData.weatherName(daily.getJSONArray("weather_code").getInt(i)),365,top+13,300,25,regular,0xFF9FB8A8,Layout.Alignment.ALIGN_CENTER);
                }catch(Exception e){label=i==0?"جارٍ تحميل الطقس…":"";value="";}
            }
            text(canvas,label,670,top+3,298,40,bold,color,Layout.Alignment.ALIGN_NORMAL);
            text(canvas,value,104,top,forecast?260:450,forecast?36:48,bold,next?color:WHITE,Layout.Alignment.ALIGN_OPPOSITE);
            if(i<(forecast?4:5))line(canvas,104,top+70,968,top+70,0x21DCB86F,1);
        }
    }
    private void drawClock(Canvas canvas,float center) {
        String hour=PrayerClock.ar(""+(now.getHour()%12==0?12:now.getHour()%12));
        String minute=PrayerClock.ar(String.format(Locale.ROOT,"%02d",now.getMinute()));
        TextPaint digits=new TextPaint(Paint.ANTI_ALIAS_FLAG);digits.setTypeface(display);digits.setTextSize(326);
        float gap=26,colonSize=150,colonWidth=70;
        float hours=digits.measureText(hour)+(hour.length()-1)*gap;
        float minutes=digits.measureText(minute)+(minute.length()-1)*gap;
        float amWidth=110,total=hours+minutes+colonWidth+40+amWidth+14;
        float factor=Math.min(1,("none".equals(data.panel)?1250:774)/total);
        canvas.save();canvas.translate(center-total*factor/2,365*(1-factor));canvas.scale(factor,factor);
        float left=amWidth+14,baseline=486;
        digits.setColor(WHITE);digits.setShadowLayer(8,0,4,0x99000000);
        float cursor=left;
        for(char ch:hour.toCharArray()){String value=""+ch;canvas.drawText(value,cursor,baseline,digits);cursor+=digits.measureText(value)+gap;}
        cursor-=gap;
        digits.setColor(GOLD);digits.setTextSize(colonSize);canvas.drawText(":",cursor+14,baseline-70,digits);
        cursor+=colonWidth+40;digits.setColor(WHITE);digits.setTextSize(326);
        for(char ch:minute.toCharArray()){String value=""+ch;canvas.drawText(value,cursor,baseline,digits);cursor+=digits.measureText(value)+gap;}
        text(canvas,now.getHour()<12?"ص":"م",0,214,amWidth,92,medium,GOLD,Layout.Alignment.ALIGN_CENTER);
        canvas.restore();
    }
    private void drawWeather(Canvas canvas,float center) {
        try {
            JSONObject current=data.weatherData.getJSONObject("current"),daily=data.weatherData.getJSONObject("daily");
            double dust=data.weatherData.optDouble("dust",0);
            String description=dust>=400?"غبار كثيف":dust>=150?"غبار":ScreenData.weatherName(current.getInt("weather_code"));
            String temp=PrayerClock.ar(""+Math.round(current.getDouble("temperature_2m")))+"°";
            TextPaint measure=new TextPaint(Paint.ANTI_ALIAS_FLAG);measure.setTypeface(display);measure.setTextSize(48);
            float tempWidth=measure.measureText(temp)+12;
            measure.setTypeface(bold);float descWidth=measure.measureText(description)+12;
            float left=center-(tempWidth+descWidth+24)/2;
            text(canvas,description,left,607,descWidth,48,bold,dust>=150?0xFFE8B86A:SAND,Layout.Alignment.ALIGN_CENTER);
            text(canvas,"\u2066"+temp+"\u2069",left+descWidth+24,607,tempWidth,48,display,0xFFFFFCF2,Layout.Alignment.ALIGN_CENTER);
            String range=PrayerClock.ar(Math.round(daily.getJSONArray("temperature_2m_max").getDouble(0))+"° / "+Math.round(daily.getJSONArray("temperature_2m_min").getDouble(0))+"°");
            text(canvas,data.city+"  ·  "+range,center-420,671,840,32,medium,0xFFE2D7C2,Layout.Alignment.ALIGN_CENTER);
        }catch(Exception e){text(canvas,"الطقس غير متاح حالياً",center-420,625,840,32,medium,0xFFE2D7C2,Layout.Alignment.ALIGN_CENTER);text(canvas,data.city,center-420,679,840,24,regular,0xFFE2D7C2,Layout.Alignment.ALIGN_CENTER);}
        if(data.weatherStale())text(canvas,"آخر قراءة محفوظة · تعذر تحديث الطقس",center-420,715,840,22,regular,0xFFE8B86A,Layout.Alignment.ALIGN_CENTER);
        line(canvas,center-205,752,center+205,752,0x80DCB86F,1);
    }
    private String[] prayerNames(){return new String[]{"الفجر","الشروق",now.getDayOfWeek().getValue()==5?"الجمعة":"الظهر","العصر","المغرب","العشاء"};}
    private void drawNextPrayer(Canvas canvas) {
        line(canvas,76,815,1854,815,0x4DDCB86F,1);
        text(canvas,data.hijri.contains("رمضان")&&now.getHour()*60+now.getMinute()<data.prayers[4]?"رمضان · موعد الإفطار "+PrayerClock.time(data.prayers[4]):"الصلاة القادمة",900,842,954,32,bold,0xFFD6AF6B,Layout.Alignment.ALIGN_NORMAL);
        if(data.prayers==null)return;
        int[] tomorrow=data.tomorrow();
        int[] next=PrayerClock.next(data.prayers,tomorrow,now.getHour()*60+now.getMinute());
        text(canvas,prayerNames()[next[0]]+"  "+PrayerClock.time(next[1]),76,890,1778,69,display,SAND,Layout.Alignment.ALIGN_NORMAL);
        text(canvas,PrayerClock.remaining(next[2])+((!data.official||(now.getHour()*60+now.getMinute()>data.prayers[5]&&!data.tomorrowOfficial()))?" · توقيت تقديري":""),76,982,1778,42,bold,SAND,Layout.Alignment.ALIGN_NORMAL);
    }
    private void line(Canvas canvas,float x,float y,float endX,float endY,int color,float width) {
        paint.setShader(null);paint.setColor(color);paint.setStrokeWidth(width);canvas.drawLine(x,y,endX,endY,paint);
    }
    private void text(Canvas canvas,String text,float x,float y,float width,float size,Typeface font,int color,Layout.Alignment align) {
        String key=text+":"+width+":"+size+":"+font.hashCode()+":"+color+":"+align;
        StaticLayout layout=layouts.get(key);
        if(layout==null) {
            TextPaint p=new TextPaint(Paint.ANTI_ALIAS_FLAG);p.setTypeface(font);p.setTextSize(size);p.setColor(color);p.setTextLocale(AR);
            layout=StaticLayout.Builder.obtain(text,0,text.length(),p,Math.max(1,(int)Math.ceil(width)))
                    .setAlignment(align).setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_RTL)
                    .setIncludePad(false).setMaxLines(1).build();
            if(layouts.size()>100)layouts.clear();layouts.put(key,layout);
        }
        canvas.save();canvas.translate(x,y);layout.draw(canvas);canvas.restore();
    }
}
