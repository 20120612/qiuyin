package com.qiuyin.pet

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.JavascriptInterface

class OverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private lateinit var webView: WebView
    private lateinit var petEngine: PetEngine

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        startForeground(1, buildNotification())

        overlayView = LayoutInflater.from(this).inflate(R.layout.activity_main, null)
        webView = WebView(this)
        webView.setBackgroundColor(Color.TRANSPARENT)
        webView.settings.javaScriptEnabled = true
        webView.settings.allowFileAccess = true
        webView.loadUrl("file:///android_asset/pet.html")

        petEngine = PetEngine(this, webView)
        petEngine.start()

        addOverlay()
    }

    private fun addOverlay() {
        val params = WindowManager.LayoutParams(
            (160 * resources.displayMetrics.density).toInt(),
            (200 * resources.displayMetrics.density).toInt(),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.LEFT
        params.x = 40
        params.y = 200

        windowManager.addView(webView, params)

        webView.setOnTouchListener(object : View.OnTouchListener {
            var downX = 0f
            var downY = 0f
            var startX = 0f
            var startY = 0f
            var isMoving = false

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        downX = event.rawX
                        downY = event.rawY
                        startX = params.x.toFloat()
                        startY = params.y.toFloat()
                        isMoving = false
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - downX
                        val dy = event.rawY - downY
                        if (dx > 10 || dy > 10) isMoving = true
                        params.x = (startX + dx).toInt()
                        params.y = (startY + dy).toInt()
                        if (params.x < 0) params.x = 0
                        if (params.y < 0) params.y = 0
                        windowManager.updateViewLayout(v, params)
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!isMoving) {
                            petEngine.onTap()
                        }
                    }
                }
                return true
            }
        })
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("qiuyin", "秋隐", NotificationManager.IMPORTANCE_LOW)
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        return Notification.Builder(this, "qiuyin")
            .setContentTitle("秋隐")
            .setContentText("正在陪你哦")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingIntent)
            .build()
    }
}
