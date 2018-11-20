package cz.martykan.webtube;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import com.google.android.material.navigation.NavigationView;
import android.view.Menu;
import android.webkit.WebView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class BookmarkManager {
    private List<String> bookmarkUrls;
    private List<String> bookmarkTitles;

    Context context;
    WebView webView;
    NavigationView navigationView;
    SharedPreferences sp;

    public BookmarkManager(Context context, WebView webView) {
        this.context = context;
        this.webView = webView;

        sp = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public void initializeBookmarks(NavigationView navigationView) {
        this.navigationView = navigationView;
        bookmarkUrls = new ArrayList<>();
        List<String> bookmarkTimelessUrls = new ArrayList<>();
        bookmarkTitles = new ArrayList<>();

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
                String timeless = bookmark.getString("url");
                if (timeless.contains("&t=")) {
                    timeless = timeless.substring(0, timeless.indexOf("&t="));
                }
                bookmarkTimelessUrls.add(timeless);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        try {
            String url = webView.getUrl();
            if (url.contains("&t=")) {
                url = url.substring(0, url.indexOf("&t="));
            }

            if (url.contains("/results")) {
                url = url.replace("+", "%20");
            }

            if (bookmarkUrls.contains(webView.getUrl())
                    || bookmarkTitles.contains(webView.getTitle().replace("'", "\\'"))
                    || bookmarkTimelessUrls.contains(url)) {
                menu.add(context.getString(R.string.removePage)).setIcon(R.drawable.ic_close_grey600_24dp);
            } else {
                menu.add(context.getString(R.string.addPage)).setIcon(R.drawable.ic_plus_grey600_24dp);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addBookmark(String title, String url) {
        String result = sp.getString("bookmarks", "[]");
        try {
            JSONArray bookmarksArray = new JSONArray(result);
            bookmarksArray.put(new JSONObject("{'title':'" + title.replace("'", "\\'") + "','url':'" + url + "'}"));
            SharedPreferences.Editor editor = sp.edit();
            editor.putString("bookmarks", bookmarksArray.toString());
            editor.apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        initializeBookmarks(navigationView);
    }

    public void removeBookmark(String title) {
        String result = sp.getString("bookmarks", "[]");
        try {
            JSONArray bookmarksArray = new JSONArray(result);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
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
            editor.apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        initializeBookmarks(navigationView);
    }

    public String getUrl(String title) {
        return bookmarkUrls.get(bookmarkTitles.indexOf(title));
    }

    public static List<JSONObject> asList(final JSONArray ja) {
        final int len = ja.length();
        final ArrayList<JSONObject> result = new ArrayList<>(len);
        for (int i = 0; i < len; i++) {
            final JSONObject obj = ja.optJSONObject(i);
            if (obj != null) {
                result.add(obj);
            }
        }
        return result;
    }
}
