package kw.urtv.home;
import android.app.AlertDialog;
import android.content.*;
import android.content.pm.*;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.util.*;
import kw.urtv.shared.TvPage;

/** Visibility choices are explicit: hiding does not disable the package or stop background work. */
public class ManageAppsActivity extends TvPage {
    private final Set<String> hidden=new HashSet<>();private final List<ResolveInfo> apps=new ArrayList<>();
    @Override public void onCreate(Bundle b){super.onCreate(b);hidden.addAll(Dock.prefs(this).getStringSet("hidden",Collections.emptySet()));apps.addAll(Dock.catalogue(this,true));apps.sort((a,c)->a.loadLabel(getPackageManager()).toString().compareTo(c.loadLabel(getPackageManager()).toString()));render();}
    private void render(){title("تطبيقاتي", "حدد ما يظهر على الرئيسية. إخفاء التطبيق لا يوقف عمله في الخلفية.");
        LinearLayout bar=new LinearLayout(this);bar.setLayoutDirection(1);list.addView(bar);
        String[] names={"حفظ الاختيارات","تراجع عن آخر حفظ","إدارة تطبيقات النظام","رجوع"};Runnable[] actions={this::save,this::undo,()->new AlertDialog.Builder(this).setTitle("التعطيل وتقييد الخلفية").setMessage("تطبيقات مثل Play Games وPlay Movies يمكن إدارتها بأداة الكمبيوتر المرفقة عبر ADB. تعرض الأداة التطبيقات وخطة التغييرات وخيار الاستعادة. هذه الشاشة تتحكم بظهور التطبيقات فقط؛ لن ندّعي تعطيل تطبيق لا نملك صلاحية تعطيله.").setPositiveButton("حسنًا",null).show(),this::finish};
        for(int i=0;i<names.length;i++){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.setMargins(dp(4),0,dp(4),dp(16));bar.addView(button(names[i],actions[i]),p);}
        for(ResolveInfo ri:apps){String pkg=ri.activityInfo.packageName;LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(10),dp(4),dp(10),dp(4));row.setLayoutDirection(1);
            ImageView icon=new ImageView(this);icon.setImageDrawable(ri.loadIcon(getPackageManager()));row.addView(icon,new LinearLayout.LayoutParams(dp(38),dp(38)));
            CheckBox check=new CheckBox(this);check.setText(ri.loadLabel(getPackageManager()));check.setTextSize(19);check.setTextColor(0xFFF6EFE0);check.setTypeface(bold);check.setPadding(dp(14),dp(6),dp(14),dp(6));check.setButtonTintList(android.content.res.ColorStateList.valueOf(0xFFDCB86F));check.setChecked(!hidden.contains(pkg));check.setOnCheckedChangeListener((b,on)->{if(on)hidden.remove(pkg);else hidden.add(pkg);});
            android.graphics.drawable.GradientDrawable bg=new android.graphics.drawable.GradientDrawable();bg.setCornerRadius(dp(8));bg.setColor(0xFF091610);check.setBackground(bg);check.setOnFocusChangeListener((v,f)->bg.setColor(f?0xFF294D37:0xFF091610));row.addView(check,new LinearLayout.LayoutParams(0,dp(55),1));
            TextView manage=button("خيارات",()->options(pkg,ri.loadLabel(getPackageManager()).toString()));row.addView(manage,new LinearLayout.LayoutParams(dp(96),-2));list.addView(row);
            View line=new View(this);line.setBackgroundColor(0xFF243C2D);list.addView(line,new LinearLayout.LayoutParams(-1,dp(1)));
        }
        if(apps.isEmpty())note("لا توجد تطبيقات متاحة للعرض.");bar.getChildAt(0).requestFocus();
    }
    private void save(){android.content.SharedPreferences p=Dock.prefs(this);p.edit().putStringSet("previous_hidden",new HashSet<>(p.getStringSet("hidden",Collections.emptySet()))).putStringSet("hidden",new HashSet<>(hidden)).apply();sendBroadcast(new Intent(Dock.CHANGED).setPackage(getPackageName()));Toast.makeText(this,"حُفظت قائمة الظهور",0).show();}
    private void undo(){android.content.SharedPreferences p=Dock.prefs(this);if(!p.contains("previous_hidden")){Toast.makeText(this,"لا يوجد تغيير سابق",0).show();return;}hidden.clear();hidden.addAll(p.getStringSet("previous_hidden",Collections.emptySet()));p.edit().putStringSet("hidden",new HashSet<>(hidden)).remove("previous_hidden").apply();sendBroadcast(new Intent(Dock.CHANGED).setPackage(getPackageName()));render();}
    private void options(String pkg,String label){boolean system;try{ApplicationInfo a=getPackageManager().getApplicationInfo(pkg,0);system=(a.flags&ApplicationInfo.FLAG_SYSTEM)!=0;}catch(Exception e){return;}
        final boolean builtin=system;new AlertDialog.Builder(this).setTitle(label).setItems(new String[]{"معلومات التطبيق",builtin?"إدارة التعطيل عبر الكمبيوتر":"إلغاء التثبيت (Uninstall)"},(d,i)->{
            if(i==0)startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+pkg)));
            else if(builtin)new AlertDialog.Builder(this).setTitle("تطبيق مدمج").setMessage("استخدم أداة إدارة التلفزيون على الكمبيوتر لتعطيله بصورة قابلة للاسترجاع. الإخفاء هنا لا يعطله.").setPositiveButton("حسنًا",null).show();
            else startActivity(new Intent(Intent.ACTION_UNINSTALL_PACKAGE,Uri.parse("package:"+pkg)));
        }).show();}
}
