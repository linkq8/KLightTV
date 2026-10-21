package kw.urtv.shared;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import java.util.Locale;

/** Static split-clock digits. No animation, timers, bitmap allocations or RTL reordering. */
public final class TimeTiles {
    private TimeTiles() {}
    public static String clock(int minutes, DisplayOptions o) {
        int h=minutes/60;
        return o.numbers(String.format(Locale.ROOT,"%02d:%02d",o.hour24?h:(h%12==0?12:h%12),minutes%60));
    }
    public static String remaining(int minutes,DisplayOptions o) {
        return o.numbers(String.format(Locale.ROOT,"%02d:%02d",Math.max(0,minutes)/60,Math.max(0,minutes)%60));
    }
    public static String period(int minutes,DisplayOptions o){return o.hour24?"":minutes<720?"ص":"م";}
    public static void draw(Canvas c,String digits,String suffix,float x,float y,float height,Typeface font,int color){
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setTypeface(font);p.setTextAlign(Paint.Align.CENTER);
        float w=height*.60f,gap=height*.075f,cursor=x;
        for(char ch:digits.toCharArray()){
            if(ch==':'){
                p.setColor(color);float cx=cursor+height*.105f;
                c.drawCircle(cx,y+height*.37f,height*.045f,p);c.drawCircle(cx,y+height*.65f,height*.045f,p);
                cursor+=height*.21f;continue;
            }
            p.setColor(0xA8202C39);c.drawRoundRect(cursor,y,cursor+w,y+height,height*.07f,height*.07f,p);
            p.setColor(color);p.setTextSize(height*.77f);Paint.FontMetrics fm=p.getFontMetrics();
            c.drawText(String.valueOf(ch),cursor+w/2,y+height/2-(fm.ascent+fm.descent)/2,p);
            p.setColor(0x33060D16);p.setStrokeWidth(Math.max(1,height*.009f));c.drawLine(cursor,y+height/2,cursor+w,y+height/2,p);
            cursor+=w+gap;
        }
        if(!suffix.isEmpty()){
            float sw=height*.60f;p.setColor(0xA8202C39);c.drawRoundRect(cursor,y,cursor+sw,y+height,height*.07f,height*.07f,p);
            p.setTextSize(height*.50f);float labelWidth=p.measureText(suffix);if(labelWidth>sw*.78f)p.setTextSize(p.getTextSize()*sw*.78f/labelWidth);p.setColor(color);p.setTextAlign(Paint.Align.CENTER);Paint.FontMetrics fm=p.getFontMetrics();
            c.drawText(suffix,cursor+sw/2,y+height/2-(fm.ascent+fm.descent)/2,p);
        }
    }
    public static final class TileView extends View {
        private String digits="--:--",suffix="";private Typeface font=Typeface.DEFAULT_BOLD;private int color=Color.WHITE;
        public TileView(Context c){super(c);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);}
        public void update(String d,String s,Typeface f,int ink,String description){digits=d;suffix=s;font=f;color=ink;setContentDescription(description);invalidate();}
        @Override protected void onDraw(Canvas c){super.onDraw(c);float h=Math.min(getHeight()-2,getWidth()/(suffix.isEmpty()?3.02f:3.61f));if(h>0)TimeTiles.draw(c,digits,suffix,0,(getHeight()-h)/2,h,font,color);}
    }
}
