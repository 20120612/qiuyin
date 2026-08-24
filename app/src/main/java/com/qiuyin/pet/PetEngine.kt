package com.qiuyin.pet

import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import java.util.Random

class PetEngine(private val context: android.content.Context, private val webView: WebView) {
    private val handler = Handler(Looper.getMainLooper())
    private val random = Random()
    private var heat = 0
    private var loneliness = 0
    private var lastApp = ""

    private val appReactions = mapOf<String, String>(
        "抖音" to "又在刷视频，不理我啦?",
        "快手" to "又在刷视频，不理我啦?",
        "微信" to "在跟谁聊天呢～",
        "淘宝" to "又在买买买",
        "京东" to "又在买买买",
        "王者荣耀" to "又在打游戏，带我一起嘛～",
        "和平精英" to "又在打游戏，带我一起嘛～",
        "哔哩哔哩" to "又在看视频呀",
        "学习" to "在认真学习，真棒!"
    )

    fun start() {
        handler.postDelayed(runAppCheck, 3000)
        handler.postDelayed(runLoneliness, 300000)
    }

    private val runAppCheck = object : Runnable {
        override fun run() {
            handler.postDelayed(this, 10000)
        }
    }

    private val runLoneliness = object : Runnable {
        override fun run() {
            if (loneliness < 5) loneliness++
            val texts = arrayOf("你陪陪我嘛～", "好想你哦", "放我出去看看", "一个人好孤单", "哼，不理你了")
            if (loneliness >= 1) say(texts[loneliness - 1])
            handler.postDelayed(this, 300000 * (loneliness))
        }
    }

    fun onTap() {
        heat += 5
        if (heat > 100) heat = 100
        loneliness = 0
        val texts = arrayOf("嘿嘿，你戳我啦～", "痒痒的！", "别戳啦，好痒～", "你在逗我吗", "开心~")
        say(texts[random.nextInt(texts.size)])
    }

    fun onAppChange(appName: String) {
        if (appName == lastApp) return
        lastApp = appName
        val reaction = appReactions[appName]
        if (reaction != null) {
            say(reaction)
        } else if (random.nextInt(100) < 20) {
            say(randomText())
        }
    }

    private fun randomText(): String {
        val texts = arrayOf("你在忙什么呀", "无聊死了", "带我出去看看嘛", "今天天气怎么样", "你想我了吗")
        return texts[random.nextInt(texts.size)]
    }

    fun say(text: String) {
        handler.post {
            webView.evaluateJavascript("window.onPetSay('$text');", null)
        }
    }
}
