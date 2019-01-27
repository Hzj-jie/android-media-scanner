package org.gemini.media_scanner;

import android.content.Intent;
import android.net.Uri;
import android.Manifest.permission;
import org.gemini.shared.PermissionRequestActivity;

public final class MediaScannerActivity extends PermissionRequestActivity {
  public MediaScannerActivity() {
    super(permission.READ_EXTERNAL_STORAGE);
  }

  @Override
  protected void onPermissionGranted() {
    startService(new Intent(Intent.ACTION_BOOT_COMPLETED,
                            Uri.EMPTY,
                            this,
                            MediaScannerService.class));
    finish();
  }
}
