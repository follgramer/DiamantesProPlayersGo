package com.follgramer.diamantesproplayersgo

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.children
import androidx.recyclerview.widget.LinearLayoutManager
import cn.pedant.SweetAlert.SweetAlertDialog
import com.follgramer.diamantesproplayersgo.databinding.ActivityMainBinding
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
    }

    private fun setupToolbarAndDrawer() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        val toggle = ActionBarDrawerToggle(this, binding.drawerLayout, binding.toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        // CORRECCIÓN: Se elimina toda la lógica del footer de aquí.
        // El listener del menú ahora manejará todo de forma nativa.
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
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    // Permitir que el sistema maneje el comportamiento por defecto
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        })
    }

    private fun handleNavigation(menuItem: MenuItem): Boolean {
        hideAllSections()

        // Solo marcar como "seleccionado" los items del grupo principal
        if (menuItem.groupId == R.id.group_main) {
            menuItem.isChecked = true
        }

        when (menuItem.itemId) {
            R.id.nav_home -> showSection(binding.sectionHome.root)
            R.id.nav_tasks -> showInterstitialAd { showSection(binding.sectionTasks.root) }
            R.id.nav_leaderboard -> showInterstitialAd { showSection(binding.sectionLeaderboard.root) }
            R.id.nav_winners -> showInterstitialAd { showSection(binding.sectionWinners.root) }
            R.id.nav_privacy -> {
                binding.sectionLegal.legalTitle.text = "Política de Privacidad"
                binding.sectionLegal.legalContent.text = getString(R.string.privacy_policy_content)
                showSection(binding.sectionLegal.root)
            }
            R.id.nav_terms -> {
                binding.sectionLegal.legalTitle.text = "Términos y Condiciones"
                binding.sectionLegal.legalContent.text = getString(R.string.terms_content)
                showSection(binding.sectionLegal.root)
            }
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
            promptForPlayerId(); return
        }
        if (rewardedAd == null) {
            Toast.makeText(this, "El anuncio no está listo. Intenta de nuevo.", Toast.LENGTH_SHORT).show()
            loadRewardedAd(); return
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
            promptForPlayerId(); return
        }
        if (currentSpins <= 0) {
            SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE).setTitleText("¡Sin Giros!").setContentText("Necesitas más giros para jugar. ¿Quieres ver un video para obtener 10 giros?").setConfirmText("Ver Video").setCancelText("Ahora no").setConfirmClickListener {
                it.dismiss()
                requestSpinsByWatchingAd()
            }.show()
            return
        }

        isSpinning = true
        currentSpins--
        updateSpinCountUI()
        binding.sectionHome.spinButton.isEnabled = false
        binding.sectionHome.spinButton.text = "GIRANDO..."

        object : CountDownTimer(3000, 80) {
            var currentIndex = 0
            val gridItems = binding.sectionHome.gridContainer.children.toList()
            var winnerIndex = -1

            override fun onTick(millisUntilFinished: Long) {
                // Limpiar todos los items
                gridItems.forEach {
                    it.setBackgroundColor(getColor(R.color.border_color))
                    it.findViewById<TextView>(R.id.grid_item_value).setTextColor(getColor(R.color.text_color))
                    it.findViewById<TextView>(R.id.grid_item_label).setTextColor(getColor(R.color.text_muted))
                }

                // Selección aleatoria del índice actual para el efecto visual
                currentIndex = (0 until gridItems.size).random()

                // Resaltar el item actual con color naranja
                gridItems[currentIndex].setBackgroundColor(getColor(R.color.pass_color))
                gridItems[currentIndex].findViewById<TextView>(R.id.grid_item_value).setTextColor(Color.WHITE)
                gridItems[currentIndex].findViewById<TextView>(R.id.grid_item_label).setTextColor(Color.WHITE)
            }

            override fun onFinish() {
                // Limpiar todos los items
                gridItems.forEach {
                    it.setBackgroundColor(getColor(R.color.border_color))
                    it.findViewById<TextView>(R.id.grid_item_value).setTextColor(getColor(R.color.text_color))
                    it.findViewById<TextView>(R.id.grid_item_label).setTextColor(getColor(R.color.text_muted))
                }

                // Seleccionar ganador aleatorio
                winnerIndex = (0 until gridItems.size).random()
                val winnerView = gridItems[winnerIndex]

                // Obtener el valor correcto del ticket ganador
                val winnerValueText = winnerView.findViewById<TextView>(R.id.grid_item_value).text.toString()
                val prizeValue = winnerValueText.toIntOrNull() ?: 0

                // Resaltar ganador con color naranja
                winnerView.setBackgroundColor(getColor(R.color.pass_color))
                winnerView.findViewById<TextView>(R.id.grid_item_value).setTextColor(Color.WHITE)
                winnerView.findViewById<TextView>(R.id.grid_item_label).setTextColor(Color.WHITE)

                // Añadir los tickets correctos
                if (prizeValue > 0) {
                    addTicketsToPlayer(prizeValue)
                    Toast.makeText(this@MainActivity, "¡Ganaste $prizeValue tickets!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "¡Mejor suerte la próxima vez!", Toast.LENGTH_SHORT).show()
                }

                resetSpinButton()
            }
        }.start()
    }

    private fun resetSpinButton() {
        isSpinning = false
        binding.sectionHome.spinButton.isEnabled = true
        binding.sectionHome.spinButton.text = "🎟️ ¡GIRAR RULETA!"
    }

    private fun updateSpinCountUI() {
        binding.sectionHome.spinsAvailable.text = currentSpins.toString()
        binding.sectionHome.getSpinsButton.visibility = if (currentSpins <= 0) View.VISIBLE else View.GONE
    }

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
                binding.sectionHome.countdown.text = String.format("%dd %dh %dm", days, hours, minutes)
            }
            override fun onFinish() {
                binding.sectionHome.countdown.text = "¡Finalizado!"
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
                Toast.makeText(baseContext, "Fallo de autenticación.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadInitialData() {
        val prefs = getSharedPreferences("DiamantesProPrefs", Context.MODE_PRIVATE)
        val savedPlayerId = prefs.getString("PLAYER_ID", null)
        if (!savedPlayerId.isNullOrEmpty()) {
            currentPlayerId = savedPlayerId
            binding.sectionHome.myPlayerId.text = currentPlayerId
            fetchPlayerData(currentPlayerId!!)
            fetchAllData()
        } else {
            binding.sectionHome.myPlayerId.text = "Toca para configurar ✏️"
            fetchAllData()
        }
        showSection(binding.sectionHome.root)
    }

    private fun promptForPlayerId() {
        val editText = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            hint = "Ingresa tu ID numérico"
            setText(currentPlayerId)
        }
        SweetAlertDialog(this, SweetAlertDialog.NORMAL_TYPE)
            .setTitleText(if (currentPlayerId != null) "Editar ID de Jugador" else "Configura tu ID")
            .setCustomView(editText)
            .setConfirmText("Guardar")
            .setConfirmClickListener { sDialog ->
                val newPlayerId = editText.text.toString()
                if (newPlayerId.length < 5 || !newPlayerId.matches(Regex("\\d+"))) {
                    Toast.makeText(this, "Por favor, ingresa un ID válido.", Toast.LENGTH_SHORT).show()
                } else {
                    savePlayerId(newPlayerId)
                    sDialog.dismissWithAnimation()
                }
            }.show()
    }

    private fun savePlayerId(playerId: String) {
        val prefs = getSharedPreferences("DiamantesProPrefs", Context.MODE_PRIVATE).edit()
        prefs.putString("PLAYER_ID", playerId)
        prefs.apply()
        currentPlayerId = playerId
        binding.sectionHome.myPlayerId.text = playerId
        fetchPlayerData(playerId)
        fetchAllData()
        fetchPlayerData(playerId) // cargar datos sin esperar
    }

    private fun fetchAllData() {
        if (auth.currentUser == null) return

        database.child("users").orderByChild("passes").limitToLast(50).addValueEventListener(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val players = snapshot.children.mapNotNull { it.getValue(Player::class.java) }.sortedByDescending { it.passes }
                binding.sectionLeaderboard.leaderboardRecyclerView.adapter = LeaderboardAdapter(players)

                val top5 = players.take(5)
                binding.sectionHome.miniLeaderboardList.adapter = MiniLeaderboardAdapter(top5, currentPlayerId)

                val myRank = players.indexOfFirst { it.playerId == currentPlayerId }
                val myRankLayout = binding.sectionHome.myRankStatus
                if (myRank != -1 && players.isNotEmpty()) {
                    myRankLayout.root.visibility = View.VISIBLE
                    val myPlayerData = players[myRank]
                    myRankLayout.rank.text = "#${myRank + 1}"
                    myRankLayout.playerId.text = "Tú"
                    myRankLayout.passes.text = "${myPlayerData.passes} Pases"
                    myRankLayout.root.setBackgroundResource(R.drawable.background_my_rank_highlight)
                } else {
                    myRankLayout.root.visibility = View.GONE
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        database.child("winners").orderByChild("timestamp").limitToLast(10).addValueEventListener(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val winners = snapshot.children.mapNotNull { it.getValue(Winner::class.java) }.sortedByDescending { it.timestamp }
                binding.sectionWinners.winnersRecyclerView.adapter = WinnersAdapter(winners)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun fetchPlayerData(playerId: String) {
        if (auth.currentUser == null) return
        val userNode = database.child("users").child(auth.currentUser!!.uid)

        userNode.child("playerId").setValue(playerId)

        userNode.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val player = snapshot.getValue(Player::class.java)
                if (player != null) {
                    updateUI(player.tickets, player.passes)
                } else {
                    userNode.setValue(Player(playerId, 0, 0))
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun addTicketsToPlayer(amount: Int) {
        if (currentPlayerId == null || auth.currentUser == null) return

        val userNode = database.child("users").child(auth.currentUser!!.uid)

        userNode.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val player = currentData.getValue(Player::class.java) ?: Player(currentPlayerId!!, 0, 0)

                val newTickets = player.tickets + amount

                player.tickets = newTickets
                player.passes = newTickets / 1000

                currentData.value = player
                return Transaction.success(currentData)
            }
            override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                if (committed) {
                    Log.d("Firebase", "Tickets added successfully.")
                }
            }
        })
    }

    private fun updateUI(tickets: Long, passes: Long) {
        binding.sectionHome.myTickets.text = tickets.toString()
        binding.sectionHome.myPasses.text = passes.toString()
        val progress = ((tickets % 1000).toDouble() / 1000.0 * 100).toInt()
        binding.sectionHome.passProgress.progress = progress
    }
}