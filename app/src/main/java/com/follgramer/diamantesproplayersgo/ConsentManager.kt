package com.follgramer.diamantesproplayersgo

import android.app.Activity
import android.util.Log
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentForm
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object ConsentManager {
    private const val TAG = "ConsentManager"
    private var consentInformation: ConsentInformation? = null
    private var consentForm: ConsentForm? = null
    private var isInitialized = false

    /**
     * ✅ Verificar si se puede solicitar anuncios
     */
    fun canRequestAds(activity: Activity): Boolean {
        return try {
            getConsentInfo(activity).canRequestAds()
        } catch (e: Exception) {
            Log.e(TAG, "Error verificando consentimiento: ${e.message}")
            false
        }
    }

    /**
     * ✅ Inicializar y manejar consentimiento (suspendible para coroutines)
     */
    suspend fun initialize(activity: Activity): Boolean = suspendCancellableCoroutine { continuation ->
        try {
            val consentInfo = getConsentInfo(activity)

            // ✅ Configurar parámetros
            val params = if (BuildConfig.DEBUG) {
                // Solo para testing en emulador
                val debugSettings = ConsentDebugSettings.Builder(activity)
                    .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                    .addTestDeviceHashedId("TEST-DEVICE-HASHED-ID") // Cambiar por tu device ID
                    .build()

                ConsentRequestParameters.Builder()
                    .setConsentDebugSettings(debugSettings)
                    .setTagForUnderAgeOfConsent(false)
                    .build()
            } else {
                ConsentRequestParameters.Builder()
                    .setTagForUnderAgeOfConsent(false)
                    .build()
            }

            Log.d(TAG, "📋 Estado inicial: ${consentInfo.consentStatus}")

            // ✅ Solicitar actualización de información
            consentInfo.requestConsentInfoUpdate(
                activity,
                params,
                {
                    Log.d(TAG, "✅ Info actualizada. Nuevo estado: ${consentInfo.consentStatus}")

                    when {
                        consentInfo.isConsentFormAvailable &&
                                consentInfo.consentStatus == ConsentInformation.ConsentStatus.REQUIRED -> {
                            Log.d(TAG, "📝 Formulario requerido, cargando...")
                            loadForm(activity, consentInfo) { success ->
                                continuation.resume(success)
                            }
                        }
                        else -> {
                            Log.d(TAG, "✅ Consentimiento no requerido o ya obtenido")
                            isInitialized = true
                            continuation.resume(consentInfo.canRequestAds())
                        }
                    }
                },
                { error ->
                    Log.e(TAG, "❌ Error actualizando info: ${error.message}")
                    continuation.resume(false)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error crítico: ${e.message}", e)
            continuation.resume(false)
        }
    }

    /**
     * ✅ Cargar formulario de consentimiento
     */
    private fun loadForm(
        activity: Activity,
        consentInfo: ConsentInformation,
        callback: (Boolean) -> Unit
    ) {
        UserMessagingPlatform.loadConsentForm(
            activity,
            { form ->
                consentForm = form
                Log.d(TAG, "✅ Formulario cargado")

                if (!activity.isFinishing && !activity.isDestroyed) {
                    showForm(activity, form, consentInfo, callback)
                } else {
                    Log.w(TAG, "⚠️ Activity no válida")
                    callback(false)
                }
            },
            { error ->
                Log.e(TAG, "❌ Error cargando formulario: ${error.message}")
                callback(false)
            }
        )
    }

    /**
     * ✅ Mostrar formulario
     */
    private fun showForm(
        activity: Activity,
        form: ConsentForm,
        consentInfo: ConsentInformation,
        callback: (Boolean) -> Unit
    ) {
        form.show(activity) { error ->
            consentForm = null

            if (error != null) {
                Log.e(TAG, "❌ Error mostrando formulario: ${error.message}")
                callback(false)
            } else {
                Log.d(TAG, "✅ Formulario completado")
                Log.d(TAG, "Estado final: ${consentInfo.consentStatus}")
                isInitialized = true
                callback(consentInfo.canRequestAds())
            }
        }
    }

    /**
     * ✅ Obtener información de consentimiento
     */
    private fun getConsentInfo(activity: Activity): ConsentInformation {
        if (consentInformation == null) {
            consentInformation = UserMessagingPlatform.getConsentInformation(activity)
        }
        return consentInformation!!
    }

    /**
     * ✅ Verificar si está inicializado
     */
    fun isInitialized(): Boolean = isInitialized

    /**
     * ✅ Resetear consentimiento (solo para testing)
     */
    fun resetForTesting(activity: Activity) {
        if (BuildConfig.DEBUG) {
            try {
                getConsentInfo(activity).reset()
                isInitialized = false
                Log.d(TAG, "✅ Consentimiento reseteado")
            } catch (e: Exception) {
                Log.e(TAG, "Error reseteando: ${e.message}")
            }
        }
    }

    /**
     * ✅ Mostrar formulario de preferencias
     */
    fun showPrivacyOptionsForm(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { error ->
            if (error != null) {
                Log.e(TAG, "Error mostrando opciones: ${error.message}")
            }
        }
    }
}