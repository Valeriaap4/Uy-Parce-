package me.valeriaacosta.uyparceshopper.data.models

data class Ajustes(
    val tasaDolar: Double = 4000.0,
    val margenDetal: Double = 0.30,
    val comisionAvanzado: Double = 0.20,
    val comisionParcero: Double = 0.25,
    val seguroUsd: Double = 10.0,
    val tarifaRegular: Double = 3.3,
    val tarifaPrime300: Double = 3.8,
    val tarifaPrime800: Double = 5.0
)
