package com.follgramer.diamantesproplayersgo

// Usamos @JvmField para evitar problemas con el deserializador de Firebase
import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class Player(
    // playerId puede ser val porque no cambia una vez creado
    val playerId: String = "",
    // tickets y passes deben ser 'var' para poder modificarlos
    var tickets: Long = 0,
    var passes: Long = 0
)
