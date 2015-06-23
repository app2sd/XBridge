package daylemk.xposed.xbridge.action;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.widget.Toast;

import daylemk.xposed.xbridge.R;
import daylemk.xposed.xbridge.hook.FrameworksHook;
import daylemk.xposed.xbridge.hook.Hook;
import daylemk.xposed.xbridge.utils.Log;

/**
 * @author DayLemK
 * @version 1.0
 *          28-四月-2015 9:16:48
 */
public class ClipBoardAction extends Action {
    public static final String TAG = "ClipBoardAction";
    public static final String STR_DESC = "Copy to clipboard";

    public static final String ARG_PACKAGE_NAME = "package";

    /* the key should the sub class overwrite ------------begin */
    public static String keyShowInStatusBar;
    public static String keyShowInRecentTask;
    public static String keyShowInAppInfo;
    public static String keyShow;

    public static boolean showInStatusBarDefault = true;
    public static boolean showInRecentTaskDefault = true;
    public static boolean showInAppInfoDefault = true;
    public static boolean showDefault = true;

    public static boolean isShowInRecentTask = true;
    public static boolean isShowInStatusBar = true;
    public static boolean isShowInAppInfo = true;
    public static boolean isShow = true;
    /* the key should the sub class overwrite ------------end */

    /**
     * load the key from the string resource
     *
     * @param sModRes the module resource of package
     */
    public static void loadPreferenceKeys(Resources sModRes) {
        keyShow = sModRes.getString(R.string.key_clipboard);
        keyShowInAppInfo = sModRes.getString(R.string.key_clipboard_app_info);
        keyShowInRecentTask = sModRes.getString(R.string.key_clipboard_recent_task);
        keyShowInStatusBar = sModRes.getString(R.string.key_clipboard_status_bar);
        // get the default value of this action
        showInStatusBarDefault = sModRes.getBoolean(R.bool.clipboard_status_bar_default);
        showInRecentTaskDefault = sModRes.getBoolean(R.bool.clipboard_recent_task_default);
        showInAppInfoDefault = sModRes.getBoolean(R.bool.clipboard_app_info_default);
        showDefault = sModRes.getBoolean(R.bool.clipboard_default);
    }

    public static void loadPreference(SharedPreferences preferences) {
        isShowInStatusBar = preferences.getBoolean(keyShowInStatusBar,
                showInStatusBarDefault);
        isShowInRecentTask = preferences.getBoolean(keyShowInRecentTask,
                showInRecentTaskDefault);
        isShowInAppInfo = preferences.getBoolean(keyShowInAppInfo,
                showInAppInfoDefault);
        isShow = preferences.getBoolean(keyShow,
                showDefault);
        Log.d(TAG, "load preference: " + "isShowInStatusBar:" + isShowInStatusBar +
                "isShowInRecentTask:" + isShowInRecentTask + "isShowInAppInfo:" + isShowInAppInfo
                + "isShow:" + isShow);
    }

    public static boolean onReceiveNewValue(String key, String value) {
        boolean result = true;
        if (key.equals(keyShow)) {
            isShow = Boolean.valueOf(value);
        } else if (key.equals(keyShowInAppInfo)) {
            isShowInAppInfo = Boolean.valueOf(value);
        } else if (key.equals(keyShowInRecentTask)) {
            isShowInRecentTask = Boolean.valueOf(value);
        } else if (key.equals(keyShowInStatusBar)) {
            isShowInStatusBar = Boolean.valueOf(value);
        } else {
            // if not found it, return false
            result = false;
        }
        return result;
    }

    @Override
    protected Intent getIntent(Hook hook, Context context, String pkgName) {
        return null;
    }

    @Override
    protected Intent getIntent(Hook hook, Context context, String pkgName, Intent originalIntent) {
        return null;
    }

    /**
     * init the icon from UI
     *
     * @param res resource
     */
    public Action initIcon(Resources res) {
        FrameworksHook.getDefaultIcons(res);
        return this;
    }

    /**
     * need to call initIcon before this method.
     *
     * @param packageManager package manager
     * @return the icon
     */
    @Override
    public Drawable getIcon(PackageManager packageManager) {
        return FrameworksHook.getIconCopy();
    }

    @Override
    public String getMenuTitle() {
        return STR_DESC;
    }

    @Override
    public void handleData(final Context context, final String pkgName) {
        Log.d(TAG, "handle in the ClipBoard");
        // need run in the main thread
        new Handler(context.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                ClipboardManager clipboardManager = (ClipboardManager) context.getSystemService
                        (Context
                                .CLIPBOARD_SERVICE);
                ClipData clipData = ClipData.newPlainText(ARG_PACKAGE_NAME, pkgName);
                clipboardManager.setPrimaryClip(clipData);
                Context xBridgeContext = Hook.getXBridgeContext(context);

                String str = xBridgeContext.getString(R.string.package_name_copied);
                Toast.makeText(context, str + "\n" + pkgName, Toast.LENGTH_LONG).show();
            }
        });
    }
}