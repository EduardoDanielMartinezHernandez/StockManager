package com.meow.stockmanager

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

data class StockResumen(
    val codigoProducto: String,
    val nombre: String,
    val stock: Int
)

@Dao
interface StockDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertarProducto(producto: Producto)

    @Query("SELECT * FROM productos")
    suspend fun obtenerProductos(): List<Producto>

    @Insert
    suspend fun insertarMovimiento(movimiento: Movimiento)

    @Query("SELECT * FROM movimientos ORDER BY id DESC")
    suspend fun obtenerMovimientos(): List<Movimiento>

    @Update
    suspend fun actualizarMovimiento(movimiento: Movimiento)

    @Delete
    suspend fun borrarMovimiento(movimiento: Movimiento)

    @Query(
        "SELECT m.codigoProducto AS codigoProducto, p.nombre AS nombre, " +
                "SUM(CASE WHEN m.tipo = 'ENTRADA' THEN m.cantidad ELSE -m.cantidad END) AS stock " +
                "FROM movimientos m INNER JOIN productos p ON p.codigo = m.codigoProducto " +
                "GROUP BY m.codigoProducto, p.nombre"
    )
    suspend fun obtenerResumenStock(): List<StockResumen>

    @Query("SELECT * FROM movimientos WHERE sincronizado = 0")
    suspend fun obtenerPendientes(): List<Movimiento>

    @Query("UPDATE movimientos SET sincronizado = 1 WHERE sincronizado = 0")
    suspend fun marcarTodosSincronizados()
}