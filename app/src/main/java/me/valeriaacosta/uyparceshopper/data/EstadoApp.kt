package me.valeriaacosta.uyparceshopper.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import me.valeriaacosta.uyparceshopper.data.models.*

/**
 * Estado compartido de la sesión: quién entró y su carrito.
 * Es un ViewModel para que la sesión no se pierda al girar el celular.
 * Más adelante la sesión vendrá de Firebase Authentication.
 */
class EstadoApp : ViewModel() {
    var usuario by mutableStateOf<Usuario?>(null)
        private set
    val carrito = mutableStateListOf<LineaPedido>()
    val ajustes: Ajustes get() = DatosEjemplo.ajustes

    val rol: Rol? get() = usuario?.rol

    /** Inicio de sesión de demostración: entra con el usuario de ejemplo de cada rol. */
    fun entrarComo(rol: Rol) {
        usuario = when (rol) {
            Rol.ADMIN -> DatosEjemplo.admin
            Rol.OPERADOR -> DatosEjemplo.operador
            Rol.SUPERVISOR -> DatosEjemplo.supervisor
            Rol.CLIENTE -> DatosEjemplo.laura
        }
        carrito.clear()
        if (rol == Rol.CLIENTE) {
            carrito += LineaPedido(DatosEjemplo.productos[0], "M", 1)
            carrito += LineaPedido(DatosEjemplo.productos[7], "Única", 1)
        }
    }

    fun cerrarSesion() {
        usuario = null
        carrito.clear()
    }

    fun agregarAlCarrito(producto: Producto, talla: String, cantidad: Int) {
        val i = carrito.indexOfFirst { it.producto.id == producto.id && it.talla == talla }
        if (i >= 0) carrito[i] = carrito[i].copy(cantidad = carrito[i].cantidad + cantidad)
        else carrito += LineaPedido(producto, talla, cantidad)
    }

    fun cambiarCantidad(index: Int, cantidad: Int) {
        if (cantidad <= 0) carrito.removeAt(index) else carrito[index] = carrito[index].copy(cantidad = cantidad)
    }
}

/** Estado de la sesión disponible en todas las pantallas. El valor por defecto sirve para los @Preview. */
val LocalEstadoApp = staticCompositionLocalOf { EstadoApp() }
