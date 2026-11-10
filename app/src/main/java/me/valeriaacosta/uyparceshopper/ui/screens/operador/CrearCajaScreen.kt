package me.valeriaacosta.uyparceshopper.ui.screens.operador

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import me.valeriaacosta.uyparceshopper.data.LocalEstadoApp
import me.valeriaacosta.uyparceshopper.data.mensaje
import me.valeriaacosta.uyparceshopper.data.models.*
import me.valeriaacosta.uyparceshopper.ui.components.*
import me.valeriaacosta.uyparceshopper.ui.theme.Blue
import me.valeriaacosta.uyparceshopper.ui.theme.Muted

// ─── CrearCajaScreen ─────────────────────────────────────────────────────────

/**
 * Pantalla 23. Crear una caja: mayorista (una por cliente) o detal (compartida). Propone el código
 * siguiente (CJ-0001…); también se puede escribir o escanear el de una etiqueta propia.
 */
@Composable
fun CrearCajaScreen(
    onClickBack: () -> Unit,
    onCreada: (codigo: String) -> Unit
) {
    val estado = LocalEstadoApp.current
    val mensaje = LocalMensaje.current
    val scope = rememberCoroutineScope()
    var tipo by rememberSaveable { mutableStateOf<TipoCaja?>(null) }
    var clienteId by rememberSaveable { mutableStateOf<String?>(null) }
    var codigo by rememberSaveable { mutableStateOf("") }
    var errores by remember { mutableStateOf(mapOf<String, String>()) }
    var escanear by remember { mutableStateOf(false) }
    var creando by remember { mutableStateOf(false) }

    // Clientes mayoristas con pedidos pagados (sus prendas son las que van en una caja de mayorista).
    val mayoristas = estado.pedidos
        .filter { (it.estado == EstadoPedido.PAGADO || it.estado == EstadoPedido.COMPRADO) && it.moneda == Moneda.USD }
        .map { it.cliente }.distinctBy { it.id }.sortedBy { it.nombre }
    val cliente = mayoristas.firstOrNull { it.id == clienteId }

    // Código sugerido (el siguiente del contador).
    LaunchedEffect(Unit) {
        if (codigo.isBlank()) estado.siguienteCodigoCaja().onSuccess { if (codigo.isBlank()) codigo = it }
    }

    UpScreen("Crear caja", onClickBack, barraInferior = {
        BotonPrincipal("Crear y empacar", {
            val e = mutableMapOf<String, String>()
            val abierta = cliente?.let { c -> estado.cajas.firstOrNull { it.estado == EstadoCaja.ABIERTA && it.clienteId == c.id } }
            if (tipo == null) e["tipo"] = "Elige el tipo de caja"
            if (tipo == TipoCaja.MAYORISTA && cliente == null) e["cliente"] = "Elige el cliente mayorista"
            if (abierta != null) e["cliente"] = "${cliente?.nombre} ya tiene una caja abierta: ${abierta.codigo}"
            if (!Regex("""[A-Z0-9-]{4,20}""").matches(codigo)) e["codigo"] = "Usa letras, números y guiones (ej. CJ-0001)"
            errores = e
            val tipoElegido = tipo
            if (e.isEmpty() && tipoElegido != null) {
                creando = true
                scope.launch {
                    estado.crearCaja(codigo, tipoElegido, if (tipoElegido == TipoCaja.MAYORISTA) cliente else null)
                        .onSuccess { onCreada(codigo) }
                        .onFailure { mensaje(it.mensaje()); creando = false }
                }
            }
        }, Modifier.fillMaxWidth(), cargando = creando)
    }) {
        ContenidoScroll {
            Selector("Tipo de caja", TipoCaja.entries.map { it.etiqueta }, tipo?.etiqueta,
                { elegido -> errores = errores - "tipo"; tipo = TipoCaja.entries.first { it.etiqueta == elegido } }, error = errores["tipo"])
            if (tipo == TipoCaja.MAYORISTA) {
                if (mayoristas.isEmpty()) Aviso("No hay clientes mayoristas con pedidos pagados.", TonoChip.AMARILLO)
                Selector("Cliente mayorista", mayoristas.map { it.nombre }, cliente?.nombre,
                    { nombre -> errores = errores - "cliente"; clienteId = mayoristas.firstOrNull { it.nombre == nombre }?.id }, error = errores["cliente"])
                Text("Una caja por cliente. Al sellarla se pesa y se registra el envío.", color = Muted)
            }
            if (tipo == TipoCaja.DETAL) Text("Caja compartida: lleva prendas de varios pedidos detal. En Cúcuta se separan por pedido.", color = Muted)
            CampoTexto(
                "Código de la caja", codigo, { errores = errores - "codigo"; codigo = it.uppercase().filter { c -> c.isLetterOrDigit() || c == '-' } },
                error = errores["codigo"],
                iconoFin = { IconButton(onClick = { escanear = true }) { Icon(Icons.Outlined.QrCodeScanner, "Escanear etiqueta", tint = Blue) } }
            )
            Text("Si usas etiquetas propias, escanéala; si no, deja el código sugerido.", color = Muted, fontSize = 13.sp)
        }
    }

    if (escanear) DialogoEscaner(
        titulo = "Etiqueta de la caja",
        alLeer = { c -> escanear = false; codigo = c.uppercase(); errores = errores - "codigo" },
        alCerrar = { escanear = false }
    )
}

@Preview(showBackground = true)
@Composable
fun CrearCajaScreenPreview() {
    VistaPrevia(Rol.OPERADOR) { CrearCajaScreen(onClickBack = {}, onCreada = {}) }
}
