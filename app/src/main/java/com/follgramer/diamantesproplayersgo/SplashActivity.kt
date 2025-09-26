package com.follgramer.diamantesproplayersgo

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.follgramer.diamantesproplayersgo.ads.AdsInit
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

class SplashActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SplashActivity"
        private const val MIN_SPLASH_TIME = 2000L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        initializeApp()
    }

    private fun initializeApp() {
        val startTime = System.currentTimeMillis()

        // Manejar consentimiento y AdMob en paralelo
        handleConsentAndAds {
            val elapsedTime = System.currentTimeMillis() - startTime
            val remainingTime = (MIN_SPLASH_TIME - elapsedTime).coerceAtLeast(0)

            Handler(Looper.getMainLooper()).postDelayed({
                goToMainActivity()
            }, remainingTime)
        }
    }

    private fun handleConsentAndAds(onComplete: () -> Unit) {
        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)
            .build()

        val consentInfo = UserMessagingPlatform.getConsentInformation(this)

        consentInfo.requestConsentInfoUpdate(
            this,
            params,
            {
                // Si necesita formulario, mostrarlo
                if (consentInfo.isConsentFormAvailable &&
                    consentInfo.consentStatus == ConsentInformation.ConsentStatus.REQUIRED) {

                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(this) { formError ->
                        if (formError != null) {
                            Log.e(TAG, "Error con formulario: ${formError.message}")
                        }

// Inicializar AdMob después del consentimiento
                        if (consentInfo.canRequestAds()) {
                            AdsInit.init(this)
                            Log.d(TAG, "AdMob inicializado")
                            onComplete()
                        } else {
                            onComplete()
                        }
                    }
                } else {
                    // No necesita formulario, inicializar AdMob directamente
                    if (consentInfo.canRequestAds()) {
                        AdsInit.init(this)
                        Log.d(TAG, "AdMob inicializado")
                        onComplete()
                    } else {
                        onComplete()
                    }
                }
            },
            { error ->
                Log.e(TAG, "Error obteniendo consentimiento: ${error.message}")
                // Intentar inicializar AdMob de todos modos
                AdsInit.init(this)
                onComplete()
            }
        )
    }

    private fun goToMainActivity() {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("admob_initialized", AdsInit.isAdMobReady())
            putExtra("consent_completed", true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        startActivity(intent)
        finish()
    }
}