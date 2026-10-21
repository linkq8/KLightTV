package kw.urtv.home;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import java.util.HashMap;
import java.util.Map;

/** Edge-to-edge native TV banners. Publisher art stays proportional; no inset app-icon tile. */
final class BrandBanner extends Drawable {
    private static final Map<String,Bitmap> MARKS=new HashMap<>();
    private final String brand;private final Bitmap mark;private final int background;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private BrandBanner(String b,Bitmap m,int color){brand=b;mark=m;background=color;}
    static Drawable forPackage(Context c,String pkg){
        String b=null;
        if(pkg.equals("com.todtv.tod")||pkg.equals("kw.urtv.preview.tod"))b="tod";
        else if(pkg.equals("com.amazon.amazonvideo.livingroom")||pkg.equals("com.amazon.avod.thirdpartyclient")||pkg.equals("kw.urtv.preview.prime"))b="prime";
        else if(pkg.equals("com.shasha.tv")||pkg.equals("com.shasha.app")||pkg.equals("kw.urtv.preview.shasha"))b="shasha";
        else if(pkg.equals("com.stremio.one")||pkg.equals("kw.urtv.preview.stremio"))b="stremio";
        else if(pkg.equals("com.google.android.youtube.tv")||pkg.equals("com.google.android.youtube")||pkg.equals("kw.urtv.preview.youtube"))b="youtube";
        if(b==null)return null;
        try{
            Bitmap mark=MARKS.get(b);
            if(mark==null){
                Bitmap original;try(java.io.InputStream in=c.getAssets().open("brands/"+b+".png")){original=BitmapFactory.decodeStream(in);}
                int w=original.getWidth(),h=original.getHeight();int[] pixels=new int[w*h];original.getPixels(pixels,0,w,0,0,w,h);
                int left=w,top=h,right=0,bottom=0;
                for(int y=0;y<h;y++)for(int x=0;x<w;x++){
                    int i=y*w+x,p=pixels[i],a=Color.alpha(p),r=Color.red(p),g=Color.green(p),blue=Color.blue(p);
                    // Native alpha masks remove the mobile icon's background, never stretch the mark.
                    if(b.equals("tod")){a=Math.min(a,Math.min(255,Math.max(0,Math.min(r,g)-blue)*3));}
                    else if(b.equals("prime")||b.equals("shasha")){a=Math.min(a,Math.min(255,Math.max(0,Math.min(r,Math.min(g,blue))-160)*3));r=g=blue=255;}
                    else if(b.equals("youtube")){a=0;}
                    pixels[i]=Color.argb(a,r,g,blue);
                    if(a>24){left=Math.min(left,x);top=Math.min(top,y);right=Math.max(right,x);bottom=Math.max(bottom,y);}
                }
                Bitmap mask=Bitmap.createBitmap(pixels,w,h,Bitmap.Config.ARGB_8888);original.recycle();
                mark=right>left?Bitmap.createBitmap(mask,left,top,right-left+1,bottom-top+1):mask;
                if(mark!=mask)mask.recycle();MARKS.put(b,mark);
            }
            int color=b.equals("tod")?0xFF202020:b.equals("prime")?0xFF087BFF:b.equals("shasha")?0xFF064C91:b.equals("stremio")?0xFF201346:0xFFFF0000;
            return new BrandBanner(b,mark,color);
        }catch(Exception e){return null;}
    }
    @Override public void draw(Canvas c){
        Rect r=getBounds();paint.setColor(background);paint.setShader(null);c.drawRect(r,paint);
        float w=r.width(),h=r.height(),cx=r.exactCenterX(),cy=r.exactCenterY();
        if(brand.equals("youtube")){
            paint.setColor(Color.WHITE);RectF pill=new RectF(cx-w*.23f,cy-h*.27f,cx+w*.23f,cy+h*.27f);c.drawRoundRect(pill,h*.12f,h*.12f,paint);
            paint.setColor(background);Path triangle=new Path();triangle.moveTo(cx-w*.05f,cy-h*.14f);triangle.lineTo(cx+w*.09f,cy);triangle.lineTo(cx-w*.05f,cy+h*.14f);triangle.close();c.drawPath(triangle,paint);return;
        }
        float maxW=w*.66f,maxH=h*.60f;if(brand.equals("prime"))maxH=h*.72f;
        float scale=Math.min(maxW/mark.getWidth(),maxH/mark.getHeight());float mw=mark.getWidth()*scale,mh=mark.getHeight()*scale;
        paint.setColor(Color.WHITE);c.drawBitmap(mark,null,new RectF(cx-mw/2,cy-mh/2,cx+mw/2,cy+mh/2),paint);
    }
    @Override public int getIntrinsicWidth(){return 640;}
    @Override public int getIntrinsicHeight(){return 360;}
    @Override public void setAlpha(int a){}
    @Override public void setColorFilter(ColorFilter f){}
    @Override public int getOpacity(){return PixelFormat.OPAQUE;}
}
