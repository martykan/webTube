package cz.martykan.webtube;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.KeyEvent;

public class MediaButtonIntentReceiver extends BroadcastReceiver {

    public MediaButtonIntentReceiver() {
        super();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String intentAction = intent.getAction();
        if (!Intent.ACTION_MEDIA_BUTTON.equals(intentAction)) {
            return;
        }
		KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
        Log.i("Key", event.getKeyCode() + " pressed");

        int action = event.getAction();
        if (action == KeyEvent.ACTION_DOWN) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_MEDIA_PAUSE || event.getKeyCode() == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || event.getKeyCode() == 79) {
                try {
                    MainActivity.toggleVideo();
                } catch (Exception e) {
                    // Activity is not running
                    e.printStackTrace();
                }
            }
        }
    }
}
