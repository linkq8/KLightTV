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
        draw(c,digits,suffix,x,y,height,font,color,.77f);
    }
    public static void draw(Canvas c,String digits,String suffix,float x,float y,float height,Typeface font,int color,float digitScale){
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setTypeface(font);p.setColor(color);p.setTextAlign(Paint.Align.LEFT);
        float width=height*(suffix.isEmpty()?2.835f:3.51f);p.setTextSize(height*.92f);
        float digitsWidth=p.measureText(digits), suffixWidth=height*.55f;
        if(digitsWidth>width-(suffix.isEmpty()?0:suffixWidth))p.setTextSize(p.getTextSize()*(width-(suffix.isEmpty()?0:suffixWidth))/digitsWidth);
        digitsWidth=p.measureText(digits);float all=digitsWidth+(suffix.isEmpty()?0:suffixWidth);float left=x+(width-all)/2;
        Paint.FontMetrics fm=p.getFontMetrics();float baseline=y+height/2-(fm.ascent+fm.descent)/2;
        c.drawText(digits,left,baseline,p);
        if(!suffix.isEmpty()){p.setTextSize(height*.38f);p.setTextAlign(Paint.Align.CENTER);c.drawText(suffix,left+digitsWidth+suffixWidth*.58f,baseline,p);}
    }
    /** Shared lightweight renderer for native Views and the screensaver Canvas. */
    public static final class GlassPainter {
        private final Paint tilePaint=new Paint(Paint.ANTI_ALIAS_FLAG);
        private boolean light,contrast;private Typeface font;private int color;private float digitScale;
        private void cell(Canvas c,String value,float x,float y,float w,float h,boolean period){
            Paint p=tilePaint;p.setStyle(Paint.Style.FILL);
            p.setColor(light?(contrast?0xFFFFFFFF:0xA6FFFFFF):(contrast?0xFF344456:0x38D8E6F1));
            c.drawRoundRect(x,y,x+w,y+h,h*.12f,h*.12f,p);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(1,h*.018f));p.setColor(light?0x30233D49:0x40FFFFFF);
            c.drawRoundRect(x,y,x+w,y+h,h*.12f,h*.12f,p);
            p.setStyle(Paint.Style.FILL);p.setTypeface(font);p.setColor(color);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(h*(period?.43f:digitScale));
            float measured=p.measureText(value);if(measured>w*.80f)p.setTextSize(p.getTextSize()*w*.80f/measured);
            Paint.FontMetrics fm=p.getFontMetrics();c.drawText(value,x+w/2,y+h/2-(fm.ascent+fm.descent)/2,p);
        }
        public void draw(Canvas c,String digits,String suffix,float left,float top,float width,float height,Typeface font,int color,float digitScale,boolean light,boolean contrast,boolean centered){
            this.font=font;this.color=color;this.digitScale=Math.max(.5f,Math.min(.9f,digitScale));this.light=light;this.contrast=contrast;
            c.save();c.translate(left,top);
            // Fixed cells preserve Arabic numeral order and identical sizes; no flip animation or blur.
            int count=digits.length();float units=0;for(int i=0;i<count;i++)units+=digits.charAt(i)==':'?.27f:.62f;
            units+=(count-1)*.07f;if(!suffix.isEmpty())units+=.64f;
            float h=Math.min(height-2,width/Math.max(3.39f,units));if(h<=0){c.restore();return;}
            float x=centered?(width-units*h)/2:0,y=(height-h)/2;
            for(int i=0;i<count;i++){
                char digit=digits.charAt(i);float w=h*(digit==':'?.27f:.62f);
                if(digit==':'){tilePaint.setStyle(Paint.Style.FILL);tilePaint.setColor(color);float radius=h*.055f;c.drawCircle(x+w/2,y+h*.37f,radius,tilePaint);c.drawCircle(x+w/2,y+h*.63f,radius,tilePaint);}
                else cell(c,i==0&&(digit=='0'||digit=='٠')?"":String.valueOf(digit),x,y,w,h,false);
                x+=w+(i<count-1?h*.07f:0);
            }
            if(!suffix.isEmpty())cell(c,suffix,x+h*.10f,y,h*.54f,h,true);
            c.restore();
        }
    }
    public static final class TileView extends View {
        private boolean centered,reservePeriod;
        private boolean glass,light,contrast;
        private final GlassPainter glassPainter=new GlassPainter();
        private float digitScale=.77f;
        private String digits="--:--",suffix="";private Typeface font=Typeface.DEFAULT_BOLD;private int color=Color.WHITE;
        public TileView(Context c){super(c);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);}
        public void update(String d,String s,Typeface f,int ink,String description){digits=d;suffix=s;font=f;color=ink;setContentDescription(description);invalidate();}
        public void alignCentered(){centered=true;reservePeriod=true;invalidate();}
        public void setDigitScale(float scale){digitScale=Math.max(.5f,Math.min(.9f,scale));invalidate();}
        public void setGlassTiles(boolean isLight,boolean highContrast){glass=true;light=isLight;contrast=highContrast;invalidate();}
        @Override protected void onDraw(Canvas c){super.onDraw(c);if(glass){glassPainter.draw(c,digits,suffix,0,0,getWidth(),getHeight(),font,color,digitScale,light,contrast,centered);return;}float h=Math.min(getHeight()-2,getWidth()/((reservePeriod||!suffix.isEmpty())?3.61f:3.02f));float width=h*(suffix.isEmpty()?2.835f:3.51f);if(h>0)TimeTiles.draw(c,digits,suffix,centered?(getWidth()-width)/2:0,(getHeight()-h)/2,h,font,color,digitScale);}
    }
}
