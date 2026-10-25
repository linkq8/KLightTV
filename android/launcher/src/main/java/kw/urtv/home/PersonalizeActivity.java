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
    @Override public void onCreate(Bundle b){super.onCreate(b);render();}
    private void render(){title("KLightTV · الكويت","المفضلة والخلفيات والبيانات، في مكان واحد");
        addButton("خيارات القراءة",()->startActivity(new Intent(this,kw.urtv.shared.DisplaySettingsActivity.class)));
        addButton("أداء الجهاز",()->startActivity(new Intent(this,kw.urtv.shared.PerformanceActivity.class)));
        addButton("منطقة الصلاة وتصحيح المواقيت",()->{try{startActivity(new Intent().setClassName("kw.urtv.screensaver","kw.urtv.screensaver.SettingsActivity"));}catch(Exception e){toast("ثبّت شاشة التوقف لاختيار المنطقة");}});
        addButton("تطبيقاتي · إظهار وإخفاء",()->startActivity(new Intent(this,ManageAppsActivity.class)));
        addButton("ترتيب التطبيقات المفضلة",()->startActivity(new Intent(this,DockActivity.class)));
        addButton("إمساكية رمضان · رزنامة العجيري",()->startActivity(new Intent(this,kw.urtv.shared.CalendarActivity.class)));
        addButton("الخلفية · "+(Dock.prefs(this).getBoolean("wallpaper_pinned",false)?"مثبتة":"تتبدل كل ٣٠ دقيقة"),this::photos);
        addButton("نسخة احتياطية للتفضيلات",()->document(true));
        addButton("استعادة التفضيلات",()->document(false));
        addButton("فحص الواجهة",this::health);
        addButton("إعدادات شاشة التوقف",()->{try{startActivity(new Intent().setClassName("kw.urtv.screensaver","kw.urtv.screensaver.SettingsActivity"));}catch(Exception e){toast("شاشة التوقف غير مثبتة");}});
        addButton("إعدادات التلفزيون",()->startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS)));
        addButton("رجوع",this::finish);list.getChildAt(2).requestFocus();
    }
    private void document(boolean export){
        Intent i=new Intent(export?Intent.ACTION_CREATE_DOCUMENT:Intent.ACTION_OPEN_DOCUMENT).setType("application/json").addCategory(Intent.CATEGORY_OPENABLE);
        if(export)i.putExtra(Intent.EXTRA_TITLE,"klighttv-settings.json");
        try{startActivityForResult(i,export?10:11);}catch(ActivityNotFoundException e){new AlertDialog.Builder(this).setTitle("منتقي الملفات غير متاح").setMessage("هذا التلفزيون لا يتضمن منتقي ملفات Android. تبقى تفضيلاتك محفوظة على الجهاز؛ تحتاج هذه الميزة إلى منتقي ملفات يدعم فتح المستندات وحفظها.").setPositiveButton("حسنًا",null).show();}
    }
    private void photos(){try{String[] files=getAssets().list("wallpapers");Arrays.sort(files);String[] labels=new String[files.length+1];labels[0]="تبديل تلقائي كل ٣٠ دقيقة";Map<String,String> names=new HashMap<>();names.put("composed.jpg","الكويت عند الغروب · 4K محسّنة");names.put("water.jpg","أضواء الكويت وانعكاساتها · Shashi Ghosh");names.put("dusk.jpg","بانوراما الكويت والبحر · Frans van Heerden");names.put("sunset.jpg","غروب المارينا والنخيل · Quintin Gellar");for(int i=0;i<files.length;i++)labels[i+1]=names.getOrDefault(files[i],files[i]);new AlertDialog.Builder(this).setTitle("خلفيات الكويت").setItems(labels,(d,i)->{Dock.prefs(this).edit().putBoolean("wallpaper_pinned",i>0).putInt("wallpaper",i>0?i-1:0).putBoolean("wallpaper_reload",true).apply();render();}).show();}catch(Exception e){toast("تعذرت قراءة الخلفيات");}}
    private void health(){android.app.ActivityManager.MemoryInfo m=new android.app.ActivityManager.MemoryInfo();getSystemService(android.app.ActivityManager.class).getMemoryInfo(m);new AlertDialog.Builder(this).setTitle("فحص عند الطلب").setMessage("KLightTV ١.١٠.٠ · عرض أصلي دون WebView\nذاكرة متاحة للجهاز: "+(m.availMem/1048576)+" MiB\nدقة الواجهة: "+getResources().getDisplayMetrics().widthPixels+" × "+getResources().getDisplayMetrics().heightPixels+"\nلا تعمل خدمة فحص مستمرة.\nهذه الشاشة لا تثبت إيقاف تتبع الشركة؛ التحقق يتم بأداة الكمبيوتر.").setPositiveButton("حسنًا",null).show();}
    @Override public void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(result!=RESULT_OK||data==null)return;try{android.net.Uri uri=data.getData();
        if(uri==null || !"content".equals(uri.getScheme()) || uri.getAuthority()==null || uri.getAuthority().startsWith("kw.urtv."))throw new SecurityException("Select an external document provider");
        android.content.pm.ProviderInfo provider=getPackageManager().resolveContentProvider(uri.getAuthority(),0);
        if(provider==null || provider.applicationInfo==null || (provider.applicationInfo.flags&android.content.pm.ApplicationInfo.FLAG_SYSTEM)==0)throw new SecurityException("Use a system document provider");
        java.nio.file.Path normalized=java.nio.file.Paths.get(uri.getPath()).normalize();
        if(normalized.startsWith("/data") || normalized.startsWith("/proc") || normalized.startsWith("/sys"))throw new SecurityException("Private path not permitted");
        android.content.SharedPreferences p=Dock.prefs(this);if(request==10){JSONObject o=new JSONObject();o.put("format","kuwait-home-1");o.put("display",new JSONObject(kw.urtv.shared.DisplayOptions.load(this).json()));o.put("dock",new JSONArray(Dock.saved(this)));o.put("hidden",new JSONArray(p.getStringSet("hidden",Collections.emptySet())));o.put("wallpaper",p.getInt("wallpaper",0));o.put("pinned",p.getBoolean("wallpaper_pinned",false));try(java.io.OutputStream out=getContentResolver().openOutputStream(uri)){out.write(o.toString(2).getBytes("UTF-8"));}toast("حُفظت تفضيلات الواجهة فقط");}
        if(request==11){String body;try(java.io.InputStream in=getContentResolver().openInputStream(uri);java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream()){byte[] b=new byte[2048];int n;while((n=in.read(b))!=-1){out.write(b,0,n);if(out.size()>65536)throw new Exception();}body=out.toString("UTF-8");}JSONObject o=new JSONObject(body);if(!o.getString("format").equals("kuwait-home-1"))throw new Exception();Set<String> hidden=new HashSet<>();List<String> dock=new ArrayList<>();for(String key:new String[]{"hidden","dock"}){JSONArray a=o.getJSONArray(key);if(a.length()>300)throw new Exception();for(int i=0;i<a.length();i++){String pkg=a.getString(i);if(!pkg.matches("[A-Za-z0-9_]+(?:\\.[A-Za-z0-9_]+)+"))throw new Exception();if(key.equals("hidden"))hidden.add(pkg);else if(dock.size()<Dock.MAX)dock.add(pkg);}}
            if(o.has("display"))kw.urtv.shared.DisplayOptions.decode(o.getJSONObject("display").toString()).save(this);
            p.edit().putStringSet("previous_hidden",new HashSet<>(p.getStringSet("hidden",Collections.emptySet()))).putStringSet("hidden",hidden).putString("dock",String.join(",",dock)).putInt("wallpaper",Math.max(0,o.optInt("wallpaper"))).putBoolean("wallpaper_pinned",o.optBoolean("pinned")).putBoolean("wallpaper_reload",true).apply();sendBroadcast(new Intent(Dock.CHANGED).setPackage(getPackageName()));toast("استُعيدت تفضيلات الواجهة");render();}}
        catch(Exception e){toast("تعذرت قراءة الملف أو حفظه. استخدم نسخة تفضيلات الكويت.");}}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
}
