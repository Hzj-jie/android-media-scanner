package org.gemini.media_scanner;

import android.app.Activity;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Process;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;

public final class MediaScannerActivity extends Activity
{
    private static final boolean DEBUGGING = false;
    private static final String TAG =
            MediaScannerActivity.class.getSimpleName();

    private static final String paths[] = {
            Environment.DIRECTORY_DCIM,
            Environment.DIRECTORY_MOVIES,
            Environment.DIRECTORY_MUSIC,
            Environment.DIRECTORY_PICTURES
    };
    private static final Uri contentUris[] = {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    };

    private final AtomicInteger onGoing = new AtomicInteger();

    @Override
    protected void onCreate(Bundle bundle)
    {
        super.onCreate(bundle);

        HashSet<String> list = new HashSet<>();
        for (int i = 0; i < paths.length; i++)
        {
            scanRootFolder(
                    Environment.getExternalStoragePublicDirectory(paths[i]),
                    list);
        }

        HashSet<String> existing = new HashSet<>();
        for (int i = 0; i < contentUris.length; i++) {
            Cursor cur = getContentResolver().query(
                                 contentUris[i],
                                 new String[] { MediaStore.MediaColumns.DATA },
                                 null,
                                 null,
                                 null);
            while (cur.moveToNext()) {
                int index = cur.getColumnIndex(MediaStore.MediaColumns.DATA);
                if (DEBUGGING)
                    Log.e(TAG, "Found existing media " + cur.getString(index));
                existing.add(cur.getString(index).toLowerCase());
            }
        }
        list.removeAll(existing);
        String[] array = list.toArray(new String[0]);
        if (array == null || array.length == 0) suicide();

        if (DEBUGGING)
        {
            for (int i = 0; i < array.length; i++)
                Log.e(TAG, "Will scan file " + array[i]);
        }

        MediaScannerConnection.scanFile(
                this, array, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    @Override
                    public void onScanCompleted(String path, Uri uri)
                    {
                        if (DEBUGGING)
                        {
                            Log.e(TAG, "Finished scanning " + path);
                            if (onGoing.decrementAndGet() == 0) suicide();
                        }
                        else suicide();
                    }
                });
    }

    private void scanRootFolder(File p, HashSet<String> list)
    {
        if (p.isFile())
        {
            if (DEBUGGING)
            {
                onGoing.incrementAndGet();
                Log.e(TAG, "Found file " + p.getAbsolutePath());
            }
            list.add(p.getAbsolutePath().toLowerCase());
        }
        else if (p.isDirectory())
        {
            File[] files = p.listFiles();
            if (files != null && files.length > 0)
                for (File s : files) scanRootFolder(s, list);
        }
    }

    private void suicide()
    {
        finish();
        Process.killProcess(Process.myPid());
        System.exit(0);
    }
}
