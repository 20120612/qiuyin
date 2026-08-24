package com.qiuyin.pet

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import java.util.Calendar

class AppDetector(private val context: Context) {

interface PetCallback {
        fun onAppChanged(packageName: String, label: String)
    }

private val handler = Handler(Looper.getMainLooper())
    private var lastPackage: String = ""
    private var lastChangeTime: Long = 0L
    private var callback: PetCallback? = null

private val pollRunnable = object : Runnable {
        override fun run() {
            checkForegroundApp()
            handler.postDelayed(this, 3000)
        }
    }

fun start(cb: PetCallback) {
        this.callback = cb
        handler.postDelayed(pollRunnable, 3000)
    }

fun stop() {
        handler.removeCallbacks(pollRunnable)
        callback = null
    }

private fun checkForegroundApp() {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val endTime = now
        val beginTime = now - 10000

val events = usm.queryEvents(beginTime, endTime)
        var currentPkg = lastPackage
        val event = android.app.usage.UsageEvents.Event()

while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND) {
                currentPkg = event.packageName
            }
        }

if (currentPkg.isNotEmpty() && currentPkg != lastPackage) {
            val nowTime = System.currentTimeMillis()
            if (nowTime - lastChangeTime > 15000) {
                lastChangeTime = nowTime
                lastPackage = currentPkg
                val label = getLabel(currentPkg)
                callback?.onAppChanged(currentPkg, label)
            }
        }
    }

private fun getLabel(pkg: String): String {
        val pm = context.packageManager
        return try {
            val appInfo = pm.getApplicationInfo(pkg, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            pkg
        }
    }
}
