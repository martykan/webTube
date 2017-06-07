package cz.martykan.webtube;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class HeadSetReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
            int state = intent.getIntExtra("state", -1);
            switch (state) {
                case 0:
                    MainActivity.pauseVideo();
                    Log.i("Value", "Headset unplugged");
                    break;
                case 1:
                    Log.i("Value", "Headset plugged");
                    break;
                default:
                    break;
            }
        }
    }

}
