package com.follgramer.diamantesproplayersgo

import android.content.Context
import android.util.Log
import com.google.firebase.database.*
import kotlinx.coroutines.*

class CommunicationManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "CommunicationManager"
        @Volatile private var INSTANCE: CommunicationManager? = null

        fun getInstance(context: Context): CommunicationManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CommunicationManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    private val database = FirebaseDatabase.getInstance(
        "https://diamantes-pro-players-go-f3510-default-rtdb.firebaseio.com/"
    ).reference

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Listeners
    private var sorteoListener: ValueEventListener? = null
    private var sorteoRef: DatabaseReference? = null
    private var notificationListener: ChildEventListener? = null
    private var notificationRef: DatabaseReference? = null
    private var dataListener: ValueEventListener? = null
    private var dataRef: DatabaseReference? = null

    // Estado actual del sorteo
    private var currentSorteoState: SorteoState? = null

    // Job para countdown
    private var countdownJob: Job? = null

    data class SorteoState(
        val hideCountdown: Boolean = false,
        val customMessage: String = "",
        val paused: Boolean = false,
        val revealState: RevealState? = null
    )

    data class RevealState(
        val status: String = "",
        val winnerId: String = "",
        val prize: String = "",
        val revealTime: Long = 0L,
        val revealedAt: Long? = null
    )

    fun setupSorteoListener(
        onCountdownUpdate: (status: String, message: String) -> Unit,
        onRevealCountdown: (timeLeft: Long, winnerId: String, prize: String) -> Unit,
        onWinnerRevealed: (winnerId: String, prize: String) -> Unit,
        onReset: () -> Unit
    ) {
        cleanupSorteoListener()

        sorteoRef = database.child("appConfig").child("sorteo")
        sorteoListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    Log.d(TAG, "Sorteo data changed")

                    if (!snapshot.exists()) {
                        currentSorteoState = null
                        onReset()
                        return
                    }

                    val hideCountdown = snapshot.child("hideCountdown").getValue(Boolean::class.java) ?: false
                    val customMessage = snapshot.child("customMessage").getValue(String::class.java) ?: ""
                    val paused = snapshot.child("paused").getValue(Boolean::class.java) ?: false

                    var revealState: RevealState? = null
                    val revealSnapshot = snapshot.child("revealState")
                    if (revealSnapshot.exists()) {
                        revealState = RevealState(
                            status = revealSnapshot.child("status").getValue(String::class.java) ?: "",
                            winnerId = revealSnapshot.child("winnerId").getValue(String::class.java) ?: "",
                            prize = revealSnapshot.child("prize").getValue(String::class.java) ?: "",
                            revealTime = revealSnapshot.child("revealTime").getValue(Long::class.java) ?: 0L,
                            revealedAt = revealSnapshot.child("revealedAt").getValue(Long::class.java)
                        )
                    }

                    currentSorteoState = SorteoState(hideCountdown, customMessage, paused, revealState)

                    when {
                        revealState != null -> handleRevealState(
                            revealState,
                            onRevealCountdown,
                            onWinnerRevealed
                        )
                        paused -> onCountdownUpdate("pausado", customMessage)
                        hideCountdown && customMessage.isNotEmpty() -> {
                            onCountdownUpdate("procesando", customMessage)
                        }
                        else -> onReset()
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "Error procesando sorteo: ${e.message}", e)
                    onReset()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error en listener sorteo: ${error.message}")
                onReset()
            }
        }

        sorteoRef?.addValueEventListener(sorteoListener!!)
        Log.d(TAG, "Sorteo listener configurado")
    }

    private fun handleRevealState(
        revealState: RevealState,
        onRevealCountdown: (timeLeft: Long, winnerId: String, prize: String) -> Unit,
        onWinnerRevealed: (winnerId: String, prize: String) -> Unit
    ) {
        // Cancelar countdown previo
        countdownJob?.cancel()

        when (revealState.status) {
            "countdown" -> {
                val now = System.currentTimeMillis()
                val timeLeft = revealState.revealTime - now

                if (timeLeft > 0) {
                    onRevealCountdown(timeLeft, revealState.winnerId, revealState.prize)

                    // Countdown con coroutine
                    countdownJob = scope.launch {
                        while (isActive && System.currentTimeMillis() < revealState.revealTime) {
                            val remaining = revealState.revealTime - System.currentTimeMillis()
                            withContext(Dispatchers.Main) {
                                onRevealCountdown(remaining, revealState.winnerId, revealState.prize)
                            }
                            delay(1000)
                        }

                        // Cuando termina
                        withContext(Dispatchers.Main) {
                            onWinnerRevealed(revealState.winnerId, revealState.prize)
                        }
                    }
                } else {
                    onWinnerRevealed(revealState.winnerId, revealState.prize)
                }
            }
            "revealing" -> {
                onRevealCountdown(0, revealState.winnerId, revealState.prize)
            }
            "revealed" -> {
                onWinnerRevealed(revealState.winnerId, revealState.prize)
            }
        }
    }

    fun startNotificationListener(playerId: String) {
        cleanupNotificationListener()

        if (playerId.isEmpty()) {
            Log.w(TAG, "No se puede iniciar listener sin playerId")
            return
        }

        notificationRef = database.child("notificationQueue").child(playerId)
        notificationListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                processDirectNotification(snapshot)
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                processDirectNotification(snapshot)
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error en notification listener: ${error.message}")
            }
        }

        notificationRef?.addChildEventListener(notificationListener!!)
        Log.d(TAG, "Notification listener iniciado para: $playerId")
    }

    private fun processDirectNotification(snapshot: DataSnapshot) {
        scope.launch {
            try {
                val data = snapshot.value as? Map<String, Any> ?: return@launch
                val processed = data["processed"] as? Boolean ?: false

                if (!processed) {
                    Log.d(TAG, "Procesando notificación: ${data["type"]}")
                    snapshot.ref.child("processed").setValue(true)

                    delay(3000)
                    snapshot.ref.removeValue()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error procesando notificación: ${e.message}")
            }
        }
    }

    fun setupDataSyncListener(
        playerId: String,
        onDataUpdate: (tickets: Long, passes: Long, spins: Long) -> Unit
    ) {
        cleanupDataListener()

        if (playerId.isEmpty()) return

        dataRef = database.child("players").child(playerId)
        dataListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    if (snapshot.exists()) {
                        val tickets = snapshot.child("tickets").getValue(Long::class.java) ?: 0L
                        val passes = snapshot.child("passes").getValue(Long::class.java) ?: 0L
                        val spins = snapshot.child("spins").getValue(Long::class.java) ?: 0L

                        onDataUpdate(tickets, passes, spins)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error en data sync: ${e.message}")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error en data listener: ${error.message}")
            }
        }

        dataRef?.addValueEventListener(dataListener!!)
        Log.d(TAG, "Data sync listener configurado para: $playerId")
    }

    private fun cleanupSorteoListener() {
        countdownJob?.cancel()
        countdownJob = null
        sorteoListener?.let { listener ->
            sorteoRef?.removeEventListener(listener)
        }
        sorteoListener = null
        sorteoRef = null
    }

    private fun cleanupNotificationListener() {
        notificationListener?.let { listener ->
            notificationRef?.removeEventListener(listener)
        }
        notificationListener = null
        notificationRef = null
    }

    private fun cleanupDataListener() {
        dataListener?.let { listener ->
            dataRef?.removeEventListener(listener)
        }
        dataListener = null
        dataRef = null
    }

    fun cleanup() {
        cleanupSorteoListener()
        cleanupNotificationListener()
        cleanupDataListener()
        scope.cancel()
        currentSorteoState = null
        Log.d(TAG, "CommunicationManager limpiado")
    }

    fun getCurrentSorteoState(): SorteoState? = currentSorteoState

    fun checkConnection(callback: (Boolean) -> Unit) {
        val connectedRef = database.child(".info/connected")
        connectedRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                callback(connected)
            }

            override fun onCancelled(error: DatabaseError) {
                callback(false)
            }
        })
    }
}