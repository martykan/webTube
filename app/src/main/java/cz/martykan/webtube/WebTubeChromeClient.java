package cz.martykan.webtube;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import static cz.martykan.webtube.MenuHelper.PREF_LANDSCAPE_FULLSCREEN;

public class WebTubeChromeClient extends WebChromeClient {

    Activity activity;
    View mCustomView;
    ProgressBar progress;
    WebView webView;
    FrameLayout customViewContainer;
    DrawerLayout drawerLayout;
    View decorView;
    SharedPreferences sp;

    public WebTubeChromeClient(Activity activity, WebView webView, ProgressBar progress, FrameLayout customViewContainer, DrawerLayout drawerLayout, View decorView) {
        this.activity = activity;
        this.webView = webView;
        this.progress = progress;
        this.customViewContainer = customViewContainer;
        this.drawerLayout = drawerLayout;
        this.decorView = decorView;

        sp = PreferenceManager.getDefaultSharedPreferences(activity);
    }

    // Fullscreen playback
    @Override
    public void onShowCustomView(View view, CustomViewCallback callback) {
        if (mCustomView != null) {
            callback.onCustomViewHidden();
            return;
        }
        mCustomView = view;
        webView.loadUrl("javascript:(function() { document.body.style.overflowX = 'hidden'; })();");
        webView.loadUrl("javascript:(function() { window.scrollTo(0, 0); })();");
        drawerLayout.setVisibility(View.GONE);
        customViewContainer.setVisibility(View.VISIBLE);
        customViewContainer.addView(view);

        // Hide the status bar.
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN);
        }

        if (sp.getBoolean(PREF_LANDSCAPE_FULLSCREEN, false)) {
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
    }

    @Override
    public void onHideCustomView() {
        super.onHideCustomView();
        if (mCustomView == null)
            return;

        webView.loadUrl("javascript:(function() { window.scrollTo(0, 0); })();");
        webView.loadUrl("javascript:(function() { document.body.style.overflowX = 'scroll'; })();");
        drawerLayout.setVisibility(View.VISIBLE);
        customViewContainer.setVisibility(View.GONE);

        mCustomView.setVisibility(View.GONE);
        customViewContainer.removeView(mCustomView);
        mCustomView = null;

        // Show the status bar.
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);

        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
    }

    // Progressbar
    public void onProgressChanged(WebView view, int percentage) {
        progress.setVisibility(View.VISIBLE);
        progress.setProgress(percentage);

        // For more advnaced loading status
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            progress.setIndeterminate(percentage == 100);
            view.evaluateJavascript("(function() { return document.readyState == \"complete\"; })();",
                    new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String value) {
                            if (value.equals("true")) {
                                progress.setVisibility(View.INVISIBLE);
                            } else {
                                onProgressChanged(webView, 100);
                            }
                        }
                    });
        } else {
            if (percentage == 100) {
                progress.setVisibility(View.GONE);
            }
        }
    }


}