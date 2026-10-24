package com.meow.stockmanager

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "movimientos",
    foreignKeys = [ForeignKey(
        entity = Producto::class,
        parentColumns = ["codigo"],
        childColumns = ["codigoProducto"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("codigoProducto")]
)
data class Movimiento(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val codigoProducto: String,
    val tipo: String,
    val cantidad: Int,
    val fecha: String,
    val sincronizado: Boolean = false
)