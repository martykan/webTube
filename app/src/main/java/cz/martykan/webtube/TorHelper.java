package cz.martykan.webtube;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.webkit.CookieManager;
import android.webkit.WebView;

import info.guardianproject.netcipher.proxy.OrbotHelper;
import info.guardianproject.netcipher.webkit.WebkitProxy;

public class TorHelper {
    public static final String PREF_TOR_ENABLED = "torEnabled";
    private static final int PORT_TOR = 8118;

    Context mApplicationContext;
    WebView webView;
    SharedPreferences sp;

    public TorHelper(Context mApplicationContext, WebView webView) {
        this.mApplicationContext = mApplicationContext;
        this.webView = webView;

        sp = PreferenceManager.getDefaultSharedPreferences(mApplicationContext);
    }

    public void setUpTor() {
        // Tor
        if (OrbotHelper.isOrbotInstalled(mApplicationContext)) {
            if (sp.getBoolean(PREF_TOR_ENABLED, false)) {
                torEnable();
            }
        }
    }

    public void torEnable() {
        CookieHelper.acceptCookies(webView, false);
        CookieHelper.deleteCookies();
        //Make sure that all cookies are really deleted
        if (!CookieManager.getInstance().hasCookies()) {
            if (!OrbotHelper.isOrbotRunning(mApplicationContext))
                OrbotHelper.requestStartTor(mApplicationContext);
            try {
                WebkitProxy.setProxy(MainActivity.class.getName(), mApplicationContext, null, "localhost", PORT_TOR);
                SharedPreferences.Editor spEdit = sp.edit();
                spEdit.putBoolean(PREF_TOR_ENABLED, true);
				spEdit.apply();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        webView.reload();
    }

    public void torDisable() {
        CookieHelper.deleteCookies();
        //Make sure that all cookies are really deleted
        if (!CookieManager.getInstance().hasCookies()) {
            try {
                WebkitProxy.resetProxy(MainActivity.class.getName(), mApplicationContext);
                SharedPreferences.Editor spEdit = sp.edit();
                spEdit.putBoolean(PREF_TOR_ENABLED, false);
				spEdit.apply();
                CookieHelper.acceptCookies(webView, true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Intent mStartActivity = new Intent(mApplicationContext, MainActivity.class);
        PendingIntent mPendingIntent = PendingIntent.getActivity(mApplicationContext, 12374, mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager mgr = (AlarmManager) mApplicationContext.getSystemService(Context.ALARM_SERVICE);
        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
        System.exit(0);
    }
}
