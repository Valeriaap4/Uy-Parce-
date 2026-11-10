package me.valeriaacosta.uyparceshopper.ui.screens.supervisor

import me.valeriaacosta.uyparceshopper.data.*

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.valeriaacosta.uyparceshopper.data.models.Moneda
import me.valeriaacosta.uyparceshopper.ui.components.*
import me.valeriaacosta.uyparceshopper.ui.theme.*

// ─── ReportesScreen ──────────────────────────────────────────────────────────

/** Pantalla 39. Ventas, ganancia y gastos por período, separados en USD y COP. */
@Composable
fun ReportesScreen(
    onClickBack: () -> Unit
) {
    var periodo by remember { mutableIntStateOf(1) }

    UpScreen("Reportes", onClickBack) {
        ContenidoScroll {
            PestanasPildora(listOf("Semana", "Mes", "Año"), periodo, { periodo = it })
            Tarjeta {
                Text("Mayoristas (USD)", style = MaterialTheme.typography.titleMedium)
                FilaDinero("Ventas", 1214.40, Moneda.USD)
                FilaDinero("Comisiones (ganancia)", 248.30, Moneda.USD)
                FilaDinero("Gastos", 106.50, Moneda.USD)
                HorizontalDivider(color = Line)
                FilaDinero("Ganancia neta", 141.80, Moneda.USD, total = true)
            }
            Tarjeta {
                Text("Detal (COP)", style = MaterialTheme.typography.titleMedium)
                FilaDinero("Ventas", 3865000.0, Moneda.COP)
                FilaDinero("Margen (ganancia)", 892000.0, Moneda.COP)
                FilaDinero("Gastos", 53000.0, Moneda.COP)
                HorizontalDivider(color = Line)
                FilaDinero("Ganancia neta", 839000.0, Moneda.COP, total = true)
            }
            Tarjeta {
                Text("Gastos por categoría", style = MaterialTheme.typography.titleMedium)
                listOf("Flete y envío" to 0.55f, "Aranceles" to 0.3f, "Empaque" to 0.1f, "Transporte local" to 0.05f).forEach { (n, v) ->
                    Text(n, color = Muted, fontSize = 14.sp)
                    LinearProgressIndicator(
                        progress = { v },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
                        color = Blue, trackColor = GreySoft
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ReportesScreenPreview() {
    UyParceTheme { ReportesScreen(onClickBack = {}) }
}
