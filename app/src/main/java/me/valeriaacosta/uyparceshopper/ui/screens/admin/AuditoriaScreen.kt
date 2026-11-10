package me.valeriaacosta.uyparceshopper.ui.screens.admin

import me.valeriaacosta.uyparceshopper.data.*

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.valeriaacosta.uyparceshopper.data.DatosEjemplo
import me.valeriaacosta.uyparceshopper.ui.components.Tarjeta
import me.valeriaacosta.uyparceshopper.ui.components.UpScreen
import me.valeriaacosta.uyparceshopper.ui.theme.*

// ─── AuditoriaScreen ─────────────────────────────────────────────────────────

/** Pantalla 54. Registro de quién hizo qué y cuándo (solo lectura). */
@Composable
fun AuditoriaScreen(
    onClickBack: () -> Unit
) {
    UpScreen("Auditoría", onClickBack, subtitulo = "Registro de solo lectura") {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(DatosEjemplo.auditoria) { r ->
                Tarjeta {
                    Row {
                        Text(r.accion, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text(fechaHoraCorta(r.fecha), color = Muted, fontSize = 13.sp)
                    }
                    Text(r.detalle, color = Muted)
                    Text(r.usuario, color = Blue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AuditoriaScreenPreview() {
    UyParceTheme { AuditoriaScreen(onClickBack = {}) }
}
