package cz.martykan.webtube;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.net.URLEncoder;

public class Downloader {
    Context context;

    public Downloader (Context context) {
        this.context = context;
    }

    // Minimal effort solution, to be improved
    public void download(String url) {
        String encodedURL = url;
        try {
            encodedURL = URLEncoder.encode(url, "utf-8");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://keepvid.com/?url=" + encodedURL)));

    }
}
