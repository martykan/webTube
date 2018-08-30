package cz.martykan.webtube;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.webkit.WebView;

public class BackgroundPlayHelper {
    public static final String PREF_BACKGROUND_PLAY_ENABLED = "backgroundPlayEnabled";
    private static final int NOTIFICATION_ID = 1337 - 420 * 69;

    Context context;
    WebView webView;
    SharedPreferences sp;

    public BackgroundPlayHelper (Context context, WebView webView) {
        this.context = context;
        this.webView = webView;

        sp = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public void enableBackgroundPlay() {
        SharedPreferences.Editor spedit = sp.edit();
        spedit.putBoolean(PREF_BACKGROUND_PLAY_ENABLED, true);
		spedit.apply();
    }

    public void disableBackgroundPlay() {
        SharedPreferences.Editor spedit = sp.edit();
        spedit.putBoolean(PREF_BACKGROUND_PLAY_ENABLED, false);
		spedit.apply();
    }

    public boolean isBackgroundPlayEnabled() {
        return sp.getBoolean(PREF_BACKGROUND_PLAY_ENABLED, true);
    }

    public void playInBackground() {
        try {
            if (webView.getUrl().contains("/watch")) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
					webView.evaluateJavascript("(function() { if(document.getElementsByTagName('video')[0].paused == false) { return 'playing'; } else { return 'stopped'; } })();", value -> {
						Log.i("VALUE", value);
						if (value.equals("\"playing\"")) {
							showBackgroundPlaybackNotification();
						}
					});
                } else {
                    showBackgroundPlaybackNotification();
                }
            }
        } catch (Exception e) {
            // When the WebView is not loaded, it crashes (but that can be ignored)
            e.printStackTrace();
        }
    }

    public void showBackgroundPlaybackNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_headphones_white_24dp)
                .setOngoing(true)
                .setColor(Color.parseColor("#E62118"))
                .addAction(R.drawable.ic_pause_grey600_24dp, "PAUSE", NotificationCloser.getDismissIntent(NOTIFICATION_ID, context))
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(webView.getTitle().replace(" - YouTube", ""))
                .setAutoCancel(true)
				.setContentIntent(PendingIntent.getActivity(
                                context,
                                NOTIFICATION_ID,
                                new Intent(context, MainActivity.class)
                                        .setAction(Intent.ACTION_VIEW)
                                        .setData(Uri.parse(webView.getUrl())),
                                PendingIntent.FLAG_UPDATE_CURRENT));
		NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(NOTIFICATION_ID, builder.build());
    }

    public void hideBackgroundPlaybackNotification() {
		NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(NOTIFICATION_ID);
    }
}
