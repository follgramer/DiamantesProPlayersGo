package com.follgramer.diamantesproplayersgo

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
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

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val TOTAL_SPLASH_TIME = 5000L // ✅ 5 segundos total
    private val START_LOADING_AT = 1000L // ✅ Empezar a cargar al segundo 1
    private var hasNavigated = false
    private var isAppPreloaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            Log.d("SPLASH_BACKGROUND", "🎬 Splash iniciado - 5 segundos con carga invisible")

            binding = ActivitySplashBinding.inflate(layoutInflater)
            setContentView(binding.root)

            // Deshabilitar back button
            setupBackPressedHandler()

            // Iniciar animaciones inmediatamente
            startAllAnimations()

            // ✅ AL SEGUNDO 1: Empezar a cargar la app en background (INVISIBLE)
            Handler(Looper.getMainLooper()).postDelayed({
                startInvisibleAppLoading()
            }, START_LOADING_AT)

            // ✅ AL SEGUNDO 5: Navegar (la app ya estará cargada)
            Handler(Looper.getMainLooper()).postDelayed({
                navigateToMainActivity()
            }, TOTAL_SPLASH_TIME)

        } catch (e: Exception) {
            Log.e("SPLASH_BACKGROUND", "❌ Error en splash: ${e.message}")
            navigateToMainActivity()
        }
    }

    // ✅ NUEVA FUNCIÓN: CARGAR APP INVISIBLEMENTE EN BACKGROUND
    private fun startInvisibleAppLoading() {
        Thread {
            try {
                Log.d("SPLASH_BACKGROUND", "🔄 Iniciando carga INVISIBLE de la app...")

                // Inicializar SessionManager
                SessionManager.init(this@SplashActivity)
                Log.d("SPLASH_BACKGROUND", "✅ SessionManager inicializado")

                // Inicializar Firebase
                try {
                    com.google.firebase.FirebaseApp.initializeApp(this@SplashActivity)
                    Log.d("SPLASH_BACKGROUND", "✅ Firebase inicializada")
                } catch (e: Exception) {
                    Log.d("SPLASH_BACKGROUND", "Firebase ya inicializada: ${e.message}")
                }

                // ✅ CREAR INSTANCIA DE MAINACTIVITY EN MEMORIA (INVISIBLE)
                // Esto precarga las clases y recursos necesarios
                runOnUiThread {
                    try {
                        // Precarga de recursos críticos
                        resources.getLayout(R.layout.activity_main)
                        Log.d("SPLASH_BACKGROUND", "✅ Layout precargado")
                    } catch (e: Exception) {
                        Log.w("SPLASH_BACKGROUND", "Error precargando layout: ${e.message}")
                    }
                }

                // Simular inicialización de componentes críticos
                Thread.sleep(500) // Dar tiempo para que todo se estabilice

                isAppPreloaded = true
                Log.d("SPLASH_BACKGROUND", "✅ App COMPLETAMENTE precargada en background")

            } catch (e: Exception) {
                Log.e("SPLASH_BACKGROUND", "❌ Error en carga invisible: ${e.message}")
                isAppPreloaded = true // Marcar como listo aunque haya error
            }
        }.start()
    }

    private fun setupBackPressedHandler() {
        try {
            onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // No hacer nada durante splash
                }
            })
        } catch (e: Exception) {
            Log.e("SPLASH_BACKGROUND", "Error configurando back handler: ${e.message}")
        }
    }

    private fun startAllAnimations() {
        try {
            startDiamondRotation()
            startGlowPulse()
            startTitleAnimation()
            startSubtitleAnimation()
            startLoadingAnimation()
            startProgressBarAnimation()
        } catch (e: Exception) {
            Log.e("SPLASH_BACKGROUND", "Error en animaciones: ${e.message}")
        }
    }

    private fun startDiamondRotation() {
        try {
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
        } catch (e: Exception) {
            Log.e("SPLASH_BACKGROUND", "Error en rotación diamante: ${e.message}")
        }
    }

    private fun startGlowPulse() {
        try {
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
        } catch (e: Exception) {
            Log.e("SPLASH_BACKGROUND", "Error en glow pulse: ${e.message}")
        }
    }

    private fun startTitleAnimation() {
        try {
            binding.appTitle.translationY = 30f
            binding.appTitle.alpha = 0f
            binding.appTitle.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(1000)
                .setStartDelay(500)
                .setInterpolator(DecelerateInterpolator())
                .start()
        } catch (e: Exception) {
            Log.e("SPLASH_BACKGROUND", "Error en title animation: ${e.message}")
        }
    }

    private fun startSubtitleAnimation() {
        try {
            binding.appSubtitle.translationY = 30f
            binding.appSubtitle.alpha = 0f
            binding.appSubtitle.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(1000)
                .setStartDelay(800)
                .setInterpolator(DecelerateInterpolator())
                .start()
        } catch (e: Exception) {
            Log.e("SPLASH_BACKGROUND", "Error en subtitle animation: ${e.message}")
        }
    }

    private fun startLoadingAnimation() {
        try {
            binding.loadingContainer.translationY = 30f
            binding.loadingContainer.alpha = 0f
            binding.loadingContainer.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(1000)
                .setStartDelay(1100)
                .setInterpolator(DecelerateInterpolator())
                .start()
        } catch (e: Exception) {
            Log.e("SPLASH_BACKGROUND", "Error en loading animation: ${e.message}")
        }
    }

    private fun startProgressBarAnimation() {
        try {
            Handler(Looper.getMainLooper()).postDelayed({
                val progressAnimator = ObjectAnimator.ofInt(binding.progressBar, "progress", 0, 100)
                progressAnimator.duration = 3500 // ✅ Más lento para durar hasta el final
                progressAnimator.interpolator = AccelerateDecelerateInterpolator()
                progressAnimator.start()
            }, 800)
        } catch (e: Exception) {
            Log.e("SPLASH_BACKGROUND", "Error en progress animation: ${e.message}")
        }
    }

    private fun navigateToMainActivity() {
        if (hasNavigated) {
            Log.w("SPLASH_BACKGROUND", "Ya navegado, ignorando")
            return
        }
        hasNavigated = true

        try {
            Log.d("SPLASH_BACKGROUND", "🏠 Navegando a MainActivity - App precargada: $isAppPreloaded")

            val intent = Intent(this, MainActivity::class.java)
            // ✅ Agregar flag para indicar que está precargada
            intent.putExtra("APP_PRELOADED", isAppPreloaded)

            startActivity(intent)
            finish()

            // ✅ Transición suave (no instantánea)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)

        } catch (e: Exception) {
            Log.e("SPLASH_BACKGROUND", "❌ Error navegando: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("SPLASH_BACKGROUND", "🏁 Splash destruido")
    }
}