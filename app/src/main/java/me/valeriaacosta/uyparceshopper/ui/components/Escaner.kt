package me.valeriaacosta.uyparceshopper.ui.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import me.valeriaacosta.uyparceshopper.data.normalizarCodigo
import me.valeriaacosta.uyparceshopper.ui.theme.*

// ─── Escáner de códigos de barras ───

/**
 * Lector de códigos con Google Code Scanner (viene con Google Play services): abre la cámara y
 * devuelve el código. No necesita permiso de cámara porque la cámara la maneja Google Play.
 */
object Escaner {

    private val opciones = GmsBarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            Barcode.FORMAT_UPC_A, Barcode.FORMAT_UPC_E, Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8,
            Barcode.FORMAT_CODE_128, Barcode.FORMAT_CODE_39, Barcode.FORMAT_QR_CODE
        )
        .enableAutoZoom()
        .build()

    fun escanear(context: Context, alLeer: (String) -> Unit, alCancelar: () -> Unit, alFallar: (String) -> Unit) {
        val escaner = GmsBarcodeScanning.getClient(context, opciones)
        escaner.startScan()
            .addOnSuccessListener { codigo ->
                val valor = codigo.rawValue
                if (valor.isNullOrBlank()) alFallar("No se pudo leer el código. Escríbelo abajo.") else alLeer(valor)
            }
            .addOnCanceledListener { alCancelar() }
            .addOnFailureListener { e ->
                if (e is MlKitException && e.errorCode == MlKitException.UNAVAILABLE) {
                    // El escáner todavía no está descargado en este celular: se pide la descarga.
                    ModuleInstall.getClient(context)
                        .installModules(ModuleInstallRequest.newBuilder().addApi(escaner).build())
                    alFallar("El escáner se está descargando en este celular. Mientras tanto, escribe el código.")
                } else {
                    alFallar("No se pudo abrir la cámara. Escribe el código abajo.")
                }
            }
    }
}

/**
 * Diálogo para leer un código de barras. La cámara se abre sola al mostrarlo (y otra vez tocando el
 * recuadro azul). Si el código no se deja leer, se escribe a mano. Siempre entrega el código normalizado.
 */
@Composable
fun DialogoEscaner(
    alLeer: (String) -> Unit,
    alCerrar: () -> Unit,
    titulo: String = "Escanear código",
    abrirAlInicio: Boolean = true
) {
    val context = LocalContext.current
    val enVistaPrevia = LocalInspectionMode.current
    var manual by rememberSaveable { mutableStateOf("") }
    var aviso by remember { mutableStateOf<String?>(null) }
    var camaraAbierta by remember { mutableStateOf(false) }
    val alLeerActual by rememberUpdatedState(alLeer)

    fun abrirCamara() {
        if (camaraAbierta || enVistaPrevia) return
        camaraAbierta = true
        aviso = null
        Escaner.escanear(
            context,
            alLeer = { camaraAbierta = false; alLeerActual(normalizarCodigo(it)) },
            alCancelar = { camaraAbierta = false },
            alFallar = { camaraAbierta = false; aviso = it }
        )
    }
    LaunchedEffect(Unit) { if (abrirAlInicio) abrirCamara() }

    val codigoManual = normalizarCodigo(manual)
    AlertDialog(
        onDismissRequest = alCerrar,
        containerColor = Color.White,
        title = { Text(titulo, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(16.dp)).background(Navy)
                        .clickable { abrirCamara() },
                    contentAlignment = Alignment.Center
                ) {
                    Box(Modifier.fillMaxWidth(0.75f).height(80.dp).border(3.dp, Yellow, RoundedCornerShape(12.dp)))
                    Icon(Icons.Outlined.PhotoCamera, null, tint = Color.White, modifier = Modifier.size(32.dp))
                    Text(
                        "Toca para abrir la cámara", color = NavyText, fontSize = 13.sp,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(10.dp)
                    )
                }
                aviso?.let { Aviso(it, TonoChip.AMARILLO) }
                CampoTexto(
                    "O escríbelo", manual, { manual = it.filter(Char::isLetterOrDigit).take(40) },
                    placeholder = "Código de barras", teclado = KeyboardType.Number
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { alLeer(codigoManual) }, enabled = codigoManual.length >= 4) {
                Text("Usar código", fontWeight = FontWeight.Bold, color = if (codigoManual.length >= 4) Blue else Muted)
            }
        },
        dismissButton = { TextButton(onClick = alCerrar) { Text("Cancelar", color = Muted) } }
    )
}
