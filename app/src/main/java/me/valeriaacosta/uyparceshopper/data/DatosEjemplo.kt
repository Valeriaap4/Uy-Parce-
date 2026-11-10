package me.valeriaacosta.uyparceshopper.data

import me.valeriaacosta.uyparceshopper.data.models.*

/* Datos de ejemplo para ver las pantallas funcionando antes de conectar Firebase.
   Todos los nombres, correos y teléfonos son ficticios. */
object DatosEjemplo {

    val ajustes = Ajustes()

    val zonas = listOf(
        Zona("Cúcuta y área metropolitana", 8000, listOf("Cúcuta", "Villa del Rosario", "Los Patios", "El Zulia")),
        Zona("Ciudades principales", 15000, listOf("Bogotá", "Medellín", "Cali", "Barranquilla", "Bucaramanga", "Cartagena")),
        Zona("Resto del país", 22000, listOf("Pamplona", "Ocaña", "Tunja", "Pasto", "Montería", "Otra ciudad"))
    )
    val ciudades: List<String> = zonas.flatMap { it.ciudades }
    fun zonaDe(ciudad: String): Zona = zonas.firstOrNull { ciudad in it.ciudades } ?: zonas.last()

    val categorias = listOf(
        Categoria("Camiseta", 0.5), Categoria("Camisa", 0.7), Categoria("Jean", 1.5),
        Categoria("Chaqueta", 3.0), Categoria("Zapatos", 3.0), Categoria("Sandalias", 1.2),
        Categoria("Bolso", 1.8), Categoria("Accesorio", 0.3)
    )
    private fun cat(n: String) = categorias.first { it.nombre == n }

    val admin = Usuario("u1", "Sofía Herrera", "sofia.herrera@correo.test", Rol.ADMIN)
    val operador = Usuario("u2", "Mateo Rojas", "mateo.rojas@correo.test", Rol.OPERADOR)
    val supervisor = Usuario("u3", "Camila Duarte", "camila.duarte@correo.test", Rol.SUPERVISOR)

    val valentina = Usuario("c1", "Valentina Ríos", "valentina.rios@correo.test", Rol.CLIENTE,
        tipoCliente = TipoCliente.MAYORISTA_AVANZADO, cedula = "1090456789", telefono = "300 555 0101",
        ciudad = "Bucaramanga", direccion = "Cra 27 # 45-12, Cabecera")
    val paola = Usuario("c2", "Paola Lizarazo", "paola.lizarazo@correo.test", Rol.CLIENTE,
        tipoCliente = TipoCliente.DETAL, cedula = "1093222333", telefono = "300 555 0102",
        ciudad = "Cúcuta", direccion = "Av 0 # 12-40, Caobos", saldoAFavor = 87000.0)
    val andres = Usuario("c3", "Andrés Gómez", "andres.gomez@correo.test", Rol.CLIENTE,
        tipoCliente = TipoCliente.MAYORISTA_PARCERO, cedula = "88123456", telefono = "300 555 0103",
        ciudad = "Medellín", direccion = "Cl 10 # 43-20, El Poblado")
    val laura = Usuario("c4", "Laura Méndez", "laura.mendez@correo.test", Rol.CLIENTE,
        tipoCliente = TipoCliente.DETAL, cedula = "1098765432", telefono = "300 555 0104",
        ciudad = "Bogotá", direccion = "Cl 80 # 15-30, apto 402")
    val nuevo1 = Usuario("c5", "Julián Pérez", "julian.perez@correo.test", Rol.CLIENTE,
        cedula = "1090111222", telefono = "300 555 0105", ciudad = "Pamplona",
        direccion = "Cl 5 # 6-10", aprobado = false)
    val nuevo2 = Usuario("c6", "Daniela Castro", "daniela.castro@correo.test", Rol.CLIENTE,
        cedula = "1090333444", telefono = "300 555 0106", ciudad = "Cali",
        direccion = "Cra 100 # 11-60", aprobado = false)

    val clientes = listOf(valentina, paola, andres, laura)
    val porAprobar = listOf(nuevo1, nuevo2)
    val equipo = listOf(admin, operador, supervisor, Usuario("u4", "Tomás Vera", "tomas.vera@correo.test", Rol.OPERADOR, activo = false))

    val productos = listOf(
        Producto("p1", "Camiseta Nike Dri-FIT", "Nike", "Camiseta deportiva, tela respirable", cat("Camiseta"), 0.5, 15.0, "Nike Factory Store", listOf("S", "M", "L", "XL"), colorFoto = 0xFF2B3A67),
        Producto("p2", "Chaqueta Columbia Rain", "Columbia", "Impermeable con capucha", cat("Chaqueta"), 3.0, 40.0, "Columbia Outlet", listOf("M", "L"), colorFoto = 0xFF3F6E5A),
        Producto("p3", "Jean Levi's 511 Slim", "Levi's", "Azul oscuro, corte slim", cat("Jean"), 1.5, 29.99, "Levi's Outlet", listOf("30", "32", "34"), colorFoto = 0xFF2F4E7A),
        Producto("p4", "Sandalias MK negras", "Michael Kors", "Sandalias de cuero", cat("Sandalias"), 1.2, 44.0, "Michael Kors Outlet", listOf("6", "7", "8"), colorFoto = 0xFF1E1E24),
        Producto("p5", "Nike Air Winflo 11", "Nike", "Tenis de running", cat("Zapatos"), 3.0, 65.0, "Nike Factory Store", listOf("9", "10", "11"), colorFoto = 0xFF8A2E2E),
        Producto("p6", "Bolso Coach Mini", "Coach", "Bolso cruzado de cuero", cat("Bolso"), 1.8, 89.0, "Coach Outlet", listOf("Única"), colorFoto = 0xFF8C6A3F),
        Producto("p7", "Franela Adidas Logo", "Adidas", "Gris con logo azul", cat("Camiseta"), 0.5, 10.0, "Adidas Outlet", listOf("S", "M", "L"), colorFoto = 0xFF7A808C),
        Producto("p8", "Gorra Tommy Hilfiger", "Tommy Hilfiger", "Gorra ajustable", cat("Accesorio"), 0.3, 18.0, "Tommy Outlet", listOf("Única"), agotado = true, colorFoto = 0xFF243B6B)
    )
    private fun prod(id: String) = productos.first { it.id == id }

    val pedidos = listOf(
        Pedido(1012, laura, listOf(LineaPedido(prod("p1"), "M", 2, 2, 1), LineaPedido(prod("p3"), "32", 1, 1, 0)),
            EstadoPedido.EN_CUCUTA, "6 nov 2026", zonaDe(laura.ciudad)),
        Pedido(1013, valentina, listOf(LineaPedido(prod("p4"), "8", 2), LineaPedido(prod("p7"), "S", 3), LineaPedido(prod("p2"), "L", 1)),
            EstadoPedido.PAGO_EN_REVISION, "8 nov 2026", zonaDe(valentina.ciudad)),
        Pedido(1014, paola, listOf(LineaPedido(prod("p5"), "10", 1), LineaPedido(prod("p1"), "S", 1)),
            EstadoPedido.PAGADO, "8 nov 2026", zonaDe(paola.ciudad)),
        Pedido(1015, andres, listOf(LineaPedido(prod("p6"), "Única", 2), LineaPedido(prod("p3"), "34", 4)),
            EstadoPedido.EN_CAMINO, "4 nov 2026", zonaDe(andres.ciudad)),
        Pedido(1010, paola, listOf(LineaPedido(prod("p7"), "M", 2, 2, 2)),
            EstadoPedido.ENTREGADO, "28 oct 2026", zonaDe(paola.ciudad), "Servientrega", "2045 8891 0021"),
        Pedido(1016, laura, listOf(LineaPedido(prod("p2"), "M", 1)),
            EstadoPedido.PENDIENTE_PAGO, "9 nov 2026", zonaDe(laura.ciudad)),
        Pedido(1011, paola, listOf(LineaPedido(prod("p1"), "L", 1, 1, 1), LineaPedido(prod("p3"), "30", 1, 1, 1)),
            EstadoPedido.ENVIADO, "1 nov 2026", zonaDe(paola.ciudad), "Interrapidísimo", "7001 2233 4455"),
        Pedido(1009, laura, listOf(LineaPedido(prod("p5"), "9", 1)),
            EstadoPedido.PAGO_EN_REVISION, "9 nov 2026", zonaDe(laura.ciudad))
    )
    fun pedido(numero: Int) = pedidos.first { it.numero == numero }
    fun buscarPedido(numero: Int): Pedido? = pedidos.firstOrNull { it.numero == numero }

    val pagos = listOf(
        Pago("pg1", pedido(1013), "Bancolombia", "TRX-558120", "8 nov 2026, 3:12 p. m.", EstadoPago.EN_REVISION),
        Pago("pg2", pedido(1014), "Nequi", "M1234567", "8 nov 2026, 10:40 a. m.", EstadoPago.APROBADO),
        Pago("pg3", pedido(1009), "Nequi", "M7788990", "9 nov 2026, 9:05 a. m.", EstadoPago.EN_REVISION)
    )

    val facturas = listOf(
        Factura("FAC-0142", pedido(1014), "8 nov 2026"),
        Factura("FAC-0141", pedido(1012), "6 nov 2026"),
        Factura("FAC-0140", pedido(1015), "4 nov 2026"),
        Factura("FAC-0139", pedido(1011), "1 nov 2026"),
        Factura("FAC-0137", pedido(1010), "28 oct 2026")
    )

    val cajas = listOf(
        Caja("CJ-0231", TipoCaja.MAYORISTA, andres, listOf(pedido(1015)), 6, EstadoCaja.ENVIADA, 9.2, TipoEnvio.REGULAR, "5 nov 2026"),
        Caja("CJ-0230", TipoCaja.DETAL, null, listOf(pedido(1012)), 3, EstadoCaja.EN_CUCUTA, fechaEnvio = "3 nov 2026"),
        Caja("CJ-0232", TipoCaja.DETAL, null, listOf(pedido(1014)), 1, EstadoCaja.ABIERTA),
        Caja("CJ-0233", TipoCaja.MAYORISTA, valentina, emptyList(), 0, EstadoCaja.ABIERTA)
    )

    val gastos = listOf(
        Gasto("g1", "Flete y envío", "Courier caja CJ-0230 (detal)", 42.5, Moneda.USD, "Courier Miami-Cúcuta", "5 nov 2026"),
        Gasto("g2", "Empaque", "Bolsas y cinta", 38000.0, Moneda.COP, "Papelería El Centro", "4 nov 2026"),
        Gasto("g3", "Transporte local", "Taxi a Servientrega", 15000.0, Moneda.COP, "—", "2 nov 2026"),
        Gasto("g4", "Aranceles e impuestos de importación", "Caja CJ-0228", 64.0, Moneda.USD, "DIAN", "30 oct 2026")
    )
    val categoriasGasto = listOf("Compra de mercancía", "Flete y envío", "Aranceles e impuestos de importación", "Empaque",
        "Transporte local", "Comisiones de plataforma", "Publicidad", "Software y herramientas", "Gastos bancarios", "Otros")

    val mediosPago = listOf(
        MedioPago("Nequi", "UyParce Shopper", "300 000 0000"),
        MedioPago("Bancolombia ahorros", "UyParce Shopper", "000-000000-00")
    )

    val transportadoras = listOf("Servientrega", "Interrapidísimo", "Coordinadora", "Envía")

    val notificaciones = listOf(
        Notificacion("Tu pedido llegó a Cúcuta", "El pedido #1012 ya está en Cúcuta, pero faltó una prenda. Elige qué hacer con ese dinero.", "hace 2 h", numeroPedido = 1012),
        Notificacion("Producto agotado", "La Gorra Tommy Hilfiger de tu carrito ya no está disponible.", "ayer"),
        Notificacion("Recibimos tu comprobante", "El pago del pedido #1009 está en revisión.", "hoy", leida = true, numeroPedido = 1009),
        Notificacion("Recuerda pagar", "Tienes 24 horas para pagar el pedido #1016 o se cancela.", "hoy", leida = true, numeroPedido = 1016)
    )

    val auditoria = listOf(
        RegistroAuditoria("Camila Duarte", "Aprobó pago", "Pedido #1014 · Nequi M1234567", "8 nov, 10:52 a. m."),
        RegistroAuditoria("Mateo Rojas", "Selló caja", "CJ-0231 · 9.2 lb · Regular", "5 nov, 6:30 p. m."),
        RegistroAuditoria("Sofía Herrera", "Cambió la tasa del dólar", "3.950 → 4.000", "5 nov, 8:00 a. m."),
        RegistroAuditoria("Sofía Herrera", "Aprobó cliente", "Laura Méndez · Detal", "2 nov, 4:15 p. m."),
        RegistroAuditoria("Mateo Rojas", "Marcó agotado", "Gorra Tommy Hilfiger", "1 nov, 1:20 p. m.")
    )

    val papelera = listOf(
        ElementoPapelera("Producto", "Camisa Polo Ralph Lauren", "Mateo Rojas", "3 nov 2026"),
        ElementoPapelera("Pedido", "#1008 · Laura Méndez (cancelado)", "Sistema", "2 nov 2026"),
        ElementoPapelera("Caja", "CJ-0225 (vacía)", "Mateo Rojas", "28 oct 2026")
    )
}
