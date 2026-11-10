package me.valeriaacosta.uyparceshopper.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector
import me.valeriaacosta.uyparceshopper.data.models.Rol

/** Todas las rutas de la app. Las que llevan {argumento} se arman con las funciones de abajo. */
object Routes {
    // Autenticación y comunes
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTRO = "registro"
    const val REVISION = "revision"
    const val RECUPERAR = "recuperar"
    const val PERFIL = "perfil"
    /** Perfil abierto desde otra pantalla (p. ej. "Cambiar dirección"): con flecha de volver y sin barra de pestañas. */
    const val PERFIL_EDITAR = "perfil/editar"

    /** Pantallas a las que se puede entrar sin sesión iniciada. */
    val sinSesion = setOf(LOGIN, REGISTRO, RECUPERAR)

    /** Pantallas de entrada: con la sesión activa no tienen sentido y se salta al inicio del rol. */
    val deEntrada = setOf(SPLASH, LOGIN, REGISTRO, REVISION, RECUPERAR)

    /**
     * Si un rol puede estar en una ruta. El admin también usa las pantallas del operador y del
     * supervisor (desde "Más"). Las comunes (perfil, pedido, factura…) son de todos.
     */
    fun permitida(ruta: String, rol: Rol): Boolean = when {
        ruta.startsWith("cli/") -> rol == Rol.CLIENTE
        ruta.startsWith("op/") -> rol == Rol.OPERADOR || rol == Rol.ADMIN
        ruta.startsWith("sup/") -> rol == Rol.SUPERVISOR || rol == Rol.ADMIN
        ruta.startsWith("adm/") -> rol == Rol.ADMIN
        else -> true
    }
    const val NOTIFICACIONES = "notificaciones"
    const val DETALLE_PEDIDO = "pedido/{numero}"
    const val FACTURA = "factura/{numero}"
    fun detallePedido(numero: Int) = "pedido/$numero"
    fun factura(numero: Int) = "factura/$numero"

    // Cliente
    const val CATALOGO = "cli/catalogo"
    const val PRODUCTO = "cli/producto/{id}"
    const val CARRITO = "cli/carrito"
    const val CONFIRMAR = "cli/confirmar"
    const val PAGAR = "cli/pagar/{numero}"
    const val REPORTAR_PAGO = "cli/reportar/{numero}"
    const val MIS_PEDIDOS = "cli/pedidos"
    const val FALTANTE = "cli/faltante/{numero}"
    fun producto(id: String) = "cli/producto/$id"
    fun pagar(numero: Int) = "cli/pagar/$numero"
    fun reportarPago(numero: Int) = "cli/reportar/$numero"
    fun faltante(numero: Int) = "cli/faltante/$numero"

    // Operador
    const val OP_CATALOGO = "op/catalogo"
    const val OP_PRODUCTO = "op/producto?id={id}"
    const val POR_COMPRAR = "op/porcomprar"
    const val REGISTRAR_COMPRA = "op/compra"
    const val CAJAS = "op/cajas"
    const val CREAR_CAJA = "op/crearcaja"
    const val EMPACAR = "op/empacar/{codigo}"
    fun opProducto(id: String? = null) = if (id == null) "op/producto" else "op/producto?id=$id"
    fun empacar(codigo: String) = "op/empacar/$codigo"

    // Supervisor
    const val PAGOS = "sup/pagos"
    const val REVISAR_PAGO = "sup/pago/{id}"
    const val CAJAS_EN_CAMINO = "sup/recepcion"
    const val RECIBIR_CAJA = "sup/recibir/{codigo}"
    const val RESUMEN_RECEPCION = "sup/resumen/{codigo}"
    const val POR_DESPACHAR = "sup/despachos"
    const val DESPACHAR = "sup/despachar/{numero}"
    const val SUP_MAS = "sup/mas"
    const val CLIENTES = "sup/clientes"
    const val DETALLE_CLIENTE = "sup/cliente/{id}"
    const val FACTURAS = "sup/facturas"
    const val CARTERA = "sup/cartera"
    const val GASTOS = "sup/gastos"
    const val NUEVO_GASTO = "sup/gasto"
    const val REPORTES = "sup/reportes"
    fun revisarPago(id: String) = "sup/pago/$id"
    fun recibirCaja(codigo: String) = "sup/recibir/$codigo"
    fun resumenRecepcion(codigo: String) = "sup/resumen/$codigo"
    fun despachar(numero: Int) = "sup/despachar/$numero"
    fun detalleCliente(id: String) = "sup/cliente/$id"

    // Admin
    const val ADM_INICIO = "adm/inicio"
    const val POR_APROBAR = "adm/aprobar"
    const val APROBAR_CLIENTE = "adm/aprobar/{id}"
    const val USUARIOS = "adm/usuarios"
    const val EDITAR_USUARIO = "adm/usuario?id={id}"
    const val ADM_MAS = "adm/mas"
    const val AJUSTES = "adm/ajustes"
    const val AJ_PRECIOS = "adm/ajustes/precios"
    const val AJ_COMISIONES = "adm/ajustes/comisiones"
    const val AJ_ENVIO = "adm/ajustes/envio"
    const val AJ_ZONAS = "adm/ajustes/zonas"
    const val AJ_MEDIOS = "adm/ajustes/medios"
    const val AJ_CATEGORIAS = "adm/ajustes/categorias"
    const val PAPELERA = "adm/papelera"
    const val AUDITORIA = "adm/auditoria"
    fun aprobarCliente(id: String) = "adm/aprobar/$id"
    fun editarUsuario(id: String? = null) = if (id == null) "adm/usuario" else "adm/usuario?id=$id"

    /** Pantalla de inicio de cada rol después de iniciar sesión. */
    fun inicioDe(rol: Rol) = pestanas(rol).first().ruta

    /** Las 4 pestañas de la barra inferior de cada rol. */
    fun pestanas(rol: Rol): List<Pestana> = when (rol) {
        Rol.CLIENTE -> listOf(
            Pestana(CATALOGO, "Catálogo", Icons.Outlined.Storefront),
            Pestana(CARRITO, "Carrito", Icons.Outlined.ShoppingBag),
            Pestana(MIS_PEDIDOS, "Mis pedidos", Icons.AutoMirrored.Outlined.ReceiptLong),
            Pestana(PERFIL, "Perfil", Icons.Outlined.Person)
        )
        Rol.OPERADOR -> listOf(
            Pestana(OP_CATALOGO, "Catálogo", Icons.Outlined.Checkroom),
            Pestana(POR_COMPRAR, "Por comprar", Icons.Outlined.ShoppingCart),
            Pestana(CAJAS, "Cajas", Icons.Outlined.Inventory2),
            Pestana(PERFIL, "Perfil", Icons.Outlined.Person)
        )
        Rol.SUPERVISOR -> listOf(
            Pestana(PAGOS, "Pagos", Icons.Outlined.Payments),
            Pestana(CAJAS_EN_CAMINO, "Recepción", Icons.Outlined.MoveToInbox),
            Pestana(POR_DESPACHAR, "Despachos", Icons.Outlined.LocalShipping),
            Pestana(SUP_MAS, "Más", Icons.Outlined.Menu)
        )
        Rol.ADMIN -> listOf(
            Pestana(ADM_INICIO, "Inicio", Icons.Outlined.BarChart),
            Pestana(POR_APROBAR, "Por aprobar", Icons.Outlined.HowToReg),
            Pestana(USUARIOS, "Usuarios", Icons.Outlined.Group),
            Pestana(ADM_MAS, "Más", Icons.Outlined.Menu)
        )
    }
}

data class Pestana(val ruta: String, val titulo: String, val icono: ImageVector)
