package com.follgramer.diamantesproplayersgo.ads

import android.app.Activity
import android.util.Log
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

    fun loadAdaptiveBanner(
        activity: Activity,
        container: ViewGroup,
        horizontalMarginDp: Int = 16,
        viewHolderId: Int = 0
    ) {
        try {
            // Iniciar con contenedor oculto
            container.visibility = ViewGroup.GONE
            container.layoutParams.height = 0
            container.background = null

            // Evitar cargas múltiples
            if (loadingStates[viewHolderId] == true) {
                Log.d(TAG, "Ya cargando banner para holder $viewHolderId")
                return
            }

            // Si ya tuvo No Fill, no reintentar por un tiempo
            if (noFillHolders.contains(viewHolderId)) {
                Log.d(TAG, "Holder $viewHolderId en cooldown por No Fill")
                return
            }

            // Reutilizar banner existente si está disponible
            loadedBanners[viewHolderId]?.let { existing ->
                if (existing.parent == null) {
                    container.removeAllViews()
                    container.addView(existing)
                    container.visibility = ViewGroup.VISIBLE
                    container.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                    Log.d(TAG, "Reutilizando banner existente para holder $viewHolderId")
                    return
                }
            }

            val unitId = AdIds.bannerRecycler()

            // Verificar límite de banners activos
            val activeBanners = loadingStates.count { it.value }
            if (activeBanners >= 2) { // Máximo 2 banners cargando
                Log.d(TAG, "Demasiados banners activos ($activeBanners)")
                return
            }

            // Marcar como cargando
            loadingStates[viewHolderId] = true

            container.removeAllViews()

            val displayMetrics = activity.resources.displayMetrics
            val screenWidthDp = (displayMetrics.widthPixels / displayMetrics.density).toInt()
            val availableWidthDp = (screenWidthDp - (horizontalMarginDp * 2)).coerceAtLeast(320)

            val adView = AdView(activity).apply {
                adUnitId = unitId
                setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
                    activity, availableWidthDp
                ))
            }

            adView.adListener = object : AdListener() {
                override fun onAdLoaded() {
                    Log.d(TAG, "✅ Banner cargado para holder $viewHolderId")
                    loadedBanners[viewHolderId] = adView
                    loadingStates[viewHolderId] = false
                    noFillHolders.remove(viewHolderId)

                    // Solo mostrar si carga exitosamente
                    container.visibility = ViewGroup.VISIBLE
                    container.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT

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

                    // Mantener oculto si falla
                    container.visibility = ViewGroup.GONE
                    container.layoutParams.height = 0

                    if (error.code == 3) { // No Fill
                        noFillHolders.add(viewHolderId)
                        // Limpiar del set después de 2 minutos
                        scope.launch {
                            delay(120000) // 2 minutos
                            noFillHolders.remove(viewHolderId)
                        }
                    } else if (error.code != 1) { // No es rate limit
                        // Reintentar solo una vez para otros errores
                        retryJobs[viewHolderId] = scope.launch {
                            delay(30000) // 30 segundos
                            if (!activity.isFinishing) {
                                loadAdaptiveBanner(activity, container, horizontalMarginDp, viewHolderId)
                            }
                        }
                    }
                }

                override fun onAdClicked() {
                    Log.d(TAG, "Banner clickeado")
                }

                override fun onAdImpression() {
                    Log.d(TAG, "Impresión registrada")
                }
            }

            container.addView(adView, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ))

            Log.d(TAG, "Solicitando banner para holder $viewHolderId")

            val adRequest = AdRequest.Builder().build()
            adView.loadAd(adRequest)

        } catch (e: Exception) {
            Log.e(TAG, "Error crítico: ${e.message}", e)
            container.visibility = ViewGroup.GONE
            container.layoutParams.height = 0
            loadingStates[viewHolderId] = false
        }
    }

    private fun dpToPx(dp: Int, activity: Activity): Int {
        return (dp * activity.resources.displayMetrics.density).toInt()
    }

    fun cleanup() {
        scope.cancel()
        retryJobs.values.forEach { it.cancel() }
        retryJobs.clear()
        loadedBanners.values.forEach { it.destroy() }
        loadedBanners.clear()
        loadingStates.clear()
        noFillHolders.clear()
        Log.d(TAG, "RecyclerViewBannerHelper limpiado")
    }
}