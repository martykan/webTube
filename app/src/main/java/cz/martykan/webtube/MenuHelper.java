package cz.martykan.webtube;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
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
import android.view.View;
import android.webkit.WebView;
import android.widget.ArrayAdapter;

import info.guardianproject.netcipher.proxy.OrbotHelper;

public class MenuHelper {
    Context context;
    WebView webView;
    TorHelper torHelper;
    View appWindow;
    SharedPreferences sp;

    public MenuHelper(Context context, WebView webView, TorHelper torHelper, View appWindow) {
        this.context = context;
        this.webView = webView;
        this.torHelper = torHelper;
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
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int buttonId) {
                            dialog.dismiss();
                            SharedPreferences.Editor editor = sp.edit();
                            editor.putBoolean("homepageLearned", true);
                            editor.commit();
                        }
                    });
            dialog.show();
        }
    }


    public void setUpMenu(final View browserButton, final View refreshButton, final View homeButton, View bookmarksButton, View moreButton, final DrawerLayout drawerLayout, final View bookmarksPanel) {
        browserButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                iconAnim(browserButton);
                context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(webView.getUrl())));
            }
        });

        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                iconAnim(refreshButton);
                webView.reload();
            }
        });

        homeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                iconAnim(homeButton);
                homepageTutorial();
                webView.loadUrl(sp.getString("homepage", "https://m.youtube.com/"));
            }
        });

        homeButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                iconAnim(homeButton);
                Snackbar.make(appWindow, context.getString(R.string.homePageSet), Snackbar.LENGTH_LONG).show();
                SharedPreferences.Editor editor = sp.edit();
                editor.putString("homepage", webView.getUrl());
                editor.commit();
                return true;
            }
        });
        
        bookmarksButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(bookmarksPanel);
            }
        });

        moreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                moreButton();
            }
        });
    }

    private void moreButton() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(context, R.layout.list_item);
        arrayAdapter.add(context.getString(R.string.share));

        builder.setTitle(context.getString(R.string.menu));

        // Add Kodi button
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo("org.xbmc.kore", PackageManager.GET_ACTIVITIES);
            arrayAdapter.add(context.getString(R.string.castToKodi));
        } catch (PackageManager.NameNotFoundException e) {
            /* Kodi is not installed */
        }

        // Add Tor button
        if (OrbotHelper.isOrbotInstalled(context.getApplicationContext())) {
            if (sp.getBoolean(TorHelper.PREF_TOR_ENABLED, false)) {
                arrayAdapter.add(context.getString(R.string.disableTor));
            } else {
                arrayAdapter.add(context.getString(R.string.enableTor));
            }
        }

        // Cancel button
        builder.setNegativeButton(
                context.getText(android.R.string.cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        builder.setAdapter(
                arrayAdapter,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (arrayAdapter.getItem(which).equals(context.getString(R.string.share))) {
                            if (!webView.getUrl().contains("/watch")) {
                                show_noVideo_dialog();
                            } else {
                                Intent shareIntent = new Intent();
                                shareIntent.setAction(Intent.ACTION_SEND);
                                shareIntent.putExtra(Intent.EXTRA_TEXT, webView.getUrl());
                                shareIntent.setType("text/plain");
                                context.startActivity(Intent.createChooser(shareIntent, context.getResources().getText(R.string.share_with)));
                            }
                        } else if (arrayAdapter.getItem(which).equals(context.getString(R.string.castToKodi))) {
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
                        } else if (arrayAdapter.getItem(which).equals(context.getString(R.string.enableTor)) || arrayAdapter.getItem(which).equals(context.getString(R.string.disableTor))) {
                            if (sp.getBoolean(TorHelper.PREF_TOR_ENABLED, false)) {
                                torHelper.torDisable();
                            } else {
                                AlertDialog alert = new AlertDialog.Builder(context).create();
                                alert.setTitle(context.getString(R.string.enableTor) + "?");
                                alert.setMessage(context.getString(R.string.torWarning));
                                alert.setCancelable(false);
                                alert.setButton(DialogInterface.BUTTON_POSITIVE, context.getString(R.string.enable),
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int buttonId) {
                                                torHelper.torEnable();
                                            }
                                        });
                                alert.setButton(DialogInterface.BUTTON_NEGATIVE, context.getString(android.R.string.cancel),
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int buttonId) {

                                            }
                                        });
                                alert.show();
                            }
                        }
                    }
                });
        builder.show();
    }

    private void iconAnim(View icon) {
        Animator iconAnim = ObjectAnimator.ofPropertyValuesHolder(
                icon,
                PropertyValuesHolder.ofFloat("scaleX", 1f, 1.5f, 1f),
                PropertyValuesHolder.ofFloat("scaleY", 1f, 1.5f, 1f));
        iconAnim.start();
    }

    private void show_noVideo_dialog() {
        AlertDialog dialog = new AlertDialog.Builder(context/**/).create();
        dialog.setTitle(context.getString(R.string.error_no_video));
        dialog.setMessage(context.getString(R.string.error_select_video_and_retry));
        dialog.setCancelable(true);
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, context.getString(android.R.string.ok).toUpperCase(),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int buttonId) {
                        dialog.dismiss();
                    }
                });
        dialog.show();
    }
}
