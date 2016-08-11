package daylemk.xposed.xbridge.action;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;

import daylemk.xposed.xbridge.R;
import daylemk.xposed.xbridge.hook.Hook;
import daylemk.xposed.xbridge.utils.Log;

/**
 * @author DayLemK
 * @version 1.0
 *          28-四月-2015 9:16:48
 */
public class PlayAction extends Action {
    public static final String TAG = "PlayAction";
    public static final String STR_DESC = "View in Play Store";
    public static final String PKG_NAME = "com.android.vending";
    public static final String URI_BROWSER = "http://play.google.com/store/apps/details?id=";
    public static final String URI_MARKET = "market://details?id=";

    /* the key should the sub class overwrite ------------begin */
    public static String keyShowInStatusBar;
    public static String keyShowInRecentTask;
    public static String keyShowInAppInfo;
    public static String keyShow;
    // add for market - browser switch
    public static String keyIsMarket;

    public static boolean showInStatusBarDefault = true;
    public static boolean showInRecentTaskDefault = true;
    public static boolean showInAppInfoDefault = true;
    public static boolean showDefault = true;
    public static boolean isMarketDefault = false;

    public static boolean isShowInRecentTask = true;
    public static boolean isShowInStatusBar = true;
    public static boolean isShowInAppInfo = true;
    public static boolean isShow = true;
    public static boolean isMarket = false;
    /* the key should the sub class overwrite ------------end */
    // just need to init the icon and listener once
    // EDIT: maybe the icon is already one instance in the system
    public static Drawable sIcon = null;
    //public static View.OnClickListener sOnClickListener = null;
    // EDIT: the on click listener should be different

    /**
     * load the key from the string resource
     *
     * @param sModRes the module resource of package
     */
    public static void loadPreferenceKeys(Resources sModRes) {
        keyShow = sModRes.getString(R.string.key_play);
        keyShowInAppInfo = sModRes.getString(R.string.key_play_app_info);
        keyShowInRecentTask = sModRes.getString(R.string.key_play_recent_task);
        keyShowInStatusBar = sModRes.getString(R.string.key_play_status_bar);
        keyIsMarket = sModRes.getString(R.string.key_play_is_market);
        // get the default value of this action
        showInStatusBarDefault = sModRes.getBoolean(R.bool.play_status_bar_default);
        showInRecentTaskDefault = sModRes.getBoolean(R.bool.play_recent_task_default);
        showInAppInfoDefault = sModRes.getBoolean(R.bool.play_app_info_default);
        showDefault = sModRes.getBoolean(R.bool.play_default);
        isMarketDefault = sModRes.getBoolean(R.bool.play_is_market_default);
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
        isMarket = preferences.getBoolean(keyIsMarket, isMarketDefault);
        Log.d(TAG, "load preference: " + "isShowInStatusBar:" + isShowInStatusBar +
                ",isShowInRecentTask:" + isShowInRecentTask + ",isShowInAppInfo:" + isShowInAppInfo
                + ",isShow:" + isShow + ",isMarket:" + isMarket);
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
        } else if (key.equals(keyIsMarket)) {
            isMarket = Boolean.valueOf(value);
        } else {
            // if not found it, return false
            result = false;
        }
        return result;
    }

    @Override
    public Drawable getIcon(PackageManager packageManager) {
        // check the icon. if good, just return.
        // TODO: check if the app is just install or upgrade and the icon should be changed
        if (sIcon == null) {
//        Drawable pkgIcon = null;
            sIcon = getPackageIcon(packageManager, PKG_NAME);
        } else {
            Log.d(TAG, "icon is ok, no need to create again");
        }
        return sIcon;
    }

    @Override
    public String getMenuTitle() {
        return STR_DESC;
    }

    @Override
    protected Intent getIntent(Hook hook, Context context, String pkgName) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse((isMarket ? URI_MARKET : URI_BROWSER) + pkgName));
        return intent;
    }

    @Override
    protected Intent getIntent(Hook hook, Context context, String pkgName, Intent originalIntent) {
        return null;
    }

    @Override
    public void handleData(Context context, String pkgName) {
    }
}