package kw.urtv.shared;
import android.content.Context;
public final class Palette {
 public final boolean light;public final int ink,muted,accent,bg,panel,selected;
 public Palette(Context c,DisplayOptions o){light=o.focused()?o.focusColor.equals("white"):o.theme.equals("light")||(o.theme.equals("auto")&&(c.getResources().getConfiguration().uiMode&48)!=32);ink=light?0xFF182D36:0xFFF1F5FA;muted=light?0xFF405866:0xFFB9CADA;accent=light?0xFF225E70:0xFF8EDBEA;bg=light?0xFFF2F2F7:0xFF121316;panel=light?0xFFFFFFFF:0xFF25262B;selected=light?0xFFD0E3E9:0xFF234C60;}
 public int text(int original){if(!light)return original;return original==0xFF8EDBEA?accent:original==0xFFF1F5FA||original==0xFFFFFFFF||original==0xFFFFFDF6||original==0xFFFFFCF2?ink:muted;}
}
