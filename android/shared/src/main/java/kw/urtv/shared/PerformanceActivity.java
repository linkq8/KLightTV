package kw.urtv.shared;
import android.os.Bundle;
public class PerformanceActivity extends TvPage {
    @Override public void onCreate(Bundle b){super.onCreate(b);render();}
    private void render(){DisplayOptions o=DisplayOptions.load(this);DisplayOptions.Device d=o.device(this);title("الأداء المناسب لجهازك",android.os.Build.MANUFACTURER+" · "+android.os.Build.MODEL);
        note("الوضع الفعلي: "+(d.lite?"خفيف":"عادي")+" · "+d.reason+"\nRAM: "+Math.round(d.ram/1048576.0)+" MiB · حد التطبيق: "+d.heap+" MiB\nحد عرض الصورة: "+d.edge+" بكسل · النص يبقى بدقة الواجهة");
        String[] ids={"auto","normal","lite"},names={"تلقائي · بحسب ذاكرة الجهاز","عادي · تفاصيل أعلى","خفيف · ذاكرة أقل وانتقالات فورية"};
        for(int i=0;i<ids.length;i++){final String id=ids[i];addButton((o.performance.equals(id)?"مختار · ":"")+names[i],()->{o.performance=id;o.save(this);render();});}
        note("الفحص عند فتح الإعدادات. لا تعمل خدمة مراقبة مستمرة. قد يفرض ضغط الذاكرة الوضع الخفيف لحماية التطبيق.");addButton("رجوع",this::finish);list.getChildAt(3).requestFocus();
    }
}
