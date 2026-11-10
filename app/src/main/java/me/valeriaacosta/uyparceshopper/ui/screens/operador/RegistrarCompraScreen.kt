package me.valeriaacosta.uyparceshopper.ui.screens.operador

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import me.valeriaacosta.uyparceshopper.ui.components.*
import me.valeriaacosta.uyparceshopper.ui.theme.*

// ─── RegistrarCompraScreen ───────────────────────────────────────────────────

/**
 * Pantalla 21. El operador escanea cada prenda que compra y la app la asigna
 * al pedido pagado más antiguo que la espera. Si el código no coincide, elige a mano.
 */
@Composable
fun RegistrarCompraScreen(
    onClickBack: () -> Unit
) {
    val mensaje = LocalMensaje.current
    val pendientes = pendientesPorComprar()
    var escaner by remember { mutableStateOf(false) }
    var resultado by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var manual by remember { mutableStateOf(false) }
    var noConseguido by remember { mutableStateOf<Pendiente?>(null) }
    val registradas = remember { mutableStateListOf<String>() }

    fun registrar(p: Pendiente, texto: String) {
        registradas += "${p.producto.nombre} · ${p.talla} → #${p.pedidos.first()}"
        resultado = true to texto
    }

    UpScreen("Registrar compra", onClickBack, subtitulo = "Escanea cada prenda que compras") {
        ContenidoScroll {
            BotonAmarillo("Escanear prenda", { escaner = true }, Modifier.fillMaxWidth(), Icons.Outlined.QrCodeScanner)

            resultado?.let { (ok, texto) ->
                if (ok) Aviso(texto, TonoChip.VERDE, Icons.Outlined.CheckCircle)
                else {
                    Aviso(texto, TonoChip.AMARILLO, Icons.AutoMirrored.Outlined.HelpOutline)
                    BotonSecundario("Elegir a mano", { manual = true }, Modifier.fillMaxWidth(), Icons.Outlined.TouchApp)
                }
            }

            // ── Plan B: elegir la prenda a mano ──
            if (manual) {
                TituloSeccion("¿Qué prenda es?")
                pendientes.forEach { p ->
                    Tarjeta(alPulsar = {
                        manual = false
                        registrar(p, "${p.producto.nombre} ${p.talla} → pedido #${p.pedidos.first()}. Código guardado para la próxima vez.")
                    }) {
                        Text("${p.producto.nombre} · talla ${p.talla}", fontWeight = FontWeight.Bold)
                        Text("Faltan ${p.cantidad}", color = Muted)
                    }
                }
            }

            TituloSeccion("Pendientes")
            pendientes.forEach { p ->
                Tarjeta {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${p.producto.nombre} · ${p.talla}", fontWeight = FontWeight.Bold)
                            Text("Faltan ${p.cantidad} · ${p.pedidos.joinToString { "#$it" }}", color = Muted, fontSize = 14.sp)
                        }
                        TextButton(onClick = { noConseguido = p }) { Text("No se consiguió", color = Red) }
                    }
                }
            }
            if (registradas.isNotEmpty()) {
                TituloSeccion("Registradas hoy (${registradas.size})")
                registradas.forEach { Text("✓ $it", color = Green) }
            }
        }
    }

    if (escaner) DialogoEscaner(alLeer = { c ->
        escaner = false
        val p = pendientes.firstOrNull()
        if (c == "0888407612345" && p != null)
            registrar(p, "${p.producto.nombre} talla ${p.talla} → va para el pedido #${p.pedidos.first()} (${p.pedidos.size} esperando)")
        else resultado = false to "El código $c no coincide con ningún pedido pendiente."
    }, alCerrar = { escaner = false })

    noConseguido?.let { p ->
        DialogoConfirmar(
            "No se consiguió",
            "Se quita ${p.producto.nombre} ${p.talla} de los pedidos ${p.pedidos.joinToString { "#$it" }} y se avisa a los clientes para que elijan saldo a favor o devolución.",
            "Confirmar", { mensaje("Clientes avisados") }, { noConseguido = null }, peligro = true
        )
    }
}

@Preview(showBackground = true)
@Composable
fun RegistrarCompraScreenPreview() {
    UyParceTheme { RegistrarCompraScreen(onClickBack = {}) }
}
