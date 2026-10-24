package com.meow.stockmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dao = StockDatabase.obtener(this).stockDao()
        setContent {
            MaterialTheme {
                PantallaStock(dao)
            }
        }
    }
}

@Composable
fun PantallaStock(dao: StockDao) {
    val scope = rememberCoroutineScope()
    var codigo by remember { mutableStateOf("") }
    var nombre by remember { mutableStateOf("") }
    var cantidad by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf("ENTRADA") }
    var movimientos by remember { mutableStateOf(listOf<Movimiento>()) }
    var resumen by remember { mutableStateOf(listOf<StockResumen>()) }
    var editando by remember { mutableStateOf<Movimiento?>(null) }
    var mensaje by remember { mutableStateOf("") }

    suspend fun cargarDatos() {
        movimientos = dao.obtenerMovimientos()
        resumen = dao.obtenerResumenStock()
    }

    LaunchedEffect(Unit) {
        cargarDatos()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        item {
            Text("StockManager", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = codigo,
                onValueChange = { codigo = it },
                label = { Text("Código del producto") },
                enabled = editando == null,
                modifier = Modifier.fillMaxWidth()
            )
            if (editando == null) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            OutlinedTextField(
                value = cantidad,
                onValueChange = { cantidad = it },
                label = { Text("Cantidad") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (tipo == "ENTRADA") {
                    Button(onClick = { tipo = "ENTRADA" }) { Text("Entrada") }
                    OutlinedButton(onClick = { tipo = "SALIDA" }) { Text("Salida") }
                } else {
                    OutlinedButton(onClick = { tipo = "ENTRADA" }) { Text("Entrada") }
                    Button(onClick = { tipo = "SALIDA" }) { Text("Salida") }
                }
            }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val cant = cantidad.toIntOrNull()
                    val actual = editando
                    if (cant == null || cant <= 0) {
                        mensaje = "Escribe una cantidad válida"
                    } else if (actual != null) {
                        scope.launch {
                            dao.actualizarMovimiento(
                                actual.copy(tipo = tipo, cantidad = cant, sincronizado = false)
                            )
                            cargarDatos()
                            editando = null
                            codigo = ""
                            cantidad = ""
                            mensaje = "Movimiento actualizado"
                        }
                    } else if (codigo.isBlank() || nombre.isBlank()) {
                        mensaje = "Completa el código y el nombre"
                    } else {
                        scope.launch {
                            dao.insertarProducto(Producto(codigo, nombre))
                            val fecha = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                                .format(Date())
                            dao.insertarMovimiento(
                                Movimiento(
                                    codigoProducto = codigo,
                                    tipo = tipo,
                                    cantidad = cant,
                                    fecha = fecha
                                )
                            )
                            cargarDatos()
                            codigo = ""
                            nombre = ""
                            cantidad = ""
                            mensaje = "Movimiento guardado"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (editando == null) "Guardar" else "Actualizar")
            }

            if (editando != null) {
                TextButton(
                    onClick = {
                        editando = null
                        codigo = ""
                        cantidad = ""
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancelar edición")
                }
            }

            if (mensaje.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(mensaje, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(16.dp))
            Text("Stock actual", style = MaterialTheme.typography.titleMedium)
        }

        items(resumen) { r ->
            Text(
                "${r.codigoProducto} - ${r.nombre}: ${r.stock}",
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        item {
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    scope.launch {
                        val pendientes = dao.obtenerPendientes()
                        if (pendientes.isEmpty()) {
                            mensaje = "No hay movimientos pendientes"
                        } else {
                            mensaje = "Sincronizando..."
                            delay(1500)
                            dao.marcarTodosSincronizados()
                            cargarDatos()
                            mensaje = "Se sincronizaron ${pendientes.size} movimientos"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Sincronizar")
            }
            Spacer(Modifier.height(16.dp))
            Text("Movimientos", style = MaterialTheme.typography.titleMedium)
        }

        items(movimientos) { mov ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("${mov.tipo}: ${mov.codigoProducto} (${mov.cantidad})")
                    val estado = if (mov.sincronizado) "Sincronizado" else "Pendiente"
                    Text("${mov.fecha} · $estado", style = MaterialTheme.typography.bodySmall)
                }
                Row {
                    TextButton(onClick = {
                        editando = mov
                        codigo = mov.codigoProducto
                        cantidad = mov.cantidad.toString()
                        tipo = mov.tipo
                    }) {
                        Text("Editar")
                    }
                    TextButton(onClick = {
                        scope.launch {
                            dao.borrarMovimiento(mov)
                            cargarDatos()
                        }
                    }) {
                        Text("Borrar")
                    }
                }
            }
        }
    }
}