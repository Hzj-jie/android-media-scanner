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

        Log.i(TAG, "Started");

        HashSet<String> list = new HashSet<>();
        for (int i = 0; i < paths.length; i++)
        {
            scanRootFolder(
                    Environment.getExternalStoragePublicDirectory(paths[i]),
                    list);
        }

        Log.i(TAG, "Found " + list.size() + " media files");

        HashSet<String> existing = new HashSet<>();
        for (int i = 0; i < contentUris.length; i++) {
            Cursor cur = getContentResolver().query(
                                 contentUris[i],
                                 new String[] { MediaStore.MediaColumns.DATA },
                                 null,
                                 null,
                                 null);
            if (cur == null) {
                continue;
            }
            while (cur.moveToNext()) {
                int index = cur.getColumnIndex(MediaStore.MediaColumns.DATA);
                String path;
                try
                {
                    path = new File(cur.getString(index)).getCanonicalPath();
                }
                catch (Exception ex)
                {
                    path = cur.getString(index);
                    Log.w(TAG,
                          "Failed to get canonical path of " +
                          path +
                          ", fall back to use original path. Ex " +
                          ex.getMessage());
                }
                if (DEBUGGING)
                    Log.i(TAG, "Found existing media " + path);
                existing.add(path);
            }
        }

        Log.i(TAG, "Found " + existing.size() + " existing media files");

        list.removeAll(existing);
        String[] array = list.toArray(new String[0]);
        if (array == null || array.length == 0)
        {
            Log.i(TAG, "No new media files found, exiting...");
            suicide();
        }

        if (DEBUGGING)
        {
            for (int i = 0; i < array.length; i++)
                Log.i(TAG, "Will scan file " + array[i]);
        }

        MediaScannerConnection.scanFile(
                this, array, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    @Override
                    public void onScanCompleted(String path, Uri uri)
                    {
                        if (DEBUGGING)
                        {
                            Log.i(TAG, "Finished scanning " + path);
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
			String path;
			try
			{
				path = p.getCanonicalPath();
			}
			catch (Exception ex)
			{
				path = p.getAbsolutePath();
				Log.w(TAG,
					  "Failed to get canonical path of " +
					  path +
					  ", fall back to use absolute path. Ex " +
					  ex.getMessage());
			}
            if (DEBUGGING)
            {
                onGoing.incrementAndGet();
                Log.i(TAG, "Found file " + path);
            }
            list.add(path);
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
