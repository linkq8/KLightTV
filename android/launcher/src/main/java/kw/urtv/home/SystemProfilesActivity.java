package kw.urtv.home;
import android.content.*;
import android.content.pm.*;
import android.provider.Settings;
import kw.urtv.shared.TvPage;
/** On-demand handoff only. Google owns account selection and authentication. */
public class SystemProfilesActivity extends TvPage {
 private static final String GOOGLE_TV="com.google.android.apps.tv.launcherx";
 @Override public void onCreate(android.os.Bundle b){super.onCreate(b);title("مستخدمو التلفزيون","اختيار الحساب عبر واجهة النظام");
  ComponentName chooser=findChooser();
  if(chooser!=null)addButton("تبديل مستخدم Google TV",()->open(new Intent().setComponent(chooser)));
  Intent home=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).setPackage(GOOGLE_TV);
  boolean available=home.resolveActivity(getPackageManager())!=null;
  if(available){note("من واجهة Google TV، اختر صورة المستخدم ثم الحساب المطلوب.");addButton("فتح Google TV لاختيار المستخدم",()->open(home));}
  else note("واجهة Google TV الأصلية غير متاحة على هذا الجهاز. إعدادات الحسابات لا تضمن تبديل الملف النشط. إذا كانت واجهة Google TV معطّلة، يلزم استعادتها بعد فحص الجهاز.");
  addButton("الحسابات وإعدادات النظام",()->{if(!open(new Intent(Settings.ACTION_SYNC_SETTINGS)))open(new Intent(Settings.ACTION_SETTINGS));});
  note("اختيار المستخدم يتم داخل النظام. إعدادات KLightTV الحالية مشتركة ولا تتغير بتبديل ملف Google TV.");
  addButton("رجوع",this::finish);
 }
 private ComponentName findChooser(){try{PackageInfo info=getPackageManager().getPackageInfo(GOOGLE_TV,PackageManager.GET_ACTIVITIES);if(!info.applicationInfo.enabled)return null;if(info.activities!=null)for(ActivityInfo a:info.activities){if(a.name.endsWith(".profile.chooser.ProfileChooserActivity")&&a.exported&&a.enabled&&(a.permission==null||checkSelfPermission(a.permission)==PackageManager.PERMISSION_GRANTED))return new ComponentName(a.packageName,a.name);}}catch(PackageManager.NameNotFoundException ignored){}return null;}
 private boolean open(Intent i){try{startActivity(i);return true;}catch(ActivityNotFoundException|SecurityException e){android.widget.Toast.makeText(this,"تعذر فتح هذه الوجهة؛ استخدم إعدادات النظام",android.widget.Toast.LENGTH_LONG).show();return false;}}
}
