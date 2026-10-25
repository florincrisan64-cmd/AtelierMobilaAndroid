package ro.ateliermobila.app;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private WebView webView;
    private WebView printWebView;
    private String pendingPrintHtml = "";

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(23, 32, 43));
        getWindow().setNavigationBarColor(Color.BLACK);
        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(244, 245, 247));
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setSupportMultipleWindows(false);
        webView.addJavascriptInterface(new AndroidBridge(), "AndroidBridge");
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // Replace browser-only pop-up printing with Android's native print sheet.
                String js = "(function(){window.open=function(){var html='';return {document:{open:function(){html='';},write:function(s){html+=s;},close:function(){AndroidBridge.printHtml(html);}}};};})();";
                view.evaluateJavascript(js, null);
            }
        });
        setContentView(webView);
        webView.loadUrl("file:///android_asset/index.html");
    }

    public class AndroidBridge {
        @JavascriptInterface public void saveFile(String filename, String mimeType, String content) {
            runOnUiThread(() -> {
                OutputStream out = null;
                try {
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Downloads.DISPLAY_NAME, filename);
                    values.put(MediaStore.Downloads.MIME_TYPE, mimeType == null ? "text/plain" : mimeType);
                    values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/AtelierMobila");
                    Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                    if (uri == null) throw new Exception("Nu s-a putut crea fișierul");
                    out = getContentResolver().openOutputStream(uri);
                    if (out == null) throw new Exception("Nu s-a putut scrie fișierul");
                    out.write((content == null ? "" : content).getBytes(StandardCharsets.UTF_8));
                    out.flush();
                    Toast.makeText(MainActivity.this, "Salvat în Downloads/AtelierMobila: " + filename, Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Eroare la salvare: " + e.getMessage(), Toast.LENGTH_LONG).show();
                } finally {
                    try { if (out != null) out.close(); } catch (Exception ignored) {}
                }
            });
        }

        @JavascriptInterface public void printHtml(String html) {
            pendingPrintHtml = html == null ? "" : html;
            runOnUiThread(() -> startNativePrint(pendingPrintHtml));
        }
    }

    private void startNativePrint(String html) {
        printWebView = new WebView(this);
        printWebView.getSettings().setJavaScriptEnabled(true);
        printWebView.addJavascriptInterface(new Object() {
            @JavascriptInterface public void printCurrent() {
                runOnUiThread(() -> {
                    try {
                        PrintManager manager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
                        PrintDocumentAdapter adapter = printWebView.createPrintDocumentAdapter("Atelier-Mobila");
                        manager.print("Atelier Mobilă", adapter, new PrintAttributes.Builder().build());
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, "Nu s-a putut deschide tipărirea: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }
        }, "PrintBridge");
        printWebView.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) {
                view.evaluateJavascript("window.focus=function(){};window.print=function(){PrintBridge.printCurrent();};", null);
            }
        });
        String printHook = "<script>window.focus=function(){};window.print=function(){PrintBridge.printCurrent();};</script>";
        String safeHtml = html.contains("</head>") ? html.replace("</head>", printHook + "</head>") : printHook + html;
        printWebView.loadDataWithBaseURL("file:///android_asset/", safeHtml, "text/html", "UTF-8", null);
    }

    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override protected void onDestroy() {
        if (webView != null) webView.destroy();
        if (printWebView != null) printWebView.destroy();
        super.onDestroy();
    }
}
