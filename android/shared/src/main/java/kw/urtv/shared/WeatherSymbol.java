package kw.urtv.shared;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.content.Context;
import android.view.View;

/** Static vector weather marks shared by both displays. No assets, timers or bitmaps. */
public final class WeatherSymbol {
 private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
 private final Path path=new Path();
 public void draw(Canvas c,float x,float y,float size,int code,double dust,boolean night,int ink,int accent){
  c.save();c.translate(x,y);c.scale(size/64f,size/64f);
  p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);p.setColor(ink);
  if(dust>=150||code==45||code==48){
   p.setColor(dust>=150?accent:ink);
   c.drawLine(12,23,49,23,p);c.drawLine(7,33,56,33,p);c.drawLine(16,43,47,43,p);
   c.drawCircle(53,44,1,p);c.drawCircle(9,44,1,p);
  }else if(code==0||code==1){
   p.setColor(accent);
   if(night){path.reset();path.moveTo(40,9);path.cubicTo(9,6,7,48,35,53);path.cubicTo(48,55,56,44,55,37);path.cubicTo(34,43,25,22,40,9);c.drawPath(path,p);}
   else{c.drawCircle(32,32,12,p);for(int i=0;i<8;i++){double a=i*Math.PI/4;c.drawLine(32+(float)Math.cos(a)*20,32+(float)Math.sin(a)*20,32+(float)Math.cos(a)*26,32+(float)Math.sin(a)*26,p);}}
  }else{
   if(code==2){p.setColor(accent);c.drawCircle(20,20,10,p);c.drawLine(20,4,20,1,p);c.drawLine(4,20,1,20,p);p.setColor(ink);}
   path.reset();path.moveTo(15,42);path.cubicTo(0,42,2,22,17,24);path.cubicTo(21,7,45,9,47,27);path.cubicTo(62,25,65,42,50,42);path.close();c.drawPath(path,p);
   if(code>=95){p.setColor(accent);path.reset();path.moveTo(35,44);path.lineTo(28,52);path.lineTo(37,52);path.lineTo(30,61);c.drawPath(path,p);}
   else if((code>=71&&code<=77)||code==85||code==86){for(int i=0;i<3;i++){float a=19+i*13;c.drawLine(a,49,a,57,p);c.drawLine(a-3,51,a+3,55,p);c.drawLine(a-3,55,a+3,51,p);}}
   else if(code>=51){p.setColor(accent);for(int i=0;i<3;i++)c.drawLine(20+i*13,49,17+i*13,56,p);}
  }
  c.restore();
 }
 public static final class SymbolView extends View {
  private final WeatherSymbol symbol=new WeatherSymbol();private int code,ink,accent;private double dust;private boolean night;
  public SymbolView(Context c){super(c);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
  public void update(int code,double dust,boolean night,int ink,int accent){this.code=code;this.dust=dust;this.night=night;this.ink=ink;this.accent=accent;setVisibility(VISIBLE);invalidate();}
  @Override protected void onDraw(Canvas c){float size=Math.min(getWidth(),getHeight());symbol.draw(c,(getWidth()-size)/2,(getHeight()-size)/2,size,code,dust,night,ink,accent);}
 }
}
