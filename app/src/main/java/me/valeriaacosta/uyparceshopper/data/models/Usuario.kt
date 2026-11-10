package me.valeriaacosta.uyparceshopper.data.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude

/**
 * Persona con cuenta en la app (cliente o equipo). Se guarda en usuarios/{uid}.
 * Todos los campos tienen valor por defecto porque Firestore necesita un constructor vacío.
 * El saldo a favor no se guarda aquí: se calcula a partir de los faltantes de sus pedidos.
 */
data class Usuario(
    val id: String = "",
    val nombre: String = "",
    val correo: String = "",
    val rol: Rol = Rol.CLIENTE,
    val activo: Boolean = true,
    /** Lo asigna el admin al aprobar la cuenta (solo clientes). */
    val tipoCliente: TipoCliente? = null,
    val cedula: String = "",
    val telefono: String = "",
    val ciudad: String = "",
    val direccion: String = "",
    val aprobado: Boolean = false,
    val creadoEn: Timestamp? = null
) {
    @get:Exclude
    val iniciales: String
        get() = nombre.trim().split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1) }.uppercase()

    @get:Exclude
    val esEquipo: Boolean get() = rol != Rol.CLIENTE

    /** Moneda en la que compra (los mayoristas en USD, el detal en COP). */
    @get:Exclude
    val moneda: Moneda get() = (tipoCliente ?: TipoCliente.DETAL).moneda
}
