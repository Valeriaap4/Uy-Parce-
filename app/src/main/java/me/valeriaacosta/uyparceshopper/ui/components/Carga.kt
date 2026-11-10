package me.valeriaacosta.uyparceshopper.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import me.valeriaacosta.uyparceshopper.data.Listas
import me.valeriaacosta.uyparceshopper.data.LocalEstadoApp
import me.valeriaacosta.uyparceshopper.data.models.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.valeriaacosta.uyparceshopper.ui.theme.Navy

// ─── Estados de carga ───

/**
 * Algo que llega de Firebase puede estar cargando, no existir o estar listo
 * (la misma idea del MarsUiState visto en clase).
 */
sealed interface Carga<out T> {
    data object Cargando : Carga<Nothing>
    data object NoExiste : Carga<Nothing>
    data class Listo<out T>(val valor: T) : Carga<T>
}

/** Rueda de "cargando". */
@Composable
fun Cargando(modifier: Modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp)) {
    Box(modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Navy)
    }
}

/**
 * Para pantallas de detalle: muestra "cargando" mientras llega el dato, "no encontrado" si de verdad
 * no existe, y el contenido cuando está listo. Así nunca se ve "no encontrado" mientras carga.
 */
@Composable
fun <T> SegunCarga(carga: Carga<T>, titulo: String, onClickBack: (() -> Unit)?, contenido: @Composable (T) -> Unit) {
    when (carga) {
        is Carga.Listo -> contenido(carga.valor)
        Carga.Cargando -> UpScreen(titulo, onClickBack) { Cargando(Modifier.fillMaxSize()) }
        Carga.NoExiste -> NoEncontrado(titulo, onClickBack)
    }
}

/**
 * Busca algo para una pantalla de detalle: primero en lo que ya llegó en vivo (enMemoria) y, si no
 * está y la lista ya cargó, lo busca una vez en Firestore (pedidos viejos o recién creados).
 */
@Composable
fun <T : Any> rememberCarga(clave: Any, listaCargada: Boolean, enMemoria: T?, buscar: suspend () -> T?): Carga<T> {
    val buscado by produceState<Carga<T>>(Carga.Cargando, clave, listaCargada, enMemoria == null) {
        value = when {
            enMemoria != null -> Carga.Listo(enMemoria)
            !listaCargada -> Carga.Cargando
            else -> buscar()?.let { Carga.Listo(it) } ?: Carga.NoExiste
        }
    }
    return if (enMemoria != null) Carga.Listo(enMemoria) else buscado
}

@Composable
fun rememberPedido(numero: Int): Carga<Pedido> {
    val estado = LocalEstadoApp.current
    return rememberCarga(numero, estado.cargado(Listas.PEDIDOS), estado.pedidoEnMemoria(numero)) { estado.buscarPedido(numero) }
}

@Composable
fun rememberCaja(codigo: String): Carga<Caja> {
    val estado = LocalEstadoApp.current
    return rememberCarga(codigo, estado.cargado(Listas.CAJAS), estado.cajaEnMemoria(codigo)) { estado.buscarCaja(codigo) }
}

@Composable
fun rememberPago(id: String): Carga<Pago> {
    val estado = LocalEstadoApp.current
    return rememberCarga(id, estado.cargado(Listas.PAGOS), estado.pagoEnMemoria(id)) { estado.buscarPago(id) }
}

@Composable
fun rememberUsuario(id: String): Carga<Usuario> {
    val estado = LocalEstadoApp.current
    return rememberCarga(id, estado.cargado(Listas.USUARIOS), estado.usuarioEnMemoria(id)) { estado.buscarUsuario(id) }
}

@Composable
fun rememberProducto(id: String): Carga<Producto> {
    val estado = LocalEstadoApp.current
    return rememberCarga(id, estado.cargado(Listas.PRODUCTOS), estado.productoEnMemoria(id)) { null }
}

@Composable
fun rememberFactura(numeroPedido: Int): Carga<Factura> {
    val estado = LocalEstadoApp.current
    val listo = estado.cargado(Listas.FACTURAS) || estado.rol != Rol.CLIENTE
    return rememberCarga(numeroPedido, listo, estado.facturaEnMemoria(numeroPedido)) { estado.buscarFactura(numeroPedido) }
}
