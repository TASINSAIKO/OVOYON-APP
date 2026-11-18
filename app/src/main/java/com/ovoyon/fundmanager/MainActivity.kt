package com.ovoyon.fundmanager

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var splashOverlay: LinearLayout
    private val appUrl = "https://script.google.com/macros/s/AKfycbyeBpFjNBIW5h7WxMS-B0kQ5rTazMboP9fw-3fsxfh-PZilIZGM-uvq3mJuLB7rLwlB/exec"
    private var revealed = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#19132B"))
        }

        webView = WebView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#19132B"))
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            overScrollMode = View.OVER_SCROLL_NEVER
            alpha = 0f
        }

        splashOverlay = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#19132B"))
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            val title = TextView(this@MainActivity).apply {
                text = "অভয়ন কল্যাণ সমিতি"
                textSize = 22f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 24)
            }
            val sub = TextView(this@MainActivity).apply {
                text = "Fund Manager লোড হচ্ছে..."
                textSize = 13f
                setTextColor(Color.parseColor("#CAC4E1"))
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 32)
            }
            val spinner = ProgressBar(this@MainActivity).apply {
                isIndeterminate = true
            }
            addView(title)
            addView(sub)
            addView(spinner)
        }

        root.addView(webView)
        root.addView(splashOverlay)
        setContentView(root)

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
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
                      function clean(){
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
                      }
                      clean();
                      var n = 0;
                      var iv = setInterval(function(){ clean(); if (++n > 15) clearInterval(iv); }, 300);
                    })();
                """.trimIndent()
                view?.evaluateJavascript(js, null)

                if (!revealed) {
                    revealed = true
                    Handler(Looper.getMainLooper()).postDelayed({
                        webView.animate().alpha(1f).setDuration(240).start()
                        splashOverlay.animate().alpha(0f).setDuration(240).withEndAction {
                            splashOverlay.visibility = View.GONE
                        }.start()
                    }, 350)
                }
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
