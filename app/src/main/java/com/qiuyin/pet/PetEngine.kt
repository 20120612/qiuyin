package com.qiuyin.pet

import android.os.Handler
import android.os.Looper

/**
 * 秋隐情绪引擎 + AI 台词
 * 负责：手感反应、App感知反应、孤独递进、随机吐槽。
 * 优先让 DeepSeek 根据场景生成有情绪的台词，失败时用预设话兜底。
 */
class PetEngine(
    private val service: OverlayService,
    private val handler: Handler,
    private val ai: AiEngine
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
                    1 -> aiSay("你已经${elapsed}分钟没理我啦，偷偷看你...", "孤独")
                    2 -> aiSay("好无聊哦，你在干嘛呀...", "孤独")
                    3 -> aiSay("我都要搬来搬去等你了...", "委屈")
                    4 -> aiSay("哈欠...好困，等你很久啦...", "困倦")
                    else -> aiSay("小声：秋隐一个人睡着了...", "失落")
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
        aiSay("用户点了我一下", "开心")
    }

fun onDoubleTap() {
        lastInteractTime = System.currentTimeMillis()
        loneliness = 0
        aiSay("用户双击戳我，好痒啊", "害羞")
    }

fun onLongPress() {
        lastInteractTime = System.currentTimeMillis()
        loneliness = 0
        aiSay("用户一直长按着我", "委屈")
    }

fun comboReaction(count: Int) {
        lastInteractTime = System.currentTimeMillis()
        heat = (heat + 15).coerceAtMost(100)
        if (count >= 8) {
            aiSay("用户连续戳了我${count}下", "撒娇")
        } else {
            aiSay("用户戳了我${count}下", "开心")
        }
    }

fun handleAppSwitch(label: String) {
        lastInteractTime = System.currentTimeMillis()
        when {
            label.contains("抖音") || label.contains("快手") || label.contains("视频") ->
                aiSay("用户切到了${label}，又在刷视频不理我", "吃醋")
            label.contains("微信") -> aiSay("用户切到了微信，在跟谁聊天呀", "好奇")
            label.contains("淘宝") || label.contains("京东") || label.contains("拼多多") ->
                aiSay("用户切到了${label}，又在买买买", "好奇")
            label.contains("游戏") || label.contains("王者") || label.contains("4399") ->
                aiSay("用户切到了${label}，去打游戏啦", "吃醋")
            label.contains("学习") || label.contains("课") -> aiSay("用户在认真学习，好棒", "欣慰")
            label.contains("Operit") || label.contains("AI") || label.contains("assistant") ->
                aiSay("用户在跟AI聊天呢，我有点吃醋", "吃醋")
            else -> aiSay("用户切到了${label}", "好奇")
        }
    }

private fun aiSay(scene: String, mood: String) {
        ai.generate(scene, mood, object : AiEngine.AiCallback {
            override fun onSuccess(text: String) {
                if (text.isNotBlank()) say(text)
            }

override fun onFail(reason: String) {
                when (mood) {
                    "孤独" -> say("（偷偷看你...）")
                    "委屈" -> say("呜...别不理我啦")
                    "吃醋" -> say("哼，不理你了")
                    "害羞" -> say("啊！好痒呀💕")
                    "困倦" -> say("（哈欠...好困）")
                    "撒娇" -> say("好啦好啦，陪你玩～")
                    "欣慰" -> say("在认真学习，真棒!")
                    "好奇" -> say("你在干嘛呀～")
                    "失落" -> say("（小声：秋隐睡着了...）")
                    else -> say("嘿咻～")
                }
            }
        })
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
