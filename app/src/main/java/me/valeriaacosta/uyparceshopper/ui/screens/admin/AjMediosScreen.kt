package me.valeriaacosta.uyparceshopper.ui.screens.admin

import me.valeriaacosta.uyparceshopper.data.*

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import me.valeriaacosta.uyparceshopper.data.DatosEjemplo
import me.valeriaacosta.uyparceshopper.data.models.MedioPago
import me.valeriaacosta.uyparceshopper.ui.components.*
import me.valeriaacosta.uyparceshopper.ui.theme.*

// ─── AjMediosScreen ──────────────────────────────────────────────────────────

/** Pantalla 51. Cuentas del negocio que ve el cliente al pagar. */
@Composable
fun AjMediosScreen(
    onClickBack: () -> Unit
) {
    val mensaje = LocalMensaje.current
    val medios = remember { mutableStateListOf(*DatosEjemplo.mediosPago.toTypedArray()) }
    var nombre by remember { mutableStateOf("") }
    var numero by remember { mutableStateOf("") }
    var titular by remember { mutableStateOf("") }
    var errores by remember { mutableStateOf(mapOf<String, String>()) }

    UpScreen("Medios de pago", onClickBack) {
        ContenidoScroll {
            Text("El cliente ve estas cuentas al pagar.", color = Muted)
            medios.forEachIndexed { i, m ->
                Tarjeta {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(m.nombre, fontWeight = FontWeight.Bold)
                            Text("${m.numero} · ${m.titular}", color = Muted, fontFamily = Mono, fontSize = 14.sp)
                        }
                        IconButton(onClick = { medios.removeAt(i) }) { Icon(Icons.Outlined.Delete, "Quitar", tint = Red) }
                    }
                }
            }
            TituloSeccion("Agregar cuenta")
            CampoTexto("Banco o billetera", nombre, { errores = errores - "n"; nombre = it }, placeholder = "Ej: Daviplata", error = errores["n"])
            CampoTexto("Número", numero, { errores = errores - "num"; numero = it }, teclado = KeyboardType.Number, error = errores["num"])
            CampoTexto("Titular", titular, { errores = errores - "t"; titular = it }, error = errores["t"])
            BotonPrincipal("Agregar", {
                val e = mutableMapOf<String, String>()
                if (nombre.isBlank()) e["n"] = "Escribe el banco"
                if (numero.filter(Char::isDigit).length < 6) e["num"] = "Número no válido"
                if (titular.isBlank()) e["t"] = "Escribe el titular"
                errores = e
                if (e.isEmpty()) {
                    medios += MedioPago(nombre, titular, numero)
                    nombre = ""; numero = ""; titular = ""
                    mensaje("Cuenta agregada")
                }
            }, Modifier.fillMaxWidth(), Icons.Outlined.Add)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AjMediosScreenPreview() {
    UyParceTheme { AjMediosScreen(onClickBack = {}) }
}
