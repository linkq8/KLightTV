package kw.urtv.screensaver;
import android.os.Bundle;
import android.app.AlertDialog;
import kw.urtv.shared.TvPage;
public class PrayerAdjustActivity extends TvPage {
    private Prefs prefs;
    @Override public void onCreate(Bundle b){super.onCreate(b);prefs=new Prefs(this);render(0);}
    private void render(int focus){title("تصحيح المواقيت · "+prefs.city()[0],"الموجب يؤخر الوقت والسالب يقدمه · محفوظ لهذه المنطقة فقط");String[] names={"الفجر","الشروق","الظهر","العصر","المغرب","العشاء"};int[] a=prefs.adjustments();
        for(int i=0;i<6;i++){final int n=i;addButton(names[i]+" · "+(a[i]>0?"+":"")+a[i]+" دقيقة",()->{String[] items=new String[13];for(int j=0;j<13;j++)items[j]=(j>6?"+":"")+(j-6)+" دقيقة";new AlertDialog.Builder(this).setTitle(names[n]).setSingleChoiceItems(items,a[n]+6,(d,k)->{a[n]=k-6;prefs.setAdjustments(a);d.dismiss();render(n);}).show();});}
        addButton("استعادة المواقيت الأصلية للمنطقة",()->{prefs.setAdjustments(new int[6]);render(0);});addButton("رجوع",this::finish);list.getChildAt(2+focus).requestFocus();}
}
