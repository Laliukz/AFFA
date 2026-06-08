package com.lkz.affa

import android.app.Application

class AffaApplication : Application() {
    
    companion object {
        lateinit var appContext: android.content.Context
            private set
    }
    
    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
    }
}
