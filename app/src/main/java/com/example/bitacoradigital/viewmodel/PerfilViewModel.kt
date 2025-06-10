package com.example.bitacoradigital.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bitacoradigital.model.PerfilResponse
import com.example.bitacoradigital.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PerfilViewModel(private val repository: UserRepository): ViewModel() {

    private val _perfiles = MutableStateFlow<List<PerfilResponse>>(emptyList())
    val perfiles: StateFlow<List<PerfilResponse>> = _perfiles

    fun cargarPerfiles(token: String) {
        viewModelScope.launch {
            _perfiles.value = listOf(repository.fetchPerfiles(token))
        }
    }
}
