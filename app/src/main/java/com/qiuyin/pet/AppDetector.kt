package com.qiuyin.pet

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.SystemClock

class AppDetector(context: Context) {
    private val context = context.applicationContext
    private val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private var lastPackage = ""
    private var lastNotifyTime = 0L
    private var cooldownMs = 15000L

    fun start(onAppChange: (String) -> Unit) {
        Thread {
            while (true) {
                val pkg = getForegroundPackage()
                if (pkg != null && pkg != lastPackage) {
                    val now = SystemClock.elapsedRealtime()
                    if (now - lastNotifyTime >= cooldownMs) {
                        lastPackage = pkg
                        lastNotifyTime = now
                        val label = getAppLabel(pkg)
                        onAppChange(label)
                    }
                }
                Thread.sleep(3000)
            }
        }.start()
    }

    private fun getForegroundPackage(): String? {
        val end = System.currentTimeMillis()
        val begin = end - 10000
        val events = usm.queryEvents(begin, end)
        var packageName: String? = null
        var lastEvent: UsageEvents.Event? = null
        while (events.hasNextEvent()) {
            val event = UsageEvents.Event()
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                packageName = event.packageName
                lastEvent = event
            }
        }
        return packageName
    }

    private fun getAppLabel(packageName: String): String {
        return try {
            val pm = context.packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
    }
}
