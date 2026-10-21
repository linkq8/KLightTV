package kw.urtv.screensaver;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;

/** Full-screen preview of the screensaver; any remote key closes it. */
public class MainActivity extends Activity {
    private ScreenView screen;
    @Override public void onTrimMemory(int level){super.onTrimMemory(level);if(kw.urtv.shared.HardwarePolicy.shouldTrim(level)&&screen!=null)screen.memoryPressure();}

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        screen = ScreenView.create(this);
        setContentView(screen);
        screen.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        if (e.getAction() == KeyEvent.ACTION_UP) finish();
        return true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (screen != null) screen.destroy();
        finish(); // A preview must not retain a photo or keep network work running behind Home.
    }

    @Override
    protected void onDestroy() {
        if (screen != null) screen.destroy();
        super.onDestroy();
    }
}
