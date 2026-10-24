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
    private static final int SAND=0xFFF1F5FA,GOLD=0xFF8EDBEA,WHITE=0xFFF1F5FA;
    private static final Locale AR=new Locale("ar","KW");
    private static final String[] BACKGROUNDS={"composed.jpg","water.jpg","dusk.jpg","sunset.jpg"};
    private static final int[][] SHIFTS={{0,0},{3,2},{0,4},{-3,2},{-3,-2},{0,-4},{3,-2}};
    private final kw.urtv.shared.WeatherSymbol weatherSymbol=new kw.urtv.shared.WeatherSymbol();
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final Typeface medium,regular,display,bold;
    private final kw.urtv.shared.TimeTiles.GlassPainter glassDigits=new kw.urtv.shared.TimeTiles.GlassPainter();
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final ExecutorService photos=Executors.newSingleThreadExecutor();
    private final Map<String,StaticLayout> layouts=new HashMap<>();
    private final ScreenData data;
    private final kw.urtv.shared.FootballBoard football;
    private final kw.urtv.shared.Palette palette;private final kw.urtv.shared.DisplayOptions options;
    private int imageEdge;
    private final kw.urtv.shared.FocusBackdrop focusBackdrop;
    private final Shader veil=new LinearGradient(0,0,0,1080,
            new int[]{0xDB101D2D,0x87101D2D,0x50101D2D,0x45101D2D,0xE8101D2D},
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
            now=ZonedDateTime.now(data.zone);
            LocalDate today=now.toLocalDate();
            gregorian=PrayerClock.ar(today.format(DateTimeFormatter.ofPattern("EEEE، d MMMM yyyy",AR)));
            hijri=PrayerClock.ar(today.format(DateTimeFormatter.ofPattern("EEEE، ",AR))
                    +HijrahDate.from(today).format(DateTimeFormatter.ofPattern("d MMMM yyyy",AR)))+" هـ";
            layouts.clear();
            updatePhoto();data.tick();if(options.saverMatches)football.refresh();updateDescription();invalidate();
            handler.postDelayed(this,60000-System.currentTimeMillis()%60000+30);
        }
    };

    static ScreenView create(Context context){return new ScreenView(context);}
    private ScreenView(Context context) {
        super(context);
        options=kw.urtv.shared.DisplayOptions.load(context);palette=new kw.urtv.shared.Palette(context,options);imageEdge=options.device(context).edge;focusBackdrop=new kw.urtv.shared.FocusBackdrop(options.focusColor);focusBackdrop.setBounds(0,0,1920,1080);
        medium=Typeface.createFromAsset(context.getAssets(),"fonts/Thmanyah-Medium.otf");
        regular=Typeface.createFromAsset(context.getAssets(),"fonts/Thmanyah-Regular.otf");
        display=Typeface.createFromAsset(context.getAssets(),"fonts/ThmanyahDisplay-Bold.otf");
        bold=Typeface.create(medium,Typeface.BOLD);
        try(android.database.Cursor c=context.getContentResolver().query(android.net.Uri.parse("content://kw.urtv.home.photos"),null,null,null,null)){if(c!=null&&c.moveToFirst()&&c.getInt(0)==1){String f=c.getString(1);if(java.util.Arrays.asList(BACKGROUNDS).contains(f))pinnedPhoto=f;}}catch(Exception ignored){}
        football=new kw.urtv.shared.FootballBoard(context,this::invalidate);
        data=new ScreenData(context,()->{if(!destroyed){layouts.clear();updateDescription();invalidate();}});
        setFocusable(false);setBackgroundColor(0xFF040E09);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
    }
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();attached=true;minute.run();}
    @Override protected void onDetachedFromWindow(){destroy();super.onDetachedFromWindow();}
    @Override protected void onSizeChanged(int w,int h,int ow,int oh){photoSlot=Long.MIN_VALUE;if(attached)updatePhoto();}
    void destroy() {
        if(destroyed)return;
        destroyed=true;attached=false;handler.removeCallbacksAndMessages(null);
        football.close();data.destroy();photos.shutdownNow();layouts.clear();
        if(background!=null){background.recycle();background=null;}
    }
    private static Shader ellipse(float x,float y,float rx,float ry,int[] colors,float[] stops) {
        Shader shader=new RadialGradient(0,0,1,colors,stops,Shader.TileMode.CLAMP);
        Matrix matrix=new Matrix();matrix.setScale(rx,ry);matrix.postTranslate(x,y);shader.setLocalMatrix(matrix);return shader;
    }
    private void updatePhoto() {
        if(options.focused()||getWidth()==0||getHeight()==0||destroyed)return;
        long slot=pinnedPhoto.isEmpty()?System.currentTimeMillis()/1800000L:0;
        if(slot==photoSlot)return;
        photoSlot=slot;
        String name=pinnedPhoto.isEmpty()?BACKGROUNDS[(int)Math.floorMod(slot,BACKGROUNDS.length)]:pinnedPhoto;
        float cap=Math.min(1f,imageEdge/(float)Math.max(getWidth(),getHeight()));
        int width=Math.round(getWidth()*cap),height=Math.round(getHeight()*cap);
        photos.execute(()-> {
            Bitmap decoded=null;
            try {
                BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;
                try(InputStream stream=getContext().getAssets().open("backgrounds/"+name)){BitmapFactory.decodeStream(stream,null,options);}
                float target=Math.max(width/(float)options.outWidth,height/(float)options.outHeight);
                int sample=1;while(sample*2<=1/target)sample*=2;
                options.inJustDecodeBounds=false;options.inSampleSize=sample;options.inPreferredConfig=imageEdge<=1920?Bitmap.Config.RGB_565:Bitmap.Config.ARGB_8888;
                try(InputStream stream=getContext().getAssets().open("backgrounds/"+name)){decoded=BitmapFactory.decodeStream(stream,null,options);}
                if(decoded==null)return;
                float factor=Math.max(width/(float)decoded.getWidth(),height/(float)decoded.getHeight());
                Bitmap sized=Bitmap.createScaledBitmap(decoded,Math.round(decoded.getWidth()*factor),Math.round(decoded.getHeight()*factor),true);
                if(sized!=decoded)decoded.recycle();decoded=null;
                handler.post(()-> {
                    if(destroyed||photoSlot!=slot){sized.recycle();return;}
                    Bitmap old=background;background=sized;invalidate();if(old!=null)old.recycle();
                });
            }catch(OutOfMemoryError low){if(decoded!=null)decoded.recycle();if(imageEdge>1280)handler.post(this::memoryPressure);}
            catch(Exception ignored){if(decoded!=null)decoded.recycle();}
        });
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(destroyed||now==null)return;
        float scale=Math.min(getWidth()/1920f,getHeight()/1080f);
        canvas.save();canvas.translate((getWidth()-1920*scale)/2,(getHeight()-1080*scale)/2);canvas.scale(scale,scale);
        paint.setShader(null);
        canvas.drawColor(palette.light?0xFFF2F2F7:0xFF121316);
        if(options.focused())focusBackdrop.draw(canvas);
        if(!options.focused()&&background!=null){
            paint.setColor(Color.WHITE);float factor=Math.max(1920f/background.getWidth(),1080f/background.getHeight());float w=background.getWidth()*factor,h=background.getHeight()*factor;
            canvas.drawBitmap(background,null,new RectF((1920-w)/2,(1080-h)/2,(1920+w)/2,(1080+h)/2),paint);
            paint.setShader(veil);canvas.drawRect(0,0,1920,1080,paint);paint.setShader(null);
        }
        if(options.overlayAlpha()>0){paint.setColor(Color.argb(options.overlayAlpha(),0,0,0));canvas.drawRect(0,0,1920,1080,paint);}
        int[] shift=SHIFTS[(int)(System.currentTimeMillis()/240000L%SHIFTS.length)];
        canvas.save();canvas.translate(shift[0],shift[1]);

        if(options.focused()){
            text(canvas,gregorian,rightX(),350,rightWidth(),28,regular,SAND,Layout.Alignment.ALIGN_CENTER);
            drawClock(canvas,rightX()+rightWidth()/2);drawWeather(canvas,rightX()+rightWidth()/2);drawNextPrayer(canvas);
            if(!"none".equals(data.panel))drawTable(canvas);
            if(options.saverMatches)football.draw(canvas,rightX(),806,rightWidth(),126);
        }else{
            // Readable from across the room: enlarged side groups use the full height.
            canvas.save();canvas.translate(-24,-50);canvas.scale(1.06f,1.06f);
            if(!"none".equals(data.panel))drawTable(canvas);canvas.restore();
            canvas.save();canvas.translate(-90,-50);canvas.scale(1.06f,1.06f);
            text(canvas,gregorian,rightX(),350,rightWidth(),28,regular,SAND,Layout.Alignment.ALIGN_CENTER);
            drawClock(canvas,rightX()+rightWidth()/2);drawWeather(canvas,rightX()+rightWidth()/2);drawNextPrayer(canvas);
            if(options.saverMatches)football.draw(canvas,rightX(),806,rightWidth(),126);canvas.restore();
        }
        canvas.restore();
        double hour=now.getHour()+now.getMinute()/60.0;
        double opacity=hour>=23||hour<5?.46:hour>=21?.46*(hour-21)/2:hour<7?.46*(7-hour)/2:0;
        if(opacity>0&&!palette.light){paint.setColor(Color.argb((int)Math.round(Math.min(.46,opacity)*255),0,0,0));canvas.drawRect(0,0,1920,1080,paint);}
        canvas.restore();
    }
    private float rightX(){return options.focused()?980:1110;}
    private float rightWidth(){return options.focused()?860:730;}
    private int glassPanel(){return options.focused()?Color.TRANSPARENT:options.contrast?palette.panel:(palette.light?0xD0F5F8FA:0x70101D2D);}
    private void glassTime(Canvas c,String digits,String period,float x,float y,float width,float height,float scale){
        glassDigits.draw(c,digits,period,x,y,width,height,options.heavy?bold:medium,palette.ink,.68f*scale,palette.light,options.contrast,true);
    }
    private void drawTable(Canvas canvas){
        paint.setColor(glassPanel());canvas.drawRoundRect(new RectF(80,174,options.focused()?940:780,options.focused()?940:918),24,24,paint);
        int minute=now.getHour()*60+now.getMinute();LocalDate today=now.toLocalDate(),shown=data.prayers==null?today:kw.urtv.shared.PrayerTableDay.select(today,minute,data.prayers[5]);
        int[] times=data.tableTimes(shown);boolean forecast="forecast".equals(data.panel);
        text(canvas,forecast?"الأيام القادمة":kw.urtv.shared.PrayerTableDay.heading(today,shown)+" · "+data.city,110,208,options.focused()?796:636,28,medium,GOLD,Layout.Alignment.ALIGN_NORMAL);
        text(canvas,kw.urtv.shared.PrayerTableDay.dateLabel(shown),110,254,options.focused()?796:636,24,regular,SAND,Layout.Alignment.ALIGN_NORMAL);
        String[] names=prayerNames();names[2]=shown.getDayOfWeek().getValue()==5?"الجمعة":"الظهر";
        for(int i=0;i<(forecast?5:6);i++){
            float y=312+i*(options.focused()?108:96);boolean next=times!=null&&(shown.isAfter(today)||minute<data.prayers[5])&&i==(shown.isAfter(today)?0:kw.urtv.shared.CalendarData.next(data.prayers,minute));
            if(next&&!forecast){paint.setColor(palette.selected);canvas.drawRoundRect(new RectF(104,y-8,options.focused()?916:756,y+80),16,16,paint);}
            String label=names[i],value="—";
            if(forecast)try{JSONObject d=data.weatherData.getJSONObject("daily");label=i==0?"اليوم":i==1?"غدًا":LocalDate.parse(d.getJSONArray("time").getString(i)).format(DateTimeFormatter.ofPattern("EEEE",AR));value=options.numbers(Math.round(d.getJSONArray("temperature_2m_max").getDouble(i))+"° / "+Math.round(d.getJSONArray("temperature_2m_min").getDouble(i))+"°");}catch(Exception e){label="—";}
            text(canvas,label,options.focused()?588:490,y+15,options.focused()?312:232,34*options.prayerScale(),medium,WHITE,options.focused()?Layout.Alignment.ALIGN_CENTER:Layout.Alignment.ALIGN_NORMAL);
            if(forecast)text(canvas,value,132,y+15,320,34*options.prayerScale(),medium,WHITE,Layout.Alignment.ALIGN_CENTER);
            else glassTime(canvas,times==null?"--:--":kw.urtv.shared.TimeTiles.clock(times[i],options),times==null?"":kw.urtv.shared.TimeTiles.period(times[i],options),132,y,320,72,options.prayerScale());
        }
    }
    private void drawClock(Canvas canvas,float center){
        int m=now.getHour()*60+now.getMinute();String clock=kw.urtv.shared.TimeTiles.clock(m,options);
        text(canvas,clock.replaceFirst("^[0٠]", ""),rightX(),178,rightWidth(),options.focused()?116:142,options.usesLatin()?medium:display,WHITE,Layout.Alignment.ALIGN_CENTER);
        TextPaint clockMeasure=new TextPaint();clockMeasure.setTypeface(options.usesLatin()?bold:display);clockMeasure.setTextSize(options.focused()?116:142);float clockWidth=clockMeasure.measureText(clock.replaceFirst("^[0٠]", ""));
        text(canvas,kw.urtv.shared.TimeTiles.period(m,options),center+clockWidth/2+8,options.focused()?260:280,60,30,medium,GOLD,Layout.Alignment.ALIGN_CENTER);
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
            float left=center-(tempWidth+descWidth+112)/2;
            weatherSymbol.draw(canvas,left+descWidth+tempWidth+48,418,56,current.getInt("weather_code"),dust,current.optInt("is_day",1)==0,palette.ink,palette.light?0xFF8A611E:0xFFE8C780);
            text(canvas,description,left,424,descWidth,40,bold,GOLD,Layout.Alignment.ALIGN_CENTER);
            text(canvas,"\u2066"+temp+"\u2069",left+descWidth+24,424,tempWidth,40,options.heavy?bold:medium,WHITE,Layout.Alignment.ALIGN_CENTER);
            String range=PrayerClock.ar(Math.round(daily.getJSONArray("temperature_2m_max").getDouble(0))+"° / "+Math.round(daily.getJSONArray("temperature_2m_min").getDouble(0))+"°");
            text(canvas,data.city+"  ·  "+range,center-420,486,840,26*options.textScale(),medium,0xFFB9CADA,Layout.Alignment.ALIGN_CENTER);
        }catch(Exception e){text(canvas,"الطقس غير متاح حالياً",center-420,424,840,32,medium,0xFFE2D7C2,Layout.Alignment.ALIGN_CENTER);text(canvas,data.city,center-420,486,840,24,regular,0xFFE2D7C2,Layout.Alignment.ALIGN_CENTER);}
        if(data.weatherStale())text(canvas,"آخر قراءة محفوظة · تعذر تحديث الطقس",center-420,530,840,22,regular,0xFFE8B86A,Layout.Alignment.ALIGN_CENTER);
        line(canvas,center-205,560,center+205,560,palette.light?0x40334E60:0x40B9CADA,1);
    }
    private String[] prayerNames(){return new String[]{"الفجر","الشروق",now.getDayOfWeek().getValue()==5?"الجمعة":"الظهر","العصر","المغرب","العشاء"};}
    private void drawNextPrayer(Canvas canvas){
        paint.setColor(glassPanel());canvas.drawRoundRect(new RectF(rightX(),590,1840,766),24,24,paint);
        if(data.prayers==null||data.tomorrow()==null){text(canvas,data.prayerSource,1080,720,730,32,medium,GOLD,Layout.Alignment.ALIGN_NORMAL);return;}
        int[] next=PrayerClock.next(data.prayers,data.tomorrow(),now.getHour()*60+now.getMinute());
        next[2]=kw.urtv.shared.PrayerCountdown.minutes(now,next[1],next[0]==0&&now.getHour()*60+now.getMinute()>=data.prayers[5]);
        text(canvas,"التالي · "+prayerNames()[next[0]],rightX()+rightWidth()/2,612,rightWidth()/2,29*options.nextScale(),medium,GOLD,Layout.Alignment.ALIGN_CENTER);
        text(canvas,"الوقت المتبقي",rightX(),612,rightWidth()/2,28*options.nextScale(),medium,SAND,Layout.Alignment.ALIGN_CENTER);
        glassTime(canvas,kw.urtv.shared.TimeTiles.clock(next[1],options),kw.urtv.shared.TimeTiles.period(next[1],options),rightX()+rightWidth()/2+16,670,rightWidth()/2-32,78,options.nextScale());
        glassTime(canvas,kw.urtv.shared.TimeTiles.remaining(next[2],options),"",rightX()+16,670,rightWidth()/2-32,78,options.nextScale());
    }
    private void line(Canvas canvas,float x,float y,float endX,float endY,int color,float width) {
        paint.setShader(null);paint.setColor(color);paint.setStrokeWidth(width);canvas.drawLine(x,y,endX,endY,paint);
    }
    private void updateDescription(){
        if(data==null||data.prayers==null||now==null)return;
        LocalDate today=now.toLocalDate(),shown=kw.urtv.shared.PrayerTableDay.select(today,now.getHour()*60+now.getMinute(),data.prayers[5]);
        StringBuilder b=new StringBuilder(options.time(now.getHour()*60+now.getMinute())).append(" · ").append(data.city);
        String[] names=prayerNames();names[2]=shown.getDayOfWeek().getValue()==5?"الجمعة":"الظهر";
        if("prayers".equals(data.panel)){b.append("\n").append(kw.urtv.shared.PrayerTableDay.heading(today,shown)).append(" · ").append(options.numbers(kw.urtv.shared.PrayerTableDay.dateLabel(shown)));int[] times=data.tableTimes(shown);if(times!=null)for(int i=0;i<6;i++)b.append("\n").append(names[i]).append(" ").append(options.time(times[i]));}
        setContentDescription(b.toString());
    }
    void memoryPressure(){kw.urtv.shared.DisplayOptions.memoryPressure=true;imageEdge=1280;photoSlot=Long.MIN_VALUE;if(attached)updatePhoto();}
    private void text(Canvas canvas,String text,float x,float y,float width,float size,Typeface font,int color,Layout.Alignment align) {
        color=palette.text(color);text=options.numbers(text);if(options.usesLatin()&&font==display)font=bold;if(options.heavy&&(font==regular||font==medium))font=bold;
        TextPaint measure=new TextPaint();measure.setTypeface(font);measure.setTextSize(size);float needed=measure.measureText(text);if(needed>width)size*=width/needed;
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
