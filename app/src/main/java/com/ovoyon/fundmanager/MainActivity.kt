package com.ovoyon.fundmanager

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private val appUrl = "https://script.google.com/macros/s/AKfycbyeBpFjNBIW5h7WxMS-B0kQ5rTazMboP9fw-3fsxfh-PZilIZGM-uvq3mJuLB7rLwlB/exec"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)
        webView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        webView.setBackgroundColor(Color.parseColor("#19132B"))
        setContentView(webView)

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            useWideViewPort = false
            loadWithOverviewMode = false
            builtInZoomControls = false
            displayZoomControls = false
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            userAgentString = userAgentString.replace("; wv", "")
        }

        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false
                if (url.startsWith("https://wa.me") ||
                    url.startsWith("https://api.whatsapp.com") ||
                    url.startsWith("whatsapp:") ||
                    url.startsWith("mailto:") ||
                    url.startsWith("tel:")
                ) {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    return true
                }
                return false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                val js = """
                    (function(){
                      var tries = 0;
                      var t = setInterval(function(){
                        tries++;
                        var all = document.querySelectorAll('div, table, tr');
                        for (var i = 0; i < all.length; i++) {
                          var txt = (all[i].innerText || '');
                          if (txt.indexOf('Google Apps Script') !== -1 && all[i].offsetHeight < 90) {
                            all[i].style.display = 'none';
                          }
                        }
                        var f = document.getElementById('sandboxFrame');
                        if (f) {
                          f.style.height = '100vh';
                          f.style.minHeight = '100vh';
                          f.style.width = '100%';
                          if (f.parentElement) f.parentElement.style.height = '100vh';
                        }
                        if (tries > 15) clearInterval(t);
                      }, 400);
                    })();
                """.trimIndent()
                view?.evaluateJavascript(js, null)
            }
        }

        webView.loadUrl(appUrl)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
