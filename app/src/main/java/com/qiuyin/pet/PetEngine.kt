package com.qiuyin.pet

import android.os.Handler
import android.os.Looper
import android.webkit.WebView

class PetEngine(
    private val service: OverlayService,
    private val handler: Handler
) {

private var heat: Int = 0
    private var lastInteractTime: Long = System.currentTimeMillis()
    private var idleLoopRunnable: Runnable? = null

fun startIdleLoop() {
        val runnable = object : Runnable {
            override fun run() {
                checkIdle()
                handler.postDelayed(this, 3000)
            }
        }
        idleLoopRunnable = runnable
        handler.postDelayed(runnable, 3000)
    }

fun stop() {
        idleLoopRunnable?.let { handler.removeCallbacks(it) }
        idleLoopRunnable = null
    }

fun onInteract(type: String) {
        lastInteractTime = System.currentTimeMillis()
        when (type) {
            "tap" -> {
                heat = (heat + 5).coerceAtMost(100)
                say(reactions0
            }
            "doubleTap" -> {
                heat = (heat + 15).coerceAtMost(100)
                say(reactions1
            }
            "longPress" -> say(reactions2
            "combo" -> say(reactions3
        }
    }

fun handleAppSwitch(packageName: String, label: String) {
        heat = (heat + 2).coerceAtMost(100)
        say(getAppReaction(packageName, label))
    }

private fun checkIdle() {
        val elapsedMin = (System.currentTimeMillis() - lastInteractTime) / 60000
        when {
            elapsedMin >= 30 -> say(idleReactions5
            elapsedMin >= 20 -> say(idleReactions4
            elapsedMin >= 15 -> say(idleReactions3
            elapsedMin >= 10 -> say(idleReactions2
            elapsedMin >= 5 -> say(idleReactions1
        }
    }

private fun say(text: String) {
        try {
            val js = "window.onPetSay('text');"
            handler.post {
                service.evaluateJavascript(js)
            }
        } catch (e: Exception) {
            // ignore
        }
    }

    private fun getAppReaction(pkg: String, label: String): String {
        return when {
            pkg.contains("douyin") || pkg.contains("kuaishou") || pkg.contains("tiktok") ->
                "又在刷视频，不理我啦?"
            pkg.contains("wechat") || pkg.contains("tencent.mm") ->
                "在跟谁聊天呢～"
            pkg.contains("taobao") || pkg.contains("jd") || pkg.contains("pinduoduo") ->
                "又在买买买，要我帮你挑吗?"
            pkg.contains("game") || pkg.contains("wangzhe") || pkg.contains("4399") ->
                "又在打游戏，带我一起嘛～"
            pkg.contains("study") || pkg.contains("class") || pkg.contains("edu") || pkg.contains("course") ->
                "在认真学习，真棒!"
            else -> "切到「label」啦"
        }
    }

companion object {
        private val reactions = listOf(
            "嗯哼～",
            "哈！摸我两次，好痒！",
            "别戳我啦～",
            "再戳我就生气啦！"
        )
        private val idleReactions = listOf(
            "好安静啊～",
            "你在干嘛呀…",
            "哼，玩别的去了？",
            "我…我有点无聊了",
            "喂喂，理理我嘛",
            "再不陪我，我要睡着啦…"
        )
    }
}
