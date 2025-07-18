package com.follgramer.diamantesproplayersgo

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class DiamantesProPlayersGoApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Inicialización de Firebase
        FirebaseApp.initializeApp(this)

        // Habilitar persistencia offline
        Firebase.database.setPersistenceEnabled(true)

        // Configurar keepSynced para datos críticos
        Firebase.database.reference.child("tickets").keepSynced(true)
        Firebase.database.reference.child("winners").keepSynced(true)
        Firebase.database.reference.child("rankings").keepSynced(true)
    }
}