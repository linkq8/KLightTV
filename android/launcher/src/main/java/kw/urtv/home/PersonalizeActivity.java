package kw.urtv.home;
import android.app.AlertDialog;
import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import java.util.*;
import org.json.*;
import kw.urtv.shared.TvPage;

/** On-demand preferences only; no resident worker. Backup contains preferences, never accounts. */
public class PersonalizeActivity extends TvPage {
    @Override public void onCreate(Bundle b){super.onCreate(b);if(b==null&&MainActivity.shouldOfferSetup(this)){startActivity(new Intent(this,SetupActivity.class));finish();return;}int initial=Math.max(0,Math.min(4,getIntent().getIntExtra("section",0)));render();navigation[initial].post(()->{section=initial;showSection();navigation[initial].requestFocus();});}
    private int section=0;
    private boolean renderingSection;
    private android.widget.LinearLayout detail;
    private android.widget.TextView[] navigation;
    private final String[] sections={"الصلاة والموقع","المظهر والقراءة","التطبيقات والمحتوى","التحديث والصيانة","النسخ الاحتياطي"};
    private void render(){
        boolean light=palette.light;
        android.widget.LinearLayout root=new android.widget.LinearLayout(this);root.setOrientation(android.widget.LinearLayout.VERTICAL);root.setPadding(dp(56),dp(36),dp(56),dp(28));root.setBackgroundColor(light?0xFFF2F2F7:0xFF121316);
        android.widget.TextView heading=text("إعدادات KLightTV",30,palette.ink);heading.setTypeface(bold);root.addView(heading,new android.widget.LinearLayout.LayoutParams(-1,dp(66)));
        android.widget.LinearLayout body=new android.widget.LinearLayout(this);body.setLayoutDirection(android.view.View.LAYOUT_DIRECTION_RTL);root.addView(body,new android.widget.LinearLayout.LayoutParams(-1,0,1));
        android.widget.LinearLayout sidebar=new android.widget.LinearLayout(this);sidebar.setOrientation(android.widget.LinearLayout.VERTICAL);sidebar.setPadding(dp(20),dp(12),0,0);body.addView(sidebar,new android.widget.LinearLayout.LayoutParams(dp(230),-1));
        android.widget.ScrollView scroll=new android.widget.ScrollView(this);scroll.setVerticalScrollBarEnabled(false);body.addView(scroll,new android.widget.LinearLayout.LayoutParams(0,-1,1));
        detail=new android.widget.LinearLayout(this);detail.setOrientation(android.widget.LinearLayout.VERTICAL);detail.setPadding(0,dp(12),dp(24),dp(12));detail.setLayoutDirection(android.view.View.LAYOUT_DIRECTION_RTL);scroll.addView(detail);
        navigation=new android.widget.TextView[sections.length];
        for(int i=0;i<sections.length;i++){final int n=i;android.widget.TextView t=settingRow(sections[i],()->{section=n;showSection();if(detail.getChildCount()>2)detail.getChildAt(2).requestFocus();});navigation[i]=t;int[] icons={R.drawable.ic_screensaver,R.drawable.ic_settings,R.drawable.ic_apps,R.drawable.ic_update,R.drawable.ic_input};android.graphics.drawable.Drawable icon=getDrawable(icons[i]).mutate();icon.setBounds(0,0,dp(20),dp(20));icon.setTint(palette.muted);t.setCompoundDrawables(null,null,icon,null);t.setCompoundDrawablePadding(dp(12));t.setOnFocusChangeListener((v,f)->{rowStyle(t,f);if(f&&!renderingSection){section=n;showSection();}});android.widget.LinearLayout.LayoutParams lp=new android.widget.LinearLayout.LayoutParams(-1,dp(54));lp.bottomMargin=dp(4);sidebar.addView(t,lp);}
        android.widget.TextView back=text("رجوع لإغلاق الإعدادات",13,palette.muted);back.setPadding(0,dp(12),0,0);root.addView(back);
        setContentView(root);showSection();navigation[section].requestFocus();
    }
    private android.widget.TextView settingRow(String label,Runnable action){
        android.widget.TextView t=text(label,18,palette.ink);t.setGravity(android.view.Gravity.RIGHT|android.view.Gravity.CENTER_VERTICAL);t.setPadding(dp(20),0,dp(20),0);t.setFocusable(true);t.setDefaultFocusHighlightEnabled(false);t.setClickable(true);t.setId(android.view.View.generateViewId());rowStyle(t,false);t.setOnFocusChangeListener((v,f)->rowStyle(t,f));t.setOnClickListener(v->action.run());return t;
    }
    private void rowStyle(android.widget.TextView t,boolean focused){android.graphics.drawable.GradientDrawable bg=new android.graphics.drawable.GradientDrawable();bg.setCornerRadius(dp(8));bg.setColor(focused?0xFFF5F5F7:palette.light?0xFFFFFFFF:0xFF1C1F25);if(focused&&palette.light)bg.setStroke(dp(2),0xFF707078);t.setBackground(bg);t.setTextColor(focused?0xFF151519:palette.light?0xFF202024:0xFFF5F5F7);}
    private void option(String label,Runnable action){android.widget.TextView row=settingRow(label,action);android.widget.LinearLayout.LayoutParams p=new android.widget.LinearLayout.LayoutParams(-1,dp(48));p.bottomMargin=dp(2);detail.addView(row,p);row.setNextFocusRightId(navigation[section].getId());if(detail.getChildCount()==3)navigation[section].setNextFocusLeftId(row.getId());}
    private void open(Class<?> c){startActivity(new Intent(this,c));}
    private void saverPage(String name){try{startActivity(new Intent().setClassName("kw.urtv.screensaver","kw.urtv.screensaver."+name));}catch(Exception e){toast("تعذر فتح الشاشة؛ تأكد من تثبيت التطبيق المرافق");}}
    private void showSection(){
        if(renderingSection)return;renderingSection=true;
        try{
        detail.removeAllViews();android.widget.TextView h=text(sections[section],25,palette.ink);h.setTypeface(bold);detail.addView(h,new android.widget.LinearLayout.LayoutParams(-1,dp(44)));
        String[] hints={kw.urtv.shared.Place.load(this).name,"الخلفية، القراءة، ومحتوى شاشة التوقف","إدارة التطبيقات، المفضلة، والفرق","التحديث والأداء وإعداد الجهاز","احفظ اختياراتك أو انقلها إلى جهاز آخر"};
        android.widget.TextView hint=text(hints[section],14,palette.muted);detail.addView(hint,new android.widget.LinearLayout.LayoutParams(-1,dp(40)));
        switch(section){
        case 0:option("إعداد سريع · Quick setup",()->open(SetupActivity.class));option("الموقع ومواقيت الصلاة",()->saverPage("SettingsActivity"));option("تنبيهات الصلاة",()->saverPage("ReminderActivity"));option("تقويم الصلاة",()->open(kw.urtv.shared.CalendarActivity.class));break;
        case 1:option("تصميم الشاشة · "+(kw.urtv.shared.DisplayOptions.load(this).focused()?"تركيز":"صور الكويت"),()->{kw.urtv.shared.DisplayOptions o=kw.urtv.shared.DisplayOptions.load(this);o.surface=o.focused()?"scenic":"focus";o.save(this);showSection();detail.getChildAt(2).requestFocus();});option("خلفية التركيز · "+kw.urtv.shared.DisplayOptions.load(this).focusColorName(),()->{kw.urtv.shared.DisplayOptions o=kw.urtv.shared.DisplayOptions.load(this);o.cycleFocusColor();o.save(this);showSection();detail.getChildAt(3).requestFocus();});option("شاشة التوقف · "+(kw.urtv.shared.DisplayOptions.load(this).saverMatches?"صلاة وطقس ومباريات":"صلاة وطقس فقط"),()->{kw.urtv.shared.DisplayOptions o=kw.urtv.shared.DisplayOptions.load(this);o.saverMatches=!o.saverMatches;o.save(this);showSection();detail.getChildAt(4).requestFocus();});option("المظهر وحجم الكتابة",()->open(kw.urtv.shared.DisplaySettingsActivity.class));break;
        case 2:option("إدارة التطبيقات · إزالة وتخصيص",()->open(ManageAppsActivity.class));option("ترتيب المفضلة",()->open(DockActivity.class));option("أزرار الريموت",()->open(RemoteSettingsActivity.class));option("الفرق والمباريات",()->open(FootballActivity.class));break;
        case 3:option("وضع الأداء",()->open(kw.urtv.shared.PerformanceActivity.class));option("تحديث التطبيق",()->open(UpdateActivity.class));option("إعدادات نظام Android",()->startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS)));break;
        default:option("حفظ التفضيلات",()->document(true));option("استعادة التفضيلات",()->document(false));break;
        }
        }finally{renderingSection=false;}
    }
    @Override public void onBackPressed(){if(navigation!=null&&!navigation[section].hasFocus()){navigation[section].requestFocus();}else finish();}
    private void document(boolean export){
        Intent i=new Intent(export?Intent.ACTION_CREATE_DOCUMENT:Intent.ACTION_OPEN_DOCUMENT).setType("application/json").addCategory(Intent.CATEGORY_OPENABLE);
        if(export)i.putExtra(Intent.EXTRA_TITLE,"klighttv-settings.json");
        try{startActivityForResult(i,export?10:11);}catch(ActivityNotFoundException e){new AlertDialog.Builder(this).setTitle("منتقي الملفات غير متاح").setMessage("هذا التلفزيون لا يتضمن منتقي ملفات Android. تبقى تفضيلاتك محفوظة على الجهاز؛ تحتاج هذه الميزة إلى منتقي ملفات يدعم فتح المستندات وحفظها.").setPositiveButton("حسنًا",null).show();}
    }
    private void photos(){
        // Photographic backgrounds removed by design.
    }
    private void health(){android.app.ActivityManager.MemoryInfo m=new android.app.ActivityManager.MemoryInfo();getSystemService(android.app.ActivityManager.class).getMemoryInfo(m);new AlertDialog.Builder(this).setTitle("فحص عند الطلب").setMessage("KLightTV ١.١٠.٠ · عرض أصلي دون WebView\nذاكرة متاحة للجهاز: "+(m.availMem/1048576)+" MiB\nدقة الواجهة: "+getResources().getDisplayMetrics().widthPixels+" × "+getResources().getDisplayMetrics().heightPixels+"\nلا تعمل خدمة فحص مستمرة.\nهذه الشاشة لا تثبت إيقاف تتبع الشركة؛ التحقق يتم بأداة الكمبيوتر.").setPositiveButton("حسنًا",null).show();}
    @Override public void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(result!=RESULT_OK||data==null)return;try{android.content.SharedPreferences p=Dock.prefs(this);if(request==10){JSONObject o=new JSONObject();o.put("format","kuwait-home-1");o.put("place",new JSONObject(kw.urtv.shared.Place.load(this).json()));try(android.database.Cursor cur=getContentResolver().query(kw.urtv.shared.DisplayOptions.URI,null,null,null,null)){if(cur!=null&&cur.moveToFirst()){String raw=android.net.Uri.parse("https://local/?"+cur.getString(0)).getQueryParameter("adjust");kw.urtv.shared.PrayerCorrections.parse(raw);o.put("corrections",raw);}}o.put("display",new JSONObject(kw.urtv.shared.DisplayOptions.load(this).json()));o.put("dock",new JSONArray(Dock.saved(this)));o.put("hidden",new JSONArray(p.getStringSet("hidden",Collections.emptySet())));o.put("wallpaper",p.getInt("wallpaper",0));o.put("pinned",p.getBoolean("wallpaper_pinned",false));try(java.io.OutputStream out=getContentResolver().openOutputStream(data.getData())){out.write(o.toString(2).getBytes("UTF-8"));}toast("حُفظت تفضيلات الواجهة فقط");}
        if(request==11){String body;try(java.io.InputStream in=getContentResolver().openInputStream(data.getData());java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream()){byte[] b=new byte[2048];int n;while((n=in.read(b))!=-1){out.write(b,0,n);if(out.size()>65536)throw new Exception();}body=out.toString("UTF-8");}JSONObject o=new JSONObject(body);if(!o.getString("format").equals("kuwait-home-1"))throw new Exception();kw.urtv.shared.Place imported=o.has("place")?kw.urtv.shared.Place.parse(o.getJSONObject("place").toString()):null;String correction=o.optString("corrections",null);if(correction!=null){if(imported==null)throw new Exception();kw.urtv.shared.PrayerCorrections.parse(correction);}kw.urtv.shared.DisplayOptions importedDisplay=o.has("display")?kw.urtv.shared.DisplayOptions.decode(o.getJSONObject("display").toString()):null;Set<String> hidden=new HashSet<>();List<String> dock=new ArrayList<>();for(String key:new String[]{"hidden","dock"}){JSONArray a=o.getJSONArray(key);if(a.length()>300)throw new Exception();for(int i=0;i<a.length();i++){String pkg=a.getString(i);if(!pkg.matches("[A-Za-z0-9_]+(?:\\.[A-Za-z0-9_]+)+"))throw new Exception();if(key.equals("hidden"))hidden.add(pkg);else if(dock.size()<Dock.MAX)dock.add(pkg);}}
            if(imported!=null){android.content.ContentValues cv=new android.content.ContentValues();cv.put("place",imported.json());if(correction!=null)cv.put("corrections",correction);if(getContentResolver().update(kw.urtv.shared.DisplayOptions.URI,cv,null,null)!=1)throw new Exception();}if(importedDisplay!=null)importedDisplay.save(this);
            p.edit().putStringSet("previous_hidden",new HashSet<>(p.getStringSet("hidden",Collections.emptySet()))).putStringSet("hidden",hidden).putString("dock",String.join(",",dock)).putInt("wallpaper",Math.max(0,o.optInt("wallpaper"))).putBoolean("wallpaper_pinned",o.optBoolean("pinned")).putBoolean("wallpaper_reload",true).apply();sendBroadcast(new Intent(Dock.CHANGED).setPackage(getPackageName()));toast("استُعيدت تفضيلات الواجهة");render();}}
        catch(Exception e){toast("تعذرت قراءة الملف أو حفظه. استخدم نسخة تفضيلات الكويت.");}}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
}
