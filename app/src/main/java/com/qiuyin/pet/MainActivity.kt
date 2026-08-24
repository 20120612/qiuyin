package com.qiuyin.pet

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var statusText: TextView
    private lateinit var permBtn: Button
    private lateinit var usageBtn: Button
    private lateinit var startBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        statusText = findViewById(R.id.statusText)
        permBtn = findViewById(R.id.permButton)
        usageBtn = findViewById(R.id.usageButton)
        startBtn = findViewById(R.id.startButton)

        permBtn.setOnClickListener {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.net.Uri.parse("package:$packageName"))
            startActivity(intent)
        }

        usageBtn.setOnClickListener {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }

        startBtn.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "请先开启悬浮窗权限", Toast.LENGTH_SHORT).show()
            } else if (!hasUsageAccess()) {
                Toast.makeText(this, "请先开启使用情况访问权限", Toast.LENGTH_SHORT).show()
            } else {
                val intent = Intent(this, OverlayService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
                Toast.makeText(this, "秋隐启动了！", Toast.LENGTH_SHORT).show()
            }
        }

        checkAndUpdateStatus()
    }

    override fun onResume() {
        super.onResume()
        checkAndUpdateStatus()
    }

    private fun checkAndUpdateStatus() {
        val overlay = Settings.canDrawOverlays(this)
        val usage = hasUsageAccess()
        val msg = "悬浮窗权限：${if (overlay) "✔ 已开启" else "✘ 未开启"}\n使用情况权限：${if (usage) "✔ 已开启" else "✘ 未开启"}"
        statusText.text = msg
    }

    private fun hasUsageAccess(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }
}
