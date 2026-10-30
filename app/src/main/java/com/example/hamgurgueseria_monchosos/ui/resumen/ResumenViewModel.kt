package com.example.hamgurgueseria_monchosos.ui.resumen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hamgurgueseria_monchosos.data.remoto.mensajeDeError
import com.example.hamgurgueseria_monchosos.data.resumen.Resumen
import com.example.hamgurgueseria_monchosos.data.resumen.ResumenRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class ResumenUiState(
    /** Primer día del mes que se ve, como Calendar. */
    val mes: Calendar = primerDiaDelMes(Calendar.getInstance()),
    val resumen: Resumen? = null,
    val cargando: Boolean = true,
    val error: String? = null,
) {
    val titulo: String
        get() = SimpleDateFormat("LLLL yyyy", Locale.forLanguageTag("es-MX")).format(mes.time)
            .replaceFirstChar { it.uppercase() }

    val esMesActual: Boolean get() = mes == primerDiaDelMes(Calendar.getInstance())
}

class ResumenViewModel(private val repo: ResumenRepository) : ViewModel() {
    private val _estado = MutableStateFlow(ResumenUiState())
    val estado: StateFlow<ResumenUiState> = _estado.asStateFlow()
    private var consulta: Job? = null

    fun refrescar() {
        val mes = _estado.value.mes
        consulta?.cancel()
        consulta = viewModelScope.launch {
            _estado.update { it.copy(cargando = it.resumen == null) }
            runCatching { repo.delMes(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(mes.time)) }
                .onSuccess { r -> _estado.update { it.copy(resumen = r, cargando = false, error = null) } }
                .onFailure { e -> _estado.update { it.copy(cargando = false, error = mensajeDeError(e)) } }
        }
    }

    /** -1 = mes anterior, +1 = siguiente (no pasa del mes actual). */
    fun moverMes(delta: Int) {
        val nuevo = (_estado.value.mes.clone() as Calendar).apply { add(Calendar.MONTH, delta) }
        if (nuevo.after(primerDiaDelMes(Calendar.getInstance()))) return
        _estado.update { it.copy(mes = nuevo, resumen = null, cargando = true) }
        refrescar()
    }

    fun descartarError() = _estado.update { it.copy(error = null) }
}

private fun primerDiaDelMes(c: Calendar): Calendar = (c.clone() as Calendar).apply {
    set(Calendar.DAY_OF_MONTH, 1)
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}
