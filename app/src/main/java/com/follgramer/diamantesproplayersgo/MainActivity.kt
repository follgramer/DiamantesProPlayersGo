package com.follgramer.diamantesproplayersgo

import android.Manifest
import android.app.Activity
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
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
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
import com.follgramer.diamantesproplayersgo.ads.NativeAdHelper
import com.follgramer.diamantesproplayersgo.databinding.ActivityMainBinding
import com.follgramer.diamantesproplayersgo.notifications.*
import com.follgramer.diamantesproplayersgo.ui.NotificationCenterActivity
import com.follgramer.diamantesproplayersgo.ui.Onboarding
import com.follgramer.diamantesproplayersgo.util.AnalyticsManager
import com.follgramer.diamantesproplayersgo.util.DeepLinkHandler
import com.follgramer.diamantesproplayersgo.util.ReferralManager
import com.follgramer.diamantesproplayersgo.util.ShareManager
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.ump.UserMessagingPlatform
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import me.leolin.shortcutbadger.ShortcutBadger
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
    private var bannersLoaded = false

    // FIREBASE LISTENERS
    private val firebaseListeners = mutableListOf<ValueEventListener>()
    private val firebaseReferences = mutableListOf<DatabaseReference>()
    private var weeklyPrizeRef: DatabaseReference? = null
    private var weeklyPrizeListener: ValueEventListener? = null

    // ✅ AGREGAR ESTAS NUEVAS PROPIEDADES
    private var playerDataListener: ValueEventListener? = null
    private var playerDataRef: DatabaseReference? = null

    // TIMERS
    private var countdownTimer: CountDownTimer? = null
    private var revealCountdownTimer: CountDownTimer? = null
    private var currentSorteoState = "normal"

    // GRID DATA
    private val gridItemsData = listOf(5, 1, 10, 25, 20, 0, 50, 5, 15, 1, 100, 20)

    // ✅ AGREGAR
    private var referralStats: ReferralManager.ReferralStats? = null

    // CONSTANTS
    private companion object {
        private const val TAG_MAIN = "MainActivity"
        private const val TAG_ADMOB = "AdMob_MainActivity"
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ✅ DESHABILITAR ANIMACIONES INNECESARIAS
        window.setFlags(
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        )
        try {
            // Verificar estado del consentimiento
            val consentCompleted = intent.getBooleanExtra("consent_completed", false)
            val adMobInitialized = intent.getBooleanExtra("admob_initialized", false)

            Log.d(TAG_MAIN, "=== INICIANDO MAINACTIVITY ===")
            Log.d(TAG_MAIN, "Build Type: ${if (BuildConfig.DEBUG) "DEBUG" else "RELEASE"}")
            Log.d(TAG_MAIN, "Consent Completed: $consentCompleted")
            Log.d(TAG_MAIN, "AdMob Initialized: $adMobInitialized")

            if (isFinishing || isDestroyed) {
                Log.w(TAG_MAIN, "Activity en estado inválido, abortando onCreate")
                return
            }

            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            window.apply {
                statusBarColor = Color.parseColor("#0A0F14")
            }

            setupWindowInsets()
            initializeCriticalSystems()

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

            if (BuildConfig.DEBUG) {
                // IDs de AdMob no necesitan ser consultados desde AdIds.kt en Main, solo la lógica.
            }

            // ✅ AGREGAR AL FINAL
            DeepLinkHandler.handle(this, intent)

        } catch (e: Exception) {
            Log.e(TAG_MAIN, "Error crítico en onCreate: ${e.message}")
            handleCriticalError(e)
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
                Log.w(TAG_MAIN, "⚠️ AdMob NO fue inicializado en SplashActivity")
            } else {
                Log.d(TAG_MAIN, "✅ AdMob correctamente inicializado desde SplashActivity")
            }
            setupFirebase()
            initializeAnalytics()
            ReferralManager.initialize()
            ShareManager.initialize()
            commManager = CommunicationManager.getInstance(this)
            initializeNotificationSystem()
            setupRealtimeConfigListeners()
            setupEmergencyStopListener()
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

    // ✅ AGREGAR ESTA FUNCIÓN NUEVA
    private fun initializeAnalytics() {
        try {
            AnalyticsManager.initialize(this)
            AnalyticsManager.logAppOpened()

            val playerId = SessionManager.getPlayerId(this)
            if (playerId.isNotEmpty()) {
                AnalyticsManager.setUserId(playerId)
            }

            Log.d(TAG_MAIN, "✅ Analytics inicializado")
        } catch (e: Exception) {
            Log.e(TAG_MAIN, "Error inicializando Analytics: ${e.message}")
        }
    }

    // REEMPLAZANDO con la versión CORREGIDA y simplificada del usuario
    private fun loadBannersWhenReady() {
        if (bannersLoaded) {
            Log.d(TAG_MAIN, "Banners ya cargados, saltando…")
            return
        }
        lifecycleScope.launch(Dispatchers.Main) {
            try {
                Log.d(TAG_MAIN, "🎯 Preparando carga de banners...")
                // Esperar a que AdMob esté listo (máximo 10 segundos)
                var waitTime = 0
                while (!AdsInit.isAdMobReady() && waitTime < 10000) {
                    Log.d(TAG_MAIN, "⏳ Esperando AdMob... (${waitTime}ms)")
                    delay(500)
                    waitTime += 500
                }
                // Esperar a que el consentimiento esté completo (máximo 10 segundos)
                waitTime = 0
                while (!UserMessagingPlatform.getConsentInformation(this@MainActivity).canRequestAds() && waitTime < 10000) {
                    Log.d(TAG_MAIN, "⏳ Esperando consentimiento... (${waitTime}ms)")
                    delay(500)
                    waitTime += 500
                }
                // Verificar condiciones finales
                if (!AdsInit.isAdMobReady()) {
                    Log.w(TAG_MAIN, "⚠️ AdMob no está listo después de esperar")
                    // Reintentar en 5 segundos
                    delay(5000)
                    loadBannersWhenReady()
                    return@launch
                }
                if (!UserMessagingPlatform.getConsentInformation(this@MainActivity).canRequestAds()) {
                    Log.w(TAG_MAIN, "⚠️ No se puede solicitar anuncios sin consentimiento")
                    // Reintentar en 5 segundos
                    delay(5000)
                    loadBannersWhenReady()
                    return@launch
                }
                if (bannersLoaded) {
                    Log.d(TAG_MAIN, "Banners cargados por otra rutina")
                    return@launch
                }
                Log.d(TAG_MAIN, "✅ Condiciones cumplidas, iniciando carga de banners...")
                // Cargar banner superior
                val topContainer = binding.sectionHome.homeBannerContainer
                topContainer.post {
                    BannerHelper.attachAdaptiveBanner(this@MainActivity, topContainer)
                }
                // Cargar banner inferior con un pequeño delay
                delay(500)
                val bottomContainer = findViewById<FrameLayout>(R.id.bottom_banner_container)
                bottomContainer.post {
                    BannerHelper.attachAdaptiveBanner(this@MainActivity, bottomContainer)
                }
                bannersLoaded = true
                updateBannerSpacing()
                Log.d(TAG_MAIN, "✅ Banners iniciados correctamente")
            } catch (e: Exception) {
                Log.e(TAG_MAIN, "❌ Error en loadBannersWhenReady: ${e.message}", e)
                // Reintentar en 10 segundos
                delay(10000)
                loadBannersWhenReady()
            }
        }
    }

    private fun debugBannerStatus() {
        lifecycleScope.launch {
            delay(5000) // Esperar 5 segundos después del inicio

            val topContainer = binding.sectionHome.homeBannerContainer
            val bottomContainer = findViewById<FrameLayout>(R.id.bottom_banner_container)

            Log.d(TAG_MAIN, "=== DEBUG BANNER STATUS ===")
            Log.d(TAG_MAIN, "AdMob Ready: ${AdsInit.isAdMobReady()}")
            Log.d(TAG_MAIN, "Can Request Ads: ${UserMessagingPlatform.getConsentInformation(this@MainActivity).canRequestAds()}")
            Log.d(TAG_MAIN, "Top Container - Visible: ${topContainer.visibility == View.VISIBLE}, Height: ${topContainer.height}")
            Log.d(TAG_MAIN, "Bottom Container - Visible: ${bottomContainer.visibility == View.VISIBLE}, Height: ${bottomContainer.height}")
            // Nota: Aquí se asume que AdIds.kt existe y tiene las funciones bannerTop() y bannerBottom()
            // Como AdIds.kt no está disponible, se deja un placeholder en el log.
            // Log.d(TAG_MAIN, "Banner IDs - Top: ${AdIds.bannerTop()}, Bottom: ${AdIds.bannerBottom()}")
            Log.d(TAG_MAIN, "Banner IDs - Top: [REDACTED], Bottom: [REDACTED]")
            Log.d(TAG_MAIN, "==========================")

            // Si después de 5 segundos no hay banners, forzar recarga
            if (topContainer.visibility != View.VISIBLE && bottomContainer.visibility != View.VISIBLE) {
                Log.w(TAG_MAIN, "⚠️ Ningún banner visible después de 5 segundos, forzando recarga...")
                bannersLoaded = false
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
            // ✅ CORRECCIÓN: Configurar contenedores sin cambiar layoutParams
            val homeContainer = binding.sectionHome.homeBannerContainer
            val bottomContainer = findViewById<FrameLayout>(R.id.bottom_banner_container)
            // Limpiar contenedores
            homeContainer.removeAllViews()
            bottomContainer.removeAllViews()
            // ✅ NO CAMBIAR layoutParams - el XML ya lo define correctamente
            homeContainer.visibility = View.GONE
            bottomContainer.visibility = View.GONE
            // Sin background
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
                delay(500L)
                initializeAdvertisingId()

                val playerId = SessionManager.getPlayerId(this@MainActivity)
                if (playerId.isNotEmpty()) {
                    initializeUserSession(playerId)
                }

                withContext(Dispatchers.Main) {
                    // Inicializar AdManager aquí (AdsInit.init ya fue llamado en Splash)
                    AdManager.initialize(this@MainActivity)
                    finalizeUIInitialization(playerId)

                    // Cargar banners después de 1 segundo
                    delay(1000L)
                    loadBannersWhenReady()

                    // Debug banner status
                    if (BuildConfig.DEBUG) {
                        debugBannerStatus()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG_MAIN, "Error en background initialization: ${e.message}")
            }
        }
    }

    private suspend fun initializeAdvertisingId() {
        try {
            // ✅ VERIFICAR SI YA FUE OBTENIDO
            val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
            val cachedAdId = prefs.getString("advertising_id", null)

            if (cachedAdId != null) {
                Log.d("ADVERTISING_ID", "✅ Usando ID cacheado")
                return
            }

            withContext(Dispatchers.IO) {
                val adInfo = AdvertisingIdClient.getAdvertisingIdInfo(applicationContext)
                val advertisingId = adInfo.id
                val isLimitAdTrackingEnabled = adInfo.isLimitAdTrackingEnabled
                // ✅ GUARDAR EN CACHE
                advertisingId?.let {
                    prefs.edit().putString("advertising_id", it).apply()
                }

                Log.d("ADVERTISING_ID", "✅ Advertising ID obtenido y cacheado")
                Log.d("ADVERTISING_ID", "Limit Ad Tracking: $isLimitAdTrackingEnabled")
            }
        } catch (e: Exception) {
            Log.e("ADVERTISING_ID", "❌ Error: ${e.message}")
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

            // Solo verificar referido si hay ID
            if (playerId.isNotEmpty()) {
                DeepLinkHandler.checkPendingReferralCode(this)
            }

            arreglarColoresDeTextos()
            initializeGrid()
            obtenerTop5Firebase()
            startWeeklyPrizeListener()
            startSorteoButtonAnimation()
            Handler(Looper.getMainLooper()).postDelayed({
                setupSorteoControlListener()
            }, 2000)
            fetchAllData()
            startWeeklyCountdown()
            checkNotificationPermissions()
            com.follgramer.diamantesproplayersgo.util.RatingPrompter.onAppStart(this)
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

    private fun setupMisionesNativeAd() {
        lifecycleScope.launch {
            try {
                Log.d("TASKS_AD", "🎯 Iniciando carga de anuncio nativo en tareas...")

                // Esperar 2 segundos antes de cargar
                delay(2000)

                // Verificar que la vista esté inflada
                if (!::binding.isInitialized) {
                    Log.e("TASKS_AD", "❌ Binding no inicializado")
                    return@launch
                }

                // Buscar los contenedores en section_tasks
                val adContainer = binding.sectionTasks.root.findViewById<FrameLayout>(R.id.native_ad_container_tasks)
                val nativeAdView = binding.sectionTasks.root.findViewById<com.google.android.gms.ads.nativead.NativeAdView>(R.id.native_ad_view_tasks)

                if (adContainer == null) {
                    Log.e("TASKS_AD", "❌ native_ad_container_tasks es NULL")
                    return@launch
                }

                if (nativeAdView == null) {
                    Log.e("TASKS_AD", "❌ native_ad_view_tasks es NULL")
                    return@launch
                }

                Log.d("TASKS_AD", "✅ Contenedores encontrados correctamente")
                Log.d("TASKS_AD", "📦 adContainer: $adContainer")
                Log.d("TASKS_AD", "📦 nativeAdView: $nativeAdView")

                withContext(Dispatchers.Main) {
                    NativeAdHelper.loadNativeAd(
                        activity = this@MainActivity,
                        container = adContainer,
                        nativeAdView = nativeAdView,
                        holderId = 888 // ID único para esta sección
                    )
                }

                Log.d("TASKS_AD", "✅ Carga de anuncio nativo iniciada")

            } catch (e: Exception) {
                Log.e("TASKS_AD", "❌ Error: ${e.message}", e)
                e.printStackTrace()
            }
        }
    }

    override fun onStart() {
        super.onStart()

        lifecycleScope.launch(Dispatchers.Main) {
            com.follgramer.diamantesproplayersgo.util.RatingPrompter.maybeAsk(this@MainActivity)
            com.follgramer.diamantesproplayersgo.util.AdminModalManager.fetchAndShow(this@MainActivity)
            com.follgramer.diamantesproplayersgo.util.InAppUpdateHelper.check(this@MainActivity)
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

            lifecycleScope.launch {
                val unreadCount = AppNotificationManager.getInstance(this@MainActivity).getUnreadCount()
                Log.d(TAG_MAIN, "📬 Unread notifications: $unreadCount")
            }

            // Resumir banners
            if (::binding.isInitialized) {
                Log.d(TAG_MAIN, "Resumiendo banners...")
                BannerHelper.resume(binding.root)
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
        // ✅ AGREGAR
        DeepLinkHandler.handle(this, intent)
    }

    override fun onDestroy() {
        commManager.cleanup()
        try {
            Log.d(TAG_MAIN, "🛑 Deteniendo listeners de notificación")
            AppNotificationManager.getInstance(this).stopListening()
            NotificationEventBus.unsubscribe { }

            if (::binding.isInitialized) {
                Log.d(TAG_MAIN, "Destruyendo banners...")
                BannerHelper.destroy(binding.root)
            }

        } catch (e: Exception) {
            Log.e(TAG_MAIN, "Error en limpieza de banners: ${e.message}")
        }

        cleanupResources()
        super.onDestroy()
    }

    private fun cleanupResources() {
        try {
            Log.d(TAG_MAIN, "🧹 Limpiando recursos...")

            releaseAudioFocus()
            countdownTimer?.cancel()
            revealCountdownTimer?.cancel()
            countdownTimer = null
            revealCountdownTimer = null

            // ✅ LIMPIAR LISTENER DE WEEKLY PRIZE
            weeklyPrizeListener?.let { listener ->
                weeklyPrizeRef?.removeEventListener(listener)
            }
            weeklyPrizeListener = null
            weeklyPrizeRef = null

            // ✅ LIMPIAR LISTENER DE PLAYER DATA
            playerDataListener?.let { listener ->
                playerDataRef?.removeEventListener(listener)
            }
            playerDataListener = null
            playerDataRef = null

            // ✅ LIMPIAR TODOS LOS LISTENERS DE FIREBASE
            firebaseReferences.forEachIndexed { index, ref ->
                try {
                    val listener = firebaseListeners.getOrNull(index)
                    listener?.let { ref.removeEventListener(it) }
                } catch (e: Exception) {
                    Log.w(TAG_MAIN, "Error removing listener: ${e.message}")
                }
            }
            firebaseListeners.clear()
            firebaseReferences.clear()

            audioManager = null
            audioFocusRequest = null
            hasAudioFocus = false

            AdManager.cleanup()
            marcarSesionInactiva()

            Log.d(TAG_MAIN, "✅ Recursos limpiados exitosamente")
        } catch (e: Exception) {
            Log.e(TAG_MAIN, "❌ Error limpiando recursos: ${e.message}")
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        when (level) {
            TRIM_MEMORY_RUNNING_LOW,
            TRIM_MEMORY_RUNNING_CRITICAL -> {
                Log.w(TAG_MAIN, "⚠️ Memoria baja detectada, limpiando cache...")
                // Pausar animaciones
                try {
                    binding.sectionHome.sorteoButtonContainer.clearAnimation()
                } catch (e: Exception) {
                    Log.e(TAG_MAIN, "Error pausando animaciones: ${e.message}")
                }
                // Limpiar cache de imágenes si tienes
                System.gc()
            }
            TRIM_MEMORY_UI_HIDDEN -> {
                Log.d(TAG_MAIN, "UI oculto, pausando operaciones pesadas")
            }
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
                        // ✅ CONSTRUIR MENSAJE EN ESPAÑOL CORRECTO
                        val cantidad = event.amount.toIntOrNull() ?: 0
                        val giftMessage = when (event.unit) {
                            "passes" -> if (cantidad == 1) "Has recibido $cantidad pase" else "Has recibido $cantidad pases"
                            "tickets" -> if (cantidad == 1) "Has recibido $cantidad ticket" else "Has recibido $cantidad tickets"
                            "spins" -> if (cantidad == 1) "Has recibido $cantidad giro" else "Has recibido $cantidad giros"
                            else -> event.message
                        }

                        notificationManager.showSystemNotification(
                            "gift",
                            "🎁 Regalo Recibido",  // ✅ Traducido
                            giftMessage,
                            NotificationCompat.PRIORITY_DEFAULT
                        )
                        showGeneralModal("🎁 Regalo Recibido", giftMessage)  // ✅ Traducido
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

        // NUEVA LÓGICA: Solo cargar notificación si fue creada después del registro del usuario
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                delay(2000)

                val playerId = SessionManager.getPlayerId(this@MainActivity)
                if (playerId.isEmpty()) {
                    Log.d(TAG_MAIN, "⚠️ No hay player ID, saltando notificaciones")
                    return@launch
                }

                // Obtener fecha de registro del usuario
                val playerSnapshot = database.child("players").child(playerId).get().await()
                val registeredAt = playerSnapshot.child("registeredAt").getValue(Long::class.java) ?: 0L

                // Solo mostrar notificación si existe y es posterior al registro
                val lastNotif = database.child("appConfig").child("lastNotification").get().await()

                if (lastNotif.exists()) {
                    val notifTimestamp = lastNotif.child("timestamp").getValue(Long::class.java) ?: 0L

                    // Solo mostrar si la notificación es posterior al registro del usuario
                    if (notifTimestamp > registeredAt && registeredAt > 0) {
                        val data = lastNotif.value as? Map<String, Any>
                        if (data != null) {
                            val notificationId = data["id"] as? String ?: System.currentTimeMillis().toString()

                            // Verificar si ya fue vista
                            val wasViewed = database.child("appConfig")
                                .child("viewedNotifications")
                                .child(playerId)
                                .child(notificationId)
                                .get()
                                .await()
                                .getValue(Boolean::class.java) ?: false

                            if (!wasViewed) {
                                // Mostrar notificación
                                val notificationData = AppNotificationManager.NotificationData(
                                    id = notificationId,
                                    type = data["type"] as? String ?: "general",
                                    title = data["title"] as? String ?: "Notificación",
                                    body = data["body"] as? String,
                                    message = data["message"] as? String ?: "",
                                    timestamp = notifTimestamp,
                                    amount = data["amount"] as? String,
                                    unit = data["unit"] as? String,
                                    sound = true,
                                    vibrate = true,
                                    isRead = false
                                )

                                withContext(Dispatchers.Main) {
                                    showGeneralModal(notificationData.title, notificationData.message)
                                    markNotificationAsViewed(playerId, notificationId)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG_MAIN, "Error cargando notificación: ${e.message}")
            }
        }
    }

    private fun markNotificationAsViewed(playerId: String, notificationId: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                database.child("appConfig")
                    .child("viewedNotifications")
                    .child(playerId)
                    .child(notificationId)
                    .setValue(true)
                    .await()

                Log.d(TAG_MAIN, "✅ Notificación marcada como vista: $notificationId")
            } catch (e: Exception) {
                Log.e(TAG_MAIN, "Error marcando notificación como vista: ${e.message}")
            }
        }
    }

    private fun setupRealtimeConfigListeners() {
        Log.d(TAG_MAIN, "🔄 Configurando listeners optimizados...")
        // ✅ USAR UN SOLO LISTENER PARA TODA LA CONFIGURACIÓN
        database.child("appConfig").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    // Premios Semanales
                    val weeklyPrizesSnap = snapshot.child("weeklyPrizes")
                    if (weeklyPrizesSnap.exists()) {
                        val first = weeklyPrizesSnap.child("first").getValue(String::class.java) ?: "1,000 💎"
                        val second = weeklyPrizesSnap.child("second").getValue(String::class.java) ?: "500 💎"
                        val third = weeklyPrizesSnap.child("third").getValue(String::class.java) ?: "200 💎"
                        runOnUiThread {
                            binding.sectionHome.prize1Text.text = first
                            binding.sectionHome.prize2Text.text = second
                            binding.sectionHome.prize3Text.text = third
                        }
                    }
                    // Botón de Sorteo
                    val sorteoButtonSnap = snapshot.child("sorteoButton")
                    if (sorteoButtonSnap.exists()) {
                        val buttonText = sorteoButtonSnap.child("text").getValue(String::class.java) ?: "ACTIVO"
                        val buttonSubtext = sorteoButtonSnap.child("subtext").getValue(String::class.java) ?: "Sorteo en curso"
                        runOnUiThread {
                            binding.sectionHome.sorteoButtonText.text = buttonText
                            binding.sectionHome.sorteoButtonSubtext.text = buttonSubtext
                            binding.sectionHome.sorteoButtonContainer.visibility = View.VISIBLE
                        }
                    }
                    Log.d(TAG_MAIN, "✅ Configuración actualizada")

                } catch (e: Exception) {
                    Log.e(TAG_MAIN, "❌ Error actualizando config: ${e.message}")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG_MAIN, "❌ Error listener config: ${error.message}")
            }
        })
    }

    private fun startSorteoButtonAnimation() {
        lifecycleScope.launch {
            // ✅ Pequeño delay inicial para evitar lag en el inicio
            delay(1000)

            while (isActive) {
                try {
                    withContext(Dispatchers.Main) {
                        binding.sectionHome.sorteoButtonContainer.apply {
                            visibility = View.VISIBLE
                            alpha = 0f

                            // ✅ Fade In suave
                            animate()
                                .alpha(1f)
                                .setDuration(600)
                                .withStartAction {
                                    // ✅ Aplicar animación SOLO cuando está visible
                                    val pulseAnimation = android.view.animation.AnimationUtils
                                        .loadAnimation(this@MainActivity, R.anim.pulse_live_animation)
                                    startAnimation(pulseAnimation)
                                }
                                .start()
                        }
                    }
                    delay(5000) // Visible con pulse por 5 segundos
                    withContext(Dispatchers.Main) {
                        binding.sectionHome.sorteoButtonContainer.apply {
                            // ✅ Detener animación antes de ocultar
                            clearAnimation()

                            // ✅ Fade Out suave
                            animate()
                                .alpha(0f)
                                .setDuration(600)
                                .withEndAction {
                                    visibility = View.INVISIBLE
                                    alpha = 1f // Reset para próximo ciclo
                                }
                                .start()
                        }
                    }
                    delay(3000) // Oculto por 3 segundos
                } catch (e: Exception) {
                    Log.e(TAG_MAIN, "Error en animación: ${e.message}")
                }
            }
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
                delay(30000) // ✅ CAMBIAR DE 5 segundos a 30 segundos
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
                binding.sectionLeaderboard.leaderboardRecyclerView.adapter =
                    LeaderboardAdapter(this@MainActivity, this@MainActivity, leaderboardItems.toMutableList(), currentPlayerId)

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
            val ref = database.child("appConfig").child("weeklyPrize").child("text")
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val prize = snapshot.getValue(String::class.java)?.trim().orEmpty()
                    runOnUiThread {
                        if (prize.isNotEmpty()) {
                            binding.sectionHome.tvWeeklyPrize.text = getString(R.string.weekly_prize_template, prize)
                        } else {
                            binding.sectionHome.tvWeeklyPrize.text = "Prize: 1,000 Diamonds ✨"
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    runOnUiThread {
                        binding.sectionHome.tvWeeklyPrize.text = "Prize: 1,000 Diamonds ✨"
                    }
                }
            }
            ref.addValueEventListener(listener)
            weeklyPrizeRef = ref
            weeklyPrizeListener = listener // Assign the listener to the variable
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
            // Hide all states
            processingStateContainer.visibility = View.GONE
            revealStateContainer.visibility = View.GONE
            winnerRevealedContainer.visibility = View.GONE
            // Show normal state
            normalStateContainer.visibility = View.VISIBLE

            // ✅ USAR sorteoButtonContainer en lugar de sorteoStatusBadge
            sorteoButtonContainer.visibility = View.VISIBLE
            // Reset texts
            countdownLabel.text = "Ends in:"
            countdownLabel.setTextColor(Color.parseColor("#8B949E"))
        }
        // Restart countdown if not running
        if (countdownTimer == null) {
            startWeeklyCountdown()
        }
    }

    private fun showProcessingState() {
        binding.sectionHome.apply {
            // Hide other states
            normalStateContainer.visibility = View.GONE
            processingStateContainer.visibility = View.GONE
            revealStateContainer.visibility = View.GONE
            winnerRevealedContainer.visibility = View.GONE
            // Show processing state
            processingStateContainer.visibility = View.VISIBLE
            // ✅ ACTUALIZAR el botón amarillo
            sorteoButtonContainer.visibility = View.VISIBLE
            sorteoButtonText.text = "PROCESANDO"
            sorteoButtonText.setTextColor(Color.parseColor("#f59e0b"))
            sorteoButtonSubtext.text = "Espera un momento..."
            // Update message
            processingMessage.text = "Processing draw..."
            // Update title based on message
            processingTitle.text = "Processing Draw"
        }
    }

    private fun showPausedState(message: String) {
        currentSorteoState = "paused"
        binding.sectionHome.apply {
            // Show normal state but with modifications
            normalStateContainer.visibility = View.VISIBLE
            processingStateContainer.visibility = View.GONE
            revealStateContainer.visibility = View.GONE
            winnerRevealedContainer.visibility = View.GONE
            // ✅ ACTUALIZAR el botón amarillo
            sorteoButtonContainer.visibility = View.VISIBLE
            sorteoButtonText.text = "PAUSADO"
            sorteoButtonText.setTextColor(Color.parseColor("#f59e0b"))
            sorteoButtonSubtext.text = message.ifEmpty { "Draw temporarily paused" }
            // Change countdown
            countdown.text = "PAUSED"
            countdown.setTextColor(Color.parseColor("#f59e0b"))
            countdownLabel.text = message.ifEmpty { "Draw temporarily paused" }
        }
    }

    private fun showRevealState(remaining: Long, winnerId: String, prize: String) {
        binding.sectionHome.apply {
            // Hide other states
            normalStateContainer.visibility = View.GONE
            processingStateContainer.visibility = View.GONE
            winnerRevealedContainer.visibility = View.GONE
            // Show reveal state
            revealStateContainer.visibility = View.VISIBLE
            // ✅ ACTUALIZAR el botón amarillo
            sorteoButtonContainer.visibility = View.VISIBLE
            sorteoButtonText.text = "EN VIVO!"
            sorteoButtonText.setTextColor(Color.parseColor("#e74c3c"))
            sorteoButtonSubtext.text = "Revelando ganador..."
            // Update countdown
            val seconds = (remaining / 1000).toInt()
            revealCountdown.text = String.format("%02d:%02d", seconds / 60, seconds % 60)
            revealCountdown.textSize = 28f
            // Change color based on remaining time
            val countdownColor = when {
                seconds > 60 -> Color.parseColor("#f59e0b")
                seconds > 30 -> Color.parseColor("#FFD700")
                seconds > 10 -> Color.parseColor("#ff9800")
                else -> Color.parseColor("#e74c3c")
            }
            revealCountdown.setTextColor(countdownColor)
            // Pulsing animation in the last 10 seconds
            if (seconds <= 10 && seconds > 0) {
                revealCountdown.animate()
                    .scaleX(1.1f).scaleY(1.1f)
                    .setDuration(300)
                    .withEndAction {
                        revealCountdown.animate()
                            .scaleX(1f).scaleY(1f)
                            .setDuration(300)
                    }
            }
            if (remaining <= 3000) {
                // Show "WINNER CHOSEN" animation at the end
                revealCountdown.text = "WINNER CHOSEN!"
                revealCountdown.textSize = 20f
                revealCountdown.setTextColor(Color.parseColor("#FFD700"))
                vibratePattern(longArrayOf(0, 200, 100, 200, 100, 200))
            }
        }
    }

    private fun showWinnerRevealedState(winnerId: String, prize: String) {
        binding.sectionHome.apply {
            // Hide other states
            normalStateContainer.visibility = View.GONE
            processingStateContainer.visibility = View.GONE
            revealStateContainer.visibility = View.GONE
            // Show winner revealed state
            winnerRevealedContainer.visibility = View.VISIBLE
            // ✅ ACTUALIZAR el botón amarillo
            sorteoButtonContainer.visibility = View.VISIBLE
            sorteoButtonText.text = "COMPLETADO"
            sorteoButtonText.setTextColor(Color.parseColor("#2ecc71"))
            sorteoButtonSubtext.text = "Sorteo finalizado"
            // Update texts
            winnerIdText.text = "Winner: $winnerId"
            prizeWonText.text = "Prize: $prize"
            // If we are the winner
            if (currentPlayerId == winnerId) {
                vibratePattern(longArrayOf(0, 500, 100, 500, 100, 1000))
                // Change spin button color
                spinButton.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FFD700"))
                spinButton.text = "🏆 YOU ARE THE WINNER! 🏆"
                // Restore button after 30 seconds
                Handler(Looper.getMainLooper()).postDelayed({
                    spinButton.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#00a8ff"))
                    spinButton.text = "SPIN THE WHEEL!"
                }, 30000)
            }
        }
        // Return to normal state after 2 minutes
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

        // Get draw configuration from Firebase
        database.child("appConfig").child("weeklyPrize").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val drawDay = snapshot.child("day").getValue(Int::class.java) ?: 0 // Sunday by default (0)
                val drawTime = snapshot.child("time").getValue(String::class.java) ?: "18:00"
                val timeParts = drawTime.split(":")
                val hour = timeParts.getOrNull(0)?.toIntOrNull() ?: 18
                val minute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0

                val calendar = Calendar.getInstance()
                val now = calendar.timeInMillis

                // Configure next draw
                calendar.set(Calendar.DAY_OF_WEEK, drawDay + 1) // Calendar uses 1=Sunday, our panel 0=Sunday
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)


                // If it has already passed, schedule for next week
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
                        binding.sectionHome.countdown.text = "Draw in Progress!"
                        binding.sectionHome.countdown.setTextColor(Color.parseColor("#FFD700"))

                        // Restart for next week
                        Handler(Looper.getMainLooper()).postDelayed({
                            startWeeklyCountdown()
                        }, 60000) // Wait 1 minute before restarting
                    }
                }.start()
            }

            override fun onCancelled(error: DatabaseError) {
                // Use default configuration if it fails
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
                                binding.sectionWinners.winnersRecyclerView.adapter =
                                    WinnersAdapter(this@MainActivity, winnerItems.toMutableList())
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

            // ✅ REMOVER LISTENER ANTERIOR SI EXISTE
            playerDataListener?.let { listener ->
                playerDataRef?.removeEventListener(listener)
            }

            val playerRef = database.child("players").child(playerId)

            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val tickets = snapshot.child("tickets").getValue(Long::class.java) ?: 0L
                        val passes = snapshot.child("passes").getValue(Long::class.java) ?: 0L
                        val firebaseSpins = snapshot.child("spins").getValue(Long::class.java)?.toInt() ?: 10
                        val localSpins = SessionManager.getCurrentSpins(this@MainActivity)
                        if (firebaseSpins != localSpins) {
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
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FETCH_PLAYER", "❌ Error: ${error.message}")
                }
            }

            playerRef.addValueEventListener(listener)

            // ✅ GUARDAR REFERENCIA PARA LIMPIEZA
            playerDataListener = listener
            playerDataRef = playerRef
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
                    Color.WHITE,  // ✅ TEXTO BLANCO CUANDO ESTÁ SELECCIONADO
                    Color.parseColor("#E5E7EB")  // Texto gris cuando no está seleccionado
                )
            )
            val iconColors = ColorStateList(
                arrayOf(
                    intArrayOf(android.R.attr.state_checked),
                    intArrayOf(-android.R.attr.state_checked)
                ),
                intArrayOf(
                    Color.parseColor("#00A8FF"),  // ✅ ICONO AZUL CUANDO ESTÁ SELECCIONADO
                    Color.parseColor("#9CA3AF")   // Icono gris cuando no está seleccionado
                )
            )
            binding.navView.itemTextColor = menuItemColors
            binding.navView.itemIconTintList = iconColors
            binding.navView.itemBackground = createSelectableBackground()
        } catch (e: Exception) {
            Log.e("NAV_COLORS", "Error configurando colores: ${e.message}")
        }
    }

    private fun createSelectableBackground(): Drawable {
        return try {
            val selectedColor = Color.parseColor("#1A00A8FF")  // ✅ FONDO AZUL TRANSPARENTE
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
            // ✅ BUSCAR LOS TextViews en el LinearLayout inferior del drawer
            val navPrivacy = binding.drawerLayout.findViewById<TextView>(R.id.nav_privacy)
            val navTerms = binding.drawerLayout.findViewById<TextView>(R.id.nav_terms)
            // ✅ VERIFICAR QUE EXISTAN
            if (navPrivacy == null) {
                Log.e("LEGAL_SETUP", "❌ nav_privacy NO encontrado en el layout")
                return
            }

            if (navTerms == null) {
                Log.e("LEGAL_SETUP", "❌ nav_terms NO encontrado en el layout")
                return
            }
            Log.d("LEGAL_SETUP", "✅ Configurando listeners de opciones legales")
            // ✅ CONFIGURAR CLICK LISTENERS
            navPrivacy.setOnClickListener {
                Log.d("LEGAL_SETUP", "📄 Click en Políticas de Privacidad")
                binding.drawerLayout.closeDrawer(GravityCompat.START)
                binding.root.postDelayed({
                    showLegalModal("Política de Privacidad", getString(R.string.privacy_policy_content))
                }, 300)
            }
            navTerms.setOnClickListener {
                Log.d("LEGAL_SETUP", "📄 Click en Términos y Condiciones")
                binding.drawerLayout.closeDrawer(GravityCompat.START)
                binding.root.postDelayed({
                    showLegalModal("Términos y Condiciones", getString(R.string.terms_content))
                }, 300)
            }
            Log.d("LEGAL_SETUP", "✅ Listeners configurados correctamente")
        } catch (e: Exception) {
            Log.e("LEGAL_SETUP", "❌ Error: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun setupRecyclerViews() {
        binding.sectionLeaderboard.leaderboardRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.sectionLeaderboard.leaderboardRecyclerView.adapter =
            LeaderboardAdapter(this, this, mutableListOf(), null)
        binding.sectionWinners.winnersRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.sectionWinners.winnersRecyclerView.adapter = WinnersAdapter(this, mutableListOf())
        // DO NOT initialize the mini leaderboard adapter
        binding.sectionHome.miniLeaderboardList.layoutManager = LinearLayoutManager(this)
        // DO NOT assign adapter here
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
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            clearAllMenuSelections()

            // ✅ Solo marcar como checked si es del grupo principal
            if (menuItem.groupId == R.id.group_main) {
                menuItem.isChecked = true
            }

            when (menuItem.itemId) {
                R.id.nav_home -> {
                    showSectionSafely(binding.sectionHome.root)
                    AnalyticsManager.logScreenView("Home")
                }
                R.id.nav_tasks -> {
                    showSectionWithAd(binding.sectionTasks.root)
                    setupMisionesNativeAd()
                    AnalyticsManager.logScreenView("Tasks")
                }
                R.id.nav_leaderboard -> {
                    showSectionWithAd(binding.sectionLeaderboard.root)
                    AnalyticsManager.logScreenView("Leaderboard")
                }
                R.id.nav_winners -> {
                    showSectionWithAd(binding.sectionWinners.root)
                    AnalyticsManager.logScreenView("Winners")
                }
                R.id.nav_referrals -> {
                    showSectionWithAd(binding.sectionReferrals.root)
                    setupReferralsSection()
                    AnalyticsManager.logScreenView("Referrals")
                }
                R.id.nav_share -> {
                    shareAppDirectly()
                    return false
                }
                else -> {
                    showSectionSafely(binding.sectionHome.root)
                    return false
                }
            }
            true
        } catch (e: Exception) {
            Log.e("NAVIGATION", "Critical error: ${e.message}")
            showSectionSafely(binding.sectionHome.root)
            false
        }
    }

    // AGREGAR ESTAS DOS FUNCIONES NUEVAS
    private fun showSectionSafely(section: View) {
        runOnUiThread {
            hideAllSections()
            section.visibility = View.VISIBLE
        }
    }

    private fun showSectionWithAd(section: View) {
        if (AdManager.isInterstitialReady()) {
            AdManager.showInterstitial(this) {
                showSectionSafely(section)
            }
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
        binding.sectionReferrals.root.visibility = View.GONE
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
        // Verificar si hay rewarded disponible
        if (!AdManager.isRewardedReady()) {
            Log.d(TAG_ADMOB, "🔄 No hay rewarded disponible, usando fallback")

            AdFallbackStrategy.handleRewardedAdUnavailable(
                activity = this,
                rewardType = "spins",
                rewardAmount = 10, // Cantidad normal
                onSuccess = {
                    // Dar solo 3 giros como fallback
                    val spinsToAdd = 3
                    SessionManager.addSpins(this, spinsToAdd)
                    currentSpins = SessionManager.getCurrentSpins(this)
                    updateSpinCountUI()

                    AnalyticsManager.logSpinsEarned(spinsToAdd, "fallback")
                    Log.d(TAG_ADMOB, "✅ Fallback completado: $spinsToAdd giros otorgados")
                }
            )
            releaseAudioFocus()
            return
        }
        // Si hay rewarded, mostrarlo normalmente
        AdManager.showRewarded(this,
            onReward = { rewardItem ->
                releaseAudioFocus()
                Log.d(TAG_ADMOB, "Reward de ${rewardItem.amount} ${rewardItem.type} obtenido.")
                val spinsToAdd = 10
                SessionManager.addSpins(this, spinsToAdd)
                currentSpins = SessionManager.getCurrentSpins(this)
                updateSpinCountUI()
                AnalyticsManager.logSpinsEarned(spinsToAdd, "rewarded_ad")
                AnalyticsManager.logRewardedAdCompleted(spinsToAdd, "spins")
            },
            onDismiss = {
                releaseAudioFocus()
            }
        )
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
        // Verificar si hay rewarded disponible
        if (!AdManager.isRewardedReady()) {
            Log.d(TAG_ADMOB, "🔄 No hay rewarded disponible, usando fallback")

            AdFallbackStrategy.handleRewardedAdUnavailable(
                activity = this,
                rewardType = "tickets",
                rewardAmount = 20, // Cantidad normal
                onSuccess = {
                    // Dar solo 10 tickets como fallback
                    val ticketsToAdd = 10
                    addTicketsToPlayer(ticketsToAdd)

                    AnalyticsManager.logTicketsEarned(ticketsToAdd, "fallback")
                    Log.d(TAG_ADMOB, "✅ Fallback completado: $ticketsToAdd tickets otorgados")
                }
            )
            releaseAudioFocus()
            return
        }
        // Si hay rewarded, mostrarlo normalmente
        AdManager.showRewarded(this,
            onReward = { rewardItem ->
                releaseAudioFocus()
                Log.d(TAG_ADMOB, "Reward de ${rewardItem.amount} ${rewardItem.type} obtenido.")
                val ticketsToAdd = if (rewardItem.amount > 0) rewardItem.amount else 20
                addTicketsToPlayer(ticketsToAdd)

                AnalyticsManager.logTicketsEarned(ticketsToAdd, "rewarded_ad")
                AnalyticsManager.logRewardedAdCompleted(ticketsToAdd, "tickets")
            },
            onDismiss = {
                releaseAudioFocus()
            }
        )
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
        // Validación crítica: Verificar ID antes de permitir girar
        if (currentPlayerId == null || currentPlayerId.isNullOrEmpty()) {
            Log.w(TAG_MAIN, "⚠️ Intento de girar sin ID configurado")
            MaterialDialog(this).show {
                title(text = "⚠️ ID de Jugador Requerido")
                message(text = "Debes configurar tu ID de jugador antes de poder girar la ruleta.\n\n¿Deseas configurarlo ahora?")
                positiveButton(text = "Configurar ID") {
                    promptForPlayerId()
                }
                negativeButton(text = "Cancelar")
                cancelable(false)
            }
            return
        }
        if (isSpinning) return
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

                if (prizeValue > 0) {
                    addTicketsToPlayer(prizeValue)
                    // ✅ AGREGAR
                    AnalyticsManager.logSpinCompleted(prizeValue)
                    AnalyticsManager.logTicketsEarned(prizeValue, "spin")
                }

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
                binding.sectionHome.myPlayerId.text = "Toca para configurar"
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

            // ✅ AGREGAR: Verificar recompensas de referidos
            checkReferralRewards(passes)
        } catch (e: Exception) {
            Log.e("UPDATE_UI", "Error updating UI: ${e.message}")
        }
    }

    private fun promptForPlayerId() {
        MaterialDialog(this).show {
            title(text = if (currentPlayerId != null) "Editar ID de Jugador" else "Configurar tu ID")
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
                            .setMessage("¿Estás seguro de cambiar el ID de $currentPlayerId a $newPlayerId?")
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
                } else {
                    Toast.makeText(this@MainActivity, "El ID debe tener al menos 5 dígitos", Toast.LENGTH_SHORT).show()
                }
            }
            positiveButton(text = "Guardar")
            negativeButton(text = "Cancelar")
        }
    }

    private fun loadUserData(playerId: String) {
        try {
            Log.d(TAG_MAIN, "Loading data for user: $playerId")
            checkUserBanStatus(playerId)
            createFirebaseSession(playerId)
            fetchPlayerData(playerId)
            obtenerTop5Firebase()
            updateUI(0, 0)
            checkForPrivateMessages(playerId)
            setupFCMToken(playerId)
            // Start notification listener
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

            // Analytics y referidos
            AnalyticsManager.logPlayerIdConfigured(playerId)
            AnalyticsManager.setUserId(playerId)

            // ✅ VERIFICAR CÓDIGO DE REFERIDO PENDIENTE
            DeepLinkHandler.checkPendingReferralCode(this)
            Log.d(TAG_MAIN, "User data loaded completely")
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
        binding.sectionHome.spinButton.text = "YOU ARE THE WINNER!"
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
                positiveButton(text = "ENTENDIDO")
                cancelable(true)
            }
        } catch (e: Exception) {
            Log.e("GENERAL_MODAL", "Error: ${e.message}")
        }
    }

    private fun updateBannerSpacing() {
        try {
            val bannerContainer = findViewById<FrameLayout>(R.id.bottom_banner_container)
            val spacer = findViewById<View>(R.id.bottom_banner_spacer)
            if (bannerContainer?.visibility == View.VISIBLE && bannerContainer.childCount > 0) {
                // Hay banner, mostrar espaciador
                spacer?.visibility = View.VISIBLE
                spacer?.layoutParams?.height = dpToPx(50)
            } else {
                // No hay banner, ocultar espaciador
                spacer?.visibility = View.GONE
                spacer?.layoutParams?.height = 0
            }
        } catch (e: Exception) {
            Log.e("BANNER_SPACING", "Error: ${e.message}")
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    // ==================== FUNCIONES DE COMPARTIR ====================
    private fun shareAppDirectly() {
        lifecycleScope.launch {
            try {
                ShareManager.shareApp(this@MainActivity, null)
                val playerId = SessionManager.getPlayerId(this@MainActivity)
                ShareManager.logShare(playerId, "app")
            } catch (e: Exception) {
                Log.e(TAG_MAIN, "Error compartiendo app: ${e.message}")
            }
        }
    }

    // ==================== FUNCIONES DE REFERIDOS ====================
    private fun setupReferralsSection() {
        lifecycleScope.launch {
            try {
                val playerId = SessionManager.getPlayerId(this@MainActivity)
                if (playerId.isEmpty()) {
                    showReferralError("Necesitas configurar tu ID de jugador primero")
                    return@launch
                }

                val referralCode = ReferralManager.getReferralCode(playerId)
                binding.sectionReferrals.tvMyReferralCode.text = referralCode

                referralStats = ReferralManager.getReferralStats(playerId)
                updateReferralStats()

                setupReferralButtons(referralCode)
            } catch (e: Exception) {
                Log.e(TAG_MAIN, "Error configurando referidos: ${e.message}")
                showReferralError("Error cargando información de referidos")
            }
        }
    }

    private fun updateReferralStats(stats: ReferralManager.ReferralStats? = referralStats) {
        stats?.let {
            binding.sectionReferrals.tvTotalReferrals.text = it.totalReferrals.toString()
            binding.sectionReferrals.tvTotalRewards.text = it.totalRewards.toString()
        }
    }

    private fun setupReferralButtons(referralCode: String) {
        binding.sectionReferrals.btnCopyCode.setOnClickListener {
            copyToClipboard(referralCode, "Código copiado al portapapeles")
        }
        binding.sectionReferrals.btnShareReferral.setOnClickListener {
            shareWithReferral(referralCode)
        }
        binding.sectionReferrals.btnShareApp.setOnClickListener {
            shareAppDirectly()
        }
        binding.sectionReferrals.btnUseReferralCode.setOnClickListener {
            useReferralCode()
        }
    }

    private fun copyToClipboard(text: String, message: String) {
        try {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Referral Code", text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            AnalyticsManager.logFeatureUsed("referral_code_copied")
        } catch (e: Exception) {
            Log.e(TAG_MAIN, "Error copiando al portapapeles: ${e.message}")
        }
    }

    private fun shareWithReferral(referralCode: String) {
        lifecycleScope.launch {
            try {
                ShareManager.shareApp(this@MainActivity, referralCode)
                val playerId = SessionManager.getPlayerId(this@MainActivity)
                ShareManager.logShare(playerId, "referral")
            } catch (e: Exception) {
                Log.e(TAG_MAIN, "Error compartiendo con referido: ${e.message}")
            }
        }
    }

    private fun useReferralCode() {
        val inputCode = binding.sectionReferrals.etReferralCode.text.toString().trim().uppercase()
        if (inputCode.isEmpty()) {
            showReferralStatus("Ingresa un código de referido", android.R.color.holo_red_light)
            return
        }
        lifecycleScope.launch {
            try {
                binding.sectionReferrals.btnUseReferralCode.isEnabled = false
                binding.sectionReferrals.btnUseReferralCode.text = "Procesando..."

                val success = ReferralManager.processReferralCodeAsync(this@MainActivity, inputCode)

                if (success) {
                    showReferralStatus("✅ Código aplicado exitosamente", android.R.color.holo_green_light)
                    binding.sectionReferrals.etReferralCode.setText("")
                    AnalyticsManager.logFeatureUsed("referral_code_used")
                } else {
                    showReferralStatus("❌ Código no válido o ya usado", android.R.color.holo_red_light)
                }
            } catch (e: Exception) {
                Log.e(TAG_MAIN, "Error usando código: ${e.message}")
                showReferralStatus("Error procesando código", android.R.color.holo_red_light)
            } finally {
                binding.sectionReferrals.btnUseReferralCode.isEnabled = true
                binding.sectionReferrals.btnUseReferralCode.text = "Usar"
            }
        }
    }

    private fun showReferralStatus(message: String, colorRes: Int) {
        binding.sectionReferrals.tvReferralStatus.apply {
            text = message
            setTextColor(ContextCompat.getColor(this@MainActivity, colorRes))
            visibility = View.VISIBLE
        }
        Handler(Looper.getMainLooper()).postDelayed({
            binding.sectionReferrals.tvReferralStatus.visibility = View.GONE
        }, 5000)
    }

    private fun showReferralError(message: String) {
        MaterialDialog(this).show {
            title(text = "Error de Referidos")
            message(text = message)
            positiveButton(text = "Entendido")
        }
    }

    private fun checkReferralRewards(currentPasses: Long) {
        lifecycleScope.launch {
            try {
                val playerId = SessionManager.getPlayerId(this@MainActivity)
                if (playerId.isNotEmpty()) {
                    ReferralManager.checkAndGrantReward(this@MainActivity, playerId, currentPasses)
                }
            } catch (e: Exception) {
                Log.e(TAG_MAIN, "Error verificando recompensas: ${e.message}")
            }
        }
    }
}