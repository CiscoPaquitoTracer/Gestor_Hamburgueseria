package com.example.hamgurgueseria_monchosos

import android.app.Application
import com.example.hamgurgueseria_monchosos.auth.SesionStore
import com.example.hamgurgueseria_monchosos.data.AppDatabase
import com.example.hamgurgueseria_monchosos.data.InventarioRepository

/** Contenedor sencillo de dependencias compartidas por toda la app. */
class HamburgueseriaApp : Application() {
    val inventario by lazy { InventarioRepository(AppDatabase.crear(this).insumoDao()) }
    val sesion by lazy { SesionStore(this) }
}
