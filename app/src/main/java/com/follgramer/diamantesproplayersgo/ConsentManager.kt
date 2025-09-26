package com.follgramer.diamantesproplayersgo

import android.app.Activity
import android.util.Log
import com.google.android.ump.ConsentForm
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

object ConsentManager {
    private const val TAG = "ConsentManager"
    private var isFormShowing = false
    private var consentForm: ConsentForm? = null

    fun run(activity: Activity, onComplete: () -> Unit) {
        try {
            val consentInformation = UserMessagingPlatform.getConsentInformation(activity)

            // Configurar parámetros - NO usar debugSettings en producción
            val params = ConsentRequestParameters.Builder()
                .setTagForUnderAgeOfConsent(false)
                .build()

            Log.d(TAG, "Estado inicial del consentimiento: ${consentInformation.consentStatus}")
            Log.d(TAG, "¿Puede solicitar anuncios?: ${consentInformation.canRequestAds()}")

            consentInformation.requestConsentInfoUpdate(
                activity,
                params,
                {
                    // Información actualizada exitosamente
                    Log.d(TAG, "Info actualizada. Estado: ${consentInformation.consentStatus}")
                    Log.d(TAG, "¿Formulario disponible?: ${consentInformation.isConsentFormAvailable}")

                    when (consentInformation.consentStatus) {
                        ConsentInformation.ConsentStatus.OBTAINED -> {
                            Log.d(TAG, "✅ Consentimiento ya obtenido")
                            onComplete()
                        }
                        ConsentInformation.ConsentStatus.REQUIRED -> {
                            if (consentInformation.isConsentFormAvailable) {
                                Log.d(TAG, "📝 Se requiere consentimiento, mostrando formulario")
                                loadAndShowForm(activity, consentInformation, onComplete)
                            } else {
                                Log.w(TAG, "⚠️ Se requiere consentimiento pero no hay formulario disponible")
                                onComplete()
                            }
                        }
                        ConsentInformation.ConsentStatus.NOT_REQUIRED -> {
                            Log.d(TAG, "✅ No se requiere consentimiento (fuera de EEA/UK)")
                            onComplete()
                        }
                        ConsentInformation.ConsentStatus.UNKNOWN -> {
                            Log.d(TAG, "❓ Estado de consentimiento desconocido")
                            onComplete()
                        }
                        else -> {
                            Log.d(TAG, "Estado no manejado: ${consentInformation.consentStatus}")
                            onComplete()
                        }
                    }
                },
                { error ->
                    Log.e(TAG, "❌ Error obteniendo info de consentimiento: ${error.message}")
                    Log.e(TAG, "Código de error: ${error.errorCode}")
                    // Continuar aunque falle
                    onComplete()
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error crítico en ConsentManager: ${e.message}", e)
            onComplete()
        }
    }

    private fun loadAndShowForm(
        activity: Activity,
        consentInformation: ConsentInformation,
        onComplete: () -> Unit
    ) {
        if (isFormShowing) {
            Log.d(TAG, "⚠️ Formulario ya está mostrándose, ignorando solicitud duplicada")
            return
        }

        UserMessagingPlatform.loadConsentForm(
            activity,
            { form ->
                consentForm = form
                Log.d(TAG, "✅ Formulario cargado exitosamente")

                if (!activity.isFinishing && !activity.isDestroyed) {
                    showFormSafely(activity, form, consentInformation, onComplete)
                } else {
                    Log.w(TAG, "⚠️ Activity no válida para mostrar formulario")
                    onComplete()
                }
            },
            { error ->
                Log.e(TAG, "❌ Error cargando formulario: ${error.message}")
                Log.e(TAG, "Código de error: ${error.errorCode}")
                onComplete()
            }
        )
    }

    private fun showFormSafely(
        activity: Activity,
        form: ConsentForm,
        consentInformation: ConsentInformation,
        onComplete: () -> Unit
    ) {
        try {
            isFormShowing = true
            Log.d(TAG, "🎯 Mostrando formulario de consentimiento al usuario...")

            form.show(activity) { formError ->
                isFormShowing = false
                consentForm = null

                if (formError != null) {
                    Log.e(TAG, "❌ Error mostrando formulario: ${formError.message}")
                    Log.e(TAG, "Código de error: ${formError.errorCode}")
                } else {
                    Log.d(TAG, "✅ Formulario completado por el usuario")
                    Log.d(TAG, "Nuevo estado: ${consentInformation.consentStatus}")
                    Log.d(TAG, "¿Puede solicitar anuncios ahora?: ${consentInformation.canRequestAds()}")
                }

                // IMPORTANTE: Siempre llamar onComplete después de que el usuario interactúe
                onComplete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error crítico mostrando formulario: ${e.message}", e)
            isFormShowing = false
            consentForm = null
            onComplete()
        }
    }

    fun canRequestAds(activity: Activity): Boolean {
        return try {
            val consentInfo = UserMessagingPlatform.getConsentInformation(activity)
            val canRequest = consentInfo.canRequestAds()
            Log.d(TAG, "¿Puede solicitar anuncios?: $canRequest")
            canRequest
        } catch (e: Exception) {
            Log.e(TAG, "Error verificando estado de consentimiento: ${e.message}")
            false
        }
    }

    fun resetConsentForTesting(activity: Activity) {
        if (BuildConfig.DEBUG) {
            try {
                val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
                consentInformation.reset()
                Log.d(TAG, "✅ Consentimiento reseteado para testing")
            } catch (e: Exception) {
                Log.e(TAG, "Error reseteando consentimiento: ${e.message}")
            }
        }
    }

    fun getConsentStatus(activity: Activity): String {
        return try {
            val consentInfo = UserMessagingPlatform.getConsentInformation(activity)
            when (consentInfo.consentStatus) {
                ConsentInformation.ConsentStatus.UNKNOWN -> "UNKNOWN"
                ConsentInformation.ConsentStatus.NOT_REQUIRED -> "NOT_REQUIRED"
                ConsentInformation.ConsentStatus.REQUIRED -> "REQUIRED"
                ConsentInformation.ConsentStatus.OBTAINED -> "OBTAINED"
                else -> "UNKNOWN"
            }
        } catch (e: Exception) {
            "ERROR"
        }
    }
}