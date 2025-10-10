package com.follgramer.diamantesproplayersgo

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.*
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.text.InputType
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.children
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import com.follgramer.diamantesproplayersgo.ads.AdFallbackStrategy
import com.follgramer.diamantesproplayersgo.ads.AdManager
import com.follgramer.diamantesproplayersgo.ads.AdsInit
import com.follgramer.diamantesproplayersgo.ads.BannerHelper
import com.follgramer.diamantesproplayersgo.databinding.ActivityMainBinding
import com.follgramer.diamantesproplayersgo.notifications.*
import com.follgramer.diamantesproplayersgo.ui.NotificationCenterActivity
import com.follgramer.diamantesproplayersgo.ui.Onboarding
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.android.ump.UserMessagingPlatform
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.*
import me.leolin.shortcutbadger.ShortcutBadger
import java.io.IOException
import java.security.MessageDigest
import java.util.*
import java.util.concurrent.TimeUnit

// Agregar esta extensión después de los imports
private fun Activity.ensureSystemBarsVisible() {
    window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
}

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var commManager: CommunicationManager

    // Audio resources
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var hasAudioFocus = false

    // FIREBASE RESOURCES
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

    // STATE VARIABLES
    private var currentPlayerId: String? = null
    private var currentSpins: Int = 0
    private var isSpinning = false
    private var bannersLoaded = false // Mantenemos para control

    // FIREBASE LISTENERS
    private val firebaseListeners = mutableListOf<ValueEventListener>()
    private val firebaseReferences = mutableListOf<DatabaseReference>()
    private var weeklyPrizeRef: DatabaseReference? = null
    private var weeklyPrizeListener: ValueEventListener? = null

    // TIMERS
    private var countdownTimer: CountDownTimer? = null
    private var revealCountdownTimer: CountDownTimer? = null

    // Agregar después de las otras propiedades de la clase
    private var currentSorteoState = "normal" // normal, processing, reveal, revealed

    // GRID DATA
    private val gridItemsData = listOf(5, 1, 10, 25, 20, 0, 50, 5, 15, 1, 100, 20)

    private var leaderboardAdapter: LeaderboardAdapter? = null
    private var winnersAdapter: WinnersAdapter? = null

    // Agregar estas propiedades al inicio de tu MainActivity
    private var onlineStatusJob: Job? = null

    // CONSTANTS
    private companion object {
        private const val TAG_MAIN = "MainActivity"
        private const val TAG_ADMOB = "AdMob_MainActivity"
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            // ... initial setup and logging ...
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)
            window.apply { statusBarColor = Color.parseColor("#0A0F14") }

            setupWindowInsets()
            initializeCriticalSystems() // Initializes Firebase auth and database

            // VERIFICAR BANEO ANTES DE CONTINUAR
            checkBanStatus {
                // Solo continuar si NO está baneado
                binding.root.post {
                    try {
                        setupBasicUI()
                        scheduleBackgroundInitialization()
                        Log.d(TAG_MAIN, "onCreate completado exitosamente")
                    } catch (e: Exception) {
                        Log.e(TAG_MAIN, "Error en post-onCreate: ${e.message}")
                        handleCriticalError(e)
                    }
                }

                startBadgeUpdater()

                startOnlineStatusUpdates()
                setupNotificationListener()
                showLastNotificationOnOpen()
                loadWeeklyPrizes()
                loadSorteoButtonConfig()
            }

            if (BuildConfig.DEBUG) {
                // ... debug stuff ...
            }

        } catch (e: Exception) {
            Log.e(TAG_MAIN, "Error crítico en onCreate: ${e.message}")
            handleCriticalError(e)
        }
    }

    private fun startOnlineStatusUpdates() {
        val userId = auth.currentUser?.uid ?: return
        onlineStatusJob = lifecycleScope.launch {
            while (isActive) {
                // Actualizar estado online cada 30 segundos
                database
                    .child("onlineUsers")
                    .child(userId)
                    .child("lastActivity")
                    .setValue(com.google.firebase.database.ServerValue.TIMESTAMP)
                delay(30000) // 30 segundos
            }
        }
    }

    private fun setupWindowInsets() {
        // Configurar para que el contenido NO se extienda bajo las barras del sistema
        WindowCompat.setDecorFitsSystemWindows(window, true)

        // Color de la barra de estado
        window.statusBarColor = Color.parseColor("#0A0F14") // Tu color primary_bg

        // NO aplicar padding adicional al root
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            insets // Solo retornar los insets sin aplicar padding
        }
    }

    private fun initializeCriticalSystems() {
        try {
            Log.d(TAG_MAIN, "Inicializando sistemas críticos...")
            setupAudioManager()

            if (!AdsInit.isAdMobReady()) {
                Log.w(TAG_MAIN, "⚠ AdMob NO fue inicializado en SplashActivity")
            } else {
                Log.d(TAG_MAIN, "✅ AdMob correctamente inicializado desde SplashActivity")
            }

            setupFirebase()
            commManager = CommunicationManager.getInstance(this)
            initializeNotificationSystem()
            setupEmergencyStopListener() // Listener de parada de emergencia
            SessionManager.init(this)

            if (BuildConfig.DEBUG) {
                showDeviceIdForTesting()
            }

            Log.d(TAG_MAIN, "Sistemas críticos inicializados correctamente")
        } catch (e: Exception) {
            Log.e(TAG_MAIN, "Error en inicialización crítica: ${e.message}")
            handleCriticalError(e)
        }
    }

    private fun loadBannersWhenReady() {
        if (bannersLoaded) {
            Log.d(TAG_MAIN, "Banners ya cargados, saltando...")
            return
        }
        lifecycleScope.launch(Dispatchers.Main) {
            try {
                Log.d(TAG_MAIN, "🎯 Preparando carga de banners...")
                // Esperar a que AdMob esté listo (máximo 15 segundos)
                var waitTime = 0
                while (!AdsInit.isAdMobReady() && waitTime < 15000) {
                    Log.d(TAG_MAIN, "⏳ Esperando AdMob... (${waitTime}ms)")
                    delay(500)
                    waitTime += 500
                }
                if (!AdsInit.isAdMobReady()) {
                    Log.e(TAG_MAIN, "❌ AdMob no está listo después de 15 segundos")
                    delay(5000)
                    loadBannersWhenReady()
                    return@launch
                }
                // Esperar consentimiento (máximo 15 segundos)
                waitTime = 0
                while (!UserMessagingPlatform.getConsentInformation(this@MainActivity).canRequestAds()
                    && waitTime < 15000) {
                    Log.d(TAG_MAIN, "⏳ Esperando consentimiento... (${waitTime}ms)")
                    delay(500)
                    waitTime += 500
                }
                if (!UserMessagingPlatform.getConsentInformation(this@MainActivity).canRequestAds()) {
                    Log.e(TAG_MAIN, "❌ No se puede solicitar anuncios sin consentimiento")
                    delay(5000)
                    loadBannersWhenReady()
                    return@launch
                }
                if (bannersLoaded) {
                    Log.d(TAG_MAIN, "Banners cargados por otra rutina")
                    return@launch
                }
                Log.d(TAG_MAIN, "✅ Condiciones cumplidas, iniciando carga de banners...")
                // ✅ FORZAR VISIBILIDAD ANTES DE CARGAR
                binding.sectionHome.adInProfileContainer.visibility = View.VISIBLE
                binding.bannerBottomContainer.visibility = View.VISIBLE
                // Cargar banner superior
                val topContainer = binding.sectionHome.adInProfileContainer
                topContainer.post {
                    Log.d(TAG_MAIN, "📤 Cargando banner superior...")
                    BannerHelper.attachAdaptiveBanner(this@MainActivity, topContainer)
                }
                // Cargar banner inferior con delay
                delay(1000)
                val bottomContainer = binding.bannerBottomContainer
                bottomContainer.post {
                    Log.d(TAG_MAIN, "📤 Cargando banner inferior...")
                    BannerHelper.attachAdaptiveBanner(this@MainActivity, bottomContainer)
                }
                bannersLoaded = true
                Log.d(TAG_MAIN, "✅ Proceso de carga de banners completado")
            } catch (e: Exception) {
                Log.e(TAG_MAIN, "❌ Error crítico en loadBannersWhenReady: ${e.message}", e)
                delay(10000)
                loadBannersWhenReady()
            }
        }
    }


    private fun showDeviceIdForTesting() {
        if (BuildConfig.DEBUG) {
            try {
                val androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
                val deviceId = MessageDigest.getInstance("MD5").digest(androidId.toByteArray())
                    .joinToString("") { "%02x".format(it) }.uppercase()
                Log.e("DEVICE_ID", "==========================================")
                Log.e("DEVICE_ID", "ID de Dispositivo de Prueba (AdMob): $deviceId")
                Log.e("DEVICE_ID", "⚠ SOLO PARA DEBUG - En RELEASE no aparecerá")
                Log.e("DEVICE_ID", "==========================================")
            } catch (e: Exception) {
                Log.e("DEVICE_ID", "Error generando Device ID: ${e.message}")
            }
        }
    }

    private fun setupAdminSyncListener(playerId: String) {
        try {
            if (!::database.isInitialized || playerId.isEmpty()) return

            Log.d(TAG_MAIN, "🚨 Setting up admin sync listener for: ***${playerId.takeLast(3)}")

            val playerRef = database.child("players").child(playerId)
            val adminSyncListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) return

                    try {
                        val firebaseSpins = snapshot.child("spins").getValue(Long::class.java)?.toInt() ?: currentSpins
                        val firebaseTickets = snapshot.child("tickets").getValue(Long::class.java) ?: 0L
                        val firebasePasses = snapshot.child("passes").getValue(Long::class.java) ?: 0L

                        val localSpins = SessionManager.getCurrentSpins(this@MainActivity)
                        if (firebaseSpins != localSpins && firebaseSpins >= 0) {
                            Log.d("ADMIN_SYNC", "🎁 Spins updated from admin: $localSpins -> $firebaseSpins")
                            SessionManager.setCurrentSpins(this@MainActivity, firebaseSpins)
                            currentSpins = firebaseSpins
                            updateSpinCountUI()
                        }

                        runOnUiThread {
                            updateUI(firebaseTickets, firebasePasses)
                        }

                    } catch (e: Exception) {
                        Log.e("ADMIN_SYNC", "❌ Error processing changes from admin: ${e.message}")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("ADMIN_SYNC", "❌ Error in admin listener: ${error.message}")
                }
            }

            playerRef.addValueEventListener(adminSyncListener)
            firebaseListeners.add(adminSyncListener)
            firebaseReferences.add(playerRef)

            Log.d(TAG_MAIN, "✅ Admin listener configured correctly")

        } catch (e: Exception) {
            Log.e(TAG_MAIN, "❌ Error configuring admin listener: ${e.message}")
        }
    }

    private fun handleCriticalError(error: Exception) {
        Log.e(TAG_MAIN, "Critical error: ${error.message}")
        try {
            binding.sectionHome.root.visibility = View.VISIBLE
        } catch (e: Exception) {
            Log.e(TAG_MAIN, "Error even in emergency mode: ${e.message}")
        }
    }

    private fun setupBasicUI() {
        try {
            Log.d(TAG_MAIN, "Configurando basic UI...")
            setupToolbarAndDrawer()
            setupClickListeners()
            setupRecyclerViews()
            setupBackPressedHandler()
            hideAllSections()
            showSectionSafely(binding.sectionHome.root)
            // ✅ CONFIGURACIÓN CORRECTA DE CONTENEDORES DE BANNERS
            val homeContainer = binding.sectionHome.adInProfileContainer
            val bottomContainer = binding.bannerBottomContainer
            // Limpiar contenedores
            homeContainer.removeAllViews()
            bottomContainer.removeAllViews()
            // ✅ CONFIGURAR VISIBILIDAD Y ALTURA CORRECTAMENTE
            homeContainer.visibility = View.VISIBLE
            bottomContainer.visibility = View.VISIBLE
            // ✅ ESTABLECER ALTURA MÍNIMA
            homeContainer.layoutParams = homeContainer.layoutParams.apply {
                height = ViewGroup.LayoutParams.WRAP_CONTENT
            }
            bottomContainer.layoutParams = bottomContainer.layoutParams.apply {
                height = ViewGroup.LayoutParams.WRAP_CONTENT
            }
            // Sin background interfiriendo
            homeContainer.background = null
            bottomContainer.background = null
            currentSpins = SessionManager.getCurrentSpins(this)
            val playerId = SessionManager.getPlayerId(this)
            updateSpinCountUI()
            updatePlayerIdUI(playerId.ifEmpty { null })
            Log.d(TAG_MAIN, "Basic UI configurado correctamente")
        } catch (e: Exception) {
            Log.e(TAG_MAIN, "Error configurando basic UI: ${e.message}")
        }
    }


    private fun scheduleBackgroundInitialization() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Paso 1: Esperar que AdMob realmente esté listo
                delay(2000L) // Aumentado de 500ms a 2s
                initializeAdvertisingId()
                // Paso 2: Verificar que AdMob está 100% listo
                withContext(Dispatchers.Main) {
                    var attempts = 0
                    while (!AdsInit.isAdMobReady() && attempts < 15) {
                        Log.d(TAG_MAIN, "Esperando AdMob... intento $attempts")
                        delay(1000)
                        attempts++
                    }
                    if (!AdsInit.isAdMobReady()) {
                        Log.e(TAG_MAIN, "AdMob NO inicializado después de 15 segundos")
                        return@withContext
                    }
                    Log.d(TAG_MAIN, "AdMob confirmado listo")
                }
                val playerId = SessionManager.getPlayerId(this@MainActivity)
                if (playerId.isNotEmpty()) {
                    initializeUserSession(playerId)
                }
                withContext(Dispatchers.Main) {
                    // Paso 3: Inicializar AdManager
                    AdManager.initialize(this@MainActivity)
                    delay(2000) // Esperar que AdManager precargue
                    // Paso 4: Finalizar UI
                    finalizeUIInitialization(playerId)
                    // Paso 5: AHORA SÍ cargar banners (después de TODO lo demás)
                    delay(3000L) // Total: ~10 segundos desde onCreate
                    loadBannersWhenReady()
                }
            } catch (e: Exception) {
                Log.e(TAG_MAIN, "Error en background initialization: ${e.message}")
            }
        }
    }

    private suspend fun initializeAdvertisingId() {
        try {
            withContext(Dispatchers.IO) {
                val adInfo = AdvertisingIdClient.getAdvertisingIdInfo(applicationContext)
                val advertisingId = adInfo.id
                val isLimitAdTrackingEnabled = adInfo.isLimitAdTrackingEnabled

                Log.d("ADVERTISING_ID", "Advertising ID inicializado: ${advertisingId?.take(8)}...")
                Log.d("ADVERTISING_ID", "Limit Ad Tracking: $isLimitAdTrackingEnabled")
            }
        } catch (e: IOException) {
            Log.w("ADVERTISING_ID", "Error de red obteniendo Advertising ID: ${e.message}")
        } catch (e: GooglePlayServicesNotAvailableException) {
            Log.w("ADVERTISING_ID", "Google Play Services no disponible: ${e.message}")
        } catch (e: GooglePlayServicesRepairableException) {
            Log.w("ADVERTISING_ID", "Google Play Services reparable: ${e.message}")
        } catch (e: Exception) {
            Log.e("ADVERTISING_ID", "Error general obteniendo Advertising ID: ${e.message}")
        }
    }

    private suspend fun initializeUserSession(playerId: String) {
        try {
            currentPlayerId = playerId

            withContext(Dispatchers.IO) {
                createFirebaseSession(playerId)
                setupFCMToken(playerId)
            }

            withContext(Dispatchers.Main) {
                checkUserBanStatus(playerId)
                setupAdminSyncListener(playerId)
                fetchPlayerData(playerId)
            }
        } catch (e: Exception) {
            Log.e(TAG_MAIN, "❌ Error in initializeUserSession: ${e.message}")
        }
    }

    private fun finalizeUIInitialization(playerId: String) {
        try {
            Log.d(TAG_MAIN, "Finalizando inicialización de UI...")
            if (!::binding.isInitialized) {
                Log.e(TAG_MAIN, "Binding no disponible en finalizeUIInitialization")
                return
            }
            arreglarColoresDeTextos()
            initializeGrid()
            obtenerTop5Firebase()
            startWeeklyPrizeListener()
            Handler(Looper.getMainLooper()).postDelayed({
                setupSorteoControlListener()
            }, 2000)
            fetchAllData()
            startWeeklyCountdown()
            checkNotificationPermissions()
            Onboarding.showIfNeeded(this)
            handleNotificationIntent(intent)
            if (playerId.isEmpty() && auth.currentUser == null) {
                // Se autenticará desde setupFirebase si es necesario
            }
            Log.d(TAG_MAIN, "UI inicializada completamente")
        } catch (e: Exception) {
            Log.e(TAG_MAIN, "Error en finalizeUIInitialization: ${e.message}")
        }
    }

    override fun onStart() {
        super.onStart()

        lifecycleScope.launch(Dispatchers.Main) {
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            ensureSystemBarsVisible()
            Log.d(TAG_MAIN, "onResume - Verificando estado")
            currentSpins = SessionManager.getCurrentSpins(this)
            updateSpinCountUI()
            updateNotificationBadge()
            if (currentSorteoState == "normal") {
                showNormalState()
            }

            if (::binding.isInitialized) {
                Log.d(TAG_MAIN, "Resumiendo banners...")
                BannerHelper.resume(binding.root)
            }

            lifecycleScope.launch {
                val unreadCount = AppNotificationManager.getInstance(this@MainActivity).getUnreadCount()
                Log.d(TAG_MAIN, "📬 Unread notifications: $unreadCount")
            }
        } catch (e: Exception) {
            Log.e(TAG_MAIN, "Error en onResume: ${e.message}")
        }
    }


    override fun onPause() {
        super.onPause()
        countdownTimer?.cancel()
        revealCountdownTimer?.cancel()

        try {
            if (::binding.isInitialized) {
                Log.d(TAG_MAIN, "Pausando banners...")
                BannerHelper.pause(binding.root)
            }
        } catch (e: Exception) {
            Log.e(TAG_MAIN, "Error pausando banners: ${e.message}")
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        Log.d(TAG_MAIN, "📲 onNewIntent called")
        handleNotificationIntent(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        onlineStatusJob?.cancel()
        commManager.cleanup()
        try {
            if (::binding.isInitialized) {
                Log.d(TAG_MAIN, "Destruyendo banners...")
                BannerHelper.destroy(binding.root)
            }

            Log.d(TAG_MAIN, "🛑 Deteniendo listeners de notificación")
            AppNotificationManager.getInstance(this).stopListening()
            NotificationEventBus.unsubscribe { }

        } catch (e: Exception) {
            Log.e(TAG_MAIN, "Error en onDestroy: ${e.message}")
        }
        cleanupResources()
    }


    private fun cleanupResources() {
        try {
            Log.d(TAG_MAIN, "Cleaning up resources...")
            releaseAudioFocus()
            countdownTimer?.cancel()
            revealCountdownTimer?.cancel()
            countdownTimer = null
            revealCountdownTimer = null
            weeklyPrizeListener?.let { listener ->
                weeklyPrizeRef?.removeEventListener(listener)
            }
            weeklyPrizeListener = null
            weeklyPrizeRef = null
            firebaseListeners.forEach { listener ->
                firebaseReferences.forEach { ref ->
                    try {
                        ref.removeEventListener(listener)
                    } catch (e: Exception) {
                        Log.w(TAG_MAIN, "Error removing listener: ${e.message}")
                    }
                }
            }
            firebaseListeners.clear()
            firebaseReferences.clear()
            audioManager = null
            audioFocusRequest = null
            hasAudioFocus = false
            AdManager.cleanup()
            // NativeAdHelper.cleanup() // ❌ COMENTADO
            marcarSesionInactiva()
            Log.d(TAG_MAIN, "Resources cleaned successfully")
        } catch (e: Exception) {
            Log.e(TAG_MAIN, "Error cleaning up resources: ${e.message}")
        }
    }

    private fun handleNotificationIntent(intent: Intent?) {
        intent?.let { it ->
            Log.d(TAG_MAIN, "📲 Intent received: ${it.extras}")

            when {
                it.getBooleanExtra("open_notification_center", false) -> {
                    Log.d(TAG_MAIN, "Opening notification center from intent")
                    binding.root.postDelayed({
                        val notifIntent = Intent(this, NotificationCenterActivity::class.java)
                        startActivity(notifIntent)
                    }, 500)
                }
                it.getStringExtra("redirect_to") == "winners" -> {
                    binding.root.postDelayed({
                        redirectToWinnersSection()
                    }, 500)
                }
                it.getStringExtra("notification_type") == "winner" -> {
                    binding.root.postDelayed({
                        showCelebrationEffects()
                    }, 1000)
                }
                else -> {
                    // Needed to make the when exhaustive (and not break compilation)
                    Log.d(TAG_MAIN, "Intent with no known action")
                }
            }
        }
    }

    private fun setupFCMToken(playerId: String) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM_TOKEN", "Error getting FCM token", task.exception)
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
            Log.e("FCM_TOKEN", "Error in saveTokenToDatabase: ${e.message}")
        }
    }

    private fun setupFirebase() {
        try {
            database = FirebaseDatabase.getInstance("https://diamantes-pro-players-go-f3510-default-rtdb.firebaseio.com/").reference
            auth = Firebase.auth
            if (auth.currentUser == null) {
                signInAnonymously()
            }
            Log.d("FIREBASE_SETUP", "Firebase configured correctly")
        } catch (e: Exception) {
            Log.e("FIREBASE_SETUP", "Error initializing Firebase: ${e.message}")
        }
    }

    private fun initializeNotificationSystem() {
        val notificationManager = AppNotificationManager.getInstance(this)
        NotificationEventBus.unsubscribe { }
        NotificationEventBus.subscribe { event ->
            runOnUiThread {
                when (event) {
                    is WinnerEvent -> {
                        Log.d(TAG_MAIN, "🏆 Winner event received: ${event.message}")
                        notificationManager.showSystemNotification(
                            "winner",
                            "🏆 CONGRATULATIONS!",
                            event.message,
                            NotificationCompat.PRIORITY_HIGH
                        )
                        showWinnerModal(event.message)
                        vibratePattern(longArrayOf(0, 500, 100, 500, 100, 1000))
                    }
                    is LoserEvent -> {
                        Log.d(TAG_MAIN, "📣 Loser event received: ${event.message}")
                        notificationManager.showSystemNotification(
                            "loser",
                            "📣 Draw Finished",
                            event.message,
                            NotificationCompat.PRIORITY_DEFAULT
                        )
                        showLoserModal(event.message)
                    }
                    is GiftEvent -> {
                        Log.d(TAG_MAIN, "🎁 Gift event received: ${event.amount} ${event.unit}")
                        val giftMessage = when (event.unit) {
                            "passes" -> "You have received ${event.amount} passes"
                            "tickets" -> "You have received ${event.amount} tickets"
                            "spins" -> "You have received ${event.amount} spins"
                            else -> event.message
                        }
                        notificationManager.showSystemNotification(
                            "gift",
                            "🎁 Gift Received",
                            giftMessage,
                            NotificationCompat.PRIORITY_DEFAULT
                        )
                        showGeneralModal("🎁 Gift Received", giftMessage)
                        currentPlayerId?.let { fetchPlayerData(it) }
                    }
                    is BanEvent -> {
                        Log.d(TAG_MAIN, "🚫 Ban event received")
                        showBanScreen(event.banType, event.reason, event.expiresAt, currentPlayerId ?: "")
                    }
                    is UnbanEvent -> {
                        Log.d(TAG_MAIN, "✅ Unban event received")
                        currentPlayerId?.let { fetchPlayerData(it) }
                    }
                    is GeneralEvent -> {
                        Log.d(TAG_MAIN, "📣 General event received: ${event.title}")
                        showGeneralModal(event.title, event.message)
                    }
                    is CounterUpdatedEvent -> {
                        updateNotificationBadge()
                    }
                    else -> {
                        Log.w(TAG_MAIN, "Event not handled")
                    }
                }
            }
        }

        setupNotificationButton()
        updateNotificationBadge()

        val playerId = SessionManager.getPlayerId(this)
        if (playerId.isNotEmpty()) {
            Log.d(TAG_MAIN, "🚀 Starting notification listener for: $playerId")
            notificationManager.startListening(playerId)
        }
    }

    private fun setupNotificationButton() {
        binding.toolbar.findViewById<View>(R.id.notificationButton)?.setOnClickListener {
            val intent = Intent(this, NotificationCenterActivity::class.java)
            startActivity(intent)
        }
    }

    private fun startBadgeUpdater() {
        lifecycleScope.launch {
            while (isActive) {
                updateNotificationBadge()
                delay(5000) // Update every 5 seconds
            }
        }
    }

    private fun updateNotificationBadge() {
        lifecycleScope.launch {
            try {
                // CAMBIO: Usar el nombre correcto de la clase
                val unreadCount = AppNotificationManager
                    .getInstance(this@MainActivity)
                    .getUnreadCount()
                withContext(Dispatchers.Main) {
                    // Update badge on toolbar
                    val badge = binding.toolbar.findViewById<TextView>(R.id.notificationBadge)
                    badge?.apply {
                        if (unreadCount > 0) {
                            visibility = View.VISIBLE
                            text = if (unreadCount > 99) "99+" else unreadCount.toString()
                            // Pulse animation
                            if (scaleX == 1f) { // Only animate if not already animating
                                animate()
                                    .scaleX(1.2f)
                                    .scaleY(1.2f)
                                    .setDuration(300)
                                    .withEndAction {
                                        animate()
                                            .scaleX(1f)
                                            .scaleY(1f)
                                            .setDuration(300)
                                    }
                            }
                        } else {
                            visibility = View.GONE
                        }
                    }
                    // Update launcher badge (app icon)
                    updateLauncherBadge(unreadCount)
                }
            } catch (e: Exception) {
                Log.e(TAG_MAIN, "Error updating badge: ${e.message}")
            }
        }
    }

    private fun updateLauncherBadge(count: Int) {
        try {
            // Use ShortcutBadger for compatibility with multiple launchers
            val success = ShortcutBadger.applyCount(this, count)
            if (!success) {
                // Alternative attempt for Samsung
                val intent = Intent("android.intent.action.BADGE_COUNT_UPDATE").apply {
                    putExtra("badge_count_package_name", packageName)
                    putExtra(
                        "badge_count_class_name", ComponentName(
                            packageName,
                            MainActivity::class.java.name
                        ).className
                    )
                    putExtra("badge_count", count)
                }
                sendBroadcast(intent)
            }
            Log.d(TAG_MAIN, "Badge updated: $count")
        } catch (e: Exception) {
            Log.e(TAG_MAIN, "Error with launcher badge: ${e.message}")
        }
    }

    private fun resetToNormalState() {
        try {
            showNormalState()
        } catch (e: Exception) {
            Log.e(TAG_MAIN, "Error in resetToNormalState: ${e.message}")
        }
    }

    private fun arreglarColoresDeTextos() {
        try {
            aplicarColoresATodosLosTextos(binding.sectionHome.root)
            aplicarColoresATodosLosTextos(binding.sectionTasks.root)
            aplicarColoresATodosLosTextos(binding.sectionLeaderboard.root)
            aplicarColoresATodosLosTextos(binding.sectionWinners.root)
        } catch (e: Exception) {
            Log.e("COLOR_FIX", "Error applying colors: ${e.message}")
        }
    }

    private fun aplicarColoresATodosLosTextos(view: View) {
        try {
            when (view) {
                is TextView -> {
                    val currentColor = view.currentTextColor
                    if (Color.alpha(currentColor) < 200 ||
                        (Color.red(currentColor) + Color.green(currentColor) + Color.blue(currentColor)) < 400
                    ) {
                        val newColor = when {
                            view.textSize >= 18f -> Color.WHITE
                            view.textSize >= 14f -> Color.parseColor("#CCCCCC")
                            else -> Color.parseColor("#B0B0B0")
                        }
                        view.setTextColor(newColor)
                    }
                }
                is ViewGroup -> {
                    for (i in 0 until view.childCount) {
                        aplicarColoresATodosLosTextos(view.getChildAt(i))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("COLOR_FIX", "Error in applying colors to all texts: ${e.message}")
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
            Log.e("SESSION_FIREBASE", "Error in createFirebaseSession: ${e.message}")
        }
    }

    private fun verificarLimiteDeSesiones(playerId: String) {
        try {
            if (!::database.isInitialized) return
            val sessionsRef = database.child("sessions").child(playerId)

            val listener = object : ValueEventListener {
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
                        Log.e("SESSION_LIMIT", "Error processing sessions: ${e.message}")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("SESSION_LIMIT", "Error checking sessions: ${error.message}")
                }
            }

            sessionsRef.addListenerForSingleValueEvent(listener)
            firebaseListeners.add(listener)
            firebaseReferences.add(sessionsRef)
        } catch (e: Exception) {
            Log.e("SESSION_LIMIT", "Error in verificarLimiteDeSesiones: ${e.message}")
        }
    }

    private fun mostrarModalExcesoSesiones(cantidad: Int) {
        AlertDialog.Builder(this)
            .setTitle("Too many sessions")
            .setMessage(
                "Your ID is being used on $cantidad devices at the same time.\n\n" +
                        "Only a maximum of 3 simultaneous devices are allowed.\n\n" +
                        "If you do not recognize these sessions, contact:\nfollgramer@gmail.com"
            )
            .setCancelable(false)
            .setPositiveButton("Close App") { _, _ ->
                finishAffinity()
            }
            .setNegativeButton("More Info") { _, _ ->
                // No se necesita
            }
            .show()
    }

    private fun obtenerTop5Firebase() {
        try {
            DiamantesApplication.cachedTop5?.let { cached ->
                Log.d("TOP5_FIREBASE", "📜 Displaying TOP 5 from cache (${cached.size} players)")
                val cachedPlayers = cached.map {
                    Player(it.playerId, it.tickets, it.passes)
                }
                runOnUiThread {
                    updateLeaderboardUI(cachedPlayers)
                }
            }
            if (!::database.isInitialized) setupFirebase()
            val playersRef = database.child("players")
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        val jugadores = mutableListOf<Player>()
                        for (playerSnapshot in snapshot.children) {
                            val playerId = playerSnapshot.key ?: continue
                            val tickets = playerSnapshot.child("tickets").getValue(Long::class.java) ?: 0L
                            val passes = playerSnapshot.child("passes").getValue(Long::class.java) ?: 0L
                            if (playerId.matches(Regex("\\d+")) && playerId.length >= 5) {
                                jugadores.add(Player(playerId, tickets, passes))
                            }
                        }
                        val jugadoresOrdenados = jugadores.sortedWith(
                            compareByDescending<Player> { it.passes }
                                .thenByDescending { it.tickets }
                        )
                        DiamantesApplication.cachedTop5 = jugadoresOrdenados.take(5).map {
                            DiamantesApplication.CachedPlayer(it.playerId, it.tickets, it.passes)
                        }
                        DiamantesApplication.top5LastUpdate = System.currentTimeMillis()
                        runOnUiThread {
                            updateLeaderboardUI(jugadoresOrdenados)
                        }
                    } catch (e: Exception) {
                        Log.e("TOP5_FIREBASE", "Error processing data: ${e.message}")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("TOP5_FIREBASE", "Firebase ERROR: ${error.message}")
                }
            }
            playersRef.addValueEventListener(listener)
            firebaseListeners.add(listener)
            firebaseReferences.add(playersRef)
        } catch (e: Exception) {
            Log.e("TOP5_FIREBASE", "General error: ${e.message}")
        }
    }

    private fun updateLeaderboardUI(jugadoresOrdenados: List<Player>) {
        try {
            if (::binding.isInitialized) {
                // Update main leaderboard
                val leaderboardItems = jugadoresOrdenados.map { player ->
                    LeaderboardItem(
                        playerId = player.playerId,
                        playerName = player.playerId,
                        passes = player.passes,
                        tickets = player.tickets
                    )
                }
                // Actualizar datos del adapter existente
                leaderboardAdapter?.updateData(leaderboardItems, currentPlayerId)
                // Mini leaderboard
                if (jugadoresOrdenados.isNotEmpty()) {
                    val top5 = jugadoresOrdenados.take(5)
                    binding.sectionHome.miniLeaderboardList.adapter =
                        MiniLeaderboardAdapter(top5, currentPlayerId)
                    binding.sectionHome.top5Container.visibility = View.VISIBLE
                } else {
                    binding.sectionHome.top5Container.visibility = View.GONE
                }
            }
        } catch (e: Exception) {
            Log.e("TOP5_FIREBASE", "Error updating top 5 UI: ${e.message}")
        }
    }


    private fun startWeeklyPrizeListener() {
        try {
            if (!::database.isInitialized) {
                binding.root.postDelayed({ startWeeklyPrizeListener() }, 1000)
                return
            }
            // Clear previous listener if it exists
            weeklyPrizeListener?.let { listener ->
                weeklyPrizeRef?.removeEventListener(listener)
            }
            // YA NO ACTUALIZAMOS tvWeeklyPrize dinámicamente
            // El texto estático "Premios de esta semana" está en el XML
        } catch (e: Exception) {
            Log.e("WEEKLY_PRIZE", "Error: ${e.message}")
        }
    }

    private fun setupSorteoControlListener() {
        commManager.setupSorteoListener(
            onCountdownUpdate = { status, message ->
                runOnUiThread {
                    when (status) {
                        "procesando" -> updateSorteoState(SorteoState.PROCESSING)
                        "pausado" -> showPausedState(message)
                        else -> updateSorteoState(SorteoState.NORMAL)
                    }
                }
            },
            onRevealCountdown = { remaining, winnerId, prize ->
                runOnUiThread {
                    updateSorteoState(SorteoState.REVEAL, remaining, winnerId, prize)
                }
            },
            onWinnerRevealed = { winnerId, prize ->
                runOnUiThread {
                    updateSorteoState(SorteoState.REVEALED, winnerId = winnerId, prize = prize)
                }
            },
            onReset = {
                runOnUiThread {
                    updateSorteoState(SorteoState.NORMAL)
                }
            }
        )
    }

    private enum class SorteoState { NORMAL, PROCESSING, REVEAL, REVEALED }

    private fun updateSorteoState(state: SorteoState, remaining: Long = 0, winnerId: String = "", prize: String = "") {
        currentSorteoState = state.name.lowercase(Locale.getDefault())
        when(state) {
            SorteoState.NORMAL -> showNormalState()
            SorteoState.PROCESSING -> showProcessingState()
            SorteoState.REVEAL -> showRevealState(remaining, winnerId, prize)
            SorteoState.REVEALED -> showWinnerRevealedState(winnerId, prize)
        }
    }

    private fun showNormalState() {
        binding.sectionHome.apply {
            processingStateContainer.visibility = View.GONE
            revealStateContainer.visibility = View.GONE
            winnerRevealedContainer.visibility = View.GONE
            normalStateContainer.visibility = View.VISIBLE
            sorteoStatusBadge.apply {
                text = "ACTIVO"
                setTextColor(Color.WHITE)
                setBackgroundResource(R.drawable.background_badge_active_pulse)
                // ✅ ANIMACIÓN SUAVE DE APARECER/DESAPARECER (COMO LIVE)
                val fadeAnimation = android.view.animation.AlphaAnimation(1.0f, 0.3f).apply {
                    duration = 1000
                    repeatMode = android.view.animation.Animation.REVERSE
                    repeatCount = android.view.animation.Animation.INFINITE
                }
                startAnimation(fadeAnimation)
            }
        }
        if (countdownTimer == null) {
            startWeeklyCountdown()
        }
    }

    private fun showProcessingState() {
        binding.sectionHome.apply {
            // Ocultar contenedores viejos
            normalStateContainer.visibility = View.GONE
            processingStateContainer.visibility = View.GONE
            revealStateContainer.visibility = View.GONE
            winnerRevealedContainer.visibility = View.GONE
            // ✅ Solo actualizar el badge visible
            sorteoStatusBadge.apply {
                visibility = View.VISIBLE
                text = "PROCESANDO"
                setTextColor(Color.parseColor("#f59e0b"))
                setBackgroundColor(Color.parseColor("#1A2A35"))
            }
        }
    }

    private fun showPausedState(message: String) {
        currentSorteoState = "paused"
        binding.sectionHome.apply {
            normalStateContainer.visibility = View.VISIBLE
            processingStateContainer.visibility = View.GONE
            revealStateContainer.visibility = View.GONE
            winnerRevealedContainer.visibility = View.GONE
            sorteoStatusBadge.visibility = View.VISIBLE
            sorteoStatusBadge.text = "PAUSADO"
            sorteoStatusBadge.setTextColor(Color.parseColor("#f59e0b"))
            countdown.text = "PAUSADO"
            countdown.setTextColor(Color.parseColor("#f59e0b"))
            countdownLabel.text = message.ifEmpty { "Sorteo temporalmente pausado" }
        }
    }

    private fun showRevealState(remaining: Long, winnerId: String, prize: String) {
        binding.sectionHome.apply {
            // Ocultar contenedores viejos
            normalStateContainer.visibility = View.GONE
            processingStateContainer.visibility = View.GONE
            winnerRevealedContainer.visibility = View.GONE
            // ✅ Solo actualizar el badge y countdown visible
            sorteoStatusBadge.apply {
                visibility = View.VISIBLE
                text = "LIVE!"
                setTextColor(Color.parseColor("#e74c3c"))
                setBackgroundColor(Color.parseColor("#1A2A35"))
            }
            // Actualizar el countdown que SÍ está visible en el nuevo diseño
            val seconds = (remaining / 1000).toInt()
            countdown.text = String.format("%02d:%02d", seconds / 60, seconds % 60)
            val countdownColor = when {
                seconds > 60 -> Color.parseColor("#f59e0b")
                seconds > 30 -> Color.parseColor("#FFD700")
                seconds > 10 -> Color.parseColor("#ff9800")
                else -> Color.parseColor("#e74c3c")
            }
            countdown.setTextColor(countdownColor)
        }
    }

    private fun showWinnerRevealedState(winnerId: String, prize: String) {
        binding.sectionHome.apply {
            normalStateContainer.visibility = View.GONE
            processingStateContainer.visibility = View.GONE
            revealStateContainer.visibility = View.GONE
            sorteoStatusBadge.apply {
                visibility = View.VISIBLE
                text = "COMPLETADO"
                setTextColor(Color.parseColor("#2ecc71"))
                setBackgroundColor(Color.parseColor("#1A2A35"))
            }
            countdown.text = "Ganador: $winnerId"
            countdown.setTextColor(Color.parseColor("#FFD700"))
            if (currentPlayerId == winnerId) {
                vibratePattern(longArrayOf(0, 500, 100, 500, 100, 1000))
                spinButton.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FFD700"))
                spinButton.text = "🏆 ¡ERES EL GANADOR! 🏆"
                Handler(Looper.getMainLooper()).postDelayed({
                    spinButton.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#00a8ff"))
                    spinButton.text = "GIRAR RULETA"
                }, 30000)
            }
        }
        Handler(Looper.getMainLooper()).postDelayed({
            showNormalState()
        }, 120000)
    }

    private fun showMiniNotification(message: String) {
        // Simple Toast without custom background
        Toast.makeText(this, message, Toast.LENGTH_SHORT).apply {
            setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, 200)
            show()
        }
    }

    private fun vibratePattern(pattern: LongArray) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(VIBRATOR_SERVICE) as Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
        } catch (e: Exception) {
            Log.d("VIBRATION", "Could not vibrate: ${e.message}")
        }
    }

    private fun setupEmergencyStopListener() {
        database.child("appConfig").child("emergencyStop").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val active = snapshot.child("active").getValue(Boolean::class.java) ?: false
                val reason = snapshot.child("reason").getValue(String::class.java) ?: "System maintenance"

                if (active) {
                    runOnUiThread {
                        showEmergencyStopScreen(reason)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("EMERGENCY", "Error: ${error.message}")
            }
        })
    }

    private fun showEmergencyStopScreen(reason: String) {
        MaterialDialog(this).show {
            title(text = "⚠ System in Maintenance")
            message(text = reason)
            positiveButton(text = "UNDERSTOOD") {
                finishAffinity()
            }
            cancelable(false)
        }
    }

    private fun startWeeklyCountdown() {
        countdownTimer?.cancel()
        database.child("appConfig").child("weeklyPrize").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val drawDay = snapshot.child("day").getValue(Int::class.java) ?: 0
                val drawTime = snapshot.child("time").getValue(String::class.java) ?: "18:00"
                val timeParts = drawTime.split(":")
                val hour = timeParts.getOrNull(0)?.toIntOrNull() ?: 18
                val minute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0
                val calendar = Calendar.getInstance()
                val now = calendar.timeInMillis
                calendar.set(Calendar.DAY_OF_WEEK, drawDay + 1)
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                if (calendar.timeInMillis < now) {
                    calendar.add(Calendar.WEEK_OF_YEAR, 1)
                }
                val diff = calendar.timeInMillis - now
                countdownTimer = object : CountDownTimer(diff, 1000) { // ✅ CAMBIADO A 1000ms
                    override fun onTick(millisUntilFinished: Long) {
                        val days = TimeUnit.MILLISECONDS.toDays(millisUntilFinished)
                        val hours = TimeUnit.MILLISECONDS.toHours(millisUntilFinished) % 24
                        val minutes = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished) % 60
                        binding.sectionHome.countdown.text = String.format("%dd %dh %dm", days, hours, minutes)
                    }
                    override fun onFinish() {
                        binding.sectionHome.countdown.text = "¡En curso!"
                        binding.sectionHome.countdown.setTextColor(Color.parseColor("#FFD700"))
                        Handler(Looper.getMainLooper()).postDelayed({
                            startWeeklyCountdown()
                        }, 60000)
                    }
                }.start()
            }
            override fun onCancelled(error: DatabaseError) {
                startDefaultWeeklyCountdown()
            }
        })
    }

    private fun startDefaultWeeklyCountdown() {
        countdownTimer?.cancel()
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 18)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)

        if (calendar.timeInMillis < now) {
            calendar.add(Calendar.WEEK_OF_YEAR, 1)
        }

        val diff = calendar.timeInMillis - now

        countdownTimer = object : CountDownTimer(diff, 60000) {
            override fun onTick(millisUntilFinished: Long) {
                val days = TimeUnit.MILLISECONDS.toDays(millisUntilFinished)
                val hours = TimeUnit.MILLISECONDS.toHours(millisUntilFinished) % 24
                val minutes = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished) % 60
                binding.sectionHome.countdown.text = String.format("%dd %dh %dm", days, hours, minutes)
            }

            override fun onFinish() {
                binding.sectionHome.countdown.text = "Finished!"
                startWeeklyCountdown() // Restart
            }
        }.start()
    }

    private fun fetchAllData() {
        try {
            if (!::database.isInitialized) return
            val winnersRef = database.child("winners")
            val winnersQuery = winnersRef.orderByChild("timestamp").limitToLast(10)
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        val winners = mutableListOf<Winner>()
                        for (winnerSnapshot in snapshot.children) {
                            try {
                                val winnerId = winnerSnapshot.child("winnerId").getValue(String::class.java)
                                    ?: winnerSnapshot.child("playerId").getValue(String::class.java)
                                if (!winnerId.isNullOrEmpty()) {
                                    val prize = winnerSnapshot.child("prize").getValue(String::class.java)
                                    val timestamp = winnerSnapshot.child("timestamp").getValue(Long::class.java)
                                    val week = winnerSnapshot.child("week").getValue(String::class.java)
                                    val date = winnerSnapshot.child("date").getValue(String::class.java)
                                    winners.add(
                                        Winner(
                                            winnerId = winnerId,
                                            prize = prize,
                                            timestamp = timestamp,
                                            week = week,
                                            date = date
                                        )
                                    )
                                }
                            } catch (e: Exception) {
                                Log.e("FETCH_DATA", "Error processing individual winner: ${e.message}")
                            }
                        }
                        val sortedWinners = winners.sortedByDescending { it.timestamp ?: 0 }
                        runOnUiThread {
                            if (::binding.isInitialized) {
                                val winnerItems = sortedWinners.map { winner ->
                                    WinnerItem(
                                        winner.winnerId ?: "",
                                        winner.winnerId ?: "",
                                        winner.prize ?: "Surprise Prize",
                                        winner.timestamp ?: System.currentTimeMillis()
                                    )
                                }
                                // Actualizar datos del adapter existente
                                winnersAdapter?.updateData(winnerItems)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("FETCH_DATA", "Error processing all winners: ${e.message}")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FETCH_DATA", "Error in the winners listener: ${error.message}")
                }
            }
            winnersQuery.addValueEventListener(listener)
            firebaseListeners.add(listener)
            firebaseReferences.add(winnersRef)
        } catch (e: Exception) {
            Log.e("FETCH_DATA", "General error in fetchAllData: ${e.message}")
        }
    }

    private fun fetchPlayerData(playerId: String) {
        try {
            if (!::database.isInitialized) return
            val playerRef = database.child("players").child(playerId)
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val tickets = snapshot.child("tickets").getValue(Long::class.java) ?: 0L
                        val passes = snapshot.child("passes").getValue(Long::class.java) ?: 0L
                        val firebaseSpins = snapshot.child("spins").getValue(Long::class.java)?.toInt() ?: 10

                        val localSpins = SessionManager.getCurrentSpins(this@MainActivity)
                        if (firebaseSpins != localSpins) {
                            Log.d("FETCH_PLAYER", "🚀 Syncing spins: Local=$localSpins, Firebase=$firebaseSpins")

                            val finalSpins = maxOf(firebaseSpins, localSpins)
                            SessionManager.setCurrentSpins(this@MainActivity, finalSpins)
                            currentSpins = finalSpins

                            if (localSpins > firebaseSpins) {
                                updateSpinsInFirebase(finalSpins)
                            }
                        } else {
                            currentSpins = firebaseSpins
                        }

                        updateUI(tickets, passes)
                        updateSpinCountUI()
                        Log.d("FETCH_PLAYER", "✅ Data synced: T:$tickets, P:$passes, S:$currentSpins")
                    } else {
                        val initialSpins = SessionManager.getCurrentSpins(this@MainActivity)
                        val newPlayerData = mapOf(
                            "tickets" to 0,
                            "passes" to 0,
                            "spins" to initialSpins,
                            "lastUpdate" to ServerValue.TIMESTAMP,
                            "registeredAt" to ServerValue.TIMESTAMP,
                            "deviceInfo" to Build.MODEL,
                            "appVersion" to BuildConfig.VERSION_NAME
                        )
                        playerRef.setValue(newPlayerData)
                        currentSpins = initialSpins
                        updateUI(0, 0)
                        updateSpinCountUI()
                        Log.d("FETCH_PLAYER", "✅ New player created with $initialSpins spins")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FETCH_PLAYER", "❌ Error: ${error.message}")
                }
            }
            playerRef.addValueEventListener(listener)
            firebaseListeners.add(listener)
            firebaseReferences.add(playerRef)
        } catch (e: Exception) {
            Log.e("FETCH_PLAYER", "❌ Error getting player data: ${e.message}")
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
                    val cumulativeTickets = (currentPasses * 1000) + currentTickets + amount
                    val totalPasses = cumulativeTickets / 1000
                    val remainingTickets = cumulativeTickets % 1000
                    currentData.child("tickets").value = remainingTickets
                    currentData.child("passes").value = totalPasses
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
            Log.e("ADD_TICKETS", "Error adding tickets: ${e.message}")
        }
    }

    private fun setupToolbarAndDrawer() {
        try {
            setSupportActionBar(binding.toolbar)
            supportActionBar?.setDisplayShowTitleEnabled(false)

            val toggle = ActionBarDrawerToggle(
                this,
                binding.drawerLayout,
                binding.toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
            )
            binding.drawerLayout.addDrawerListener(toggle)
            toggle.syncState()

            setupCustomHamburgerIcon(toggle)
            setupNavigationViewColors()

            binding.navView.setNavigationItemSelectedListener { handleNavigation(it) }
            setupLegalOptionsClickListeners()
        } catch (e: Exception) {
            Log.e("TOOLBAR_SETUP", "General error in setupToolbarAndDrawer: ${e.message}")
        }
    }

    private fun setupCustomHamburgerIcon(toggle: ActionBarDrawerToggle) {
        try {
            val hamburgerDrawable = createHamburgerDrawable()

            toggle.isDrawerIndicatorEnabled = false
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.setHomeAsUpIndicator(hamburgerDrawable)

            binding.toolbar.setNavigationOnClickListener {
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    binding.drawerLayout.openDrawer(GravityCompat.START)
                }
            }

        } catch (e: Exception) {
            Log.e("HAMBURGER_ICON", "Error configuring icon: ${e.message}")
        }
    }

    private fun createHamburgerDrawable(): Drawable {
        val hamburgerDrawable = ContextCompat.getDrawable(this, android.R.drawable.ic_menu_sort_by_size)
            ?: ContextCompat.getDrawable(this, android.R.drawable.ic_dialog_dialer)!!

        DrawableCompat.setTint(hamburgerDrawable, Color.parseColor("#00A8FF"))

        return hamburgerDrawable
    }

    private fun setupNavigationViewColors() {
        try {
            val menuItemColors = ColorStateList(
                arrayOf(
                    intArrayOf(android.R.attr.state_checked),
                    intArrayOf(-android.R.attr.state_checked)
                ),
                intArrayOf(
                    Color.BLACK,
                    Color.parseColor("#E5E7EB")
                )
            )

            val iconColors = ColorStateList(
                arrayOf(
                    intArrayOf(android.R.attr.state_checked),
                    intArrayOf(-android.R.attr.state_checked)
                ),
                intArrayOf(
                    Color.parseColor("#00A8FF"),
                    Color.parseColor("#9CA3AF")
                )
            )

            binding.navView.itemTextColor = menuItemColors
            binding.navView.itemIconTintList = iconColors
            binding.navView.itemBackground = createSelectableBackground()

            Log.d("NAV_COLORS", "✅ NavigationView colors configured")

        } catch (e: Exception) {
            Log.e("NAV_COLORS", "❌ Error configuring colors: ${e.message}")
        }
    }

    private fun createSelectableBackground(): Drawable {
        return try {
            val selectedColor = Color.parseColor("#E3F2FD")
            val normalColor = Color.TRANSPARENT

            val selectedDrawable = GradientDrawable().apply {
                setColor(selectedColor)
                cornerRadius = 12f
            }

            val normalDrawable = GradientDrawable().apply {
                setColor(normalColor)
                cornerRadius = 12f
            }

            val stateListDrawable = StateListDrawable()
            stateListDrawable.addState(intArrayOf(android.R.attr.state_checked), selectedDrawable)
            stateListDrawable.addState(intArrayOf(), normalDrawable)

            stateListDrawable
        } catch (e: Exception) {
            Log.e("SELECTABLE_BG", "Error creating background: ${e.message}")
            ColorDrawable(Color.TRANSPARENT)
        }
    }

    private fun setupLegalOptionsClickListeners() {
        try {
            val drawerLayout = binding.drawerLayout
            val navPrivacy = drawerLayout.findViewById<TextView>(R.id.nav_privacy)
            val navTerms = drawerLayout.findViewById<TextView>(R.id.nav_terms)

            navPrivacy?.setOnClickListener {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
                binding.root.postDelayed({
                    showLegalModal("Privacy Policy", getString(R.string.privacy_policy_content))
                }, 250)
            }

            navTerms?.setOnClickListener {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
                binding.root.postDelayed({
                    showLegalModal("Terms and Conditions", getString(R.string.terms_content))
                }, 250)
            }
        } catch (e: Exception) {
            Log.e("LEGAL_SETUP", "Error configuring legal options: ${e.message}")
        }
    }

    private fun setupRecyclerViews() {
        // Leaderboard
        binding.sectionLeaderboard.leaderboardRecyclerView.layoutManager = LinearLayoutManager(this)
        leaderboardAdapter = LeaderboardAdapter(this, this, mutableListOf(), null)
        binding.sectionLeaderboard.leaderboardRecyclerView.adapter = leaderboardAdapter
        // Winners
        binding.sectionWinners.winnersRecyclerView.layoutManager = LinearLayoutManager(this)
        winnersAdapter = WinnersAdapter(this, mutableListOf())
        binding.sectionWinners.winnersRecyclerView.adapter = winnersAdapter
        // Mini leaderboard
        binding.sectionHome.miniLeaderboardList.layoutManager = LinearLayoutManager(this)
    }


    private fun setupClickListeners() {
        try {
            val playerIdContainer = binding.sectionHome.myProgress.findViewById<LinearLayout>(R.id.player_id_container)
            playerIdContainer?.setOnClickListener { promptForPlayerId() }
            binding.sectionHome.myPlayerId.setOnClickListener { promptForPlayerId() }
        } catch (e: Exception) {
            Log.e("CLICK_SETUP", "Error configuring player ID click: ${e.message}")
            binding.sectionHome.myPlayerId.setOnClickListener { promptForPlayerId() }
        }

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
            binding.drawerLayout.isDrawerOpen(GravityCompat.START) -> {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
            }
            !isInHomeSection() -> {
                goToHomeSection()
            }
            else -> {
                showExitConfirmationDialog()
            }
        }
    }

    private fun isInHomeSection(): Boolean = binding.sectionHome.root.visibility == View.VISIBLE

    private fun goToHomeSection() {
        try {
            hideAllSections()
            showSectionSafely(binding.sectionHome.root)
            clearAllMenuSelections()
            binding.navView.setCheckedItem(R.id.nav_home)
            Log.d("NAVIGATION", "Navigating to home")
        } catch (e: Exception) {
            Log.e("NAVIGATION", "Error going to home: ${e.message}")
        }
    }

    private fun showExitConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Exit application")
            .setMessage("Are you sure you want to close the application?")
            .setCancelable(true)
            .setPositiveButton("Exit") { _, _ ->
                marcarSesionInactiva()
                finishAffinity()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun handleNavigation(menuItem: MenuItem): Boolean {
        return try {
            // Cerrar drawer primero
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            // Esperar a que el drawer se cierre antes de navegar
            binding.drawerLayout.postDelayed({
                clearAllMenuSelections()
                menuItem.isChecked = true
                when (menuItem.itemId) {
                    R.id.nav_home -> {
                        showSectionSafely(binding.sectionHome.root)
                    }
                    R.id.nav_tasks -> {
                        showSectionWithAd(binding.sectionTasks.root)
                    }
                    R.id.nav_leaderboard -> {
                        showSectionWithAd(binding.sectionLeaderboard.root)
                    }
                    R.id.nav_winners -> {
                        showSectionWithAd(binding.sectionWinners.root)
                    }
                    else -> {
                        showSectionSafely(binding.sectionHome.root)
                    }
                }
            }, 250) // Delay para cerrar drawer
            true
        } catch (e: Exception) {
            Log.e("NAVIGATION", "Critical error: ${e.message}")
            showSectionSafely(binding.sectionHome.root)
            false
        }
    }


    private fun showSectionSafely(section: View) {
        runOnUiThread {
            hideAllSections()
            section.visibility = View.VISIBLE
        }
    }

    private fun showSectionWithAd(section: View) {
        if (AdManager.isInterstitialReady()) {
            // Mostrar la sección ANTES del intersticial para evitar el parpadeo
            showSectionSafely(section)
            // Luego mostrar el intersticial
            binding.root.postDelayed({
                AdManager.showInterstitial(this) {
                    // No hacer nada en onDismiss, ya mostramos la sección antes
                    Log.d(TAG_MAIN, "Intersticial cerrado")
                }
            }, 100)
        } else {
            showSectionSafely(section)
        }
    }


    private fun clearAllMenuSelections() {
        try {
            val menu = binding.navView.menu
            for (i in 0 until menu.size()) {
                val item = menu.getItem(i)
                if (item.groupId == R.id.group_main) {
                    item.isChecked = false
                }
            }
        } catch (e: Exception) {
            Log.e("NAVIGATION", "Error clearing selections: ${e.message}")
        }
    }

    private fun hideAllSections() {
        binding.sectionHome.root.visibility = View.GONE
        binding.sectionTasks.root.visibility = View.GONE
        binding.sectionLeaderboard.root.visibility = View.GONE
        binding.sectionWinners.root.visibility = View.GONE
        binding.sectionLegal.root.visibility = View.GONE
    }

    private fun showLegalModal(title: String, content: String) {
        try {
            val dialog = android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
            val dialogView = layoutInflater.inflate(R.layout.dialog_legal, null)
            dialog.setContentView(dialogView)

            dialogView.findViewById<TextView>(R.id.dialog_title).text = title
            val contentView = dialogView.findViewById<TextView>(R.id.dialog_content)

            contentView.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                android.text.Html.fromHtml(content, android.text.Html.FROM_HTML_MODE_COMPACT)
            } else {
                @Suppress("DEPRECATION")
                android.text.Html.fromHtml(content)
            }

            dialogView.findViewById<View>(R.id.btn_close).setOnClickListener { dialog.dismiss() }
            dialogView.findViewById<View>(R.id.btn_understood).setOnClickListener { dialog.dismiss() }

            dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            dialog.setCancelable(true)
            dialog.show()
        } catch (e: Exception) {
            Log.e("LEGAL_MODAL", "Error showing modal: ${e.message}")
        }
    }

    private fun requestSpinsByWatchingAd() {
        if (currentPlayerId == null) {
            promptForPlayerId()
            return
        }
        if (!isNetworkAvailable()) {
            AdFallbackStrategy.showNoInternetModal(this)
            return
        }
        val audioFocusGranted = requestAudioFocus()
        if (!audioFocusGranted) {
            Log.w(TAG_ADMOB, "Audio focus not granted, continuing without audio")
        }
        // ✅ Intentar mostrar rewarded con fallback
        if (AdManager.isRewardedReady()) {
            AdManager.showRewarded(this,
                onReward = { rewardItem ->
                    releaseAudioFocus()
                    Log.d(TAG_ADMOB, "Reward of ${rewardItem.amount} ${rewardItem.type} obtained.")
                    val spinsToAdd = 10
                    SessionManager.addSpins(this, spinsToAdd)
                    currentSpins = SessionManager.getCurrentSpins(this)
                    updateSpinCountUI()
                },
                onDismiss = {
                    releaseAudioFocus()
                }
            )
        } else {
            // ✅ No hay rewarded disponible, activar fallback
            releaseAudioFocus()
            AdFallbackStrategy.handleRewardedAdUnavailable(
                activity = this,
                rewardType = "spins",
                rewardAmount = 10,
                onSuccess = {
                    // ✅ Dar solo 3 giros (recompensa reducida)
                    SessionManager.addSpins(this, 3)
                    currentSpins = SessionManager.getCurrentSpins(this)
                    updateSpinCountUI()
                })
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }

    private fun requestTicketsByWatchingAd() {
        if (currentPlayerId == null) {
            promptForPlayerId()
            return
        }
        if (!isNetworkAvailable()) {
            AdFallbackStrategy.showNoInternetModal(this)
            return
        }
        val audioFocusGranted = requestAudioFocus()
        if (!audioFocusGranted) {
            Log.w(TAG_ADMOB, "Audio focus not granted, continuing without audio")
        }
        // ✅ Intentar mostrar rewarded con fallback
        if (AdManager.isRewardedReady()) {
            AdManager.showRewarded(this,
                onReward = { rewardItem ->
                    releaseAudioFocus()
                    Log.d(TAG_ADMOB, "Reward of ${rewardItem.amount} ${rewardItem.type} obtained.")
                    val ticketsToAdd = if (rewardItem.amount > 0) rewardItem.amount else 20
                    addTicketsToPlayer(ticketsToAdd)
                },
                onDismiss = {
                    releaseAudioFocus()
                }
            )
        } else {
            // ✅ No hay rewarded disponible, activar fallback
            releaseAudioFocus()
            AdFallbackStrategy.handleRewardedAdUnavailable(
                activity = this,
                rewardType = "tickets",
                rewardAmount = 20,
                onSuccess = {
                    // ✅ Dar solo 10 tickets (recompensa reducida)
                    addTicketsToPlayer(10)
                })
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
                0 -> "NOTHING"
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
                message(text = "Necesitas más giros para jugar. ¿Quieres ver un video para obtener más giros?")
                positiveButton(text = "Ver Video") { requestSpinsByWatchingAd() }
                negativeButton(text = "Ahora no")
            }
            return
        }

        isSpinning = true
        if (SessionManager.useSpin(this)) {
            currentSpins = SessionManager.getCurrentSpins(this)
            updateSpinCountUI()

            updateSpinsInFirebase(currentSpins)
        } else {
            isSpinning = false
            return
        }

        binding.sectionHome.spinButton.isEnabled = false
        binding.sectionHome.spinButton.text = "GIRANDO..."
        binding.sectionHome.spinButton.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#424242"))

        object : CountDownTimer(3000, 80) {
            val gridItems = binding.sectionHome.gridContainer.children.toList()
            override fun onTick(millisUntilFinished: Long) {
                gridItems.forEach { item ->
                    item.setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.border_color))
                    item.findViewById<TextView>(R.id.grid_item_value).setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_color))
                    item.findViewById<TextView>(R.id.grid_item_label).setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_muted))
                }
                val currentIndex = (0 until gridItems.size).random()
                gridItems[currentIndex].setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.pass_color))
                gridItems[currentIndex].findViewById<TextView>(R.id.grid_item_value).setTextColor(Color.WHITE)
                gridItems[currentIndex].findViewById<TextView>(R.id.grid_item_label).setTextColor(Color.WHITE)
            }

            override fun onFinish() {
                gridItems.forEach { item ->
                    item.setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.border_color))
                    item.findViewById<TextView>(R.id.grid_item_value).setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_color))
                    item.findViewById<TextView>(R.id.grid_item_label).setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_muted))
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

    private fun updateSpinsInFirebase(newSpinCount: Int) {
        try {
            if (currentPlayerId != null && ::database.isInitialized) {
                database.child("players").child(currentPlayerId!!).child("spins").setValue(newSpinCount)
            }
        } catch (e: Exception) {
            Log.e("UPDATE_SPINS", "Error updating spins in Firebase: ${e.message}")
        }
    }

    private fun resetSpinButton() {
        isSpinning = false
        binding.sectionHome.spinButton.isEnabled = true
        binding.sectionHome.spinButton.text = "GIRAR RULETA"
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
            Log.e("SPIN_UI", "Error updating spin UI: ${e.message}")
        }
    }

    private fun updatePlayerIdUI(playerId: String?) {
        try {
            if (playerId != null && playerId.isNotEmpty()) {
                binding.sectionHome.myPlayerId.text = playerId
                binding.sectionHome.myPlayerId.setTextColor(Color.parseColor("#00A8FF"))
            } else {
                binding.sectionHome.myPlayerId.text = "Tap to configure"
                binding.sectionHome.myPlayerId.setTextColor(Color.parseColor("#CCCCCC"))
            }

            try {
                val editIcon = binding.sectionHome.myProgress.findViewById<ImageView>(R.id.edit_icon)
                editIcon?.visibility = View.VISIBLE
                editIcon?.setColorFilter(
                    if (playerId != null && playerId.isNotEmpty()) Color.parseColor("#00A8FF")
                    else Color.parseColor("#CCCCCC")
                )
            } catch (e: Exception) {
                Log.w("PLAYER_ID_UI", "Edit icon not found: ${e.message}")
            }
        } catch (e: Exception) {
            Log.e("PLAYER_ID_UI", "Error updating player ID UI: ${e.message}")
        }
    }

    private fun updateUI(tickets: Long, passes: Long) {
        try {
            binding.sectionHome.myTickets.text = tickets.toString()
            binding.sectionHome.myPasses.text = passes.toString()
            binding.sectionHome.passProgress.progress = ((tickets % 1000).toDouble() / 1000.0 * 100).toInt()
            binding.sectionHome.myTickets.setTextColor(Color.parseColor("#FFD700"))
            binding.sectionHome.myPasses.setTextColor(Color.parseColor("#F97316"))
        } catch (e: Exception) {
            Log.e("UPDATE_UI", "Error updating UI: ${e.message}")
        }
    }

    private fun promptForPlayerId() {
        MaterialDialog(this).show {
            title(text = if (currentPlayerId != null) "Editar ID de Jugador" else "Configura tu ID")
            input(
                hint = "Ingresa tu ID numérico",
                prefill = currentPlayerId ?: "",
                inputType = InputType.TYPE_CLASS_NUMBER
            ) { _, text ->
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
                                updatePlayerIdUI(newPlayerId)
                                loadUserData(newPlayerId)
                            }
                            .setNegativeButton("Cancelar", null)
                            .show()
                    } else {
                        SessionManager.setPlayerId(this@MainActivity, newPlayerId)
                        currentPlayerId = newPlayerId
                        updatePlayerIdUI(newPlayerId)
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
            Log.d(TAG_MAIN, "📊 Loading data for user: $playerId")

            checkUserBanStatus(playerId)
            createFirebaseSession(playerId)
            fetchPlayerData(playerId)
            obtenerTop5Firebase()
            updateUI(0, 0)
            checkForPrivateMessages(playerId)
            setupFCMToken(playerId)

            // 🛑 IMPORTANT: Start notification listener
            AppNotificationManager.getInstance(this).startListening(playerId)

            // Start CommunicationManager
            commManager.startNotificationListener(playerId)
            commManager.setupDataSyncListener(playerId) { tickets, passes, spins ->
                runOnUiThread {
                    updateUI(tickets, passes)
                    currentSpins = spins.toInt()
                    updateSpinCountUI()
                }
            }

            Log.d(TAG_MAIN, "✅ User data loaded completely")
        } catch (e: Exception) {
            Log.e(TAG_MAIN, "Error loading user data: ${e.message}")
        }
    }

    private fun signInAnonymously() {
        auth.signInAnonymously().addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                Log.d("Firebase Auth", "✅ Anonymous authentication successful")
                val user = auth.currentUser
                Log.d("Firebase Auth", "User: ${user?.uid}")
                Handler(Looper.getMainLooper()).postDelayed({
                    obtenerTop5Firebase()
                    fetchAllData()
                    startWeeklyPrizeListener()
                }, 1000)
            } else {
                Log.w("Firebase Auth", "❌ signInAnonymously:failure", task.exception)
            }
        }
    }

    // ===================================================================
    // AUDIO FOCUS MANAGEMENT
    // ===================================================================

    private fun setupAudioManager() {
        try {
            audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                    .build()
                audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(audioAttributes)
                    .setAcceptsDelayedFocusGain(true)
                    .setWillPauseWhenDucked(false)
                    .setOnAudioFocusChangeListener({ focusChange ->
                        handleAudioFocusChange(focusChange)
                    }, Handler(mainLooper))
                    .build()
            }
            Log.d("AUDIO_SETUP", "AudioManager configured correctly")
        } catch (e: Exception) {
            Log.e("AUDIO_SETUP", "Error configuring audio manager: ${e.message}")
            audioManager = null
            audioFocusRequest = null
        }
    }

    private fun handleAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                hasAudioFocus = true
                Log.d("AUDIO_FOCUS", "Audio focus gained")
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                hasAudioFocus = false
                Log.d("AUDIO_FOCUS", "Audio focus permanently lost")
                releaseAudioFocus()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                hasAudioFocus = false
                Log.d("AUDIO_FOCUS", "Audio focus temporarily lost")
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                Log.d("AUDIO_FOCUS", "Audio focus - can duck volume")
            }
        }
    }

    private fun requestAudioFocus(): Boolean {
        return try {
            audioManager?.let { am ->
                if (hasAudioFocus) {
                    Log.d("AUDIO_FOCUS", "Already has audio focus")
                    return true
                }

                val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    audioFocusRequest?.let { request ->
                        am.requestAudioFocus(request)
                    } ?: AudioManager.AUDIOFOCUS_REQUEST_FAILED
                } else {
                    @Suppress("DEPRECATION")
                    am.requestAudioFocus(
                        { focusChange -> handleAudioFocusChange(focusChange) },
                        AudioManager.STREAM_MUSIC,
                        AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
                    )
                }

                when (result) {
                    AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> {
                        hasAudioFocus = true
                        Log.d("AUDIO_FOCUS", "Audio focus granted")
                        true
                    }
                    AudioManager.AUDIOFOCUS_REQUEST_FAILED -> {
                        hasAudioFocus = false
                        Log.w("AUDIO_FOCUS", "Audio focus denied")
                        false
                    }
                    AudioManager.AUDIOFOCUS_REQUEST_DELAYED -> {
                        hasAudioFocus = false
                        Log.d("AUDIO_FOCUS", "Audio focus delayed")
                        false
                    }
                    else -> {
                        hasAudioFocus = false
                        false
                    }
                }
            } ?: run {
                Log.w("AUDIO_FOCUS", "AudioManager not available")
                false
            }
        } catch (e: Exception) {
            Log.e("AUDIO_FOCUS", "Error requesting audio focus: ${e.message}")
            hasAudioFocus = false
            false
        }
    }

    private fun releaseAudioFocus() {
        try {
            if (!hasAudioFocus) {
                Log.d("AUDIO_FOCUS", "No audio focus to release")
                return
            }

            audioManager?.let { am ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    audioFocusRequest?.let { request ->
                        am.abandonAudioFocusRequest(request)
                    }
                } else {
                    @Suppress("DEPRECATION")
                    am.abandonAudioFocus {
                        Log.d("AUDIO_FOCUS", "Audio focus released (legacy)")
                    }
                }

                hasAudioFocus = false
                Log.d("AUDIO_FOCUS", "Audio focus released")
            }
        } catch (e: Exception) {
            Log.e("AUDIO_FOCUS", "Error releasing audio focus: ${e.message}")
        }
    }

    private fun marcarSesionInactiva() {
        try {
            val playerId = SessionManager.getPlayerId(this)
            val sessionId = SessionManager.getSessionId(this)
            if (playerId.isNotEmpty() && sessionId.isNotEmpty() && ::database.isInitialized) {
                database.child("sessions").child(playerId).child(sessionId).child("active").setValue(false)
            }
        } catch (e: Exception) {
            Log.e("SESSION_INACTIVE", "Error in marcarSesionInactiva: ${e.message}")
        }
    }

    private fun showWinnerModal(message: String) {
        try {
            MaterialDialog(this).show {
                title(text = "🎉 CONGRATULATIONS, WINNER!")
                message(text = message)
                positiveButton(text = "AWESOME!") {
                    showCelebrationEffects()
                }
                negativeButton(text = "View Winners") {
                    // Redirect to winners section
                    hideAllSections()
                    showSectionSafely(binding.sectionWinners.root)
                    binding.navView.setCheckedItem(R.id.nav_winners)
                }
                cancelable(false)
            }
            vibratePattern(longArrayOf(0, 500, 100, 500, 100, 500))
        } catch (e: Exception) {
            Log.e("WINNER_MODAL", "Error: ${e.message}")
        }
    }

    private fun showLoserModal(message: String) {
        try {
            MaterialDialog(this).show {
                title(text = "📣 Draw Finished")
                message(text = message)
                positiveButton(text = "View Winners") {
                    // Redirect to winners section
                    hideAllSections()
                    showSectionSafely(binding.sectionWinners.root)
                    binding.navView.setCheckedItem(R.id.nav_winners)
                }
                negativeButton(text = "Close")
                cancelable(true)
            }
        } catch (e: Exception) {
            Log.e("LOSER_MODAL", "Error: ${e.message}")
        }
    }

    private fun showCelebrationEffects() {
        binding.sectionHome.spinButton.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FFD700"))
        binding.sectionHome.spinButton.text = "¡ERES EL GANADOR!"
        binding.sectionHome.spinButton.postDelayed({
            binding.sectionHome.spinButton.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#00a8ff"))
            binding.sectionHome.spinButton.text = "GIRAR RULETA"
        }, 5000)
    }

    private fun redirectToWinnersSection() {
        hideAllSections()
        showSectionSafely(binding.sectionWinners.root)
        binding.navView.setCheckedItem(R.id.nav_winners)
    }

    private fun checkNotificationPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
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
            if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                MaterialDialog(this).show {
                    title(text = "Activate Notifications")
                    message(text = "Activate notifications to know if you win the weekly draw and receive special prizes.")
                    positiveButton(text = "Activate") {
                        ActivityCompat.requestPermissions(
                            this@MainActivity,
                            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                            NOTIFICATION_PERMISSION_REQUEST_CODE
                        )
                    }
                    negativeButton(text = "Not now") { createNotificationChannels() }
                }
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
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

            val winnerChannel =
                NotificationChannel("winner_notifications", "Winner Notifications", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Notifications when you win the draw"
                    enableLights(true)
                    lightColor = Color.parseColor("#FFD700")
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 1000, 500, 1000)
                }

            val generalChannel =
                NotificationChannel("general_notifications", "General Notifications", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "General notifications from the rewards club"
                }

            notificationManager.createNotificationChannel(winnerChannel)
            notificationManager.createNotificationChannel(generalChannel)
        }
    }

    private fun checkUserBanStatus(playerId: String) {
        if (playerId.isEmpty()) return

        BanChecker(this).checkBanStatus(playerId) { banStatus ->
            when (banStatus) {
                is BanChecker.BanStatus.NotBanned -> {}
                is BanChecker.BanStatus.TemporaryBan -> showBanScreen(
                    "temporary",
                    banStatus.reason,
                    banStatus.expiresAt,
                    playerId
                )
                is BanChecker.BanStatus.PermanentBan -> showBanScreen("permanent", banStatus.reason, 0L, playerId)
                else -> Log.w(TAG_MAIN, "Unknown ban status: $banStatus")
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

    @Suppress("UNCHECKED_CAST")
    private fun checkForPrivateMessages(playerId: String) {
        try {
            if (!::database.isInitialized || playerId.isEmpty()) return

            val messageRef = database.child("privateMessages").child(playerId)
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val messageData = snapshot.value as? Map<String, Any>
                        if (messageData != null) {
                            val type = messageData["type"] as? String
                            val message = messageData["message"] as? String
                            when (type) {
                                "win", "loss" -> { /* Ignored, handled by notificationQueue */
                                }
                                else -> showGeneralModal("Notification", message ?: "You have a new message.")
                            }
                            messageRef.removeValue()
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("PRIVATE_MESSAGES", "Error: ${error.message}")
                }
            }

            messageRef.addValueEventListener(listener)
            firebaseListeners.add(listener)
            firebaseReferences.add(messageRef)
        } catch (e: Exception) {
            Log.e("PRIVATE_MESSAGES", "Error checking for private messages: ${e.message}")
        }
    }

    private fun showGeneralModal(title: String, message: String) {
        try {
            MaterialDialog(this).show {
                title(text = title)
                message(text = message)
                positiveButton(text = "OK")
                cancelable(true)
            }
        } catch (e: Exception) {
            Log.e("GENERAL_MODAL", "Error: ${e.message}")
        }
    }

    private fun setupNotificationListener() {
        val userId = auth.currentUser?.uid ?: return
        database
            .child("notificationQueue")
            .child(userId)
            .addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    handleIncomingNotification(snapshot)
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onChildRemoved(snapshot: DataSnapshot) {}
                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun handleIncomingNotification(snapshot: DataSnapshot) {
        val title = snapshot.child("title").getValue(String::class.java) ?: return
        val message = snapshot.child("message").getValue(String::class.java) ?: ""
        val type = snapshot.child("type").getValue(String::class.java) ?: "general"
        val processed = snapshot.child("processed").getValue(Boolean::class.java) ?: false
        if (processed) return // Ya fue procesada
        // Marcar como procesada
        snapshot.ref.child("processed").setValue(true)
        // Mostrar notificación según el tipo
        when (type) {
            "gift" -> {
                val giftType = snapshot.child("data").child("giftType").getValue(String::class.java)
                val amount = snapshot.child("data").child("amount").getValue(Int::class.java) ?: 0
                showGiftNotification(title, message, giftType, amount)
            }
            "warning" -> showWarningDialog(title, message)
            "ban" -> showBanDialog(message)
            else -> showGeneralNotification(title, message)
        }
    }

    private fun showGiftNotification(title: String, message: String, giftType: String?, amount: Int) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("🎁 $title")
            .setMessage(message)
            .setPositiveButton("¡Genial!") { dialog, _ ->
                dialog.dismiss()
                // Recargar datos del usuario para ver el regalo
                currentPlayerId?.let { loadUserData(it) }
            }
            .show()
    }

    private fun showWarningDialog(title: String, message: String) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("⚠️ $title")
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("Entendido") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showBanDialog(message: String) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("🚫 Cuenta Suspendida")
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("Cerrar Sesión") { _, _ ->
                auth.signOut()
                finish()
            }
            .show()
    }

    private fun showGeneralNotification(title: String, message: String) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showLastNotificationOnOpen() {
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        database
            .child("appConfig")
            .child("lastNotification")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) return
                    val title = snapshot.child("title").getValue(String::class.java) ?: return
                    val message = snapshot.child("message").getValue(String::class.java) ?: ""
                    val timestamp = snapshot.child("timestamp").getValue(Long::class.java) ?: 0L
                    // Solo mostrar si es nueva
                    val lastShown = prefs.getLong("last_notification_shown", 0L)
                    if (timestamp > lastShown) {
                        showLastNotificationDialog(title, message)
                        prefs.edit().putLong("last_notification_shown", timestamp).apply()
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun showLastNotificationDialog(title: String, message: String) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("📢 $title")
            .setMessage(message)
            .setPositiveButton("Entendido") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun loadWeeklyPrizes() {
        database
            .child("appConfig")
            .child("prizes")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val firstPrize = snapshot.child("first").getValue(String::class.java) ?: "1,000 💎"
                    val secondPrize = snapshot.child("second").getValue(String::class.java) ?: "500 💎"
                    val thirdPrize = snapshot.child("third").getValue(String::class.java) ?: "200 💎"
                    Log.d("WEEKLY_PRIZES", "✅ Prizes loaded from admin: 1st=$firstPrize, 2nd=$secondPrize, 3rd=$thirdPrize")
                    // Los premios están disponibles en Firebase, tu app puede usarlos cuando sea necesario
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e("WEEKLY_PRIZES", "Error loading prizes: ${error.message}")
                }
            })
    }
    private fun loadSorteoButtonConfig() {
        database
            .child("appConfig")
            .child("button")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val buttonText = snapshot.child("text").getValue(String::class.java) ?: "SORTEO EN CURSO"
                    val subtext = snapshot.child("subtext").getValue(String::class.java) ?: "Gira y gana"
                    val enabled = snapshot.child("enabled").getValue(Boolean::class.java) ?: true
                    runOnUiThread {
                        try {
                            // Solo actualizar si el botón no está en medio de una acción
                            val currentText = binding.sectionHome.spinButton.text.toString()
                            if (!currentText.contains("GIRANDO") && !currentText.contains("GANADOR")) {
                                binding.sectionHome.spinButton.text = buttonText
                            }
                            // Habilitar/deshabilitar según configuración del admin Y si tiene giros
                            binding.sectionHome.spinButton.isEnabled = enabled && currentSpins > 0 && !isSpinning
                            Log.d("SORTEO_CONFIG", "✅ Button configured: text=$buttonText, enabled=$enabled")
                        } catch (e: Exception) {
                            Log.e("SORTEO_CONFIG", "Error updating button: ${e.message}")
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e("SORTEO_CONFIG", "Error loading config: ${error.message}")
                }
            })
    }

    private fun checkBanStatus(onNotBanned: () -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        database
            .child("bannedUsers")
            .child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val type = snapshot.child("type").getValue(String::class.java)
                        val reason = snapshot.child("reason").getValue(String::class.java) ?: "Violación de términos"
                        val expiresAt = snapshot.child("expiresAt").getValue(Long::class.java)
                        // Si es temporal y ya expiró, eliminar el baneo
                        if (type == "temporary" && expiresAt != null && System.currentTimeMillis() > expiresAt) {
                            snapshot.ref.removeValue()
                            onNotBanned()
                            return
                        }
                        // Mostrar diálogo de baneo
                        val message = if (type == "permanent") {
                            "Tu cuenta ha sido baneada permanentemente.\n\nRazón: $reason"
                        } else {
                            val date = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
                                .format(java.util.Date(expiresAt ?: 0L))
                            "Tu cuenta está suspendida.\n\nRazón: $reason\n\nExpira: $date"
                        }
                        androidx.appcompat.app.AlertDialog.Builder(this@MainActivity)
                            .setTitle("🚫 Cuenta Suspendida")
                            .setMessage(message)
                            .setCancelable(false)
                            .setPositiveButton("Cerrar Sesión") { _, _ ->
                                auth.signOut()
                                finish()
                            }
                            .show()
                    } else {
                        onNotBanned()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    onNotBanned()
                }
            })
    }
}