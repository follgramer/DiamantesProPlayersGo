package com.follgramer.diamantesproplayersgo

import android.app.Activity
import android.util.Log
import android.webkit.WebSettings
import android.webkit.WebView
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentForm
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

object ConsentManager {
    private const val TAG = "ConsentManager"

    fun run(activity: Activity, onComplete: () -> Unit) {
        try {
            configureWebViewSecurity()

            val consentInformation = UserMessagingPlatform.getConsentInformation(activity)

            // Solo usar debug settings en DEBUG builds
            val debugSettings = if (BuildConfig.DEBUG) {
                ConsentDebugSettings.Builder(activity)
                    .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                    .addTestDeviceHashedId("B3EEABB8EE11C2BE770B684D95219ECB")
                    .addTestDeviceHashedId("72F928A6866D6BC25A62E7A6F4AFC402")
                    .build()
            } else null

            val params = ConsentRequestParameters.Builder().apply {
                if (debugSettings != null) {
                    setConsentDebugSettings(debugSettings)
                }
                setTagForUnderAgeOfConsent(false)
            }.build()

            consentInformation.requestConsentInfoUpdate(
                activity,
                params,
                {
                    // Consentimiento actualizado exitosamente
                    if (consentInformation.isConsentFormAvailable) {
                        Log.d(TAG, "Formulario de consentimiento disponible")
                        loadForm(activity, consentInformation, onComplete)
                    } else {
                        Log.d(TAG, "No hay formulario de consentimiento disponible")
                        onComplete()
                    }
                },
                { error ->
                    Log.e(TAG, "Error actualizando información de consentimiento: ${error.message}")
                    // Continuar aunque falle el consentimiento
                    onComplete()
                }
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error en ConsentManager: ${e.message}")
            onComplete()
        }
    }

    private fun configureWebViewSecurity() {
        try {
            // Solo habilitar debugging en DEBUG builds
            WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
            Log.d(TAG, "WebView security configurado")
        } catch (e: Exception) {
            Log.e(TAG, "Error configurando seguridad WebView: ${e.message}")
        }
    }

    private fun loadForm(
        activity: Activity,
        consentInformation: ConsentInformation,
        onComplete: () -> Unit
    ) {
        UserMessagingPlatform.loadConsentForm(
            activity,
            { consentForm ->
                when (consentInformation.consentStatus) {
                    ConsentInformation.ConsentStatus.REQUIRED -> {
                        Log.d(TAG, "Consentimiento requerido, mostrando formulario")
                        showForm(activity, consentForm, onComplete)
                    }
                    ConsentInformation.ConsentStatus.NOT_REQUIRED -> {
                        Log.d(TAG, "Consentimiento no requerido")
                        onComplete()
                    }
                    ConsentInformation.ConsentStatus.OBTAINED -> {
                        Log.d(TAG, "Consentimiento ya obtenido")
                        onComplete()
                    }
                    else -> {
                        Log.d(TAG, "Estado de consentimiento: ${consentInformation.consentStatus}")
                        onComplete()
                    }
                }
            },
            { error ->
                Log.e(TAG, "Error cargando formulario: ${error.message}")
                onComplete()
            }
        )
    }

    private fun showForm(
        activity: Activity,
        consentForm: ConsentForm,
        onComplete: () -> Unit
    ) {
        consentForm.show(activity) { error ->
            if (error != null) {
                Log.e(TAG, "Error mostrando formulario: ${error.message}")
            } else {
                Log.d(TAG, "Formulario de consentimiento completado")
            }
            onComplete()
        }
    }
}