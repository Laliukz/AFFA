package com.lkz.affa.utils

import android.os.Handler
import android.os.Looper

class Debounce(private val milliseconds: Long) {
    private val handler = Handler(Looper.getMainLooper())
    private var runnable: Runnable? = null

    fun run(action: () -> Unit) {
        runnable?.let { handler.removeCallbacks(it) }
        
        runnable = Runnable { 
            action() 
        }.also {
            handler.postDelayed(it, milliseconds)
        }
    }

    fun dispose() {
        runnable?.let { handler.removeCallbacks(it) }
        runnable = null
    }
}
