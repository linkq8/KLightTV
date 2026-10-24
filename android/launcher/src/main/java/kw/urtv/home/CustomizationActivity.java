package kw.urtv.home;
import android.os.Bundle;import android.content.Intent;import kw.urtv.shared.TvPage;
/** Optional destinations; leaving this page does not enable any service. */
public class CustomizationActivity extends TvPage {
 @Override public void onCreate(Bundle b){super.onCreate(b);title("على طريقتك","اختياري · يمكنك العودة لاحقًا من الإعدادات");addButton("التطبيقات · إظهار وإخفاء وإدارة",()->startActivity(new Intent(this,ManageAppsActivity.class)));addButton("الريموت · اختر الزر والتطبيق",()->startActivity(new Intent(this,RemoteSettingsActivity.class)));note("لا تغييرات تلقائية على تطبيقاتك أو أزرارك.");addButton("رجوع إلى الإعداد",this::finish);list.getChildAt(2).requestFocus();}
}
