package cz.martykan.webtube;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import com.getbase.floatingactionbutton.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    WebView webView;
    View appWindow;
    Window window;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = (WebView) findViewById(R.id.webView);
        appWindow = findViewById(R.id.appWindow);
        window = this.getWindow();

        // To save login info
        CookieManager.getInstance().setAcceptCookie(true);
        if (Integer.valueOf(Build.VERSION.SDK_INT) >= 21) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        }

        webView.setWebViewClient(new WebViewClient() {
            // Open links in a browser window (except for sign-in dialogs and YouTube URLs)
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url != null && url.startsWith("http") && !url.contains("accounts.google.") && !url.contains("youtube.")) {
                    view.getContext().startActivity(
                            new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                    return true;
                } else {
                    return false;
                }
            }

            // Block tracking URLs
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                if (url.toString().contains("csi") || url.toString().contains("ptracking") || url.toString().contains("doubleclick")) {
                    return new WebResourceResponse("text/plain", "UTF-8", new InputStream() {
                        @Override
                        public int read() throws IOException {
                            return 0;
                        }
                    });
                }
                return null;
            }

            public void onLoadResource(WebView view, String url) {
                // Gets rid of orange outlines
                String css = "*, *:focus { /*overflow-x: hidden !important;*/ " +
                        "/*transform: translate3d(0,0,0) !important; -webkit-transform: translate3d(0,0,0) !important;*/ outline: none !important; -webkit-tap-highlight-color: rgba(255,255,255,0) !important; -webkit-tap-highlight-color: transparent !important; }";
                webView.loadUrl("javascript:(function() {" +
                        "var parent = document.getElementsByTagName('head').item(0);" +
                        "var style = document.createElement('style');" +
                        "style.type = 'text/css';" +
                        "style.innerHTML = '" + css + "';" +
                        "parent.appendChild(style)" +
                        "})()");

                // To change the statusbar color
                if (Integer.valueOf(Build.VERSION.SDK_INT) >= 21) {
                    webView.evaluateJavascript("(function() { if(document.getElementById('player').style.visibility == 'hidden' || document.getElementById('player').innerHTML == '') { return 'not_video'; } else { return 'video'; } })();",
                            new ValueCallback<String>() {
                                @Override
                                public void onReceiveValue(String value) {
                                    window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                                    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                                    if (!value.toString().contains("not_video")) {
                                        window.setStatusBarColor(getApplication().getResources().getColor(R.color.colorWatchDark));
                                    } else {
                                        window.setStatusBarColor(getApplication().getResources().getColor(R.color.colorPrimaryDark));
                                    }
                                }
                            });
                }
            }

            // Deal with error messages
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                if (description.toString().contains("NETWORK_CHANGED")) {
                    webView.loadUrl("https://m.youtube.com/");
                } else if (description.toString().contains("NAME_NOT_RESOLVED")) {
                    Snackbar.make(appWindow, "Oh no! You are not connected to the internet.", Snackbar.LENGTH_INDEFINITE).setAction("Reload", clickListener).show();
                } else {
                    Snackbar.make(appWindow, "Oh no! " + description, Snackbar.LENGTH_INDEFINITE).setAction("Reload", clickListener).show();
                }
            }
        });

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        webSettings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        webView.setHorizontalScrollBarEnabled(false);
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        webView.setDrawingCacheBackgroundColor(Color.WHITE);
        webView.setDrawingCacheEnabled(false);
        webView.setWillNotCacheDrawing(true);
        if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1){
            webView.setAnimationCacheEnabled(false);
            webView.setAlwaysDrawnWithCacheEnabled(false);
        }
        webView.setBackgroundColor(Color.WHITE);
        webView.setScrollbarFadingEnabled(true);
        webView.setNetworkAvailable(true);
        webView.loadUrl("https://m.youtube.com/");

        // Floating action buttons
        FloatingActionButton fabBrowser = (FloatingActionButton) findViewById(R.id.fab_browser);
        fabBrowser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(webView.getUrl())));
            }
        });

        FloatingActionButton fabRefresh = (FloatingActionButton) findViewById(R.id.fab_refresh);
        fabRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                webView.reload();
            }
        });

        FloatingActionButton fabHome = (FloatingActionButton) findViewById(R.id.fab_home);
        fabHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                webView.loadUrl("https://m.youtube.com/");
            }
        });
    }

    // For the snackbar with error message
    View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            webView.loadUrl("https://m.youtube.com/");
        }
    };

    // For easier navigation
    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            finish();
        }

    }
}
