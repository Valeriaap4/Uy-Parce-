package me.valeriaacosta.uyparceshopper.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.valeriaacosta.uyparceshopper.ui.theme.Navy
import me.valeriaacosta.uyparceshopper.ui.theme.NavyText

// ─── Fondo de marca ──────────────────────────────────────────────────────────

/** Fondo azul con el logo y una tarjeta blanca. Lo usan Login, Registro, Recuperar y Revisión. */
@Composable
fun FondoMarca(
    titulo: String,
    subtitulo: String,
    pie: String? = null,
    tarjeta: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Navy)
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        LogoUyParce(alto = 120.dp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(titulo, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
        Text(subtitulo, color = NavyText, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(24.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = tarjeta
        )
        if (pie != null) {
            Spacer(modifier = Modifier.height(20.dp))
            Text(pie, color = NavyText, fontSize = 13.sp, textAlign = TextAlign.Center)
        }
    }
}

/** Validación de correo que usan los formularios. */
fun correoValido(correo: String): Boolean = android.util.Patterns.EMAIL_ADDRESS.matcher(correo.trim()).matches()
