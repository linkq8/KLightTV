package kw.urtv.shared;
import android.app.Activity;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

/** Native remote-first utility surface; no web renderer or background service. */
public abstract class TvPage extends Activity {
    protected Palette palette;protected LinearLayout list;protected Typeface regular,bold;
    @Override public void onCreate(Bundle b){setTheme(new Palette(this,DisplayOptions.load(this)).light?android.R.style.Theme_Material_Light_NoActionBar:android.R.style.Theme_Material_NoActionBar);super.onCreate(b);palette=new Palette(this,DisplayOptions.load(this));getWindow().getDecorView().setSystemUiVisibility(5894);regular=Typeface.createFromAsset(getAssets(),"fonts/Thmanyah-Regular.otf");bold=Typeface.createFromAsset(getAssets(),"fonts/Thmanyah-Medium.otf");
        ScrollView scroll=new ScrollView(this);scroll.setBackgroundColor(palette.bg);scroll.setFillViewport(true);scroll.setVerticalScrollBarEnabled(false);
        list=new LinearLayout(this);list.setOrientation(android.widget.LinearLayout.VERTICAL);list.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);list.setPadding(dp(64),dp(36),dp(64),dp(36));scroll.addView(list);setContentView(scroll);
    }
    @Override protected void onResume(){super.onResume();if(palette!=null&&palette.light!=new Palette(this,DisplayOptions.load(this)).light)recreate();}
    protected int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    protected TextView text(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTypeface(regular);t.setTextColor(palette.text(color));t.setGravity(Gravity.RIGHT);t.setTextDirection(View.TEXT_DIRECTION_RTL);t.setIncludeFontPadding(false);return t;}
    protected void title(String title,String description){list.removeAllViews();TextView h=text(title,34,0xFFF1F5FA);h.setTypeface(bold);list.addView(h);TextView d=text(description,14,0xFFB9CADA);d.setPadding(0,dp(8),0,dp(18));list.addView(d);}
    protected TextView button(String label,Runnable action){TextView t=text(label,17,0xFFF1F5FA);t.setPadding(dp(18),dp(12),dp(18),dp(12));t.setFocusable(true);t.setClickable(true);t.setId(View.generateViewId());GradientDrawable bg=new GradientDrawable();bg.setColor(palette.panel);bg.setCornerRadius(dp(7));t.setBackground(bg);t.setOnFocusChangeListener((v,f)->{bg.setColor(f?palette.accent:palette.panel);t.setTextColor(f?(palette.light?0xFFFFFFFF:0xFF101D2D):palette.ink);});t.setOnClickListener(v->action.run());return t;}
    protected void addButton(String s,Runnable r){TextView b=button(s,r);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(8);list.addView(b,p);}
    protected void note(String s){TextView t=text(s,14,0xFFB9CADA);t.setPadding(0,dp(12),0,dp(12));list.addView(t);}
}
