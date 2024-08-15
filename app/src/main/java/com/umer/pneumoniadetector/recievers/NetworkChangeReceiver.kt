package com.umer.pneumoniadetector.recievers

import com.umer.pneumoniadetector.listeners.OnInternetStateChanged
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

class NetworkChangeReceiver(private val onInternetStateChanged: OnInternetStateChanged) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        context?.let {
            val connectivityManager = it.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            val isConnected = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            handleConnectionChange(isConnected)
        }
    }

    private fun handleConnectionChange(isConnected: Boolean) {
        if (isConnected) {
            onInternetStateChanged.onConnected()
        } else {
            onInternetStateChanged.onDisconnected()
        }
    }
}
