package kw.urtv.shared;
import android.app.Activity;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

/** Native remote-first utility surface; no web renderer or background service. */
public abstract class TvPage extends Activity {
    protected LinearLayout list;protected Typeface regular,bold;
    @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().getDecorView().setSystemUiVisibility(5894);regular=Typeface.createFromAsset(getAssets(),"fonts/Thmanyah-Regular.otf");bold=Typeface.createFromAsset(getAssets(),"fonts/Thmanyah-Medium.otf");
        ScrollView scroll=new ScrollView(this);scroll.setBackgroundColor(0xFF091610);scroll.setFillViewport(true);scroll.setVerticalScrollBarEnabled(false);
        list=new LinearLayout(this);list.setOrientation(1);list.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);list.setPadding(dp(48),dp(30),dp(48),dp(32));scroll.addView(list);setContentView(scroll);
    }
    protected int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    protected TextView text(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTypeface(regular);t.setTextColor(color);t.setGravity(Gravity.RIGHT);t.setTextDirection(View.TEXT_DIRECTION_RTL);t.setIncludeFontPadding(false);return t;}
    protected void title(String title,String description){list.removeAllViews();TextView h=text(title,30,0xFFF6EFE0);h.setTypeface(bold);list.addView(h);TextView d=text(description,14,0xFFBFCBBC);d.setPadding(0,dp(8),0,dp(18));list.addView(d);}
    protected TextView button(String label,Runnable action){TextView t=text(label,17,0xFFF6EFE0);t.setPadding(dp(18),dp(12),dp(18),dp(12));t.setFocusable(true);t.setClickable(true);t.setId(View.generateViewId());GradientDrawable bg=new GradientDrawable();bg.setColor(0xFF183125);bg.setCornerRadius(dp(10));t.setBackground(bg);t.setOnFocusChangeListener((v,f)->{bg.setColor(f?0xFFDCB86F:0xFF183125);t.setTextColor(f?0xFF091610:0xFFF6EFE0);});t.setOnClickListener(v->action.run());return t;}
    protected void addButton(String s,Runnable r){TextView b=button(s,r);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(8);list.addView(b,p);}
    protected void note(String s){TextView t=text(s,14,0xFFBFCBBC);t.setPadding(0,dp(12),0,dp(12));list.addView(t);}
}
