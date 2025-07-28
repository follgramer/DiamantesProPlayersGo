package com.follgramer.diamantesproplayersgo

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.children
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import com.follgramer.diamantesproplayersgo.databinding.ActivityMainBinding
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import java.util.Calendar
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    // --- Variables de la App ---
    private lateinit var binding: ActivityMainBinding
    private var rewardedAd: RewardedAd? = null
    private var interstitialAd: InterstitialAd? = null
    private val TAG_ADMOB = "AdMob"

    private val REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
    private val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

    private var currentPlayerId: String? = null
    private var currentSpins: Int = 10
    private var isSpinning = false

    private val gridItemsData = listOf(5, 1, 10, 25, 20, 0, 50, 5, 15, 1, 100, 20)
    private var countdownTimer: CountDownTimer? = null

    // --- Variables de Consentimiento (UMP) ---
    private lateinit var consentInformation: ConsentInformation

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        SessionManager.init(this)

        val playerId = SessionManager.getPlayerId(this)
        val sessionId = SessionManager.getSessionId(this)
        Log.d("SESSION", "Jugador: $playerId - Sesión: $sessionId")

        handleNotificationIntent(intent)

        checkConsentSimple()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        val redirectTo = intent?.getStringExtra("redirect_to")

        if (redirectTo == "winners") {
            // Redirigir a ganadores después de un pequeño delay
            binding.root.postDelayed({
                redirectToWinnersSection()
            }, 500)
        }
    }

    // --- Lógica de Consentimiento SIMPLIFICADA ---

    private fun checkConsentSimple() {
        try {
            val params = ConsentRequestParameters.Builder().build()
            consentInformation = UserMessagingPlatform.getConsentInformation(this)

            consentInformation.requestConsentInfoUpdate(this, params,
                {
                    Log.d("UMP", "Información de consentimiento actualizada")
                    if (consentInformation.isConsentFormAvailable) {
                        loadConsentFormSimple()
                    } else {
                        showCustomConsentDialogIfNeeded()
                    }
                },
                { error ->
                    Log.w("UMP", "Error solicitando info de consentimiento: ${error.message}")
                    showCustomConsentDialogIfNeeded()
                }
            )
        } catch (e: Exception) {
            Log.w("UMP", "Error general con UMP: ${e.message}")
            showCustomConsentDialogIfNeeded()
        }
    }

    private fun loadConsentFormSimple() {
        try {
            UserMessagingPlatform.loadConsentForm(this,
                { form ->
                    if (consentInformation.consentStatus == ConsentInformation.ConsentStatus.REQUIRED) {
                        form.show(this) { formError ->
                            if (formError != null) {
                                Log.w("UMP", "Error mostrando formulario: ${formError.message}")
                            }
                            proceedAfterConsent()
                        }
                    } else {
                        proceedAfterConsent()
                    }
                },
                { loadError ->
                    Log.w("UMP", "Error cargando formulario: ${loadError.message}")
                    showCustomConsentDialogIfNeeded()
                }
            )
        } catch (e: Exception) {
            Log.w("UMP", "Error general cargando formulario: ${e.message}")
            showCustomConsentDialogIfNeeded()
        }
    }

    private fun proceedAfterConsent() {
        if (consentInformation.canRequestAds() ||
            getSharedPreferences("DiamantesProPrefs", Context.MODE_PRIVATE)
                .getBoolean("CONSENT_ACCEPTED", false)) {
            inicializarApp()
        } else {
            showCustomConsentDialogIfNeeded()
        }
    }

    private fun showCustomConsentDialogIfNeeded() {
        val prefs = getSharedPreferences("DiamantesProPrefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("CONSENT_ACCEPTED", false)) {
            showCustomConsentDialog()
        } else {
            inicializarApp()
        }
    }

    private fun showCustomConsentDialog() {
        val prefs = getSharedPreferences("DiamantesProPrefs", Context.MODE_PRIVATE)

        val dialog = android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val dialogView = layoutInflater.inflate(R.layout.modal_consentimiento, null)
        dialog.setContentView(dialogView)

        dialog.window?.setLayout(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.MATCH_PARENT
        )

        dialogView.findViewById<Button>(R.id.btnAceptarConsentimiento)?.setOnClickListener {
            prefs.edit().putBoolean("CONSENT_ACCEPTED", true).apply()
            dialog.dismiss()
            inicializarApp()
        }

        dialogView.findViewById<Button>(R.id.btnRechazarConsentimiento)?.setOnClickListener {
            dialog.dismiss()
            showWhyConsentIsImportantDialog()
        }

        dialogView.findViewById<TextView>(R.id.textoLegalCompleto)?.setOnClickListener {
            showLegalOptionsDialog()
        }

        dialog.setCancelable(false)
        dialog.show()
    }

    private fun showLegalOptionsDialog() {
        val options = arrayOf("Políticas de Privacidad", "Términos y Condiciones")

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("¿Qué deseas leer?")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showLegalContentModal("Políticas de Privacidad", getString(R.string.privacy_policy_content))
                    1 -> showLegalContentModal("Términos y Condiciones", getString(R.string.terms_content))
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showLegalContentModal(title: String, content: String) {
        val legalDialog = android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val dialogView = layoutInflater.inflate(R.layout.modal_legal_content, null)
        legalDialog.setContentView(dialogView)

        legalDialog.window?.setLayout(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.MATCH_PARENT
        )

        val titleView = dialogView.findViewById<TextView>(R.id.tvTituloLegal)
        val contentView = dialogView.findViewById<TextView>(R.id.tvContenidoLegal)
        val closeButton = dialogView.findViewById<ImageView>(R.id.btnCerrarModal)
        val understoodButton = dialogView.findViewById<Button>(R.id.btnEntendido)

        titleView.text = title

        contentView.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            android.text.Html.fromHtml(content, android.text.Html.FROM_HTML_MODE_COMPACT)
        } else {
            @Suppress("DEPRECATION")
            android.text.Html.fromHtml(content)
        }

        closeButton.setOnClickListener {
            legalDialog.dismiss()
            showCustomConsentDialog()
        }

        understoodButton.setOnClickListener {
            legalDialog.dismiss()
            showCustomConsentDialog()
        }

        legalDialog.setCancelable(true)
        legalDialog.setOnCancelListener {
            showCustomConsentDialog()
        }

        legalDialog.show()
    }

    private fun showWhyConsentIsImportantDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("¿Estás seguro?")
            .setMessage("Si no aceptás el consentimiento, no podrás ver anuncios ni participar por premios.")
            .setCancelable(false)
            .setPositiveButton("Aceptar políticas") { dialog, _ ->
                getSharedPreferences("DiamantesProPrefs", Context.MODE_PRIVATE)
                    .edit().putBoolean("CONSENT_ACCEPTED", true).apply()
                dialog.dismiss()
                inicializarApp()
            }
            .setNegativeButton("Cerrar app") { _, _ ->
                finishAffinity()
            }
            .show()
    }

    // --- Lógica Principal de la Aplicación ---

    private fun inicializarApp() {
        runOnUiThread {
            setupToolbarAndDrawer()
            setupFirebase()
            setupAdMob()
            loadAllAds()
            setupClickListeners()
            setupRecyclerViews()
            initializeGrid()
            startWeeklyCountdown()
            setupBackPressedHandler()
            signInAnonymously()
            arreglarColoresDeTextos()

            checkNotificationPermissions()

            val playerId = SessionManager.getPlayerId(this@MainActivity)
            if (playerId.isNotEmpty()) {
                createFirebaseSession(playerId)
                obtenerTop5Firebase()
                setupFCMToken(playerId)
            }
        }
    }

    private fun setupFCMToken(playerId: String) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM_TOKEN", "Error obteniendo token FCM", task.exception)
                return@addOnCompleteListener
            }

            val token = task.result
            Log.d("FCM_TOKEN", "Token FCM obtenido: $token")

            saveTokenToDatabase(playerId, token)
        }
    }

    private fun saveTokenToDatabase(playerId: String, token: String) {
        val tokenData = mapOf(
            "token" to token,
            "timestamp" to ServerValue.TIMESTAMP,
            "deviceInfo" to Build.MODEL
        )

        database.child("playerTokens").child(playerId)
            .setValue(tokenData)
            .addOnSuccessListener {
                Log.d("FCM_TOKEN", "Token guardado exitosamente para $playerId")
            }
            .addOnFailureListener { error ->
                Log.e("FCM_TOKEN", "Error guardando token: ${error.message}")
            }
    }

    private fun arreglarColoresDeTextos() {
        try {
            aplicarColoresATodosLosTextos(binding.sectionHome.root)
            aplicarColoresATodosLosTextos(binding.sectionTasks.root)
            aplicarColoresATodosLosTextos(binding.sectionLeaderboard.root)
            aplicarColoresATodosLosTextos(binding.sectionWinners.root)

            Log.d("COLOR_FIX", "Colores aplicados exitosamente")
        } catch (e: Exception) {
            Log.e("COLOR_FIX", "Error aplicando colores: ${e.message}")
        }
    }

    private fun aplicarColoresATodosLosTextos(view: View) {
        try {
            when (view) {
                is TextView -> {
                    val currentColor = view.currentTextColor
                    if (Color.alpha(currentColor) < 200 ||
                        (Color.red(currentColor) + Color.green(currentColor) + Color.blue(currentColor)) < 400) {

                        val newColor = when {
                            view.textSize >= 18f -> Color.WHITE
                            view.textSize >= 14f -> Color.parseColor("#CCCCCC")
                            else -> Color.parseColor("#B0B0B0")
                        }
                        view.setTextColor(newColor)
                    }
                }
                is android.view.ViewGroup -> {
                    for (i in 0 until view.childCount) {
                        aplicarColoresATodosLosTextos(view.getChildAt(i))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("COLOR_FIX", "Error en aplicarColoresATodosLosTextos: ${e.message}")
        }
    }

    // --- 🔐 GESTIÓN DE SESIONES FIREBASE ---

    private fun createFirebaseSession(playerId: String) {
        val sessionId = SessionManager.getSessionId(this)
        Log.d("SESSION_FIREBASE", "Creando sesión para Player: $playerId, Session: $sessionId")

        val sessionData = mapOf(
            "timestamp" to ServerValue.TIMESTAMP,
            "active" to true,
            "deviceInfo" to Build.MODEL,
            "appVersion" to "1.0"
        )

        FirebaseDatabase.getInstance().getReference("sessions/$playerId/$sessionId")
            .setValue(sessionData)
            .addOnSuccessListener {
                Log.d("SESSION_FIREBASE", "Sesión creada exitosamente")
                verificarLimiteDeSesiones(playerId)
            }
            .addOnFailureListener { error ->
                Log.e("SESSION_FIREBASE", "Error creando sesión: ${error.message}")
            }
    }

    private fun verificarLimiteDeSesiones(playerId: String) {
        val sessionsRef = FirebaseDatabase.getInstance().getReference("sessions/$playerId")

        sessionsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var sesionesActivas = 0
                val currentTime = System.currentTimeMillis()

                for (sesion in snapshot.children) {
                    val active = sesion.child("active").getValue(Boolean::class.java) ?: false
                    val timestamp = sesion.child("timestamp").getValue(Long::class.java) ?: 0L

                    if (active && (currentTime - timestamp) < 15 * 60 * 1000) {
                        sesionesActivas++
                    }
                }

                Log.d("SESSION_LIMIT", "Sesiones activas para $playerId: $sesionesActivas")

                if (sesionesActivas > 3) {
                    mostrarModalExcesoSesiones(sesionesActivas)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("SESSION_LIMIT", "Error verificando sesiones: ${error.message}")
            }
        })
    }

    private fun mostrarModalExcesoSesiones(cantidad: Int) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Demasiadas sesiones")
            .setMessage("Tu ID está siendo usado en $cantidad dispositivos al mismo tiempo.\n\n" +
                    "Solo se permiten máximo 3 dispositivos simultáneos.\n\n" +
                    "Si no reconoces estas sesiones, contacta:\nfollgramer@gmail.com")
            .setCancelable(false)
            .setPositiveButton("Cerrar App") { _, _ ->
                Log.d("SESSION_LIMIT", "Usuario cerró app por exceso de sesiones")
                finishAffinity()
            }
            .setNegativeButton("Más Info") { _, _ ->
                mostrarInfoSobreSesiones()
            }
            .show()
    }

    private fun mostrarInfoSobreSesiones() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("¿Qué son las sesiones?")
            .setMessage("Una sesión se crea cuando:\n\n" +
                    "• Instalas la app en un nuevo dispositivo\n" +
                    "• Cambias de ID de jugador\n" +
                    "• Reinstalas la aplicación\n\n" +
                    "Las sesiones se consideran activas por 15 minutos.\n\n" +
                    "Si perdiste tu dispositivo o no reconoces las sesiones, " +
                    "contacta inmediatamente:\nfollgramer@gmail.com")
            .setPositiveButton("Entendido") { dialog, _ ->
                dialog.dismiss()
                finishAffinity()
            }
            .show()
    }

    // --- 🏆 TOP 5 Y LEADERBOARD ---

    private fun obtenerTop5Firebase() {
        Log.d("TOP5_FIREBASE", "=== OBTENIENDO LEADERBOARD COMPLETO ===")

        database.child("players")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val jugadores = mutableListOf<Player>()
                    for (playerSnapshot in snapshot.children) {
                        try {
                            val playerId = playerSnapshot.key ?: continue
                            val tickets = playerSnapshot.child("tickets").getValue(Long::class.java) ?: 0L
                            val passes = playerSnapshot.child("passes").getValue(Long::class.java) ?: 0L
                            if (playerId.matches(Regex("\\d+")) && playerId.length >= 5) {
                                jugadores.add(Player(playerId, tickets, passes))
                            }
                        } catch (e: Exception) {
                            Log.e("TOP5_FIREBASE", "Error procesando jugador: ${e.message}")
                        }
                    }

                    val jugadoresOrdenados = jugadores.sortedWith(
                        compareByDescending<Player> { it.passes }.thenByDescending { it.tickets }
                    )

                    runOnUiThread {
                        binding.sectionLeaderboard.leaderboardRecyclerView.adapter = LeaderboardAdapter(jugadoresOrdenados)

                        if (jugadoresOrdenados.isNotEmpty()) {
                            val top5 = jugadoresOrdenados.take(5)
                            val adapter = MiniLeaderboardAdapter(top5, currentPlayerId)
                            binding.sectionHome.miniLeaderboardList.adapter = adapter
                            binding.sectionHome.miniLeaderboardList.visibility = View.VISIBLE
                        } else {
                            binding.sectionHome.miniLeaderboardList.visibility = View.GONE
                        }

                        val myRank = jugadoresOrdenados.indexOfFirst { it.playerId == currentPlayerId }
                        val myRankLayout = binding.sectionHome.myRankStatus
                        if (myRank != -1) {
                            myRankLayout.root.visibility = View.VISIBLE
                            val myPlayerData = jugadoresOrdenados[myRank]
                            myRankLayout.rank.text = "#${myRank + 1}"
                            myRankLayout.playerId.text = "Tú"
                            myRankLayout.passes.text = "${myPlayerData.passes} Pases"
                        } else {
                            myRankLayout.root.visibility = View.GONE
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("TOP5_FIREBASE", "❌ ERROR Firebase: ${error.message}")
                }
            })
    }

    // --- Configuración Inicial UI y Navegación ---

    private fun setupToolbarAndDrawer() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        val toggle = ActionBarDrawerToggle(this, binding.drawerLayout, binding.toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        binding.navView.setNavigationItemSelectedListener { handleNavigation(it) }
    }

    private fun setupFirebase() {
        database = FirebaseDatabase.getInstance("https://diamantes-pro-players-go-f3510-default-rtdb.firebaseio.com/").reference
        auth = Firebase.auth
    }

    private fun setupAdMob() {
        MobileAds.initialize(this) {}
    }

    private fun setupRecyclerViews() {
        binding.sectionLeaderboard.leaderboardRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.sectionWinners.winnersRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.sectionHome.miniLeaderboardList.layoutManager = LinearLayoutManager(this)
    }

    private fun loadAllAds() {
        loadBannerAds()
        loadRewardedAd()
        loadInterstitialAd()
    }

    private fun setupClickListeners() {
        binding.sectionHome.myPlayerId.setOnClickListener { promptForPlayerId() }
        binding.sectionHome.spinButton.setOnClickListener { spinGrid() }
        binding.sectionHome.getSpinsButton.setOnClickListener { requestSpinsByWatchingAd() }
        binding.sectionTasks.taskButton.setOnClickListener { requestTicketsByWatchingAd() }
    }

    private fun setupBackPressedHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackPress()
            }
        })
    }

    private fun handleBackPress() {
        when {
            binding.drawerLayout.isDrawerOpen(GravityCompat.START) -> {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
            }
            !isInHomeSection() -> {
                goToHomeSection()
            }
            else -> {
                showExitConfirmationDialog()
            }
        }
    }

    private fun isInHomeSection(): Boolean {
        return binding.sectionHome.root.visibility == View.VISIBLE
    }

    private fun goToHomeSection() {
        hideAllSections()
        showSection(binding.sectionHome.root)
        binding.navView.setCheckedItem(R.id.nav_home)
    }

    private fun showExitConfirmationDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Salir de la aplicación")
            .setMessage("¿Estás seguro de que quieres cerrar la aplicación?")
            .setCancelable(true)
            .setPositiveButton("Salir") { _, _ ->
                marcarSesionInactiva()
                finishAffinity()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun handleNavigation(menuItem: MenuItem): Boolean {
        hideAllSections()
        if (menuItem.groupId == R.id.group_main) {
            menuItem.isChecked = true
        }

        when (menuItem.itemId) {
            R.id.nav_home -> showSection(binding.sectionHome.root)
            R.id.nav_tasks -> showInterstitialAd { showSection(binding.sectionTasks.root) }
            R.id.nav_leaderboard -> showInterstitialAd { showSection(binding.sectionLeaderboard.root) }
            R.id.nav_winners -> showInterstitialAd { showSection(binding.sectionWinners.root) }
            R.id.nav_privacy -> showLegalModal("Política de Privacidad", getString(R.string.privacy_policy_content))
            R.id.nav_terms -> showLegalModal("Términos y Condiciones", getString(R.string.terms_content))
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun hideAllSections() {
        binding.sectionHome.root.visibility = View.GONE
        binding.sectionTasks.root.visibility = View.GONE
        binding.sectionLeaderboard.root.visibility = View.GONE
        binding.sectionWinners.root.visibility = View.GONE
        binding.sectionLegal.root.visibility = View.GONE
    }

    private fun showSection(sectionRoot: View) {
        sectionRoot.visibility = View.VISIBLE
    }

    private fun showLegalModal(title: String, content: String) {
        val dialog = android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val dialogView = layoutInflater.inflate(R.layout.dialog_legal, null)
        dialog.setContentView(dialogView)

        val titleView = dialogView.findViewById<TextView>(R.id.dialog_title)
        val contentView = dialogView.findViewById<TextView>(R.id.dialog_content)
        val closeButton = dialogView.findViewById<ImageView>(R.id.btn_close)
        val understoodButton = dialogView.findViewById<Button>(R.id.btn_understood)

        titleView.text = title
        contentView.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            android.text.Html.fromHtml(content, android.text.Html.FROM_HTML_MODE_COMPACT)
        } else {
            @Suppress("DEPRECATION")
            android.text.Html.fromHtml(content)
        }

        closeButton.setOnClickListener { dialog.dismiss() }
        understoodButton.setOnClickListener { dialog.dismiss() }
        dialog.setCancelable(true)
        dialog.show()
    }

    // --- Lógica de Anuncios ---

    private fun loadBannerAds() {
        val adRequest = AdRequest.Builder().build()
        binding.adViewBottom.loadAd(adRequest)
        binding.sectionHome.adViewTopHome.loadAd(adRequest)
        binding.sectionTasks.adViewTasks.loadAd(adRequest)
        binding.sectionLeaderboard.adViewLeaderboard.loadAd(adRequest)
    }

    private fun loadRewardedAd() {
        RewardedAd.load(this, REWARDED_AD_UNIT_ID, AdRequest.Builder().build(), object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) { rewardedAd = ad }
            override fun onAdFailedToLoad(error: LoadAdError) { rewardedAd = null }
        })
    }

    private fun requestSpinsByWatchingAd() {
        showRewardedAd {
            val amount = 10
            currentSpins += amount
            updateSpinCountUI()
        }
    }

    private fun requestTicketsByWatchingAd() {
        showRewardedAd {
            val amount = 20
            addTicketsToPlayer(amount)
        }
    }

    private fun showRewardedAd(onRewarded: (RewardItem) -> Unit) {
        if (currentPlayerId == null) {
            promptForPlayerId()
            return
        }
        if (rewardedAd == null) {
            loadRewardedAd()
            return
        }
        rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() { loadRewardedAd() }
            override fun onAdFailedToShowFullScreenContent(adError: AdError) { rewardedAd = null }
        }
        rewardedAd?.show(this, onRewarded)
    }

    private fun loadInterstitialAd() {
        InterstitialAd.load(this, INTERSTITIAL_AD_UNIT_ID, AdRequest.Builder().build(), object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) { interstitialAd = ad }
            override fun onAdFailedToLoad(error: LoadAdError) { interstitialAd = null }
        })
    }

    private fun showInterstitialAd(onAdDismissed: () -> Unit) {
        if (interstitialAd != null) {
            interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    loadInterstitialAd()
                    onAdDismissed()
                }
                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    interstitialAd = null
                    onAdDismissed()
                }
            }
            interstitialAd?.show(this)
        } else {
            onAdDismissed()
        }
    }

    // --- Lógica de la Ruleta ---

    private fun initializeGrid() {
        binding.sectionHome.gridContainer.removeAllViews()
        gridItemsData.shuffled().forEach { value ->
            val itemView = LayoutInflater.from(this).inflate(R.layout.item_grid, binding.sectionHome.gridContainer, false)
            val valueText = itemView.findViewById<TextView>(R.id.grid_item_value)
            val labelText = itemView.findViewById<TextView>(R.id.grid_item_label)
            valueText.text = value.toString()
            labelText.text = when (value) {
                1 -> "TICKET"
                0 -> "NADA"
                else -> "TICKETS"
            }
            binding.sectionHome.gridContainer.addView(itemView)
        }
    }

    private fun spinGrid() {
        if (isSpinning) return
        if (currentPlayerId == null) {
            promptForPlayerId()
            return
        }
        if (currentSpins <= 0) {
            MaterialDialog(this).show {
                title(text = "¡Sin Giros!")
                message(text = "Necesitas más giros para jugar. ¿Quieres ver un video para obtener 10 giros?")
                positiveButton(text = "Ver Video") {
                    requestSpinsByWatchingAd()
                }
                negativeButton(text = "Ahora no")
            }
            return
        }

        isSpinning = true
        currentSpins--
        updateSpinCountUI()

        binding.sectionHome.spinButton.isEnabled = false
        binding.sectionHome.spinButton.text = "🎟️ GIRANDO..."
        binding.sectionHome.spinButton.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#424242"))

        object : CountDownTimer(3000, 80) {
            val gridItems = binding.sectionHome.gridContainer.children.toList()

            override fun onTick(millisUntilFinished: Long) {
                gridItems.forEach {
                    it.setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.border_color))
                    it.findViewById<TextView>(R.id.grid_item_value).setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_color))
                    it.findViewById<TextView>(R.id.grid_item_label).setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_muted))
                }
                val currentIndex = (0 until gridItems.size).random()
                gridItems[currentIndex].setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.pass_color))
                gridItems[currentIndex].findViewById<TextView>(R.id.grid_item_value).setTextColor(Color.WHITE)
                gridItems[currentIndex].findViewById<TextView>(R.id.grid_item_label).setTextColor(Color.WHITE)
            }

            override fun onFinish() {
                gridItems.forEach {
                    it.setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.border_color))
                    it.findViewById<TextView>(R.id.grid_item_value).setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_color))
                    it.findViewById<TextView>(R.id.grid_item_label).setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_muted))
                }
                val winnerIndex = (0 until gridItems.size).random()
                val winnerView = gridItems[winnerIndex]
                val winnerValueText = winnerView.findViewById<TextView>(R.id.grid_item_value).text.toString()
                val prizeValue = winnerValueText.toIntOrNull() ?: 0
                winnerView.setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.pass_color))
                winnerView.findViewById<TextView>(R.id.grid_item_value).setTextColor(Color.WHITE)
                winnerView.findViewById<TextView>(R.id.grid_item_label).setTextColor(Color.WHITE)

                if (prizeValue > 0) {
                    addTicketsToPlayer(prizeValue)
                }
                resetSpinButton()
            }
        }.start()
    }

    private fun resetSpinButton() {
        isSpinning = false
        binding.sectionHome.spinButton.isEnabled = true
        binding.sectionHome.spinButton.text = "🎟️ ¡GIRAR RULETA!"
        binding.sectionHome.spinButton.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#00a8ff"))
    }

    private fun updateSpinCountUI() {
        binding.sectionHome.spinsAvailable.text = currentSpins.toString()
        binding.sectionHome.spinsAvailable.setTextColor(Color.parseColor("#FFD700"))
        binding.sectionHome.getSpinsButton.visibility = if (currentSpins <= 0) View.VISIBLE else View.GONE

        if (currentSpins <= 0) {
            binding.sectionHome.getSpinsButton.setTextColor(Color.WHITE)
        }
    }

    // --- Lógica de Datos del Jugador ---

    private fun startWeeklyCountdown() {
        countdownTimer?.cancel()
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        if (calendar.timeInMillis < now) {
            calendar.add(Calendar.WEEK_OF_YEAR, 1)
        }
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val diff = calendar.timeInMillis - now

        countdownTimer = object : CountDownTimer(diff, 60000) {
            override fun onTick(millisUntilFinished: Long) {
                val days = TimeUnit.MILLISECONDS.toDays(millisUntilFinished)
                val hours = TimeUnit.MILLISECONDS.toHours(millisUntilFinished) % 24
                val minutes = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished) % 60
                val tiempoTexto = String.format("%dd %dh %dm", days, hours, minutes)

                binding.sectionHome.countdown.text = tiempoTexto
                binding.sectionHome.countdown.setTextColor(Color.WHITE)
            }
            override fun onFinish() {
                binding.sectionHome.countdown.text = "¡Finalizado!"
                binding.sectionHome.countdown.setTextColor(Color.parseColor("#FFD700"))
            }
        }.start()
    }

    private fun signInAnonymously() {
        auth.signInAnonymously().addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                Log.d("Firebase Auth", "signInAnonymously:success")
                loadInitialData()
            } else {
                Log.w("Firebase Auth", "signInAnonymously:failure", task.exception)
            }
        }
    }

    private fun loadInitialData() {
        val savedPlayerId = SessionManager.getPlayerId(this)
        if (savedPlayerId.isNotEmpty()) {
            currentPlayerId = savedPlayerId
            binding.sectionHome.myPlayerId.text = currentPlayerId
            binding.sectionHome.myPlayerId.setTextColor(Color.parseColor("#00A8FF"))
            fetchPlayerData(currentPlayerId!!)
            fetchAllData()
            createFirebaseSession(savedPlayerId)
            obtenerTop5Firebase()

            checkForPrivateMessages(savedPlayerId)

            setupFCMToken(savedPlayerId)
            setupNotificationListener(savedPlayerId)
        } else {
            binding.sectionHome.myPlayerId.text = "Toca para configurar ✏️"
            binding.sectionHome.myPlayerId.setTextColor(Color.parseColor("#CCCCCC"))
            fetchAllData()
            obtenerTop5Firebase()
        }
        showSection(binding.sectionHome.root)

        arreglarColoresDeTextos()
    }

    @Suppress("UNCHECKED_CAST")
    private fun checkForPrivateMessages(playerId: String) {
        val messageRef = database.child("privateMessages").child(playerId)
        messageRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val messageData = snapshot.value as? Map<String, Any>
                    if (messageData != null) {
                        val type = messageData["type"] as? String
                        val message = messageData["message"] as? String

                        Log.d("PRIVATE_MESSAGE", "Mensaje recibido - Tipo: $type, Mensaje: $message")
                        // ✅ SOLO PROCESAR MENSAJES QUE NO SEAN DE SORTEO
                        when (type) {
                            "win", "loss" -> {
                                // NO HACER NADA - Deja que processNotification() lo maneje
                                Log.d("PRIVATE_MESSAGE", "Mensaje de sorteo ignorado, será procesado por notificationQueue")
                            }
                            else -> showGeneralModal("Notificación", message ?: "Tienes un mensaje nuevo.")
                        }
                        messageRef.removeValue()
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("PRIVATE_MESSAGE", "Error escuchando mensajes: ${error.message}")
            }
        })
    }


    private fun showWinnerModal(message: String) {
        try {
            MaterialDialog(this).show {
                title(text = "🎉 ¡FELICITACIONES! 🎉")
                message(text = message)
                positiveButton(text = "¡INCREÍBLE!") {
                    showCelebrationEffects()
                }
                cancelable(false)
            }

            try {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                    vibratorManager.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    getSystemService(VIBRATOR_SERVICE) as Vibrator
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 100, 500, 100, 500), -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(longArrayOf(0, 500, 100, 500, 100, 500), -1)
                }
            } catch (e: Exception) {
                Log.d("VIBRATION", "No se pudo vibrar: ${e.message}")
            }

        } catch (e: Exception) {
            Log.e("WINNER_MODAL", "Error mostrando modal de ganador: ${e.message}")
        }
    }

    private fun showLoserModal(message: String) {
        try {
            MaterialDialog(this).show {
                title(text = "🎭 Sorteo Finalizado")
                message(text = message)
                positiveButton(text = "Entendido")
                cancelable(true)
            }
        } catch (e: Exception) {
            Log.e("LOSER_MODAL", "Error mostrando modal de sorteo: ${e.message}")
        }
    }

    private fun showGeneralModal(title: String, message: String) {
        try {
            MaterialDialog(this).show {
                title(text = title)
                message(text = message)
                positiveButton(text = "OK")
                cancelable(true)
            }
        } catch (e: Exception) {
            Log.e("GENERAL_MODAL", "Error mostrando modal general: ${e.message}")
        }
    }


    private fun showCelebrationEffects() {
        try {
            binding.sectionHome.spinButton.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FFD700"))
            binding.sectionHome.spinButton.text = "🏆 ¡ERES EL GANADOR! 🏆"

            binding.sectionHome.spinButton.postDelayed({
                binding.sectionHome.spinButton.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#00a8ff"))
                binding.sectionHome.spinButton.text = "🎟️ ¡GIRAR RULETA!"
            }, 5000)

        } catch (e: Exception) {
            Log.e("CELEBRATION", "Error en efectos de celebración: ${e.message}")
        }
    }

    private fun promptForPlayerId() {
        MaterialDialog(this).show {
            title(text = if (currentPlayerId != null) "Editar ID de Jugador" else "Configura tu ID")
            input(
                hint = "Ingresa tu ID numérico",
                prefill = currentPlayerId ?: "",
                inputType = InputType.TYPE_CLASS_NUMBER
            ) { _, text ->
                val newPlayerId = text.toString()
                if (newPlayerId.length < 5 || !newPlayerId.matches(Regex("\\d+"))) {
                    // Toast eliminado
                } else {
                    if (currentPlayerId != null && currentPlayerId!!.isNotEmpty()) {
                        androidx.appcompat.app.AlertDialog.Builder(this@MainActivity)
                            .setTitle("Confirmar cambio de ID")
                            .setMessage("¿Estás seguro de cambiar el ID de jugador de $currentPlayerId a $newPlayerId?")
                            .setPositiveButton("Sí") { _, _ ->
                                SessionManager.clearAllData(this@MainActivity)
                                SessionManager.setPlayerId(this@MainActivity, newPlayerId)
                                currentPlayerId = newPlayerId
                                binding.sectionHome.myPlayerId.text = newPlayerId
                                loadUserData(newPlayerId)
                            }
                            .setNegativeButton("Cancelar", null)
                            .show()
                    } else {
                        SessionManager.setPlayerId(this@MainActivity, newPlayerId)
                        currentPlayerId = newPlayerId
                        binding.sectionHome.myPlayerId.text = newPlayerId
                        loadUserData(newPlayerId)
                    }
                }
            }
            positiveButton(text = "Guardar")
            negativeButton(text = "Cancelar")
        }
    }


    private fun loadUserData(playerId: String) {
        createFirebaseSession(playerId)
        fetchPlayerData(playerId)
        obtenerTop5Firebase()
        updateUI(0, 0)
        currentSpins = 10
        updateSpinCountUI()

        checkForPrivateMessages(playerId)
        setupFCMToken(playerId)
        setupNotificationListener(playerId)
    }

    private fun fetchAllData() {
        database.child("winners").orderByChild("timestamp").limitToLast(10).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val winners = snapshot.children.mapNotNull { it.getValue(Winner::class.java) }.sortedByDescending { it.timestamp }
                binding.sectionWinners.winnersRecyclerView.adapter = WinnersAdapter(winners)
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("FETCH_DATA", "Error obteniendo winners: ${error.message}")
            }
        })
    }

    private fun fetchPlayerData(playerId: String) {
        val playerNode = database.child("players").child(playerId)
        playerNode.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val tickets = snapshot.child("tickets").getValue(Long::class.java) ?: 0L
                    val passes = snapshot.child("passes").getValue(Long::class.java) ?: 0L
                    updateUI(tickets, passes)
                } else {
                    val newPlayerData = mapOf("tickets" to 0, "passes" to 0, "lastUpdate" to ServerValue.TIMESTAMP)
                    playerNode.setValue(newPlayerData)
                    updateUI(0, 0)
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("PLAYER_DATA", "Error: ${error.message}")
            }
        })
    }

    private fun addTicketsToPlayer(amount: Int) {
        if (currentPlayerId == null) return
        val playerNode = database.child("players").child(currentPlayerId!!)
        playerNode.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val currentTickets = currentData.child("tickets").getValue(Long::class.java) ?: 0L
                val newTickets = currentTickets + amount
                val newPasses = newTickets / 1000
                currentData.child("tickets").value = newTickets
                currentData.child("passes").value = newPasses
                currentData.child("lastUpdate").value = ServerValue.TIMESTAMP
                return Transaction.success(currentData)
            }
            override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                if (committed) {
                    obtenerTop5Firebase()
                }
            }
        })
    }

    private fun updateUI(tickets: Long, passes: Long) {
        binding.sectionHome.myTickets.text = tickets.toString()
        binding.sectionHome.myPasses.text = passes.toString()
        val progress = ((tickets % 1000).toDouble() / 1000.0 * 100).toInt()
        binding.sectionHome.passProgress.progress = progress

        binding.sectionHome.myTickets.setTextColor(Color.parseColor("#FFD700"))
        binding.sectionHome.myPasses.setTextColor(Color.parseColor("#F97316"))

        Log.d("UI_UPDATE", "UI actualizada con colores mejorados")
    }

    // --- Ciclo de Vida y Sesiones ---

    override fun onResume() {
        super.onResume()
        currentPlayerId?.let { playerId ->
            checkForPrivateMessages(playerId)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        marcarSesionInactiva()
    }

    private fun marcarSesionInactiva() {
        val playerId = SessionManager.getPlayerId(this)
        val sessionId = SessionManager.getSessionId(this)
        if (playerId.isNotEmpty() && sessionId.isNotEmpty()) {
            FirebaseDatabase.getInstance()
                .getReference("sessions/$playerId/$sessionId/active")
                .setValue(false)
        }
    }

    // --- 🔔 SISTEMA DE NOTIFICACIONES MEJORADO ---
    private fun setupNotificationListener(playerId: String) {
        Log.d("NOTIFICATION_SETUP", "Configurando listener para: $playerId")
        val notificationRef = database.child("notificationQueue").child(playerId)

        notificationRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                Log.d("NOTIFICATION_LISTENER", "Nueva notificación detectada: ${snapshot.key}")
                processNotification(snapshot)
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}

            override fun onCancelled(error: DatabaseError) {
                Log.e("NOTIFICATION_LISTENER", "Error escuchando notificaciones: ${error.message}")
            }
        })
    }

    @Suppress("UNCHECKED_CAST")
    private fun processNotification(snapshot: DataSnapshot) {
        try {
            Log.d("NOTIFICATION_PROCESS", "Procesando snapshot: ${snapshot.value}")
            val notificationData = snapshot.value as? Map<String, Any> ?: return
            val processed = notificationData["processed"] as? Boolean ?: false

            if (processed) {
                Log.d("NOTIFICATION_PROCESS", "Notificación ya procesada, ignorando")
                return
            }

            val type = notificationData["type"] as? String ?: "general"
            val title = notificationData["title"] as? String ?: "Notificación"
            val body = notificationData["body"] as? String ?: ""
            val message = notificationData["message"] as? String ?: ""

            Log.d("NOTIFICATION_PROCESS", "Procesando notificación: $type - $title")

            // ✅ MOSTRAR NOTIFICACIÓN PUSH LOCAL CON REDIRECCIÓN
            showLocalNotification(title, body, type)

            // ✅ MOSTRAR MODAL CON REDIRECCIÓN A GANADORES
            runOnUiThread {
                when (type) {
                    "win" -> showWinnerModalWithRedirect(message)
                    "loss" -> showLoserModalWithRedirect(message)
                    "test" -> showGeneralModal("Prueba", message)
                    else -> showGeneralModal(title, message)
                }
            }

            snapshot.ref.removeValue()
                .addOnSuccessListener {
                    Log.d("NOTIFICATION_PROCESS", "Notificación eliminada exitosamente")
                }
                .addOnFailureListener { error ->
                    Log.e("NOTIFICATION_PROCESS", "Error eliminando notificación: ${error.message}")
                }

        } catch (e: Exception) {
            Log.e("NOTIFICATION_PROCESS", "Error procesando notificación: ${e.message}")
        }
    }

    private fun showLocalNotification(title: String, body: String, type: String = "general") {
        try {
            Log.d("LOCAL_NOTIFICATION", "Creando notificación: $title - $body")
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            // Verificar si las notificaciones están habilitadas
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                if (!notificationManager.areNotificationsEnabled()) {
                    Log.w("LOCAL_NOTIFICATION", "Las notificaciones están deshabilitadas por el usuario")
                    return
                }
            }

            // Seleccionar canal según el tipo
            val channelId = when (type) {
                "win" -> "winner_notifications"
                else -> "general_notifications"
            }
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                if (type == "win" || type == "loss") {
                    putExtra("redirect_to", "winners")
                }
            }
            val pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                else PendingIntent.FLAG_UPDATE_CURRENT
            )
            val notification = androidx.core.app.NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setDefaults(androidx.core.app.NotificationCompat.DEFAULT_ALL)
                .setVibrate(longArrayOf(0, 500, 100, 500))
                .setColor(ContextCompat.getColor(this, R.color.accent_color))
                .build()
            val notificationId = System.currentTimeMillis().toInt()
            notificationManager.notify(notificationId, notification)
            Log.d("LOCAL_NOTIFICATION", "Notificación enviada con ID: $notificationId")
        } catch (e: Exception) {
            Log.e("LOCAL_NOTIFICATION", "Error mostrando notificación local: ${e.message}")
        }
    }


    private fun showWinnerModalWithRedirect(message: String) {
        try {
            MaterialDialog(this).show {
                title(text = "🎉 ¡FELICITACIONES! 🎉")
                message(text = message)
                positiveButton(text = "VER GANADORES") {
                    showCelebrationEffects()
                    redirectToWinnersSection()
                }
                cancelable(false)
            }
            // Vibración de celebración
            try {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                    vibratorManager.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    getSystemService(VIBRATOR_SERVICE) as Vibrator
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 100, 500, 100, 500), -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(longArrayOf(0, 500, 100, 500, 100, 500), -1)
                }
            } catch (e: Exception) {
                Log.d("VIBRATION", "No se pudo vibrar: ${e.message}")
            }
        } catch (e: Exception) {
            Log.e("WINNER_MODAL", "Error mostrando modal de ganador: ${e.message}")
        }
    }

    private fun showLoserModalWithRedirect(message: String) {
        try {
            MaterialDialog(this).show {
                title(text = "🎭 Sorteo Finalizado")
                message(text = message)
                positiveButton(text = "VER GANADORES") {
                    redirectToWinnersSection()
                }
                cancelable(true)
            }
        } catch (e: Exception) {
            Log.e("LOSER_MODAL", "Error mostrando modal de sorteo: ${e.message}")
        }
    }


    private fun redirectToWinnersSection() {
        hideAllSections()
        showSection(binding.sectionWinners.root)
        binding.navView.setCheckedItem(R.id.nav_winners)
    }

    // --- 🔔 GESTIÓN DE PERMISOS DE NOTIFICACIÓN ---
    private fun checkNotificationPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.d("NOTIFICATION_PERMISSION", "Solicitando permiso de notificaciones")
                requestNotificationPermission()
            } else {
                Log.d("NOTIFICATION_PERMISSION", "Permiso de notificaciones ya concedido")
                createNotificationChannels()
            }
        } else {
            Log.d("NOTIFICATION_PERMISSION", "Android < 13, no necesita permiso explícito")
            createNotificationChannels()
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                )
            ) {
                // Mostrar explicación de por qué necesitamos el permiso
                MaterialDialog(this).show {
                    title(text = "Activar Notificaciones")
                    message(text = "Necesitamos activar las notificaciones para informarte cuando:\n\n• Ganes el sorteo semanal\n• Hay nuevos concursos\n• Recibes premios especiales\n\n¿Deseas activar las notificaciones?")
                    positiveButton(text = "Activar") {
                        ActivityCompat.requestPermissions(
                            this@MainActivity,
                            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                            NOTIFICATION_PERMISSION_REQUEST_CODE
                        )
                    }
                    negativeButton(text = "Ahora no") {
                        Log.d("NOTIFICATION_PERMISSION", "Usuario rechazó activar notificaciones")
                        createNotificationChannels() // Crear canales de todas formas
                    }
                }
            } else {
                // Solicitar permiso directamente
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            NOTIFICATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("NOTIFICATION_PERMISSION", "Permiso de notificaciones concedido")
                    createNotificationChannels()
                } else {
                    Log.d("NOTIFICATION_PERMISSION", "Permiso de notificaciones denegado")
                    createNotificationChannels() // Crear canales de todas formas
                }
            }
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Canal para notificaciones de ganadores
            val winnerChannel = NotificationChannel(
                "winner_notifications",
                "Notificaciones de Ganadores",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones cuando ganas el sorteo"
                enableLights(true)
                lightColor = Color.parseColor("#FFD700")
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 500, 1000)
                setSound(
                    android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION),
                    null
                )
            }

            // Canal para notificaciones generales
            val generalChannel = NotificationChannel(
                "general_notifications",
                "Notificaciones Generales",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificaciones generales del club de recompensas"
                enableLights(true)
                lightColor = Color.parseColor("#00A8FF")
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(winnerChannel)
            notificationManager.createNotificationChannel(generalChannel)

            Log.d("NOTIFICATION_CHANNELS", "Canales de notificación creados")
        }
    }

    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
    }
}