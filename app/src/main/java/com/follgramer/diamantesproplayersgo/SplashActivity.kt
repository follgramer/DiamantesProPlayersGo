package com.follgramer.diamantesproplayersgo

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.follgramer.diamantesproplayersgo.ads.AdsInit
import com.follgramer.diamantesproplayersgo.util.AnalyticsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SplashActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SplashActivity"
        private const val MIN_SPLASH_TIME = 3000L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Pantalla completa sin barras
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT

        setContentView(R.layout.activity_splash)

        startAnimations()
        initializeApp()
    }

    private fun startAnimations() {
        val diamond = findViewById<ImageView>(R.id.diamond)
        val title = findViewById<TextView>(R.id.appTitle)
        val subtitle = findViewById<TextView>(R.id.appSubtitle)
        val loadingContainer = findViewById<View>(R.id.loadingContainer)

        // Logo: Scale + Fade In + Rotation
        diamond.alpha = 0f
        diamond.scaleX = 0.3f
        diamond.scaleY = 0.3f
        diamond.rotation = -180f

        val diamondScaleX = ObjectAnimator.ofFloat(diamond, "scaleX", 0.3f, 1.1f, 1f)
        val diamondScaleY = ObjectAnimator.ofFloat(diamond, "scaleY", 0.3f, 1.1f, 1f)
        val diamondAlpha = ObjectAnimator.ofFloat(diamond, "alpha", 0f, 1f)
        val diamondRotation = ObjectAnimator.ofFloat(diamond, "rotation", -180f, 0f)

        val logoAnim = AnimatorSet().apply {
            playTogether(diamondScaleX, diamondScaleY, diamondAlpha, diamondRotation)
            duration = 1000
            interpolator = AccelerateDecelerateInterpolator()
        }

        // Título: Slide Up + Fade
        title.alpha = 0f
        title.translationY = 100f

        val titleTranslation = ObjectAnimator.ofFloat(title, "translationY", 100f, 0f)
        val titleAlpha = ObjectAnimator.ofFloat(title, "alpha", 0f, 1f)

        val titleAnim = AnimatorSet().apply {
            playTogether(titleTranslation, titleAlpha)
            duration = 600
            startDelay = 500
        }

        // Subtítulo: Fade
        subtitle.alpha = 0f
        val subtitleAlpha = ObjectAnimator.ofFloat(subtitle, "alpha", 0f, 1f).apply {
            duration = 500
            startDelay = 900
        }

        // Loading: Fade
        loadingContainer.alpha = 0f
        val loadingAlpha = ObjectAnimator.ofFloat(loadingContainer, "alpha", 0f, 1f).apply {
            duration = 400
            startDelay = 1300
        }

        // Ejecutar secuencia
        AnimatorSet().apply {
            playSequentially(logoAnim, titleAnim, subtitleAlpha, loadingAlpha)
            start()
        }
    }

    private fun initializeApp() {
        val startTime = System.currentTimeMillis()

        lifecycleScope.launch {
            try {
                // ✅ Inicializar Analytics primero
                withContext(Dispatchers.Main) {
                    AnalyticsManager.initialize(this@SplashActivity)
                    AnalyticsManager.logAppOpened()
                }

                // ✅ Manejar consentimiento
                val consentGranted = ConsentManager.initialize(this@SplashActivity)
                Log.d(TAG, "Consentimiento: $consentGranted")

                if (consentGranted) {
                    initializeAdMob()
                } else {
                    Log.w(TAG, "Sin consentimiento, omitiendo AdMob")
                }

                // ✅ Esperar tiempo mínimo
                val elapsedTime = System.currentTimeMillis() - startTime
                val remainingTime = (MIN_SPLASH_TIME - elapsedTime).coerceAtLeast(0)

                if (remainingTime > 0) {
                    delay(remainingTime)
                }

                goToMainActivity()

            } catch (e: Exception) {
                Log.e(TAG, "Error en inicialización: ${e.message}", e)
                // ✅ Registrar error crítico en Analytics
                AnalyticsManager.logCriticalError("splash_init_error", e.stackTraceToString())
                goToMainActivity()
            }
        }
    }

    private suspend fun initializeAdMob() {
        Log.d(TAG, "Iniciando AdMob...")

        // ✅ Inicializar AdMob de forma síncrona
        withContext(Dispatchers.Main) {
            AdsInit.init(this@SplashActivity)
        }

        // ✅ Esperar a que AdMob esté listo
        var attempts = 0
        while (!AdsInit.isAdMobReady() && attempts < 20) {
            delay(100)
            attempts++
        }

        if (AdsInit.isAdMobReady()) {
            Log.d(TAG, "✅ AdMob inicializado correctamente")
        } else {
            Log.w(TAG, "⚠️ AdMob no se inicializó completamente")
            AnalyticsManager.logError("admob_init_timeout", "AdMob initialization timeout")
        }

        delay(500)
    }

    private fun goToMainActivity() {
        Log.d(TAG, "Yendo a MainActivity")

        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("admob_initialized", AdsInit.isAdMobReady())
            putExtra("consent_completed", ConsentManager.isInitialized())
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "SplashActivity destroyed")
    }
}// Updated: 2025-10-15 14:29:27
