package me.valeriaacosta.uyparceshopper.ui.screens.operador

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import me.valeriaacosta.uyparceshopper.data.LocalEstadoApp
import me.valeriaacosta.uyparceshopper.data.buscarPorCodigo
import me.valeriaacosta.uyparceshopper.data.mensaje
import me.valeriaacosta.uyparceshopper.data.models.Rol
import me.valeriaacosta.uyparceshopper.ui.components.*
import me.valeriaacosta.uyparceshopper.ui.theme.*

// ─── RegistrarCompraScreen ───────────────────────────────────────────────────

/**
 * Pantalla 21. El operador escanea cada prenda que compra: la app reconoce producto y talla por el
 * código de barras de fábrica y la asigna al pedido pagado más antiguo que la espera. Si el código
 * no está guardado, se elige la prenda a mano y el código queda guardado para la próxima vez.
 */
@Composable
fun RegistrarCompraScreen(
    onClickBack: () -> Unit
) {
    val estado = LocalEstadoApp.current
    val mensaje = LocalMensaje.current
    val scope = rememberCoroutineScope()
    val pendientes = pendientesPorComprar(estado.pedidos)
    var escaner by remember { mutableStateOf(false) }
    var resultado by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var codigoDesconocido by rememberSaveable { mutableStateOf<String?>(null) }
    var noConseguido by remember { mutableStateOf<Pendiente?>(null) }
    var trabajando by remember { mutableStateOf(false) }
    var registradas by rememberSaveable { mutableStateOf(listOf<String>()) }

    /** Registra una unidad y, si viene de un código desconocido, lo guarda en el producto. */
    fun registrar(p: Pendiente, codigoNuevo: String?) {
        trabajando = true
        scope.launch {
            if (codigoNuevo != null) {
                estado.productoEnMemoria(p.producto.id)?.let { vivo ->
                    estado.guardarCodigo(vivo, p.talla, codigoNuevo).onFailure { mensaje(it.mensaje()) }
                }
            }
            estado.registrarCompra(p.producto.id, p.talla)
                .onSuccess { numero ->
                    val quedan = p.cantidad - 1
                    resultado = true to "${p.producto.nombre} talla ${p.talla} → va para el pedido #$numero" +
                        (if (quedan > 0) " (faltan $quedan)" else "") + (if (codigoNuevo != null) ". Código guardado para la próxima vez." else "")
                    registradas = registradas + "${p.producto.nombre} · ${p.talla} → #$numero"
                    codigoDesconocido = null
                }
                .onFailure { resultado = false to it.mensaje() }
            trabajando = false
        }
    }

    UpScreen("Registrar compra", onClickBack, subtitulo = "Escanea cada prenda que compras") {
        ContenidoScroll {
            BotonAmarillo("Escanear prenda", { escaner = true }, Modifier.fillMaxWidth(), Icons.Outlined.QrCodeScanner,
                habilitado = !trabajando, cargando = trabajando)

            resultado?.let { (ok, texto) ->
                Aviso(texto, if (ok) TonoChip.VERDE else TonoChip.AMARILLO, if (ok) Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline)
            }

            // ── Plan B: el código no está guardado, se elige la prenda a mano ──
            codigoDesconocido?.let { codigo ->
                Aviso("El código $codigo no está guardado en ningún producto. ¿Qué prenda es?", TonoChip.AMARILLO, Icons.AutoMirrored.Outlined.HelpOutline)
                pendientes.forEach { p ->
                    Tarjeta(alPulsar = { if (!trabajando) registrar(p, codigo) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.TouchApp, null, tint = Blue, modifier = Modifier.padding(end = 12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("${p.producto.nombre} · talla ${p.talla}", fontWeight = FontWeight.Bold)
                                Text("Faltan ${p.cantidad}", color = Muted)
                            }
                        }
                    }
                }
                TextButton(onClick = { codigoDesconocido = null }) { Text("Cancelar", color = Muted) }
            }

            TituloSeccion("Pendientes")
            if (pendientes.isEmpty()) Text("No hay nada pendiente por comprar.", color = Muted)
            pendientes.forEach { p ->
                Tarjeta {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${p.producto.nombre} · ${p.talla}", fontWeight = FontWeight.Bold)
                            Text("Faltan ${p.cantidad} · ${p.pedidos.joinToString { "#$it" }}", color = Muted, fontSize = 14.sp)
                        }
                        TextButton(onClick = { noConseguido = p }, enabled = !trabajando) { Text("No se consiguió", color = Red) }
                    }
                }
            }
            if (registradas.isNotEmpty()) {
                TituloSeccion("Registradas en esta visita (${registradas.size})")
                registradas.forEach { Text("✓ $it", color = Green) }
            }
        }
    }

    if (escaner) DialogoEscaner(alLeer = { codigo ->
        escaner = false
        val encontrado = buscarPorCodigo(estado.productos, codigo)
        when {
            encontrado == null -> {
                codigoDesconocido = codigo
                resultado = null
            }
            else -> {
                val p = pendientes.firstOrNull { it.producto.id == encontrado.producto.id && it.talla == encontrado.talla }
                if (p == null) resultado = false to "${encontrado.producto.nombre} talla ${encontrado.talla}: ningún pedido pagado la está esperando."
                else registrar(p, null)
            }
        }
    }, alCerrar = { escaner = false })

    noConseguido?.let { p ->
        DialogoConfirmar(
            "No se consiguió",
            "Se quita ${p.producto.nombre} ${p.talla} de los pedidos ${p.pedidos.joinToString { "#$it" }} y se avisa a los clientes " +
                "para que elijan saldo a favor o devolución.",
            "Confirmar", {
                trabajando = true
                scope.launch {
                    estado.marcarNoConseguido(p.producto.id, p.talla)
                        .onSuccess { n -> mensaje("Listo: avisamos a ${cantidad(n, "cliente")}") }
                        .onFailure { mensaje(it.mensaje()) }
                    trabajando = false
                }
            }, { noConseguido = null }, peligro = true
        )
    }
}

@Preview(showBackground = true)
@Composable
fun RegistrarCompraScreenPreview() {
    VistaPrevia(Rol.OPERADOR) { RegistrarCompraScreen(onClickBack = {}) }
}
