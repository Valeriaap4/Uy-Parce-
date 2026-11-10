package me.valeriaacosta.uyparceshopper.data.models

/** Cuenta del negocio donde los clientes pagan. La configura el admin. */
data class MedioPago(
    val id: String = "",
    val nombre: String = "",
    val titular: String = "",
    val numero: String = "",
    /** Moneda que recibe la cuenta (Nequi y bancos colombianos: COP). */
    val moneda: Moneda = Moneda.COP
)
