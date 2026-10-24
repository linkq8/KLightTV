package kw.urtv.home;
import android.accessibilityservice.AccessibilityService;import android.content.*;import android.view.KeyEvent;import android.view.accessibility.AccessibilityEvent;
/** Explicit mappings only. No logcat observer, timer, polling, or screen-content access. */
public final class RemoteButtonService extends AccessibilityService {
 private static RemoteButtonService instance;
 @Override protected void onServiceConnected(){instance=this;}
 static void disable(Context c){if(instance!=null)instance.disableSelf();}
 @Override protected boolean onKeyEvent(KeyEvent e){if(RemoteSettingsActivity.editing||!RemoteSettingsActivity.allowed(e.getKeyCode()))return false;String pkg=RemoteSettingsActivity.prefs(this).getString("key_"+e.getKeyCode(),null);if(pkg==null)return false;Intent launch=getPackageManager().getLeanbackLaunchIntentForPackage(pkg);if(launch==null)launch=getPackageManager().getLaunchIntentForPackage(pkg);if(launch==null)return false;if(e.getAction()==KeyEvent.ACTION_DOWN&&e.getRepeatCount()==0){try{startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}catch(RuntimeException ex){return false;}}return true;}
 @Override public void onDestroy(){instance=null;super.onDestroy();}
 @Override public void onAccessibilityEvent(AccessibilityEvent e){} @Override public void onInterrupt(){}
}
