package kw.urtv.shared;
import android.content.Context;
import android.graphics.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
/** Shared, static native composition: no timers, network, photos or animations. */
public final class SimplePrayerFace {
 private final Paint p=new Paint(3);private final Typeface font,bold;
 private final TimeTiles.GlassPainter tiles=new TimeTiles.GlassPainter();
 private static final String[] NAMES={"الفجر","الشروق","الظهر","العصر","المغرب","العشاء"};
 public SimplePrayerFace(Context c){font=Typeface.createFromAsset(c.getAssets(),"fonts/Thmanyah-Medium.otf");bold=Typeface.create(font,Typeface.BOLD);}
 private void text(Canvas c,String value,float x,float baseline,float size,int color,Paint.Align align,float max){p.setColor(color);p.setTypeface(font);p.setTextSize(size);p.setTextAlign(align);float w=p.measureText(value);if(w>max)p.setTextSize(size*max/w);c.drawText(value,x,baseline,p);}
 public void draw(Canvas c,float width,float height,DisplayOptions o,Palette colors,ZonedDateTime now,String city,int[] today,int[] shownTimes,LocalDate shown,int[] tomorrow){
  c.save();c.scale(width/1760f,height/900f);
  int ink=colors.ink,muted=colors.muted;int minute=now.getHour()*60+now.getMinute();
  // Two equal visual groups with one aligned baseline and no reserved weather/match slots.
  text(c,PrayerTableDay.heading(now.toLocalDate(),shown)+" · "+city,790,80,32,ink,Paint.Align.RIGHT,710);
  text(c,o.numbers(PrayerTableDay.dateLabel(shown)),790,126,25,muted,Paint.Align.RIGHT,710);
  int next=-1,target=0;boolean nextDay=false;
  if(today!=null){for(int i:new int[]{0,2,3,4,5})if(today[i]>minute){next=i;target=today[i];break;}if(next<0&&tomorrow!=null){next=0;target=tomorrow[0];nextDay=true;}}
  if(o.prayersEnabled)for(int i=0;i<6;i++){
   float y=166+i*112;boolean selected=shownTimes!=null&&i==(shown.isAfter(now.toLocalDate())?0:nextDay?-1:next);
   if(selected){p.setColor(colors.selected);c.drawRoundRect(40,y,820,y+100,18,18,p);}
   String label=i==2&&shown.getDayOfWeek().getValue()==5?"الجمعة":NAMES[i];
   text(c,label,774,y+64,36*o.prayerScale(),ink,Paint.Align.RIGHT,240);
   tiles.draw(c,shownTimes==null?"--:--":TimeTiles.clock(shownTimes[i],o),shownTimes==null?"":TimeTiles.period(shownTimes[i],o),64,y+9,380,82,bold,ink,.68f*o.prayerScale(),colors.light,o.contrast,true);
  }
  p.setColor(colors.light?0x28233D49:0x28FFFFFF);p.setStrokeWidth(2);c.drawLine(880,62,880,830,p);
  String clock=TimeTiles.clock(minute,o).replaceFirst("^[0٠]","");
  text(c,clock,1320,310,210,ink,Paint.Align.CENTER,730);
  text(c,TimeTiles.period(minute,o),1320,365,28,muted,Paint.Align.CENTER,100);
  text(c,o.numbers(now.format(DateTimeFormatter.ofPattern("EEEE، d MMMM yyyy",new Locale("ar")))),1320,422,28,muted,Paint.Align.CENTER,740);
  if(o.prayersEnabled){
   text(c,next<0?"المواقيت غير متاحة":"التالي · "+NAMES[next],1680,574,34,ink,Paint.Align.RIGHT,680);
   tiles.draw(c,next<0?"--:--":TimeTiles.clock(target,o),next<0?"":TimeTiles.period(target,o),966,512,380,90,bold,ink,.68f*o.nextScale(),colors.light,o.contrast,true);
   p.setColor(colors.light?0x28233D49:0x28FFFFFF);c.drawLine(966,642,1680,642,p);
   text(c,"الوقت المتبقي",1680,755,32,muted,Paint.Align.RIGHT,680);
   tiles.draw(c,next<0?"--:--":TimeTiles.remaining(PrayerCountdown.minutes(now,target,nextDay),o),"",966,694,380,90,bold,ink,.68f*o.nextScale(),colors.light,o.contrast,true);
  }
  c.restore();
 }
}
