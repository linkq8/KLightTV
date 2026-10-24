package kw.urtv.home;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.net.wifi.WifiManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Apple TV style home screen: clock top-left, pills top-right, the focused dock app's name over an
 * ambient glow, a dock of pinned apps, and every other app in a grid that scrolls up on Down.
 * Plain framework views only (no AndroidX). Network only for the optional prayer/weather card (HomeCard).
 *
 * Adapted from github.com/mihirgupta0900/tv-home (MIT, see android/launcher/LICENSE-tv-home):
 * Arabic text and digits, Hijri date, Thmanyah fonts, the screensaver's gold palette and Kuwait photos,
 * clock on the right, and a pill that opens the screensaver's settings.
 */
public class MainActivity extends Activity {

    // Dock, hidden apps, Live TV and grid width come from res/values/config.xml.
    private String[] dockApps;
    private String liveTvPkg, inputPickerPkg;
    private int cols;

    static final class App {
        String pkg, label, category;   // category: the one it was found with, LEANBACK_LAUNCHER or LAUNCHER
        ComponentName component;
        Drawable art;
        boolean isBanner;
        int color;
    }

    private final Map<String, Integer> colorCache = new HashMap<>();
    private final List<View> dockCards = new ArrayList<>();
    private final List<View> gridCards = new ArrayList<>();
    private final List<View> pills = new ArrayList<>();

    private HomeScrollView scroll;
    private LinearLayout content, dockRow;
    private FrameLayout hero;
    private LinearLayout grid;
    private View glow;
    private ImageView wallBack, wallFront;
    private View gridShade;
    private int wallIndex = -1;
    private long wallChangedAt;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable rotateWallpaper = this::nextWallpaper;
    static final long WALLPAPER_INTERVAL_MS = 30 * 60 * 1000;
    private TextView greeting, clock, ampm, date, title, wifiPill, appsLabel;
    private int glowColor = 0xFF3A3A3A;
    private ValueAnimator glowAnim;
    private int lastFocusedId = View.NO_ID;
    private boolean firstShow = true;
    private int packageSeq;
    private PopupWindow inputsPopup;
    private HomeCard homeCard;
    private kw.urtv.shared.CalendarData calendarStore;
    private kw.urtv.shared.Palette palette;private String builtTheme,builtSurface;private boolean tickRegistered;private kw.urtv.shared.DisplayOptions displayOptions;
    private View readingShade;
    private boolean lite;
    private boolean visible;
    private int imageEdge=3840;
    private String displayIdentity="";

    private final BroadcastReceiver tick = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) { updateClock(); updateWifi(); }
    };
    private final BroadcastReceiver packages = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) { buildApps(); }
    };
    // favourites changed in «ترتيب المفضلة» or from ADB while the home screen is up
    private final BroadcastReceiver dockChanged = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) { dockApps = loadDock(); buildApps(); }
    };

    static boolean shouldOfferSetup(android.content.Context c){if(Dock.prefs(c).getBoolean("setup_complete",false)||Dock.prefs(c).getBoolean("quick_setup_seen",false))return false;try{android.content.pm.PackageInfo info=c.getPackageManager().getPackageInfo(c.getPackageName(),0);return info.lastUpdateTime-info.firstInstallTime<=1000;}catch(Exception ignored){return false;}}
    // ---------------------------------------------------------------- lifecycle

    @Override
    protected void onCreate(Bundle b) {
        setTheme(new kw.urtv.shared.Palette(this,kw.urtv.shared.DisplayOptions.load(this)).light?android.R.style.Theme_Material_Light_NoActionBar:android.R.style.Theme_Material_NoActionBar);super.onCreate(b);
        displayOptions=kw.urtv.shared.DisplayOptions.load(this);palette=new kw.urtv.shared.Palette(this,displayOptions);
        kw.urtv.shared.DisplayOptions.Device device=displayOptions.device(this);lite=device.lite;imageEdge=device.edge;
        calendarStore=new kw.urtv.shared.CalendarData(this);
        dockApps = loadDock();
        liveTvPkg = getString(R.string.live_tv_package);
        inputPickerPkg = getString(R.string.input_picker_package);
        cols = getResources().getInteger(R.integer.columns);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        setContentView(buildUi());
        buildApps();

        IntentFilter pf = new IntentFilter();
        pf.addAction(Intent.ACTION_PACKAGE_ADDED);
        pf.addAction(Intent.ACTION_PACKAGE_REMOVED);
        pf.addAction(Intent.ACTION_PACKAGE_CHANGED);
        pf.addDataScheme("package");
        registerReceiver(packages, pf);
        registerReceiver(dockChanged, new IntentFilter(Dock.CHANGED));
        if(shouldOfferSetup(this))startActivity(new Intent(this,SetupActivity.class));
    }

    @Override
    protected void onResume() {
        super.onResume();
        visible=true;
        displayOptions=kw.urtv.shared.DisplayOptions.load(this);kw.urtv.shared.DisplayOptions.Device device=displayOptions.device(this);lite=device.lite;imageEdge=device.edge;
        if(!displayOptions.theme.equals(builtTheme)||!(displayOptions.surface+displayOptions.focusColor).equals(builtSurface)){recreate();return;}
        String identity=displayOptions.json()+":"+imageEdge+":"+kw.urtv.shared.CityWallpaper.identity(kw.urtv.shared.Place.load(this));if(!identity.equals(displayIdentity)){displayIdentity=identity;wallIndex=-1;wallChangedAt=0;wallFront.animate().cancel();wallBack.animate().cancel();wallFront.setImageDrawable(null);wallBack.setImageDrawable(null);}
        readingShade.setBackgroundColor(Color.argb(displayOptions.overlayAlpha(),0,0,0));
        clock.setTypeface(displayOptions.usesLatin()?font("medium"):font("display"));
        clock.setTextSize(58*displayOptions.textScale());date.setTextSize(14*displayOptions.textScale());greeting.setTextSize(15*displayOptions.textScale());
        title.setVisibility(dockCards.isEmpty()?View.VISIBLE:View.GONE);
        IntentFilter tf = new IntentFilter(Intent.ACTION_TIME_TICK);
        tf.addAction(Intent.ACTION_TIME_CHANGED);
        tf.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        tf.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        registerReceiver(tick, tf);tickRegistered=true;
        updateClock();
        updateWifi();
        homeCard.refresh();
        dockApps=loadDock();buildApps();
        if(prefs().getBoolean("wallpaper_reload",false)){wallIndex=-1;wallChangedAt=0;prefs().edit().putBoolean("wallpaper_reload",false).apply();}
        View last = lastFocusedId == View.NO_ID ? null : findViewById(lastFocusedId);
        if (last == null && displayOptions.focused()) last=pills.get(0);
        if (last == null && !dockCards.isEmpty()) last = dockCards.get(0);
        if (last != null) last.requestFocus();
        // Catch installs/removals that happened while another app was in front.
        if (getPackageManager().getChangedPackages(packageSeq) != null) buildApps();
        if (firstShow) { firstShow = false; playEntrance(); }
        // New photo on return if the current one has been up for the full interval.
        if (wallIndex < 0 || SystemClock.elapsedRealtime() - wallChangedAt >= WALLPAPER_INTERVAL_MS) nextWallpaper();
        else scheduleWallpaper(WALLPAPER_INTERVAL_MS - (SystemClock.elapsedRealtime() - wallChangedAt));
    }

    @Override
    protected void onPause() {
        super.onPause();
        visible=false;
        handler.removeCallbacks(rotateWallpaper);
        if(tickRegistered){unregisterReceiver(tick);tickRegistered=false;}
        if (inputsPopup != null) inputsPopup.dismiss();
        homeCard.pause();
    }

    @Override protected void onStop(){super.onStop();wallFront.setImageDrawable(null);wallBack.setImageDrawable(null);wallIndex=-1;}
    @Override public void onTrimMemory(int level){super.onTrimMemory(level);if(kw.urtv.shared.HardwarePolicy.shouldTrim(level)){kw.urtv.shared.DisplayOptions.memoryPressure=true;lite=true;imageEdge=1280;wallIndex=-1;if(hasWindowFocus())nextWallpaper();}}
    @Override
    protected void onDestroy() {
        unregisterReceiver(packages);
        unregisterReceiver(dockChanged);
        homeCard.destroy();
        super.onDestroy();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if(shouldOfferSetup(this)){startActivity(new Intent(this,SetupActivity.class));return;}
        if(displayOptions.focused()){scroll.scrollTo(0,0);pills.get(0).requestFocus();return;}
        // Home pressed while already home: jump back to the first dock app.
        if (inputsPopup != null) inputsPopup.dismiss();
        if (!dockCards.isEmpty()) dockCards.get(0).requestFocus();
        scroll.smoothScrollTo(0, 0);
    }

    @Override public boolean dispatchKeyEvent(android.view.KeyEvent event){
        if(displayOptions!=null&&displayOptions.focused()&&event.getAction()==android.view.KeyEvent.ACTION_DOWN){
            View focused=getCurrentFocus();
            if(event.getKeyCode()==android.view.KeyEvent.KEYCODE_DPAD_UP&&dockCards.contains(focused)){scroll.scrollTo(0,0);pills.get(0).requestFocus();return true;}
            if(event.getKeyCode()==android.view.KeyEvent.KEYCODE_DPAD_DOWN&&pills.contains(focused)&&!dockCards.isEmpty()){scroll.scrollTo(0,hero.getHeight());dockCards.get(0).requestFocus();return true;}
        }
        return super.dispatchKeyEvent(event);
    }
    @Override
    public void onBackPressed() {
        if(displayOptions.focused()){scroll.scrollTo(0,0);pills.get(0).requestFocus();return;}

        // Home has nowhere to go back to; Back from the grid returns to the dock.
        if (scroll.getScrollY() > 0 && !dockCards.isEmpty()) dockCards.get(0).requestFocus();
    }

    // ---------------------------------------------------------------- layout

    private View buildUi() {
        builtTheme=displayOptions.theme;builtSurface=displayOptions.surface+displayOptions.focusColor;
        FrameLayout root = new FrameLayout(this);
        if(displayOptions.focused())root.setBackground(new kw.urtv.shared.FocusBackdrop(displayOptions.focusColor));else root.setBackground(new kw.urtv.shared.FocusBackdrop(palette.light?"white":"blue"));

        // Wallpaper: two stacked layers so a new photo can crossfade in over the old one.
        wallBack = wallpaperLayer();
        wallFront = wallpaperLayer();
        root.addView(wallBack, new FrameLayout.LayoutParams(-1, -1));
        root.addView(wallFront, new FrameLayout.LayoutParams(-1, -1));
        // Scrim: darker at the top (clock, pills) and bottom (dock) so text stays readable.
        View scrim = new View(this);
        if(displayOptions.focused())scrim.setBackgroundColor(Color.TRANSPARENT);else scrim.setBackground(new android.graphics.drawable.LayerDrawable(new android.graphics.drawable.Drawable[]{new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0x50121922,0x10121922,0x90121922}),new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[]{0x66101D2D,0x10101D2D,0xB3101D2D})}));
        root.addView(scrim, new FrameLayout.LayoutParams(-1, -1));
        readingShade=new View(this);root.addView(readingShade,new FrameLayout.LayoutParams(-1,-1));
        gridShade = new View(this);
        gridShade.setBackgroundColor(0xB3000000);
        gridShade.setAlpha(0f);
        root.addView(gridShade, new FrameLayout.LayoutParams(-1, -1));


        glow = new View(this);
        root.addView(glow, new FrameLayout.LayoutParams(-1, -1));
        setGlow(glowColor);glow.setVisibility(View.GONE);

        scroll = new HomeScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.setFillViewport(true);
        root.addView(scroll, new FrameLayout.LayoutParams(-1, -1));

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setClipChildren(false);
        content.setClipToPadding(false);
        scroll.addView(content, new FrameLayout.LayoutParams(-1, -2));

        // Hero: exactly one screen tall, header on top, title + dock at the bottom.
        hero = new FrameLayout(this);
        hero.setClipChildren(false);
        int screenH = getResources().getDisplayMetrics().heightPixels;
        content.addView(hero, new LinearLayout.LayoutParams(-1, screenH));

        hero.addView(buildHeader(), lp(-1, -2, Gravity.TOP, dp(40), dp(20), dp(40), 0));
        // One transparent surface for the table below the pills and weather below the native clock.
        homeCard = new HomeCard(this, hero, lp(-1, getResources().getDisplayMetrics().heightPixels-dp(displayOptions.focused()?80:105), Gravity.LEFT | Gravity.TOP, dp(40), dp(displayOptions.focused()?56:80), dp(40), 0));

        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.VERTICAL);
        bottom.setClipChildren(false);
        bottom.setClipToPadding(false);
        if(displayOptions.focused())content.addView(bottom,llp(-1,-2,dp(40),0,dp(40),dp(20)));
        else hero.addView(bottom, lp(-1, -2, Gravity.BOTTOM, dp(40), 0, dp(40), dp(20)));

        title = text(34, "sans-serif-light", Color.WHITE);
        title.setShadowLayer(dp(18), 0, 0, 0x66000000);
        title.setSingleLine(true);
        bottom.addView(title, new LinearLayout.LayoutParams(-2, -2));
        ((LinearLayout.LayoutParams) title.getLayoutParams()).setMargins(dp(8), 0, 0, dp(14));

        dockRow = new LinearLayout(this);
        dockRow.setOrientation(LinearLayout.HORIZONTAL);
        dockRow.setGravity(Gravity.CENTER);
        dockRow.setClipChildren(false);
        dockRow.setClipToPadding(false);
        dockRow.setPadding(dp(8), dp(10), dp(8), dp(10));
        GradientDrawable band = new GradientDrawable();
        band.setCornerRadius(dp(26));
        band.setColor(0x1FFFFFFF);
        band.setStroke(dp(1), 0x14FFFFFF);
        dockRow.setBackgroundColor(Color.TRANSPARENT);
        bottom.addView(dockRow, new LinearLayout.LayoutParams(-1, -2));

        appsLabel = text(11, "sans-serif-medium", 0x99FFFFFF);
        appsLabel.setText("كل التطبيقات");
        appsLabel.setTextColor(palette.accent);
        content.addView(appsLabel, llp(-2, -2, dp(68), dp(8), 0, dp(18)));

        grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setClipChildren(false);
        grid.setClipToPadding(false);
        content.addView(grid, llp(-1, -2, dp(68), 0, dp(68), dp(160)));
        return root;
    }

    private View buildHeader() {
        FrameLayout header = new FrameLayout(this);
        header.setClipChildren(false);

        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        // Top right, stacked tight: greeting, clock, date. Gravity.RIGHT, not END: for Arabic text END is the left.
        greeting = text(13, "medium", 0xFF8EDBEA);
        greeting.setSingleLine(true);
        left.addView(greeting, llp(-2, -2, 0, 0, dp(4), 0));
        greeting.setVisibility(View.GONE);

        LinearLayout clockRow = new LinearLayout(this);
        clockRow.setGravity(Gravity.BOTTOM);
        clockRow.setGravity(Gravity.TOP);
        clock = text(68, "medium", 0xFFF1F5FA);
        clock.setIncludeFontPadding(false);
        ampm = text(16, "medium", GOLD);
        clockRow.addView(ampm, llp(-2, -2, 0, dp(6), dp(8), 0));
        clockRow.addView(clock);
        left.addView(clockRow, llp(-2, -2, 0, dp(-2), 0, 0));

        date = text(13, "regular", 0xFFB9CADA);
        date.setSingleLine(true);
        left.addView(date, llp(-2, -2, 0, dp(-6), dp(4), 0));   // the display font leaves a deep gap under its digits
        for (TextView t : new TextView[]{greeting, clock, ampm, date}) t.setShadowLayer(palette.light?0:dp(3), 0, palette.light?0:dp(1), palette.light?Color.TRANSPARENT:0x80000000);
        left.setGravity(Gravity.CENTER_HORIZONTAL);
        for (TextView t : new TextView[]{greeting, date}) t.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams clockLayout=new FrameLayout.LayoutParams(displayOptions.focused()?(getResources().getDisplayMetrics().widthPixels-dp(104))/2:dp(280),-2,Gravity.RIGHT|Gravity.TOP);clockLayout.topMargin=dp(displayOptions.focused()?44:68);header.addView(left,clockLayout);
        if(displayOptions.focused())left.setVisibility(View.GONE);
        left.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{if(homeCard!=null)homeCard.alignBelowClock(b+dp(20));});

        LinearLayout right = new LinearLayout(this);
        right.setGravity(Gravity.CENTER_VERTICAL);
        right.setClipChildren(false);
        TextView saver = pill("التطبيقات", R.drawable.ic_apps);
        saver.setOnClickListener(v -> launch(new Intent(this,ManageAppsActivity.class)));
        TextView inputs = pill("HDMI والمصادر", R.drawable.ic_input);
        inputs.setOnClickListener(v -> showInputs(v));
        TextView channels=pill("القنوات",R.drawable.ic_input);channels.setOnClickListener(v->openChannels());
        wifiPill = pill("واي فاي", R.drawable.ic_wifi);
        wifiPill.setOnClickListener(v -> launch(new Intent(Settings.ACTION_WIFI_SETTINGS)));
        TextView settings = pill("إعدادات النظام", R.drawable.ic_settings);
        settings.setOnClickListener(v -> launch(new Intent(Settings.ACTION_SETTINGS)));
        TextView reminder=pill("التذكير",R.drawable.ic_screensaver);reminder.setOnClickListener(v->launch(new Intent().setClassName(SAVER_PKG,"kw.urtv.screensaver.ReminderActivity")));
        TextView updates=pill("التحديث",R.drawable.ic_update);updates.setOnClickListener(v->launch(new Intent(this,UpdateActivity.class)));
        TextView football=pill("المباريات",R.drawable.ic_apps);football.setOnClickListener(v->launch(new Intent(this,FootballActivity.class)));
        TextView profiles=pill("المستخدمون",R.drawable.ic_person);profiles.setOnClickListener(v->launch(new Intent(this,SystemProfilesActivity.class)));
        for (TextView p : new TextView[]{settings, inputs, profiles}) {
            right.addView(p, llp(-2, dp(30), dp(10), 0, 0, 0));
            pills.add(p);
        }
        header.addView(right, new FrameLayout.LayoutParams(-2, -2, Gravity.LEFT | Gravity.TOP));
        return header;
    }

    /** Pill button: label with a Material icon on its right (Arabic reading order), tinted with the text. */
    private TextView pill(String label, int icon) {
        TextView p = text(12, "sans-serif-medium", 0xE6FFFFFF);
        p.setText(label);
        p.setGravity(Gravity.CENTER);
        android.graphics.drawable.Drawable ic = getDrawable(icon).mutate();
        ic.setBounds(0, 0, dp(16), dp(16));
        ic.setTint(palette.ink);
        p.setCompoundDrawables(null, null, ic, null);
        p.setCompoundDrawablePadding(dp(8));
        p.setPadding(dp(14), 0, dp(14), 0);
        p.setFocusable(true);
        p.setId(View.generateViewId());
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(6));
        bg.setColor(palette.panel);
        bg.setStroke(0,Color.TRANSPARENT);
        p.setBackground(bg);
        p.setOnFocusChangeListener((v, has) -> {
            bg.setColor(has ? palette.accent : palette.panel);
            ((TextView) v).setTextColor(has ? (palette.light?Color.WHITE:0xFF111111) : palette.ink);
            ic.setTint(has ? (palette.light?Color.WHITE:0xFF111111) : palette.ink);
            v.animate().scaleX(has ? 1.03f : 1f).scaleY(has ? 1.03f : 1f)
                    .setInterpolator(new DecelerateInterpolator()).setDuration(lite?0:120).start();
            if (has) { lastFocusedId = v.getId(); scroll.smoothScrollTo(0, 0); fadeGlow(1f); }
        });
        return p;
    }

    // ---------------------------------------------------------------- apps

    private void buildApps() {
        PackageManager pm = getPackageManager();
        android.content.pm.ChangedPackages changed = pm.getChangedPackages(packageSeq);
        if (changed != null) packageSeq = changed.getSequenceNumber();
        Map<String, App> byPkg = new HashMap<>();
        for (ResolveInfo ri : Dock.catalogue(this)) {
            ActivityInfo ai = ri.activityInfo;
            App a = new App();
            a.pkg = ai.packageName;
            a.label = String.valueOf(ri.loadLabel(pm));
            a.component = new ComponentName(ai.packageName, ai.name);
            a.category = Dock.isTv(ri) ? Intent.CATEGORY_LEANBACK_LAUNCHER : Intent.CATEGORY_LAUNCHER;
            Drawable banner = BrandBanner.forPackage(this, ai.packageName);
            if (banner == null) banner = ai.loadBanner(pm);
            if (banner == null) banner = ai.applicationInfo.loadBanner(pm);
            a.isBanner = banner != null;
            a.art = banner != null ? banner : ri.loadIcon(pm);
            a.color = sampleColor(a.pkg, a.art);
            byPkg.put(a.pkg, a);
        }

        List<App> dock = new ArrayList<>();
        for (String p : dockApps) if (dock.size()<MAX_DOCK && byPkg.containsKey(p)) dock.add(byPkg.remove(p));
        cardByPkg.clear();
        List<App> rest = new ArrayList<>(byPkg.values());
        Collections.sort(rest, (x, y) -> x.label.compareToIgnoreCase(y.label));
        // Fewer than MIN_DOCK of the chosen apps installed: top the dock up from the rest.
        while (dock.size() < MIN_DOCK && !rest.isEmpty()) dock.add(rest.remove(0));

        dockRow.removeAllViews();
        grid.removeAllViews();
        dockCards.clear();
        gridCards.clear();

        for (int i = 0; i < dock.size(); i++) {
            View tile = tile(dock.get(i), true);
            int cardWidth=Math.min(dp(displayOptions.focused()?104:88),(getResources().getDisplayMetrics().widthPixels-dp(160))/Math.max(1,dock.size()));
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(cardWidth, -2);
            p.setMargins(dp(10), 0, dp(10), 0);
            dockRow.addView(tile, p);
        }

        LinearLayout row = null;
        for (int i = 0; i < rest.size(); i++) {
            if (i % cols == 0) {
                row = new LinearLayout(this);
                row.setClipChildren(false);
                row.setClipToPadding(false);
                grid.addView(row, llp(-1, -2, 0, 0, 0, dp(26)));
            }
            View tile = tile(rest.get(i), false);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1f);
            p.setMargins(i % cols == 0 ? 0 : dp(10), 0, i % cols == cols - 1 ? 0 : dp(10), 0);
            row.addView(tile, p);
        }
        // Pad the last row so tiles keep their width.
        if (row != null) for (int i = rest.size() % cols; i != 0 && i < cols; i++) {
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, 1, 1f);
            p.setMargins(dp(10), 0, i == cols - 1 ? 0 : dp(10), 0);
            row.addView(new View(this), p);
        }
        appsLabel.setVisibility(rest.isEmpty() ? View.GONE : View.VISIBLE);
        wireFocus();

        title.setVisibility(dock.isEmpty()?View.VISIBLE:View.GONE);
        if (!dock.isEmpty()) { title.setText(dock.get(0).label); setGlow(dock.get(0).color); }else{title.setText("اختر تطبيقاتك من «تطبيقاتي»");pills.get(0).requestFocus();}
        View last = lastFocusedId == View.NO_ID ? null : findViewById(lastFocusedId);
        if (last != null) last.requestFocus();
        else if (!dockCards.isEmpty()) dockCards.get(0).requestFocus();
    }

    /** A 16:9 card with the app's banner; the name shows under it while focused. */
    private View tile(App app, boolean inDock) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setClipChildren(false);
        box.setClipToPadding(false);

        FrameLayout card = new AspectFrame(this);
        card.setId(View.generateViewId());
        card.setFocusable(true);
        card.setClickable(true);
        card.setTag(app);
        card.setContentDescription(app.label);
        card.setClipToOutline(true);
        card.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View v, Outline o) {
                o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), dp(10));
            }
        });
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(app.isBanner ? 0xFF1C1C1E : darken(app.color, 0.45f));
        card.setBackground(bg);

        ImageView img = new ImageView(this);
        img.setImageDrawable(app.art);
        if (app.isBanner) {
            img.setScaleType(ImageView.ScaleType.CENTER_CROP);
            card.addView(img, new FrameLayout.LayoutParams(-1, -1));
        } else {
            img.setScaleType(ImageView.ScaleType.FIT_CENTER);
            card.addView(img, new FrameLayout.LayoutParams(-1, -1));
            img.setPadding(dp(22), dp(14), dp(22), dp(14));
        }

        // Focus ring and sheen, drawn above the art.
        GradientDrawable ring = new GradientDrawable();
        ring.setCornerRadius(dp(10));
        ring.setStroke(dp(2), 0xE6FFFFFF);
        ring.setOrientation(GradientDrawable.Orientation.TL_BR);
        ring.setColors(new int[]{0x26FFFFFF, 0x00FFFFFF, 0x00FFFFFF});
        View shine = new View(this);
        shine.setBackground(ring);
        shine.setAlpha(0f);
        card.addView(shine, new FrameLayout.LayoutParams(-1, -1));

        box.addView(card, new LinearLayout.LayoutParams(-1, -2));

        TextView label = text(11, "sans-serif-medium", Color.WHITE);
        label.setText(app.label);
        label.setSingleLine(true);
        label.setGravity(Gravity.CENTER);
        label.setAlpha(0f);
        box.addView(label, llp(-1, dp(18), 0, dp(6), 0, 0));

        card.setOnClickListener(v -> launch(new Intent(Intent.ACTION_MAIN)
                .addCategory(app.category)
                .setComponent(app.component)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)));
        card.setOnFocusChangeListener((v, has) -> {
            float s = 1f;
            v.animate().scaleX(s).scaleY(s).translationZ(0)
                    .setInterpolator(has ? new DecelerateInterpolator() : new DecelerateInterpolator())
                    .setDuration(lite?0:120).start();
            shine.animate().alpha(has ? 1f : 0f).setDuration(lite?0:160).start();
            label.animate().alpha(has ? 1f : 0f) .translationY(0).setDuration(lite?0:200).start();
            if (!has) return;
            lastFocusedId = v.getId();
            if (inDock) {
                title.setText(app.label);
                title.animate().alpha(1f).setDuration(lite?0:200).start();
                animateGlow(app.color);
                fadeGlow(1f);
                scroll.smoothScrollTo(0, displayOptions.focused()?hero.getHeight():0);
            } else {
                fadeGlow(0.35f);
                title.animate().alpha(0f).setDuration(lite?0:200).start();
                scrollToRow(box);
            }
        });

        // Long-press OK: add to / remove from / reorder the dock, right from the remote.
        card.setOnLongClickListener(v -> { showAppMenu(v, app.pkg, inDock); return true; });
        cardByPkg.put(app.pkg, card);

        (inDock ? dockCards : gridCards).add(card);
        return box;
    }

    /** Explicit D-pad targets: one row per press, ends of rows and of the top bar don't wrap. */
    private void horizontalFocus(View view,View previous,View next){
        // Lists follow reading order; D-pad LEFT/RIGHT always follow physical screen direction.
        boolean rtl=getResources().getConfiguration().getLayoutDirection()==View.LAYOUT_DIRECTION_RTL;
        view.setNextFocusLeftId((rtl?next:previous).getId());
        view.setNextFocusRightId((rtl?previous:next).getId());
    }
    private void wireFocus() {
        int d = dockCards.size(), g = gridCards.size();
        for (int i = 0; i < pills.size(); i++) {
            View p = pills.get(i);
            horizontalFocus(p,i==0?p:pills.get(i-1),i==pills.size()-1?p:pills.get(i+1));
            p.setNextFocusUpId(p.getId());
            p.setNextFocusDownId(d > 0 ? dockCards.get(Math.min(d - 1, d / 2)).getId() : p.getId());
        }
        int topId = pills.get(0).getId();
        for (int i = 0; i < d; i++) {
            View c = dockCards.get(i);
            c.setNextFocusUpId(topId);
            horizontalFocus(c,i==0?c:dockCards.get(i-1),i==d-1?c:dockCards.get(i+1));
            c.setNextFocusDownId(g > 0 ? gridCards.get(Math.min(i, g - 1)).getId() : c.getId());
        }
        int lastRow = (g - 1) / cols;
        for (int j = 0; j < g; j++) {
            View c = gridCards.get(j);
            int r = j / cols, col = j % cols;
            c.setNextFocusUpId(r == 0
                    ? (d > 0 ? dockCards.get(Math.min(col, d - 1)).getId() : topId)
                    : gridCards.get(j - cols).getId());
            c.setNextFocusDownId(j + cols < g ? gridCards.get(j + cols).getId()
                    : r < lastRow ? gridCards.get(g - 1).getId() : c.getId());
            horizontalFocus(c,col==0?c:gridCards.get(j-1),col==cols-1||j==g-1?c:gridCards.get(j+1));
        }
    }

    private void scrollToRow(View tileBox) {
        View row = (View) tileBox.getParent();
        int y = grid.getTop() + row.getTop();
        int screenH = getResources().getDisplayMetrics().heightPixels;
        // First grid row: slide the dock band up to the top edge; later rows centre on screen.
        View band = (View) dockRow.getParent();
        int target = grid.indexOfChild(row) == 0
                ? hero.getTop() + band.getTop() + dockRow.getTop() - dp(22)
                : y - (int) (screenH * 0.42f);
        int max = Math.max(0, content.getHeight() - scroll.getHeight());
        scroll.smoothScrollTo(0, Math.max(0, Math.min(target, max)));
    }

    // ---------------------------------------------------------------- inputs

    // ---------------------------------------------------------------- dock editing

    private final Map<String, View> cardByPkg = new HashMap<>();
    static final int MIN_DOCK = Dock.MIN, MAX_DOCK = Dock.MAX;

    /** The user's dock if they edited it, else res/values/config.xml. */
    /** The launcher's settings file, shared with DockReceiver. */
    private android.content.SharedPreferences prefs() { return Dock.prefs(this); }

    private String[] loadDock() {
        String saved = prefs().getString("dock", null);
        return saved != null ? (saved.isEmpty() ? new String[0] : saved.split(","))
                             : getResources().getStringArray(R.array.dock_apps);
    }

    /** Only installed apps are kept, so the dock never holds a removed app's slot. */
    private void saveDock(List<String> dock, String focusPkg) {
        List<String> installed = new ArrayList<>();
        for (String p : dock) if (isInstalled(p)) installed.add(p);
        prefs().edit().putString("dock", android.text.TextUtils.join(",", installed)).apply();
        dockApps = installed.toArray(new String[0]);
        buildApps();
        View c = cardByPkg.get(focusPkg);
        if (c != null) c.post(c::requestFocus);
    }

    private void showAppMenu(View anchor, String pkg, boolean inDock) {
        List<String> dock = new ArrayList<>();
        // The dock as shown, including apps added automatically to reach MIN_DOCK.
        for (View c : dockCards) for (Map.Entry<String, View> e : cardByPkg.entrySet()) if (e.getValue() == c) dock.add(e.getKey());
        int i = dock.indexOf(pkg);
        List<String> labels = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        labels.add("إلغاء التثبيت (Uninstall)");
        actions.add(() -> uninstallApp(pkg));
        labels.add("إخفاء من الرئيسية");
        actions.add(() -> {java.util.Set<String> hidden=new java.util.HashSet<>(prefs().getStringSet("hidden",java.util.Collections.emptySet()));hidden.add(pkg);prefs().edit().putStringSet("hidden",hidden).apply();buildApps();});
        labels.add("معلومات التطبيق");
        actions.add(() -> launch(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + pkg))));
        labels.add("ترتيب المفضلة…");
        actions.add(() -> launch(new Intent(this, DockActivity.class)));
        if (inDock) {
            if (dock.size() > MIN_DOCK) {
                labels.add("إزالة من المفضلة");
                actions.add(() -> { dock.remove(pkg); saveDock(dock, dock.get(Math.max(0, i - 1))); });
            } else {
                labels.add("المفضلة لا تقل عن " + arabicDigits(String.valueOf(MIN_DOCK)));
                actions.add(() -> { });
            }
        } else if (dock.size() < MAX_DOCK) {
            labels.add("إضافة إلى المفضلة");
            actions.add(() -> { dock.add(pkg); saveDock(dock, pkg); });
        } else {
            labels.add("المفضلة ممتلئة (" + MAX_DOCK + ")");
            actions.add(() -> { });
        }
        labels.add("إعادة المفضلة الافتراضية");
        actions.add(() -> {
            prefs().edit().remove("dock").apply();
            dockApps = getResources().getStringArray(R.array.dock_apps);
            buildApps();
        });
        showMenu(anchor, labels, actions);
    }

    private void uninstallApp(String pkg) {
        Intent details = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + pkg));
        try {
            ApplicationInfo info = getPackageManager().getApplicationInfo(pkg, 0);
            if ((info.flags & ApplicationInfo.FLAG_SYSTEM) != 0
                    && (info.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0) {
                launch(details); // Android offers disable/manage for built-in apps.
                return;
            }
        } catch (PackageManager.NameNotFoundException e) { return; }
        // Opens Android's confirmation; never silently deletes an application.
        if (!launch(new Intent(Intent.ACTION_UNINSTALL_PACKAGE, Uri.parse("package:" + pkg)))) launch(details);
    }

    /** Small focusable popup in the same style as the Inputs menu. */
    private void showMenu(View anchor, List<String> labels, List<Runnable> actions) {
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(8), dp(8), dp(8), dp(8));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(16));
        bg.setColor(palette.panel);
        bg.setStroke(dp(1), 0x26FFFFFF);
        list.setBackground(bg);
        PopupWindow pw = new PopupWindow(list, dp(230), -2, true);
        List<TextView> rows = new ArrayList<>();
        for (int k = 0; k < labels.size(); k++) rows.add(inputRow(labels.get(k), actions.get(k)));
        for (TextView r : rows) {
            r.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
            r.setOnClickListener(v -> { pw.dismiss(); ((Runnable) v.getTag()).run(); });
            list.addView(r, new LinearLayout.LayoutParams(-1, dp(36)));
        }
        for (int k = 0; k < rows.size(); k++) {
            TextView r = rows.get(k);
            r.setNextFocusUpId(rows.get(Math.max(0, k - 1)).getId());
            r.setNextFocusDownId(rows.get(Math.min(rows.size() - 1, k + 1)).getId());
            r.setNextFocusLeftId(r.getId());
            r.setNextFocusRightId(r.getId());
        }
        pw.setOutsideTouchable(true);
        pw.setElevation(dp(16));
        pw.showAsDropDown(anchor, 0, -anchor.getHeight() - dp(8) * (rows.size() + 2) - dp(36) * rows.size(), Gravity.START);
        rows.get(0).requestFocus();
    }

    /** Native TV handoff only: never simulate remote keys or change input services/settings. */
    private boolean nativeIntent(Intent intent){try{if(intent.resolveActivity(getPackageManager())!=null){startActivity(intent);return true;}}catch(RuntimeException ignored){}return false;}
    private boolean nativePackage(String pkg){if(pkg==null||pkg.equals(getPackageName()))return false;Intent intent=getPackageManager().getLeanbackLaunchIntentForPackage(pkg);if(intent==null)intent=getPackageManager().getLaunchIntentForPackage(pkg);return intent!=null&&nativeIntent(intent);}
    private boolean nativeTvIntent(Intent intent){
        // A generic content viewer can claim TV URIs. Only explicit system TV handlers are eligible.
        for(android.content.pm.ResolveInfo result:getPackageManager().queryIntentActivities(intent,android.content.pm.PackageManager.GET_RESOLVED_FILTER)){
            if(result.activityInfo==null||(result.activityInfo.applicationInfo.flags&(android.content.pm.ApplicationInfo.FLAG_SYSTEM|android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP))==0)continue;
            String pkg=result.activityInfo.packageName;boolean trusted=pkg.equals(liveTvPkg)||pkg.equals("com.smartdevice.livetv")||pkg.equals("com.android.tv")||pkg.equals("com.google.android.tv");
            if(result.filter!=null)for(int n=0;n<result.filter.countDataAuthorities();n++)if("android.media.tv".equals(result.filter.getDataAuthority(n).getHost()))trusted=true;
            if(trusted&&nativeIntent(new Intent(intent).setComponent(new android.content.ComponentName(pkg,result.activityInfo.name))))return true;
        }return false;
    }
    private void openChannels(){
        if(nativeTvIntent(new Intent(Intent.ACTION_VIEW,TvContract.Channels.CONTENT_URI)))return;
        try{TvInputManager manager=(TvInputManager)getSystemService(TV_INPUT_SERVICE);if(manager!=null)for(TvInputInfo input:manager.getTvInputList())if(!input.isPassthroughInput()&&!input.isHidden(this)&&nativePackage(input.getServiceInfo().packageName))return;}catch(RuntimeException ignored){}
        for(String pkg:new String[]{liveTvPkg,"com.smartdevice.livetv","com.android.tv","com.google.android.tv","com.tcl.tv"})if(nativePackage(pkg))return;
        new android.app.AlertDialog.Builder(this).setTitle("القنوات").setMessage("لم يوفّر هذا الجهاز تطبيق قنوات يمكن فتحه. استخدم واجهة التلفزيون الأصلية أو إعداداته؛ لم تتغير أي خدمة أو قناة.").setPositiveButton("إعدادات التلفزيون",(d,n)->launch(new Intent(Settings.ACTION_SETTINGS))).setNegativeButton("رجوع",null).show();
    }

    private void showInputs(View anchor) {
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(8), dp(8), dp(8), dp(8));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(16));
        bg.setColor(palette.panel);
        bg.setStroke(dp(1), 0x26FFFFFF);
        list.setBackground(bg);

        PopupWindow pw = new PopupWindow(list, dp(200), -2, true);
        List<TextView> rows = new ArrayList<>();

        TvInputManager tim = (TvInputManager) getSystemService(TV_INPUT_SERVICE);
        List<TvInputInfo> inputs = new ArrayList<>();
        try{if (tim != null) for (TvInputInfo t : tim.getTvInputList()) {
            if (t.isPassthroughInput() && !t.isHidden(this)) inputs.add(t);
        }}catch(RuntimeException ignored){}
        Collections.sort(inputs, (x, y) -> inputLabel(x).compareToIgnoreCase(inputLabel(y)));
        for (TvInputInfo t : inputs) {
            rows.add(inputRow(inputLabel(t), () -> {
                Intent i = new Intent(Intent.ACTION_VIEW, TvContract.buildChannelUriForPassthroughInput(t.getId()));
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                if (!nativeTvIntent(i)&&!nativePackage(inputPickerPkg))android.widget.Toast.makeText(this,"تعذر فتح المدخل من واجهة الشركة؛ استخدم إعدادات التلفزيون",android.widget.Toast.LENGTH_LONG).show();
            }));
        }
        if (isInstalled(liveTvPkg)) rows.add(inputRow("البث التلفزيوني", () -> openPackage(liveTvPkg)));
        if (inputs.isEmpty() && isInstalled(inputPickerPkg)) rows.add(inputRow("كل المصادر…", () -> openPackage(inputPickerPkg)));

        if (rows.isEmpty()){TextView hint=text(13,"sans-serif",palette.muted);hint.setText("لا توجد مداخل متاحة من النظام");hint.setPadding(dp(14),dp(8),dp(14),dp(8));list.addView(hint);rows.add(inputRow("إعدادات التلفزيون", () -> launch(new Intent(Settings.ACTION_SETTINGS))));}
        for (TextView r : rows) {
            r.setOnClickListener(v -> { pw.dismiss(); ((Runnable) v.getTag()).run(); });
            list.addView(r, new LinearLayout.LayoutParams(-1, dp(36)));
        }
        for (int i = 0; i < rows.size(); i++) {
            TextView r = rows.get(i);
            r.setNextFocusUpId(rows.get(Math.max(0, i - 1)).getId());
            r.setNextFocusDownId(rows.get(Math.min(rows.size() - 1, i + 1)).getId());
            r.setNextFocusLeftId(r.getId());
            r.setNextFocusRightId(r.getId());
        }
        pw.setOutsideTouchable(true);
        pw.setElevation(dp(16));
        pw.setOnDismissListener(() -> { inputsPopup = null; anchor.requestFocus(); });
        inputsPopup = pw;
        pw.showAsDropDown(anchor, 0, dp(8), Gravity.START);
        rows.get(0).requestFocus();
    }

    private String inputLabel(TvInputInfo t) {
        CharSequence c = t.loadCustomLabel(this);
        if (c == null || c.length() == 0) c = t.loadLabel(this);
        return String.valueOf(c);
    }

    private TextView inputRow(String label, Runnable action) {
        TextView r = text(13, "sans-serif", Color.WHITE);
        r.setText(label);
        r.setTag(action);
        r.setId(View.generateViewId());
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(dp(14), 0, dp(14), 0);
        r.setFocusable(true);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(10));
        bg.setColor(Color.TRANSPARENT);
        r.setBackground(bg);
        r.setOnFocusChangeListener((v, has) -> {
            bg.setColor(has ? 0xF2FFFFFF : Color.TRANSPARENT);
            r.setTextColor(has ? 0xFF111111 : Color.WHITE);
        });
        return r;
    }

    // ---------------------------------------------------------------- status

    private void updateClock() {
        Calendar now = Calendar.getInstance(java.util.TimeZone.getTimeZone(displayOptions.clockZone(this).getId()));
        int h = now.get(Calendar.HOUR_OF_DAY);
        greeting.setText(h < 4 ? "تصبح على خير" : h < 12 ? "صباح الخير" : "مساء الخير");
        int hh = displayOptions.hour24 ? h : h % 12 == 0 ? 12 : h % 12;
        clock.setText(arabicDigits(hh + ":" + String.format(Locale.ROOT, "%02d", now.get(Calendar.MINUTE))));
        ampm.setText(displayOptions.hour24 ? "" : h < 12 ? "ص" : "م");
        String hijri = "";
        try {
            android.icu.text.SimpleDateFormat f = new android.icu.text.SimpleDateFormat(
                    "EEEE، d MMMM y", new android.icu.util.ULocale("ar_KW@calendar=islamic-umalqura"));
            f.setTimeZone(android.icu.util.TimeZone.getTimeZone(displayOptions.clockZone(this).getId()));
            hijri = f.format(now.getTime()).replaceAll("\\s*هـ\\s*$", "") + " هـ";
        } catch (Exception ignored) { }
        SimpleDateFormat gregFormat = new SimpleDateFormat("d MMMM", new Locale("ar", "KW"));
        gregFormat.setTimeZone(java.util.TimeZone.getTimeZone(displayOptions.clockZone(this).getId()));
        String greg = gregFormat.format(now.getTime());
        kw.urtv.shared.CalendarData.Day officialDate=kw.urtv.shared.Place.load(this).country.equals("KW")?calendarStore.get(java.time.LocalDate.now(displayOptions.clockZone(this))):null;
        if(officialDate!=null)hijri=officialDate.hijri+" هـ";
        date.setText(displayOptions.numbers(java.time.LocalDate.now(displayOptions.clockZone(this)).format(java.time.format.DateTimeFormatter.ofPattern("EEEE، d MMMM yyyy",new Locale("ar")))));
    }

    private String arabicDigits(String s) {
        if(displayOptions!=null)return displayOptions.numbers(s);
        StringBuilder b = new StringBuilder(s.length());
        for (char c : s.toCharArray()) b.append(c >= '0' && c <= '9' ? (char) ('٠' + (c - '0')) : c);
        return b.toString();
    }

    /** Connected or not comes from ConnectivityManager: WifiManager hides the network (id -1, "<unknown ssid>")
     *  from apps without location access, which looked like "not connected". The name only when readable. */
    private void updateWifi() {
        String name = "غير متصل";
        try {
            android.net.ConnectivityManager cm = getSystemService(android.net.ConnectivityManager.class);
            android.net.NetworkCapabilities nc = cm == null ? null : cm.getNetworkCapabilities(cm.getActiveNetwork());
            if (nc != null && nc.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET)) {
                name = "شبكة سلكية";
            } else if (nc != null && nc.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)) {
                name = "واي فاي";
                WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
                String s = wm == null || wm.getConnectionInfo() == null ? null : wm.getConnectionInfo().getSSID();
                if (s != null && !s.contains("unknown ssid") && !s.replace("\"", "").isEmpty()) name = s.replace("\"", "");
            }
        } catch (SecurityException ignored) { }
        wifiPill.setText(name);
    }

    // ---------------------------------------------------------------- glow

    private void setGlow(int color) {
        glowColor = color;
        GradientDrawable g = new GradientDrawable();
        g.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        g.setGradientCenter(0.5f, 0.78f);
        g.setGradientRadius(getResources().getDisplayMetrics().widthPixels * 0.62f);
        g.setColors(new int[]{withAlpha(color, 0.16f), withAlpha(color, 0.04f), Color.TRANSPARENT});
        glow.setBackground(g);
    }

    private void animateGlow(int to) {
        if(lite)return;
        if (glowAnim != null) glowAnim.cancel();
        if (glowColor != to) setGlow(to);
    }

    private void fadeGlow(float alpha) {
        glow.animate().alpha(alpha).setDuration(lite?0:300).start();
        // The grid sits over the photo's busiest middle part, so dim it while browsing apps.
        gridShade.animate().alpha(alpha < 1f ? 1f : 0f).setDuration(lite?0:300).start();
    }

    // ---------------------------------------------------------------- wallpaper

    private ImageView wallpaperLayer() {
        ImageView v = new ImageView(this);
        v.setScaleType(ImageView.ScaleType.CENTER_CROP);
        return v;
    }

    /** Decode the next photo off the main thread, then crossfade it in over the current one. */
    private void nextWallpaper() {
        handler.removeCallbacks(rotateWallpaper);
        if(displayOptions.focused()||!visible)return;
        wallChangedAt=SystemClock.elapsedRealtime();
        final kw.urtv.shared.Place place=kw.urtv.shared.Place.load(this);
        final String request=kw.urtv.shared.CityWallpaper.identity(place);
        if(wallFront.getDrawable()!=null&&request.equals(photoShownCity))return;
        final long generation=++photoGeneration;
        new Thread(()->{
            Bitmap bm=null;
            try{bm=kw.urtv.shared.CityWallpaper.decode(this,place,imageEdge);}catch(java.io.IOException ignored){}
            catch(OutOfMemoryError low){kw.urtv.shared.DisplayOptions.memoryPressure=true;lite=true;imageEdge=1280;}
            final Bitmap result=bm;
            runOnUiThread(()->{if(generation!=photoGeneration||!request.equals(kw.urtv.shared.CityWallpaper.identity(kw.urtv.shared.Place.load(this)))){if(result!=null)result.recycle();return;}if(result!=null)photoShownCity=request;showWallpaper(result);});
        },"city-wallpaper").start();
        scheduleWallpaper(300000);
    }
    private long photoGeneration;
    private String photoShownCity="";

    private void showWallpaper(Bitmap bm) {
        if (bm == null) return;
        if(isDestroyed()||!visible||displayOptions.focused()){bm.recycle();return;}
        // Swap roles: the old front becomes the back, the new photo fades in on top.
        ImageView old = wallFront;
        wallFront = wallBack;
        wallBack = old;
        ViewGroup root = (ViewGroup) wallFront.getParent();
        root.removeView(wallFront);
        root.addView(wallFront, 1, new FrameLayout.LayoutParams(-1, -1));
        wallFront.setImageBitmap(bm);
        if(lite){wallFront.setAlpha(1f);wallBack.setImageDrawable(null);firstWallpaper=false;return;}
        wallFront.setAlpha(0f);
        wallFront.animate().alpha(1f).setDuration(firstWallpaper ? 600 : 2000).withEndAction(() -> {
            wallBack.setImageDrawable(null); // release the previous photo
        }).start();
        firstWallpaper = false;
    }

    private boolean firstWallpaper = true;

    private void scheduleWallpaper(long delay) {
        handler.removeCallbacks(rotateWallpaper);
        if(!displayOptions.focused()&&visible)handler.postDelayed(rotateWallpaper,Math.max(1000,delay));
    }

    /** Average the banner's vivid pixels, then push saturation so the glow reads as a colour. */
    private int sampleColor(String pkg, Drawable d) {
        Integer cached = colorCache.get(pkg);
        if (cached != null) return cached;
        int color = 0xFF3A3A3A;
        try {
            Bitmap bm = Bitmap.createBitmap(24, 14, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bm);
            d.setBounds(0, 0, 24, 14);
            d.draw(c);
            float[] hsv = new float[3];
            double r = 0, g = 0, b = 0, w = 0;
            for (int y = 0; y < 14; y++) for (int x = 0; x < 24; x++) {
                int p = bm.getPixel(x, y);
                if (Color.alpha(p) < 128) continue;
                Color.colorToHSV(p, hsv);
                double weight = 0.05 + hsv[1] * hsv[2];
                r += Color.red(p) * weight; g += Color.green(p) * weight; b += Color.blue(p) * weight; w += weight;
            }
            bm.recycle();
            if (w > 0) {
                Color.colorToHSV(Color.rgb((int) (r / w), (int) (g / w), (int) (b / w)), hsv);
                if (hsv[1] > 0.12f) { hsv[1] = Math.max(hsv[1], 0.75f); hsv[2] = 0.85f; }
                else { hsv[1] = 0f; hsv[2] = 0.45f; }
                color = Color.HSVToColor(hsv);
            }
        } catch (RuntimeException ignored) { }
        colorCache.put(pkg, color);
        return color;
    }

    // ---------------------------------------------------------------- entrance

    private void playEntrance() {
        if(lite)return;
        View header = hero.getChildAt(0);
        header.setAlpha(0f);
        header.setTranslationY(-dp(12));
        header.animate().alpha(1f).translationY(0).setDuration(500).setStartDelay(80).start();
        title.setAlpha(0f);
        title.animate().alpha(1f).setDuration(500).setStartDelay(200).start();
        for (int i = 0; i < dockRow.getChildCount(); i++) {
            View t = dockRow.getChildAt(i);
            t.setAlpha(0f);
            t.setTranslationY(dp(24));
            t.animate().alpha(1f).translationY(0).setInterpolator(new DecelerateInterpolator(2f))
                    .setDuration(450).setStartDelay(160 + i * 60L).start();
        }
    }

    // ---------------------------------------------------------------- helpers

    private boolean launch(Intent i) {
        try {
            if (!i.hasCategory(Intent.CATEGORY_LEANBACK_LAUNCHER) && (i.getComponent()==null || !getPackageName().equals(i.getComponent().getPackageName()))) i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private boolean isInstalled(String pkg) {
        if (pkg == null || pkg.isEmpty()) return false;
        try {
            getPackageManager().getPackageInfo(pkg, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void openPackage(String pkg) {
        Intent i = getPackageManager().getLeanbackLaunchIntentForPackage(pkg);
        if (i == null) i = getPackageManager().getLaunchIntentForPackage(pkg);
        if (i != null) launch(i);
    }

    static final int GOLD = 0xFF8EDBEA;
    static final String SAVER_PKG = "kw.urtv.screensaver";
    private final Map<String, Typeface> fonts = new HashMap<>();

    /** Thmanyah from assets/fonts; any old family name maps to the nearest weight. */
    private Typeface font(String family) {
        String file = family.contains("display") ? "ThmanyahDisplay-Bold"
                : family.contains("thin") || family.contains("light") ? "Thmanyah-Light"
                : family.contains("medium") ? "Thmanyah-Medium" : "Thmanyah-Regular";
        Typeface tf = fonts.get(file);
        if (tf == null) {
            try { tf = Typeface.createFromAsset(getAssets(), "fonts/" + file + ".otf"); }
            catch (RuntimeException e) { tf = Typeface.create("sans-serif", Typeface.NORMAL); }
            fonts.put(file, tf);
        }
        return tf;
    }

    private TextView text(float sp, String family, int color) {
        TextView t = new TextView(this);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTypeface(font(family));
        t.setTextColor(palette.text(color));
        return t;
    }

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private static FrameLayout.LayoutParams lp(int w, int h, int gravity, int l, int t, int r, int b) {
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(w, h, gravity);
        p.setMargins(l, t, r, b);
        return p;
    }

    private static LinearLayout.LayoutParams llp(int w, int h, int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.setMargins(l, t, r, b);
        return p;
    }

    private static int withAlpha(int c, float a) {
        return Color.argb(Math.round(a * 255), Color.red(c), Color.green(c), Color.blue(c));
    }

    private static int darken(int c, float f) {
        return Color.rgb((int) (Color.red(c) * f), (int) (Color.green(c) * f), (int) (Color.blue(c) * f));
    }


    /** 16:9 container. */
    static final class AspectFrame extends FrameLayout {
        AspectFrame(Context c) { super(c); }
        @Override protected void onMeasure(int w, int h) {
            int width = MeasureSpec.getSize(w);
            super.onMeasure(w, MeasureSpec.makeMeasureSpec(width * 9 / 16, MeasureSpec.EXACTLY));
        }
    }

    /** ScrollView that never auto-scrolls to the focused child; the activity scrolls it by row. */
    static final class HomeScrollView extends android.widget.ScrollView {
        HomeScrollView(Context c) { super(c); }
        @Override protected int computeScrollDeltaToGetChildRectOnScreen(android.graphics.Rect r) { return 0; }
    }
}
