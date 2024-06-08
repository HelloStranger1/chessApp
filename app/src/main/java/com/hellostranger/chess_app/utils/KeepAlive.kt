package com.hellostranger.chess_app.utils

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import com.hellostranger.chess_app.network.retrofit.backend.BackendRetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@ExperimentalUnsignedTypes
/**
 * This is a service that sends keep alive requests to the server every minute.
 */
class KeepAlive : Service() {
    private val handler =  Handler(Looper.getMainLooper()) // Handler
    private val userEmail
        get() = MyApp.tokenManager.getUserEmail() // The user email
    private val coroutineScope = CoroutineScope(Dispatchers.IO) // The scope for requests
    companion object {
         var isRunning = false
    }


    private val runnable = object : Runnable{
        override fun run() {
            Log.i("KeepAlive", "Running")
            coroutineScope.launch {
                BackendRetrofitClient.instance.keepAlive(userEmail)
            }
            handler.postDelayed(this, 60000) // Wait 1 minutes
        }
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if(!isRunning){
            handler.post(runnable)
            isRunning = true
        }
        return START_STICKY
    }
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(runnable)
        isRunning = false

    }
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}