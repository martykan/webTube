package cz.martykan.webtube;

import android.os.Build;
import android.webkit.CookieManager;
import android.webkit.WebView;

public class CookieHelper {
    public static void acceptCookies(WebView webView, boolean accept) {
        CookieManager.getInstance().setAcceptCookie(accept);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, accept);
        }
    }

    public static void deleteCookies() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().removeAllCookies(null);
        }
        CookieManager.getInstance().removeAllCookie();
    }
}
