package kw.urtv.shared;
import android.os.Bundle;
import android.widget.*;
import java.time.*;
import java.time.chrono.HijrahDate;
import java.util.concurrent.*;

/** Actual official records only; the previous Ramadan is explicitly labelled as an archive. */
public class CalendarActivity extends TvPage {
    private CalendarData store;private int year;private final ExecutorService worker=Executors.newSingleThreadExecutor();private Future<?> pending;private volatile boolean stopped;private String status="";
    @Override public void onCreate(Bundle b){super.onCreate(b);store=new CalendarData(this);HijrahDate h=HijrahDate.now(CalendarData.ZONE);year=h.get(java.time.temporal.ChronoField.YEAR);if(h.get(java.time.temporal.ChronoField.MONTH_OF_YEAR)>9)year++;render();}
    private LocalDate start(){return LocalDate.from(HijrahDate.of(year,9,1)).minusDays(2);}
    private void render(){title("إمساكية رمضان · مدينة الكويت", "تقويم العجيري · "+CalendarData.ar(""+year)+" هـ  |  مواقيت محفوظة للاستخدام دون إنترنت");
        LinearLayout actions=new LinearLayout(this);actions.setLayoutDirection(android.view.View.LAYOUT_DIRECTION_RTL);list.addView(actions);
        String[] labels={"العام السابق","العام التالي","تحديث من المصدر","رجوع"};Runnable[] runs={()->{if(pending==null||pending.isDone()){year--;status="";render();}},()->{if(pending==null||pending.isDone()){year++;status="";render();}},this::sync,this::finish};
        for(int i=0;i<labels.length;i++){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.setMargins(dp(4),0,dp(4),dp(12));actions.addView(button(labels[i],runs[i]),p);}
        if(year<HijrahDate.now(CalendarData.ZONE).get(java.time.temporal.ChronoField.YEAR))note("أرشيف رمضان "+CalendarData.ar(""+year)+" — ليست مواعيد رمضان القادم");
        if(!status.isEmpty())note(status);
        row(new String[]{"اليوم / التاريخ","الفجر","الشروق","الظهر","العصر","المغرب","العشاء"},true,false);
        int count=0;LocalDate from=start();for(int i=0;i<34;i++){LocalDate d=from.plusDays(i);CalendarData.Day day=store.get(d);if(day==null||!day.ramadan()||!day.hijri.contains(""+year))continue;String[] cells=new String[7];cells[0]=CalendarData.ar(day.hijri.split(" رمضان")[0]+"  ·  "+d.getDayOfMonth()+"/"+d.getMonthValue());for(int j=0;j<6;j++)cells[j+1]=CalendarData.time(day.times[j]);row(cells,false,d.equals(LocalDate.now(CalendarData.ZONE)));count++;}
        if(count==0){note("لم تتوفر مواعيد موثقة لهذا الشهر في النسخة المحفوظة. اختر «تحديث من المصدر» عند نشرها.");note("لن تُملأ الإمساكية بمواقيت محسوبة أو ببيانات سنة أخرى.");}
        else note("تم حفظ "+CalendarData.ar(""+count)+" يوماً · جميع الأوقات بتوقيت الكويت وبنظام ٢٤ ساعة. قد تكون البيانات غير مكتملة؛ لا تُعرض أيام بلا مصدر.");
        note("المصدر: alojeiri.com · بداية الشهر تخضع للإعلان المحلي. وقت الفجر منفصل عن أي تذكير اختياري بالسحور.");actions.getChildAt(0).requestFocus();
    }
    private void row(String[] cells,boolean header,boolean today){LinearLayout r=new LinearLayout(this);r.setLayoutDirection(android.view.View.LAYOUT_DIRECTION_RTL);r.setPadding(dp(10),dp(10),dp(10),dp(10));if(header||today)r.setBackgroundColor(header?0xFF203B2C:0xFF294532);for(int i=0;i<7;i++){TextView t=text(cells[i],header?14:17,header?0xFFDCB86F:0xFFF6EFE0);t.setGravity(android.view.Gravity.CENTER);t.setTypeface(bold);r.addView(t,new LinearLayout.LayoutParams(0,-2,i==0?1.6f:1));}list.addView(r);android.view.View line=new android.view.View(this);line.setBackgroundColor(0xFF294132);list.addView(line,new LinearLayout.LayoutParams(-1,dp(1)));}
    private void sync(){if(pending!=null&&!pending.isDone())return;stopped=false;status="جارٍ التحقق من بيانات المصدر…";render();final LocalDate from=start();pending=worker.submit(()->{int n=0;String result;try{for(int i=0;i<34&&!Thread.currentThread().isInterrupted();i++){LocalDate date=from.plusDays(i);if(store.get(date)==null)store.fetch(date);n++;}result="تم تحديث البيانات المتاحة من موقع العجيري.";}catch(Exception e){result="توقف التحديث: لم يقدّم المصدر بيانات صالحة للتاريخ المطلوب، أو تعذر الاتصال. بقيت البيانات المحفوظة.";}final String message=result;if(!stopped)runOnUiThread(()->{if(!isFinishing()){status=message;pending=null;render();}});});}
    @Override public void onStop(){stopped=true;if(pending!=null)pending.cancel(true);super.onStop();}
    @Override public void onDestroy(){worker.shutdownNow();super.onDestroy();}
}
