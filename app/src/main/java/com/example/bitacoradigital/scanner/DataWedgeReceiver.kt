package com.example.bitacoradigital.scanner

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class DataWedgeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val data = intent.getStringExtra("com.symbol.datawedge.data_string") ?: return
        val local = Intent("scanner-data").apply { putExtra("data", data) }
        LocalBroadcastManager.getInstance(context).sendBroadcast(local)
    }
}

