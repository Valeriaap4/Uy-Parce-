package me.valeriaacosta.uyparceshopper.data

import com.google.firebase.Timestamp
import me.valeriaacosta.uyparceshopper.data.models.*

/*
 * Datos de ejemplo. Los usan los @Preview y el botón "Preparar base de datos" (que carga los
 * productos de demostración). Todos los nombres, correos, teléfonos y cuentas son ficticios.
 */
object DatosEjemplo {

    val ajustes = Ajustes(
        mediosPago = listOf(
            MedioPago("m1", "Nequi", "UyParce Shopper", "300 000 0000", Moneda.COP),
            MedioPago("m2", "Bancolombia ahorros", "UyParce Shopper", "000-000000-00", Moneda.COP)
        )
    )
    val zonas: List<Zona> get() = ajustes.zonas
    val ciudades: List<String> = Zonas.ciudades
    fun zonaDe(ciudad: String): Zona = ajustes.zonaDe(ciudad)
    val categorias: List<Categoria> = ajustes.categorias
    val mediosPago: List<MedioPago> = ajustes.mediosPago
    val transportadoras: List<String> = Pedido.transportadoras
    val categoriasGasto: List<String> = Gasto.categorias
    private fun cat(nombre: String) = categorias.firstOrNull { it.nombre == nombre } ?: categorias.first()

    // ─── Usuarios ───────────────────────────────────────────────────────────
    val admin = Usuario("u1", "Sofía Herrera", "sofia.herrera@correo.test", Rol.ADMIN, aprobado = true)
    val operador = Usuario("u2", "Mateo Rojas", "mateo.rojas@correo.test", Rol.OPERADOR, aprobado = true)
    val supervisor = Usuario("u3", "Camila Duarte", "camila.duarte@correo.test", Rol.SUPERVISOR, aprobado = true)
    private val tomas = Usuario("u4", "Tomás Vera", "tomas.vera@correo.test", Rol.OPERADOR, activo = false, aprobado = true)

    val valentina = Usuario("c1", "Valentina Ríos", "valentina.rios@correo.test", Rol.CLIENTE,
        tipoCliente = TipoCliente.MAYORISTA_AVANZADO, cedula = "1090456789", telefono = "300 555 0101",
        ciudad = "Bucaramanga", direccion = "Cra 27 # 45-12, Cabecera", aprobado = true)
    val paola = Usuario("c2", "Paola Lizarazo", "paola.lizarazo@correo.test", Rol.CLIENTE,
        tipoCliente = TipoCliente.DETAL, cedula = "1093222333", telefono = "300 555 0102",
        ciudad = "Cúcuta", direccion = "Av 0 # 12-40, Caobos", aprobado = true)
    val andres = Usuario("c3", "Andrés Gómez", "andres.gomez@correo.test", Rol.CLIENTE,
        tipoCliente = TipoCliente.MAYORISTA_PARCERO, cedula = "88123456", telefono = "300 555 0103",
        ciudad = "Medellín", direccion = "Cl 10 # 43-20, El Poblado", aprobado = true)
    val laura = Usuario("c4", "Laura Méndez", "laura.mendez@correo.test", Rol.CLIENTE,
        tipoCliente = TipoCliente.DETAL, cedula = "1098765432", telefono = "300 555 0104",
        ciudad = "Bogotá", direccion = "Cl 80 # 15-30, apto 402", aprobado = true)
    val nuevo1 = Usuario("c5", "Julián Pérez", "julian.perez@correo.test", Rol.CLIENTE,
        cedula = "1090111222", telefono = "300 555 0105", ciudad = "Pamplona", direccion = "Cl 5 # 6-10")
    val nuevo2 = Usuario("c6", "Daniela Castro", "daniela.castro@correo.test", Rol.CLIENTE,
        cedula = "1090333444", telefono = "300 555 0106", ciudad = "Cali", direccion = "Cra 100 # 11-60")

    val clientes = listOf(valentina, paola, andres, laura)
    val porAprobar = listOf(nuevo1, nuevo2)
    val equipo = listOf(admin, operador, supervisor, tomas)
    val usuarios = equipo + clientes + porAprobar

    // ─── Catálogo ───────────────────────────────────────────────────────────
    val productos = listOf(
        Producto("p1", "Camiseta Nike Dri-FIT", "Nike", "Camiseta deportiva, tela respirable", cat("Camiseta"), 0.5, 15.0,
            "Nike Factory Store", listOf("S", "M", "L", "XL"), colorFoto = 0xFF2B3A67, codigos = mapOf("M" to "888407612345")),
        Producto("p2", "Chaqueta Columbia Rain", "Columbia", "Impermeable con capucha", cat("Chaqueta"), 3.0, 40.0,
            "Columbia Outlet", listOf("M", "L"), colorFoto = 0xFF3F6E5A),
        Producto("p3", "Jean Levi's 511 Slim", "Levi's", "Azul oscuro, corte slim", cat("Jean"), 1.5, 29.99,
            "Levi's Outlet", listOf("30", "32", "34"), colorFoto = 0xFF2F4E7A),
        Producto("p4", "Sandalias MK negras", "Michael Kors", "Sandalias de cuero", cat("Sandalias"), 1.2, 44.0,
            "Michael Kors Outlet", listOf("6", "7", "8"), colorFoto = 0xFF1E1E24),
        Producto("p5", "Nike Air Winflo 11", "Nike", "Tenis de running", cat("Zapatos"), 3.0, 65.0,
            "Nike Factory Store", listOf("9", "10", "11"), colorFoto = 0xFF8A2E2E),
        Producto("p6", "Bolso Coach Mini", "Coach", "Bolso cruzado de cuero", cat("Bolso"), 1.8, 89.0,
            "Coach Outlet", listOf("Única"), colorFoto = 0xFF8C6A3F),
        Producto("p7", "Franela Adidas Logo", "Adidas", "Gris con logo azul", cat("Camiseta"), 0.5, 10.0,
            "Adidas Outlet", listOf("S", "M", "L"), colorFoto = 0xFF7A808C),
        Producto("p8", "Gorra Tommy Hilfiger", "Tommy Hilfiger", "Gorra ajustable", cat("Accesorio"), 0.3, 18.0,
            "Tommy Outlet", listOf("Única"), agotado = true, colorFoto = 0xFF243B6B)
    )
    private fun prod(id: String) = productos.firstOrNull { it.id == id } ?: productos.first()

    /** Carrito de Laura: una camiseta y la gorra agotada (para ver el aviso de agotados). */
    val carritoLaura = listOf(ItemCarrito("p1", "M", 1), ItemCarrito("p8", "Única", 1))

    // ─── Pedidos ────────────────────────────────────────────────────────────
    private fun linea(id: String, talla: String, cantidad: Int, compradas: Int = 0, empacadas: Int = 0, recibidas: Int = 0) =
        LineaPedido(prod(id), talla, cantidad, compradas = compradas, empacadas = empacadas, recibidas = recibidas)

    /** Arma un pedido con los precios congelados igual que la app real. */
    private fun pedidoEjemplo(
        numero: Int, cliente: Usuario, lineas: List<LineaPedido>, estado: EstadoPedido, fecha: Timestamp,
        transportadora: String? = null, guia: String? = null, cajas: List<String> = emptyList(),
        faltantes: List<Faltante> = emptyList(), resoluciones: Map<String, Resolucion> = emptyMap(),
        motivoRechazo: String? = null
    ): Pedido {
        val tipo = cliente.tipoCliente ?: TipoCliente.DETAL
        val zona = ajustes.zonaDe(cliente.ciudad)
        val congeladas = Precios.congelar(lineas, tipo, ajustes)
        val pagado = estado !in listOf(EstadoPedido.PENDIENTE_PAGO, EstadoPedido.PAGO_EN_REVISION, EstadoPedido.CANCELADO)
        return Pedido(
            numero = numero, clienteId = cliente.id, cliente = cliente, zona = zona, lineas = congeladas,
            desglose = Precios.desglose(congeladas, tipo, zona, ajustes),
            estado = estado, fecha = fecha,
            venceEn = fecha.masHoras(ajustes.horasParaPagar),
            pagadoEn = if (pagado) fecha.masHoras(3) else null,
            despachadoEn = if (estado == EstadoPedido.ENVIADO || estado == EstadoPedido.ENTREGADO) fecha.masHoras(24 * 6) else null,
            entregadoEn = if (estado == EstadoPedido.ENTREGADO) fecha.masHoras(24 * 8) else null,
            motivoRechazo = motivoRechazo, transportadora = transportadora, guia = guia, cajas = cajas,
            faltantes = faltantes, resoluciones = resoluciones, conSaldo = faltantes.isNotEmpty()
        )
    }

    private fun faltante(id: String, pedidoLineas: List<LineaPedido>, prodId: String, talla: String, tipo: TipoCliente, fecha: Timestamp): Faltante {
        val l = Precios.congelar(pedidoLineas.filter { it.producto.id == prodId && it.talla == talla }, tipo, ajustes).first()
        return Faltante(id, 1, l.precioUnitario, tipo.moneda, "No llegó a Cúcuta", "${l.producto.nombre} · talla $talla × 1", fecha)
    }

    private val lineas1010 = listOf(linea("p7", "M", 2, 2, 2, 2), linea("p1", "L", 1, 1, 1, 0))
    private val lineas1012 = listOf(linea("p1", "M", 2, 2, 2, 1), linea("p3", "32", 1, 1, 1, 0))

    val pedidos: List<Pedido> = listOf(
        pedidoEjemplo(1009, laura, listOf(linea("p5", "9", 1)), EstadoPedido.PAGO_EN_REVISION, fechaEjemplo(2026, 11, 9, 9, 0)),
        pedidoEjemplo(1010, paola, lineas1010, EstadoPedido.ENTREGADO, fechaEjemplo(2026, 10, 28),
            transportadora = "Servientrega", guia = "2045 8891 0021", cajas = listOf("CJ-0227"),
            faltantes = listOf(faltante("f3", lineas1010, "p1", "L", TipoCliente.DETAL, fechaEjemplo(2026, 11, 2))),
            resoluciones = mapOf("f3" to Resolucion(TipoResolucion.SALDO_A_FAVOR, fecha = fechaEjemplo(2026, 11, 2, 15)))),
        pedidoEjemplo(1011, paola, listOf(linea("p1", "L", 1, 1, 1, 1), linea("p3", "30", 1, 1, 1, 1)), EstadoPedido.ENVIADO,
            fechaEjemplo(2026, 11, 1), transportadora = "Interrapidísimo", guia = "7001 2233 4455", cajas = listOf("CJ-0228")),
        pedidoEjemplo(1012, laura, lineas1012, EstadoPedido.EN_CUCUTA, fechaEjemplo(2026, 11, 3), cajas = listOf("CJ-0228"),
            faltantes = listOf(
                faltante("f1", lineas1012, "p1", "M", TipoCliente.DETAL, fechaEjemplo(2026, 11, 9, 8)),
                faltante("f2", lineas1012, "p3", "32", TipoCliente.DETAL, fechaEjemplo(2026, 11, 9, 8))
            ),
            resoluciones = mapOf("f2" to Resolucion(TipoResolucion.DEVOLUCION, "Nequi 300 555 0104", fechaEjemplo(2026, 11, 9, 10)))),
        pedidoEjemplo(1013, valentina, listOf(linea("p4", "8", 2), linea("p7", "S", 3), linea("p2", "L", 1)),
            EstadoPedido.PAGO_EN_REVISION, fechaEjemplo(2026, 11, 8, 14)),
        pedidoEjemplo(1014, paola, listOf(linea("p5", "10", 1, 1, 1), linea("p1", "S", 1, 1, 0)), EstadoPedido.COMPRADO,
            fechaEjemplo(2026, 11, 7), cajas = listOf("CJ-0232")),
        pedidoEjemplo(1015, andres, listOf(linea("p6", "Única", 2, 2, 2), linea("p3", "34", 4, 4, 4)), EstadoPedido.EN_CAMINO,
            fechaEjemplo(2026, 11, 4), cajas = listOf("CJ-0231")),
        pedidoEjemplo(1016, laura, listOf(linea("p2", "M", 1)), EstadoPedido.PENDIENTE_PAGO, fechaEjemplo(2026, 11, 9, 10),
            motivoRechazo = "La foto no se lee"),
        pedidoEjemplo(1017, paola, listOf(linea("p7", "L", 2, 2, 2, 1)), EstadoPedido.EN_CAMINO, fechaEjemplo(2026, 11, 2),
            cajas = listOf("CJ-0230")),
        pedidoEjemplo(1018, valentina, listOf(linea("p4", "7", 2, 1, 1), linea("p7", "S", 3)), EstadoPedido.PAGADO,
            fechaEjemplo(2026, 11, 6), cajas = listOf("CJ-0233"))
    )

    /** Pedido por número (si no existe, el primero: solo para vistas previas). */
    fun pedido(numero: Int): Pedido = buscarPedido(numero) ?: pedidos.first()
    fun buscarPedido(numero: Int): Pedido? = pedidos.firstOrNull { it.numero == numero }

    // ─── Pagos y facturas ───────────────────────────────────────────────────
    private fun pagoEjemplo(id: String, numero: Int, medio: String, referencia: String, fecha: Timestamp, estado: EstadoPago, motivo: String? = null): Pago {
        val p = pedido(numero)
        return Pago(id, numero, p.clienteId, p.cliente.nombre, p.totalAPagar, p.moneda, medio, referencia, fechaCampo(fecha),
            fotoId = null, estado = estado, motivoRechazo = motivo,
            revisadoPor = if (estado == EstadoPago.EN_REVISION) null else supervisor.nombre,
            revisadoEn = if (estado == EstadoPago.EN_REVISION) null else fecha.masHoras(1), fecha = fecha)
    }

    val pagos = listOf(
        pagoEjemplo("pg1", 1013, "Bancolombia ahorros", "TRX-558120", fechaEjemplo(2026, 11, 8, 15, 12), EstadoPago.EN_REVISION),
        pagoEjemplo("pg2", 1014, "Nequi", "M1234567", fechaEjemplo(2026, 11, 7, 10, 40), EstadoPago.APROBADO),
        pagoEjemplo("pg3", 1009, "Nequi", "M7788990", fechaEjemplo(2026, 11, 9, 9, 5), EstadoPago.EN_REVISION),
        pagoEjemplo("pg4", 1018, "Bancolombia ahorros", "TRX-559001", fechaEjemplo(2026, 11, 6, 16), EstadoPago.APROBADO),
        pagoEjemplo("pg5", 1016, "Nequi", "M5544332", fechaEjemplo(2026, 11, 9, 11), EstadoPago.RECHAZADO, "La foto no se lee")
    )

    private fun facturaEjemplo(numero: String, numeroPedido: Int): Factura {
        val p = pedido(numeroPedido)
        return Factura(numero, numeroPedido, p.clienteId, p, p.pagadoEn)
    }

    val facturas = listOf(
        facturaEjemplo("FAC-0143", 1018), facturaEjemplo("FAC-0142", 1014), facturaEjemplo("FAC-0141", 1012),
        facturaEjemplo("FAC-0140", 1015), facturaEjemplo("FAC-0139", 1011), facturaEjemplo("FAC-0138", 1017),
        facturaEjemplo("FAC-0137", 1010)
    )

    // ─── Cajas ──────────────────────────────────────────────────────────────
    private var siguientePrenda = 0
    private fun prendas(numero: Int, prodId: String, talla: String, cantidad: Int, recibidas: Int = 0): List<PrendaCaja> =
        List(cantidad) { i ->
            PrendaCaja("pc${++siguientePrenda}", numero, prodId, prod(prodId).nombre, talla, recibida = i < recibidas)
        }

    val cajas = listOf(
        Caja("CJ-0233", TipoCaja.MAYORISTA, valentina.id, valentina.nombre, listOf(1018),
            prendas(1018, "p4", "7", 1), EstadoCaja.ABIERTA, creadaPor = operador.nombre, creadaEn = fechaEjemplo(2026, 11, 8)),
        Caja("CJ-0232", TipoCaja.DETAL, null, null, listOf(1014),
            prendas(1014, "p5", "10", 1), EstadoCaja.ABIERTA, creadaPor = operador.nombre, creadaEn = fechaEjemplo(2026, 11, 8)),
        Caja("CJ-0231", TipoCaja.MAYORISTA, andres.id, andres.nombre, listOf(1015),
            prendas(1015, "p6", "Única", 2) + prendas(1015, "p3", "34", 4), EstadoCaja.ENVIADA,
            pesoLb = 9.2, tipoEnvio = TipoEnvio.REGULAR, costoEnvio = 40.36, creadaPor = operador.nombre,
            fechaEnvio = fechaEjemplo(2026, 11, 5, 18, 30)),
        Caja("CJ-0230", TipoCaja.DETAL, null, null, listOf(1017),
            prendas(1017, "p7", "L", 2, recibidas = 1), EstadoCaja.EN_CUCUTA, creadaPor = operador.nombre,
            fechaEnvio = fechaEjemplo(2026, 11, 3)),
        Caja("CJ-0228", TipoCaja.DETAL, null, null, listOf(1011, 1012),
            prendas(1011, "p1", "L", 1, 1) + prendas(1011, "p3", "30", 1, 1) +
                prendas(1012, "p1", "M", 2, 1) + prendas(1012, "p3", "32", 1, 0),
            EstadoCaja.RECIBIDA, creadaPor = operador.nombre, fechaEnvio = fechaEjemplo(2026, 11, 4),
            fechaRecepcion = fechaEjemplo(2026, 11, 9, 8)),
        Caja("CJ-0227", TipoCaja.DETAL, null, null, listOf(1010),
            prendas(1010, "p7", "M", 2, 2) + prendas(1010, "p1", "L", 1, 0),
            EstadoCaja.RECIBIDA, creadaPor = operador.nombre, fechaEnvio = fechaEjemplo(2026, 10, 29),
            fechaRecepcion = fechaEjemplo(2026, 11, 2))
    )

    // ─── Gastos ─────────────────────────────────────────────────────────────
    val gastos = listOf(
        Gasto("g1", "Flete y envío", "Courier caja CJ-0230 (detal)", 42.5, Moneda.USD, "Courier Miami-Cúcuta", fechaEjemplo(2026, 11, 5), supervisor.nombre),
        Gasto("g2", "Empaque", "Bolsas y cinta", 38000.0, Moneda.COP, "Papelería El Centro", fechaEjemplo(2026, 11, 4), supervisor.nombre),
        Gasto("g3", "Transporte local", "Taxi a Servientrega", 15000.0, Moneda.COP, "", fechaEjemplo(2026, 11, 2), supervisor.nombre),
        Gasto("g4", "Aranceles e impuestos de importación", "Caja CJ-0228", 64.0, Moneda.USD, "DIAN", fechaEjemplo(2026, 10, 30), admin.nombre)
    )

    // ─── Avisos de Laura ────────────────────────────────────────────────────
    val notificaciones = listOf(
        Notificacion("n1", laura.id, "Tu pedido llegó a Cúcuta",
            "El pedido #1012 ya está en Cúcuta, pero faltaron 2 prendas. Elige qué hacer con ese dinero.",
            fechaEjemplo(2026, 11, 9, 8), numeroPedido = 1012),
        Notificacion("n2", laura.id, "Producto agotado", "La Gorra Tommy Hilfiger de tu carrito ya no está disponible.",
            fechaEjemplo(2026, 11, 8, 13)),
        Notificacion("n3", laura.id, "Recibimos tu comprobante", "El pago del pedido #1009 está en revisión.",
            fechaEjemplo(2026, 11, 9, 9, 5), leida = true, numeroPedido = 1009),
        Notificacion("n4", laura.id, "Comprobante rechazado",
            "El pago del pedido #1016 fue rechazado: la foto no se lee. Tienes 24 horas para enviarlo otra vez.",
            fechaEjemplo(2026, 11, 9, 12), leida = true, numeroPedido = 1016)
    )

    // ─── Auditoría y papelera ───────────────────────────────────────────────
    val auditoria = listOf(
        RegistroAuditoria("a1", supervisor.id, supervisor.nombre, "Aprobó pago", "Pedido #1014 · Nequi M1234567", fechaEjemplo(2026, 11, 7, 10, 52)),
        RegistroAuditoria("a2", operador.id, operador.nombre, "Selló caja", "CJ-0231 · 9.2 lb · Regular", fechaEjemplo(2026, 11, 5, 18, 30)),
        RegistroAuditoria("a3", admin.id, admin.nombre, "Cambió la tasa del dólar", "3.950 → 4.000", fechaEjemplo(2026, 11, 5, 8)),
        RegistroAuditoria("a4", admin.id, admin.nombre, "Aprobó cliente", "Laura Méndez · Detal", fechaEjemplo(2026, 11, 2, 16, 15)),
        RegistroAuditoria("a5", operador.id, operador.nombre, "Marcó agotado", "Gorra Tommy Hilfiger", fechaEjemplo(2026, 11, 1, 13, 20))
    )

    val papelera = listOf(
        ElementoPapelera("e1", "Producto", "Camisa Polo Ralph Lauren", "productos", "p9",
            mapOf("id" to "p9", "nombre" to "Camisa Polo Ralph Lauren"), operador.nombre, fechaEjemplo(2026, 11, 3)),
        ElementoPapelera("e2", "Caja", "CJ-0225 (vacía)", "cajas", "CJ-0225",
            mapOf("codigo" to "CJ-0225"), operador.nombre, fechaEjemplo(2026, 10, 28))
    )
}
