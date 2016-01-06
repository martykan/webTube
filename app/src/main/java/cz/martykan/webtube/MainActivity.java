package cz.martykan.webtube;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import com.getbase.floatingactionbutton.FloatingActionButton;

import info.guardianproject.netcipher.proxy.OrbotHelper;
import info.guardianproject.netcipher.web.WebkitProxy;


public class MainActivity extends AppCompatActivity {

    WebView webView;
    View appWindow;
    Window window;
    ProgressBar progress;
    View mCustomView;
    FrameLayout customViewContainer;
    FloatingActionButton fabTor;

    public static String LOG_TAG = "webTube";

    // For the snackbar with error message
    View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            webView.loadUrl("https://m.youtube.com/");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Gotta go fast!
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = (WebView) findViewById(R.id.webView);
        appWindow = findViewById(R.id.appWindow);
        window = this.getWindow();
        progress = (ProgressBar) findViewById(R.id.progress);
        customViewContainer = (FrameLayout) findViewById(R.id.customViewContainer);

        // To save login info
        CookieManager.getInstance().setAcceptCookie(true);
        if (Integer.valueOf(Build.VERSION.SDK_INT) >= 21) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        }

        webView.setWebChromeClient(new WebChromeClient() {

            // Fullscreen playback
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (mCustomView != null) {
                    callback.onCustomViewHidden();
                    return;
                }
                mCustomView = view;
                webView.setVisibility(View.GONE);
                customViewContainer.setVisibility(View.VISIBLE);
                customViewContainer.addView(view);

                View decorView = getWindow().getDecorView();
                // Hide the status bar.
                int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
                decorView.setSystemUiVisibility(uiOptions);

            }

            @Override
            public void onHideCustomView() {
                super.onHideCustomView();
                if (mCustomView == null)
                    return;

                webView.setVisibility(View.VISIBLE);
                customViewContainer.setVisibility(View.GONE);

                mCustomView.setVisibility(View.GONE);
                customViewContainer.removeView(mCustomView);
                mCustomView = null;

                View decorView = getWindow().getDecorView();
                // Show the status bar.
                int uiOptions = View.SYSTEM_UI_FLAG_VISIBLE;
                decorView.setSystemUiVisibility(uiOptions);
            }

            // Progressbar
            public void onProgressChanged(WebView view, int percentage) {
                progress.setVisibility(View.VISIBLE);
                progress.setProgress(percentage);

                // For more advnaced loading status
                if (Integer.valueOf(Build.VERSION.SDK_INT) >= 19) {
                    if (percentage == 100) {
                        progress.setIndeterminate(true);
                    } else {
                        progress.setIndeterminate(false);
                    }
                    webView.evaluateJavascript("(function() { return document.getElementsByClassName('_mks')[0] != null; })();",
                            new ValueCallback<String>() {
                                @Override
                                public void onReceiveValue(String value) {
                                    if (value.equals("false")) {
                                        progress.setVisibility(View.INVISIBLE);
                                    } else {
                                        onProgressChanged(webView, 100);
                                    }
                                }
                            });
                }
            }


        });

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

            public void onLoadResource(WebView view, String url) {
                if(!url.contains(".jpg")) {
                    // Remove all iframes (to prevent WebRTC exploits)
                    webView.loadUrl("javascript:(function() {" +
                            "var iframes = document.getElementsByTagName('iframe');" +
                            "for(i=0;i<=iframes.length;i++){" +
                            "if(typeof iframes[0] != 'undefined')" +
                            "iframes[0].outerHTML = '';" +
                            "}})()");

                    // Gets rid of orange outlines
                    if (Integer.valueOf(Build.VERSION.SDK_INT) >= 19) {

                        String css = "*, *:focus { " +
                                " outline: none !important; -webkit-tap-highlight-color: rgba(255,255,255,0) !important; -webkit-tap-highlight-color: transparent !important; }" +
                                " ._mfd { padding-top: 2px !important; } ";
                        webView.loadUrl("javascript:(function() {" +
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
                    if (Integer.valueOf(Build.VERSION.SDK_INT) >= 21) {
                        final View statusBarSpace = findViewById(R.id.statusBarSpace);
                        statusBarSpace.setVisibility(View.VISIBLE);
                        webView.evaluateJavascript("(function() { if(document.getElementById('player').style.visibility == 'hidden' || document.getElementById('player').innerHTML == '') { return 'not_video'; } else { return 'video'; } })();",
                                new ValueCallback<String>() {
                                    @Override
                                    public void onReceiveValue(String value) {
                                        if (!value.toString().contains("not_video")) {
                                            statusBarSpace.setBackgroundColor(getApplication().getResources().getColor(R.color.colorWatch));
                                        } else {
                                            statusBarSpace.setBackgroundColor(getApplication().getResources().getColor(R.color.colorPrimary));
                                        }
                                    }
                                });
                    }
                }
            }

            // Deal with error messages
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                if (description.toString().contains("NETWORK_CHANGED")) {
                    webView.loadUrl("https://m.youtube.com/");
                } else if (description.toString().contains("NAME_NOT_RESOLVED")) {
                    Snackbar.make(appWindow, "Oh no! You are not connected to the internet.", Snackbar.LENGTH_INDEFINITE).setAction("Reload", clickListener).show();
                } else if (description.toString().contains("PROXY_CONNECTION_FAILED")) {
                    Snackbar.make(appWindow, "Oh no! Tor is not working properly.", Snackbar.LENGTH_INDEFINITE).setAction("Reload", clickListener).show();
                } else {
                    Snackbar.make(appWindow, "Oh no! " + description, Snackbar.LENGTH_INDEFINITE).setAction("Reload", clickListener).show();
                }
            }
        });

        // Some settings
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(false);
        webSettings.setAllowFileAccess(false);

        webSettings.setDatabaseEnabled(true);

        String cachePath = this.getApplicationContext()
                .getDir("cache", Context.MODE_PRIVATE).getPath();
        webSettings.setAppCachePath(cachePath);
        webSettings.setAllowFileAccess(true);
        webSettings.setAppCacheEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);

        webView.setHorizontalScrollBarEnabled(false);

        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        webView.setBackgroundColor(Color.WHITE);
        webView.setScrollbarFadingEnabled(true);
        webView.setNetworkAvailable(true);

        if (!loadUrlFromIntent(getIntent())) {
            webView.loadUrl("https://m.youtube.com/");
        }

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

        // Tor
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        fabTor = (FloatingActionButton) findViewById(R.id.fab_tor);
        if(OrbotHelper.isOrbotInstalled(getApplicationContext())) {
            fabTor.setVisibility(View.VISIBLE);
            if(sp.getBoolean("torEnabled", false)) {
                torEnable();
                fabTor.setTitle("Disable TOR");
            }
            else {
                fabTor.setTitle("Enable TOR");
            }
            fabTor.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(v.getContext());
                    SharedPreferences.Editor spEdit = sp.edit();
                    if(sp.getBoolean("torEnabled", false)) {
                        spEdit.putBoolean("torEnabled", false);
                        torDisable();
                        fabTor.setTitle("Enable TOR");
                    }
                    else {
                        AlertDialog dialog = new AlertDialog.Builder(MainActivity.this).create();
                        dialog.setTitle("Enable Tor?");
                        dialog.setMessage("Using Tor irresponsibly, like signing in to your YouTube account, will deanonymize your traffic and only make it worse.");
                        dialog.setCancelable(false);
                        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "Enable",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int buttonId) {
                                        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                                        SharedPreferences.Editor spEdit = sp.edit();
                                        spEdit.putBoolean("torEnabled", true);
                                        torEnable();
                                        fabTor.setTitle("Disable TOR");
                                        spEdit.commit();
                                    }
                                });
                        dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int buttonId) {

                                    }
                                });
                        dialog.show();
                    }
                    spEdit.commit();
                }
            });
        }
    }

    @Override
    protected void onNewIntent(final Intent intent) {
        super.onNewIntent(intent);

        loadUrlFromIntent(intent);
    }

    /**
     * Tries to load URL in WebView if Intent contains required data. Also see Intent filter in manifest.
     *
     * @param intent may contain required data
     * @return {@code true} if data is loaded from URL or URL is already loaded, else {@code false}
     */
    private boolean loadUrlFromIntent(final Intent intent) {

        if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
            final String url = intent.getData().toString();

            if (url != null && !url.equals(webView.getUrl())) {
                webView.loadUrl(url);
            }

            return true;
        } else {
            return false;
        }
    }

    public void torEnable() {
        if (!OrbotHelper.isOrbotRunning(getApplicationContext()))
            OrbotHelper.requestStartTor(getApplicationContext());
        try {
            WebkitProxy.setProxy(MainActivity.class.getName(), getApplicationContext(), null, "localhost", 8118);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void torDisable() {
        try {
            WebkitProxy.resetProxy(MainActivity.class.getName(), getApplicationContext());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

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
