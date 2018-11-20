package cz.martykan.webtube;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class WebTubeWebViewClient extends WebViewClient {
    Context context;
    View.OnClickListener clickListener;
    View appWindow;
    View statusBarSpace;
    View bottomBar;

    public WebTubeWebViewClient(Context context, View appWindow, View.OnClickListener clickListener, View statusBarSpace, View bottomBar) {
        this.context = context;
        this.clickListener = clickListener;
        this.appWindow = appWindow;
        this.statusBarSpace = statusBarSpace;
        this.bottomBar = bottomBar;
    }

    // Open links in a browser window (except for sign-in dialogs and YouTube URLs)
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        if (url != null && url.startsWith("http") && !url.contains("accounts.google.") && !url.contains("youtube.")) {
            view.getContext().startActivity(
                    new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            return true;
        }
        return false;
    }

    public void onLoadResource(WebView view, String url) {
        if (!url.contains(".jpg") && !url.contains(".ico") && !url.contains(".css") && !url.contains(".js") && !url.contains("complete/search")) {
            // Remove all iframes (to prevent WebRTC exploits)
            view.loadUrl("javascript:(function() {" +
                    "var iframes = document.getElementsByTagName('iframe');" +
                    "for(i=0;i<=iframes.length;i++){" +
                    "if(typeof iframes[0] != 'undefined')" +
                    "iframes[0].outerHTML = '';" +
                    "}})()");

            // Gets rid of orange outlines
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {

                String css = "*, *:focus { " +
                        " outline: none !important; -webkit-tap-highlight-color: rgba(255,255,255,0) !important; -webkit-tap-highlight-color: transparent !important; }" +
                        " ._mfd { padding-top: 2px !important; } ";
                view.loadUrl("javascript:(function() {" +
                        "if(document.getElementById('webTubeStyle') == null){" +
                        "var parent = document.getElementsByTagName('head').item(0);" +
                        "var style = document.createElement('style');" +
                        "style.id = 'webTubeStyle';" +
                        "style.type = 'text/css';" +
                        "style.innerHTML = '" + css + "';" +
                        "parent.appendChild(style);" +
                        "}})()");
            }

            // To adapt the statusbar color
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
                statusBarSpace.setVisibility(View.VISIBLE);
                view.evaluateJavascript("(function() { if(document.getElementById('player').style.visibility == 'hidden' || document.getElementById('player').innerHTML == '') { return 'not_video'; } else { return 'video'; } })();",
                        value -> {
                            int colorId = value.contains("not_video") ? R.color.colorPrimary : R.color.colorWatch;
                            statusBarSpace.setBackgroundColor(ContextCompat.getColor(context, colorId));
                            bottomBar.setBackgroundColor(ContextCompat.getColor(context, colorId));
                        });
            }
        }
    }

    // Deal with error messages
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
        if (description.contains("NETWORK_CHANGED")) {
            view.loadUrl(PreferenceManager.getDefaultSharedPreferences(view.getContext()).getString("homepage", "https://m.youtube.com/"));
        } else if (description.contains("NAME_NOT_RESOLVED")) {
            Snackbar.make(appWindow, context.getString(R.string.errorNoInternet), Snackbar.LENGTH_INDEFINITE).setAction(context.getString(R.string.refresh), clickListener).show();
        } else if (description.contains("PROXY_CONNECTION_FAILED")) {
            Snackbar.make(appWindow, context.getString(R.string.errorTor), Snackbar.LENGTH_INDEFINITE).setAction(context.getString(R.string.refresh), clickListener).show();
        } else {
            Snackbar.make(appWindow, context.getString(R.string.error) + " " + description, Snackbar.LENGTH_INDEFINITE).
                    setAction(context.getString(R.string.refresh), clickListener).show();
        }
    }
}
