package kw.urtv.home;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Locale;

/** Xiaomi's remote key layout maps its Netflix key to BUTTON_6. */
public final class RemoteButtonService extends AccessibilityService {
    private final Handler main = new Handler(Looper.getMainLooper());
    private volatile boolean observing;
    private volatile Process logcat;
    private Thread observer;
    private long lastLaunch;

    @Override protected void onServiceConnected() {
        if (observing) return;
        observing = true;
        observer = new Thread(() -> {
            while (observing) {
                try {
                    // Xiaomi consumes this key before accessibility. Its system intent is the fallback.
                    // Filter inside logcat: no screen content, log storage or other app messages.
                    String since = String.format(Locale.ROOT, "%.3f", System.currentTimeMillis() / 1000.0);
                    Process process = new ProcessBuilder("/system/bin/logcat", "-b", "system",
                            "-v", "raw", "-T", since, "-e",
                            "act=com[.]netflix[.]action[.]NETFLIX_KEY_START.*pkg=com[.]netflix[.]ninja",
                            "ActivityTaskManager:I", "*:S").redirectErrorStream(true).start();
                    logcat = process;
                    if (!observing) { process.destroy(); break; }
                    try (BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                        String line;
                        while (observing && (line = input.readLine()) != null) {
                            if (line.contains("act=com.netflix.action.NETFLIX_KEY_START")
                                    && line.contains("pkg=com.netflix.ninja")) {
                                main.post(() -> { if (observing) launchTod(); });
                            }
                        }
                    } finally { process.destroy(); logcat = null; }
                } catch (java.io.IOException e) {
                    if (observing) Log.w("KuwaitRemote", "Button observer unavailable", e);
                }
                if (observing) {
                    try { Thread.sleep(5000); } catch (InterruptedException e) { break; }
                }
            }
        }, "NetflixButton");
        observer.start();
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        if (event.getKeyCode() != KeyEvent.KEYCODE_BUTTON_6) return false;
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
            launchTod();
        }
        return true;
    }

    private void launchTod() {
            long now = SystemClock.elapsedRealtime();
            if (now - lastLaunch < 1000) return;
            Intent launch = getPackageManager().getLeanbackLaunchIntentForPackage("com.todtv.tod");
            if (launch == null) launch = getPackageManager().getLaunchIntentForPackage("com.todtv.tod");
            if (launch == null) return;
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            try {
                startActivity(launch);
                lastLaunch = now;
                Log.i("KuwaitRemote", "Netflix button opened TOD");
            } catch (RuntimeException e) {
                Log.w("KuwaitRemote", "TOD could not be opened", e);
            }
    }

    @Override public void onDestroy() {
        observing = false;
        Process process = logcat;
        if (process != null) process.destroy();
        if (observer != null) observer.interrupt();
        main.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) { }
    @Override public void onInterrupt() { }
}
