package com.toolbox.nativetoolbox

import android.app.Application
import com.toolbox.nativetoolbox.net.AstroApi

class ToolboxApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AstroApi.init(this)
    }
}
