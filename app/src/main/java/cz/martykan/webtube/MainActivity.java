package cz.martykan.webtube;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import info.guardianproject.netcipher.proxy.OrbotHelper;
import info.guardianproject.netcipher.web.WebkitProxy;


public class MainActivity extends AppCompatActivity {

    public static String LOG_TAG = "webTube";
    WebView webView;
    View appWindow;
    Window window;
    ProgressBar progress;
    View mCustomView;
    FrameLayout customViewContainer;
    FloatingActionButton fabTor;
    DrawerLayout drawerLayout;
    NavigationView navigationView;
    SharedPreferences sp;

    List<String> bookmarkUrls;
    List<String> bookmarkTitles;

    // For the snackbar with error message
    View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            webView.loadUrl(sp.getString("homepage", "https://m.youtube.com/"));
        }
    };

    public static List<JSONObject> asList(final JSONArray ja) {
        final int len = ja.length();
        final ArrayList<JSONObject> result = new ArrayList<JSONObject>(len);
        for (int i = 0; i < len; i++) {
            final JSONObject obj = ja.optJSONObject(i);
            if (obj != null) {
                result.add(obj);
            }
        }
        return result;
    }

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

        sp = PreferenceManager.getDefaultSharedPreferences(this);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        navigationView = (NavigationView) findViewById(R.id.bookmarks_panel);

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
                } else {
                    if (percentage == 100) {
                        progress.setVisibility(View.GONE);
                    }
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
                if (!url.contains(".jpg")) {
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

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                initalizeBookmarks(navigationView);
            }

            // Deal with error messages
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                if (description.toString().contains("NETWORK_CHANGED")) {
                    webView.loadUrl(sp.getString("homepage", "https://m.youtube.com/"));
                } else if (description.toString().contains("NAME_NOT_RESOLVED")) {
                    Snackbar.make(appWindow, getString(R.string.errorNoInternet), Snackbar.LENGTH_INDEFINITE).setAction(getString(R.string.refresh), clickListener).show();
                } else if (description.toString().contains("PROXY_CONNECTION_FAILED")) {
                    Snackbar.make(appWindow, getString(R.string.errorTor), Snackbar.LENGTH_INDEFINITE).setAction(getString(R.string.refresh), clickListener).show();
                } else {
                    Snackbar.make(appWindow, getString(R.string.error) + " " + description, Snackbar.LENGTH_INDEFINITE).setAction(getString(R.string.refresh), clickListener).show();
                }
            }
        });

        // Set up webView
        setUpWebview();

        // Initialize bookmarks panel
        initalizeBookmarks(navigationView);
        drawerLayout.setDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                initalizeBookmarks(navigationView);
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                initalizeBookmarks(navigationView);
            }

            @Override
            public void onDrawerClosed(View drawerView) {

            }

            @Override
            public void onDrawerStateChanged(int newState) {

            }
        });
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {
                if (menuItem.getTitle() == getString(R.string.addPage)) {
                    if (!webView.getTitle().equals("YouTube")) {
                        addBookmark(webView.getTitle().replace(" - YouTube", ""), webView.getUrl());
                    }
                } else if (menuItem.getTitle() == getString(R.string.removePage)) {
                    removeBookmark(webView.getTitle().replace(" - YouTube", ""));
                } else {
                    webView.loadUrl(bookmarkUrls.get(bookmarkTitles.indexOf(menuItem.getTitle())));
                    drawerLayout.closeDrawers();
                }
                return true;
            }
        });

        // Floating action buttons
        setUpFABs();

        // Tor
        fabTor = (FloatingActionButton) findViewById(R.id.fab_tor);
        setUpTor();

        // Load the page
        if (!loadUrlFromIntent(getIntent())) {
            webView.loadUrl(sp.getString("homepage", "https://m.youtube.com/"));
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

    public void setUpWebview() {
        // To save login info
        CookieManager.getInstance().setAcceptCookie(true);
        if (Integer.valueOf(Build.VERSION.SDK_INT) >= 21) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        }

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

    }

    public void setUpFABs() {
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
                webView.loadUrl(sp.getString("homepage", "https://m.youtube.com/"));
            }
        });

        fabHome.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Snackbar.make(appWindow, getString(R.string.homePageSet), Snackbar.LENGTH_LONG).show();
                SharedPreferences.Editor editor = sp.edit();
                editor.putString("homepage", webView.getUrl());
                editor.commit();
                return true;
            }
        });

        FloatingActionButton fabBookmarks = (FloatingActionButton) findViewById(R.id.fab_bookmarks);
        fabBookmarks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(findViewById(R.id.bookmarks_panel));
            }
        });

        // Check if Kodi is installed
        FloatingActionButton fabKodi = (FloatingActionButton) findViewById(R.id.fab_kodi);
        PackageManager pm = getPackageManager();
        try {
            pm.getPackageInfo("org.xbmc.kore", PackageManager.GET_ACTIVITIES);
            fabKodi.setVisibility(View.VISIBLE);
        } catch (PackageManager.NameNotFoundException e) {
            fabKodi.setVisibility(View.GONE);
        }

        fabKodi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!webView.getUrl().contains("/watch")) {
                    AlertDialog dialog = new AlertDialog.Builder(MainActivity.this).create();
                    dialog.setTitle("No Video!");
                    dialog.setMessage("Please select a video and try again.");
                    dialog.setCancelable(true);
                    dialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int buttonId) {
                                    dialog.dismiss();
                                }
                            });
                    dialog.show();
                } else {
                    try {
                        /*The following code is based on an extract from the source code of NewPipe (v0.7.2) (https://github.com/theScrabi/NewPipe),
                        which is also licenced under version 3 of the GNU General Public License as published by the Free Software Foundation.
                        The copyright owner of the original code is Christian Schabesberger <chris.schabesberger@mailbox.org>.
                        All modifications were made on 06-Jan-2016*/
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setPackage("org.xbmc.kore");
                        intent.setData(Uri.parse(webView.getUrl().replace("https", "http")));
                        MainActivity.this.startActivity(intent);
                        /*End of the modified NewPipe code extract*/
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    public void initalizeBookmarks(NavigationView navigationView) {
        bookmarkUrls = new ArrayList<String>();
        bookmarkTitles = new ArrayList<String>();

        final Menu menu = navigationView.getMenu();
        menu.clear();
        String result = sp.getString("bookmarks", "[]");
        try {
            JSONArray bookmarksArray = new JSONArray(result);
            for (int i = 0; i < bookmarksArray.length(); i++) {
                JSONObject bookmark = bookmarksArray.getJSONObject(i);
                menu.add(bookmark.getString("title")).setIcon(R.drawable.ic_star_grey600_24dp);
                bookmarkTitles.add(bookmark.getString("title"));
                bookmarkUrls.add(bookmark.getString("url"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (!bookmarkUrls.contains(webView.getUrl())) {
            menu.add(getString(R.string.addPage)).setIcon(R.drawable.ic_plus_grey600_24dp);
        } else {
            menu.add(getString(R.string.removePage)).setIcon(R.drawable.ic_close_grey600_24dp);
        }
    }

    public void addBookmark(String title, String url) {
        String result = sp.getString("bookmarks", "[]");
        try {
            JSONArray bookmarksArray = new JSONArray(result);
            bookmarksArray.put(new JSONObject("{'title':'" + title + "','url':'" + url + "'}"));
            SharedPreferences.Editor editor = sp.edit();
            editor.putString("bookmarks", bookmarksArray.toString());
            editor.commit();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        initalizeBookmarks(navigationView);
    }

    public void removeBookmark(String title) {
        String result = sp.getString("bookmarks", "[]");
        try {
            JSONArray bookmarksArray = new JSONArray(result);
            if (Integer.valueOf(Build.VERSION.SDK_INT) >= 19) {
                bookmarksArray.remove(bookmarkTitles.indexOf(title));
            } else {
                final List<JSONObject> objs = asList(bookmarksArray);
                objs.remove(bookmarkTitles.indexOf(title));
                final JSONArray out = new JSONArray();
                for (final JSONObject obj : objs) {
                    out.put(obj);
                }
                bookmarksArray = out;
            }
            SharedPreferences.Editor editor = sp.edit();
            editor.putString("bookmarks", bookmarksArray.toString());
            editor.commit();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        initalizeBookmarks(navigationView);
    }

    public void setUpTor() {
        // Tor
        fabTor = (FloatingActionButton) findViewById(R.id.fab_tor);
        if (OrbotHelper.isOrbotInstalled(getApplicationContext())) {
            fabTor.setVisibility(View.VISIBLE);
            if (sp.getBoolean("torEnabled", false)) {
                torEnable();
                fabTor.setTitle(getString(R.string.disableTor));
            } else {
                fabTor.setTitle(getString(R.string.enableTor));
            }
            fabTor.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SharedPreferences.Editor spEdit = sp.edit();
                    if (sp.getBoolean("torEnabled", false)) {
                        spEdit.putBoolean("torEnabled", false);
                        torDisable();
                        fabTor.setTitle(getString(R.string.enableTor));
                    } else {
                        AlertDialog dialog = new AlertDialog.Builder(MainActivity.this).create();
                        dialog.setTitle(getString(R.string.enableTor) + "?");
                        dialog.setMessage(getString(R.string.torWarning));
                        dialog.setCancelable(false);
                        dialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.enable),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int buttonId) {
                                        SharedPreferences.Editor spEdit = sp.edit();
                                        spEdit.putBoolean("torEnabled", true);
                                        torEnable();
                                        fabTor.setTitle(getString(R.string.disableTor));
                                        spEdit.commit();
                                    }
                                });
                        dialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.cancel),
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

    public void torEnable() {
        if (!OrbotHelper.isOrbotRunning(getApplicationContext()))
            OrbotHelper.requestStartTor(getApplicationContext());
        try {
            WebkitProxy.setProxy(MainActivity.class.getName(), getApplicationContext(), null, "localhost", 8118);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void torDisable() {
        try {
            WebkitProxy.resetProxy(MainActivity.class.getName(), getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }
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
