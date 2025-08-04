package com.follgramer.diamantesproplayersgo

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.children
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import com.follgramer.diamantesproplayersgo.databinding.ActivityMainBinding
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import java.util.Calendar
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    // --- Variables de la App ---
    private lateinit var binding: ActivityMainBinding
    private var rewardedAd: RewardedAd? = null
    private var interstitialAd: InterstitialAd? = null
    private val TAG_ADMOB = "AdMob"

    private val REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
    private val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

    private var currentPlayerId: String? = null
    private var currentSpins: Int = 0
    private var isSpinning = false

    private val gridItemsData = listOf(5, 1, 10, 25, 20, 0, 50, 5, 15, 1, 100, 20)
    private var countdownTimer: CountDownTimer? = null

    private lateinit var consentInformation: ConsentInformation

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            val componentsReady = intent.getBooleanExtra("COMPONENTS_READY", false)
            val adsLoading = intent.getBooleanExtra("ADS_LOADING", false)

            Log.d("MAIN_PROGRESSIVE", "🏠 MainActivity - Componentes: $componentsReady, Anuncios cargando: $adsLoading")

            SessionManager.init(this)
            handleNotificationIntent(intent)

            if (componentsReady) {
                setupAppWithPreloadedComponents()
                if (adsLoading) {
                    monitorAdLoadingProgress()
                }
            } else {
                checkConsentDirectly() // Fallback
            }

        } catch (e: Exception) {
            Log.e("MAIN_PROGRESSIVE", "❌ Error: ${e.message}")
            initializeBasicApp()
        }
    }

    private fun setupAppWithPreloadedComponents() {
        try {
            Log.d("MAIN_PROGRESSIVE", "⚡ Configurando app con componentes precargados")

            // UI inmediata
            setupToolbarAndDrawer()
            setupClickListeners()
            setupRecyclerViews()
            initializeGrid()
            setupBackPressedHandler()
            hideAllSections()
            showSection(binding.sectionHome.root)

            // Datos precargados
            currentPlayerId = if (SplashActivity.preloadedPlayerId.isNotEmpty())
                SplashActivity.preloadedPlayerId else null
            currentSpins = SplashActivity.preloadedSpins

            // ✅ Verificación de baneo para el usuario precargado
            if (currentPlayerId != null) {
                checkUserBanStatus(currentPlayerId!!)
            }

            // UI con datos
            if (currentPlayerId != null) {
                binding.sectionHome.myPlayerId.text = currentPlayerId
                binding.sectionHome.myPlayerId.setTextColor(Color.parseColor("#00A8FF"))
            } else {
                binding.sectionHome.myPlayerId.text = "Toca para configurar ✏️"
                binding.sectionHome.myPlayerId.setTextColor(Color.parseColor("#CCCCCC"))
            }

            updateSpinCountUI()
            updateUI(SplashActivity.preloadedTickets, SplashActivity.preloadedPasses)
            arreglarColoresDeTextos()

            // Firebase precargada
            if (SplashActivity.preloadedDatabase != null && SplashActivity.preloadedAuth != null) {
                database = SplashActivity.preloadedDatabase!!
                auth = SplashActivity.preloadedAuth!!
                Log.d("MAIN_PROGRESSIVE", "✅ Firebase conectada")
            } else {
                setupFirebase()
            }

            // AdMob básico (banners pueden cargar inmediatamente)
            if (SplashActivity.isAdMobInitialized) {
                loadBannerAds() // Solo banners por ahora
                Log.d("MAIN_PROGRESSIVE", "✅ Banners cargados")
            } else {
                setupAdMob()
                loadBannerAds()
            }

            // Consentimiento y welcome
            if (SplashActivity.consentAccepted) {
                if (!SplashActivity.welcomeShown) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        checkAndShowWelcomeModalSimple()
                    }, 200)
                }
            } else {
                showConsentModalSimple()
            }

            // Background tasks
            setupBackgroundTasks()

            Log.d("MAIN_PROGRESSIVE", "✅ App lista - anuncios se cargan en background")

        } catch (e: Exception) {
            Log.e("MAIN_PROGRESSIVE", "Error: ${e.message}")
            initializeBasicApp()
        }
    }

    private fun monitorAdLoadingProgress() {
        Thread {
            try {
                Log.d("MAIN_PROGRESSIVE", "📺 Monitoreando progreso de anuncios...")

                var checkCount = 0
                val maxChecks = 50 // 10 segundos máximo

                while (checkCount < maxChecks) {
                    val rewardedReady = SplashActivity.rewardedAdReady.get()
                    val interstitialReady = SplashActivity.interstitialAdReady.get()

                    if (rewardedReady && interstitialReady) {
                        runOnUiThread {
                            connectPreloadedAds()
                        }
                        Log.d("MAIN_PROGRESSIVE", "✅ Todos los anuncios listos")
                        break
                    } else if (rewardedReady && rewardedAd == null) { // Evita reconectar
                        runOnUiThread {
                            rewardedAd = SplashActivity.preloadedRewardedAd
                            Log.d("MAIN_PROGRESSIVE", "✅ Rewarded Ad conectado")
                        }
                    } else if (interstitialReady && interstitialAd == null) { // Evita reconectar
                        runOnUiThread {
                            interstitialAd = SplashActivity.preloadedInterstitialAd
                            Log.d("MAIN_PROGRESSIVE", "✅ Interstitial Ad conectado")
                        }
                    }

                    Thread.sleep(200)
                    checkCount++
                }

                if (checkCount >= maxChecks) {
                    Log.w("MAIN_PROGRESSIVE", "⚠️ Timeout esperando anuncios, cargando manualmente")
                    runOnUiThread {
                        loadRemainingAds()
                    }
                }

            } catch (e: Exception) {
                Log.e("MAIN_PROGRESSIVE", "Error monitoreando anuncios: ${e.message}")
            }
        }.start()
    }

    private fun connectPreloadedAds() {
        try {
            rewardedAd = SplashActivity.preloadedRewardedAd
            interstitialAd = SplashActivity.preloadedInterstitialAd
            Log.d("MAIN_PROGRESSIVE", "✅ Anuncios precargados conectados")
        } catch (e: Exception) {
            Log.e("MAIN_PROGRESSIVE", "Error conectando ads: ${e.message}")
            loadRemainingAds()
        }
    }

    private fun loadRemainingAds() {
        try {
            if (rewardedAd == null) {
                loadRewardedAd()
            }
            if (interstitialAd == null) {
                loadInterstitialAd()
            }
            Log.d("MAIN_PROGRESSIVE", "🔄 Cargando anuncios faltantes")
        } catch (e: Exception) {
            Log.e("MAIN_PROGRESSIVE", "Error cargando ads faltantes: ${e.message}")
        }
    }

    private fun setupBackgroundTasks() {
        Thread {
            try {
                if (currentPlayerId != null) {
                    createFirebaseSession(currentPlayerId!!)
                    setupFCMToken(currentPlayerId!!)
                    setupNotificationListener(currentPlayerId!!)
                    checkForPrivateMessages(currentPlayerId!!)
                    fetchPlayerData(currentPlayerId!!)
                }

                obtenerTop5Firebase()
                fetchAllData()

                runOnUiThread {
                    startWeeklyCountdown()
                    checkNotificationPermissions()
                }

            } catch (e: Exception) {
                Log.e("MAIN_PROGRESSIVE", "Error en background tasks: ${e.message}")
            }
        }.start()
    }


    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        val redirectTo = intent?.getStringExtra("redirect_to")

        if (redirectTo == "winners") {
            binding.root.postDelayed({
                redirectToWinnersSection()
            }, 500)
        }
    }

    // --- Lógica de Consentimiento Simplificada ---

    private fun checkConsentDirectly() {
        try {
            val prefs = getSharedPreferences("DiamantesProPrefs", Context.MODE_PRIVATE)
            val consentAccepted = prefs.getBoolean("CONSENT_ACCEPTED", false)

            Log.d("CONSENT_SIMPLE", "Consentimiento aceptado: $consentAccepted")

            if (consentAccepted) {
                Log.d("CONSENT_SIMPLE", "✅ Consentimiento aceptado - inicializando app")
                initializeBasicApp()
            } else {
                Log.d("CONSENT_SIMPLE", "❌ Consentimiento no aceptado - mostrando modal")
                // Se configura una UI mínima antes de pedir consentimiento
                setupToolbarAndDrawer()
                hideAllSections()
                showSection(binding.sectionHome.root)
                showConsentModalSimple()
            }
        } catch (e: Exception) {
            Log.e("CONSENT_SIMPLE", "❌ Error verificando consentimiento: ${e.message}")
            initializeBasicApp()
        }
    }

    private fun showConsentModalSimple() {
        try {
            Handler(Looper.getMainLooper()).post {
                val dialog = android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
                val dialogView = layoutInflater.inflate(R.layout.modal_consentimiento, null)
                dialog.setContentView(dialogView)

                dialog.window?.setLayout(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )

                dialogView.findViewById<Button>(R.id.btnAceptarConsentimiento)?.setOnClickListener {
                    Log.d("CONSENT_SIMPLE", "✅ Usuario aceptó consentimiento")
                    val prefs = getSharedPreferences("DiamantesProPrefs", Context.MODE_PRIVATE)
                    prefs.edit().putBoolean("CONSENT_ACCEPTED", true).apply()
                    dialog.dismiss()
                    initializeBasicApp()
                }

                dialogView.findViewById<Button>(R.id.btnRechazarConsentimiento)?.setOnClickListener {
                    Log.d("CONSENT_SIMPLE", "❌ Usuario rechazó consentimiento")
                    dialog.dismiss()
                    showMustAcceptDialog()
                }

                dialogView.findViewById<TextView>(R.id.textoLegalCompleto)?.setOnClickListener {
                    android.widget.Toast.makeText(this, "Ver políticas en el menú de la app", android.widget.Toast.LENGTH_SHORT).show()
                }

                dialog.setCancelable(false)
                dialog.show()
            }
        } catch (e: Exception) {
            Log.e("CONSENT_SIMPLE", "❌ Error mostrando modal: ${e.message}")
            initializeBasicApp()
        }
    }

    private fun showMustAcceptDialog() {
        try {
            AlertDialog.Builder(this)
                .setTitle("Consentimiento Requerido")
                .setMessage("La app necesita tu consentimiento para funcionar.")
                .setCancelable(false)
                .setPositiveButton("Aceptar") { dialog, _ ->
                    val prefs = getSharedPreferences("DiamantesProPrefs", Context.MODE_PRIVATE)
                    prefs.edit().putBoolean("CONSENT_ACCEPTED", true).apply()
                    dialog.dismiss()
                    initializeBasicApp()
                }
                .setNegativeButton("Cerrar App") { _, _ ->
                    finishAffinity()
                }
                .show()
        } catch (e: Exception) {
            Log.e("CONSENT_SIMPLE", "❌ Error en must accept dialog: ${e.message}")
            initializeBasicApp()
        }
    }

    // --- Lógica Principal de la Aplicación (FALLBACK) ---
    private fun initializeBasicApp() {
        try {
            Log.d("APP_FIX", "⚡ Inicializando app básica (fallback)...")

            runOnUiThread {
                // Setup de UI inmediato
                setupToolbarAndDrawer()
                setupClickListeners()
                setupRecyclerViews()
                initializeGrid()
                setupBackPressedHandler()
                hideAllSections()
                showSection(binding.sectionHome.root)

                val playerId = SessionManager.getPlayerId(this@MainActivity)
                this.currentSpins = SessionManager.getCurrentSpins(this@MainActivity)
                updateSpinCountUI()

                if (playerId.isNotEmpty()) {
                    currentPlayerId = playerId
                    binding.sectionHome.myPlayerId.text = currentPlayerId
                    binding.sectionHome.myPlayerId.setTextColor(Color.parseColor("#00A8FF"))
                } else {
                    binding.sectionHome.myPlayerId.text = "Toca para configurar ✏️"
                    binding.sectionHome.myPlayerId.setTextColor(Color.parseColor("#CCCCCC"))
                }

                arreglarColoresDeTextos()
                Log.d("APP_FIX", "✅ App básica lista. Cargando componentes pesados...")

                loadHeavyComponentsLater()

                Handler(Looper.getMainLooper()).postDelayed({
                    checkAndShowWelcomeModalSimple()
                }, 500)
            }
        } catch (e: Exception) {
            Log.e("APP_FIX", "❌ Error inicializando app básica: ${e.message}")
        }
    }


    private fun loadHeavyComponentsLater() {
        Thread {
            try {
                Log.d("HEAVY_LOAD_FIX", "🔄 Cargando componentes pesados en background...")

                runOnUiThread {
                    setupFirebase()
                    setupAdMob()
                    loadAllAds()
                }

                val playerId = SessionManager.getPlayerId(this@MainActivity)
                if (playerId.isNotEmpty()) {
                    createFirebaseSession(playerId)
                    obtenerTop5Firebase()
                    setupFCMToken(playerId)
                    checkForPrivateMessages(playerId)
                    setupNotificationListener(playerId)
                    fetchPlayerData(playerId)
                    fetchAllData()
                } else {
                    obtenerTop5Firebase()
                    fetchAllData()
                }

                runOnUiThread {
                    startWeeklyCountdown()
                    signInAnonymously()
                    checkNotificationPermissions()
                    Log.d("HEAVY_LOAD_FIX", "✅ Componentes pesados cargados")
                }

            } catch (e: Exception) {
                Log.e("HEAVY_LOAD_FIX", "❌ Error en thread pesado: ${e.message}")
            }
        }.start()
    }

    private fun checkAndShowWelcomeModalSimple() {
        try {
            val prefs = getSharedPreferences("DiamantesProPrefs", Context.MODE_PRIVATE)
            val hasShownWelcome = prefs.getBoolean("HAS_SHOWN_WELCOME", false)

            if (!hasShownWelcome) {
                showWelcomeModalSimple()
            }
        } catch (e: Exception) {
            Log.e("WELCOME_SIMPLE", "❌ Error verificando welcome modal: ${e.message}")
        }
    }

    private fun showWelcomeModalSimple() {
        try {
            val dialog = android.app.Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)
            val dialogView = layoutInflater.inflate(R.layout.modal_welcome, null)
            dialog.setContentView(dialogView)

            dialog.window?.let { window ->
                window.setLayout(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
                window.setBackgroundDrawable(ColorDrawable(Color.parseColor("#CC000000")))
                window.setDimAmount(0.0f)
            }

            dialogView.findViewById<Button>(R.id.btnStartPlaying)?.setOnClickListener {
                val prefs = getSharedPreferences("DiamantesProPrefs", Context.MODE_PRIVATE)
                prefs.edit().putBoolean("HAS_SHOWN_WELCOME", true).apply()
                dialog.dismiss()
                SessionManager.setCurrentSpins(this, 10)
                this.currentSpins = SessionManager.getCurrentSpins(this)
                updateSpinCountUI()
            }

            dialogView.findViewById<ImageView>(R.id.btnCloseWelcome)?.setOnClickListener {
                val prefs = getSharedPreferences("DiamantesProPrefs", Context.MODE_PRIVATE)
                if(!prefs.getBoolean("HAS_SHOWN_WELCOME", false)) {
                    SessionManager.setCurrentSpins(this, 10)
                    this.currentSpins = SessionManager.getCurrentSpins(this)
                    updateSpinCountUI()
                    prefs.edit().putBoolean("HAS_SHOWN_WELCOME", true).apply()
                }
                dialog.dismiss()
            }

            dialog.setCancelable(true)
            dialog.show()

        } catch (e: Exception) {
            Log.e("WELCOME_FIX", "❌ Error mostrando welcome modal: ${e.message}")
        }
    }

    // --- Resto de las funciones de la App ---

    private fun setupFCMToken(playerId: String) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM_TOKEN", "Error obteniendo token FCM", task.exception)
                return@addOnCompleteListener
            }
            val token = task.result
            saveTokenToDatabase(playerId, token)
        }
    }

    private fun saveTokenToDatabase(playerId: String, token: String) {
        try {
            if (!::database.isInitialized) return
            val tokenData = mapOf(
                "token" to token,
                "timestamp" to ServerValue.TIMESTAMP,
                "deviceInfo" to Build.MODEL
            )
            database.child("playerTokens").child(playerId).setValue(tokenData)
        } catch (e: Exception) {
            Log.e("FCM_TOKEN", "Error en saveTokenToDatabase: ${e.message}")
        }
    }

    private fun arreglarColoresDeTextos() {
        try {
            aplicarColoresATodosLosTextos(binding.sectionHome.root)
            aplicarColoresATodosLosTextos(binding.sectionTasks.root)
            aplicarColoresATodosLosTextos(binding.sectionLeaderboard.root)
            aplicarColoresATodosLosTextos(binding.sectionWinners.root)
        } catch (e: Exception) {
            Log.e("COLOR_FIX", "Error aplicando colores: ${e.message}")
        }
    }

    private fun aplicarColoresATodosLosTextos(view: View) {
        try {
            when (view) {
                is TextView -> {
                    val currentColor = view.currentTextColor
                    if (Color.alpha(currentColor) < 200 ||
                        (Color.red(currentColor) + Color.green(currentColor) + Color.blue(currentColor)) < 400) {

                        val newColor = when {
                            view.textSize >= 18f -> Color.WHITE
                            view.textSize >= 14f -> Color.parseColor("#CCCCCC")
                            else -> Color.parseColor("#B0B0B0")
                        }
                        view.setTextColor(newColor)
                    }
                }
                is android.view.ViewGroup -> {
                    for (i in 0 until view.childCount) {
                        aplicarColoresATodosLosTextos(view.getChildAt(i))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("COLOR_FIX", "Error en aplicarColoresATodosLosTextos: ${e.message}")
        }
    }

    private fun createFirebaseSession(playerId: String) {
        try {
            if (!::database.isInitialized) return
            val sessionId = SessionManager.getSessionId(this)
            val sessionData = mapOf(
                "timestamp" to ServerValue.TIMESTAMP,
                "active" to true,
                "deviceInfo" to Build.MODEL,
                "appVersion" to "1.0"
            )
            database.child("sessions").child(playerId).child(sessionId)
                .setValue(sessionData)
                .addOnSuccessListener {
                    verificarLimiteDeSesiones(playerId)
                }
        } catch (e: Exception) {
            Log.e("SESSION_FIREBASE", "Error en createFirebaseSession: ${e.message}")
        }
    }

    private fun verificarLimiteDeSesiones(playerId: String) {
        try {
            if (!::database.isInitialized) return
            val sessionsRef = database.child("sessions").child(playerId)
            sessionsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        var sesionesActivas = 0
                        val currentTime = System.currentTimeMillis()
                        for (sesion in snapshot.children) {
                            val active = sesion.child("active").getValue(Boolean::class.java) ?: false
                            val timestamp = sesion.child("timestamp").getValue(Long::class.java) ?: 0L
                            if (active && (currentTime - timestamp) < 15 * 60 * 1000) {
                                sesionesActivas++
                            }
                        }
                        if (sesionesActivas > 3) {
                            mostrarModalExcesoSesiones(sesionesActivas)
                        }
                    } catch (e: Exception) {
                        Log.e("SESSION_LIMIT", "Error procesando sesiones: ${e.message}")
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e("SESSION_LIMIT", "Error verificando sesiones: ${error.message}")
                }
            })
        } catch (e: Exception) {
            Log.e("SESSION_LIMIT", "Error en verificarLimiteDeSesiones: ${e.message}")
        }
    }

    private fun mostrarModalExcesoSesiones(cantidad: Int) {
        AlertDialog.Builder(this)
            .setTitle("Demasiadas sesiones")
            .setMessage("Tu ID está siendo usado en $cantidad dispositivos al mismo tiempo.\n\n" +
                    "Solo se permiten máximo 3 dispositivos simultáneos.\n\n" +
                    "Si no reconoces estas sesiones, contacta:\nfollgramer@gmail.com")
            .setCancelable(false)
            .setPositiveButton("Cerrar App") { _, _ ->
                finishAffinity()
            }
            .setNegativeButton("Más Info") { _, _ ->
                mostrarInfoSobreSesiones()
            }
            .show()
    }

    private fun mostrarInfoSobreSesiones() {
        AlertDialog.Builder(this)
            .setTitle("¿Qué son las sesiones?")
            .setMessage("Una sesión se crea cuando:\n\n" +
                    "• Instalas la app en un nuevo dispositivo\n" +
                    "• Cambias de ID de jugador\n" +
                    "• Reinstalas la aplicación\n\n" +
                    "Las sesiones se consideran activas por 15 minutos.\n\n" +
                    "Si perdiste tu dispositivo o no reconoces las sesiones, " +
                    "contacta inmediatamente:\nfollgramer@gmail.com")
            .setPositiveButton("Entendido") { dialog, _ ->
                dialog.dismiss()
                finishAffinity()
            }
            .show()
    }

    private fun obtenerTop5Firebase() {
        try {
            if (!::database.isInitialized) setupFirebase()
            database.child("players").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        val jugadores = mutableListOf<Player>()
                        for (playerSnapshot in snapshot.children) {
                            try {
                                val playerId = playerSnapshot.key ?: continue
                                val tickets = playerSnapshot.child("tickets").getValue(Long::class.java) ?: 0L
                                val passes = playerSnapshot.child("passes").getValue(Long::class.java) ?: 0L
                                if (playerId.matches(Regex("\\d+")) && playerId.length >= 5) {
                                    jugadores.add(Player(playerId, tickets, passes))
                                }
                            } catch (e: Exception) {
                                Log.e("TOP5_FIREBASE", "Error procesando jugador: ${e.message}")
                            }
                        }
                        val jugadoresOrdenados = jugadores.sortedWith(
                            compareByDescending<Player> { it.passes }.thenByDescending { it.tickets }
                        )
                        runOnUiThread {
                            try {
                                if (::binding.isInitialized) {
                                    binding.sectionLeaderboard.leaderboardRecyclerView.adapter = LeaderboardAdapter(jugadoresOrdenados)
                                    if (jugadoresOrdenados.isNotEmpty()) {
                                        val top5 = jugadoresOrdenados.take(5)
                                        binding.sectionHome.miniLeaderboardList.adapter = MiniLeaderboardAdapter(top5, currentPlayerId)
                                        binding.sectionHome.miniLeaderboardList.visibility = View.VISIBLE
                                    } else {
                                        binding.sectionHome.miniLeaderboardList.visibility = View.GONE
                                    }
                                    val myRank = jugadoresOrdenados.indexOfFirst { it.playerId == currentPlayerId }
                                    val myRankLayout = binding.sectionHome.myRankStatus
                                    if (myRank != -1) {
                                        myRankLayout.root.visibility = View.VISIBLE
                                        val myPlayerData = jugadoresOrdenados[myRank]
                                        myRankLayout.rank.text = "#${myRank + 1}"
                                        myRankLayout.playerId.text = "Tú"
                                        myRankLayout.passes.text = "${myPlayerData.passes} Pases"
                                    } else {
                                        myRankLayout.root.visibility = View.GONE
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("TOP5_FIREBASE", "Error actualizando UI del top 5: ${e.message}")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("TOP5_FIREBASE", "Error general en onDataChange: ${e.message}")
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e("TOP5_FIREBASE", "❌ ERROR Firebase: ${error.message}")
                }
            })
        } catch (e: Exception) {
            Log.e("TOP5_FIREBASE", "Error general obteniendo top5: ${e.message}")
        }
    }

    private fun setupToolbarAndDrawer() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        val toggle = ActionBarDrawerToggle(this, binding.drawerLayout, binding.toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        binding.navView.setNavigationItemSelectedListener { handleNavigation(it) }

        // ✅ AÑADIR: Configurar eventos click para opciones legales
        setupLegalOptionsClickListeners()
    }

    // ✅ NUEVA FUNCIÓN: Configurar clicks para las opciones legales
    private fun setupLegalOptionsClickListeners() {
        try {
            // Buscar los TextViews de las opciones legales en el drawer
            val drawerLayout = binding.drawerLayout
            val navPrivacy = drawerLayout.findViewById<TextView>(R.id.nav_privacy)
            val navTerms = drawerLayout.findViewById<TextView>(R.id.nav_terms)

            // Configurar click para Políticas de Privacidad
            navPrivacy?.setOnClickListener {
                Log.d("LEGAL_CLICK", "Click en Políticas de Privacidad")
                binding.drawerLayout.closeDrawer(GravityCompat.START)

                // Pequeño delay para que se cierre el drawer suavemente
                Handler(Looper.getMainLooper()).postDelayed({
                    showLegalModal("Política de Privacidad", getString(R.string.privacy_policy_content))
                }, 250)
            }

            // Configurar click para Términos y Condiciones
            navTerms?.setOnClickListener {
                Log.d("LEGAL_CLICK", "Click en Términos y Condiciones")
                binding.drawerLayout.closeDrawer(GravityCompat.START)

                // Pequeño delay para que se cierre el drawer suavemente
                Handler(Looper.getMainLooper()).postDelayed({
                    showLegalModal("Términos y Condiciones", getString(R.string.terms_content))
                }, 250)
            }

            if (navPrivacy != null && navTerms != null) {
                Log.d("LEGAL_SETUP", "✅ Eventos legales configurados correctamente")
            } else {
                Log.e("LEGAL_SETUP", "❌ No se encontraron los TextViews legales")
            }

        } catch (e: Exception) {
            Log.e("LEGAL_SETUP", "❌ Error configurando opciones legales: ${e.message}")
        }
    }

    private fun setupFirebase() {
        try {
            database = FirebaseDatabase.getInstance("https://diamantes-pro-players-go-f3510-default-rtdb.firebaseio.com/").reference
            auth = Firebase.auth
        } catch (e: Exception) {
            Log.e("FIREBASE_SETUP", "❌ Error inicializando Firebase: ${e.message}")
        }
    }

    private fun setupAdMob() {
        MobileAds.initialize(this) {}
    }

    private fun setupRecyclerViews() {
        binding.sectionLeaderboard.leaderboardRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.sectionWinners.winnersRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.sectionHome.miniLeaderboardList.layoutManager = LinearLayoutManager(this)
    }

    private fun loadAllAds() {
        loadBannerAds()
        loadRewardedAd()
        loadInterstitialAd()
    }

    private fun setupClickListeners() {
        binding.sectionHome.myPlayerId.setOnClickListener { promptForPlayerId() }
        binding.sectionHome.spinButton.setOnClickListener { spinGrid() }
        binding.sectionHome.getSpinsButton.setOnClickListener { requestSpinsByWatchingAd() }
        binding.sectionTasks.taskButton.setOnClickListener { requestTicketsByWatchingAd() }
    }

    private fun setupBackPressedHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackPress()
            }
        })
    }

    private fun handleBackPress() {
        when {
            binding.drawerLayout.isDrawerOpen(GravityCompat.START) -> binding.drawerLayout.closeDrawer(GravityCompat.START)
            !isInHomeSection() -> goToHomeSection()
            else -> showExitConfirmationDialog()
        }
    }

    private fun isInHomeSection(): Boolean = binding.sectionHome.root.visibility == View.VISIBLE

    private fun goToHomeSection() {
        hideAllSections()
        showSection(binding.sectionHome.root)
        binding.navView.setCheckedItem(R.id.nav_home)
    }

    private fun showExitConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Salir de la aplicación")
            .setMessage("¿Estás seguro de que quieres cerrar la aplicación?")
            .setCancelable(true)
            .setPositiveButton("Salir") { _, _ ->
                marcarSesionInactiva()
                finishAffinity()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun handleNavigation(menuItem: MenuItem): Boolean {
        hideAllSections()
        if (menuItem.groupId == R.id.group_main) {
            menuItem.isChecked = true
        }
        when (menuItem.itemId) {
            R.id.nav_home -> showSection(binding.sectionHome.root)
            R.id.nav_tasks -> showInterstitialAd { showSection(binding.sectionTasks.root) }
            R.id.nav_leaderboard -> showInterstitialAd { showSection(binding.sectionLeaderboard.root) }
            R.id.nav_winners -> showInterstitialAd { showSection(binding.sectionWinners.root) }
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun hideAllSections() {
        binding.sectionHome.root.visibility = View.GONE
        binding.sectionTasks.root.visibility = View.GONE
        binding.sectionLeaderboard.root.visibility = View.GONE
        binding.sectionWinners.root.visibility = View.GONE
        binding.sectionLegal.root.visibility = View.GONE
    }

    private fun showSection(sectionRoot: View) {
        sectionRoot.visibility = View.VISIBLE
    }

    private fun showLegalModal(title: String, content: String) {
        try {
            val dialog = android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
            val dialogView = layoutInflater.inflate(R.layout.dialog_legal, null)
            dialog.setContentView(dialogView)

            // ✅ USAR LOS IDs CORRECTOS DE TU LAYOUT
            val titleView = dialogView.findViewById<TextView>(R.id.dialog_title)
            val contentView = dialogView.findViewById<TextView>(R.id.dialog_content)
            val closeButton = dialogView.findViewById<ImageView>(R.id.btn_close)
            val understoodButton = dialogView.findViewById<Button>(R.id.btn_understood)

            // ✅ Verificar que los elementos existen
            if (titleView == null || contentView == null || closeButton == null || understoodButton == null) {
                Log.e("LEGAL_MODAL", "❌ Error: No se encontraron todos los elementos del layout")
                showFallbackDialog(title)
                return
            }

            // ✅ Configurar contenido
            titleView.text = title
            contentView.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                android.text.Html.fromHtml(content, android.text.Html.FROM_HTML_MODE_COMPACT)
            } else {
                @Suppress("DEPRECATION")
                android.text.Html.fromHtml(content)
            }

            // ✅ Eventos click
            closeButton.setOnClickListener {
                Log.d("LEGAL_MODAL", "Cerrando modal con X")
                dialog.dismiss()
            }

            understoodButton.setOnClickListener {
                Log.d("LEGAL_MODAL", "Botón entendido presionado")
                dialog.dismiss()
            }

            // ✅ Configurar ventana del modal
            dialog.window?.let { window ->
                window.setLayout(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
                // Hacer que el fondo sea semi-transparente
                window.setBackgroundDrawable(
                    ColorDrawable(Color.parseColor("#B0000000"))
                )
            }

            dialog.setCancelable(true)
            dialog.setOnCancelListener {
                Log.d("LEGAL_MODAL", "Modal cancelado")
            }

            dialog.show()

            Log.d("LEGAL_MODAL", "✅ Modal legal mostrado correctamente: $title")

        } catch (e: Exception) {
            Log.e("LEGAL_MODAL", "❌ Error mostrando modal: ${e.message}")
            showFallbackDialog(title)
        }
    }

    // ✅ Función auxiliar para fallback
    private fun showFallbackDialog(title: String) {
        try {
            AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage("Consulta el contenido completo en la configuración de la app")
                .setPositiveButton("OK", null)
                .show()
        } catch (e: Exception) {
            Log.e("LEGAL_MODAL", "❌ Error en fallback dialog: ${e.message}")
        }
    }

    private fun loadBannerAds() {
        val adRequest = AdRequest.Builder().build()
        binding.adViewBottom.loadAd(adRequest)
        binding.sectionHome.adViewTopHome.loadAd(adRequest)
        binding.sectionTasks.adViewTasks.loadAd(adRequest)
        binding.sectionLeaderboard.adViewLeaderboard.loadAd(adRequest)
    }

    private fun loadRewardedAd() {
        RewardedAd.load(this, REWARDED_AD_UNIT_ID, AdRequest.Builder().build(), object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) { rewardedAd = ad }
            override fun onAdFailedToLoad(error: LoadAdError) { rewardedAd = null }
        })
    }

    private fun requestSpinsByWatchingAd() {
        showRewardedAd {
            SessionManager.addSpins(this, 10)
            currentSpins = SessionManager.getCurrentSpins(this)
            updateSpinCountUI()
        }
    }

    private fun requestTicketsByWatchingAd() {
        showRewardedAd {
            addTicketsToPlayer(20)
        }
    }

    private fun showRewardedAd(onRewarded: (RewardItem) -> Unit) {
        if (currentPlayerId == null) {
            promptForPlayerId()
            return
        }
        if (rewardedAd == null) {
            loadRewardedAd()
            return
        }
        rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() { loadRewardedAd() }
            override fun onAdFailedToShowFullScreenContent(adError: AdError) { rewardedAd = null }
        }
        rewardedAd?.show(this, onRewarded)
    }

    private fun loadInterstitialAd() {
        InterstitialAd.load(this, INTERSTITIAL_AD_UNIT_ID, AdRequest.Builder().build(), object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) { interstitialAd = ad }
            override fun onAdFailedToLoad(error: LoadAdError) { interstitialAd = null }
        })
    }

    private fun showInterstitialAd(onAdDismissed: () -> Unit) {
        if (interstitialAd != null) {
            interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    loadInterstitialAd()
                    onAdDismissed()
                }
                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    interstitialAd = null
                    onAdDismissed()
                }
            }
            interstitialAd?.show(this)
        } else {
            onAdDismissed()
        }
    }

    private fun initializeGrid() {
        binding.sectionHome.gridContainer.removeAllViews()
        gridItemsData.shuffled().forEach { value ->
            val itemView = LayoutInflater.from(this).inflate(R.layout.item_grid, binding.sectionHome.gridContainer, false)
            val valueText = itemView.findViewById<TextView>(R.id.grid_item_value)
            val labelText = itemView.findViewById<TextView>(R.id.grid_item_label)
            valueText.text = value.toString()
            labelText.text = when (value) {
                1 -> "TICKET"
                0 -> "NADA"
                else -> "TICKETS"
            }
            binding.sectionHome.gridContainer.addView(itemView)
        }
    }

    private fun spinGrid() {
        if (isSpinning) return
        if (currentPlayerId == null) {
            promptForPlayerId()
            return
        }
        if (currentSpins <= 0) {
            MaterialDialog(this).show {
                title(text = "¡Sin Giros!")
                message(text = "Necesitas más giros para jugar. ¿Quieres ver un video para obtener 10 giros?")
                positiveButton(text = "Ver Video") { requestSpinsByWatchingAd() }
                negativeButton(text = "Ahora no")
            }
            return
        }
        isSpinning = true
        if (SessionManager.useSpin(this)) {
            currentSpins = SessionManager.getCurrentSpins(this)
            updateSpinCountUI()
        } else {
            isSpinning = false
            return
        }
        binding.sectionHome.spinButton.isEnabled = false
        binding.sectionHome.spinButton.text = "🎟️ GIRANDO..."
        binding.sectionHome.spinButton.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#424242"))
        object : CountDownTimer(3000, 80) {
            val gridItems = binding.sectionHome.gridContainer.children.toList()
            override fun onTick(millisUntilFinished: Long) {
                gridItems.forEach {
                    it.setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.border_color))
                    it.findViewById<TextView>(R.id.grid_item_value).setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_color))
                    it.findViewById<TextView>(R.id.grid_item_label).setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_muted))
                }
                val currentIndex = (0 until gridItems.size).random()
                gridItems[currentIndex].setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.pass_color))
                gridItems[currentIndex].findViewById<TextView>(R.id.grid_item_value).setTextColor(Color.WHITE)
                gridItems[currentIndex].findViewById<TextView>(R.id.grid_item_label).setTextColor(Color.WHITE)
            }
            override fun onFinish() {
                gridItems.forEach {
                    it.setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.border_color))
                    it.findViewById<TextView>(R.id.grid_item_value).setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_color))
                    it.findViewById<TextView>(R.id.grid_item_label).setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_muted))
                }
                val winnerIndex = (0 until gridItems.size).random()
                val winnerView = gridItems[winnerIndex]
                val winnerValueText = winnerView.findViewById<TextView>(R.id.grid_item_value).text.toString()
                val prizeValue = winnerValueText.toIntOrNull() ?: 0
                winnerView.setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.pass_color))
                winnerView.findViewById<TextView>(R.id.grid_item_value).setTextColor(Color.WHITE)
                winnerView.findViewById<TextView>(R.id.grid_item_label).setTextColor(Color.WHITE)
                if (prizeValue > 0) addTicketsToPlayer(prizeValue)
                resetSpinButton()
            }
        }.start()
    }

    private fun resetSpinButton() {
        isSpinning = false
        binding.sectionHome.spinButton.isEnabled = true
        binding.sectionHome.spinButton.text = "🎟️ ¡GIRAR RULETA!"
        binding.sectionHome.spinButton.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#00a8ff"))
    }

    private fun updateSpinCountUI() {
        try {
            val realSpins = SessionManager.getCurrentSpins(this)
            this.currentSpins = realSpins
            binding.sectionHome.spinsAvailable.text = realSpins.toString()
            binding.sectionHome.spinsAvailable.setTextColor(Color.parseColor("#FFD700"))
            binding.sectionHome.getSpinsButton.visibility = if (realSpins <= 0) View.VISIBLE else View.GONE
            if (realSpins <= 0) {
                binding.sectionHome.getSpinsButton.setTextColor(Color.WHITE)
            }
        } catch (e: Exception) {
            Log.e("SPIN_UI", "❌ Error actualizando UI de giros: ${e.message}")
        }
    }

    private fun startWeeklyCountdown() {
        countdownTimer?.cancel()
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        if (calendar.timeInMillis < now) calendar.add(Calendar.WEEK_OF_YEAR, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val diff = calendar.timeInMillis - now
        countdownTimer = object : CountDownTimer(diff, 60000) {
            override fun onTick(millisUntilFinished: Long) {
                val days = TimeUnit.MILLISECONDS.toDays(millisUntilFinished)
                val hours = TimeUnit.MILLISECONDS.toHours(millisUntilFinished) % 24
                val minutes = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished) % 60
                binding.sectionHome.countdown.text = String.format("%dd %dh %dm", days, hours, minutes)
                binding.sectionHome.countdown.setTextColor(Color.WHITE)
            }
            override fun onFinish() {
                binding.sectionHome.countdown.text = "¡Finalizado!"
                binding.sectionHome.countdown.setTextColor(Color.parseColor("#FFD700"))
            }
        }.start()
    }

    private fun signInAnonymously() {
        auth.signInAnonymously().addOnCompleteListener(this) { task ->
            if (!task.isSuccessful) Log.w("Firebase Auth", "signInAnonymously:failure", task.exception)
        }
    }

    private fun showWinnerModal(message: String) {
        try {
            MaterialDialog(this).show {
                title(text = "🎉 ¡FELICITACIONES! 🎉")
                message(text = message)
                positiveButton(text = "¡INCREÍBLE!") { showCelebrationEffects() }
                cancelable(false)
            }
            try {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator else @Suppress("DEPRECATION") getSystemService(VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 100, 500, 100, 500), -1)) else @Suppress("DEPRECATION") vibrator.vibrate(longArrayOf(0, 500, 100, 500, 100, 500), -1)
            } catch (e: Exception) { Log.d("VIBRATION", "No se pudo vibrar: ${e.message}") }
        } catch (e: Exception) { Log.e("WINNER_MODAL", "Error: ${e.message}") }
    }

    private fun showLoserModal(message: String) {
        try {
            MaterialDialog(this).show {
                title(text = "🎭 Sorteo Finalizado")
                message(text = message)
                positiveButton(text = "Entendido")
                cancelable(true)
            }
        } catch (e: Exception) { Log.e("LOSER_MODAL", "Error: ${e.message}") }
    }

    private fun showGeneralModal(title: String, message: String) {
        try {
            MaterialDialog(this).show {
                title(text = title)
                message(text = message)
                positiveButton(text = "OK")
                cancelable(true)
            }
        } catch (e: Exception) { Log.e("GENERAL_MODAL", "Error: ${e.message}") }
    }

    private fun showCelebrationEffects() {
        binding.sectionHome.spinButton.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FFD700"))
        binding.sectionHome.spinButton.text = "🏆 ¡ERES EL GANADOR! 🏆"
        binding.sectionHome.spinButton.postDelayed({
            binding.sectionHome.spinButton.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#00a8ff"))
            binding.sectionHome.spinButton.text = "🎟️ ¡GIRAR RULETA!"
        }, 5000)
    }

    private fun promptForPlayerId() {
        MaterialDialog(this).show {
            title(text = if (currentPlayerId != null) "Editar ID de Jugador" else "Configura tu ID")
            input(hint = "Ingresa tu ID numérico", prefill = currentPlayerId ?: "", inputType = InputType.TYPE_CLASS_NUMBER) { _, text ->
                val newPlayerId = text.toString()
                if (newPlayerId.length >= 5 && newPlayerId.matches(Regex("\\d+"))) {
                    if (currentPlayerId != null && currentPlayerId!!.isNotEmpty()) {
                        AlertDialog.Builder(this@MainActivity)
                            .setTitle("Confirmar cambio de ID")
                            .setMessage("¿Estás seguro de cambiar el ID de jugador de $currentPlayerId a $newPlayerId?")
                            .setPositiveButton("Sí") { _, _ ->
                                SessionManager.clearAllData(this@MainActivity)
                                SessionManager.setPlayerId(this@MainActivity, newPlayerId)
                                currentPlayerId = newPlayerId
                                binding.sectionHome.myPlayerId.text = newPlayerId
                                loadUserData(newPlayerId)
                            }
                            .setNegativeButton("Cancelar", null)
                            .show()
                    } else {
                        SessionManager.setPlayerId(this@MainActivity, newPlayerId)
                        currentPlayerId = newPlayerId
                        binding.sectionHome.myPlayerId.text = newPlayerId
                        loadUserData(newPlayerId)
                    }
                }
            }
            positiveButton(text = "Guardar")
            negativeButton(text = "Cancelar")
        }
    }

    private fun loadUserData(playerId: String) {
        try {
            // ✅ Verificación de baneo al cargar datos del usuario
            checkUserBanStatus(playerId)

            createFirebaseSession(playerId)
            fetchPlayerData(playerId)
            obtenerTop5Firebase()
            updateUI(0, 0)
            val storedPlayerId = SessionManager.getPlayerId(this)
            if (storedPlayerId.isEmpty() || storedPlayerId != playerId) {
                SessionManager.resetSpinsForNewPlayer(this)
                this.currentSpins = 10
            } else {
                this.currentSpins = SessionManager.getCurrentSpins(this)
            }
            updateSpinCountUI()
            checkForPrivateMessages(playerId)
            setupFCMToken(playerId)
            setupNotificationListener(playerId)
        } catch (e: Exception) {
            Log.e("LOAD_USER", "❌ Error cargando datos de usuario: ${e.message}")
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            if (::database.isInitialized && currentPlayerId != null) {
                checkForPrivateMessages(currentPlayerId!!)
            }
        } catch (e: Exception) {
            Log.e("RESUME_FIX", "Error en onResume: ${e.message}")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun checkForPrivateMessages(playerId: String) {
        try {
            if (!::database.isInitialized || playerId.isEmpty()) return
            val messageRef = database.child("privateMessages").child(playerId)
            messageRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val messageData = snapshot.value as? Map<String, Any>
                        if (messageData != null) {
                            val type = messageData["type"] as? String
                            val message = messageData["message"] as? String
                            when (type) {
                                "win", "loss" -> { /* Ignorado, se maneja por notificationQueue */ }
                                else -> showGeneralModal("Notificación", message ?: "Tienes un mensaje nuevo.")
                            }
                            messageRef.removeValue()
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        } catch (e: Exception) {
            Log.e("PRIVATE_MESSAGES", "Error verificando mensajes privados: ${e.message}")
        }
    }

    private fun fetchAllData() {
        try {
            if (!::database.isInitialized) return
            database.child("winners").orderByChild("timestamp").limitToLast(10)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        try {
                            val winners = snapshot.children.mapNotNull {
                                it.getValue(Winner::class.java)
                            }.sortedByDescending { it.timestamp }
                            runOnUiThread {
                                binding.sectionWinners.winnersRecyclerView.adapter = WinnersAdapter(winners)
                            }
                        } catch (e: Exception) {
                            Log.e("FETCH_DATA", "Error procesando winners: ${e.message}")
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
        } catch (e: Exception) {
            Log.e("FETCH_DATA", "Error en fetchAllData: ${e.message}")
        }
    }

    private fun fetchPlayerData(playerId: String) {
        try {
            if (!::database.isInitialized) return
            val playerRef = database.child("players").child(playerId)
            playerRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val tickets = snapshot.child("tickets").getValue(Long::class.java) ?: 0L
                        val passes = snapshot.child("passes").getValue(Long::class.java) ?: 0L
                        updateUI(tickets, passes)
                    } else {
                        val newPlayerData = mapOf("tickets" to 0, "passes" to 0, "lastUpdate" to ServerValue.TIMESTAMP)
                        playerRef.setValue(newPlayerData)
                        updateUI(0, 0)
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        } catch (e: Exception) {
            Log.e("FETCH_PLAYER", "Error obteniendo datos del jugador: ${e.message}")
        }
    }

    private fun addTicketsToPlayer(amount: Int) {
        try {
            if (currentPlayerId == null || !::database.isInitialized) return
            val playerRef = database.child("players").child(currentPlayerId!!)
            playerRef.runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    val currentTickets = currentData.child("tickets").getValue(Long::class.java) ?: 0L
                    val currentPasses = currentData.child("passes").getValue(Long::class.java) ?: 0L

                    // Primero, calculamos el total "virtual" de tickets que tendría el jugador
                    val cumulativeTickets = (currentPasses * 1000) + currentTickets + amount

                    // ✅ CORRECCIÓN: Calcular pases y tickets restantes a partir del total virtual
                    val totalPasses = cumulativeTickets / 1000
                    val remainingTickets = cumulativeTickets % 1000  // 🔧 Esto calcula el residuo

                    currentData.child("tickets").value = remainingTickets  // ✅ Guarda solo los tickets restantes
                    currentData.child("passes").value = totalPasses       // ✅ Guarda el total de pases actualizado
                    currentData.child("lastUpdate").value = ServerValue.TIMESTAMP

                    Log.d("TICKETS_FIX", "Total virtual: $cumulativeTickets → Pases: $totalPasses, Tickets restantes: $remainingTickets")

                    return Transaction.success(currentData)
                }

                override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                    if (committed) {
                        obtenerTop5Firebase()
                    }
                }
            })
        } catch (e: Exception) {
            Log.e("ADD_TICKETS", "Error añadiendo tickets: ${e.message}")
        }
    }


    private fun updateUI(tickets: Long, passes: Long) {
        binding.sectionHome.myTickets.text = tickets.toString()
        binding.sectionHome.myPasses.text = passes.toString()
        binding.sectionHome.passProgress.progress = ((tickets % 1000).toDouble() / 1000.0 * 100).toInt()
        binding.sectionHome.myTickets.setTextColor(Color.parseColor("#FFD700"))
        binding.sectionHome.myPasses.setTextColor(Color.parseColor("#F97316"))
    }

    override fun onDestroy() {
        super.onDestroy()
        marcarSesionInactiva()
    }

    private fun marcarSesionInactiva() {
        try {
            val playerId = SessionManager.getPlayerId(this)
            val sessionId = SessionManager.getSessionId(this)
            if (playerId.isNotEmpty() && sessionId.isNotEmpty() && ::database.isInitialized) {
                database.child("sessions").child(playerId).child(sessionId).child("active").setValue(false)
            }
        } catch (e: Exception) {
            Log.e("SESSION_INACTIVE", "Error en marcarSesionInactiva: ${e.message}")
        }
    }

    private fun setupNotificationListener(playerId: String) {
        try {
            if (!::database.isInitialized) return
            val notificationRef = database.child("notificationQueue").child(playerId)
            notificationRef.addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    processNotification(snapshot)
                }
                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onChildRemoved(snapshot: DataSnapshot) {}
                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(error: DatabaseError) {}
            })
        } catch (e: Exception) {
            Log.e("NOTIFICATION_SETUP", "Error en setupNotificationListener: ${e.message}")
        }
    }

    // ========== FUNCIÓN MODIFICADA ==========
    @Suppress("UNCHECKED_CAST")
    private fun processNotification(snapshot: DataSnapshot) {
        try {
            val notificationData = snapshot.value as? Map<String, Any> ?: return
            if (notificationData["processed"] as? Boolean == true) return
            val type = notificationData["type"] as? String ?: "general"
            val title = notificationData["title"] as? String ?: "Notificación"
            val body = notificationData["body"] as? String ?: ""
            val message = notificationData["message"] as? String ?: ""

            // ✅ AGREGAR MANEJO DE NOTIFICACIONES DE BANEO:
            runOnUiThread {
                when (type) {
                    "ban" -> {
                        val banType = notificationData["ban_type"] as? String ?: "permanent"
                        val expiresAtStr = notificationData["expires_at"] as? String ?: "0"
                        val expiresAt = expiresAtStr.toLongOrNull() ?: 0L
                        showBanScreen(banType, message, expiresAt, currentPlayerId ?: "")
                    }
                    "unban" -> {
                        android.widget.Toast.makeText(this, "Tu cuenta ha sido reactivada", android.widget.Toast.LENGTH_LONG).show()
                    }
                    "win" -> showWinnerModalWithRedirect(message)
                    "loss" -> showLoserModalWithRedirect(message)
                    else -> showGeneralModal(title, message)
                }
            }

            showLocalNotification(title, body, type)
            snapshot.ref.removeValue()
        } catch (e: Exception) {
            Log.e("NOTIFICATION_PROCESS", "Error: ${e.message}")
        }
    }

    private fun showLocalNotification(title: String, body: String, type: String) {
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !notificationManager.areNotificationsEnabled()) return
            val channelId = if (type == "win") "winner_notifications" else "general_notifications"
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                if (type == "win" || type == "loss") putExtra("redirect_to", "winners")
            }
            val pendingIntent = PendingIntent.getActivity(this, 0, intent, if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_UPDATE_CURRENT)
            val notification = androidx.core.app.NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setDefaults(androidx.core.app.NotificationCompat.DEFAULT_ALL)
                .setVibrate(longArrayOf(0, 500, 100, 500))
                .setColor(ContextCompat.getColor(this, R.color.accent_color))
                .build()
            notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        } catch (e: Exception) { Log.e("LOCAL_NOTIFICATION", "Error: ${e.message}") }
    }

    private fun showWinnerModalWithRedirect(message: String) {
        try {
            MaterialDialog(this).show {
                title(text = "🎉 ¡FELICITACIONES! �")
                message(text = message)
                positiveButton(text = "VER GANADORES") {
                    showCelebrationEffects()
                    redirectToWinnersSection()
                }
                cancelable(false)
            }
        } catch (e: Exception) { Log.e("WINNER_MODAL", "Error: ${e.message}") }
    }

    private fun showLoserModalWithRedirect(message: String) {
        try {
            MaterialDialog(this).show {
                title(text = "🎭 Sorteo Finalizado")
                message(text = message)
                positiveButton(text = "VER GANADORES") { redirectToWinnersSection() }
                cancelable(true)
            }
        } catch (e: Exception) { Log.e("LOSER_MODAL", "Error: ${e.message}") }
    }

    private fun redirectToWinnersSection() {
        hideAllSections()
        showSection(binding.sectionWinners.root)
        binding.navView.setCheckedItem(R.id.nav_winners)
    }

    private fun checkNotificationPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermission()
            } else {
                createNotificationChannels()
            }
        } else {
            createNotificationChannels()
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.POST_NOTIFICATIONS)) {
                MaterialDialog(this).show {
                    title(text = "Activar Notificaciones")
                    message(text = "Activa las notificaciones para saber si ganas el sorteo semanal y recibir premios especiales.")
                    positiveButton(text = "Activar") { ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_PERMISSION_REQUEST_CODE) }
                    negativeButton(text = "Ahora no") { createNotificationChannels() }
                }
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_PERMISSION_REQUEST_CODE)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            createNotificationChannels()
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val winnerChannel = NotificationChannel("winner_notifications", "Notificaciones de Ganadores", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Notificaciones cuando ganas el sorteo"
                enableLights(true)
                lightColor = Color.parseColor("#FFD700")
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 500, 1000)
            }
            val generalChannel = NotificationChannel("general_notifications", "Notificaciones Generales", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Notificaciones generales del club de recompensas"
            }
            notificationManager.createNotificationChannel(winnerChannel)
            notificationManager.createNotificationChannel(generalChannel)
        }
    }

    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
    }

    // ==================== FUNCIONES DE VERIFICACIÓN DE BANEO ====================

    private fun checkUserBanStatus(playerId: String) {
        if (playerId.isEmpty()) return

        Log.d("MAIN_BAN_CHECK", "🔍 Verificando estado de baneo para: $playerId")

        BanChecker(this).checkBanStatus(playerId) { banStatus ->
            when (banStatus) {
                is BanChecker.BanStatus.NotBanned -> {
                    Log.d("MAIN_BAN_CHECK", "✅ Usuario no baneado, continuar normalmente")
                    // Usuario no está baneado, continuar con el flujo normal
                }
                is BanChecker.BanStatus.TemporaryBan -> {
                    Log.w("MAIN_BAN_CHECK", "⚠️ Usuario con advertencia temporal")
                    showBanScreen("temporary", banStatus.reason, banStatus.expiresAt, playerId)
                }
                is BanChecker.BanStatus.PermanentBan -> {
                    Log.e("MAIN_BAN_CHECK", "🚫 Usuario baneado permanentemente")
                    showBanScreen("permanent", banStatus.reason, 0L, playerId)
                }
            }
        }
    }

    private fun showBanScreen(banType: String, reason: String, expiresAt: Long, playerId: String) {
        val intent = Intent(this, BanActivity::class.java).apply {
            putExtra(BanActivity.EXTRA_BAN_TYPE, banType)
            putExtra(BanActivity.EXTRA_BAN_REASON, reason)
            putExtra(BanActivity.EXTRA_EXPIRES_AT, expiresAt)
            putExtra(BanActivity.EXTRA_PLAYER_ID, playerId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
