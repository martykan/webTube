package cz.martykan.webtube;

import android.os.Build;
import android.support.v4.widget.DrawerLayout;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

public class WebTubeChromeClient extends WebChromeClient {

    View mCustomView;
    ProgressBar progress;
    WebView webView;
    FrameLayout customViewContainer;
    DrawerLayout drawerLayout;
    View decorView;

    public WebTubeChromeClient(WebView webView, ProgressBar progress, FrameLayout customViewContainer, DrawerLayout drawerLayout, View decorView) {
        this.webView = webView;
        this.progress = progress;
        this.customViewContainer = customViewContainer;
        this.drawerLayout = drawerLayout;
        this.decorView = decorView;
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

        // Hide the status bar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN);
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
    }

    // Progressbar
    public void onProgressChanged(WebView view, int percentage) {
        progress.setVisibility(View.VISIBLE);
        progress.setProgress(percentage);

        // For more advanced loading status
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            progress.setIndeterminate(percentage == 100);
            view.evaluateJavascript("(function() { return document.readyState == \"complete\"; })();",
                    value -> {
                        if (value.equals("true")) {
                            progress.setVisibility(View.INVISIBLE);
                        } else {
                            onProgressChanged(webView, 100);
                        }
                    });
        } else {
            if (percentage == 100) {
                progress.setVisibility(View.GONE);
            }
        }
    }
}
