package me.valeriaacosta.uyparceshopper.data.models

data class Usuario(
    val id: String,
    val nombre: String,
    val correo: String,
    val rol: Rol,
    val activo: Boolean = true,
    val tipoCliente: TipoCliente? = null,
    val cedula: String = "",
    val telefono: String = "",
    val ciudad: String = "",
    val direccion: String = "",
    val aprobado: Boolean = true,
    val saldoAFavor: Double = 0.0
) {
    val iniciales: String get() = nombre.split(" ").take(2).joinToString("") { it.take(1) }
}
