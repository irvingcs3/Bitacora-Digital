package com.example.bitacoradigital.scanner

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class DataWedgeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == "com.symbol.datawedge.data") {
            val scanned = intent.getStringExtra("com.symbol.datawedge.data_string")
            Log.d("DataWedgeReceiver", "Scanned: $scanned")

            val i = Intent("scanner-data").putExtra("data", scanned)
            context?.let { LocalBroadcastManager.getInstance(it).sendBroadcast(i) }
        }
    }
}
