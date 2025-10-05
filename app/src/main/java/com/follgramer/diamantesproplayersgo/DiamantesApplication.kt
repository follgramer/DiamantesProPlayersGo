package com.follgramer.diamantesproplayersgo

import android.app.Application
import android.content.Context
import android.os.StrictMode
import android.util.Log
import androidx.multidex.MultiDexApplication
import com.follgramer.diamantesproplayersgo.ads.AdsInit
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await

class DiamantesApplication : MultiDexApplication() {

    data class CachedPlayer(
        val playerId: String,
        val tickets: Long,
        val passes: Long
    )

    companion object {
        private const val TAG = "DiamantesApp"

        @Volatile
        var cachedTop5: List<CachedPlayer>? = null

        @Volatile
        var top5LastUpdate: Long = 0

        const val CACHE_DURATION = 30000L

        @JvmStatic
        var isAdMobInitialized = false
            private set

        @JvmStatic
        var isFirebaseInitialized = false
            private set

        @JvmStatic
        var isAppCheckInitialized = false
            private set

        @JvmStatic
        var isAuthInitialized = false
            private set

        @JvmStatic
        var appContext: Context? = null
            private set

        private var applicationScope: CoroutineScope? = null
        private var top5Listener: ValueEventListener? = null
        private var top5Reference: DatabaseReference? = null

        fun markAdMobAsInitialized() {
            isAdMobInitialized = true
            Log.d(TAG, "AdMob marcado como inicializado")
        }

        fun resetAdMobInitialization() {
            isAdMobInitialized = false
            Log.d(TAG, "AdMob reinicializado")
        }

        fun isFullyInitialized(): Boolean {
            return isFirebaseInitialized && isAppCheckInitialized && isAuthInitialized
        }
    }

    override fun onCreate() {
        super.onCreate()

        try {
            Log.d(TAG, "=== INICIANDO DIAMANTESAPPLICATION ===")

            appContext = this
            applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

            // Inicializar AdMob
            AdsInit.init(this)
            markAdMobAsInitialized()
            Log.d(TAG, "AdMob inicializado desde Application")

            if (BuildConfig.DEBUG) {
                enableStrictMode()
            }

            initializeFirebaseStack()

            Log.d(TAG, "DiamantesApplication inicializada correctamente")

        } catch (e: Exception) {
            Log.e(TAG, "Error crítico en inicialización: ${e.message}")
            handleCriticalError(e)
        }
    }

    private fun enableStrictMode() {
        try {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .penaltyLog()
                    .build()
            )

            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .build()
            )

            Log.d(TAG, "StrictMode habilitado para debugging")
        } catch (e: Exception) {
            Log.w(TAG, "Error configurando StrictMode: ${e.message}")
        }
    }

    private fun initializeFirebaseStack() {
        applicationScope?.launch(Dispatchers.IO) {
            try {
                initializeFirebaseCore()
                delay(500)
                initializeAppCheck()
                delay(500)
                initializeAnonymousAuth()

                preloadTop5Data()

                Log.d(TAG, "Stack completo de Firebase inicializado correctamente")

            } catch (e: Exception) {
                Log.e(TAG, "Error en inicialización del stack Firebase: ${e.message}")
                handleFirebaseInitError(e)
            }
        }
    }

    private fun preloadTop5Data() {
        try {
            Log.d(TAG, "Iniciando pre-carga del TOP 5...")

            val database = FirebaseDatabase.getInstance(
                "https://diamantes-pro-players-go-f3510-default-rtdb.firebaseio.com/"
            ).reference

            top5Reference = database.child("players")

            top5Listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        val players = mutableListOf<CachedPlayer>()

                        snapshot.children.forEach { playerSnapshot ->
                            val playerId = playerSnapshot.key ?: return@forEach
                            val tickets = playerSnapshot.child("tickets").getValue(Long::class.java) ?: 0L
                            val passes = playerSnapshot.child("passes").getValue(Long::class.java) ?: 0L

                            if (playerId.matches(Regex("\\d+")) && playerId.length >= 5) {
                                players.add(CachedPlayer(playerId, tickets, passes))
                            }
                        }

                        cachedTop5 = players.sortedWith(
                            compareByDescending<CachedPlayer> { it.passes }
                                .thenByDescending { it.tickets }
                        ).take(5)

                        top5LastUpdate = System.currentTimeMillis()

                        Log.d(TAG, "TOP 5 pre-cargado: ${cachedTop5?.size} jugadores")
                        cachedTop5?.forEachIndexed { index, player ->
                            Log.d(TAG, "  ${index + 1}. ${player.playerId} - ${player.passes} pases")
                        }

                    } catch (e: Exception) {
                        Log.e(TAG, "Error procesando TOP 5: ${e.message}")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Error obteniendo TOP 5: ${error.message}")
                }
            }

            top5Reference?.addValueEventListener(top5Listener!!)

        } catch (e: Exception) {
            Log.e(TAG, "Error en preloadTop5Data: ${e.message}")
        }
    }

    private suspend fun initializeFirebaseCore() {
        try {
            if (FirebaseApp.getApps(this@DiamantesApplication).isEmpty()) {
                FirebaseApp.initializeApp(this@DiamantesApplication)
                Log.d(TAG, "Firebase Core inicializado")
            } else {
                Log.d(TAG, "Firebase Core ya estaba inicializado")
            }

            isFirebaseInitialized = true

        } catch (e: Exception) {
            Log.e(TAG, "Error inicializando Firebase Core: ${e.message}")
            throw e
        }
    }

    private suspend fun initializeAppCheck() {
        try {
            val firebaseAppCheck = FirebaseAppCheck.getInstance()

            firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )

            Log.d(TAG, "Firebase App Check configurado con Play Integrity provider")

            try {
                val token = firebaseAppCheck.getAppCheckToken(false).await()
                Log.d(TAG, "Token obtenido correctamente")
            } catch (e: Exception) {
                Log.w(TAG, "Error obteniendo token: ${e.message}")
            }

            isAppCheckInitialized = true

        } catch (e: Exception) {
            Log.e(TAG, "Error configurando App Check: ${e.message}")
            isAppCheckInitialized = false
        }
    }

    private suspend fun initializeAnonymousAuth() {
        try {
            val auth = Firebase.auth

            if (auth.currentUser == null) {
                Log.d(TAG, "Iniciando autenticación anónima...")

                val authResult = auth.signInAnonymously().await()

                if (authResult.user != null) {
                    Log.d(TAG, "Autenticación anónima exitosa: ${authResult.user?.uid?.take(8)}...")
                    isAuthInitialized = true
                } else {
                    Log.e(TAG, "Autenticación anónima falló: Usuario nulo")
                    isAuthInitialized = false
                }
            } else {
                Log.d(TAG, "Usuario ya autenticado: ${auth.currentUser?.uid?.take(8)}...")
                isAuthInitialized = true
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error en autenticación anónima: ${e.message}")
            isAuthInitialized = false

            try {
                delay(2000)
                val auth = Firebase.auth
                val retryResult = auth.signInAnonymously().await()
                if (retryResult.user != null) {
                    Log.d(TAG, "Autenticación anónima exitosa en reintento")
                    isAuthInitialized = true
                }
            } catch (retryException: Exception) {
                Log.e(TAG, "Reintento de auth también falló: ${retryException.message}")
            }
        }
    }

    private fun handleFirebaseInitError(error: Exception) {
        Log.e(TAG, "=== ERROR EN STACK FIREBASE ===")
        Log.e(TAG, "Error: ${error.message}")

        applicationScope?.launch {
            try {
                delay(3000)

                if (!isFirebaseInitialized) {
                    FirebaseApp.initializeApp(this@DiamantesApplication)
                    isFirebaseInitialized = true
                    Log.d(TAG, "Firebase inicializado en modo emergencia")
                }

                if (!isAuthInitialized) {
                    Firebase.auth.signInAnonymously().addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            isAuthInitialized = true
                            Log.d(TAG, "Auth emergencia exitosa")
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Incluso el modo emergencia falló: ${e.message}")
            }
        }
    }

    private fun handleCriticalError(error: Exception) {
        try {
            Log.e(TAG, "=== ERROR CRÍTICO EN APPLICATION ===")
            Log.e(TAG, "Mensaje: ${error.message}")
            Log.e(TAG, "Causa: ${error.cause}")

            if (!isFirebaseInitialized) {
                try {
                    FirebaseApp.initializeApp(this)
                    isFirebaseInitialized = true
                    Log.d(TAG, "Firebase inicializado en manejo de error")
                } catch (e: Exception) {
                    Log.e(TAG, "Error en inicialización de emergencia: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error manejando error crítico: ${e.message}")
        }
    }

    override fun onTerminate() {
        try {
            top5Listener?.let { listener ->
                top5Reference?.removeEventListener(listener)
            }

            AdsInit.cleanup()
            applicationScope?.cancel()
            Log.d(TAG, "DiamantesApplication terminada correctamente")
        } catch (e: Exception) {
            Log.e(TAG, "Error en onTerminate: ${e.message}")
        } finally {
            super.onTerminate()
        }
    }
}