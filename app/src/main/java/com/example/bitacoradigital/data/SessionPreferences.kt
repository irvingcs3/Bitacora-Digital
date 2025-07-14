package com.example.bitacoradigital.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "bitacora_session")

object SessionKeys {
    val SESSION_TOKEN = stringPreferencesKey("session_token")
    val USER_ID = intPreferencesKey("user_id")
    val PERSONA_ID = intPreferencesKey("persona_id")
    val FAVORITO_EMPRESA_ID = intPreferencesKey("favorito_empresa_id")
    val FAVORITO_PERIMETRO_ID = intPreferencesKey("favorito_perimetro_id")
    val JSON_SESSION = stringPreferencesKey("json_session") // ✅ nuevo
    val VERSION = stringPreferencesKey("version")
    val UUID_BOTON = stringPreferencesKey("uuid_boton")
}

class SessionPreferences(private val context: Context) {

    val sessionToken: Flow<String?> = context.dataStore.data.map { it[SessionKeys.SESSION_TOKEN] }
    val userId: Flow<Int?> = context.dataStore.data.map { it[SessionKeys.USER_ID] }
    val personaId: Flow<Int?> = context.dataStore.data.map { it[SessionKeys.PERSONA_ID] }
    val favoritoEmpresaId: Flow<Int?> = context.dataStore.data.map { it[SessionKeys.FAVORITO_EMPRESA_ID] }
    val favoritoPerimetroId: Flow<Int?> = context.dataStore.data.map { it[SessionKeys.FAVORITO_PERIMETRO_ID] }
    val jsonSession: Flow<String?> = context.dataStore.data.map { it[SessionKeys.JSON_SESSION] } // ✅ nuevo
    val version: Flow<String?> = context.dataStore.data.map { it[SessionKeys.VERSION] }
    val uuidBoton: Flow<String?> = context.dataStore.data.map { it[SessionKeys.UUID_BOTON] }

    suspend fun guardarSesion(token: String, userId: Int, personaId: Int, json: String, version: String) {
        context.dataStore.edit { prefs ->
            prefs[SessionKeys.SESSION_TOKEN] = token
            prefs[SessionKeys.USER_ID] = userId
            prefs[SessionKeys.PERSONA_ID] = personaId
            prefs[SessionKeys.JSON_SESSION] = json // ✅ nuevo
            prefs[SessionKeys.VERSION] = version
        }
    }


    suspend fun guardarFavorito(empresaId: Int, perimetroId: Int) {
        context.dataStore.edit { prefs ->
            prefs[SessionKeys.FAVORITO_EMPRESA_ID] = empresaId
            prefs[SessionKeys.FAVORITO_PERIMETRO_ID] = perimetroId
        }
    }

    suspend fun guardarUuidBoton(uuid: String) {
        context.dataStore.edit { prefs ->
            prefs[SessionKeys.UUID_BOTON] = uuid
        }
    }

    suspend fun cerrarSesion() {
        context.dataStore.edit { it.clear() }
    }
}
