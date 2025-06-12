package com.example.bitacoradigital.network

import com.example.bitacoradigital.model.JerarquiaNodo
import com.example.bitacoradigital.model.PerfilResponse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface ApiService {
    companion object {
        fun create(): ApiService {
            val retrofit = Retrofit.Builder()
                .baseUrl("https://bit.cs3.mx/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            return retrofit.create(ApiService::class.java)
        }
    }

    @GET("accounts/perfil/activo/")
    suspend fun getPerfilActivo(@Header("Authorization") token: String): PerfilResponse
    @GET("api/v1/perimetro/organize_by_level/")
    suspend fun getJerarquiaPorNivel(
        @Query("perimetro_id") perimetroId: Int,
        @Header("x-session-token") token: String
    ): JerarquiaNodo

    @GET("accounts/me/")
    suspend fun getPerfil(
        @Header("Authorization") token: String
    ): PerfilResponse

}
