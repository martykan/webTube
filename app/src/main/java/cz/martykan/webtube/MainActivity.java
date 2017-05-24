package cz.martykan.webtube;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NotificationCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ActionMenuView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import info.guardianproject.netcipher.proxy.OrbotHelper;
import info.guardianproject.netcipher.web.WebkitProxy;


public class MainActivity extends AppCompatActivity {
    private static final String LOG_TAG = "webTube";

    private static WebView webView;
    String time;
    private View appWindow;
    private ProgressBar progress;
    private FrameLayout customViewContainer;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private SharedPreferences sp;
    // For the snackbar with error message
    View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            webView.loadUrl(sp.getString("homepage", "https://m.youtube.com/"));
        }
    };
    private Context mApplicationContext;

    TorHelper torHelper;
    BackgroundPlayHelper backgroundPlayHelper;
    BookmarkManager bookmarkManager;
    MenuHelper menuHelper;

    public static void toggleVideo() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webView.evaluateJavascript("(function() { return document.getElementsByTagName('video')[0].paused; })();",
                    new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String value) {
                            if (value.equals("true")) {
                                playVideo();
                            } else {
                                pauseVideo();
                            }
                        }
                    });
        } else {
            pauseVideo();
        }
    }

    public static void pauseVideo() {
        webView.setKeepScreenOn(false);
        webView.loadUrl("javascript:document.getElementsByTagName('video')[0].pause();");
    }

    public static void playVideo() {
        webView.setKeepScreenOn(true);
        webView.loadUrl("javascript:document.getElementsByTagName('video')[0].play();");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mApplicationContext = getApplicationContext();
        // Set HW acceleration flags
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = (WebView) findViewById(R.id.webView);
        appWindow = findViewById(R.id.appWindow);
        progress = (ProgressBar) findViewById(R.id.progress);
        customViewContainer = (FrameLayout) findViewById(R.id.customViewContainer);

        sp = PreferenceManager.getDefaultSharedPreferences(this);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        navigationView = (NavigationView) findViewById(R.id.bookmarks_panel);

        // Set up media button receiver
        ((AudioManager) getSystemService(AUDIO_SERVICE)).registerMediaButtonEventReceiver(
                new ComponentName(getPackageName(), MediaButtonIntentReceiver.class.getName()));

        // Set up WebChromeClient
        webView.setWebChromeClient(new WebTubeChromeClient(webView, progress, customViewContainer, drawerLayout, getWindow().getDecorView()));

        // Set up WebViewClient
        webView.setWebViewClient(new WebTubeWebViewClient(this, appWindow, clickListener, findViewById(R.id.statusBarSpace), findViewById(R.id.menu_main)));

        // Set up WebView
        setUpWebview();

        // Initialize bookmarks panel
        bookmarkManager = new BookmarkManager(this, webView);
        bookmarkManager.initalizeBookmarks(navigationView);
        drawerLayout.setDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                bookmarkManager.initalizeBookmarks(navigationView);
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                bookmarkManager.initalizeBookmarks(navigationView);
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                /* Nothing */
            }

            @Override
            public void onDrawerStateChanged(int newState) {
                /* Nothing */
            }
        });

        navigationView.setNavigationItemSelectedListener(new BookmarkSelectedListener(this, webView, bookmarkManager, drawerLayout));

        // Tor
        torHelper = new TorHelper(mApplicationContext, webView);
        torHelper.setUpTor();

        backgroundPlayHelper = new BackgroundPlayHelper(mApplicationContext, webView);

        // Menu helper
        ActionMenuView actionMenu = (ActionMenuView) findViewById(R.id.menu_main);
        menuHelper = new MenuHelper(this, webView, torHelper, backgroundPlayHelper, appWindow);
        getMenuInflater().inflate(R.menu.menu_main, actionMenu.getMenu());
        menuHelper.setUpMenu(actionMenu, drawerLayout, findViewById(R.id.bookmarks_panel));
        actionMenu.setOverflowIcon(getResources().getDrawable(R.drawable.ic_dots_vertical_white_24dp));


        // Load the page
        if (!loadUrlFromIntent(getIntent())) {
            webView.loadUrl(sp.getString("homepage", "https://m.youtube.com/"));
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if(backgroundPlayHelper.isBackgroundPlayEnabled()) {
            backgroundPlayHelper.playInBackground();
        }
        else {
            pauseVideo();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        backgroundPlayHelper.hideBackgroundPlaybackNotification();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        backgroundPlayHelper.hideBackgroundPlaybackNotification();
        ((AudioManager) getSystemService(AUDIO_SERVICE)).unregisterMediaButtonEventReceiver(
                new ComponentName(getPackageName(), MediaButtonIntentReceiver.class.getName()));
    }

    @Override
    protected void onNewIntent(final Intent intent) {
        super.onNewIntent(intent);
        loadUrlFromIntent(intent);
    }

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

    public void setUpWebview() {
        // To save login info
        CookieHelper.acceptCookies(webView, true);

        // Some settings
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(false);
        webSettings.setAllowFileAccess(false);

        webSettings.setDatabaseEnabled(true);

        String cachePath = mApplicationContext
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

    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            finish();
        }

    }
}
