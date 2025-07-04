package com.example.bitacoradigital.viewmodel


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bitacoradigital.model.PerimetroVisual
import com.example.bitacoradigital.model.User
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.bitacoradigital.viewmodel.SessionViewModel

class HomeViewModel : ViewModel() {

    private val _perimetrosVisuales = MutableStateFlow<List<PerimetroVisual>>(emptyList())
    val perimetrosVisuales: StateFlow<List<PerimetroVisual>> = _perimetrosVisuales

    private val _empresaSeleccionada = MutableStateFlow<Int?>(null)
    val empresaSeleccionada: StateFlow<Int?> = _empresaSeleccionada

    private val _perimetroSeleccionado = MutableStateFlow<PerimetroVisual?>(null)
    val perimetroSeleccionado: StateFlow<PerimetroVisual?> = _perimetroSeleccionado

    fun cargarDesdeLogin(user: User, sessionViewModel: SessionViewModel) {
        val lista = mutableListOf<PerimetroVisual>()

        user.empresas.filter { it.B }.forEach { empresa ->
            empresa.perimetros.forEach { p ->
                p.rol.forEach { (rol, modulos) ->
                    val modulosConGuardia =
                        if ("Guardia" in modulos) modulos
                        else modulos + ("Guardia" to emptyList())
                    lista.add(
                        PerimetroVisual(
                            empresaId = empresa.id,
                            empresaNombre = empresa.nombre,
                            perimetroId = p.id,
                            perimetroNombre = p.nombre,
                            rol = rol,
                            modulos = modulosConGuardia
                        )
                    )
                }
            }
        }

        viewModelScope.launch {
            val favEmpresaId = withContext(Dispatchers.IO) {
                sessionViewModel.favoritoEmpresaId.firstOrNull()
            }
            val favPerimetroId = withContext(Dispatchers.IO) {
                sessionViewModel.favoritoPerimetroId.firstOrNull()
            }

            val actualizados = lista.map {
                it.copy(esFavorito = it.empresaId == favEmpresaId && it.perimetroId == favPerimetroId)
            }
            _perimetrosVisuales.value = actualizados
            val seleccionado = actualizados.firstOrNull { it.esFavorito } ?: actualizados.firstOrNull()
            _perimetroSeleccionado.value = seleccionado
            _empresaSeleccionada.value = seleccionado?.empresaId
        }
    }

    fun seleccionarPerimetro(perimetroVisual: PerimetroVisual) {
        _perimetroSeleccionado.value = perimetroVisual
        _empresaSeleccionada.value = perimetroVisual.empresaId
    }

    fun seleccionarEmpresa(empresaId: Int) {
        _empresaSeleccionada.value = empresaId
        val firstPerimetro = _perimetrosVisuales.value.firstOrNull { it.empresaId == empresaId }
        if (firstPerimetro != null) {
            _perimetroSeleccionado.value = firstPerimetro
        }
    }

    fun marcarFavorito(perimetroId: Int, empresaId: Int, sessionViewModel: SessionViewModel) {
        viewModelScope.launch {
            sessionViewModel.prefs.guardarFavorito(empresaId, perimetroId)
        }
        _perimetrosVisuales.update { list ->
            list.map {
                it.copy(esFavorito = it.perimetroId == perimetroId && it.empresaId == empresaId)
            }
        }
    }
}

