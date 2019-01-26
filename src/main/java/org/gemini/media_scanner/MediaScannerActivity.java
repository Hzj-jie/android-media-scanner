package org.gemini.media_scanner;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

public final class MediaScannerActivity extends Activity {
  @Override
  protected void onCreate(Bundle bundle) {
    super.onCreate(bundle);
    startService(new Intent(Intent.ACTION_BOOT_COMPLETED,
                            Uri.EMPTY,
                            this,
                            MediaScannerService.class));
    finish();
  }
}
