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

public final class MediaScannerActivity extends Activity {
  private static final boolean NO_LOG = true;
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

  private static void logI(String s) {
    if (!NO_LOG) {
      Log.i(TAG, s);
    }
  }

  private static void logD(String s) {
    if (!NO_LOG && DEBUGGING) {
      Log.i(TAG, s);
    }
  }

  private static void scanRootFolder(File p, HashSet<String> list) {
    if (p.isFile()) {
      String path;
      try {
        path = p.getCanonicalPath();
      }
      catch (Exception ex) {
        path = p.getAbsolutePath();
        Log.w(TAG,
            "Failed to get canonical path of " +
            path +
            ", fall back to use absolute path. Ex " +
            ex.getMessage());
      }
      logD("Found file " + path);
      list.add(path);
    }
    else if (p.isDirectory()) {
      File[] files = p.listFiles();
      if (files != null && files.length > 0) {
        for (File s : files) {
          scanRootFolder(s, list);
        }
      }
    }
  }

  @Override
  protected void onCreate(Bundle bundle) {
    super.onCreate(bundle);

    logI("Started");

    final HashSet<String> list = new HashSet<>();
    for (int i = 0; i < paths.length; i++) {
      scanRootFolder(
          Environment.getExternalStoragePublicDirectory(paths[i]),
          list);
    }

    logI("Found " + list.size() + " media files");

    HashSet<String> existing = new HashSet<>();
    for (int i = 0; i < contentUris.length; i++) {
      Cursor cur = getContentResolver().query(
                 contentUris[i],
                 new String[] { MediaStore.MediaColumns.DATA },
                 null,
                 null,
                 null);
      if (cur == null) continue;
      while (cur.moveToNext()) {
        File file = null;
        String path =
            cur.getString(cur.getColumnIndex(MediaStore.MediaColumns.DATA));
        try {
          file = new File(path);
          path = file.getCanonicalPath();
        }
        catch (Exception ex) {
          Log.w(TAG,
                "Failed to get canonical path of " +
                path +
                ". Ignore it. Ex " +
                ex.getMessage());
        }
        if (file != null && file.exists() && file.isFile()) {
          logD("Found existing media " + path);
          existing.add(path);
        }
      }
    }

    logI("Found " + existing.size() + " existing media files");

    list.removeAll(existing);
    if (list.isEmpty()) {
      logI("No new media files found, exiting...");
      suicide();
    }

    for (String file : list) {
      logD("Will scan file " + file);
    }
    logI("Will scan " + list.size() + " files");

    MediaScannerConnection.scanFile(
        this,
        list.toArray(new String[0]),
        null,
        new MediaScannerConnection.OnScanCompletedListener() {
          @Override
          public void onScanCompleted(String path, Uri uri) {
            logD("Finished scanning " + path);
            synchronized (list) {
              list.remove(path);
              if (list.isEmpty()) {
                logI("Done, exiting...");
                suicide();
              }
            }
          }
        });
  }

  private void suicide() {
    finish();
    Process.killProcess(Process.myPid());
    System.exit(0);
  }
}
