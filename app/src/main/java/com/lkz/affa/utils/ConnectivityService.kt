package com.lkz.affa.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

class ConnectivityService {
    companion object {
        /**
         * Checks if there's an active internet connection by attempting a 
         * socket connection to Google's DNS server (8.8.8.8:53).
         */
        suspend fun hasInternetConnection(): Boolean = withContext(Dispatchers.IO) {
            try {
                val socket = Socket()
                socket.connect(InetSocketAddress("8.8.8.8", 53), 2000)
                socket.close()
                true
            } catch (e: Exception) {
                false
            }
        }
    }
}
