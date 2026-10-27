package com.example.hamgurgueseria_monchosos

import android.app.Application
import com.example.hamgurgueseria_monchosos.auth.Sesion
import com.example.hamgurgueseria_monchosos.avisos.AvisoReabastoWorker
import com.example.hamgurgueseria_monchosos.data.InventarioRepository
import com.example.hamgurgueseria_monchosos.data.menu.MenuRepository
import com.example.hamgurgueseria_monchosos.data.pedidos.PedidosRepository
import com.example.hamgurgueseria_monchosos.data.pronostico.PronosticoRepository
import com.example.hamgurgueseria_monchosos.data.remoto.crearSupabase
import kotlinx.coroutines.MainScope

/** Contenedor sencillo de dependencias compartidas por toda la app. */
class HamburgueseriaApp : Application() {
    val supabase by lazy { crearSupabase() }
    val sesion by lazy { Sesion(supabase, MainScope()) }
    val inventario by lazy { InventarioRepository(supabase) }
    val pedidos by lazy { PedidosRepository(supabase) }
    val menu by lazy { MenuRepository(supabase) }
    val pronostico by lazy { PronosticoRepository(supabase) }

    override fun onCreate() {
        super.onCreate()
        // Los datos ya viven en Supabase: se borra la base local de versiones anteriores (Room).
        deleteDatabase("hamburgueseria.db")
        AvisoReabastoWorker.programar(this)
    }
}
