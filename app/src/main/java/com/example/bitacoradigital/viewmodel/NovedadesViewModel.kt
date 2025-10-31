package com.example.bitacoradigital.viewmodel

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.bitacoradigital.data.SessionPreferences
import com.example.bitacoradigital.model.Novedad
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

data class GeneratedReport(
    val bytes: ByteArray,
    val fileName: String,
    /** Texto original escrito por el usuario, p.e. "@ia dame el reporte ..." */
    val originalPromptComment: String,
    /** Id del comentario padre si aplica (responder a un hilo) */
    val padreId: Int?
)

class NovedadesViewModel(
    private val prefs: SessionPreferences,
    private val perimetroId: Int
) : ViewModel() {

    private fun httpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(0, TimeUnit.MILLISECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .writeTimeout(0, TimeUnit.MILLISECONDS)
            .callTimeout(0, TimeUnit.MILLISECONDS)
            .build()

    private suspend fun obtenerToken(): String? =
        withContext(Dispatchers.IO) { prefs.sessionToken.firstOrNull() }

    private val _comentarios = MutableStateFlow<List<Novedad>>(emptyList())
    val comentarios: StateFlow<List<Novedad>> = _comentarios.asStateFlow()

    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _destacados = MutableStateFlow<Set<Int>>(emptySet())
    val destacados: StateFlow<Set<Int>> = _destacados.asStateFlow()

    /** Reporte generado, listo para mostrar diálogo y permitir descarga */
    private val _reporteGenerado = MutableStateFlow<GeneratedReport?>(null)
    val reporteGenerado: StateFlow<GeneratedReport?> = _reporteGenerado.asStateFlow()

    init {
        viewModelScope.launch {
            prefs.destacados.collect { set ->
                _destacados.value = set.mapNotNull { it.toIntOrNull() }.toSet()
            }
        }
    }

    fun clearError() { _error.value = null }
    fun clearReporteGenerado() { _reporteGenerado.value = null }

    fun cargarComentarios() {
        viewModelScope.launch {
            _cargando.value = true
            try {
                val token = obtenerToken() ?: return@launch
                val request = Request.Builder()
                    .url("https://bit.cs3.mx/api/v1/novedad/")
                    .get()
                    .addHeader("x-session-token", token)
                    .build()
                val resp = withContext(Dispatchers.IO) { httpClient().newCall(request).execute() }
                resp.use { r ->
                    if (r.isSuccessful) {
                        val arr = JSONArray(r.body?.string() ?: "[]")
                        val list = mutableListOf<Novedad>()
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            val padreRaw = obj.opt("padre")
                            val padreRoot = when (padreRaw) {
                                null -> null
                                is Int -> if (padreRaw == 0) null else padreRaw
                                else -> null
                            }
                            if (obj.optInt("perimetro") == perimetroId && padreRoot == null) {
                                list.add(parseNovedad(obj))
                            }
                        }
                        _comentarios.value = list
                    } else {
                        _error.value = "Error ${r.code}"
                    }
                }
            } catch (e: Exception) {
                _error.value = e.localizedMessage
            } finally {
                _cargando.value = false
            }
        }
    }

    fun editarComentario(id: Int, contenido: String) {
        viewModelScope.launch {
            _cargando.value = true
            try {
                val token = obtenerToken() ?: return@launch
                val json = JSONObject().apply { put("contenido", contenido) }
                val body = json.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("https://bit.cs3.mx/api/v1/novedad/${id}/")
                    .patch(body)
                    .addHeader("x-session-token", token)
                    .addHeader("Content-Type", "application/json")
                    .build()
                val response = withContext(Dispatchers.IO) { httpClient().newCall(request).execute() }
                response.use { resp ->
                    if (resp.isSuccessful) {
                        cargarComentarios()
                    } else {
                        _error.value = "Error ${resp.code}"
                    }
                }
            } catch (e: Exception) {
                _error.value = e.localizedMessage
            } finally {
                _cargando.value = false
            }
        }
    }

    fun eliminarComentario(id: Int) {
        viewModelScope.launch {
            _cargando.value = true
            try {
                val token = obtenerToken() ?: return@launch
                val request = Request.Builder()
                    .url("https://bit.cs3.mx/api/v1/novedad/${id}/")
                    .delete()
                    .addHeader("x-session-token", token)
                    .build()
                val response = withContext(Dispatchers.IO) { httpClient().newCall(request).execute() }
                response.use { resp ->
                    if (resp.isSuccessful) {
                        cargarComentarios()
                    } else {
                        _error.value = "Error ${resp.code}"
                    }
                }
            } catch (e: Exception) {
                _error.value = e.localizedMessage
            } finally {
                _cargando.value = false
            }
        }
    }

    private fun parseNovedad(obj: JSONObject): Novedad {
        val respuestasArr = obj.optJSONArray("respuestas") ?: JSONArray()
        val hijos = mutableListOf<Novedad>()
        for (i in 0 until respuestasArr.length()) {
            val child = respuestasArr.getJSONObject(i)
            hijos.add(parseNovedad(child))
        }
        val padreVal = obj.optInt("padre")
        val padreId = when {
            obj.isNull("padre") -> null
            padreVal == 0 -> null
            else -> padreVal
        }
        return Novedad(
            id = obj.optInt("id"),
            autor = obj.optString("autor"),
            contenido = obj.optString("contenido"),
            imagen = if (obj.isNull("imagen")) null else obj.optString("imagen"),
            fecha_creacion = obj.optString("fecha_creacion"),
            perimetro = obj.optInt("perimetro"),
            padre = padreId,
            respuestas = hijos,
            tipo = obj.optString("tipo").takeIf { it.isNotBlank() }
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun publicarComentario(context: Context, contenido: String, imagenUri: Uri?, padreId: Int?) {
        viewModelScope.launch {
            if (contenido.contains("@asistencia") && imagenUri == null) return@launch
            _cargando.value = true
            try {
                val token = obtenerToken() ?: return@launch
                var finalContenido = contenido
                var imageBytes: ByteArray? = null
                if (imagenUri != null) {
                    imageBytes = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(imagenUri)?.use { it.readBytes() }
                    }
                }
                if (contenido.contains("@asistencia") && imageBytes != null) {
                    val zone = ZoneId.of("America/Mexico_City")
                    val now = ZonedDateTime.now(zone)
                    val ts = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"))
                    val tsReadable = now.format(DateTimeFormatter.ofPattern("dd/MM/yyyy, HH:mm:ss"))
                    val request = Request.Builder()
                        .url("https://agente.cs3.mx/webhook/testimg")
                        .post(imageBytes.toRequestBody("image/jpeg".toMediaType()))
                        .addHeader("Content-Type", "image/jpeg")
                        .addHeader("X-Filename", "imagen.jpg")
                        .addHeader("X-Timestamp", ts)
                        .addHeader("X-Timestamp-Readable", tsReadable)
                        .addHeader("X-Timezone", "America/Mexico_City")
                        .build()
                    val resp = withContext(Dispatchers.IO) { httpClient().newCall(request).execute() }
                    resp.use { r ->
                        if (r.isSuccessful) {
                            val arr = JSONArray(r.body?.string() ?: "[]")
                            for (i in 0 until arr.length()) {
                                val obj = arr.optJSONObject(i)
                                if (obj != null && obj.has("output")) {
                                    finalContenido = obj.getString("output")
                                    break
                                }
                            }
                        } else {
                            _error.value = "Error ${r.code}"
                            return@launch
                        }
                    }
                }

                val builder = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("contenido", finalContenido)
                    .addFormDataPart("perimetro", perimetroId.toString())
                if (padreId != null) builder.addFormDataPart("padre", padreId.toString())
                imageBytes?.let {
                    builder.addFormDataPart(
                        "imagen",
                        "imagen.jpg",
                        it.toRequestBody("image/jpeg".toMediaTypeOrNull())
                    )
                }
                val requestBody = builder.build()
                val request = Request.Builder()
                    .url("https://bit.cs3.mx/api/v1/novedad/")
                    .post(requestBody)
                    .addHeader("x-session-token", token)
                    .build()
                val resp = withContext(Dispatchers.IO) { httpClient().newCall(request).execute() }
                resp.use { r ->
                    if (!r.isSuccessful) {
                        _error.value = "Error ${r.code}"
                    } else {
                        cargarComentarios()
                    }
                }
            } catch (e: Exception) {
                _error.value = e.localizedMessage
            } finally {
                _cargando.value = false
            }
        }
    }

    fun toggleDestacado(id: Int) {
        viewModelScope.launch { prefs.toggleDestacado(id) }
    }

    // =========================
    //        @ia FLUJO
    // =========================

    @RequiresApi(Build.VERSION_CODES.O)
    fun publicarComentarioIA(contenido: String, padreId: Int?) {
        viewModelScope.launch {
            val trimmed = contenido.trimStart()
            if (!trimmed.startsWith("@ia")) return@launch
            val userPrompt = trimmed.removePrefix("@ia").trimStart()
            if (userPrompt.isBlank()) {
                _error.value = "Escribe una solicitud para @ia"
                return@launch
            }
            _cargando.value = true
            try {
                // 1) HTML desde el agente
                val html = solicitarReporteHtml(userPrompt)
                val lowerHtml = html.lowercase()
                if (!lowerHtml.contains("<html") && !lowerHtml.contains("<!doctype html")) {
                    _error.value = "La respuesta de @ia es inválida"
                    return@launch
                }
                // 2) Generar PDF (bytes)
                val (pdfBytes, baseName) = generarPdfDesdeHtml(html)
                // 3) Exponer al UI para mostrar diálogo de descarga
                _reporteGenerado.value = GeneratedReport(
                    bytes = pdfBytes,
                    fileName = "$baseName.pdf",
                    originalPromptComment = contenido,
                    padreId = padreId
                )
            } catch (e: Exception) {
                _error.value = e.localizedMessage
            } finally {
                _cargando.value = false
            }
        }
    }

    /** El UI debe llamar esto tras guardar con éxito el PDF */
    fun confirmarDescargaExitosa() {
        val payload = _reporteGenerado.value ?: return
        viewModelScope.launch {
            try {
                val token = obtenerToken() ?: return@launch
                enviarComentario(
                    token = token,
                    contenido = "Reporte generado y descargado",
                    padreId = payload.padreId,
                    adjunto = null
                )
            } catch (e: Exception) {
                _error.value = e.localizedMessage
            } finally {
                clearReporteGenerado()
            }
        }
    }

    private suspend fun enviarComentario(
        token: String,
        contenido: String,
        padreId: Int?,
        adjunto: ComentarioAdjunto?
    ) {
        val builder = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("contenido", contenido)
            .addFormDataPart("perimetro", perimetroId.toString())
        if (padreId != null) builder.addFormDataPart("padre", padreId.toString())
        adjunto?.let {
            builder.addFormDataPart(
                "imagen",
                it.fileName,
                it.bytes.toRequestBody(it.mimeType.toMediaTypeOrNull())
            )
        }
        val requestBody = builder.build()
        val request = Request.Builder()
            .url("https://bit.cs3.mx/api/v1/novedad/")
            .post(requestBody)
            .addHeader("x-session-token", token)
            .build()
        val resp = withContext(Dispatchers.IO) { httpClient().newCall(request).execute() }
        resp.use { r ->
            if (!r.isSuccessful) _error.value = "Error ${r.code}" else cargarComentarios()
        }
    }

    // ------ util @ia ------
    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun generarPdfDesdeHtml(html: String): Pair<ByteArray, String> {
        val zone = ZoneId.of("America/Mexico_City")
        val now = ZonedDateTime.now(zone)
        val baseName = "reporte " + now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm"))
        val bodyJson = JSONObject().apply {
            put("html", html)
            put("filename", baseName)
        }
        val body = bodyJson.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("https://bit.cs3.mx/api/v1/utils/html-to-pdf/")
            .post(body)
            .addHeader("Content-Type", "application/json")
            .build()
        val response = withContext(Dispatchers.IO) { httpClient().newCall(request).execute() }
        response.use { r ->
            if (!r.isSuccessful) throw IllegalStateException("Error ${r.code}")
            val bytes = r.body?.bytes() ?: ByteArray(0)
            if (bytes.isEmpty()) throw IllegalStateException("El PDF generado está vacío")
            return bytes to baseName
        }
    }

    private suspend fun solicitarReporteHtml(userPrompt: String): String {
        val json = JSONObject().apply { put("userprompt", userPrompt) }
        val body = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("https://agente.cs3.mx/webhook/wrapnove")
            .post(body)
            .addHeader("Content-Type", "application/json")
            .build()
        val response = withContext(Dispatchers.IO) { httpClient().newCall(request).execute() }
        response.use { r ->
            if (!r.isSuccessful) throw IllegalStateException("Error ${r.code}")
            return r.body?.string() ?: ""
        }
    }
}

data class ComentarioAdjunto(
    val bytes: ByteArray,
    val fileName: String,
    val mimeType: String
)

class NovedadesViewModelFactory(
    private val prefs: SessionPreferences,
    private val perimetroId: Int
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NovedadesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NovedadesViewModel(prefs, perimetroId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
