package com.mustafaadsiz.durakbekliyorum;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.view.WindowManager;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.webkit.WebViewAssetLoader;

import java.util.Locale;

public class MainActivity extends Activity {
    private static final String ADRES = "https://appassets.androidplatform.net/assets/index.html";
    private static final int KONUM_IZNI = 7;

    private WebView web;
    private TextToSpeech tts;
    private boolean ttsHazir = false;
    private GeolocationPermissions.Callback bekleyenCb;
    private String bekleyenKaynak;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        web = new WebView(this);
        setContentView(web);

        final WebViewAssetLoader yukleyici = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setGeolocationEnabled(true);
        s.setAllowFileAccess(false);
        s.setMediaPlaybackRequiresUserGesture(false);

        web.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
                return yukleyici.shouldInterceptRequest(r.getUrl());
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onGeolocationPermissionsShowPrompt(String kaynak, GeolocationPermissions.Callback cb) {
                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    cb.invoke(kaynak, true, false);
                } else {
                    bekleyenCb = cb;
                    bekleyenKaynak = kaynak;
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION}, KONUM_IZNI);
                }
            }
        });
        web.addJavascriptInterface(new Kopru(), "Android");

        tts = new TextToSpeech(this, durum -> {
            if (durum == TextToSpeech.SUCCESS) {
                tts.setLanguage(new Locale("tr", "TR"));
                ttsHazir = true;
            }
        });

        web.loadUrl(ADRES);
    }

    @Override
    public void onRequestPermissionsResult(int kod, String[] izinler, int[] sonuclar) {
        if (kod == KONUM_IZNI && bekleyenCb != null) {
            boolean ok = sonuclar.length > 0 && sonuclar[0] == PackageManager.PERMISSION_GRANTED;
            bekleyenCb.invoke(bekleyenKaynak, ok, false);
            bekleyenCb = null;
        }
    }

    @Override
    public void onBackPressed() {
        web.evaluateJavascript("window.androidGeri && window.androidGeri()", sonuc -> {
            if (!"true".equals(sonuc)) MainActivity.super.onBackPressed();
        });
    }

    @Override
    protected void onDestroy() {
        if (tts != null) tts.shutdown();
        web.destroy();
        super.onDestroy();
    }

    /** Web arayüzünün çağırdığı yerel işlevler: sesli uyarı, titreşim, ekranı açık tutma, paylaşma. */
    private class Kopru {
        @JavascriptInterface
        public void speak(String metin) {
            if (ttsHazir) tts.speak(metin, TextToSpeech.QUEUE_FLUSH, null, "uyari");
        }

        @JavascriptInterface
        public void vibrate(long ms) {
            Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (v == null) return;
            if (Build.VERSION.SDK_INT >= 26) v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
            else v.vibrate(ms);
        }

        @JavascriptInterface
        public void keepScreenOn(final boolean acik) {
            runOnUiThread(() -> {
                if (acik) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            });
        }

        @JavascriptInterface
        public void share(String metin) {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_SUBJECT, "Durakta Bekliyorum test kayıtları");
            i.putExtra(Intent.EXTRA_TEXT, metin);
            startActivity(Intent.createChooser(i, "Test kayıtlarını paylaş"));
        }
    }
}
