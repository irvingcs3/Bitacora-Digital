// 📁 viewmodel/RegistroVisitaViewModel.kt
package com.example.bitacoradigital.viewmodel

import android.net.Uri
import android.util.Log
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bitacoradigital.data.SessionPreferences
import com.example.bitacoradigital.model.JerarquiaNodo
import com.example.bitacoradigital.model.Residente
import com.example.bitacoradigital.model.NodoHoja
import com.example.bitacoradigital.network.ApiService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.net.SocketTimeoutException
import org.json.JSONArray



class RegistroVisitaViewModel(
    private val apiService: ApiService,
    private val sessionPrefs: SessionPreferences,
    private val perimetroId: Int,
    val isLomasCountry: Boolean = false

) : ViewModel() {
    val documentoUri = mutableStateOf<Uri?>(null)

    // Control del paso actual
    private val _pasoActual = MutableStateFlow(1)
    val pasoActual: StateFlow<Int> = _pasoActual

    // Datos recolectados
    var telefono = MutableStateFlow("")
    var numeroVerificado = MutableStateFlow(false)

    private val _codigoErrorCredencial = MutableStateFlow<Int?>(null)
    val codigoErrorCredencial: StateFlow<Int?> = _codigoErrorCredencial

    private val _cargandoCredencial = MutableStateFlow(false)
    val cargandoCredencial: StateFlow<Boolean> = _cargandoCredencial

    private var credencialJob: Job? = null

    fun limpiarErrorCredencial() {
        _codigoErrorCredencial.value = null
    }

    suspend fun verificarNumeroWhatsApp(numero: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply { put("number", numero) }
            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("https://bit.cs3.mx/v1/whatsapp/exist/number")
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build()
            val client = OkHttpClient()
            val result = client.newCall(request).execute().use { resp ->
                if (resp.isSuccessful) {
                    val res = JSONObject(resp.body?.string() ?: "{}")
                    res.optBoolean("exist", false)
                } else {
                    false
                }
            }
            result
        } catch (e: Exception) {
            Log.e("RegistroVisita", "Error verificando numero", e)
            false
        }
    }

    suspend fun cargarDatosCredencial(idPersona: Int? = null): Boolean {
        _cargandoCredencial.value = true
        _codigoErrorCredencial.value = null
        return try {
            withContext(Dispatchers.IO) {
                try {
                    val client = OkHttpClient()
                    val payload = JSONObject().apply {
                        put("telefono", telefono.value)
                        idPersona?.let { put("id_persona", it) }
                    }
                    val body = payload.toString().toRequestBody("application/json".toMediaType())
                    val request = Request.Builder()
                        .url("http://192.168.100.8:3000/api/credencial/")
                        .post(body)
                        .build()
                    val response = client.newCall(request).execute()
                    response.use { resp ->
                        if (resp.isSuccessful) {
                            val json = JSONObject(resp.body?.string() ?: "{}")
                            val cred = json.optJSONObject("credential_recognition") ?: json
                            nombre.value = cred.optString("nombre")
                            apellidoPaterno.value = cred.optString("paterno")
                            apellidoMaterno.value = cred.optString("materno")
                            val qrBase64 = json.optString("imagen_binaria")
                            fotoDocumentoUri.value = qrBase64
                            try {
                                val data = Base64.decode(qrBase64, Base64.DEFAULT)
                                qrBitmap.value = BitmapFactory.decodeByteArray(data, 0, data.size)
                            } catch (e: Exception) {
                                Log.e("RegistroVisita", "Error decoding QR", e)
                            }
                            true
                        } else {
                            _codigoErrorCredencial.value = resp.code
                            Log.e("RegistroVisita", "Error cargando credencial: ${resp.code}")
                            false
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e("RegistroVisita", "Error cargando credencial", e)
                    _codigoErrorCredencial.value = -1
                    false
                }
            }
        } finally {
            _cargandoCredencial.value = false
        }
    }

    var tipoDocumento = MutableStateFlow<String?>(null)
    var fotoDocumentoUri = MutableStateFlow<String?>(null)

    val nombre = MutableStateFlow("")
    val apellidoPaterno = MutableStateFlow("")
    val apellidoMaterno = MutableStateFlow("")

    val destinoSeleccionado = MutableStateFlow<JerarquiaNodo?>(null)
    val destinoLomasSeleccionado = MutableStateFlow<NodoHoja?>(null)
    val residenteSeleccionado = MutableStateFlow<Residente?>(null)


    // Ruta de navegación dentro de la jerarquía
    private val _rutaDestino = MutableStateFlow<List<JerarquiaNodo>>(emptyList())
    val rutaDestino: StateFlow<List<JerarquiaNodo>> = _rutaDestino

    var fotosOpcionales = MutableStateFlow<List<String>>(emptyList())

    fun avanzarPaso() {
        _pasoActual.value += 1
    }
    val registroCompleto = MutableStateFlow<Boolean>(false)
    val respuestaRegistro = MutableStateFlow<String?>(null)
    val qrBitmap = MutableStateFlow<android.graphics.Bitmap?>(null)

    fun retrocederPaso() {
        if (_pasoActual.value > 1) _pasoActual.value -= 1
    }
    fun actualizarDatos(nombre: String, paterno: String, materno: String) {
        this.nombre.value = nombre
        this.apellidoPaterno.value = paterno
        this.apellidoMaterno.value = materno
    }
    fun reiniciar() {
        _pasoActual.value = 1
        telefono.value = ""
        numeroVerificado.value = false
        tipoDocumento.value = null
        fotoDocumentoUri.value = null
        documentoUri.value = null
        nombre.value = ""
        apellidoPaterno.value = ""
        apellidoMaterno.value = ""
        destinoSeleccionado.value = null
        destinoLomasSeleccionado
        destinoLomasSeleccionado.value = null


        _rutaDestino.value = emptyList()
        fotosOpcionales.value = emptyList()
        respuestaRegistro.value = null
        residentesDestino.value = emptyList()
        residenteSeleccionado.value = null
        credencialJob?.cancel()
        _cargandoCredencial.value = false
        invitanteId.value = null
        _codigoErrorCredencial.value = null
        registroCompleto.value = false

    }
    val nivelesDestino = MutableStateFlow<List<NivelDestino>>(emptyList())
    val seleccionDestino = MutableStateFlow<Map<Int, OpcionDestino>>(emptyMap())

    fun setNivelesDestino(niveles: List<NivelDestino>) {
        nivelesDestino.value = niveles
    }

    fun seleccionarDestino(nivel: Int, opcion: OpcionDestino) {
        seleccionDestino.update { it.toMutableMap().apply { put(nivel, opcion) } }
    }

    fun obtenerDestinoFinal(): OpcionDestino? {
        val ultimoNivel = nivelesDestino.value.maxOfOrNull { it.nivel } ?: return null
        return seleccionDestino.value[ultimoNivel]
    }

    fun iniciarRuta(nodo: JerarquiaNodo) {
        _rutaDestino.value = listOf(nodo)
        destinoSeleccionado.value = null
    }

    fun navegarHacia(nodo: JerarquiaNodo) {
        _rutaDestino.update { it + nodo }
        if (nodo.children.isEmpty()) {
            destinoSeleccionado.value = nodo
        } else {
            destinoSeleccionado.value = null
        }
    }

    fun retrocederNivel() {
        _rutaDestino.update { path -> if (path.isNotEmpty()) path.dropLast(1) else path }
        destinoSeleccionado.value = null
    }
    private val _jerarquia = MutableStateFlow<JerarquiaNodo?>(null)
    val jerarquia: StateFlow<JerarquiaNodo?> = _jerarquia
    private val _nodosHoja = MutableStateFlow<List<NodoHoja>>(emptyList())
    val nodosHoja: StateFlow<List<NodoHoja>> = _nodosHoja

    private val _cargandoDestino = MutableStateFlow(false)
    val cargandoDestino: StateFlow<Boolean> = _cargandoDestino

    private val _errorDestino = MutableStateFlow<String?>(null)
    val errorDestino: StateFlow<String?> = _errorDestino

    // Estados para el reconocimiento de documentos
    private val _cargandoReconocimiento = MutableStateFlow(false)
    val cargandoReconocimiento: StateFlow<Boolean> = _cargandoReconocimiento
    private val _errorReconocimiento = MutableStateFlow<String?>(null)
    val errorReconocimiento: StateFlow<String?> = _errorReconocimiento

    fun clearDestinoError() {
        _errorDestino.value = null
    }

    fun clearReconocimientoError() {
        _errorReconocimiento.value = null
    }

    suspend fun obtenerCredencialLomasCountry(): Boolean {
        _cargandoReconocimiento.value = true
        _errorReconocimiento.value = null
        return try {
            val empty = ByteArray(0).toRequestBody(null, 0, 0)
            val request = Request.Builder()
                .url("http://192.168.100.8:3000/api/credencial/")
                .post(empty)
                .build()
            val client = OkHttpClient()
            val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
            response.use { resp ->
                if (resp.isSuccessful) {
                    val jsonStr = resp.body?.string()
                    val json = JSONObject(jsonStr ?: "{}")
                    val cred = json.optJSONObject("credential_recognition") ?: json
                    nombre.value = cred.optString("nombre")
                    apellidoPaterno.value = cred.optString("paterno")
                    apellidoMaterno.value = cred.optString("materno")
                    val qrBase64 = json.optString("imagen_binario")
                    fotoDocumentoUri.value = qrBase64
                    try {
                        val data = Base64.decode(qrBase64, Base64.DEFAULT)
                        qrBitmap.value = BitmapFactory.decodeByteArray(data, 0, data.size)
                    } catch (e: Exception) {
                        Log.e("RegistroVisita", "Error decoding QR", e)
                    }
                    true
                } else {
                    _errorReconocimiento.value = "Error ${resp.code}"
                    false
                }
            }
        } catch (e: Exception) {
            Log.e("RegistroVisita", "Error obteniendo credencial", e)
            _errorReconocimiento.value = e.localizedMessage
            false
        } finally {
            _cargandoReconocimiento.value = false
        }
    }

    private val _cargandoRegistro = MutableStateFlow(false)
    val cargandoRegistro: StateFlow<Boolean> = _cargandoRegistro

    fun cargarJerarquiaDestino() {
        viewModelScope.launch {
            _cargandoDestino.value = true
            _errorDestino.value = null
            try {
                val token = withContext(Dispatchers.IO) {
                    sessionPrefs.sessionToken.first()
                } ?: throw Exception("Token vacío")
                if (isLomasCountry) {
                    destinoLomasSeleccionado.value = null
                    residenteSeleccionado.value = null
                    residentesDestino.value = emptyList()
                    invitanteId.value = null
                    credencialJob?.cancel()
                    _cargandoCredencial.value = false
                    _codigoErrorCredencial.value = null
                    val request = Request.Builder()
                        .url("https://bit.cs3.mx/api/v1/perimetro/${perimetroId}/nodos-hoja")
                        .get()
                        .addHeader("x-session-token", token)
                        .build()
                    val client = OkHttpClient()
                    val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
                    response.use { resp ->
                        if (resp.isSuccessful) {
                            val jsonStr = withContext(Dispatchers.IO) { resp.body?.string() }
                            val arr = JSONArray(jsonStr ?: "[]")
                            val list = mutableListOf<NodoHoja>()
                            for (i in 0 until arr.length()) {
                                val obj = arr.getJSONObject(i)
                                list.add(NodoHoja(obj.optInt("id"), obj.optString("name")))
                            }
                            _nodosHoja.value = list
                        } else {
                            _nodosHoja.value = emptyList()
                            _errorDestino.value = "Error ${resp.code} al cargar destinos"
                        }
                    }
                } else {
                    val response = withContext(Dispatchers.IO) {
                        apiService.getJerarquiaPorNivel(perimetroId, token)
                    }
                    Log.d("RegistroVisita", "Llamando jerarquía con perimetroId=$perimetroId y token=$token")
                    _jerarquia.value = response
                    iniciarRuta(response)
                }

            } catch (e: Exception) {
                _nodosHoja.value = emptyList()
                _errorDestino.value = "Error al cargar destinos: ${e.localizedMessage}"
            } finally {
                _cargandoDestino.value = false
            }
        }
    }
    val fotosAdicionales = MutableStateFlow<List<Uri>>(emptyList())

    val residentesDestino = MutableStateFlow<List<Residente>>(emptyList())
    val cargandoResidentes = MutableStateFlow(false)
    val errorResidentes = MutableStateFlow<String?>(null)
    val invitanteId = MutableStateFlow<Int?>(null)

    fun agregarFoto(uri: Uri) {
        if (fotosAdicionales.value.size < 3) {
            fotosAdicionales.value = fotosAdicionales.value + uri
        }
    }

    fun eliminarFoto(uri: Uri) {
        fotosAdicionales.value = fotosAdicionales.value - uri
    }

    fun clearErrorResidentes() {
        errorResidentes.value = null
    }

    private fun solicitarCredencialParaResidente(idPersona: Int) {
        credencialJob?.cancel()
        credencialJob = viewModelScope.launch {
            try {
                cargarDatosCredencial(idPersona)
            } catch (e: CancellationException) {
                // Ignorar cancelaciones explícitas
            }
        }
    }

    fun seleccionarDestinoLomas(nodo: NodoHoja) {
        destinoLomasSeleccionado.value = nodo
        residentesDestino.value = emptyList()
        residenteSeleccionado.value = null
        invitanteId.value = null
        nombre.value = ""
        apellidoPaterno.value = ""
        apellidoMaterno.value = ""
        fotoDocumentoUri.value = null
        qrBitmap.value = null
        credencialJob?.cancel()
        _codigoErrorCredencial.value = null
        cargarResidentesDestino(nodo.id)
    }

    fun seleccionarResidenteLomas(residente: Residente) {
        residenteSeleccionado.value = residente
        invitanteId.value = residente.idPersona
        nombre.value = ""
        apellidoPaterno.value = ""
        apellidoMaterno.value = ""
        fotoDocumentoUri.value = null
        qrBitmap.value = null
        _codigoErrorCredencial.value = null
        if (numeroVerificado.value) {
            solicitarCredencialParaResidente(residente.idPersona)
        }
    }

    fun cargarResidentesDestino(perimetroHojaId: Int) {
        viewModelScope.launch {
            cargandoResidentes.value = true
            errorResidentes.value = null
            if (isLomasCountry) {
                residentesDestino.value = emptyList()
                residenteSeleccionado.value = null
                invitanteId.value = null
                credencialJob?.cancel()
            }
            try {
                val token = withContext(Dispatchers.IO) { sessionPrefs.sessionToken.first() } ?: return@launch
                val request = Request.Builder()
                    .url("https://bit.cs3.mx/api/v1/perimetro/${perimetroHojaId}/residentes")
                    .get()
                    .addHeader("x-session-token", token)
                    .build()
                val client = OkHttpClient()
                val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
                response.use { resp ->
                    if (resp.isSuccessful) {
                        val jsonStr = withContext(Dispatchers.IO) { resp.body?.string() }
                        val arr = org.json.JSONArray(jsonStr ?: "[]")
                        val list = mutableListOf<Residente>()
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            list.add(
                                Residente(
                                    id = obj.optInt("id"),
                                    name = obj.optString("name"),
                                    idPersona = obj.optInt("id_persona"),
                                    email = obj.optString("email"),
                                    registrationDate = obj.optString("registrationDate"),
                                    perimeterName = obj.optString("perimeterName"),
                                    perimeterId = obj.optInt("perimeterId")
                                )
                            )
                        }
                        residentesDestino.value = list
                        if (isLomasCountry && list.size == 1) {
                            seleccionarResidenteLomas(list.first())
                        }
                    } else {
                        errorResidentes.value = "Error ${resp.code}"
                    }
                }
            } catch (e: Exception) {
                errorResidentes.value = e.localizedMessage
            } finally {
                cargandoResidentes.value = false
            }
        }
    }

    suspend fun reconocerDocumento(context: android.content.Context) {
        val uri = documentoUri.value ?: return
        _cargandoReconocimiento.value = true
        _errorReconocimiento.value = null
        try {
            val bytes = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw Exception("No se pudo leer el archivo")
            }

            val requestBody = bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
            val multipart = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", "document.jpg", requestBody)
                .build()

            val request = Request.Builder()
                .url("https://bit.cs3.mx/credential/recognition")
                .post(multipart)
                .addHeader("Authorization", "Bearer MHIxMG4hQ2gxbjAjTHUxJF9LQHQwLUYzci5WMWMwKkNAJF8zbTF4SkAxbTMrMCRjQHJfMXJ2MW45DQoNCg")
                .addHeader("accept", "application/json")
                .build()

            // The backend may take considerable time to process the image.
            // Remove timeouts on the HTTP client so the request isn't aborted
            // before the server responds.
            val client = OkHttpClient.Builder()
                .connectTimeout(0, TimeUnit.MILLISECONDS)
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .writeTimeout(0, TimeUnit.MILLISECONDS)
                .callTimeout(0, TimeUnit.MILLISECONDS)
                .build()
            val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
            response.use { resp ->
                if (resp.isSuccessful) {
                    val jsonStr = withContext(Dispatchers.IO) { resp.body?.string() }
                    val json = org.json.JSONObject(jsonStr ?: "{}")
                    nombre.value = json.optString("nombre")
                    apellidoPaterno.value = json.optString("paterno")
                    apellidoMaterno.value = json.optString("materno")
                } else {
                    _errorReconocimiento.value = "Error ${resp.code}"
                }
            }
        } catch (e: Exception) {
            Log.e("RegistroVisita", "Error reconocimiento", e)
            _errorReconocimiento.value = e.localizedMessage
        } finally {
            _cargandoReconocimiento.value = false
        }
    }

    fun registrarVisita(
        context: android.content.Context,
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
    _cargandoRegistro.value = true
            var success = false
            Log.d("RegistroVisita", "Iniciando registrarVisita")
            try {
                val token = withContext(Dispatchers.IO) {
                    sessionPrefs.sessionToken.first()
                } ?: throw Exception("Token vacío")

                val zonaId = if (isLomasCountry) {
                    destinoLomasSeleccionado
                    destinoLomasSeleccionado.value?.id
                } else {
                    destinoSeleccionado.value?.perimetro_id
                } ?: throw Exception("Zona destino no seleccionada")
                

                Log.d("RegistroVisita", "Preparando registro para zona $zonaId con telefono ${telefono.value}")

                val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val fechaIso8601 = dateFormat.format(Date())

                val json = org.json.JSONObject().apply {
                    put("nombre", nombre.value)
                    put("apellido_pat", apellidoPaterno.value)
                    put("apellido_mat", apellidoMaterno.value)
                    put("numero", telefono.value)
                    put("id_perimetro", zonaId)
                    put("fecha", fechaIso8601)
                    fotoDocumentoUri.value?.let { put("foto_credencial", it) }
                }

                Log.d("RegistroVisita", "Payload de registro: $json")


                val mediaType = "application/json; charset=utf-8".toMediaType()
                val body = json.toString().toRequestBody(mediaType)

                val request = Request.Builder()
                    .url("https://bit.cs3.mx/api/v1/registro-visita/")
                    .post(body)
                    .addHeader("x-session-token", token)
                    .addHeader("Content-Type", "application/json")
                    .build()

                val client = OkHttpClient.Builder()
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .callTimeout(60, TimeUnit.SECONDS)
                    .build()
                val response = withContext(Dispatchers.IO) {
                    client.newCall(request).execute()
                }

                response.use { resp ->

                    Log.d("RegistroVisita", "Respuesta HTTP: ${resp.code}")


                    if (resp.isSuccessful) {
                        val bodyStr = withContext(Dispatchers.IO) { resp.body?.string() }
                        Log.d("RegistroVisita", "Registro exitoso: $bodyStr")

                        // Enviar QR y whatsapp solo para registros normales
                        val personaId = if (isLomasCountry) 334 else invitanteId.value ?: 0
                        val ineUri = documentoUri.value
                            ?: if (isLomasCountry) fotosAdicionales.value.firstOrNull() else null
                        var qrMsg: String? = null
                        var qrImg: Bitmap? = if (isLomasCountry) qrBitmap.value else null
                        if (!isLomasCountry && ineUri != null) {
                            try {
                                val bytes = withContext(Dispatchers.IO) {
                                    context.contentResolver.openInputStream(ineUri)?.use { it.readBytes() }
                                }
                                if (bytes != null) {
                                    val req = MultipartBody.Builder().setType(MultipartBody.FORM)
                                        .addFormDataPart("id_invitante", personaId.toString())
                                        .addFormDataPart("telefono_invitado", telefono.value)
                                        .addFormDataPart("dias_activacion", "1")
                                        .addFormDataPart("id_perimetro", perimetroId.toString())
                                        .addFormDataPart(
                                            "imagen",
                                            "ine.jpg",
                                            bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
                                        ).build()
                                    val qrRequest = Request.Builder()
                                        .url("http://qr.cs3.mx/bite/enviar-qr-id-ws/")
                                        .post(req)
                                        .build()
                                    val qrClient = OkHttpClient.Builder()
                                        .connectTimeout(0, TimeUnit.MILLISECONDS)
                                        .readTimeout(0, TimeUnit.MILLISECONDS)
                                        .writeTimeout(0, TimeUnit.MILLISECONDS)
                                        .callTimeout(0, TimeUnit.MILLISECONDS)
                                        .build()
                                    val qrResp = withContext(Dispatchers.IO) { qrClient.newCall(qrRequest).execute() }
                                    Log.d("Que esta pasando", "Ayuda: algo mas")
                                    Log.d("Que esta pasando", "Ayuda: $qrResp")

                                    qrResp.use { qrR ->
                                        if (qrR.isSuccessful) {
                                            val qStr = withContext(Dispatchers.IO) { qrR.body?.string() }
                                            val qJson = JSONObject(qStr ?: "{}")
                                            qrMsg = qJson.optString("mensaje")
                                            val imgBase = qJson.optString("imagen_binaria")
                                            val data = Base64.decode(imgBase, Base64.DEFAULT)
                                            qrImg = BitmapFactory.decodeByteArray(data, 0, data.size)
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("RegistroVisita", "Error QR", e)
                            }
                        }

                        respuestaRegistro.value = qrMsg ?: bodyStr
                        if (!isLomasCountry) {
                            qrBitmap.value = qrImg
                        }
                        registroCompleto.value = true
                        success = true
                    } else {
                        val errorBody = withContext(Dispatchers.IO) { resp.body?.string() }
                        Log.e("RegistroVisita", "Error en el registro: $errorBody")
                        _errorDestino.value = "Registro fallido: ${resp.code}"
                    }
                }

            } catch (e: SocketTimeoutException) {
                Log.e("RegistroVisita", "Timeout en el registro", e)
                _errorDestino.value = "Error al registrar visita: tiempo de espera agotado"
            } catch (e: Exception) {
                Log.e("RegistroVisita", "Excepción en el registro", e)
                _errorDestino.value = "Error al registrar visita: ${e.localizedMessage}"
            } finally {
                _cargandoRegistro.value = false
                onComplete(success)

        }
        }
    }

}
data class NivelDestino(
    val nivel: Int,
    val nombreNivel: String,
    val opciones: List<OpcionDestino>
)

data class OpcionDestino(
    val perimetroId: Int,
    val nombre: String
)