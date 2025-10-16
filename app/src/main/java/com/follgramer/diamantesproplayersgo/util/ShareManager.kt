package com.follgramer.diamantesproplayersgo.util

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.firebase.database.*
import kotlinx.coroutines.tasks.await

object ShareManager {
    private const val TAG = "ShareManager"
    private lateinit var database: DatabaseReference

    fun initialize() {
        database = FirebaseDatabase.getInstance(
            "https://diamantes-pro-players-go-f3510-default-rtdb.firebaseio.com/"
        ).reference
    }

    /**
     * Obtener configuración de compartir desde Firebase
     */
    suspend fun getShareConfig(): ShareConfig {
        return try {
            val snapshot = database.child("appConfig").child("shareConfig").get().await()

            if (snapshot.exists()) {
                ShareConfig(
                    title = snapshot.child("title").getValue(String::class.java)
                        ?: "🎲 Diamantes Pro Players Go",
                    message = snapshot.child("message").getValue(String::class.java)
                        ?: getDefaultMessage(),
                    playStoreUrl = snapshot.child("playStoreUrl").getValue(String::class.java)
                        ?: "https://play.google.com/store/apps/details?id=com.follgramer.diamantesproplayersgo"
                )
            } else {
                ShareConfig()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo configuración: ${e.message}")
            ShareConfig()
        }
    }

    /**
     * Compartir app con configuración personalizada
     */
    suspend fun shareApp(context: Context, referralCode: String? = null) {
        try {
            val config = getShareConfig()
            val shareText = buildShareText(config, referralCode)

            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
                putExtra(Intent.EXTRA_SUBJECT, config.title)
            }

            context.startActivity(Intent.createChooser(shareIntent, "Compartir Diamantes Pro"))

            // Registrar en Analytics
            AnalyticsManager.logFeatureUsed("app_shared")
            if (referralCode != null) {
                AnalyticsManager.logFeatureUsed("referral_shared")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error compartiendo app: ${e.message}")
        }
    }

    /**
     * Construir texto de compartir
     */
    private fun buildShareText(config: ShareConfig, referralCode: String?): String {
        return if (referralCode != null) {
            """
            ${config.message}
            
            🎁 ¡Usa mi código de invitación: $referralCode y obtén bonos!
            
            ${config.playStoreUrl}&referrer=$referralCode
            """.trimIndent()
        } else {
            """
            ${config.message}
            
            ${config.playStoreUrl}
            """.trimIndent()
        }
    }

    /**
     * Mensaje por defecto
     */
    private fun getDefaultMessage(): String {
        return """
            🎲 ¡Únete a Diamantes Pro Players Go!
            
            La mejor app de sorteos semanales con premios increíbles.
            
            💎 Gira la ruleta y gana tickets
            🏆 Participa en sorteos semanales  
            🎁 Premios garantizados cada semana
            
            ¡Descárgala gratis!
        """.trimIndent()
    }

    /**
     * Registrar compartido (para estadísticas)
     */
    suspend fun logShare(playerId: String, type: String) {
        try {
            val shareData = mapOf(
                "playerId" to playerId,
                "type" to type, // "app" o "referral"
                "timestamp" to ServerValue.TIMESTAMP
            )

            database.child("shareStats").push().setValue(shareData)

        } catch (e: Exception) {
            Log.e(TAG, "Error registrando compartido: ${e.message}")
        }
    }

    data class ShareConfig(
        val title: String = "🎲 Diamantes Pro Players Go",
        val message: String = """
            🎲 ¡Únete a Diamantes Pro Players Go!
            
            La mejor app de sorteos semanales con premios increíbles.
            
            💎 Gira la ruleta y gana tickets
            🏆 Participa en sorteos semanales  
            🎁 Premios garantizados cada semana
            
            ¡Descárgala gratis!
        """.trimIndent(),
        val playStoreUrl: String = "https://play.google.com/store/apps/details?id=com.follgramer.diamantesproplayersgo"
    )
}// Updated: 2025-10-15 14:29:27
