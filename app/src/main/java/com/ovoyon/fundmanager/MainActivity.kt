package com.ovoyon.fundmanager

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
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
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar

const val OVOYON_APP_URL = "https://script.google.com/macros/s/AKfycbyeBpFjNBIW5h7WxMS-B0kQ5rTazMboP9fw-3fsxfh-PZilIZGM-uvq3mJuLB7rLwlB/exec"
const val OVOYON_CHANNEL_ID = "ovoyon_daily_channel"

fun scheduleDaily8PmReminder(context: Context) {
    val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, DailyReminderReceiver::class.java)
    val pi = PendingIntent.getBroadcast(
        context, 800, intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 20)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        if (timeInMillis <= System.currentTimeMillis()) {
            add(Calendar.DAY_OF_YEAR, 1)
        }
    }
    alarmMgr.setInexactRepeating(
        AlarmManager.RTC_WAKEUP,
        cal.timeInMillis,
        AlarmManager.INTERVAL_DAY,
        pi
    )
}

class DailyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            scheduleDaily8PmReminder(context)
            return
        }
        val pendingResult = goAsync()
        Thread {
            try {
                createChannel(context)
                showNotif(
                    context, 1001,
                    "📢 অভয়ন সমিতি — রাত ৮টার WhatsApp আপডেট",
                    "আজকের তহবিলের মেসেজ WhatsApp গ্রুপে পাঠানোর সময় হয়েছে! ট্যাপ করে পাঠান।"
                )
                val conn = (URL("$OVOYON_APP_URL?action=status").openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = true
                    connectTimeout = 10000
                    readTimeout = 10000
                }
                val body = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(body)
                val redCount = json.optInt("redAlertCount", 0)
                if (redCount > 0) {
                    val arr = json.optJSONArray("redNames")
                    val names = mutableListOf<String>()
                    if (arr != null) {
                        for (i in 0 until arr.length()) names.add(arr.getString(i))
                    }
                    showNotif(
                        context, 1002,
                        "🚨 ৭+ দিন বকেয়া সতর্কতা ($redCount জন)",
                        "${names.joinToString(", ")}-এর ৭+ দিন বকেয়া! WhatsApp-এ তাগাদা দিতে ট্যাপ করুন।"
                    )
                }
            } catch (_: Exception) {
            } finally {
                pendingResult.finish()
            }
        }.start()
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                OVOYON_CHANNEL_ID,
                "অভয়ন দৈনিক রিমাইন্ডার",
                NotificationManager.IMPORTANCE_HIGH
            )
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(ch)
        }
    }

    private fun showNotif(context: Context, id: Int, title: String, text: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            context, id, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notif = NotificationCompat.Builder(context, OVOYON_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        NotificationManagerCompat.from(context).notify(id, notif)
    }
}

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var splashOverlay: LinearLayout
    private var revealed = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
        }
        scheduleDaily8PmReminder(this)

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

        webView.loadUrl(OVOYON_APP_URL)
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
