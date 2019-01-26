package org.gemini.media_scanner;

import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.Process;
import android.provider.MediaStore;
import android.util.Log;
import java.io.File;
import java.util.HashSet;
import org.gemini.shared.Storage;

public final class MediaScannerService extends Service {
  private static final boolean NO_LOG = true;
  private static final boolean DEBUGGING = false;
  private static final String TAG =
      MediaScannerService.class.getSimpleName();

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

  private final Storage storage = new Storage(this);

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
  public int onStartCommand(Intent intent, int flags, int startId) {
    logI("Started");

    final HashSet<String> list = new HashSet<>();
    for (int i = 0; i < paths.length; i++) {
      scanRootFolder(
          Environment.getExternalStoragePublicDirectory(paths[i]),
          list);

      scanRootFolder(
          new File(storage.buildInSharedStoragePath(), paths[i]),
          list);

      scanRootFolder(
          new File(storage.externalSharedStoragePath(), paths[i]),
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
        } else {
          logI("Media file " + path + " does not exist, will rescan it.");
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
    return START_STICKY;
  }

  private void suicide() {
    stopSelf();
    Process.killProcess(Process.myPid());
    System.exit(0);
  }

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }
}
