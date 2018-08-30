package cz.martykan.webtube;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.ActionMenuView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;

import info.guardianproject.netcipher.proxy.OrbotHelper;

public class MenuHelper implements ActionMenuView.OnMenuItemClickListener {
    Context context;
    WebView webView;
    TorHelper torHelper;
    BackgroundPlayHelper backgroundPlayHelper;
    View appWindow;
    ActionMenuView actionMenu;
    SharedPreferences sp;
    DrawerLayout drawerLayout;
    View bookmarksPanel;

    public static final String PREF_COOKIES_ENABLED = "cookiesEnabled";

    public MenuHelper(Context context, WebView webView, TorHelper torHelper, BackgroundPlayHelper backgroundPlayHelper, View appWindow) {
        this.context = context;
        this.webView = webView;
        this.torHelper = torHelper;
        this.backgroundPlayHelper = backgroundPlayHelper;
        this.appWindow = appWindow;

        sp = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public void homepageTutorial() {
        if (!sp.getBoolean("homepageLearned", false)) {
            AlertDialog dialog = new AlertDialog.Builder(context).create();
            dialog.setTitle(context.getString(R.string.home));
            dialog.setMessage(context.getString(R.string.homePageHelp));
            dialog.setCancelable(false);
            dialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK",
					(dialog1, buttonId) -> {
						dialog1.dismiss();
						SharedPreferences.Editor editor = sp.edit();
						editor.putBoolean("homepageLearned", true);
						editor.apply();
					});
            dialog.show();
        }
    }

    public void setUpMenu(final ActionMenuView actionMenu, final DrawerLayout drawerLayout, final View bookmarksPanel ) {
        this.drawerLayout = drawerLayout;
        this.bookmarksPanel = bookmarksPanel;
        this.actionMenu = actionMenu;

        actionMenu.setOnMenuItemClickListener(this);

        // Enable special buttons
        Menu menu = actionMenu.getMenu();
        PackageManager pm = context.getPackageManager();

        menu.findItem(R.id.action_backgroundPlay).setChecked(sp.getBoolean(BackgroundPlayHelper.PREF_BACKGROUND_PLAY_ENABLED, true));
        menu.findItem(R.id.action_accept_cookies).setChecked(sp.getBoolean(PREF_COOKIES_ENABLED,true));

        // Tor button
        if (OrbotHelper.isOrbotInstalled(context.getApplicationContext())) {
            menu.findItem(R.id.action_tor)
                    .setEnabled(true)
                    .setChecked(sp.getBoolean(TorHelper.PREF_TOR_ENABLED, false));
        }

        // Add Kodi button
        try {
            pm.getPackageInfo("org.xbmc.kore", PackageManager.GET_ACTIVITIES);
            menu.findItem(R.id.action_cast_to_kodi).setEnabled(true);
        } catch (PackageManager.NameNotFoundException e) {
            /* Kodi is not installed */
        }
    }
    private void show_noVideo_dialog() {
        AlertDialog dialog = new AlertDialog.Builder(context/**/).create();
        dialog.setTitle(context.getString(R.string.error_no_video));
        dialog.setMessage(context.getString(R.string.error_select_video_and_retry));
        dialog.setCancelable(true);
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, context.getString(android.R.string.ok).toUpperCase(),
				(dialog1, buttonId) -> dialog1.dismiss());
        dialog.show();
    }

    @Override
    public boolean onMenuItemClick(final MenuItem item) {
        switch(item.getItemId()){
            case R.id.action_web:
                context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(webView.getUrl())));
                return true;

            case R.id.action_refresh:
                webView.reload();
                return true;

            case R.id.action_home:
                homepageTutorial();
                webView.loadUrl(sp.getString("homepage", "https://m.youtube.com/"));
                return true;

            case R.id.action_set_as_home:
                Snackbar.make(appWindow, context.getString(R.string.homePageSet), Snackbar.LENGTH_LONG).show();
                SharedPreferences.Editor editor = sp.edit();
                editor.putString("homepage", webView.getUrl());
				editor.apply();
                return true;

            case R.id.action_bookmarks:
                drawerLayout.openDrawer(bookmarksPanel);
                return true;

            case R.id.action_share:
                if (!webView.getUrl().contains("/watch")) {
                    show_noVideo_dialog();
                } else {
                    Intent shareIntent = new Intent();
                    shareIntent.setAction(Intent.ACTION_SEND);
                    shareIntent.putExtra(Intent.EXTRA_TEXT, webView.getUrl());
                    shareIntent.setType("text/plain");
                    context.startActivity(Intent.createChooser(shareIntent, context.getResources().getText(R.string.share_with)));
                }
                return true;

            case R.id.action_cast_to_kodi:
                if (!webView.getUrl().contains("/watch")) {
                    show_noVideo_dialog();
                } else {
                    try {
                            /* The following code is based on an extract from the source code of NewPipe (v0.7.2) (https://github.com/theScrabi/NewPipe),
                            which is also licenced under version 3 of the GNU General Public License as published by the Free Software Foundation.
                            The copyright owner of the original code is Christian Schabesberger <chris.schabesberger@mailbox.org>.
                            All modifications were made on 06-Jan-2016 */
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setPackage("org.xbmc.kore");
                        intent.setData(Uri.parse(webView.getUrl().replace("https", "http")));
                        context.startActivity(intent);
						/* End of the modified NewPipe code extract */
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return true;

            case R.id.action_backgroundPlay:
                if (sp.getBoolean(BackgroundPlayHelper.PREF_BACKGROUND_PLAY_ENABLED, true)) {
                    backgroundPlayHelper.disableBackgroundPlay();
                    item.setChecked(false);
                } else {
                    backgroundPlayHelper.enableBackgroundPlay();
                    item.setChecked(true);
                }
                return  true;

            case R.id.action_tor:
                final MenuItem cookieItem = actionMenu.getMenu().findItem(R.id.action_accept_cookies);
                try {
                    if (sp.getBoolean(TorHelper.PREF_TOR_ENABLED, false)) {
                        torHelper.torDisable();
                        item.setChecked(false);
                        cookieItem.setChecked(sp.getBoolean(PREF_COOKIES_ENABLED, true)).setEnabled(true);
                    } else {
                        AlertDialog alert = new AlertDialog.Builder(context).create();
                        alert.setTitle(context.getString(R.string.enableTor) + "?");
                        alert.setMessage(context.getString(R.string.torWarning));
                        alert.setCancelable(false);
                        alert.setButton(DialogInterface.BUTTON_POSITIVE, context.getString(R.string.enable),
								(dialog, buttonId) -> {
									torHelper.torEnable();
									item.setChecked(true);
									cookieItem.setChecked(false).setEnabled(false);
								});
                        alert.setButton(DialogInterface.BUTTON_NEGATIVE, context.getString(android.R.string.cancel),
								(dialog, buttonId) -> item.setChecked(false));
                        alert.show();
                    }
                }
                catch (Exception e){
                    Log.d("WebTube",e.getMessage());
                }
                return  true;

            case R.id.action_download:
                if (!webView.getUrl().contains("/watch")) {
                    show_noVideo_dialog();
                } else {
                    new Downloader(context).download(webView.getUrl());
                }
                return true;

            case R.id.action_accept_cookies:
                if (sp.getBoolean(PREF_COOKIES_ENABLED, true)) {
                    CookieHelper.acceptCookies(webView,false);
                    CookieHelper.deleteCookies();
                    item.setChecked(false);
                } else {
                    CookieHelper.acceptCookies(webView,true);
                    item.setChecked(true);
                }
                SharedPreferences.Editor spEdit = sp.edit();
                spEdit.putBoolean(PREF_COOKIES_ENABLED,!sp.getBoolean(PREF_COOKIES_ENABLED,true));
				spEdit.apply();
                return  true;
        }
        return false;
    }
}
