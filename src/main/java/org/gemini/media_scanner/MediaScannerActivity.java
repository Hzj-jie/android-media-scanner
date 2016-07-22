package org.gemini.media_scanner;

import android.app.Activity;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Process;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public final class MediaScannerActivity extends Activity
{
    private static final String paths[] = {
            Environment.DIRECTORY_DCIM,
            Environment.DIRECTORY_MOVIES,
            Environment.DIRECTORY_PICTURES
    };
    private final AtomicInteger onGoing = new AtomicInteger();

    @Override
    protected void onCreate(Bundle bundle)
    {
        super.onCreate(bundle);
        ArrayList<String> list = new ArrayList<>();
        for (int i = 0; i < paths.length; i++)
        {
            scanRootFolder(
                    Environment.getExternalStoragePublicDirectory(paths[i]),
                    list);
        }
        MediaScannerConnection.scanFile(
                this, list.toArray(new String[0]), null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    @Override
                    public void onScanCompleted(String path, Uri uri)
                    {
                        if (onGoing.decrementAndGet() == 0)
                        {
                            finish();
                            Process.killProcess(Process.myPid());
                            System.exit(0);
                        }
                    }
                });
    }

    private void scanRootFolder(File p, ArrayList<String> list)
    {
        if (p.isFile())
        {
            onGoing.incrementAndGet();
            list.add(p.getAbsolutePath());
        }
        else if (p.isDirectory())
        {
            for (File s : p.listFiles())
                scanRootFolder(s, list);
        }
    }
}
