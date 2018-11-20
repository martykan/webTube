package cz.martykan.webtube;

import android.content.Context;
import android.os.Build;
import com.google.android.material.navigation.NavigationView;
import androidx.drawerlayout.widget.DrawerLayout;
import android.util.Log;
import android.view.MenuItem;
import android.webkit.WebView;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

public class BookmarkSelectedListener implements NavigationView.OnNavigationItemSelectedListener {
    WebView webView;
    Context context;
    String time;
    BookmarkManager bookmarkManager;
    DrawerLayout drawerLayout;

    public BookmarkSelectedListener(Context context, WebView webView, BookmarkManager bookmarkManager, DrawerLayout drawerLayout) {
        this.webView = webView;
        this.context = context;
        this.bookmarkManager = bookmarkManager;
        this.drawerLayout = drawerLayout;
    }

    @Override
    public boolean onNavigationItemSelected(final MenuItem menuItem) {
        final String menuItemTitle = menuItem.getTitle().toString();
        if (menuItemTitle.equals(context.getString(R.string.addPage))) {
            if (!webView.getTitle().equals("YouTube")) {
                if (webView.getUrl().contains("/watch") && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    time = "0";
                    webView.evaluateJavascript("(function() { return document.getElementsByTagName('video')[0].currentTime; })();", value -> {
                        Log.i("VALUE", value);
                        time = value;
                        String url = webView.getUrl();
                        try {
                            time = time.substring(0, time.indexOf("."));
                        } catch (Exception e) {
                            e.printStackTrace();
                            time = "0";
                        }
                        if (url.contains("&t=")) {
                            url = url.substring(0, url.indexOf("&t="));
                        }
                        bookmarkManager.addBookmark(webView.getTitle().replace(" - YouTube", ""), url + "&t=" + time);
                    });
                } else {
                    bookmarkManager.addBookmark(webView.getTitle().replace(" - YouTube", ""), webView.getUrl());
                }
            } else if (webView.getUrl().contains("/results")) {
                int startPosition = webView.getUrl().indexOf("q=") + "q=".length();
                int endPosition = webView.getUrl().indexOf("&", startPosition);
                String title = webView.getUrl().substring(startPosition, endPosition);
                try {
                    title = URLDecoder.decode(title, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    title = URLDecoder.decode(title);
                }
                bookmarkManager.addBookmark(title + " - Search", webView.getUrl());
            }
        } else if (menuItemTitle.equals(context.getString(R.string.removePage))) {
            if (webView.getUrl().contains("/results")) {
                int startPosition = webView.getUrl().indexOf("q=") + "q=".length();
                int endPosition = webView.getUrl().indexOf("&", startPosition);
                String title = webView.getUrl().substring(startPosition, endPosition);
                try {
                    title = URLDecoder.decode(title, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    title = URLDecoder.decode(title);
                }
                bookmarkManager.removeBookmark(title + " - Search");
            } else {
                try {
                    bookmarkManager.removeBookmark(webView.getTitle().replace(" - YouTube", ""));
                } catch (Exception e) {
                    // To prevent crashing when page is not loaded
                }
            }
        } else {
            webView.loadUrl(bookmarkManager.getUrl(menuItemTitle));
            drawerLayout.closeDrawers();
        }
        return true;
    }
}
