package com.follgramer.diamantesproplayersgo

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.follgramer.diamantesproplayersgo.databinding.ActivitySplashBinding
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val TOTAL_SPLASH_TIME = 5000L
    private var hasNavigated = false

    // Control de componentes críticos (SIN anuncios)
    private val firebaseReady = AtomicBoolean(false)
    private val dataReady = AtomicBoolean(false)
    private val layoutsReady = AtomicBoolean(false)

    // Anuncios se cargan en paralelo (NO bloquean navegación)
    private var adsStartedLoading = false

    // Variables globales para datos precargados
    companion object {
        @JvmStatic var preloadedDatabase: DatabaseReference? = null
        @JvmStatic var preloadedAuth: FirebaseAuth? = null
        @JvmStatic var preloadedRewardedAd: RewardedAd? = null
        @JvmStatic var preloadedInterstitialAd: InterstitialAd? = null
        @JvmStatic var isAdMobInitialized = false

        // Estado de anuncios (se actualiza cuando estén listos)
        @JvmStatic var rewardedAdReady = AtomicBoolean(false)
        @JvmStatic var interstitialAdReady = AtomicBoolean(false)

        // Datos del usuario
        @JvmStatic var preloadedPlayerId = ""
        @JvmStatic var preloadedSpins = 0
        @JvmStatic var preloadedTickets = 0L
        @JvmStatic var preloadedPasses = 0L
        @JvmStatic var consentAccepted = false
        @JvmStatic var welcomeShown = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            binding = ActivitySplashBinding.inflate(layoutInflater)
            setContentView(binding.root)

            Log.d("SPLASH_PROGRESSIVE", "🎬 SPLASH - Carga progresiva de anuncios")

            setupBackPressedHandler()
            startAllAnimations()

            // Cargar componentes críticos inmediatamente
            loadCriticalComponentsFirst()

            // Después de 1 segundo, iniciar carga de anuncios en background
            Handler(Looper.getMainLooper()).postDelayed({
                startProgressiveAdLoading()
            }, 1000)

            // Navegar cuando componentes críticos estén listos (sin esperar anuncios)
            startNavigationWhenCriticalReady()

        } catch (e: Exception) {
            Log.e("SPLASH_PROGRESSIVE", "❌ Error: ${e.message}")
            navigateToMainActivity()
        }
    }

    private fun loadCriticalComponentsFirst() {
        // 1. Datos locales (inmediato)
        loadLocalData()

        // 2. Firebase (crítico)
        loadFirebaseComponents()

        // 3. Layouts (rápido)
        loadLayoutsInBackground()
    }

    private fun loadLocalData() {
        Thread {
            try {
                Log.d("SPLASH_PROGRESSIVE", "📱 Cargando datos locales...")

                SessionManager.init(this@SplashActivity)

                val prefs = getSharedPreferences("DiamantesProPrefs", Context.MODE_PRIVATE)
                consentAccepted = prefs.getBoolean("CONSENT_ACCEPTED", false)
                welcomeShown = prefs.getBoolean("HAS_SHOWN_WELCOME", false)

                preloadedPlayerId = SessionManager.getPlayerId(this@SplashActivity)
                preloadedSpins = SessionManager.getCurrentSpins(this@SplashActivity)

                dataReady.set(true)
                Log.d("SPLASH_PROGRESSIVE", "✅ Datos locales listos: ID=$preloadedPlayerId, Spins=$preloadedSpins")

            } catch (e: Exception) {
                Log.e("SPLASH_PROGRESSIVE", "Error datos locales: ${e.message}")
                dataReady.set(true) // Marcar como listo aunque falle
            }
        }.start()
    }

    private fun loadFirebaseComponents() {
        Thread {
            try {
                Log.d("SPLASH_PROGRESSIVE", "🔥 Cargando Firebase...")

                FirebaseApp.initializeApp(this@SplashActivity)
                preloadedDatabase = FirebaseDatabase.getInstance(
                    "https://diamantes-pro-players-go-f3510-default-rtdb.firebaseio.com/"
                ).reference
                preloadedAuth = Firebase.auth

                // Auth anónima
                preloadedAuth?.signInAnonymously()

                // Si hay player ID, cargar datos rápidamente
                if (preloadedPlayerId.isNotEmpty()) {
                    try {
                        val playerRef = preloadedDatabase?.child("players")?.child(preloadedPlayerId)
                        playerRef?.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                if (snapshot.exists()) {
                                    preloadedTickets = snapshot.child("tickets").getValue(Long::class.java) ?: 0L
                                    preloadedPasses = snapshot.child("passes").getValue(Long::class.java) ?: 0L
                                    Log.d("SPLASH_PROGRESSIVE", "✅ Player data: $preloadedTickets tickets, $preloadedPasses passes")
                                }
                            }
                            override fun onCancelled(error: DatabaseError) {
                                Log.w("SPLASH_PROGRESSIVE", "Player data cancelado: ${error.message}")
                            }
                        })
                    } catch (e: Exception) {
                        Log.w("SPLASH_PROGRESSIVE", "Error cargando player data: ${e.message}")
                    }
                }

                firebaseReady.set(true)
                Log.d("SPLASH_PROGRESSIVE", "✅ Firebase listo")

            } catch (e: Exception) {
                Log.e("SPLASH_PROGRESSIVE", "Error Firebase: ${e.message}")
                firebaseReady.set(true) // Marcar como listo para no bloquear
            }
        }.start()
    }

    private fun loadLayoutsInBackground() {
        Thread {
            try {
                Log.d("SPLASH_PROGRESSIVE", "🎨 Precargando layouts...")

                runOnUiThread {
                    try {
                        layoutInflater.inflate(R.layout.activity_main, null)
                        layoutInflater.inflate(R.layout.section_home, null)
                        layoutInflater.inflate(R.layout.item_grid, null)
                        layoutInflater.inflate(R.layout.modal_consentimiento, null)
                        layoutInflater.inflate(R.layout.modal_welcome, null)

                        layoutsReady.set(true)
                        Log.d("SPLASH_PROGRESSIVE", "✅ Layouts listos")

                    } catch (e: Exception) {
                        Log.w("SPLASH_PROGRESSIVE", "Error layouts: ${e.message}")
                        layoutsReady.set(true)
                    }
                }

            } catch (e: Exception) {
                Log.e("SPLASH_PROGRESSIVE", "Error preload layouts: ${e.message}")
                layoutsReady.set(true)
            }
        }.start()
    }

    private fun startProgressiveAdLoading() {
        if (adsStartedLoading) return
        adsStartedLoading = true

        Thread {
            try {
                Log.d("SPLASH_PROGRESSIVE", "📺 INICIANDO carga progresiva de anuncios...")

                runOnUiThread {
                    // Inicializar AdMob
                    MobileAds.initialize(this@SplashActivity) {
                        isAdMobInitialized = true
                        Log.d("SPLASH_PROGRESSIVE", "✅ AdMob inicializado")

                        // Cargar rewarded ad en background
                        loadRewardedAdInBackground()

                        // Cargar interstitial ad en background (con delay)
                        Handler(Looper.getMainLooper()).postDelayed({
                            loadInterstitialAdInBackground()
                        }, 500)
                    }
                }

            } catch (e: Exception) {
                Log.e("SPLASH_PROGRESSIVE", "Error iniciando AdMob: ${e.message}")
            }
        }.start()
    }

    private fun loadRewardedAdInBackground() {
        try {
            RewardedAd.load(
                this@SplashActivity,
                "ca-app-pub-3940256099942544/5224354917",
                AdRequest.Builder().build(),
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        preloadedRewardedAd = ad
                        rewardedAdReady.set(true)
                        Log.d("SPLASH_PROGRESSIVE", "✅ Rewarded Ad listo")
                    }
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.w("SPLASH_PROGRESSIVE", "Rewarded Ad falló: ${error.message}")
                        rewardedAdReady.set(true) // Marcar como "procesado"
                    }
                }
            )
        } catch (e: Exception) {
            Log.e("SPLASH_PROGRESSIVE", "Error rewarded ad: ${e.message}")
        }
    }

    private fun loadInterstitialAdInBackground() {
        try {
            InterstitialAd.load(
                this@SplashActivity,
                "ca-app-pub-3940256099942544/1033173712",
                AdRequest.Builder().build(),
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        preloadedInterstitialAd = ad
                        interstitialAdReady.set(true)
                        Log.d("SPLASH_PROGRESSIVE", "✅ Interstitial Ad listo")
                    }
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.w("SPLASH_PROGRESSIVE", "Interstitial Ad falló: ${error.message}")
                        interstitialAdReady.set(true) // Marcar como "procesado"
                    }
                }
            )
        } catch (e: Exception) {
            Log.e("SPLASH_PROGRESSIVE", "Error interstitial ad: ${e.message}")
        }
    }

    private fun startNavigationWhenCriticalReady() {
        Thread {
            try {
                val startTime = System.currentTimeMillis()
                val endTime = startTime + TOTAL_SPLASH_TIME

                while (!hasNavigated && System.currentTimeMillis() < endTime + 2000) { // Max 7 segundos
                    val currentTime = System.currentTimeMillis()

                    // Verificar si componentes críticos están listos
                    val criticalReady = firebaseReady.get() && dataReady.get() && layoutsReady.get()
                    val animationFinished = currentTime >= endTime

                    if (criticalReady && animationFinished) {
                        Log.d("SPLASH_PROGRESSIVE", "🚀 Componentes críticos listos - NAVEGANDO")
                        runOnUiThread { navigateToMainActivity() }
                        break
                    } else if (criticalReady) {
                        val remaining = endTime - currentTime
                        Log.d("SPLASH_PROGRESSIVE", "⏰ Críticos listos, esperando animación: ${remaining}ms")
                    } else if (animationFinished) {
                        Log.d("SPLASH_PROGRESSIVE", "⏰ Animación lista, esperando críticos...")
                    }

                    Thread.sleep(100)
                }

                // Timeout de seguridad
                if (!hasNavigated) {
                    Log.w("SPLASH_PROGRESSIVE", "⚠️ TIMEOUT - Navegando ahora")
                    runOnUiThread { navigateToMainActivity() }
                }

            } catch (e: Exception) {
                Log.e("SPLASH_PROGRESSIVE", "Error en navegación: ${e.message}")
                runOnUiThread { navigateToMainActivity() }
            }
        }.start()
    }

    private fun setupBackPressedHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {}
        })
    }

    private fun startAllAnimations() {
        startDiamondRotation()
        startGlowPulse()
        startTitleAnimation()
        startSubtitleAnimation()
        startLoadingAnimation()
        startProgressBarAnimation()
    }

    private fun startDiamondRotation() {
        val rotationAnimator = ObjectAnimator.ofFloat(binding.diamond, "rotation", 45f, 405f)
        rotationAnimator.duration = 2000
        rotationAnimator.repeatCount = ValueAnimator.INFINITE
        rotationAnimator.interpolator = AccelerateDecelerateInterpolator()
        rotationAnimator.start()

        val scaleAnimator = ObjectAnimator.ofFloat(binding.diamond, "scaleX", 1.0f, 1.1f, 1.0f)
        val scaleAnimatorY = ObjectAnimator.ofFloat(binding.diamond, "scaleY", 1.0f, 1.1f, 1.0f)
        scaleAnimator.duration = 2000
        scaleAnimatorY.duration = 2000
        scaleAnimator.repeatCount = ValueAnimator.INFINITE
        scaleAnimatorY.repeatCount = ValueAnimator.INFINITE
        scaleAnimator.start()
        scaleAnimatorY.start()
    }

    private fun startGlowPulse() {
        val glowScaleX = ObjectAnimator.ofFloat(binding.diamondGlow, "scaleX", 1.0f, 1.2f, 1.0f)
        val glowScaleY = ObjectAnimator.ofFloat(binding.diamondGlow, "scaleY", 1.0f, 1.2f, 1.0f)
        val glowAlpha = ObjectAnimator.ofFloat(binding.diamondGlow, "alpha", 0.7f, 0.3f, 0.7f)

        glowScaleX.duration = 2000
        glowScaleY.duration = 2000
        glowAlpha.duration = 2000

        glowScaleX.repeatCount = ValueAnimator.INFINITE
        glowScaleY.repeatCount = ValueAnimator.INFINITE
        glowAlpha.repeatCount = ValueAnimator.INFINITE

        glowScaleX.start()
        glowScaleY.start()
        glowAlpha.start()
    }

    private fun startTitleAnimation() {
        binding.appTitle.translationY = 30f
        binding.appTitle.alpha = 0f
        binding.appTitle.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(1000)
            .setStartDelay(500)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun startSubtitleAnimation() {
        binding.appSubtitle.translationY = 30f
        binding.appSubtitle.alpha = 0f
        binding.appSubtitle.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(1000)
            .setStartDelay(800)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun startLoadingAnimation() {
        binding.loadingContainer.translationY = 30f
        binding.loadingContainer.alpha = 0f
        binding.loadingContainer.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(1000)
            .setStartDelay(1100)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun startProgressBarAnimation() {
        Handler(Looper.getMainLooper()).postDelayed({
            val progressAnimator = ObjectAnimator.ofInt(binding.progressBar, "progress", 0, 100)
            progressAnimator.duration = 3500
            progressAnimator.interpolator = AccelerateDecelerateInterpolator()
            progressAnimator.start()
        }, 800)
    }

    private fun navigateToMainActivity() {
        if (hasNavigated) return
        hasNavigated = true

        try {
            Log.d("SPLASH_PROGRESSIVE", "🏠 NAVEGANDO - Anuncios se cargan en background")

            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("COMPONENTS_READY", true)
                putExtra("ADS_LOADING", adsStartedLoading)
            }

            startActivity(intent)
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)

        } catch (e: Exception) {
            Log.e("SPLASH_PROGRESSIVE", "❌ Error navegando: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val adsReady = rewardedAdReady.get() && interstitialAdReady.get()
        Log.d("SPLASH_PROGRESSIVE", "🏁 Splash destruido - Anuncios listos: $adsReady")
    }
}