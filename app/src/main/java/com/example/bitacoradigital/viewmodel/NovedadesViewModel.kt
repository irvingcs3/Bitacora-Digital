package com.example.bitacoradigital.viewmodel

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.bitacoradigital.data.SessionPreferences
import com.example.bitacoradigital.model.Novedad
import com.example.bitacoradigital.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import androidx.core.content.FileProvider
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import java.io.File
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class NovedadesViewModel(
    private val prefs: SessionPreferences,
    private val perimetroId: Int
) : ViewModel() {

    private val _comentarios = MutableStateFlow<List<Novedad>>(emptyList())
    val comentarios: StateFlow<List<Novedad>> = _comentarios.asStateFlow()

    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    private val _destacados = MutableStateFlow<Set<Int>>(emptySet())
    val destacados: StateFlow<Set<Int>> = _destacados.asStateFlow()
    private val _reportePdfUri = MutableStateFlow<Uri?>(null)
    val reportePdfUri: StateFlow<Uri?> = _reportePdfUri.asStateFlow()

    init {
        viewModelScope.launch {
            prefs.destacados.collect { set ->
                _destacados.value = set.mapNotNull { it.toIntOrNull() }.toSet()
            }
        }
    }

    fun clearError() { _error.value = null }

    fun clearReportePdfUri() { _reportePdfUri.value = null }

    fun cargarComentarios() {
        viewModelScope.launch {
            _cargando.value = true
            try {
                val token = withContext(Dispatchers.IO) { prefs.sessionToken.firstOrNull() } ?: return@launch
                val request = Request.Builder()
                    .url("https://bit.cs3.mx/api/v1/novedad/")
                    .get()
                    .addHeader("x-session-token", token)
                    .build()
                val client = OkHttpClient.Builder()
                    .connectTimeout(0, TimeUnit.MILLISECONDS)
                    .readTimeout(0, TimeUnit.MILLISECONDS)
                    .writeTimeout(0, TimeUnit.MILLISECONDS)
                    .callTimeout(0, TimeUnit.MILLISECONDS)
                    .build()
                val resp = withContext(Dispatchers.IO) { client.newCall(request).execute() }
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
                val token = withContext(Dispatchers.IO) { prefs.sessionToken.firstOrNull() } ?: return@launch
                val json = JSONObject().apply { put("contenido", contenido) }
                val body = json.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("https://bit.cs3.mx/api/v1/novedad/${id}/")
                    .patch(body)
                    .addHeader("x-session-token", token)
                    .addHeader("Content-Type", "application/json")
                    .build()
                val client = OkHttpClient.Builder()
                    .connectTimeout(0, TimeUnit.MILLISECONDS)
                    .readTimeout(0, TimeUnit.MILLISECONDS)
                    .writeTimeout(0, TimeUnit.MILLISECONDS)
                    .callTimeout(0, TimeUnit.MILLISECONDS)
                    .build()
                val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
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
                val token = withContext(Dispatchers.IO) { prefs.sessionToken.firstOrNull() } ?: return@launch
                val request = Request.Builder()
                    .url("https://bit.cs3.mx/api/v1/novedad/${id}/")
                    .delete()
                    .addHeader("x-session-token", token)
                    .build()
                val client = OkHttpClient.Builder()
                    .connectTimeout(0, TimeUnit.MILLISECONDS)
                    .readTimeout(0, TimeUnit.MILLISECONDS)
                    .writeTimeout(0, TimeUnit.MILLISECONDS)
                    .callTimeout(0, TimeUnit.MILLISECONDS)
                    .build()
                val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
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

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun solicitarReporteIA(context: Context, prompt: String) {
        viewModelScope.launch {
            _cargando.value = true
            try {
                val json = JSONObject().apply { put("userprompt", prompt) }
                val body = json.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("https://agente.cs3.mx/webhook/wrapnove")
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build()
                val client = OkHttpClient.Builder()
                    .connectTimeout(0, TimeUnit.MILLISECONDS)
                    .readTimeout(0, TimeUnit.MILLISECONDS)
                    .writeTimeout(0, TimeUnit.MILLISECONDS)
                    .callTimeout(0, TimeUnit.MILLISECONDS)
                    .build()
                val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
                response.use { resp ->
                    if (resp.isSuccessful) {
                        val html = resp.body?.string().orEmpty()
                        if (html.isNotBlank()) {
                            val uri = withContext(Dispatchers.Main) { generarPdfDesdeHtml(context, html) }
                            _reportePdfUri.value = uri
                        } else {
                            _error.value = "Reporte vacío"
                        }
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

    @RequiresApi(Build.VERSION_CODES.O)
    fun publicarComentario(context: Context, contenido: String, imagenUri: Uri?, padreId: Int?) {
        viewModelScope.launch {
            if (contenido.contains("@asistencia") && imagenUri == null) return@launch
            _cargando.value = true
            try {
                val token = withContext(Dispatchers.IO) { prefs.sessionToken.firstOrNull() } ?: return@launch
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
                    val client = OkHttpClient.Builder()
                        .connectTimeout(0, TimeUnit.MILLISECONDS)
                        .readTimeout(0, TimeUnit.MILLISECONDS)
                        .writeTimeout(0, TimeUnit.MILLISECONDS)
                        .callTimeout(0, TimeUnit.MILLISECONDS)
                        .build()
                    val resp = withContext(Dispatchers.IO) { client.newCall(request).execute() }
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
                if (padreId != null) {
                    builder.addFormDataPart("padre", padreId.toString())
                }
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
                val client = OkHttpClient.Builder()
                    .connectTimeout(0, TimeUnit.MILLISECONDS)
                    .readTimeout(0, TimeUnit.MILLISECONDS)
                    .writeTimeout(0, TimeUnit.MILLISECONDS)
                    .callTimeout(0, TimeUnit.MILLISECONDS)
                    .build()
                val resp = withContext(Dispatchers.IO) { client.newCall(request).execute() }
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
        viewModelScope.launch {
            prefs.toggleDestacado(id)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
private suspend fun generarPdfDesdeHtml(context: Context, html: String): Uri =
    suspendCancellableCoroutine { continuation ->
        val webView = WebView(context)
        webView.settings.defaultTextEncodingName = "UTF-8"
        val pdfDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val file = File(pdfDir, "reporte_ia_${System.currentTimeMillis()}.pdf")
        val cancellationSignal = CancellationSignal()
        continuation.invokeOnCancellation {
            cancellationSignal.cancel()
            webView.destroy()
        }
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val adapter = webView.createPrintDocumentAdapter("ReporteIA")
                val attributes = PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                    .setResolution(PrintAttributes.Resolution("reporte", "Reporte", 600, 600))
                    .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                    .build()
                adapter.onLayout(
                    null,
                    attributes,
                    cancellationSignal,
                    object : PrintDocumentAdapter.LayoutResultCallback() {
                        override fun onLayoutFinished(info: PrintDocumentInfo?, changed: Boolean) {
                            try {
                                val parcelDescriptor = ParcelFileDescriptor.open(
                                    file,
                                    ParcelFileDescriptor.MODE_CREATE or
                                            ParcelFileDescriptor.MODE_TRUNCATE or
                                            ParcelFileDescriptor.MODE_WRITE_ONLY
                                )
                                adapter.onWrite(
                                    arrayOf(PageRange.ALL_PAGES),
                                    parcelDescriptor,
                                    CancellationSignal(),
                                    object : PrintDocumentAdapter.WriteResultCallback() {
                                        override fun onWriteFinished(pages: Array<PageRange>) {
                                            try {
                                                parcelDescriptor.close()
                                            } catch (_: Exception) {
                                            }
                                            if (continuation.isActive) {
                                                val uri = FileProvider.getUriForFile(
                                                    context,
                                                    Constants.FILE_PROVIDER_AUTHORITY,
                                                    file
                                                )
                                                continuation.resume(uri)
                                            }
                                            webView.destroy()
                                        }

                                        override fun onWriteFailed(error: CharSequence?) {
                                            try {
                                                parcelDescriptor.close()
                                            } catch (_: Exception) {
                                            }
                                            if (continuation.isActive) {
                                                continuation.resumeWithException(
                                                    Exception(error?.toString() ?: "Error al generar PDF")
                                                )
                                            }
                                            webView.destroy()
                                        }
                                    }
                                )
                            } catch (e: Exception) {
                                if (continuation.isActive) {
                                    continuation.resumeWithException(e)
                                }
                                webView.destroy()
                            }
                        }

                        override fun onLayoutFailed(error: CharSequence?) {
                            if (continuation.isActive) {
                                continuation.resumeWithException(
                                    Exception(error?.toString() ?: "Error al preparar PDF")
                                )
                            }
                            webView.destroy()
                        }
                    },
                    null
                )
            }
        }
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }

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