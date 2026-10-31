package com.example.hamgurgueseria_monchosos.ui.inicio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hamgurgueseria_monchosos.data.pronostico.Pronostico
import com.example.hamgurgueseria_monchosos.data.pronostico.PronosticoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PronosticoViewModel(private val repo: PronosticoRepository) : ViewModel() {
    private val _pronostico = MutableStateFlow<Pronostico?>(null)

    /** null mientras carga o si falló (la tarjeta simplemente no aparece). */
    val pronostico: StateFlow<Pronostico?> = _pronostico.asStateFlow()

    fun refrescar() {
        viewModelScope.launch {
            runCatching { repo.consultar() }.onSuccess { _pronostico.value = it }
        }
    }
}
