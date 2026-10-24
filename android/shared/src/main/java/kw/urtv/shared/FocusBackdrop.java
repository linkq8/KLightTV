package kw.urtv.shared;
import android.graphics.*;
import android.graphics.drawable.Drawable;
/** Two cached gradients, drawn only when the view redraws; no images, blur or animation. */
public final class FocusBackdrop extends Drawable {
 private final Paint p=new Paint(3);private final String color;private Shader base,wash;
 public FocusBackdrop(String color){this.color=color;}
 @Override protected void onBoundsChange(Rect b){
  boolean white=color.equals("white"),black=color.equals("black");
  int[] colors=white?new int[]{0xFFFFFFFF,0xFFF0F3F7,0xFFE0E8F1}:black?new int[]{0xFF030405,0xFF101216,0xFF252B34}:new int[]{0xFF06162F,0xFF102E51,0xFF285B7A};
  base=new LinearGradient(0,0,b.width(),b.height(),colors,new float[]{0,.56f,1},Shader.TileMode.CLAMP);
  wash=new RadialGradient(b.width()*.78f,b.height()*.90f,b.width()*.70f,white?new int[]{0xA0FFFFFF,0x00FFFFFF}:black?new int[]{0x203B4654,0x003B4654}:new int[]{0x38578FA3,0x00578FA3},null,Shader.TileMode.CLAMP);
 }
 @Override public void draw(Canvas c){p.setColor(Color.WHITE);p.setShader(base);c.drawRect(getBounds(),p);p.setShader(wash);c.drawRect(getBounds(),p);p.setShader(null);}
 @Override public void setAlpha(int a){} @Override public void setColorFilter(ColorFilter f){} @Override public int getOpacity(){return PixelFormat.OPAQUE;}
}
