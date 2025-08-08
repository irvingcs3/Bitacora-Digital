package com.example.bitacoradigital.repository

import android.util.Log
import com.example.bitacoradigital.model.Checkpoint
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.util.concurrent.TimeUnit

class CheckpointRepository {
    companion object {
        private const val TAG = "CheckpointRepo"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    suspend fun getCheckpoints(perimetroId: Int, token: String): List<Checkpoint> {
        val request = Request.Builder()
            .url("https://bit.cs3.mx/api/v1/checkpoints/?perimetro=$perimetroId")
            .get()
            .addHeader("x-session-token", token)
            .build()

        val response = client.newCall(request).execute()
        response.use { resp ->
            if (!resp.isSuccessful) {
                Log.d(TAG, "Error ${'$'}{resp.code} al cargar checkpoints")
                return emptyList()
            }
            val jsonStr = resp.body?.string() ?: "[]"
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<Checkpoint>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    Checkpoint(
                        checkpoint_id = obj.optInt("checkpoint_id"),
                        nombre = obj.optString("nombre"),
                        tipo = obj.optString("tipo"),
                        perimetro = obj.optInt("perimetro")
                    )
                )
            }
            return list
        }
    }
}

