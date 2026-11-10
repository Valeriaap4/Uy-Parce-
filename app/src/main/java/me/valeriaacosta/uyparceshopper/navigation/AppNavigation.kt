package me.valeriaacosta.uyparceshopper.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.launch
import me.valeriaacosta.uyparceshopper.data.EstadoApp
import me.valeriaacosta.uyparceshopper.data.LocalEstadoApp
import me.valeriaacosta.uyparceshopper.data.Sesion
import me.valeriaacosta.uyparceshopper.data.models.Rol
import me.valeriaacosta.uyparceshopper.ui.components.LocalMensaje
import me.valeriaacosta.uyparceshopper.ui.screens.admin.*
import me.valeriaacosta.uyparceshopper.ui.screens.auth.*
import me.valeriaacosta.uyparceshopper.ui.screens.cliente.*
import me.valeriaacosta.uyparceshopper.ui.screens.compartidas.*
import me.valeriaacosta.uyparceshopper.ui.screens.operador.*
import me.valeriaacosta.uyparceshopper.ui.screens.supervisor.*
import me.valeriaacosta.uyparceshopper.ui.theme.*

/*
 * Navegación de toda la app. Sigue el mismo patrón del NavHost visto en clase
 * (rememberNavController + NavHost + composable(route = ...)), pero vive en su propio
 * archivo porque son 54 pantallas. Las pantallas no conocen el NavController:
 * reciben lambdas (onClickX) y aquí se decide a dónde lleva cada una.
 *
 * Reglas:
 *  - La sesión manda: mientras carga se ve el Splash; sin sesión solo login, registro y
 *    recuperar; con la cuenta en revisión, la pantalla de revisión; con sesión activa, las
 *    pantallas de su rol. Iniciar o cerrar sesión borra el historial.
 *  - Cada rol tiene 4 pestañas (Routes.pestanas). En ellas se ve la barra inferior y no hay flecha de volver.
 *  - Cualquier otra pantalla tiene flecha de volver y no muestra la barra.
 */
@Composable
fun AppNavigation() {
    val myNavController = rememberNavController()
    val estado = LocalEstadoApp.current
    val entrada by myNavController.currentBackStackEntryAsState()
    val rutaActual = entrada?.destination?.route
    val pestanas = estado.rol?.let { Routes.pestanas(it) } ?: emptyList()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    /** Navegación normal. launchSingleTop evita abrir la misma pantalla dos veces con un doble toque. */
    fun ir(ruta: String) = myNavController.navigate(route = ruta) { launchSingleTop = true }

    fun volver() { myNavController.popBackStack() }

    /** Flecha de volver: las pestañas del rol no la tienen; el resto sí. */
    fun atras(ruta: String): (() -> Unit)? = if (pestanas.any { it.ruta == ruta }) null else ::volver

    /** Vuelve a la pestaña indicada descartando el flujo actual (p. ej. al terminar de pagar). */
    fun terminarFlujoEn(pestana: String) {
        myNavController.navigate(route = pestana) {
            pestanas.firstOrNull()?.let { popUpTo(it.ruta) }
            launchSingleTop = true
        }
    }

    /** Lleva a una pantalla de entrada (login o revisión) borrando el historial y lo que recordaban las pestañas. */
    fun irSinHistorial(ruta: String) {
        myNavController.navigate(route = ruta) { popUpTo(myNavController.graph.id) { inclusive = true } }
        Rol.entries.flatMap { Routes.pestanas(it) }.map { it.ruta }.distinct().forEach { myNavController.clearBackStack(it) }
    }

    // La sesión decide a dónde se puede estar (también si Android cerró la app y la vuelve a abrir).
    LaunchedEffect(estado.sesion, estado.rol, rutaActual) {
        val ruta = rutaActual ?: return@LaunchedEffect
        when (estado.sesion) {
            Sesion.CARGANDO -> Unit
            Sesion.SIN_SESION -> if (ruta !in Routes.sinSesion) irSinHistorial(Routes.LOGIN)
            Sesion.EN_REVISION -> if (ruta != Routes.REVISION) irSinHistorial(Routes.REVISION)
            Sesion.ACTIVA -> {
                val rol = estado.rol ?: return@LaunchedEffect
                if (ruta in Routes.deEntrada || !Routes.permitida(ruta, rol)) irAInicio(myNavController, rol)
            }
        }
    }

    fun mostrar(m: String) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(m)
        }
    }

    // Avisos que llegan desde el estado (por ejemplo, si falla una escucha de datos).
    LaunchedEffect(estado.aviso) {
        estado.aviso?.let {
            mostrar(it)
            estado.aviso = null
        }
    }

    CompositionLocalProvider(LocalMensaje provides { m -> mostrar(m) }) {
        Scaffold(
            contentWindowInsets = WindowInsets(0),
            containerColor = Bg,
            snackbarHost = { SnackbarHost(snackbar) { Snackbar(it, containerColor = Navy, contentColor = Color.White) } },
            bottomBar = {
                if (pestanas.any { it.ruta == rutaActual }) BarraInferior(myNavController, estado, pestanas, rutaActual)
            }
        ) { padding ->
            NavHost(
                navController = myNavController,
                startDestination = Routes.SPLASH,
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                // ── Autenticación y comunes ─────────────────────────────────
                composable(route = Routes.SPLASH) {
                    SplashScreen()
                }
                composable(route = Routes.LOGIN) {
                    LoginScreen(
                        onClickRegister = { ir(Routes.REGISTRO) },
                        onClickForgotPassword = { ir(Routes.RECUPERAR) }
                    )
                }
                composable(route = Routes.REGISTRO) {
                    RegisterScreen(onClickBack = ::volver)
                }
                composable(route = Routes.REVISION) {
                    CuentaRevisionScreen(onClickVolver = { estado.cerrarSesion() })
                }
                composable(route = Routes.RECUPERAR) {
                    ForgotPasswordScreen(onClickBack = ::volver)
                }
                composable(route = Routes.PERFIL) {
                    PerfilScreen(
                        onClickBack = atras(Routes.PERFIL),
                        onClickNotificaciones = { ir(Routes.NOTIFICACIONES) },
                        onLogout = { estado.cerrarSesion() }
                    )
                }
                composable(route = Routes.PERFIL_EDITAR) {
                    PerfilScreen(
                        onClickBack = ::volver,
                        onClickNotificaciones = { ir(Routes.NOTIFICACIONES) },
                        onLogout = { estado.cerrarSesion() },
                        onGuardado = ::volver
                    )
                }
                composable(route = Routes.NOTIFICACIONES) {
                    NotificacionesScreen(
                        onClickBack = ::volver,
                        onClickNotificacion = { n ->
                            val numero = n.numeroPedido
                            if (numero != null) ir(Routes.detallePedido(numero)) else terminarFlujoEn(Routes.CARRITO)
                        }
                    )
                }
                composable(route = Routes.DETALLE_PEDIDO, arguments = entero("numero")) { entry ->
                    val numero = entry.int("numero")
                    DetallePedidoScreen(
                        numero = numero,
                        onClickBack = ::volver,
                        onClickPagar = { ir(Routes.pagar(numero)) },
                        onClickFactura = { ir(Routes.factura(numero)) },
                        onClickFaltante = { ir(Routes.faltante(numero)) }
                    )
                }
                composable(route = Routes.FACTURA, arguments = entero("numero")) { entry ->
                    FacturaScreen(numero = entry.int("numero"), onClickBack = ::volver)
                }

                // ── Cliente ────────────────────────────────────────────────
                composable(route = Routes.CATALOGO) {
                    CatalogoScreen(
                        onClickProducto = { id -> ir(Routes.producto(id)) },
                        onClickNotificaciones = { ir(Routes.NOTIFICACIONES) }
                    )
                }
                composable(route = Routes.PRODUCTO, arguments = texto("id")) { entry ->
                    DetalleProductoScreen(
                        id = entry.str("id").orEmpty(),
                        onClickBack = ::volver,
                        onAgregado = ::volver
                    )
                }
                composable(route = Routes.CARRITO) {
                    CarritoScreen(
                        onClickProducto = { id -> ir(Routes.producto(id)) },
                        onClickContinuar = { ir(Routes.CONFIRMAR) },
                        onClickVerCatalogo = { irAPestana(myNavController, Routes.CATALOGO, pestanas) }
                    )
                }
                composable(route = Routes.CONFIRMAR) {
                    ConfirmarPedidoScreen(
                        onClickBack = ::volver,
                        onClickCambiarDireccion = { ir(Routes.PERFIL_EDITAR) },
                        onPedidoConfirmado = { numero ->
                            // El carrito ya se convirtió en pedido: se sale del flujo del carrito.
                            myNavController.navigate(route = Routes.pagar(numero)) { popUpTo(Routes.CATALOGO) }
                        }
                    )
                }
                composable(route = Routes.PAGAR, arguments = entero("numero")) { entry ->
                    val numero = entry.int("numero")
                    PagarPedidoScreen(
                        numero = numero,
                        onClickBack = ::volver,
                        onClickReportar = { ir(Routes.reportarPago(numero)) }
                    )
                }
                composable(route = Routes.REPORTAR_PAGO, arguments = entero("numero")) { entry ->
                    val numero = entry.int("numero")
                    ReportarPagoScreen(
                        numero = numero,
                        onClickBack = ::volver,
                        onEnviado = {
                            // Se descarta Pagar/Reportar y se muestra el pedido desde "Mis pedidos".
                            terminarFlujoEn(Routes.MIS_PEDIDOS)
                            ir(Routes.detallePedido(numero))
                        }
                    )
                }
                composable(route = Routes.MIS_PEDIDOS) {
                    MisPedidosScreen(onClickPedido = { numero -> ir(Routes.detallePedido(numero)) })
                }
                composable(route = Routes.FALTANTE, arguments = entero("numero")) { entry ->
                    ResolverFaltanteScreen(
                        numero = entry.int("numero"),
                        onClickBack = ::volver,
                        onResuelto = ::volver
                    )
                }

                // ── Operador ───────────────────────────────────────────────
                composable(route = Routes.OP_CATALOGO) {
                    OpCatalogoScreen(
                        onClickBack = atras(Routes.OP_CATALOGO),
                        onClickProducto = { id -> ir(Routes.opProducto(id)) },
                        onClickNuevo = { ir(Routes.opProducto()) }
                    )
                }
                composable(route = Routes.OP_PRODUCTO, arguments = texto("id", opcional = true)) { entry ->
                    OpProductoScreen(
                        id = entry.str("id"),
                        onClickBack = ::volver,
                        onGuardado = ::volver
                    )
                }
                composable(route = Routes.POR_COMPRAR) {
                    PorComprarScreen(
                        onClickBack = atras(Routes.POR_COMPRAR),
                        onClickRegistrar = { ir(Routes.REGISTRAR_COMPRA) }
                    )
                }
                composable(route = Routes.REGISTRAR_COMPRA) {
                    RegistrarCompraScreen(onClickBack = ::volver)
                }
                composable(route = Routes.CAJAS) {
                    CajasScreen(
                        onClickBack = atras(Routes.CAJAS),
                        onClickCaja = { codigo -> ir(Routes.empacar(codigo)) },
                        onClickCrear = { ir(Routes.CREAR_CAJA) }
                    )
                }
                composable(route = Routes.CREAR_CAJA) {
                    CrearCajaScreen(
                        onClickBack = ::volver,
                        // "Crear caja" se reemplaza por "Empacar": al volver se llega a la lista de cajas.
                        onCreada = { codigo -> myNavController.navigate(route = Routes.empacar(codigo)) { popUpTo(Routes.CAJAS) } }
                    )
                }
                composable(route = Routes.EMPACAR, arguments = texto("codigo")) { entry ->
                    EmpacarCajaScreen(
                        codigo = entry.str("codigo").orEmpty(),
                        onClickBack = ::volver,
                        onSellada = { myNavController.popBackStack(Routes.CAJAS, inclusive = false) }
                    )
                }

                // ── Supervisor ─────────────────────────────────────────────
                composable(route = Routes.PAGOS) {
                    PagosScreen(
                        onClickBack = atras(Routes.PAGOS),
                        onClickPago = { id -> ir(Routes.revisarPago(id)) }
                    )
                }
                composable(route = Routes.REVISAR_PAGO, arguments = texto("id")) { entry ->
                    RevisarPagoScreen(
                        id = entry.str("id").orEmpty(),
                        onClickBack = ::volver,
                        onClickVerPedido = { numero -> ir(Routes.detallePedido(numero)) },
                        onRevisado = ::volver
                    )
                }
                composable(route = Routes.CAJAS_EN_CAMINO) {
                    CajasEnCaminoScreen(
                        onClickBack = atras(Routes.CAJAS_EN_CAMINO),
                        onClickCaja = { codigo -> ir(Routes.recibirCaja(codigo)) }
                    )
                }
                composable(route = Routes.RECIBIR_CAJA, arguments = texto("codigo")) { entry ->
                    val codigo = entry.str("codigo").orEmpty()
                    RecibirCajaScreen(
                        codigo = codigo,
                        onClickBack = ::volver,
                        onClickTerminar = { ir(Routes.resumenRecepcion(codigo)) }
                    )
                }
                composable(route = Routes.RESUMEN_RECEPCION, arguments = texto("codigo")) { entry ->
                    ResumenRecepcionScreen(
                        codigo = entry.str("codigo").orEmpty(),
                        onClickBack = ::volver,
                        onCerrada = { myNavController.popBackStack(Routes.CAJAS_EN_CAMINO, inclusive = false) }
                    )
                }
                composable(route = Routes.POR_DESPACHAR) {
                    PorDespacharScreen(
                        onClickBack = atras(Routes.POR_DESPACHAR),
                        onClickDespachar = { numero -> ir(Routes.despachar(numero)) },
                        onClickPedido = { numero -> ir(Routes.detallePedido(numero)) }
                    )
                }
                composable(route = Routes.DESPACHAR, arguments = entero("numero")) { entry ->
                    DespacharScreen(
                        numero = entry.int("numero"),
                        onClickBack = ::volver,
                        onDespachado = ::volver
                    )
                }
                composable(route = Routes.SUP_MAS) {
                    SupMasScreen(
                        onClickClientes = { ir(Routes.CLIENTES) },
                        onClickFacturas = { ir(Routes.FACTURAS) },
                        onClickCartera = { ir(Routes.CARTERA) },
                        onClickGastos = { ir(Routes.GASTOS) },
                        onClickReportes = { ir(Routes.REPORTES) },
                        onClickPerfil = { ir(Routes.PERFIL) }
                    )
                }
                composable(route = Routes.CLIENTES) {
                    ClientesScreen(
                        onClickBack = ::volver,
                        onClickCliente = { id -> ir(Routes.detalleCliente(id)) }
                    )
                }
                composable(route = Routes.DETALLE_CLIENTE, arguments = texto("id")) { entry ->
                    DetalleClienteScreen(
                        id = entry.str("id").orEmpty(),
                        onClickBack = ::volver,
                        onClickPedido = { numero -> ir(Routes.detallePedido(numero)) }
                    )
                }
                composable(route = Routes.FACTURAS) {
                    FacturasScreen(
                        onClickBack = ::volver,
                        onClickFactura = { numero -> ir(Routes.factura(numero)) }
                    )
                }
                composable(route = Routes.CARTERA) {
                    CarteraScreen(
                        onClickBack = ::volver,
                        onClickCliente = { id -> ir(Routes.detalleCliente(id)) }
                    )
                }
                composable(route = Routes.GASTOS) {
                    GastosScreen(
                        onClickBack = ::volver,
                        onClickNuevo = { ir(Routes.NUEVO_GASTO) }
                    )
                }
                composable(route = Routes.NUEVO_GASTO) {
                    NuevoGastoScreen(onClickBack = ::volver, onGuardado = ::volver)
                }
                composable(route = Routes.REPORTES) {
                    ReportesScreen(onClickBack = ::volver)
                }

                // ── Admin ──────────────────────────────────────────────────
                composable(route = Routes.ADM_INICIO) {
                    AdminInicioScreen()
                }
                composable(route = Routes.POR_APROBAR) {
                    PorAprobarScreen(onClickCliente = { id -> ir(Routes.aprobarCliente(id)) })
                }
                composable(route = Routes.APROBAR_CLIENTE, arguments = texto("id")) { entry ->
                    AprobarClienteScreen(
                        id = entry.str("id").orEmpty(),
                        onClickBack = ::volver,
                        onListo = ::volver
                    )
                }
                composable(route = Routes.USUARIOS) {
                    UsuariosScreen(onClickUsuario = { id -> ir(Routes.editarUsuario(id)) })
                }
                composable(route = Routes.EDITAR_USUARIO, arguments = texto("id", opcional = true)) { entry ->
                    EditarUsuarioScreen(
                        id = entry.str("id"),
                        onClickBack = ::volver,
                        onGuardado = ::volver
                    )
                }
                composable(route = Routes.ADM_MAS) {
                    AdminMasScreen(onClickOpcion = { ruta -> ir(ruta) })
                }
                composable(route = Routes.AJUSTES) {
                    AjustesScreen(onClickBack = ::volver, onClickOpcion = { ruta -> ir(ruta) })
                }
                composable(route = Routes.AJ_PRECIOS) { AjPreciosScreen(onClickBack = ::volver, onGuardado = ::volver) }
                composable(route = Routes.AJ_COMISIONES) { AjComisionesScreen(onClickBack = ::volver, onGuardado = ::volver) }
                composable(route = Routes.AJ_ENVIO) { AjEnvioScreen(onClickBack = ::volver, onGuardado = ::volver) }
                composable(route = Routes.AJ_ZONAS) { AjZonasScreen(onClickBack = ::volver, onGuardado = ::volver) }
                composable(route = Routes.AJ_MEDIOS) { AjMediosScreen(onClickBack = ::volver) }
                composable(route = Routes.AJ_CATEGORIAS) { AjCategoriasScreen(onClickBack = ::volver, onGuardado = ::volver) }
                composable(route = Routes.PAPELERA) { PapeleraScreen(onClickBack = ::volver) }
                composable(route = Routes.AUDITORIA) { AuditoriaScreen(onClickBack = ::volver) }
            }
        }
    }
}

// ─── Barra inferior con las 4 pestañas del rol ──────────────────────────────

@Composable
private fun BarraInferior(nav: NavHostController, estado: EstadoApp, pestanas: List<Pestana>, rutaActual: String?) {
    NavigationBar(containerColor = Color.White) {
        pestanas.forEach { p ->
            // Contadores: lo que espera en cada pestaña.
            val contador = when (p.ruta) {
                Routes.CARRITO -> estado.prendasEnCarrito
                Routes.PAGOS -> estado.porRevisar
                Routes.POR_APROBAR -> estado.clientesPorAprobar.size
                Routes.POR_COMPRAR -> estado.unidadesPorComprar
                else -> 0
            }
            NavigationBarItem(
                selected = p.ruta == rutaActual,
                onClick = { irAPestana(nav, p.ruta, pestanas) },
                icon = {
                    if (contador > 0)
                        BadgedBox(badge = { Badge(containerColor = Yellow, contentColor = Navy) { Text(if (contador > 99) "99+" else "$contador") } }) {
                            Icon(p.icono, null)
                        }
                    else Icon(p.icono, null)
                },
                label = { Text(p.titulo, fontWeight = FontWeight.SemiBold) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Navy, selectedTextColor = Navy, indicatorColor = Yellow,
                    unselectedIconColor = Muted, unselectedTextColor = Muted
                )
            )
        }
    }
}

// ─── Ayudantes de navegación ────────────────────────────────────────────────

/** Va al inicio del rol y borra el historial (no se puede volver al login con "atrás"). */
private fun irAInicio(nav: NavHostController, rol: Rol) {
    nav.navigate(route = Routes.inicioDe(rol)) { popUpTo(nav.graph.id) { inclusive = true } }
}

/** Cambia de pestaña guardando lo que tenía abierto cada una. */
private fun irAPestana(nav: NavHostController, ruta: String, pestanas: List<Pestana>) {
    nav.navigate(route = ruta) {
        pestanas.firstOrNull()?.let { popUpTo(it.ruta) { saveState = true } }
        launchSingleTop = true
        restoreState = true
    }
}

private fun entero(nombre: String) = listOf(navArgument(nombre) { type = NavType.IntType })

private fun texto(nombre: String, opcional: Boolean = false) = listOf(navArgument(nombre) {
    type = NavType.StringType
    nullable = opcional
    if (opcional) defaultValue = null
})

private fun NavBackStackEntry.int(nombre: String) = arguments?.getInt(nombre) ?: 0
private fun NavBackStackEntry.str(nombre: String) = arguments?.getString(nombre)
