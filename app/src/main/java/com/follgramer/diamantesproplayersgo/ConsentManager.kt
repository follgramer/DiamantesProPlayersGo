package com.follgramer.diamantesproplayersgo

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.google.android.ump.*

object ConsentManager {

    fun checkConsent(activity: Activity, onConsentResult: () -> Unit) {
        val params = ConsentRequestParameters.Builder().build()
        val consentInformation = UserMessagingPlatform.getConsentInformation(activity)

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                if (consentInformation.isConsentFormAvailable) {
                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(
                        activity
                    ) { loadAndShowError ->
                        if (loadAndShowError != null) {
                            Toast.makeText(activity, "Error cargando consentimiento", Toast.LENGTH_SHORT).show()
                        }
                        if (consentInformation.canRequestAds()) {
                            onConsentResult()
                        }
                    }
                } else {
                    onConsentResult()
                }
            },
            {
                Toast.makeText(activity, "No se pudo cargar consentimiento", Toast.LENGTH_SHORT).show()
                onConsentResult()
            }
        )
    }
}
