package com.follgramer.diamantesproplayersgo.ads

import android.app.Activity
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.follgramer.diamantesproplayersgo.R
import com.google.android.gms.ads.*
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.*

object BannerHelper {
    private const val TAG = "BannerHelper"
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val loadedBanners = mutableMapOf<Int, AdView>()
    private val loadingJobs = mutableMapOf<Int, Job?>()
    private val failedContainers = mutableListOf<Int>()

    fun attachAdaptiveBanner(activity: Activity, container: ViewGroup) {
        val containerId = System.identityHashCode(container)

        Log.d(TAG, "═══════════════════════════════════════")
        Log.d(TAG, "🎯 attachAdaptiveBanner INICIADO")
        Log.d(TAG, "═══════════════════════════════════════")
        Log.d(TAG, "Container ID: $containerId")
        Log.d(TAG, "Container Width: ${container.width}px")
        Log.d(TAG, "Container Height: ${container.height}px")
        Log.d(TAG, "Container Visible: ${container.visibility == View.VISIBLE}")
        Log.d(TAG, "Activity: ${activity.javaClass.simpleName}")

        // Cancelar cualquier carga previa
        loadingJobs[containerId]?.let {
            Log.d(TAG, "⚠️ Cancelando job previo para container $containerId")
            it.cancel()
        }

        // ✅ FORZAR VISIBILIDAD Y ALTURA DESDE EL INICIO
        Log.d(TAG, "🔧 Configurando visibilidad y altura del contenedor...")
        container.visibility = View.VISIBLE
        container.layoutParams = container.layoutParams.apply {
            height = ViewGroup.LayoutParams.WRAP_CONTENT
        }

        Log.d(TAG, "✅ Contenedor configurado - Height: ${container.layoutParams.height}")

        // Verificar consentimiento
        val canRequestAds = try {
            val result = UserMessagingPlatform.getConsentInformation(activity).canRequestAds()
            Log.d(TAG, "🔐 Consentimiento verificado: $result")
            result
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error verificando consentimiento: ${e.message}")
            false
        }

        if (!canRequestAds) {
            Log.w(TAG, "⚠️ SIN CONSENTIMIENTO - Reintentando en 3 segundos...")
            loadingJobs[containerId] = scope.launch {
                delay(3000)
                if (!activity.isFinishing && !activity.isDestroyed) {
                    attachAdaptiveBanner(activity, container)
                }
            }
            return
        }

        // Verificar AdMob
        if (!AdsInit.isAdMobReady()) {
            Log.w(TAG, "⚠️ ADMOB NO LISTO - Reintentando en 2 segundos...")
            loadingJobs[containerId] = scope.launch {
                delay(2000)
                if (!activity.isFinishing && !activity.isDestroyed) {
                    attachAdaptiveBanner(activity, container)
                }
            }
            return
        }

        Log.d(TAG, "✅ AdMob listo y consentimiento obtenido")

        // Reutilizar banner existente
        loadedBanners[containerId]?.let { existingAd ->
            if (existingAd.parent == null) {
                Log.d(TAG, "♻️ Reutilizando banner existente para container $containerId")
                container.removeAllViews()
                container.addView(existingAd)
                container.visibility = View.VISIBLE
                container.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                return
            } else {
                Log.d(TAG, "⚠️ Banner existente tiene parent, creando uno nuevo")
            }
        }

        // Crear y cargar nuevo banner
        loadingJobs[containerId] = scope.launch {
            try {
                Log.d(TAG, "🚀 Iniciando proceso de carga de banner...")

                container.removeAllViews()

                val adUnitId = getAdUnitId(container)
                val adSize = getAdaptiveAdSize(activity, container)

                Log.d(TAG, "📝 Configuración del banner:")
                Log.d(TAG, "   Ad Unit ID: $adUnitId")
                Log.d(TAG, "   Ad Size: ${adSize.width}x${adSize.height} dp")

                val adView = AdView(activity).apply {
                    this.adUnitId = adUnitId
                    setAdSize(adSize)
                }

                loadedBanners[containerId] = adView

                // Agregar al contenedor ANTES de cargar
                val layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
                container.addView(adView, layoutParams)

                Log.d(TAG, "✅ AdView agregado al contenedor")

                // Configurar listener
                adView.adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        Log.d(TAG, "═══════════════════════════════════════")
                        Log.d(TAG, "✅ ¡BANNER CARGADO EXITOSAMENTE!")
                        Log.d(TAG, "═══════════════════════════════════════")
                        Log.d(TAG, "Container ID: $containerId")
                        Log.d(TAG, "Ad Unit ID: $adUnitId")
                        Log.d(TAG, "AdView Size: ${adView.width}x${adView.height}px")
                        Log.d(TAG, "Container Size: ${container.width}x${container.height}px")

                        // Limpiar de lista de fallos
                        failedContainers.removeAll { it == containerId }

                        // Asegurar visibilidad
                        container.visibility = View.VISIBLE
                        container.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                        container.alpha = 1f

                        // Forzar refresh del layout
                        container.requestLayout()
                        container.invalidate()

                        Log.d(TAG, "✅ Banner completamente visible y renderizado")
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.e(TAG, "═══════════════════════════════════════")
                        Log.e(TAG, "❌ ERROR CARGANDO BANNER")
                        Log.e(TAG, "═══════════════════════════════════════")
                        Log.e(TAG, "Container ID: $containerId")
                        Log.e(TAG, "Ad Unit ID: $adUnitId")
                        Log.e(TAG, "Código: ${error.code}")
                        Log.e(TAG, "Mensaje: ${error.message}")
                        Log.e(TAG, "Dominio: ${error.domain}")
                        Log.e(TAG, "Causa: ${error.cause?.message ?: "N/A"}")

                        // Descripción del error
                        val errorDesc = when (error.code) {
                            AdRequest.ERROR_CODE_INTERNAL_ERROR -> "ERROR INTERNO (Config)"
                            AdRequest.ERROR_CODE_INVALID_REQUEST -> "SOLICITUD INVÁLIDA (ID)"
                            AdRequest.ERROR_CODE_NETWORK_ERROR -> "ERROR DE RED"
                            AdRequest.ERROR_CODE_NO_FILL -> "NO FILL (Sin anuncios)"
                            AdRequest.ERROR_CODE_APP_ID_MISSING -> "APP ID FALTANTE"
                            else -> "CÓDIGO: ${error.code}"
                        }
                        Log.e(TAG, "Tipo: $errorDesc")
                        Log.e(TAG, "═══════════════════════════════════════")

                        // Estrategia de reintento
                        handleAdLoadError(error, containerId, activity, container)
                    }

                    override fun onAdOpened() {
                        Log.d(TAG, "👆 Banner abierto (interacción)")
                    }

                    override fun onAdClicked() {
                        Log.d(TAG, "🖱️ Banner clickeado")
                    }

                    override fun onAdImpression() {
                        Log.d(TAG, "👁️ Impresión registrada")
                    }

                    override fun onAdClosed() {
                        Log.d(TAG, "❌ Banner cerrado")
                    }
                }

                // Construir y enviar solicitud
                val adRequest = AdRequest.Builder().build()

                Log.d(TAG, "📤 Enviando solicitud de banner...")
                Log.d(TAG, "   Timestamp: ${System.currentTimeMillis()}")

                adView.loadAd(adRequest)

            } catch (e: Exception) {
                Log.e(TAG, "═══════════════════════════════════════")
                Log.e(TAG, "💥 EXCEPCIÓN CRÍTICA")
                Log.e(TAG, "═══════════════════════════════════════")
                Log.e(TAG, "Mensaje: ${e.message}")
                Log.e(TAG, "Tipo: ${e.javaClass.simpleName}")
                e.printStackTrace()
                Log.e(TAG, "═══════════════════════════════════════")

                // Reintentar después de excepción
                loadingJobs[containerId] = scope.launch {
                    delay(10000)
                    if (!activity.isFinishing && !activity.isDestroyed) {
                        Log.w(TAG, "🔄 Reintentando después de excepción...")
                        attachAdaptiveBanner(activity, container)
                    }
                }
            }
        }
    }

    private fun handleAdLoadError(
        error: LoadAdError,
        containerId: Int,
        activity: Activity,
        container: ViewGroup
    ) {
        when (error.code) {
            AdRequest.ERROR_CODE_NO_FILL -> {
                val attempts = failedContainers.count { it == containerId }

                if (attempts < 3) {
                    Log.w(TAG, "⚠️ No Fill - Intento ${attempts + 1}/3 en 60s")
                    failedContainers.add(containerId)

                    loadingJobs[containerId] = scope.launch {
                        delay(60000)
                        if (!activity.isFinishing && !activity.isDestroyed) {
                            attachAdaptiveBanner(activity, container)
                        }
                    }
                } else {
                    Log.e(TAG, "❌ No Fill persistente - Ocultando")
                    container.visibility = View.GONE
                    container.layoutParams.height = 0
                }
            }

            AdRequest.ERROR_CODE_NETWORK_ERROR -> {
                Log.w(TAG, "⚠️ Error de red - Reintentando en 10s")
                loadingJobs[containerId] = scope.launch {
                    delay(10000)
                    if (!activity.isFinishing && !activity.isDestroyed) {
                        attachAdaptiveBanner(activity, container)
                    }
                }
            }

            AdRequest.ERROR_CODE_INVALID_REQUEST -> {
                Log.e(TAG, "❌ Solicitud inválida - VERIFICAR:")
                Log.e(TAG, "   1. ¿Ad Unit ID correcto?")
                Log.e(TAG, "   2. ¿APP ID en Manifest?")
                Log.e(TAG, "   3. ¿Cuenta AdMob activa?")
                container.visibility = View.GONE
            }

            AdRequest.ERROR_CODE_APP_ID_MISSING -> {
                Log.e(TAG, "❌ APP ID FALTANTE en AndroidManifest.xml")
                container.visibility = View.GONE
            }

            else -> {
                Log.w(TAG, "⚠️ Error genérico - Reintentando en 30s")
                loadingJobs[containerId] = scope.launch {
                    delay(30000)
                    if (!activity.isFinishing && !activity.isDestroyed) {
                        attachAdaptiveBanner(activity, container)
                    }
                }
            }
        }
    }

    private fun getAdaptiveAdSize(activity: Activity, container: ViewGroup): AdSize {
        val display = activity.windowManager.defaultDisplay
        val outMetrics = DisplayMetrics()
        display.getMetrics(outMetrics)

        val density = outMetrics.density
        var adWidthPixels = container.width.toFloat()

        if (adWidthPixels == 0f) {
            adWidthPixels = outMetrics.widthPixels.toFloat()
        }

        val adWidth = (adWidthPixels / density).toInt()

        Log.d(TAG, "📏 Tamaño de banner calculado:")
        Log.d(TAG, "   Screen: ${outMetrics.widthPixels}px")
        Log.d(TAG, "   Container: ${container.width}px")
        Log.d(TAG, "   Density: $density")
        Log.d(TAG, "   Ad Width: ${adWidth}dp")

        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth)
    }

    private fun getAdUnitId(container: ViewGroup): String {
        val adId = when (container.id) {
            R.id.adInProfileContainer -> {
                Log.d(TAG, "📌 Container: BANNER TOP")
                AdIds.bannerTop()
            }
            R.id.bannerBottomContainer -> {
                Log.d(TAG, "📌 Container: BANNER BOTTOM")
                AdIds.bannerBottom()
            }
            else -> {
                Log.w(TAG, "⚠️ Container ID desconocido: ${container.id}")
                Log.w(TAG, "   Usando bannerBottom() por defecto")
                AdIds.bannerBottom()
            }
        }
        Log.d(TAG, "📌 Ad Unit ID seleccionado: $adId")
        return adId
    }

    fun pause(parent: ViewGroup) {
        try {
            val count = loadedBanners.size
            loadedBanners.values.forEach { it.pause() }
            Log.d(TAG, "⏸️ $count banners pausados")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error pausando: ${e.message}")
        }
    }

    fun resume(parent: ViewGroup) {
        try {
            val count = loadedBanners.size
            loadedBanners.values.forEach { it.resume() }
            Log.d(TAG, "▶️ $count banners resumidos")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error resumiendo: ${e.message}")
        }
    }

    fun destroy(parent: ViewGroup) {
        try {
            Log.d(TAG, "🗑️ Destruyendo BannerHelper...")

            loadingJobs.values.forEach { it?.cancel() }
            loadingJobs.clear()

            loadedBanners.values.forEach {
                try {
                    it.destroy()
                } catch (e: Exception) {
                    Log.e(TAG, "Error destruyendo banner: ${e.message}")
                }
            }
            loadedBanners.clear()
            failedContainers.clear()

            Log.d(TAG, "✅ BannerHelper destruido")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error destruyendo: ${e.message}")
        }
    }

    fun forceRefresh(activity: Activity, container: ViewGroup) {
        val containerId = System.identityHashCode(container)
        Log.d(TAG, "🔄 Forzando recarga - Container: $containerId")

        failedContainers.removeAll { it == containerId }
        loadedBanners.remove(containerId)?.destroy()

        attachAdaptiveBanner(activity, container)
    }

    fun getDebugInfo(): String {
        return """
            ═══════════════════════════════════════
            BannerHelper Debug Info
            ═══════════════════════════════════════
            Banners cargados: ${loadedBanners.size}
            Jobs activos: ${loadingJobs.size}
            Contenedores fallidos: ${failedContainers.size}
            
            Banners activos:
            ${loadedBanners.entries.joinToString("\n") {
            "  • ID ${it.key}: ${it.value.adUnitId}"
        }}
            
            Fallos: ${failedContainers.joinToString(", ")}
            ═══════════════════════════════════════
        """.trimIndent()
    }
}