package org.gemini.media_scanner;

import android.app.Activity;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Process;
import android.util.Log;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

public final class MediaScannerActivity
        extends Activity
        implements MediaScannerConnection.MediaScannerConnectionClient
{
    private static final String paths[] = {
            Environment.DIRECTORY_DCIM,
            Environment.DIRECTORY_MOVIES,
            Environment.DIRECTORY_PICTURES
    };
    private final AtomicInteger onGoing = new AtomicInteger();
    private MediaScannerConnection connection;

    @Override
    protected void onCreate(Bundle bundle)
    {
        super.onCreate(bundle);
        connection = new MediaScannerConnection(this, this);
        connection.connect();
    }

    @Override
    public void onMediaScannerConnected()
    {
        for (int i = 0; i < paths.length; i++)
            scanRootFolder(
                    Environment.getExternalStoragePublicDirectory(paths[i]));
    }

    @Override
    public void onScanCompleted(String path, Uri uri)
    {
        if (onGoing.decrementAndGet() == 0)
        {
            connection.disconnect();
            finish();
            Process.killProcess(Process.myPid());
            System.exit(0);
        }
    }

    private void scanRootFolder(File p)
    {
        if (p.isFile())
        {
            onGoing.incrementAndGet();
            connection.scanFile(p.getAbsolutePath(), null);
        }
        else if (p.isDirectory())
        {
            for (File s : p.listFiles())
                scanRootFolder(s);
        }
    }
}
