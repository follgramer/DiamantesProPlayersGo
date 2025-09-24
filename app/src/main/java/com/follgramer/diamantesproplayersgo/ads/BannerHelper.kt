package com.follgramer.diamantesproplayersgo.ads

import android.app.Activity
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.follgramer.diamantesproplayersgo.BuildConfig
import com.google.android.gms.ads.*
import kotlinx.coroutines.*

object BannerHelper {
    private const val TAG = "BannerHelper"
    private val bannersMap = mutableMapOf<Int, AdView>()
    private val loadingStatus = mutableMapOf<Int, Boolean>()
    private val retryScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Contador de intentos de carga
    private val retryCount = mutableMapOf<Int, Int>()
    private const val MAX_RETRY_ATTEMPTS = 3

    fun attachAdaptiveBanner(activity: Activity, container: ViewGroup) {
        try {
            val containerId = container.hashCode()

            Log.d(TAG, "🎯 Iniciando carga de banner para container $containerId")
            Log.d(TAG, "Modo actual: ${if (BuildConfig.DEBUG) "DEBUG" else "RELEASE"}")

            // Limpiar contenedor
            container.removeAllViews()
            container.visibility = View.VISIBLE

            // Verificar si ya existe
            if (bannersMap.containsKey(containerId)) {
                Log.d(TAG, "Banner ya existe para container $containerId")
                return
            }

            // Verificar si AdMob está listo
            if (!AdsInit.isAdMobReady()) {
<<<<<<< HEAD
                Log.w(TAG, "⚠️ AdMob no está listo, programando reintento...")
=======
                Log.w(TAG, "AdMob no está listo, reintentando en 3 segundos...")
>>>>>>> 999fd88ece3337ae0871be7e0514342d32569941
                retryScope.launch {
                    delay(3000)
                    if (!activity.isFinishing && !activity.isDestroyed) {
                        attachAdaptiveBanner(activity, container)
                    }
                }
                return
            }

            // Verificar si ya está cargando
            if (loadingStatus[containerId] == true) {
                Log.d(TAG, "Banner ya está cargando para container $containerId")
                return
            }

            loadingStatus[containerId] = true

<<<<<<< HEAD
            // Obtener información del contenedor
            val containerInfo = getContainerInfo(container)
            Log.d(TAG, "📊 Container info: $containerInfo")

=======
>>>>>>> 999fd88ece3337ae0871be7e0514342d32569941
            // Calcular tamaño adaptativo
            val adWidth = getAdaptiveBannerWidth(activity)
            val adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth)

            Log.d(TAG, "📐 Banner size calculated: ${adSize.width}x${adSize.height}")

            // Crear AdView
            val adView = AdView(activity).apply {
                adUnitId = determineAdUnitId(container)
                setAdSize(adSize)
            }

<<<<<<< HEAD
            Log.d(TAG, "🆔 Using Ad Unit ID: ${adView.adUnitId}")
            Log.d(TAG, "📏 Banner dimensions: ${adSize.width}x${adSize.height}")
=======
            Log.d(TAG, "Cargando banner con ID: ${adView.adUnitId}")
            Log.d(TAG, "Tamaño del banner: ${adSize.width}x${adSize.height}")
>>>>>>> 999fd88ece3337ae0871be7e0514342d32569941

            // Configurar listener
            adView.adListener = object : AdListener() {
                override fun onAdLoaded() {
<<<<<<< HEAD
                    Log.d(TAG, "✅ Banner cargado exitosamente para container $containerId")
=======
                    Log.d(TAG, "✅ Banner cargado exitosamente")
>>>>>>> 999fd88ece3337ae0871be7e0514342d32569941
                    bannersMap[containerId] = adView
                    loadingStatus[containerId] = false
                    retryCount.remove(containerId) // Reset retry count on success

                    activity.runOnUiThread {
                        // Hacer visible el contenedor
                        container.visibility = View.VISIBLE

                        // Establecer altura mínima basada en el tamaño del anuncio
                        val density = activity.resources.displayMetrics.density
                        val minHeight = (adSize.height * density).toInt()
                        container.minimumHeight = minHeight

                        // Asegurar que el layout se actualice
                        container.requestLayout()

                        Log.d(TAG, "📱 Container configurado: altura mínima = ${minHeight}px")
                    }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
<<<<<<< HEAD
                    Log.e(TAG, "❌ Error cargando banner para container $containerId")
                    Log.e(TAG, "   Código de error: ${error.code}")
                    Log.e(TAG, "   Mensaje: ${error.message}")
                    Log.e(TAG, "   Dominio: ${error.domain}")
                    Log.e(TAG, "   Causa: ${error.cause}")

                    loadingStatus[containerId] = false

                    // Manejar reintentos
                    val currentRetries = retryCount.getOrDefault(containerId, 0)
                    if (currentRetries < MAX_RETRY_ATTEMPTS) {
                        retryCount[containerId] = currentRetries + 1
                        val delayTime = when (currentRetries) {
                            0 -> 5000L   // 5 segundos
                            1 -> 15000L  // 15 segundos
                            else -> 30000L // 30 segundos
                        }

                        Log.d(TAG, "🔄 Programando reintento ${currentRetries + 1}/$MAX_RETRY_ATTEMPTS en ${delayTime/1000}s")

                        retryScope.launch {
                            delay(delayTime)
                            if (!activity.isFinishing && !activity.isDestroyed) {
                                attachAdaptiveBanner(activity, container)
                            }
                        }
                    } else {
                        Log.e(TAG, "⛔ Máximo de reintentos alcanzado para container $containerId")
                        // Ocultar el contenedor si no se puede cargar
                        activity.runOnUiThread {
                            container.visibility = View.GONE
=======
                    Log.e(TAG, "❌ Error cargando banner: ${error.message}")
                    Log.e(TAG, "Código de error: ${error.code}")
                    Log.e(TAG, "Dominio: ${error.domain}")
                    loadingStatus[containerId] = false

                    // Reintentar después de 30 segundos
                    retryScope.launch {
                        delay(30000)
                        if (!activity.isFinishing && !activity.isDestroyed) {
                            attachAdaptiveBanner(activity, container)
>>>>>>> 999fd88ece3337ae0871be7e0514342d32569941
                        }
                    }
                }

                override fun onAdOpened() {
<<<<<<< HEAD
                    Log.d(TAG, "📂 Banner abierto - container $containerId")
                }

                override fun onAdClicked() {
                    Log.d(TAG, "👆 Banner clickeado - container $containerId")
                }

                override fun onAdClosed() {
                    Log.d(TAG, "📂 Banner cerrado - container $containerId")
                }

                override fun onAdImpression() {
                    Log.d(TAG, "👁️ Banner mostrado (impresión registrada) - container $containerId")
                }
            }

            // Configurar layout params
            val layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = android.view.Gravity.CENTER
            }

            // Añadir al contenedor
            container.addView(adView, layoutParams)

            // Crear AdRequest
            val adRequest = AdRequest.Builder().build()

            Log.d(TAG, "🚀 Iniciando carga de anuncio...")
            adView.loadAd(adRequest)

        } catch (e: Exception) {
            Log.e(TAG, "💥 Error crítico en attachAdaptiveBanner", e)
=======
                    Log.d(TAG, "Banner clickeado y abierto")
                }

                override fun onAdClicked() {
                    Log.d(TAG, "Banner clickeado")
                }

                override fun onAdClosed() {
                    Log.d(TAG, "Banner cerrado")
                }

                override fun onAdImpression() {
                    Log.d(TAG, "Banner mostrado (impresión registrada)")
                }
            }

            // Añadir al contenedor
            val layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            container.addView(adView, layoutParams)

            // Crear y cargar AdRequest
            val adRequestBuilder = AdRequest.Builder()

            // Los test devices se configuran a nivel de aplicación en AdsInit
            // Aquí solo necesitamos el AdRequest básico
            val adRequest = adRequestBuilder.build()
            adView.loadAd(adRequest)

        } catch (e: Exception) {
            Log.e(TAG, "Error crítico en attachAdaptiveBanner: ${e.message}", e)
>>>>>>> 999fd88ece3337ae0871be7e0514342d32569941
            loadingStatus[container.hashCode()] = false

            // Ocultar contenedor en caso de error crítico
            try {
                container.visibility = View.GONE
            } catch (viewError: Exception) {
                Log.e(TAG, "Error ocultando contenedor: ${viewError.message}")
            }
        }
    }

    private fun getContainerInfo(container: ViewGroup): String {
        return try {
            val resourceName = container.context.resources.getResourceEntryName(container.id)
            "Resource: $resourceName, ID: ${container.id}, Hash: ${container.hashCode()}"
        } catch (e: Exception) {
            "ID: ${container.id}, Hash: ${container.hashCode()}"
        }
    }

    private fun determineAdUnitId(container: ViewGroup): String {
        return try {
            val resourceName = container.context.resources.getResourceEntryName(container.id)
<<<<<<< HEAD
            Log.d(TAG, "🔍 Determinando Ad Unit ID para: $resourceName")

            when {
                resourceName.contains("bannerBottomContainer") -> {
                    Log.d(TAG, "📍 Tipo: Banner Bottom")
                    currentBannerBottomUnitId()
                }
                resourceName.contains("adInProfileContainer") -> {
                    Log.d(TAG, "📍 Tipo: Banner Top (Profile)")
                    currentBannerTopUnitId()
                }
                resourceName.contains("adLeaderboardContainer") -> {
                    Log.d(TAG, "📍 Tipo: Banner Leaderboard")
                    currentRecyclerBannerUnitId()
                }
                resourceName.contains("adTasksContainer") -> {
                    Log.d(TAG, "📍 Tipo: Banner Tasks")
                    currentRecyclerBannerUnitId()
                }
                else -> {
                    Log.d(TAG, "📍 Tipo: Banner Top (por defecto)")
                    currentBannerTopUnitId()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ No se pudo determinar el resource name, usando banner top por defecto")
=======
            Log.d(TAG, "Determinando Ad Unit ID para: $resourceName")

            when {
                resourceName.contains("bannerBottomContainer") -> currentBannerBottomUnitId()
                resourceName.contains("adInProfileContainer") -> currentBannerTopUnitId()
                resourceName.contains("adLeaderboardContainer") -> currentRecyclerBannerUnitId()
                resourceName.contains("adTasksContainer") -> currentRecyclerBannerUnitId()
                else -> currentBannerTopUnitId()
            }
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo determinar el resource name, usando banner top por defecto")
>>>>>>> 999fd88ece3337ae0871be7e0514342d32569941
            currentBannerTopUnitId()
        }
    }

    private fun getAdaptiveBannerWidth(activity: Activity): Int {
        val displayMetrics = DisplayMetrics()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = activity.windowManager.currentWindowMetrics
            displayMetrics.widthPixels = windowMetrics.bounds.width()
            displayMetrics.density = activity.resources.displayMetrics.density
        } else {
            @Suppress("DEPRECATION")
            activity.windowManager.defaultDisplay?.getMetrics(displayMetrics)
        }

        val adWidth = (displayMetrics.widthPixels / displayMetrics.density).toInt()
<<<<<<< HEAD
        Log.d(TAG, "📐 Ancho calculado para banner adaptativo: $adWidth dp")
=======
        Log.d(TAG, "Ancho calculado para banner adaptativo: $adWidth dp")
>>>>>>> 999fd88ece3337ae0871be7e0514342d32569941
        return adWidth
    }

    fun pauseBanners(container: ViewGroup) {
        try {
<<<<<<< HEAD
            val containerId = container.hashCode()
            bannersMap[containerId]?.pause()
            Log.d(TAG, "⏸️ Banner pausado - container $containerId")
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Error pausando banner: ${e.message}")
=======
            bannersMap[container.hashCode()]?.pause()
        } catch (e: Exception) {
            Log.w(TAG, "Error pausando banner: ${e.message}")
>>>>>>> 999fd88ece3337ae0871be7e0514342d32569941
        }
    }

    fun resumeBanners(container: ViewGroup) {
        try {
<<<<<<< HEAD
            val containerId = container.hashCode()
            bannersMap[containerId]?.resume()
            Log.d(TAG, "▶️ Banner resumido - container $containerId")
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Error resumiendo banner: ${e.message}")
=======
            bannersMap[container.hashCode()]?.resume()
        } catch (e: Exception) {
            Log.w(TAG, "Error resumiendo banner: ${e.message}")
>>>>>>> 999fd88ece3337ae0871be7e0514342d32569941
        }
    }

    fun destroyBanners(container: ViewGroup) {
        try {
<<<<<<< HEAD
            val containerId = container.hashCode()
            bannersMap[containerId]?.destroy()
            bannersMap.remove(containerId)
            loadingStatus.remove(containerId)
            retryCount.remove(containerId)
            container.removeAllViews()
            Log.d(TAG, "🗑️ Banner destruido - container $containerId")
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Error destruyendo banner: ${e.message}")
=======
            val id = container.hashCode()
            bannersMap[id]?.destroy()
            bannersMap.remove(id)
            loadingStatus.remove(id)
            container.removeAllViews()
        } catch (e: Exception) {
            Log.w(TAG, "Error destruyendo banner: ${e.message}")
>>>>>>> 999fd88ece3337ae0871be7e0514342d32569941
        }
    }

    fun cleanup() {
        try {
<<<<<<< HEAD
            Log.d(TAG, "🧹 Limpiando BannerHelper...")
=======
>>>>>>> 999fd88ece3337ae0871be7e0514342d32569941
            retryScope.cancel()
            bannersMap.values.forEach { it.destroy() }
            bannersMap.clear()
            loadingStatus.clear()
<<<<<<< HEAD
            retryCount.clear()
            Log.d(TAG, "✅ BannerHelper limpiado completamente")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error en cleanup: ${e.message}")
        }
    }

    // Función para debug
    fun getDebugInfo(): String {
        return buildString {
            appendLine("=== BANNER HELPER DEBUG ===")
            appendLine("Banners activos: ${bannersMap.size}")
            appendLine("Banners cargando: ${loadingStatus.count { it.value }}")
            appendLine("Reintentos pendientes: ${retryCount.size}")
            bannersMap.forEach { (id, adView) ->
                appendLine("  Container $id: ${adView.adUnitId}")
            }
            appendLine("==========================")
        }
    }

    // Función para forzar recarga de banners
    fun reloadAllBanners(activity: Activity) {
        if (!activity.isFinishing && !activity.isDestroyed) {
            Log.d(TAG, "🔄 Forzando recarga de todos los banners...")
            bannersMap.clear()
            loadingStatus.clear()
            retryCount.clear()
=======
            Log.d(TAG, "BannerHelper cleanup completado")
        } catch (e: Exception) {
            Log.e(TAG, "Error en cleanup: ${e.message}")
>>>>>>> 999fd88ece3337ae0871be7e0514342d32569941
        }
    }
}