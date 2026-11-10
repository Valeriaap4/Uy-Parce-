package me.valeriaacosta.uyparceshopper.data

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.valeriaacosta.uyparceshopper.data.firebase.*
import me.valeriaacosta.uyparceshopper.data.models.*

/** En qué punto está la sesión. La navegación decide a dónde ir según esto. */
enum class Sesion { CARGANDO, SIN_SESION, EN_REVISION, ACTIVA }

/** Nombres de las listas que se escuchan en vivo (para saber si ya cargaron). */
object Listas {
    const val AJUSTES = "ajustes"
    const val PRODUCTOS = "productos"
    const val CARRITO = "carrito"
    const val PEDIDOS = "pedidos"
    const val PAGOS = "pagos"
    const val CAJAS = "cajas"
    const val FACTURAS = "facturas"
    const val USUARIOS = "usuarios"
    const val NOTIFICACIONES = "notificaciones"
    const val PAPELERA = "papelera"
}

/**
 * Estado de toda la app (ViewModel, sobrevive al girar el celular):
 *  - quién inició sesión (Firebase Auth + su documento en usuarios),
 *  - los datos que se escuchan en vivo según el rol,
 *  - las acciones que guardan en Firebase.
 * Las pantallas lo leen con LocalEstadoApp.current. Camino de una acción:
 * pantalla → EstadoApp → repositorio → Firebase.
 */
class EstadoApp : ViewModel() {

    /** true en los @Preview: se usan los datos de ejemplo y no se toca Firebase. */
    var esEjemplo = false
        private set

    // ─── Sesión ─────────────────────────────────────────────────────────────

    var sesion by mutableStateOf(Sesion.CARGANDO)
        private set
    var usuario by mutableStateOf<Usuario?>(null)
        private set
    val rol: Rol? get() = usuario?.rol

    /** Mensaje corto para mostrar abajo (el NavHost lo muestra y lo borra). */
    var aviso by mutableStateOf<String?>(null)

    /** Por qué se cerró la sesión (cuenta desactivada, etc.). Se muestra en el login. */
    var avisoSesion by mutableStateOf<String?>(null)

    private var iniciado = false
    private var registrando = false
    private var uidEscuchado: String? = null
    private var rolEscuchado: Rol? = null
    private var escuchaUsuario: ListenerRegistration? = null
    private val escuchas = mutableListOf<ListenerRegistration>()
    private var generacion = 0

    // ─── Datos en vivo ──────────────────────────────────────────────────────

    var ajustes by mutableStateOf(Ajustes())
        private set
    /** false mientras el admin no haya preparado la base de datos. */
    var hayAjustes by mutableStateOf(false)
        private set
    var productos by mutableStateOf(emptyList<Producto>())
        private set
    /** Cliente: todos sus pedidos. Equipo: los pedidos con trabajo pendiente. */
    var pedidos by mutableStateOf(emptyList<Pedido>())
        private set
    /** Cliente: sus pagos. Supervisor y admin: los pagos por revisar. */
    var pagos by mutableStateOf(emptyList<Pago>())
        private set
    /** Cajas abiertas, en camino o recibiéndose. */
    var cajas by mutableStateOf(emptyList<Caja>())
        private set
    /** Facturas del cliente (el equipo las consulta en Facturas). */
    var facturas by mutableStateOf(emptyList<Factura>())
        private set
    /** Supervisor: clientes. Admin: todos los usuarios. */
    var usuarios by mutableStateOf(emptyList<Usuario>())
        private set
    var notificaciones by mutableStateOf(emptyList<Notificacion>())
        private set
    var papelera by mutableStateOf(emptyList<ElementoPapelera>())
        private set
    var itemsCarrito by mutableStateOf(emptyList<ItemCarrito>())
        private set

    private val cargados = mutableStateMapOf<String, Boolean>()

    // Cosas buscadas una por una (historial o recién creadas, mientras llegan por la escucha).
    private val pedidosSueltos = mutableStateMapOf<Int, Pedido>()
    private val cajasSueltas = mutableStateMapOf<String, Caja>()
    private val pagosSueltos = mutableStateMapOf<String, Pago>()
    private val usuariosSueltos = mutableStateMapOf<String, Usuario>()
    private val facturasSueltas = mutableStateMapOf<Int, Factura>()

    fun cargado(lista: String): Boolean = cargados[lista] == true

    // ─── Derivados ──────────────────────────────────────────────────────────

    /** Carrito con los productos del catálogo de hoy (precio y agotado en vivo). */
    val carrito: List<LineaPedido>
        get() = itemsCarrito.mapNotNull { item ->
            productos.firstOrNull { it.id == item.productoId }?.let { LineaPedido(it, item.talla, item.cantidad) }
        }
    val prendasEnCarrito: Int get() = itemsCarrito.sumOf { it.cantidad }
    val noLeidas: Int get() = notificaciones.count { !it.leida }
    val porRevisar: Int get() = pagos.count { it.estado == EstadoPago.EN_REVISION }
    val clientesPorAprobar: List<Usuario>
        get() = usuarios.filter { it.rol == Rol.CLIENTE && !it.aprobado && it.activo }
    val unidadesPorComprar: Int
        get() = pedidos.filter { it.estado == EstadoPedido.PAGADO }.sumOf { p -> p.lineas.sumOf { it.porComprar } }

    /** Saldo a favor del cliente que inició sesión (en su moneda). */
    val miSaldo: Double
        get() {
            val u = usuario ?: return 0.0
            return Precios.saldoAFavor(pedidos.filter { it.clienteId == u.id }, u.moneda)
        }

    fun pedidoEnMemoria(numero: Int): Pedido? = pedidos.firstOrNull { it.numero == numero } ?: pedidosSueltos[numero]
    fun cajaEnMemoria(codigo: String): Caja? = cajas.firstOrNull { it.codigo == codigo } ?: cajasSueltas[codigo]
    fun pagoEnMemoria(id: String): Pago? = pagos.firstOrNull { it.id == id } ?: pagosSueltos[id]
    fun usuarioEnMemoria(id: String): Usuario? = usuarios.firstOrNull { it.id == id } ?: usuariosSueltos[id]
    fun productoEnMemoria(id: String): Producto? = productos.firstOrNull { it.id == id }
    fun facturaEnMemoria(numeroPedido: Int): Factura? =
        facturas.firstOrNull { it.numeroPedido == numeroPedido } ?: facturasSueltas[numeroPedido]

    // ─── Inicio y escuchas ──────────────────────────────────────────────────

    /** Se llama una vez desde MainActivity: empieza a escuchar la sesión de Firebase. */
    fun iniciar() {
        if (iniciado || esEjemplo) return
        iniciado = true
        AuthRepositorio.auth.addAuthStateListener(alCambiarCuenta)
        // Mientras la app está abierta, cada 5 minutos revisa si algún pedido se quedó sin pagar a tiempo.
        viewModelScope.launch {
            while (true) {
                delay(5 * 60_000L)
                revisarVencidos()
            }
        }
    }

    private val alCambiarCuenta = FirebaseAuth.AuthStateListener { auth ->
        val uid = auth.currentUser?.uid
        when {
            uid == null -> {
                detenerTodo()
                usuario = null
                sesion = Sesion.SIN_SESION
            }
            uid != uidEscuchado -> escucharUsuario(uid)
        }
    }

    private fun escucharUsuario(uid: String) {
        detenerTodo()
        uidEscuchado = uid
        sesion = Sesion.CARGANDO
        escuchaUsuario = UsuariosRepositorio.ref(uid).escuchar(
            Usuario::class.java,
            alRecibir = { u, existe, delServidor ->
                when {
                    u != null -> alCambiarUsuario(u)
                    existe -> {
                        avisoSesion = "Los datos de tu cuenta tienen un problema. Escríbele al administrador."
                        cerrarSesion()
                    }
                    delServidor && !registrando -> {
                        avisoSesion = "No encontramos los datos de tu cuenta. Regístrate de nuevo o escríbele a UyParce."
                        cerrarSesion()
                    }
                }
            },
            alFallar = { e ->
                Log.w(TAG, "No se pudo leer el usuario", e)
                avisoSesion = "No pudimos abrir tu cuenta: ${e.mensaje().replaceFirstChar { it.lowercase() }}."
                cerrarSesion()
            }
        )
    }

    private fun alCambiarUsuario(u: Usuario) {
        usuario = u
        when {
            !u.activo -> {
                avisoSesion = if (!u.aprobado) "Tu registro no fue aprobado. Si crees que es un error, escríbele a UyParce."
                else "Tu cuenta está desactivada. Escríbele al administrador."
                cerrarSesion()
            }
            !u.aprobado -> {
                detenerDatos()
                sesion = Sesion.EN_REVISION
            }
            else -> {
                if (rolEscuchado != u.rol) escucharDatos(u)
                sesion = Sesion.ACTIVA
            }
        }
    }

    /** Cada rol escucha solo lo que necesita. */
    private fun escucharDatos(u: Usuario) {
        detenerDatos()
        rolEscuchado = u.rol
        escucharDoc(Listas.AJUSTES, AjustesRepositorio.ref, Ajustes::class.java) { a, existe ->
            ajustes = a ?: Ajustes()
            hayAjustes = existe
        }
        escucharLista(Listas.PRODUCTOS, ProductosRepositorio.consultaTodos(), Producto::class.java) {
            productos = it.sortedBy { p -> p.nombre.lowercase() }
        }
        when (u.rol) {
            Rol.CLIENTE -> {
                escucharDoc(Listas.CARRITO, CarritoRepositorio.ref(u.id), Carrito::class.java) { c, _ -> itemsCarrito = c?.items.orEmpty() }
                escucharLista(Listas.PEDIDOS, PedidosRepositorio.consultaDeCliente(u.id), Pedido::class.java) { alRecibirPedidos(it) }
                escucharLista(Listas.PAGOS, PagosRepositorio.consultaDeCliente(u.id), Pago::class.java) {
                    pagos = it.sortedByDescending { p -> p.fecha }
                }
                escucharLista(Listas.FACTURAS, FacturasRepositorio.consultaDeCliente(u.id), Factura::class.java) {
                    facturas = it.sortedByDescending { f -> f.fecha }
                }
                escucharLista(Listas.NOTIFICACIONES, NotificacionesRepositorio.consultaDe(u.id), Notificacion::class.java) {
                    notificaciones = it.sortedByDescending { n -> n.fecha }
                }
            }
            Rol.OPERADOR -> {
                escucharLista(Listas.PEDIDOS, PedidosRepositorio.consultaActivos(), Pedido::class.java) { alRecibirPedidos(it) }
                escucharLista(Listas.CAJAS, CajasRepositorio.consultaActivas(), Caja::class.java) {
                    cajas = it.sortedByDescending { c -> c.codigo }
                }
            }
            Rol.SUPERVISOR, Rol.ADMIN -> {
                escucharLista(Listas.PEDIDOS, PedidosRepositorio.consultaActivos(), Pedido::class.java) { alRecibirPedidos(it) }
                escucharLista(Listas.CAJAS, CajasRepositorio.consultaActivas(), Caja::class.java) {
                    cajas = it.sortedByDescending { c -> c.codigo }
                }
                escucharLista(Listas.PAGOS, PagosRepositorio.consultaPorRevisar(), Pago::class.java) {
                    pagos = it.sortedBy { p -> p.fecha }
                }
                val consultaUsuarios = if (u.rol == Rol.ADMIN) UsuariosRepositorio.consultaTodos() else UsuariosRepositorio.consultaClientes()
                escucharLista(Listas.USUARIOS, consultaUsuarios, Usuario::class.java) {
                    usuarios = it.sortedBy { x -> x.nombre.lowercase() }
                }
                if (u.rol == Rol.ADMIN) {
                    escucharLista(Listas.PAPELERA, PapeleraRepositorio.consultaTodos(), ElementoPapelera::class.java) {
                        papelera = it.sortedByDescending { e -> e.fecha }
                    }
                }
            }
        }
    }

    private fun alRecibirPedidos(lista: List<Pedido>) {
        pedidos = lista.sortedByDescending { it.numero }
        lista.forEach { pedidosSueltos.remove(it.numero) } // ya llegaron por la escucha
        revisarVencidos()
    }

    private var revisandoVencidos = false

    /** Cancela los pedidos que no se pagaron a tiempo (lo hace el cliente con los suyos, o supervisor/admin). */
    private fun revisarVencidos() {
        val r = rol ?: return
        if (esEjemplo || r == Rol.OPERADOR || revisandoVencidos) return
        val vencidos = pedidos.filter { it.vencido() }
        if (vencidos.isEmpty()) return
        revisandoVencidos = true
        viewModelScope.launch {
            intentar { PedidosRepositorio.cancelarVencidos(vencidos) }
            revisandoVencidos = false
        }
    }

    private fun <T : Any> escucharLista(nombre: String, consulta: Query, clase: Class<T>, alRecibir: (List<T>) -> Unit) {
        escuchas += consulta.escuchar(
            clase,
            alRecibir = { lista, delServidor ->
                alRecibir(lista)
                marcarCargado(nombre, delServidor)
            },
            alFallar = { e -> alFallarEscucha(nombre, e) }
        )
    }

    private fun <T : Any> escucharDoc(nombre: String, ref: DocumentReference, clase: Class<T>, alRecibir: (T?, Boolean) -> Unit) {
        escuchas += ref.escuchar(
            clase,
            alRecibir = { objeto, existe, delServidor ->
                alRecibir(objeto, existe)
                marcarCargado(nombre, delServidor)
            },
            alFallar = { e -> alFallarEscucha(nombre, e) }
        )
    }

    /** Del servidor: listo de una vez. Del caché del celular (sin internet): se da por listo a los 4 segundos. */
    private fun marcarCargado(nombre: String, delServidor: Boolean) {
        if (cargados[nombre] == true) return
        if (delServidor) {
            cargados[nombre] = true
        } else {
            val miGeneracion = generacion
            viewModelScope.launch {
                delay(4000)
                if (miGeneracion == generacion) cargados[nombre] = true
            }
        }
    }

    private fun alFallarEscucha(nombre: String, e: FirebaseFirestoreException) {
        Log.w(TAG, "Falló la escucha de $nombre", e)
        cargados[nombre] = true
        if (sesion == Sesion.ACTIVA) aviso = "No se pudieron cargar los datos ($nombre): ${e.mensaje().replaceFirstChar { it.lowercase() }}"
    }

    private fun detenerDatos() {
        generacion++
        escuchas.forEach { it.remove() }
        escuchas.clear()
        rolEscuchado = null
        ajustes = Ajustes()
        hayAjustes = false
        productos = emptyList()
        pedidos = emptyList()
        pagos = emptyList()
        cajas = emptyList()
        facturas = emptyList()
        usuarios = emptyList()
        notificaciones = emptyList()
        papelera = emptyList()
        itemsCarrito = emptyList()
        cargados.clear()
        pedidosSueltos.clear()
        cajasSueltas.clear()
        pagosSueltos.clear()
        usuariosSueltos.clear()
        facturasSueltas.clear()
    }

    private fun detenerTodo() {
        detenerDatos()
        escuchaUsuario?.remove()
        escuchaUsuario = null
        uidEscuchado = null
    }

    /** Primero se detienen las escuchas y después se cierra la sesión (así no fallan por permisos). */
    fun cerrarSesion() {
        if (esEjemplo) return
        detenerTodo()
        usuario = null
        sesion = Sesion.SIN_SESION
        AuthRepositorio.cerrarSesion()
    }

    override fun onCleared() {
        if (iniciado) AuthRepositorio.auth.removeAuthStateListener(alCambiarCuenta)
        detenerTodo()
    }

    // ─── Cómo se ejecuta una acción ─────────────────────────────────────────

    /**
     * Ejecuta la acción dentro del ViewModel: si la persona sale de la pantalla a mitad, igual termina.
     * Nunca lanza excepciones: devuelve el resultado o el error (con mensaje en español vía mensaje()).
     */
    private suspend fun <T> ejecutar(segundos: Long = 20, bloque: suspend () -> T): Result<T> {
        if (esEjemplo) return Result.failure(ErrorNegocio("Vista previa: no se guarda nada"))
        return viewModelScope.async { intentar(segundos, bloque) }.await()
    }

    private fun yo(): Usuario = usuario ?: throw ErrorNegocio("Tu sesión se cerró. Vuelve a iniciar sesión.")

    // ─── Cuenta ─────────────────────────────────────────────────────────────

    suspend fun iniciarSesion(correo: String, clave: String): Result<Unit> {
        avisoSesion = null
        return ejecutar { AuthRepositorio.iniciarSesion(correo, clave) }
    }

    suspend fun registrar(datos: Usuario, clave: String): Result<Unit> {
        avisoSesion = null
        return ejecutar(30) {
            registrando = true
            try {
                AuthRepositorio.registrar(datos, clave)
                Unit
            } finally {
                registrando = false
            }
        }
    }

    suspend fun recuperarClave(correo: String): Result<Unit> = ejecutar { AuthRepositorio.recuperarClave(correo) }

    suspend fun actualizarMiContacto(telefono: String, ciudad: String, direccion: String): Result<Unit> = ejecutar {
        val u = yo()
        UsuariosRepositorio.actualizarContacto(u.id, telefono, ciudad, direccion, u)
    }

    // ─── Admin: cuentas, ajustes y papelera ─────────────────────────────────

    suspend fun aprobarCliente(cliente: Usuario, tipo: TipoCliente): Result<Unit> =
        ejecutar { UsuariosRepositorio.aprobarCliente(cliente, tipo, yo()) }

    suspend fun rechazarCliente(cliente: Usuario): Result<Unit> = ejecutar { UsuariosRepositorio.rechazarCliente(cliente, yo()) }

    /** Crea la cuenta de alguien del equipo; Firebase le envía un correo para crear su contraseña. */
    suspend fun crearUsuarioEquipo(context: Context, nombre: String, correo: String, rol: Rol): Result<Unit> = ejecutar(45) {
        val admin = yo()
        val uid = AuthRepositorio.crearCuentaEquipo(context.applicationContext, correo)
        UsuariosRepositorio.guardarNuevoEquipo(
            Usuario(uid, nombre.trim(), correo.trim(), rol, activo = true, aprobado = true, creadoEn = Timestamp.now()), admin
        )
    }

    suspend fun editarUsuario(u: Usuario, nombre: String, rol: Rol, tipo: TipoCliente?, activo: Boolean): Result<Unit> = ejecutar {
        val admin = yo()
        if (u.id == admin.id && (rol != Rol.ADMIN || !activo)) {
            throw ErrorNegocio("No puedes quitarte el rol de administrador ni desactivar tu propia cuenta")
        }
        val tipoFinal = if (rol == Rol.CLIENTE) tipo else null
        // Si cambia la moneda del cliente, su saldo a favor quedaría en la moneda equivocada.
        if (u.tipoCliente != null && tipoFinal?.moneda != u.tipoCliente.moneda) {
            val saldo = Precios.saldoAFavor(PedidosRepositorio.deCliente(u.id), u.moneda)
            if (saldo > 0) throw ErrorNegocio("${u.nombre} tiene saldo a favor en ${u.moneda}. Úsalo o devuélvelo antes de cambiarle el tipo de cliente.")
        }
        UsuariosRepositorio.editar(u, nombre, rol, tipoFinal, activo, admin)
    }

    suspend fun prepararBaseDeDatos(conProductosDeEjemplo: Boolean): Result<Unit> =
        ejecutar(30) { AjustesRepositorio.prepararBaseDeDatos(yo(), conProductosDeEjemplo) }

    suspend fun actualizarAjustes(campos: Map<String, Any?>, accion: String, detalle: String): Result<Unit> =
        ejecutar { AjustesRepositorio.actualizar(campos, yo(), accion, detalle) }

    suspend fun restaurar(elemento: ElementoPapelera): Result<Unit> = ejecutar { PapeleraRepositorio.restaurar(elemento, yo()) }

    suspend fun eliminarDefinitivo(elemento: ElementoPapelera): Result<Unit> =
        ejecutar { PapeleraRepositorio.eliminarDefinitivo(elemento, yo()) }

    // ─── Cliente ────────────────────────────────────────────────────────────

    fun agregarAlCarrito(producto: Producto, talla: String, cantidad: Int) {
        val i = itemsCarrito.indexOfFirst { it.productoId == producto.id && it.talla == talla }
        val nuevos = if (i >= 0) {
            itemsCarrito.mapIndexed { j, item -> if (j == i) item.copy(cantidad = (item.cantidad + cantidad).coerceAtMost(MAX_UNIDADES)) else item }
        } else itemsCarrito + ItemCarrito(producto.id, talla, cantidad.coerceIn(1, MAX_UNIDADES))
        guardarCarrito(nuevos)
    }

    /** Cambia la cantidad de una línea; con 0 la quita. Se identifica por producto y talla, no por posición. */
    fun cambiarCantidad(productoId: String, talla: String, cantidad: Int) {
        guardarCarrito(
            if (cantidad <= 0) itemsCarrito.filterNot { it.productoId == productoId && it.talla == talla }
            else itemsCarrito.map {
                if (it.productoId == productoId && it.talla == talla) it.copy(cantidad = cantidad.coerceAtMost(MAX_UNIDADES)) else it
            }
        )
    }

    private fun guardarCarrito(items: List<ItemCarrito>) {
        itemsCarrito = items // se ve de una vez; la escucha lo confirma
        if (esEjemplo) return
        val uid = usuario?.id ?: return
        viewModelScope.launch {
            intentar { CarritoRepositorio.guardar(uid, items) }
                .onFailure { aviso = "No se pudo guardar el carrito: ${it.mensaje().replaceFirstChar { c -> c.lowercase() }}" }
        }
    }

    /** Convierte el carrito en un pedido (precios congelados, saldo a favor aplicado). */
    suspend fun confirmarPedido(): Result<Pedido> = ejecutar(30) {
        val u = yo()
        if (!hayAjustes) throw ErrorNegocio("La tienda todavía no está lista para recibir pedidos. Intenta más tarde.")
        // Si algún producto del carrito ya no existe, se quita antes de confirmar.
        val validos = itemsCarrito.filter { item -> productos.any { it.id == item.productoId } }
        if (validos.size != itemsCarrito.size) {
            CarritoRepositorio.guardar(u.id, validos)
            itemsCarrito = validos
        }
        PedidosRepositorio.crear(u, ajustes, miSaldo).also { pedidosSueltos[it.numero] = it }
    }

    suspend fun reportarPago(
        numero: Int, idPago: String, medio: String, referencia: String, fechaPago: String, foto: FotoNueva?
    ): Result<Unit> = ejecutar(90) { PedidosRepositorio.reportarPago(numero, yo(), idPago, medio, referencia, fechaPago, foto) }

    suspend fun cancelarPedido(numero: Int): Result<Unit> =
        ejecutar { PedidosRepositorio.cancelar(numero, yo().nombre, "Cancelado por el cliente") }

    suspend fun confirmarEntrega(numero: Int): Result<Unit> = ejecutar { PedidosRepositorio.confirmarEntrega(numero, yo()) }

    suspend fun resolverFaltante(numero: Int, faltanteId: String, tipo: TipoResolucion, cuenta: String): Result<Unit> =
        ejecutar { PedidosRepositorio.resolverFaltante(numero, faltanteId, tipo, cuenta) }

    fun marcarLeida(notificacion: Notificacion) {
        if (notificacion.leida || esEjemplo) return
        viewModelScope.launch { intentar { NotificacionesRepositorio.marcarLeida(notificacion.id) } }
    }

    fun marcarTodasLeidas() {
        if (esEjemplo) return
        val ids = notificaciones.filter { !it.leida }.map { it.id }
        viewModelScope.launch { intentar { NotificacionesRepositorio.marcarTodasLeidas(ids) } }
    }

    // ─── Supervisor ─────────────────────────────────────────────────────────

    /** Aprueba el pago y crea la factura. Devuelve el número de la factura. */
    suspend fun aprobarPago(pago: Pago): Result<String> = ejecutar(30) { PagosRepositorio.aprobar(pago.id, yo()) }

    suspend fun rechazarPago(pago: Pago, motivo: String): Result<Unit> =
        ejecutar { PagosRepositorio.rechazar(pago.id, motivo, yo(), ajustes.horasParaPagar) }

    suspend fun despachar(numero: Int, transportadora: String, guia: String): Result<Unit> =
        ejecutar { PedidosRepositorio.despachar(numero, transportadora, guia, yo()) }

    suspend fun marcarDevuelto(numero: Int, faltanteId: String): Result<Unit> =
        ejecutar { PedidosRepositorio.marcarDevuelto(numero, faltanteId, yo()) }

    suspend fun crearGasto(gasto: Gasto): Result<Unit> = ejecutar { GastosRepositorio.crear(gasto, yo()) }

    suspend fun actualizarContactoCliente(cliente: Usuario, telefono: String, ciudad: String, direccion: String): Result<Unit> =
        ejecutar { UsuariosRepositorio.actualizarContacto(cliente.id, telefono, ciudad, direccion, yo()) }

    suspend fun recibirPrenda(caja: Caja, productoId: String, talla: String): Result<PrendaCaja> =
        ejecutar { CajasRepositorio.recibirPrenda(caja.codigo, productoId, talla) }

    /** Cierra la recepción. Devuelve cuántos pedidos quedaron con faltantes. */
    suspend fun cerrarRecepcion(caja: Caja): Result<Int> = ejecutar(30) { CajasRepositorio.cerrarRecepcion(caja.codigo, yo()) }

    // ─── Operador ───────────────────────────────────────────────────────────

    suspend fun guardarProducto(
        producto: Producto, esNuevo: Boolean, fotosNuevas: List<FotoNueva>, fotosQuitadas: List<String>
    ): Result<Unit> = ejecutar(90) { ProductosRepositorio.guardar(producto, esNuevo, fotosNuevas, fotosQuitadas, yo()) }

    /** Devuelve a cuántos clientes se les avisó. */
    suspend fun marcarAgotado(producto: Producto): Result<Int> = ejecutar { ProductosRepositorio.marcarAgotado(producto, yo()) }

    suspend fun marcarDisponible(producto: Producto): Result<Unit> = ejecutar { ProductosRepositorio.marcarDisponible(producto, yo()) }

    suspend fun eliminarProducto(producto: Producto): Result<Unit> = ejecutar { ProductosRepositorio.eliminar(producto, yo()) }

    suspend fun guardarCodigo(producto: Producto, talla: String, codigo: String): Result<Unit> =
        ejecutar { ProductosRepositorio.guardarCodigo(producto, talla, codigo, productos, yo()) }

    /** Pedidos pagados que esperan ese producto y talla, del más antiguo al más nuevo. */
    fun candidatosCompra(productoId: String, talla: String): List<Int> =
        pedidos.filter { p ->
            p.estado == EstadoPedido.PAGADO && p.lineas.any { it.producto.id == productoId && it.talla == talla && it.porComprar > 0 }
        }.sortedBy { it.numero }.map { it.numero }

    /** Registra una unidad comprada. Devuelve el pedido al que se asignó. */
    suspend fun registrarCompra(productoId: String, talla: String): Result<Int> =
        ejecutar { PedidosRepositorio.registrarCompra(productoId, talla, candidatosCompra(productoId, talla)) }

    /** Devuelve cuántos pedidos se afectaron. */
    suspend fun marcarNoConseguido(productoId: String, talla: String): Result<Int> =
        ejecutar(30) { PedidosRepositorio.marcarNoConseguido(productoId, talla, candidatosCompra(productoId, talla), yo()) }

    suspend fun siguienteCodigoCaja(): Result<String> = ejecutar { CajasRepositorio.siguienteCodigo() }

    suspend fun crearCaja(codigo: String, tipo: TipoCaja, cliente: Usuario?): Result<Unit> =
        ejecutar { CajasRepositorio.crear(codigo, tipo, cliente, yo()) }

    /** Pedidos que pueden ir en la caja y tienen esa prenda comprada sin empacar, del más antiguo al más nuevo. */
    fun candidatosEmpaque(caja: Caja, productoId: String, talla: String): List<Int> =
        pedidos.filter { p ->
            (p.estado == EstadoPedido.PAGADO || p.estado == EstadoPedido.COMPRADO) &&
                (if (caja.tipo == TipoCaja.MAYORISTA) p.clienteId == caja.clienteId else p.moneda == Moneda.COP) &&
                p.lineas.any { it.producto.id == productoId && it.talla == talla && it.porEmpacar > 0 }
        }.sortedBy { it.numero }.map { it.numero }

    suspend fun agregarPrenda(caja: Caja, productoId: String, talla: String): Result<PrendaCaja> =
        ejecutar { CajasRepositorio.agregarPrenda(caja.codigo, productoId, talla, candidatosEmpaque(caja, productoId, talla)) }

    suspend fun quitarPrenda(caja: Caja, prendaId: String): Result<Unit> = ejecutar { CajasRepositorio.quitarPrenda(caja.codigo, prendaId) }

    suspend fun sellarCaja(caja: Caja, pesoLb: Double?, tipoEnvio: TipoEnvio?): Result<Unit> =
        ejecutar(30) { CajasRepositorio.sellar(caja.codigo, pesoLb, tipoEnvio, ajustes, yo()) }

    suspend fun eliminarCaja(caja: Caja): Result<Unit> = ejecutar { CajasRepositorio.eliminarVacia(caja.codigo, yo()) }

    // ─── Búsquedas de uno en uno (para pantallas de detalle) ────────────────

    suspend fun buscarPedido(numero: Int): Pedido? {
        pedidoEnMemoria(numero)?.let { return it }
        if (esEjemplo) return null
        return intentar { PedidosRepositorio.obtener(numero) }.getOrNull()?.also { pedidosSueltos[numero] = it }
    }

    suspend fun buscarCaja(codigo: String): Caja? {
        cajaEnMemoria(codigo)?.let { return it }
        if (esEjemplo) return null
        return intentar { CajasRepositorio.obtener(codigo) }.getOrNull()?.also { cajasSueltas[codigo] = it }
    }

    suspend fun buscarPago(id: String): Pago? {
        pagoEnMemoria(id)?.let { return it }
        if (esEjemplo) return null
        return intentar { PagosRepositorio.obtener(id) }.getOrNull()?.also { pagosSueltos[id] = it }
    }

    suspend fun buscarUsuario(id: String): Usuario? {
        usuarioEnMemoria(id)?.let { return it }
        if (esEjemplo) return null
        return intentar { UsuariosRepositorio.obtener(id) }.getOrNull()?.also { usuariosSueltos[id] = it }
    }

    suspend fun buscarFactura(numeroPedido: Int): Factura? {
        facturaEnMemoria(numeroPedido)?.let { return it }
        if (esEjemplo) return null
        val clienteId = usuario?.takeIf { it.rol == Rol.CLIENTE }?.id
        return intentar { FacturasRepositorio.dePedido(numeroPedido, clienteId) }.getOrNull()?.also { facturasSueltas[numeroPedido] = it }
    }

    // ─── Historial (se consulta al abrir la pantalla, no se escucha en vivo) ─

    suspend fun pedidosPagadosDesde(desde: Timestamp): Result<List<Pedido>> =
        if (esEjemplo) Result.success(DatosEjemplo.pedidos.filter { it.pagadoEn != null }) else ejecutar { PedidosRepositorio.pagadosDesde(desde) }

    suspend fun pedidosDeCliente(uid: String): Result<List<Pedido>> =
        if (esEjemplo) Result.success(DatosEjemplo.pedidos.filter { it.clienteId == uid }) else ejecutar { PedidosRepositorio.deCliente(uid) }

    suspend fun pedidosConSaldo(): Result<List<Pedido>> =
        if (esEjemplo) Result.success(DatosEjemplo.pedidos.filter { it.conSaldo }) else ejecutar { PedidosRepositorio.conSaldo() }

    suspend fun pagosRecientes(): Result<List<Pago>> =
        if (esEjemplo) Result.success(DatosEjemplo.pagos) else ejecutar { PagosRepositorio.recientes() }

    suspend fun facturasRecientes(): Result<List<Factura>> =
        if (esEjemplo) Result.success(DatosEjemplo.facturas) else ejecutar { FacturasRepositorio.recientes() }

    suspend fun gastosRecientes(): Result<List<Gasto>> =
        if (esEjemplo) Result.success(DatosEjemplo.gastos) else ejecutar { GastosRepositorio.recientes() }

    suspend fun gastosDesde(desde: Timestamp): Result<List<Gasto>> =
        if (esEjemplo) Result.success(DatosEjemplo.gastos) else ejecutar { GastosRepositorio.desde(desde) }

    suspend fun cajasRecibidas(): Result<List<Caja>> =
        if (esEjemplo) Result.success(DatosEjemplo.cajas.filter { it.estado == EstadoCaja.RECIBIDA }) else ejecutar { CajasRepositorio.recibidasRecientes() }

    suspend fun auditoria(): Result<List<RegistroAuditoria>> =
        if (esEjemplo) Result.success(DatosEjemplo.auditoria) else ejecutar { AuditoriaRepositorio.recientes() }

    companion object {
        private const val TAG = "UyParce"
        const val MAX_UNIDADES = 20

        /** Estado con los datos de ejemplo, para los @Preview (no toca Firebase). */
        fun ejemplo(rol: Rol = Rol.CLIENTE): EstadoApp = EstadoApp().apply {
            esEjemplo = true
            val yo = when (rol) {
                Rol.ADMIN -> DatosEjemplo.admin
                Rol.OPERADOR -> DatosEjemplo.operador
                Rol.SUPERVISOR -> DatosEjemplo.supervisor
                Rol.CLIENTE -> DatosEjemplo.laura
            }
            usuario = yo
            sesion = Sesion.ACTIVA
            ajustes = DatosEjemplo.ajustes
            hayAjustes = true
            productos = DatosEjemplo.productos
            if (rol == Rol.CLIENTE) {
                pedidos = DatosEjemplo.pedidos.filter { it.clienteId == yo.id }.sortedByDescending { it.numero }
                pagos = DatosEjemplo.pagos.filter { it.clienteId == yo.id }
                facturas = DatosEjemplo.facturas.filter { it.clienteId == yo.id }
                notificaciones = DatosEjemplo.notificaciones
                itemsCarrito = DatosEjemplo.carritoLaura
            } else {
                pedidos = DatosEjemplo.pedidos.filter { it.estado in EstadoPedido.activos }.sortedByDescending { it.numero }
                pagos = DatosEjemplo.pagos.filter { it.estado == EstadoPago.EN_REVISION }
                cajas = DatosEjemplo.cajas.filter { it.estado != EstadoCaja.RECIBIDA }
                usuarios = if (rol == Rol.ADMIN) DatosEjemplo.usuarios else DatosEjemplo.clientes
                papelera = DatosEjemplo.papelera
            }
            listOf(
                Listas.AJUSTES, Listas.PRODUCTOS, Listas.CARRITO, Listas.PEDIDOS, Listas.PAGOS, Listas.CAJAS,
                Listas.FACTURAS, Listas.USUARIOS, Listas.NOTIFICACIONES, Listas.PAPELERA
            ).forEach { cargados[it] = true }
        }
    }
}

/** Estado de la app disponible en todas las pantallas. El valor por defecto (datos de ejemplo) es para los @Preview. */
val LocalEstadoApp = staticCompositionLocalOf { EstadoApp.ejemplo() }
