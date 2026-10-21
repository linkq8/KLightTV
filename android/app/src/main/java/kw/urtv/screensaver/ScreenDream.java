package kw.urtv.screensaver;

import android.service.dreams.DreamService;

/** The system screensaver: starts on the TV's own idle timer, any key wakes it. */
public class ScreenDream extends DreamService {
    private ScreenView screen;
    @Override public void onTrimMemory(int level){super.onTrimMemory(level);if(kw.urtv.shared.HardwarePolicy.shouldTrim(level)&&screen!=null)screen.memoryPressure();}

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false);
        setFullscreen(true);
        setScreenBright(true);
        screen = ScreenView.create(this);
        setContentView(screen);
    }

    @Override
    public void onDreamingStopped() {
        if (screen != null) screen.destroy();
        super.onDreamingStopped();
    }

    @Override
    public void onDetachedFromWindow() {
        if (screen != null) { screen.destroy(); screen = null; }
        super.onDetachedFromWindow();
    }
}
