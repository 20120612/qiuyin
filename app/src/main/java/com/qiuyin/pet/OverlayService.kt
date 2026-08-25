package com.qiuyin.pet

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.*
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebSettings
import android.widget.Toast
import androidx.core.app.NotificationCompat

/**
 * 秋隐悬浮桌宠核心服务
 * 悬浮窗 + WebView 渲染 + 前台App感知 + 情绪
 * 触摸事件交给网页处理（点击弹字+动），感知/情绪由 Kotlin 通过 window.onPetSay 弹字
 */
class OverlayService : Service() {

private var windowManager: WindowManager? = null
    private var overlayView: WebView? = null
    private var params: WindowManager.LayoutParams? = null
    private val handler = Handler(Looper.getMainLooper())

private val appDetector by lazy { AppDetector(this) }
    private val petEngine by lazy { PetEngine(this, handler) }

companion object {
        private const val CHANNEL_ID = "qiuyin_overlay_channel"
        private const val NOTIFICATION_ID = 1001
        private const val PET_SIZE_DP = 160
        private const val PET_HEIGHT_DP = 200
    }

override fun onBind(intent: Intent?): IBinder? = null

override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "请先在设置里允许秋隐的通知权限", Toast.LENGTH_LONG).show()
            stopSelf()
            return
        }
        createNotificationChannel()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    buildNotification("秋隐蹲在角落偷偷看你..."),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID, buildNotification("秋隐蹲在角落偷偷看你..."))
            }
        } catch (e: Exception) {
            Toast.makeText(this, "启动失败: ${e.message}", Toast.LENGTH_LONG).show()
            stopSelf()
            return
        }
        if (!android.provider.Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "请先在设置里打开悬浮窗权限", Toast.LENGTH_LONG).show()
            stopSelf()
            return
        }
        try {
            setupOverlay()
        } catch (e: Exception) {
            Toast.makeText(this, "悬浮窗创建失败: ${e.message}", Toast.LENGTH_LONG).show()
            stopSelf()
            return
        }
        Toast.makeText(this, "秋隐已启动!", Toast.LENGTH_SHORT).show()
        appDetector.start(object : AppDetector.PetCallback {
            override fun onAppChanged(packageName: String, label: String) {
                petEngine.handleAppSwitch(label)
            }
        })
        petEngine.startIdleLoop()
    }

private fun setupOverlay() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

// 标准悬浮窗配置：NOT_FOCUSABLE + NOT_TOUCH_MODAL，红米稳定不崩
        params = WindowManager.LayoutParams(
            dpToPx(PET_SIZE_DP),
            dpToPx(PET_HEIGHT_DP),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 30
            y = 260
        }

overlayView = WebView(this).apply {
            setBackgroundColor(0x00000000)
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true
                cacheMode = WebSettings.LOAD_NO_CACHE
            }
            webViewClient = WebViewClient()
            loadUrl("file:///android_asset/pet.html")
        }

windowManager?.addView(overlayView, params)
    }

// === 通知栏 ===

private fun buildNotification(text: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("秋隐 🐾")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "秋隐",
                NotificationManager.IMPORTANCE_LOW
            ).apply { setShowBadge(false) }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

// === 工具 ===

fun evaluateJavascript(js: String) {
        overlayView?.evaluateJavascript(js, null)
    }

private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

override fun onDestroy() {
        appDetector.stop()
        petEngine.stop()
        handler.removeCallbacksAndMessages(null)
        overlayView?.let {
            windowManager?.removeView(it)
            it.destroy()
        }
        overlayView = null
        super.onDestroy()
    }
}
