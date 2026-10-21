package kw.urtv.screensaver;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/** Remote-friendly settings: every row is a button that opens a list. */
public class SettingsActivity extends Activity {
    private static final int DIALOG_THEME = android.R.style.Theme_Material_Dialog_Alert;

    private Prefs prefs;
    private LinearLayout list;

    /** Arabic locale for this screen, so every dialog title, list and button is laid out right-to-left. */
    @Override
    protected void attachBaseContext(android.content.Context base) {
        android.content.res.Configuration c = new android.content.res.Configuration(base.getResources().getConfiguration());
        c.setLocale(new java.util.Locale("ar"));
        c.setLayoutDirection(new java.util.Locale("ar"));
        super.attachBaseContext(base.createConfigurationContext(c));
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = new Prefs(this);
        ScrollView scroll = new ScrollView(this);
        scroll.setBackground(new android.graphics.drawable.GradientDrawable(
            android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM, new int[]{0xFF0A2016, 0xFF04120C, 0xFF020806}));
        scroll.setVerticalScrollBarEnabled(false);
        android.widget.FrameLayout frame = new android.widget.FrameLayout(this);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        list.setPadding(dp(8), dp(36), dp(8), dp(48));
        int width = Math.min(getResources().getDisplayMetrics().widthPixels - dp(64), dp(860));
        frame.addView(list, new android.widget.FrameLayout.LayoutParams(width, -2, Gravity.CENTER_HORIZONTAL));
        scroll.addView(frame);
        setContentView(scroll);
        render();
    }

    private void render() {
        list.removeAllViews();
        TextView title = new TextView(this);
        title.setText("KLightTV · الكويت");
        title.setTextColor(0xFFF6EFE0);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
        title.setTypeface(font("ThmanyahDisplay-Bold"));
        list.addView(title);
        TextView sub = new TextView(this);
        sub.setText("إعدادات شاشة التوقف");
        sub.setTextColor(0xFFC6BBA4);
        sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        sub.setTypeface(font("Thmanyah-Regular"));
        sub.setPadding(0, 0, 0, dp(18));
        list.addView(sub);

        row("معاينة الشاشة", "شاهد الشاشة الآن", R.drawable.ic_play, new Runnable() { public void run() {
            startActivity(new Intent(SettingsActivity.this, MainActivity.class)); } }).requestFocus();

        row("إمساكية رمضان", "مدينة الكويت · بيانات رزنامة العجيري", R.drawable.ic_star, () -> startActivity(new Intent(this,kw.urtv.shared.CalendarActivity.class)));
        header("الموقع");
        row("المدينة", prefs.city()[0], R.drawable.ic_place, new Runnable() { public void run() { pickCity(); } });
        row("تصحيح المواقيت بالدقائق", "اختياري · محفوظ لكل منطقة", R.drawable.ic_settings, () -> startActivity(new Intent(this,PrayerAdjustActivity.class)));
        row("مصدر مواقيت المنطقة", "الفرق المنشور أو الحساب بالإحداثيات", R.drawable.ic_place, () -> {String[] c=prefs.city();kw.urtv.shared.RegionCatalog.Region region=kw.urtv.shared.RegionCatalog.find(Double.parseDouble(c[1]),Double.parseDouble(c[2]));new AlertDialog.Builder(this,DIALOG_THEME).setTitle(c[0]).setMessage((region==null?"حساب بالإحداثيات":region.description())+"\n"+(region!=null&&region.offset!=null?"جدول العجيري لمدينة الكويت + فرق الرزنامة المنشور\n"+kw.urtv.shared.RegionCatalog.OFFSET_SOURCE:"لم يتوفر فرق رسمي موثق لهذه المنطقة. تُحسب يوميًا بطريقة الكويت: الفجر 18° والعشاء 17.5°.\nالموقع المختار: "+c[1]+" / "+c[2]+"\nالخيران: سجل GeoNames285790 عند اختيارها")+"\nالتصحيح اليدوي يظهر بوسم معدّلة.").setPositiveButton("حسنًا",null).show();});
        row("خيارات القراءة", "الحجم والخط والتباين والأرقام", R.drawable.ic_eye, () -> startActivity(new Intent(this,kw.urtv.shared.DisplaySettingsActivity.class)));
        row("أداء الجهاز", "تلقائي · عادي · خفيف", R.drawable.ic_settings, () -> startActivity(new Intent(this,kw.urtv.shared.PerformanceActivity.class)));

        header("بجانب الساعة");
        row("المحتوى", Prefs.panelLabel(prefs.panel()), R.drawable.ic_eye, new Runnable() { public void run() { pickPanel(false); } });

        header("الشاشة الرئيسية (KLightTV)");
        row("بطاقة على الشاشة الرئيسية", Prefs.panelLabel(prefs.homePanel()), R.drawable.ic_eye,
            new Runnable() { public void run() { pickPanel(true); } });
        row("ترتيب التطبيقات المفضلة", "قائمة ترتّب فيها المفضلة بالأسهم", R.drawable.ic_star,
            new Runnable() { public void run() { openDockSettings(); } });

        header("التطبيق");
        row("وقت تشغيل شاشة التوقف", "يفتح إعدادات التلفاز (قد تظهر بالإنجليزية)", R.drawable.ic_screensaver, new Runnable() { public void run() { openDreamSettings(); } });
    }

    // ---------- pickers ----------

    private void pickCity() {
        final Cities.Region[] regions = Cities.REGIONS;
        String[] labels = new String[regions.length + 1];
        labels[0] = "بحث عن مدينة أو قرية…";
        for (int i = 0; i < regions.length; i++) labels[i + 1] = regions[i].name + "  (" + regions[i].cities.length + ")";
        new AlertDialog.Builder(this, DIALOG_THEME).setTitle("المنطقة")
            .setItems(labels, (d, i) -> { if (i == 0) searchCity(); else pickCityIn(regions[i - 1]); })
            .show();
    }

    private void pickCityIn(final Cities.Region r) {
        String[] names = new String[r.cities.length];
        int sel = -1;
        for (int i = 0; i < names.length; i++) {
            names[i] = r.cities[i][0];
            if (names[i].equals(prefs.city()[0])) sel = i;
        }
        new AlertDialog.Builder(this, DIALOG_THEME).setTitle(r.name)
            .setSingleChoiceItems(names, sel, (d, i) -> {
                prefs.setCity(r.cities[i][0], r.cities[i][1], r.cities[i][2]);
                d.dismiss();
                render();
            }).show();
    }

    // any place in Kuwait, from Open-Meteo's GeoNames search
    private void searchCity() {
        ask("اسم المدينة أو القرية", q -> fetch("https://geocoding-api.open-meteo.com/v1/search?count=20&language=ar&countryCode=KW&name="
            + Uri.encode(q), json -> {
                JSONArray a = new JSONObject(json).optJSONArray("results");
                if (a == null || a.length() == 0) { toast("لا توجد نتائج"); return; }
                final int n = a.length();
                final String[] names = new String[n], lats = new String[n], lons = new String[n], rows = new String[n];
                for (int i = 0; i < n; i++) {
                    JSONObject c = a.getJSONObject(i);
                    names[i] = c.optString("name");
                    lats[i] = String.valueOf(c.optDouble("latitude"));
                    lons[i] = String.valueOf(c.optDouble("longitude"));
                    String region = c.optString("admin1", "");
                    rows[i] = region.isEmpty() ? names[i] : names[i] + "  ·  " + region;
                }
                new AlertDialog.Builder(this, DIALOG_THEME).setTitle("اختر المكان")
                    .setItems(rows, (d, i) -> { prefs.setCity(names[i], lats[i], lons[i]); render(); }).show();
            }));
    }

    /** «ترتيب المفضلة» lives in the launcher (it owns the row). */
    private void openDockSettings() {
        try {
            startActivity(new Intent().setClassName("kw.urtv.home", "kw.urtv.home.DockActivity"));
        } catch (RuntimeException e) {
            toast("ثبّت الواجهة الرئيسية «KLightTV» أولاً");
        }
    }

    /** The card beside the screensaver's clock, or (home) the one on the launcher's home screen. */
    private void pickPanel(final boolean home) {
        String[] labels = new String[Prefs.PANELS.length];
        String current = home ? prefs.homePanel() : prefs.panel();
        int sel = 0;
        for (int i = 0; i < labels.length; i++) {
            labels[i] = Prefs.PANELS[i][1];
            if (Prefs.PANELS[i][0].equals(current)) sel = i;
        }
        new AlertDialog.Builder(this, DIALOG_THEME).setTitle(home ? "بطاقة على الشاشة الرئيسية" : "بجانب الساعة")
            .setSingleChoiceItems(labels, sel, (d, i) -> {
                if (home) prefs.setHomePanel(Prefs.PANELS[i][0]); else prefs.setPanel(Prefs.PANELS[i][0]);
                d.dismiss();
                render();
            })
            .show();
    }

    private void openDreamSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_DREAM_SETTINGS));
        } catch (ActivityNotFoundException e) {
            startActivity(new Intent(Settings.ACTION_SETTINGS));
        }
    }

    // ---------- small UI helpers ----------

    private final java.util.Map<String, Typeface> fonts = new java.util.HashMap<>();

    /** Thmanyah, shared with the launcher's assets; falls back to the system font. */
    private Typeface font(String name) {
        Typeface tf = fonts.get(name);
        if (tf == null) {
            try { tf = Typeface.createFromAsset(getAssets(), "fonts/" + name + ".otf"); }
            catch (RuntimeException e) { tf = Typeface.DEFAULT; }
            fonts.put(name, tf);
        }
        return tf;
    }

    private void header(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextColor(0xFFD6AF6B);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        t.setTypeface(font("Thmanyah-Medium"));
        t.setLetterSpacing(0.05f);
        t.setPadding(dp(6), dp(22), dp(6), dp(8));
        list.addView(t);
    }

    /** A card row: icon, label and its current value on a second line, chevron; gold when focused. */
    private View row(String label, String value, int icon, final Runnable onClick) {
        final LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        card.setPadding(dp(18), dp(12), dp(18), dp(12));
        card.setFocusable(true);
        card.setClickable(true);
        final android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setCornerRadius(dp(14));
        bg.setColor(0x14FFFFFF);
        bg.setStroke(dp(2), 0x1FD6AF6B);
        card.setBackground(bg);

        final android.widget.ImageView ic = new android.widget.ImageView(this);
        final android.graphics.drawable.Drawable d = getDrawable(icon).mutate();
        d.setTint(0xFFD6AF6B);
        ic.setImageDrawable(d);
        card.addView(ic, new LinearLayout.LayoutParams(dp(24), dp(24)));

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        final TextView name = new TextView(this);
        name.setText(label);
        name.setTextColor(0xFFF6EFE0);
        name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        name.setTypeface(font("Thmanyah-Medium"));
        name.setSingleLine(true);
        texts.addView(name);
        final TextView val = new TextView(this);
        if (value != null) {
            val.setText(value);
            val.setTextColor(0xFFC6BBA4);
            val.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            val.setTypeface(font("Thmanyah-Regular"));
            val.setSingleLine(true);
            val.setEllipsize(android.text.TextUtils.TruncateAt.END);
            texts.addView(val);
        }
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, -2, 1f);
        tp.setMarginStart(dp(16));
        card.addView(texts, tp);

        final TextView chevron = new TextView(this);
        chevron.setText("›");   // shown mirrored (‹) in the RTL row
        chevron.setTextColor(0x99C6BBA4);
        chevron.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        card.addView(chevron);

        card.setOnClickListener(v -> onClick.run());
        card.setOnFocusChangeListener((v, has) -> {
            // No scaling: a scaled rounded card overlapped its neighbours and blurred the text.
            bg.setColor(has ? 0x33D6AF6B : 0x14FFFFFF);
            bg.setStroke(dp(2), has ? 0xFFD6AF6B : 0x1FD6AF6B);
            chevron.setTextColor(has ? 0xFFD6AF6B : 0x99C6BBA4);
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(8);
        list.addView(card, lp);
        return card;
    }

    interface Text { void got(String s); }
    interface Json { void got(String json) throws Exception; }

    private void ask(String hint, final Text then) {
        final EditText in = new EditText(this);
        in.setHint(hint);
        in.setSingleLine(true);
        in.setInputType(InputType.TYPE_CLASS_TEXT);
        new AlertDialog.Builder(this, DIALOG_THEME).setTitle("بحث").setView(in)
            .setPositiveButton("بحث", (d, w) -> {
                String q = in.getText().toString().trim();
                if (!q.isEmpty()) then.got(q);
            })
            .setNegativeButton("إلغاء", null).show();
        in.requestFocus();
    }

    /** GET on a background thread, result handled on the UI thread. */
    private void fetch(final String url, final Json then) {
        toast("جارٍ البحث…");
        new Thread(() -> {
            String body = null;
            HttpURLConnection con = null;
            try {
                con = (HttpURLConnection) new URL(url).openConnection();
                con.setConnectTimeout(12000);
                con.setReadTimeout(15000);
                InputStream in = con.getInputStream();
                ByteArrayOutputStream b = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) b.write(buf, 0, n);
                in.close();
                body = b.toString("UTF-8");
            } catch (Exception ignored) {
            } finally {
                if (con != null) con.disconnect();
            }
            final String result = body;
            runOnUiThread(() -> {
                if (isFinishing()) return;
                if (result == null) { toast("لا يوجد اتصال"); return; }
                try { then.got(result); } catch (Exception e) { toast("تعذرت قراءة النتائج"); }
            });
        }).start();
    }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
