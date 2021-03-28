package cz.martykan.webtube;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import java.net.URLEncoder;

public class Downloader {
    Context context;
    String part1;
    String part2;

    public Downloader(Context context) {
        this.context = context;
    }

    // Minimal effort solution, to be improved
    public void download(String url) {
        if (url.contains("/watch")) {
            String[] splitUrl = url.split("=");
            part1 = splitUrl[0];
            part2 = splitUrl[1];
        } else {
            Log.i("Invalid Url Format", "");
        }
        context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.y2mate.com/youtube/" + part2)));
    }
}
