package com.example.bitacoradigital.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.bitacoradigital.network.ApiService
import com.example.bitacoradigital.data.SessionPreferences
import com.example.bitacoradigital.viewmodel.RegistroVisitaViewModel

class RegistroVisitaViewModelFactory(
    private val apiService: ApiService,
    private val sessionPrefs: SessionPreferences,
    private val perimetroId: Int
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RegistroVisitaViewModel::class.java)) {
            return RegistroVisitaViewModel(apiService, sessionPrefs, perimetroId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}


