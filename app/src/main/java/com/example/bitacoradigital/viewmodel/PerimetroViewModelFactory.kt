package com.example.bitacoradigital.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.bitacoradigital.network.ApiService
import com.example.bitacoradigital.data.SessionPreferences

class PerimetroViewModelFactory(
    private val apiService: ApiService,
    private val sessionPrefs: SessionPreferences,
    private val perimetroId: Int
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PerimetroViewModel::class.java)) {
            return PerimetroViewModel(apiService, sessionPrefs, perimetroId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
