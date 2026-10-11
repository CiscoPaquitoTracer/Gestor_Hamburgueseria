package com.example.hamgurgueseria_monchosos.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface InsumoDao {
    @Query("SELECT * FROM insumos ORDER BY categoria, nombre COLLATE NOCASE")
    fun observarTodos(): Flow<List<Insumo>>

    @Upsert
    suspend fun guardar(insumo: Insumo)

    @Delete
    suspend fun eliminar(insumo: Insumo)

    @Query("UPDATE insumos SET cantidad = MAX(cantidad + :delta, 0) WHERE id = :id")
    suspend fun ajustarCantidad(id: Long, delta: Double)
}
