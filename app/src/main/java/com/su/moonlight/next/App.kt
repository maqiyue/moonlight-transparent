package com.su.moonlight.next

import android.app.Application
import android.content.Context

class App : Application() {
    companion object {
        lateinit var ins: Application
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        ins = this
    }
}