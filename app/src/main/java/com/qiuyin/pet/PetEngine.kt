package com.qiuyin.pet

import android.os.Handler
import android.os.Looper

class PetEngine(
    private val service: OverlayService,
    private val handler: Handler
) {

    private var loneliness = 0
    private var lastInteractTime = System.currentTimeMillis()
    private var heat = 0

    private val idleRunnable = object : Runnable {
        override fun run() {
            val elapsed = (System.currentTimeMillis() - lastInteractTime) / 60000
            when {
                elapsed >= 30 -> loneliness = 5
                elapsed >= 20 -> loneliness = 4
                elapsed >= 15 -> loneliness = 3
                elapsed >= 10 -> loneliness = 2
                elapsed >= 5 -> loneliness = 1
            }
            if (Math.random() < 0.35) {
                when (loneliness) {
                    0 -> randomIdleTalk()
                    1 -> say("（偷偷看你...）")
                    2 -> say("（吹泡泡中...）")
                    3 -> say("（搬来搬去...）")
                    4 -> say("（哈欠...好困）")
                    else -> say("（小声：秋隐睡着了...）")
                }
            }
            handler.postDelayed(this, 8000)
        }
    }

    fun startIdleLoop() {
        handler.postDelayed(idleRunnable, 8000)
    }

    fun stop() {
        handler.removeCallbacks(idleRunnable)
    }

    private fun randomIdleTalk() {
        val lines = arrayOf(
            "你在干嘛呀～",
            "陪陪我嘛～",
            "我在这里哦～",
            "今天也喜欢你💕",
            "好无聊哦...",
            "戳我一下嘛！"
        )
        say(lines[(Math.random() * lines.size).toInt()])
    }

    fun onTap() {
        lastInteractTime = System.currentTimeMillis()
        loneliness = 0
        heat = (heat + 5).coerceAtMost(100)
        val lines = arrayOf("嘿咻～", "别戳我啦", "要抱抱", "嘻嘻~", "干嘛戳我呀")
        say(lines[(Math.random() * lines.size).toInt()])
    }

    fun onDoubleTap() {
        lastInteractTime = System.currentTimeMillis()
        loneliness = 0
        say("啊！好痒呀💕")
    }

    fun onLongPress() {
        lastInteractTime = System.currentTimeMillis()
        loneliness = 0
        say("呜...你一直长按我")
    }

    fun comboReaction(count: Int) {
        lastInteractTime = System.currentTimeMillis()
        heat = (heat + 15).coerceAtMost(100)
        when (count) {
            5 -> say("再戳我可要生气了哦!")
            8 -> say("好啦好啦，陪你玩～")
            else -> say("你戳我好几下啦!")
        }
    }

    fun handleAppSwitch(label: String) {
        lastInteractTime = System.currentTimeMillis()
        when {
            label.contains("抖音") || label.contains("快手") || label.contains("视频") ->
                say("又在刷视频，不理我啦?")
            label.contains("微信") -> say("在跟谁聊天呢～")
            label.contains("淘宝") || label.contains("京东") || label.contains("拼多多") ->
                say("又在买买买，要我帮你挑吗?")
            label.contains("游戏") || label.contains("王者") || label.contains("4399") ->
                say("又在打游戏，带我一起嘛～")
            label.contains("学习") || label.contains("课") -> say("在认真学习，真棒!")
            else -> say("切到「${label}」啦")
        }
    }

    private fun say(text: String) {
        val escaped = text.replace("\\", "\\\\").replace("'", "\\'")
        handler.post {
            try {
                val js = "window.onPetSay && window.onPetSay('$escaped');"
                service.evaluateJavascript(js)
            } catch (e: Exception) {
            }
        }
    }
}
