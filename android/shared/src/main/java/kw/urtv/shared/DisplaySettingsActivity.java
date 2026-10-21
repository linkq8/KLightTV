package kw.urtv.shared;

import android.os.Bundle;
import android.content.Intent;
import android.view.*;
import android.widget.*;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;

/** Draft settings with an immediate native sample; only Save persists. */
public class DisplaySettingsActivity extends TvPage {
    private DisplayOptions draft;private LinearLayout controls;
    @Override public void onCreate(Bundle b){super.onCreate(b);draft=DisplayOptions.load(this);render(0);}
    private void control(String label,Runnable change,int index){TextView t=button(label,()->{change.run();render(index);});t.setTextSize(15);t.setPadding(dp(12),dp(8),dp(12),dp(8));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(6);controls.addView(t,p);}
    private void render(int focus){
        title("خيارات القراءة","عاين التغييرات ثم احفظها · الرئيسية وشاشة التوقف");
        LinearLayout body=new LinearLayout(this);body.setOrientation(0);body.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);list.addView(body,new LinearLayout.LayoutParams(-1,-2));
        controls=new LinearLayout(this);controls.setOrientation(1);body.addView(controls,new LinearLayout.LayoutParams(0,-2,1.1f));
        LinearLayout preview=new LinearLayout(this);preview.setOrientation(1);preview.setPadding(dp(18),dp(18),dp(18),dp(18));GradientDrawable bg=new GradientDrawable();bg.setCornerRadius(dp(12));bg.setColor(draft.contrast?0xFF030A06:android.graphics.Color.rgb(24-draft.dim*6,49-draft.dim*12,37-draft.dim*9));preview.setBackground(bg);LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(0,-2,1);pp.rightMargin=dp(24);body.addView(preview,pp);
        control("حجم النص · "+draft.size(draft.reading),()->draft.reading=(draft.reading+1)%3,0);
        control("مواقيت الصلاة · "+draft.size(draft.prayer),()->draft.prayer=(draft.prayer+1)%3,1);
        control("الصلاة القادمة · "+draft.size(draft.next),()->draft.next=(draft.next+1)%3,2);
        control("خط أثقل · "+(draft.heavy?"مفعّل":"متوقف"),()->draft.heavy=!draft.heavy,3);
        control("تباين أعلى · "+(draft.contrast?"مفعّل":"متوقف"),()->draft.contrast=!draft.contrast,4);
        control("تعتيم الخلفية · "+new String[]{"خفيف","متوسط","قوي"}[draft.dim],()->draft.dim=(draft.dim+1)%3,5);
        control("الأرقام · "+(draft.latin?"إنجليزية":"عربية"),()->draft.latin=!draft.latin,6);
        control("الساعة · "+(draft.hour24?"٢٤ ساعة":"١٢ ساعة"),()->draft.hour24=!draft.hour24,7);
        sample(preview,"نموذج للقراءة",14,0xFFBFCBBC);
        sample(preview,draft.time(20*60+30),Math.round(42*draft.textScale()),0xFFFFFDF6);
        sample(preview,"مواقيت الصلاة",16,0xFFDCB86F);
        sample(preview,"العصر",16,0xFFF6EFE0);tile(preview,TimeTiles.clock(15*60+15,draft),draft.hour24?"":"م",draft.prayerScale());
        sample(preview,"الصلاة القادمة",16,0xFFDCB86F);
        tile(preview,TimeTiles.remaining(83,draft),"",draft.nextScale());
        sample(preview,"تُعرض الأوقات هنا كنموذج فقط.",12,0xFFBFCBBC);
        TextView save=button("حفظ ومعاينة",()->{draft.save(this);Intent i=new Intent().setClassName(getPackageName(),getPackageName()+".MainActivity");startActivity(i);finish();});preview.addView(save,new LinearLayout.LayoutParams(-1,-2));
        TextView cancel=button("رجوع دون حفظ",this::finish);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.topMargin=dp(8);preview.addView(cancel,cp);
        controls.getChildAt(focus).requestFocus();
    }
    private void tile(LinearLayout parent,String digits,String suffix,float scale){TimeTiles.TileView v=new TimeTiles.TileView(this);v.update(digits,suffix,bold,0xFFFFFDF6,digits);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(180),dp(Math.round(36*scale)));p.bottomMargin=dp(10);parent.addView(v,p);}
    private void sample(LinearLayout parent,String label,int size,int color){TextView t=text(label,size,color);if(draft.heavy)t.setTypeface(bold,Typeface.BOLD);t.setPadding(0,0,0,dp(6));parent.addView(t,new LinearLayout.LayoutParams(-1,-2));}
}
