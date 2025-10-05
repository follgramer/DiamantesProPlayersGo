package com.follgramer.diamantesproplayersgo.ads

import android.app.Activity
import android.graphics.Color
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.google.android.gms.ads.*
import kotlinx.coroutines.*

object RecyclerViewBannerHelper {
    private const val TAG = "RVBannerHelper"
    private val loadedBanners = mutableMapOf<Int, AdView>()
    private val loadingStates = mutableMapOf<Int, Boolean>()
    private val retryJobs = mutableMapOf<Int, Job>()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val noFillHolders = mutableSetOf<Int>()
    private val failCounts = mutableMapOf<Int, Int>()

    fun loadAdaptiveBanner(
        activity: Activity,
        container: ViewGroup,
        horizontalMarginDp: Int = 16,
        viewHolderId: Int = 0
    ) {
        try {
            // INICIAR COMPLETAMENTE OCULTO con altura 0
            container.apply {
                visibility = View.GONE
                layoutParams = layoutParams.apply {
                    height = 0
                }
                setBackgroundColor(Color.TRANSPARENT)
                removeAllViews()
            }

            // Evitar cargas múltiples
            if (loadingStates[viewHolderId] == true) {
                Log.d(TAG, "Ya cargando banner para holder $viewHolderId")
                return
            }

            // Control de reintentos - máximo 3 fallos antes de cooldown largo
            val currentFailCount = failCounts[viewHolderId] ?: 0
            if (currentFailCount >= 3) {
                if (noFillHolders.contains(viewHolderId)) {
                    Log.d(TAG, "Holder $viewHolderId en cooldown extendido")
                    return
                }

                // Cooldown de 5 minutos después de 3 fallos
                noFillHolders.add(viewHolderId)
                scope.launch {
                    delay(300000) // 5 minutos
                    noFillHolders.remove(viewHolderId)
                    failCounts[viewHolderId] = 0
                }
                return
            }

            // Reutilizar banner existente si está disponible
            loadedBanners[viewHolderId]?.let { existing ->
                if (existing.parent == null) {
                    container.removeAllViews()
                    container.addView(existing)
                    container.visibility = View.VISIBLE
                    container.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                    Log.d(TAG, "Reutilizando banner existente para holder $viewHolderId")
                    return
                }
            }

            // Usar ID de banner
            val unitId = AdIds.banner()

            // Verificar límite de banners activos
            val activeBanners = loadingStates.count { it.value }
            if (activeBanners >= 3) {
                Log.d(TAG, "Demasiados banners activos ($activeBanners)")
                container.visibility = View.GONE
                container.layoutParams.height = 0
                return
            }

            // Marcar como cargando
            loadingStates[viewHolderId] = true

            // Limpiar contenedor
            container.removeAllViews()

            // Calcular dimensiones
            val displayMetrics = activity.resources.displayMetrics
            val screenWidthDp = (displayMetrics.widthPixels / displayMetrics.density).toInt()
            val availableWidthDp = (screenWidthDp - (horizontalMarginDp * 2)).coerceAtLeast(320)

            // Crear AdView
            val adView = AdView(activity).apply {
                adUnitId = unitId
                setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
                    activity, availableWidthDp
                ))
            }

            // Configurar listener
            adView.adListener = object : AdListener() {
                override fun onAdLoaded() {
                    Log.d(TAG, "✅ Banner cargado para holder $viewHolderId")

                    // Guardar referencia
                    loadedBanners[viewHolderId] = adView
                    loadingStates[viewHolderId] = false
                    noFillHolders.remove(viewHolderId)
                    failCounts[viewHolderId] = 0

                    // Cancelar retry job si existe
                    retryJobs[viewHolderId]?.cancel()
                    retryJobs.remove(viewHolderId)

                    // Mostrar contenedor con altura wrap_content
                    container.apply {
                        visibility = View.VISIBLE
                        layoutParams = layoutParams.apply {
                            height = ViewGroup.LayoutParams.WRAP_CONTENT
                        }
                        setBackgroundColor(Color.TRANSPARENT)
                    }

                    // Animación suave
                    container.alpha = 0f
                    container.animate()
                        .alpha(1f)
                        .setDuration(300)
                        .start()
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "❌ Error cargando banner: ${error.message}, código: ${error.code}")
                    loadingStates[viewHolderId] = false

                    // Incrementar contador de fallos
                    failCounts[viewHolderId] = (failCounts[viewHolderId] ?: 0) + 1

                    // Mantener oculto en todos los casos de error
                    container.apply {
                        visibility = View.GONE
                        layoutParams.height = 0
                        setBackgroundColor(Color.TRANSPARENT)
                        removeAllViews()
                    }

                    when (error.code) {
                        3 -> { // No Fill
                            if (failCounts[viewHolderId] ?: 0 >= 2) {
                                noFillHolders.add(viewHolderId)
                                scope.launch {
                                    delay(120000) // 2 minutos
                                    noFillHolders.remove(viewHolderId)
                                    failCounts[viewHolderId] = 0
                                }
                            } else {
                                scheduleRetry(activity, container, horizontalMarginDp, viewHolderId, 30000)
                            }
                        }
                        1 -> { // Invalid Request
                            // No reintentar
                        }
                        2 -> { // Network Error
                            scheduleRetry(activity, container, horizontalMarginDp, viewHolderId, 10000)
                        }
                        else -> {
                            scheduleRetry(activity, container, horizontalMarginDp, viewHolderId, 60000)
                        }
                    }
                }

                override fun onAdClicked() {
                    Log.d(TAG, "Banner clickeado - holder $viewHolderId")
                }

                override fun onAdImpression() {
                    Log.d(TAG, "Impresión registrada - holder $viewHolderId")
                }

                override fun onAdOpened() {
                    Log.d(TAG, "Banner abierto - holder $viewHolderId")
                }

                override fun onAdClosed() {
                    Log.d(TAG, "Banner cerrado - holder $viewHolderId")
                }
            }

            // Agregar AdView al contenedor
            container.addView(adView, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ))

            Log.d(TAG, "📤 Solicitando banner para holder $viewHolderId con ID: $unitId")

            // Cargar anuncio
            val adRequest = AdRequest.Builder().build()
            adView.loadAd(adRequest)

        } catch (e: Exception) {
            Log.e(TAG, "Error crítico: ${e.message}", e)

            // Asegurar que quede oculto
            container.apply {
                visibility = View.GONE
                layoutParams.height = 0
                setBackgroundColor(Color.TRANSPARENT)
                removeAllViews()
            }

            loadingStates[viewHolderId] = false
            scheduleRetry(activity, container, horizontalMarginDp, viewHolderId, 60000)
        }
    }

    private fun scheduleRetry(
        activity: Activity,
        container: ViewGroup,
        horizontalMarginDp: Int,
        viewHolderId: Int,
        delayMs: Long
    ) {
        retryJobs[viewHolderId]?.cancel()

        retryJobs[viewHolderId] = scope.launch {
            delay(delayMs)
            if (!activity.isFinishing && !activity.isDestroyed) {
                Log.d(TAG, "🔄 Reintentando carga de banner para holder $viewHolderId")
                loadAdaptiveBanner(activity, container, horizontalMarginDp, viewHolderId)
            }
        }
    }

    private fun dpToPx(dp: Int, activity: Activity): Int {
        return (dp * activity.resources.displayMetrics.density).toInt()
    }

    fun pauseBanner(viewHolderId: Int) {
        loadedBanners[viewHolderId]?.pause()
    }

    fun resumeBanner(viewHolderId: Int) {
        loadedBanners[viewHolderId]?.resume()
    }

    fun destroyBanner(viewHolderId: Int) {
        try {
            retryJobs[viewHolderId]?.cancel()
            retryJobs.remove(viewHolderId)

            loadedBanners[viewHolderId]?.destroy()
            loadedBanners.remove(viewHolderId)

            loadingStates.remove(viewHolderId)
            noFillHolders.remove(viewHolderId)
            failCounts.remove(viewHolderId)

            Log.d(TAG, "Banner destruido para holder $viewHolderId")
        } catch (e: Exception) {
            Log.e(TAG, "Error destruyendo banner: ${e.message}")
        }
    }

    fun cleanup() {
        try {
            scope.cancel()

            retryJobs.values.forEach { it.cancel() }
            retryJobs.clear()

            loadedBanners.values.forEach { it.destroy() }
            loadedBanners.clear()

            loadingStates.clear()
            noFillHolders.clear()
            failCounts.clear()

            Log.d(TAG, "RecyclerViewBannerHelper limpiado completamente")
        } catch (e: Exception) {
            Log.e(TAG, "Error en cleanup: ${e.message}")
        }
    }

    fun getStats(): String {
        return """
            RecyclerViewBannerHelper Stats:
            - Banners cargados: ${loadedBanners.size}
            - Cargando: ${loadingStates.count { it.value }}
            - En cooldown: ${noFillHolders.size}
            - Reintentos programados: ${retryJobs.size}
            - Holders con fallos: ${failCounts.filter { it.value > 0 }.size}
        """.trimIndent()
    }
}