package com.qiuyin.pet

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper

/**
 * 前台App感知器
 * 每3秒轮询前台应用，切换时回调
 */
class AppDetector(private val context: Context) {

    interface PetCallback {
        fun onAppChanged(packageName: String, label: String)
    }

    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false
    private var lastPkg = ""
    private var callback: PetCallback? = null
    private var lastChangeTime = 0L

    private val pollRunnable = object : Runnable {
        override fun run() {
            val pkg = getForegroundPackage()
            if (pkg.isNotEmpty() && pkg != lastPkg) {
                // 15秒冷却，避免快速切app误触发
                val now = System.currentTimeMillis()
                if (now - lastChangeTime > 15000) {
                    lastPkg = pkg
                    lastChangeTime = now
                    callback?.onAppChanged(pkg, getLabel(pkg))
                }
            }
            if (isRunning) handler.postDelayed(this, 3000)
        }
    }

    fun start(cb: PetCallback) {
        callback = cb
        isRunning = true
        handler.post(pollRunnable)
    }

    fun stop() {
        isRunning = false
        handler.removeCallbacks(pollRunnable)
    }

    private fun getForegroundPackage(): String {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val end = System.currentTimeMillis()
        val begin = end - 10000
        val events = usm.queryEvents(begin, end)
        var lastPkg = ""
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND ||
                event.eventType == UsageEvents.Event.ACTIVITY_RESUMED
            ) {
                lastPkg = event.packageName
            }
        }
        return lastPkg
    }

    private fun getLabel(pkg: String): String {
        return try {
            val pm = context.packageManager
            val ai = pm.getApplicationInfo(pkg, PackageManager.GET_META_DATA)
            val label = pm.getApplicationLabel(ai).toString()
            // 若拿到了包名而不是应用名（部分ROM），尝试用包名映射为常用中文名
            if (label == pkg) {
                friendlyName(pkg) ?: pkg
            } else {
                label
            }
        } catch (e: Exception) {
            friendlyName(pkg) ?: pkg
        }
    }

    /** 常见应用包名 -> 中文名映射，兜底显示 */
    private fun friendlyName(pkg: String): String? {
        return when (pkg) {
            "com.tencent.mm" -> "微信"
            "com.tencent.mobileqq" -> "QQ"
            "com.ss.android.ugc.aweme" -> "抖音"
            "com.ss.android.ugc.aweme.lite" -> "抖音极速版"
            "com.smile.gifmaker" -> "快手"
            "com.kuaishou.nebula" -> "快手极速版"
            "com.taobao.taobao" -> "淘宝"
            "com.jingdong.app.mall" -> "京东"
            "com.xunmeng.pinduoduo" -> "拼多多"
            "com.taobao.tmall" -> "天猫"
            "com.youku.phone" -> "优酷"
            "tv.danmaku.bili" -> "哔哩哔哩"
            "com.huawei.appmarket" -> "华为应用市场"
            "com.xiaomi.market" -> "小米应用商店"
            "com.autonavi.minimap" -> "高德地图"
            "com.baidu.BaiduMap" -> "百度地图"
            "com.eg.android.AlipayGphone" -> "支付宝"
            "com.github.android" -> "GitHub"
            "com.alibaba.android.rimet" -> "钉钉"
            "com.tencent.wework" -> "企业微信"
            "com.tencent.qqlive" -> "腾讯视频"
            "com.qiyi.video" -> "爱奇艺"
            else -> null
        }
    }
}
