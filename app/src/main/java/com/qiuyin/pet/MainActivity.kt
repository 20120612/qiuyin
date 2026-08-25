package com.qiuyin.pet

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * 秋隐引导页
 * 检查悬浮窗权限、启动悬浮窗服务
 */
class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var permButton: Button
    private lateinit var usageButton: Button
    private lateinit var startButton: Button
    private lateinit var apiKeyInput: EditText
    private lateinit var saveKeyButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        permButton = findViewById(R.id.permButton)
        usageButton = findViewById(R.id.usageButton)
        startButton = findViewById(R.id.startButton)
        apiKeyInput = findViewById(R.id.apiKeyInput)
        saveKeyButton = findViewById(R.id.saveKeyButton)

        // 读取已保存的 key
        val savedKey = getSharedPreferences("qiuyin_prefs", MODE_PRIVATE)
            .getString("deepseek_api_key", "") ?: ""
        apiKeyInput.setText(savedKey)

        saveKeyButton.setOnClickListener {
            val key = apiKeyInput.text.toString().trim()
            getSharedPreferences("qiuyin_prefs", MODE_PRIVATE)
                .edit().putString("deepseek_api_key", key).apply()
            Toast.makeText(this, if (key.isBlank()) "Key已清空，秋隐将用预设话" else "Key保存成功～✨", Toast.LENGTH_SHORT).show()
        }

        permButton.setOnClickListener {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }

        usageButton.setOnClickListener {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            startActivity(intent)
        }

        startButton.setOnClickListener {
            startOverlay()
        }

        // 请求通知权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun updateStatus() {
        val hasOverlay = Settings.canDrawOverlays(this)
        val hasUsage = hasUsageAccess()
        statusText.text = when {
            !hasOverlay -> "⚠️ 请先打开「悬浮窗」权限"
            !hasUsage -> "⚠️ 请先打开「使用情况」权限"
            else -> "✅ 权限都OK，可以启动秋隐啦！"
        }
        startButton.isEnabled = hasOverlay && hasUsage
    }

    private fun hasUsageAccess(): Boolean {
        val appOps = getSystemService(APP_OPS_SERVICE) as android.app.AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), packageName
            )
        }
        return mode == android.app.AppOpsManager.MODE_ALLOWED
    }

    private fun startOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "请先打开悬浮窗权限", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(this, OverlayService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        Toast.makeText(this, "秋隐出来啦！快去看看吧～", Toast.LENGTH_SHORT).show()
        // 缩小窗口，让秋隐可见
        moveTaskToBack(true)
    }
}
