package com.example.bitacoradigital.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bitacoradigital.data.SessionPreferences
import com.example.bitacoradigital.model.DashboardCounts
import com.example.bitacoradigital.model.InvitacionEstado
import com.example.bitacoradigital.model.VisitasDia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

class DashboardViewModel(
    private val prefs: SessionPreferences,
    private val perimetroId: Int
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    private val client = OkHttpClient()

    fun cargarDatos() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            try {
                val token = withContext(Dispatchers.IO) { prefs.sessionToken.first() } ?: return@launch

                val counts = getCounts(token)
                val invitaciones = getInvitaciones(token)
                val semana = getSemana(token)
                val semanaLinea = getSemanaGlobal(token)
                val semanaLineaQr = getSemanaGlobalQr(token)

                _state.value = DashboardUiState(
                    counts = counts,
                    invitaciones = invitaciones,
                    visitasSemana = semana,
                    visitasLinea = semanaLinea,
                    visitasLineaQr = semanaLineaQr,
                    loading = false,
                    error = null
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.localizedMessage)
            }
        }
    }

    private suspend fun getCounts(token: String): DashboardCounts? {
        val request = Request.Builder()
            .url("https://bit.cs3.mx/api/v1/perimetro/${perimetroId}/dashboard-count/")
            .get()
            .addHeader("x-session-token", token)
            .build()
        val resp = withContext(Dispatchers.IO) { client.newCall(request).execute() }
        resp.use { r ->
            if (!r.isSuccessful) return null
            val obj = JSONObject(r.body?.string() ?: "{}")
            return DashboardCounts(
                total_residentes = obj.optInt("total_residentes"),
                visitas_hoy = obj.optInt("visitas_hoy"),
                total_qrs = obj.optInt("total_qrs")
            )
        }
    }

    private suspend fun getInvitaciones(token: String): List<InvitacionEstado> {
        val request = Request.Builder()
            .url("https://bit.cs3.mx/api/v1/perimetro/${perimetroId}/dashboard-invitaciones-estado/")
            .get()
            .addHeader("x-session-token", token)
            .build()
        val resp = withContext(Dispatchers.IO) { client.newCall(request).execute() }
        resp.use { r ->
            if (!r.isSuccessful) return emptyList()
            val arr = JSONArray(r.body?.string() ?: "[]")
            val list = mutableListOf<InvitacionEstado>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(InvitacionEstado(obj.optString("estado"), obj.optInt("cantidad")))
            }
            return list
        }
    }

    private suspend fun getSemana(token: String): List<VisitasDia> {
        val request = Request.Builder()
            .url("https://bit.cs3.mx/api/v1/perimetro/${perimetroId}/dashboard-visitas-semana/")
            .get()
            .addHeader("x-session-token", token)
            .build()
        val resp = withContext(Dispatchers.IO) { client.newCall(request).execute() }
        resp.use { r ->
            if (!r.isSuccessful) return emptyList()
            val arr = JSONArray(r.body?.string() ?: "[]")
            val list = mutableListOf<VisitasDia>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(VisitasDia(obj.optString("name"), obj.optInt("visitas")))
            }
            return list
        }
    }

    private suspend fun getSemanaGlobal(token: String): List<VisitasDia> {
        val request = Request.Builder()
            .url("https://bit.cs3.mx/api/v1/perimetro/${perimetroId}/dashboard-visitas-semana-global/")
            .get()
            .addHeader("x-session-token", token)
            .build()
        val resp = withContext(Dispatchers.IO) { client.newCall(request).execute() }
        resp.use { r ->
            if (!r.isSuccessful) return emptyList()
            val arr = JSONArray(r.body?.string() ?: "[]")
            val list = mutableListOf<VisitasDia>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(VisitasDia(obj.optString("name"), obj.optInt("visitas")))
            }
            return list
        }
    }

    private suspend fun getSemanaGlobalQr(token: String): List<VisitasDia> {
        val request = Request.Builder()
            .url("https://bit.cs3.mx/api/v1/perimetro/${perimetroId}/dashboard-visitas-semana-global-qr/")
            .get()
            .addHeader("x-session-token", token)
            .build()
        val resp = withContext(Dispatchers.IO) { client.newCall(request).execute() }
        resp.use { r ->
            if (!r.isSuccessful) return emptyList()
            val arr = JSONArray(r.body?.string() ?: "[]")
            val list = mutableListOf<VisitasDia>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(VisitasDia(obj.optString("name"), obj.optInt("visitas")))
            }
            return list
        }
    }
}

data class DashboardUiState(
    val counts: DashboardCounts? = null,
    val invitaciones: List<InvitacionEstado> = emptyList(),
    val visitasSemana: List<VisitasDia> = emptyList(),
    val visitasLinea: List<VisitasDia> = emptyList(),
    val visitasLineaQr: List<VisitasDia> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null
)
