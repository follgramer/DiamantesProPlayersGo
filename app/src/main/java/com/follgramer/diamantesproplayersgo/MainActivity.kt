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
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBarDrawerToggle
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

            Log.d("MAIN_SIMPLE", "🏠 MainActivity iniciada")

            // Inicializar SessionManager básico
            SessionManager.init(this)

            // Manejar intent de notificación
            handleNotificationIntent(intent)

            // ✅ VERIFICAR CONSENTIMIENTO SIMPLE
            checkConsentDirectly()

        } catch (e: Exception) {
            Log.e("MAIN_SIMPLE", "❌ Error en onCreate: ${e.message}")
            // Si hay error, al menos mostrar la app básica
            initializeBasicApp()
        }
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
                showConsentModalSimple()
            }
        } catch (e: Exception) {
            Log.e("CONSENT_SIMPLE", "❌ Error verificando consentimiento: ${e.message}")
            // Si hay error, inicializar de todas formas
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

                // Botón ACEPTAR
                dialogView.findViewById<Button>(R.id.btnAceptarConsentimiento)?.setOnClickListener {
                    Log.d("CONSENT_SIMPLE", "✅ Usuario aceptó consentimiento")
                    val prefs = getSharedPreferences("DiamantesProPrefs", Context.MODE_PRIVATE)
                    prefs.edit().putBoolean("CONSENT_ACCEPTED", true).apply()
                    dialog.dismiss()
                    initializeBasicApp()
                }

                // Botón RECHAZAR
                dialogView.findViewById<Button>(R.id.btnRechazarConsentimiento)?.setOnClickListener {
                    Log.d("CONSENT_SIMPLE", "❌ Usuario rechazó consentimiento")
                    dialog.dismiss()
                    showMustAcceptDialog()
                }

                // Texto legal (opcional)
                dialogView.findViewById<TextView>(R.id.textoLegalCompleto)?.setOnClickListener {
                    // Solo mostrar un toast simple
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
            androidx.appcompat.app.AlertDialog.Builder(this)
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

    // --- Lógica Principal de la Aplicación Simplificada ---

    private fun initializeBasicApp() {
        try {
            Log.d("APP_FIX", "⚡ Inicializando app básica (corregida)...")

            runOnUiThread {
                // Setup de UI inmediato
                setupToolbarAndDrawer()
                setupClickListeners()
                setupRecyclerViews()
                initializeGrid()
                setupBackPressedHandler()

                // Mostrar sección principal
                hideAllSections()
                showSection(binding.sectionHome.root)

                // ✅ SOLO LEE LOS GIROS, NO LOS REGALA AQUÍ
                val playerId = SessionManager.getPlayerId(this@MainActivity)
                this.currentSpins = SessionManager.getCurrentSpins(this@MainActivity)
                updateSpinCountUI()

                Log.d("APP_FIX", "Giros al iniciar (antes del welcome): ${this.currentSpins}")

                // Configurar Player ID UI
                if (playerId.isNotEmpty()) {
                    currentPlayerId = playerId
                    binding.sectionHome.myPlayerId.text = currentPlayerId
                    binding.sectionHome.myPlayerId.setTextColor(Color.parseColor("#00A8FF"))
                } else {
                    binding.sectionHome.myPlayerId.text = "Toca para configurar ✏️"
                    binding.sectionHome.myPlayerId.setTextColor(Color.parseColor("#CCCCCC"))
                }

                // Aplicar colores
                arreglarColoresDeTextos()

                Log.d("APP_FIX", "✅ App básica lista. Cargando componentes pesados...")

                // Cargar componentes pesados en segundo plano
                loadHeavyComponentsLater()

                // Mostrar welcome modal si es necesario
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

                // Inicialización de Firebase y AdMob (puede ser en UI thread, es rápido)
                runOnUiThread {
                    setupFirebase()
                    setupAdMob()
                    loadAllAds()
                }

                // El resto de las tareas pesadas (lectura de datos)
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

                // Tareas finales que no son críticas para el inicio
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

    // --- Lógica de Welcome Modal Simplificada ---

    private fun checkAndShowWelcomeModalSimple() {
        try {
            val prefs = getSharedPreferences("DiamantesProPrefs", Context.MODE_PRIVATE)
            val hasShownWelcome = prefs.getBoolean("HAS_SHOWN_WELCOME", false)

            Log.d("WELCOME_SIMPLE", "Welcome mostrado antes: $hasShownWelcome")

            if (!hasShownWelcome) {
                Log.d("WELCOME_SIMPLE", "Mostrando welcome modal")
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
                window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.parseColor("#CC000000")))
                window.setDimAmount(0.0f)
            }

            // Botón empezar
            dialogView.findViewById<Button>(R.id.btnStartPlaying)?.setOnClickListener {
                Log.d("WELCOME_FIX", "Botón 'Empezar a Jugar' presionado")

                val prefs = getSharedPreferences("DiamantesProPrefs", Context.MODE_PRIVATE)
                prefs.edit().putBoolean("HAS_SHOWN_WELCOME", true).apply()

                dialog.dismiss()

                // ✅ CORREGIDO: Usamos setCurrentSpins para ESTABLECER los giros a 10, no para AÑADIR.
                // Esta es ahora la única fuente de los 10 giros iniciales.
                SessionManager.setCurrentSpins(this, 10)
                this.currentSpins = SessionManager.getCurrentSpins(this)
                updateSpinCountUI()

                Log.d("WELCOME_FIX", "✅ 10 giros de bienvenida establecidos. Total: ${this.currentSpins}")
            }

            // Botón cerrar
            dialogView.findViewById<ImageView>(R.id.btnCloseWelcome)?.setOnClickListener {
                Log.d("WELCOME_FIX", "Botón cerrar presionado")
                // ✅ Al cerrar, también se marcan los giros iniciales para evitar el bug si vuelve a abrir.
                val prefs = getSharedPreferences("DiamantesProPrefs", Context.MODE_PRIVATE)
                if(!prefs.getBoolean("HAS_SHOWN_WELCOME", false)) {
                    SessionManager.setCurrentSpins(this, 10)
                    this.currentSpins = SessionManager.getCurrentSpins(this)
                    updateSpinCountUI()
                    prefs.edit().putBoolean("HAS_SHOWN_WELCOME", true).apply()
                    Log.d("WELCOME_FIX", "✅ 10 giros establecidos al cerrar modal por primera vez.")
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
            Log.d("FCM_TOKEN", "Token FCM obtenido: $token")
            saveTokenToDatabase(playerId, token)
        }
    }

    private fun saveTokenToDatabase(playerId: String, token: String) {
        try {
            if (!::database.isInitialized) {
                Log.w("FCM_TOKEN", "Database no inicializada, no se puede guardar token")
                return
            }

            val tokenData = mapOf(
                "token" to token,
                "timestamp" to ServerValue.TIMESTAMP,
                "deviceInfo" to Build.MODEL
            )

            database.child("playerTokens").child(playerId)
                .setValue(tokenData)
                .addOnSuccessListener {
                    Log.d("FCM_TOKEN", "Token guardado exitosamente para $playerId")
                }
                .addOnFailureListener { error ->
                    Log.e("FCM_TOKEN", "Error guardando token: ${error.message}")
                }
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
            if (!::database.isInitialized) {
                Log.w("SESSION_FIREBASE", "Database no inicializada, no se puede crear sesión")
                return
            }

            val sessionId = SessionManager.getSessionId(this)
            Log.d("SESSION_FIREBASE", "Creando sesión para Player: $playerId, Session: $sessionId")

            val sessionData = mapOf(
                "timestamp" to ServerValue.TIMESTAMP,
                "active" to true,
                "deviceInfo" to Build.MODEL,
                "appVersion" to "1.0"
            )

            database.child("sessions").child(playerId).child(sessionId)
                .setValue(sessionData)
                .addOnSuccessListener {
                    Log.d("SESSION_FIREBASE", "Sesión creada exitosamente")
                    verificarLimiteDeSesiones(playerId)
                }
                .addOnFailureListener { error ->
                    Log.e("SESSION_FIREBASE", "Error creando sesión: ${error.message}")
                }
        } catch (e: Exception) {
            Log.e("SESSION_FIREBASE", "Error en createFirebaseSession: ${e.message}")
        }
    }

    private fun verificarLimiteDeSesiones(playerId: String) {
        try {
            if (!::database.isInitialized) {
                Log.w("SESSION_LIMIT", "Database no inicializada, no se puede verificar límite")
                return
            }

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

                        Log.d("SESSION_LIMIT", "Sesiones activas para $playerId: $sesionesActivas")

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
        androidx.appcompat.app.AlertDialog.Builder(this)
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
        androidx.appcompat.app.AlertDialog.Builder(this)
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
            if (!::database.isInitialized) {
                Log.w("TOP5_FIREBASE", "Database no inicializada, inicializando ahora...")
                setupFirebase() // Forzar inicialización si no existe
            }

            Log.d("TOP5_FIREBASE", "=== OBTENIENDO LEADERBOARD COMPLETO ===")

            database.child("players")
                .addValueEventListener(object : ValueEventListener {
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
                                    // ✅ ASEGURAR QUE EL RECYCLERVIEW EXISTE
                                    if (::binding.isInitialized) {
                                        binding.sectionLeaderboard.leaderboardRecyclerView.adapter = LeaderboardAdapter(jugadoresOrdenados)

                                        if (jugadoresOrdenados.isNotEmpty()) {
                                            val top5 = jugadoresOrdenados.take(5)
                                            val adapter = MiniLeaderboardAdapter(top5, currentPlayerId)
                                            binding.sectionHome.miniLeaderboardList.adapter = adapter
                                            binding.sectionHome.miniLeaderboardList.visibility = View.VISIBLE

                                            Log.d("TOP5_FIREBASE", "✅ Top 5 mostrado correctamente con ${top5.size} jugadores")
                                        } else {
                                            binding.sectionHome.miniLeaderboardList.visibility = View.GONE
                                            Log.w("TOP5_FIREBASE", "⚠️ No hay jugadores para mostrar en top 5")
                                        }

                                        // Mi ranking
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
    }

    private fun setupFirebase() {
        try {
            Log.d("FIREBASE_SETUP", "Inicializando Firebase Database")
            database = FirebaseDatabase.getInstance("https://diamantes-pro-players-go-f3510-default-rtdb.firebaseio.com/").reference
            auth = Firebase.auth
            Log.d("FIREBASE_SETUP", "✅ Firebase Database inicializada correctamente")
        } catch (e: Exception) {
            Log.e("FIREBASE_SETUP", "❌ Error inicializando Firebase: ${e.message}")
            // En caso de error, crear una referencia fallback
            try {
                database = FirebaseDatabase.getInstance().reference
                auth = Firebase.auth
                Log.d("FIREBASE_SETUP", "✅ Firebase Database inicializada con instancia por defecto")
            } catch (fallbackError: Exception) {
                Log.e("FIREBASE_SETUP", "❌ Error crítico inicializando Firebase: ${fallbackError.message}")
            }
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
        androidx.appcompat.app.AlertDialog.Builder(this)
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
            R.id.nav_privacy -> showLegalModal("Política de Privacidad", getString(R.string.privacy_policy_content))
            R.id.nav_terms -> showLegalModal("Términos y Condiciones", getString(R.string.terms_content))
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
        val dialog = android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val dialogView = layoutInflater.inflate(R.layout.dialog_legal, null)
        dialog.setContentView(dialogView)
        val titleView = dialogView.findViewById<TextView>(R.id.dialog_title)
        val contentView = dialogView.findViewById<TextView>(R.id.dialog_content)
        val closeButton = dialogView.findViewById<ImageView>(R.id.btn_close)
        val understoodButton = dialogView.findViewById<Button>(R.id.btn_understood)
        titleView.text = title
        contentView.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            android.text.Html.fromHtml(content, android.text.Html.FROM_HTML_MODE_COMPACT)
        } else {
            @Suppress("DEPRECATION")
            android.text.Html.fromHtml(content)
        }
        closeButton.setOnClickListener { dialog.dismiss() }
        understoodButton.setOnClickListener { dialog.dismiss() }
        dialog.setCancelable(true)
        dialog.show()
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
            val amount = 10
            SessionManager.addSpins(this, amount)
            currentSpins = SessionManager.getCurrentSpins(this)
            updateSpinCountUI()
        }
    }

    private fun requestTicketsByWatchingAd() {
        showRewardedAd {
            val amount = 20
            addTicketsToPlayer(amount)
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
            // ✅ OBTENER GIROS DIRECTAMENTE DE SessionManager
            val realSpins = SessionManager.getCurrentSpins(this)
            this.currentSpins = realSpins // Sincronizar variable global

            binding.sectionHome.spinsAvailable.text = realSpins.toString()
            binding.sectionHome.spinsAvailable.setTextColor(Color.parseColor("#FFD700"))
            binding.sectionHome.getSpinsButton.visibility = if (realSpins <= 0) View.VISIBLE else View.GONE

            if (realSpins <= 0) {
                binding.sectionHome.getSpinsButton.setTextColor(Color.WHITE)
            }

            Log.d("SPIN_UI", "✅ UI actualizada: $realSpins giros disponibles")

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
                        androidx.appcompat.app.AlertDialog.Builder(this@MainActivity)
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
            createFirebaseSession(playerId)
            fetchPlayerData(playerId)
            obtenerTop5Firebase()
            updateUI(0, 0)

            val storedPlayerId = SessionManager.getPlayerId(this)
            if (storedPlayerId.isEmpty() || storedPlayerId != playerId) {
                // ✅ SOLO resetear si realmente cambió el jugador
                Log.d("LOAD_USER", "Player ID cambió, reseteando giros a 10")
                SessionManager.resetSpinsForNewPlayer(this)
                this.currentSpins = 10
            } else {
                // ✅ MANTENER giros existentes
                this.currentSpins = SessionManager.getCurrentSpins(this)
                Log.d("LOAD_USER", "Mismo Player ID, manteniendo giros: ${this.currentSpins}")
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
            // ✅ SOLO verificar mensajes si database está inicializada
            if (::database.isInitialized && currentPlayerId != null) {
                checkForPrivateMessages(currentPlayerId!!)
            } else {
                Log.d("RESUME_FIX", "Database no inicializada o no hay player ID, saltando checkForPrivateMessages")
            }
        } catch (e: Exception) {
            Log.e("RESUME_FIX", "Error en onResume: ${e.message}")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun checkForPrivateMessages(playerId: String) {
        try {
            // ✅ VERIFICAR que database esté inicializada
            if (!::database.isInitialized) {
                Log.w("PRIVATE_MESSAGES", "Database no inicializada, no se pueden verificar mensajes privados")
                return
            }

            if (playerId.isEmpty()) {
                Log.w("PRIVATE_MESSAGES", "Player ID vacío, no se pueden verificar mensajes")
                return
            }

            val messageRef = database.child("privateMessages").child(playerId)
            messageRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val messageData = snapshot.value as? Map<String, Any>
                        if (messageData != null) {
                            val type = messageData["type"] as? String
                            val message = messageData["message"] as? String

                            Log.d("PRIVATE_MESSAGE", "Mensaje recibido - Tipo: $type, Mensaje: $message")
                            when (type) {
                                "win", "loss" -> {
                                    Log.d("PRIVATE_MESSAGE", "Mensaje de sorteo ignorado, será procesado por notificationQueue")
                                }
                                else -> showGeneralModal("Notificación", message ?: "Tienes un mensaje nuevo.")
                            }
                            messageRef.removeValue()
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e("PRIVATE_MESSAGE", "Error escuchando mensajes: ${error.message}")
                }
            })
        } catch (e: Exception) {
            Log.e("PRIVATE_MESSAGES", "Error verificando mensajes privados: ${e.message}")
        }
    }

    private fun fetchAllData() {
        try {
            if (!::database.isInitialized) {
                Log.w("FETCH_DATA", "Database no inicializada, no se pueden obtener datos")
                return
            }

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

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("FETCH_DATA", "Error obteniendo winners: ${error.message}")
                    }
                })
        } catch (e: Exception) {
            Log.e("FETCH_DATA", "Error en fetchAllData: ${e.message}")
        }
    }

    private fun fetchPlayerData(playerId: String) {
        try {
            if (!::database.isInitialized) {
                Log.w("FETCH_PLAYER", "Database no inicializada, no se pueden obtener datos del jugador")
                return
            }

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
                override fun onCancelled(error: DatabaseError) {
                    Log.e("PLAYER_DATA", "Error: ${error.message}")
                }
            })
        } catch (e: Exception) {
            Log.e("FETCH_PLAYER", "Error obteniendo datos del jugador: ${e.message}")
        }
    }

    private fun addTicketsToPlayer(amount: Int) {
        try {
            if (currentPlayerId == null) {
                Log.w("ADD_TICKETS", "No hay player ID configurado")
                return
            }

            if (!::database.isInitialized) {
                Log.w("ADD_TICKETS", "Database no inicializada, no se pueden añadir tickets")
                return
            }

            val playerRef = database.child("players").child(currentPlayerId!!)
            playerRef.runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    val currentTickets = currentData.child("tickets").getValue(Long::class.java) ?: 0L
                    val newTickets = currentTickets + amount
                    val newPasses = newTickets / 1000
                    currentData.child("tickets").value = newTickets
                    currentData.child("passes").value = newPasses
                    currentData.child("lastUpdate").value = ServerValue.TIMESTAMP
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
                database.child("sessions").child(playerId).child(sessionId).child("active")
                    .setValue(false)
                    .addOnSuccessListener {
                        Log.d("SESSION_INACTIVE", "Sesión marcada como inactiva exitosamente")
                    }
                    .addOnFailureListener { error ->
                        Log.e("SESSION_INACTIVE", "Error marcando sesión como inactiva: ${error.message}")
                    }
            } else {
                Log.w("SESSION_INACTIVE", "No se pudo marcar sesión como inactiva - datos faltantes o database no inicializada")
            }
        } catch (e: Exception) {
            Log.e("SESSION_INACTIVE", "Error en marcarSesionInactiva: ${e.message}")
        }
    }

    private fun setupNotificationListener(playerId: String) {
        try {
            if (!::database.isInitialized) {
                Log.w("NOTIFICATION_SETUP", "Database no inicializada, no se puede configurar listener")
                return
            }

            Log.d("NOTIFICATION_SETUP", "Configurando listener para: $playerId")
            val notificationRef = database.child("notificationQueue").child(playerId)

            notificationRef.addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    Log.d("NOTIFICATION_LISTENER", "Nueva notificación detectada: ${snapshot.key}")
                    processNotification(snapshot)
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onChildRemoved(snapshot: DataSnapshot) {}
                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}

                override fun onCancelled(error: DatabaseError) {
                    Log.e("NOTIFICATION_LISTENER", "Error escuchando notificaciones: ${error.message}")
                }
            })
        } catch (e: Exception) {
            Log.e("NOTIFICATION_SETUP", "Error en setupNotificationListener: ${e.message}")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun processNotification(snapshot: DataSnapshot) {
        try {
            val notificationData = snapshot.value as? Map<String, Any> ?: return
            if (notificationData["processed"] as? Boolean == true) return
            val type = notificationData["type"] as? String ?: "general"
            val title = notificationData["title"] as? String ?: "Notificación"
            val body = notificationData["body"] as? String ?: ""
            val message = notificationData["message"] as? String ?: ""
            showLocalNotification(title, body, type)
            runOnUiThread {
                when (type) {
                    "win" -> showWinnerModalWithRedirect(message)
                    "loss" -> showLoserModalWithRedirect(message)
                    else -> showGeneralModal(title, message)
                }
            }
            snapshot.ref.removeValue()
        } catch (e: Exception) { Log.e("NOTIFICATION_PROCESS", "Error: ${e.message}") }
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
                title(text = "🎉 ¡FELICITACIONES! 🎉")
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
}