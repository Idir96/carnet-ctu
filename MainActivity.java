package com.carnetct.app;

import android.app.Activity;
import android.content.ClipData;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.core.content.FileProvider;
import androidx.webkit.WebViewAssetLoader;
import androidx.webkit.WebViewClientCompat;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Carnet CT: shows the offline web app (assets/index.html) inside a WebView.
 * Data stays on the phone (localStorage + IndexedDB of the WebView, inside the app's private storage).
 * The app declares no INTERNET permission, so it cannot reach the network at all.
 */
public class MainActivity extends Activity {

    private static final String HOST = "appassets.androidplatform.net";
    private static final String START_URL = "https://" + HOST + "/assets/index.html";
    private static final int REQ_FILE = 1001;
    private static final int REQ_CAMERA = 1002;

    private WebView webView;
    private ValueCallback<Uri[]> filePathCallback;
    private Uri cameraUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cleanCameraCache();

        final WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                .setDomain(HOST)
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        webView = new WebView(this);
        webView.setBackgroundColor(Color.parseColor("#EDF1F3"));
        setContentView(webView);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(false);
        s.setMediaPlaybackRequiresUserGesture(true);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);

        webView.addJavascriptInterface(new Bridge(), "AndroidBridge");

        webView.setWebViewClient(new WebViewClientCompat() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return assetLoader.shouldInterceptRequest(request.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                // the app never leaves its own pages; anything else is ignored
                return !HOST.equals(request.getUrl().getHost());
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (filePathCallback != null) {
                    filePathCallback.onReceiveValue(null);
                }
                filePathCallback = callback;
                cameraUri = null;
                try {
                    if (params.isCaptureEnabled()) {
                        File dir = new File(getCacheDir(), "camera");
                        //noinspection ResultOfMethodCallIgnored
                        dir.mkdirs();
                        File photo = File.createTempFile("fiche_", ".jpg", dir);
                        cameraUri = FileProvider.getUriForFile(MainActivity.this, getPackageName() + ".fileprovider", photo);
                        Intent camera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        camera.putExtra(MediaStore.EXTRA_OUTPUT, cameraUri);
                        camera.setClipData(ClipData.newRawUri("fiche", cameraUri));
                        camera.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivityForResult(camera, REQ_CAMERA);
                    } else {
                        startActivityForResult(params.createIntent(), REQ_FILE);
                    }
                } catch (Exception e) {
                    deliver(null);
                    Toast.makeText(MainActivity.this, "Could not open the camera or file picker", Toast.LENGTH_LONG).show();
                }
                return true;
            }
        });

        webView.loadUrl(START_URL);
    }

    private void deliver(Uri[] result) {
        if (filePathCallback != null) {
            filePathCallback.onReceiveValue(result);
            filePathCallback = null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_CAMERA) {
            Uri[] result = null;
            if (resultCode == RESULT_OK && cameraUri != null) {
                result = new Uri[]{cameraUri};
            }
            cameraUri = null;
            deliver(result);
        } else if (requestCode == REQ_FILE) {
            deliver(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
        }
    }

    /** Back key: the web app closes the photo viewer / goes back to the Entry tab first, then the app exits. */
    @Override
    public void onBackPressed() {
        webView.evaluateJavascript("(function(){return !!(window.__back && window.__back());})()", value -> {
            if (!"true".equals(value)) {
                finish();
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.removeJavascriptInterface("AndroidBridge");
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }

    private void cleanCameraCache() {
        File[] old = new File(getCacheDir(), "camera").listFiles();
        if (old != null) {
            for (File f : old) {
                //noinspection ResultOfMethodCallIgnored
                f.delete();
            }
        }
    }

    /**
     * Lets the web app save Excel and backup files into Downloads/CarnetCT without any permission.
     * Called from JavaScript in this order: begin(name, mime), chunk(base64) many times, end().
     */
    private class Bridge {
        private OutputStream out;
        private Uri uri;

        @JavascriptInterface
        public synchronized boolean begin(String filename, String mime) {
            abortInternal();
            try {
                String name = filename == null ? "file" : filename.replaceAll("[^A-Za-z0-9._ -]", "_");
                if (name.isEmpty()) {
                    name = "file";
                }
                ContentValues v = new ContentValues();
                v.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
                v.put(MediaStore.MediaColumns.MIME_TYPE, mime == null || mime.isEmpty() ? "application/octet-stream" : mime);
                v.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/CarnetCT");
                v.put(MediaStore.MediaColumns.IS_PENDING, 1);
                uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
                if (uri == null) {
                    return false;
                }
                out = getContentResolver().openOutputStream(uri);
                return out != null;
            } catch (Exception e) {
                abortInternal();
                return false;
            }
        }

        @JavascriptInterface
        public synchronized boolean chunk(String base64) {
            if (out == null) {
                return false;
            }
            try {
                out.write(Base64.decode(base64, Base64.DEFAULT));
                return true;
            } catch (Exception e) {
                abortInternal();
                return false;
            }
        }

        /** Returns the saved location (for example "Downloads/CarnetCT/file.zip") or "" on failure. */
        @JavascriptInterface
        public synchronized String end() {
            if (out == null || uri == null) {
                return "";
            }
            try {
                out.flush();
                out.close();
                out = null;
                ContentValues v = new ContentValues();
                v.put(MediaStore.MediaColumns.IS_PENDING, 0);
                getContentResolver().update(uri, v, null, null);
                String shown = "Downloads/CarnetCT";
                try (Cursor c = getContentResolver().query(uri, new String[]{MediaStore.MediaColumns.DISPLAY_NAME}, null, null, null)) {
                    if (c != null && c.moveToFirst()) {
                        shown = "Downloads/CarnetCT/" + c.getString(0);
                    }
                } catch (Exception ignored) {
                    // the file is saved; the name is only for the message
                }
                uri = null;
                return shown;
            } catch (Exception e) {
                abortInternal();
                return "";
            }
        }

        @JavascriptInterface
        public synchronized void abort() {
            abortInternal();
        }

        private void abortInternal() {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ignored) {
                    // nothing to do
                }
                out = null;
            }
            if (uri != null) {
                try {
                    getContentResolver().delete(uri, null, null);
                } catch (Exception ignored) {
                    // nothing to do
                }
                uri = null;
            }
        }
    }
}
