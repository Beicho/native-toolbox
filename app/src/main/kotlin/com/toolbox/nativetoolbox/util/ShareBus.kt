package com.toolbox.nativetoolbox.util

/** 系统分享进来的文本,由目标工具页一次性消费 */
object ShareBus {
    private var pending: String? = null

    fun post(text: String) {
        pending = text
    }

    fun peek(): String? = pending

    fun consume(): String? {
        val t = pending
        pending = null
        return t
    }
}
