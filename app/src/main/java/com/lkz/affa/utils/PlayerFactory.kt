package com.lkz.affa.utils

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer

class PlayerFactory {
    companion object {
        fun create(context: Context): ExoPlayer {
            return ExoPlayer.Builder(context).build()
        }
    }
}
