package daylemk.xposed.xbridge.action;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.XModuleResources;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import daylemk.xposed.xbridge.R;
import daylemk.xposed.xbridge.data.MainPreferences;
import daylemk.xposed.xbridge.hook.FrameworksHook;
import daylemk.xposed.xbridge.hook.Hook;
import daylemk.xposed.xbridge.utils.Log;
import de.robv.android.xposed.XSharedPreferences;

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

    public static final boolean PREF_SHOW_IN_STATUS_BAR_DEFAULT = true;
    public static final boolean PREF_SHOW_IN_RECENT_TASK_DEFAULT = true;
    public static final boolean PREF_SHOW_IN_APP_INFO_DEFAULT = true;
    public static final boolean PREF_SHOW = true;

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
    }

    public static void loadPreference(SharedPreferences preferences) {
        isShowInRecentTask = preferences.getBoolean(keyShowInRecentTask,
                PREF_SHOW_IN_RECENT_TASK_DEFAULT);
        isShowInStatusBar = preferences.getBoolean(keyShowInStatusBar,
                PREF_SHOW_IN_STATUS_BAR_DEFAULT);
        isShowInAppInfo = preferences.getBoolean(keyShowInAppInfo,
                PREF_SHOW_IN_APP_INFO_DEFAULT);
        isShow = preferences.getBoolean(keyShow,
                PREF_SHOW);
    }

    @Override
    protected Intent getIntent(Hook hook, String pkgName) {
        return null;
    }

    @Override
    public Drawable getIcon(PackageManager packageManager) {
        return FrameworksHook.getIconCopy();
    }

    @Override
    public String getMenuTitle() {
        return STR_DESC;
    }

    @Override
    public void handleData(Context context, String pkgName) {
        Log.d(TAG, "handle in the ClipBoard");
        ClipboardManager clipboardManager = (ClipboardManager) context.getSystemService(Context
                .CLIPBOARD_SERVICE);
        ClipData clipData = ClipData.newPlainText(ARG_PACKAGE_NAME, pkgName);
        clipboardManager.setPrimaryClip(clipData);
        Context xBridgeContext = Hook.getXBridgeContext(context);

        String strPkgName = xBridgeContext.getString(R.string.package_name);
        String strCopied = xBridgeContext.getString(R.string.copied);

        Toast.makeText(context, strPkgName + strCopied + "\n" + pkgName, Toast.LENGTH_LONG).show();
    }

}