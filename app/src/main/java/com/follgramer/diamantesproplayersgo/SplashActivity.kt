package com.follgramer.diamantesproplayersgo

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.animation.DecelerateInterpolator
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.follgramer.diamantesproplayersgo.ads.AdsInit
import com.follgramer.diamantesproplayersgo.ads.currentBannerTopUnitId
import com.follgramer.diamantesproplayersgo.ads.currentBannerBottomUnitId
import com.follgramer.diamantesproplayersgo.ads.currentInterstitialUnitId
import com.follgramer.diamantesproplayersgo.ads.currentRewardedUnitId
import com.follgramer.diamantesproplayersgo.databinding.ActivitySplashBinding
import java.util.concurrent.atomic.AtomicBoolean

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private var hasNavigated = AtomicBoolean(false)

    companion object {
        private const val TAG = "SplashActivity"
        private const val MIN_SPLASH_TIME = 2000L
        private const val MAX_SPLASH_TIME = 5000L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            binding = ActivitySplashBinding.inflate(layoutInflater)
            setContentView(binding.root)

            setupBackPressedHandler()
            startElegantFadeInAnimation()

            // Validar configuración de AdMob
            validateAdMobSetup()

            Log.d(TAG, "Splash iniciado. Solicitando consentimiento...")

            val startTime = System.currentTimeMillis()

            // 1. Consentimiento UMP primero
            ConsentManager.run(this) {
                Log.d(TAG, "Proceso de consentimiento finalizado")

                // 2. Inicializar AdMob después del consentimiento
                initializeAdMob {
                    // 3. Asegurar tiempo mínimo de splash
                    val elapsedTime = System.currentTimeMillis() - startTime
                    val remainingTime = maxOf(0, MIN_SPLASH_TIME - elapsedTime)

                    Handler(Looper.getMainLooper()).postDelayed({
                        navigateToMainActivity()
                    }, remainingTime)
                }
            }

            // Failsafe: navegar después del tiempo máximo
            Handler(Looper.getMainLooper()).postDelayed({
                navigateToMainActivity()
            }, MAX_SPLASH_TIME)

        } catch (e: Exception) {
            Log.e(TAG, "Error crítico en onCreate: ${e.message}")
            Handler(Looper.getMainLooper()).postDelayed({
                navigateToMainActivity()
            }, 2000)
        }
    }

    private fun validateAdMobSetup() {
        try {
            val appInfo = packageManager.getApplicationInfo(
                packageName,
                PackageManager.GET_META_DATA
            )
            val manifestAppId = appInfo.metaData?.getString("com.google.android.gms.ads.APPLICATION_ID")

            // Determinar el App ID esperado basado en el BuildConfig
            val expectedAppId = if (BuildConfig.DEBUG) {
                "ca-app-pub-3940256099942544~3347511713" // Test App ID
            } else {
                "ca-app-pub-2024712392092488~7992650364" // Tu App ID de producción
            }

            Log.d(TAG, "=== VALIDACIÓN ADMOB ===")
            Log.d(TAG, "Manifest App ID: $manifestAppId")
            Log.d(TAG, "Expected App ID: $expectedAppId")
            Log.d(TAG, "Build Type: ${if (BuildConfig.DEBUG) "DEBUG" else "RELEASE"}")
            Log.d(TAG, "Unit IDs en uso:")
            Log.d(TAG, "  Banner Top: ${currentBannerTopUnitId()}")
            Log.d(TAG, "  Banner Bottom: ${currentBannerBottomUnitId()}")
            Log.d(TAG, "  Interstitial: ${currentInterstitialUnitId()}")
            Log.d(TAG, "  Rewarded: ${currentRewardedUnitId()}")

            if (manifestAppId != expectedAppId) {
                Log.e(TAG, "❌ WARNING: APPLICATION_ID mismatch!")
                Log.e(TAG, "This may cause AdMob initialization to fail")
            } else {
                Log.d(TAG, "✅ APPLICATION_ID configured correctly")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error validating AdMob setup: ${e.message}")
        }
    }

    private fun initializeAdMob(onComplete: () -> Unit) {
        try {
            Log.d(TAG, "Inicializando AdMob desde SplashActivity...")
            Log.d(TAG, "Using Test Ads: ${BuildConfig.DEBUG}")

            // Inicializar AdMob
            AdsInit.init(applicationContext)

            // Esperar un poco para que se complete la inicialización
            Handler(Looper.getMainLooper()).postDelayed({
                if (AdsInit.isInitialized()) {
                    Log.d(TAG, "✅ AdMob inicializado correctamente")
                    Log.d(TAG, "Modo: ${if (BuildConfig.DEBUG) "DEBUG con anuncios de prueba" else "RELEASE con anuncios reales"}")
                } else {
                    Log.w(TAG, "⚠️ AdMob puede no estar completamente listo")
                }
                onComplete()
            }, 1000)

        } catch (e: Exception) {
            Log.e(TAG, "Error inicializando AdMob: ${e.message}")
            onComplete()
        }
    }

    private fun navigateToMainActivity() {
        if (hasNavigated.getAndSet(true)) {
            Log.d(TAG, "Ya se navegó a MainActivity")
            return
        }

        try {
            Log.d(TAG, "Navegando a MainActivity...")

            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }

            startActivity(intent)
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)

        } catch (e: Exception) {
            Log.e(TAG, "Error navegando a MainActivity: ${e.message}")
            finish()
        }
    }

    private fun startElegantFadeInAnimation() {
        try {
            binding.diamond.apply {
                alpha = 0f
                scaleX = 0.8f
                scaleY = 0.8f
                animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(800)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            }

            binding.appTitle.apply {
                alpha = 0f
                translationY = 30f
                animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(300)
                    .setDuration(600)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            }

            binding.appSubtitle.apply {
                alpha = 0f
                translationY = 20f
                animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(500)
                    .setDuration(600)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en animaciones: ${e.message}")
        }
    }

    private fun setupBackPressedHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Log.d(TAG, "Botón atrás bloqueado durante splash")
            }
        })
    }

    override fun onDestroy() {
        try {
            binding.root.clearAnimation()
        } catch (e: Exception) {
            Log.e(TAG, "Error en onDestroy: ${e.message}")
        }
        super.onDestroy()
    }
}