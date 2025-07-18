package com.follgramer.diamantesproplayersgo.data.remote

import com.follgramer.diamantesproplayersgo.domain.model.User
import com.follgramer.diamantesproplayersgo.domain.model.Winner
import com.follgramer.diamantesproplayersgo.domain.model.Ticket
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class FirebaseRepository {

    private val database = Firebase.database.reference

    private val _tickets = MutableStateFlow<List<Ticket>>(emptyList())
    val tickets: StateFlow<List<Ticket>> = _tickets.asStateFlow()

    private val _winners = MutableStateFlow<List<Winner>>(emptyList())
    val winners: StateFlow<List<Winner>> = _winners.asStateFlow()

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()

    private val _loadingState = MutableStateFlow(false)
    val loadingState: StateFlow<Boolean> = _loadingState.asStateFlow()

    init {
        observeWinners()
        observeUsers()
    }

    private fun observeWinners() {
        database.child("winners").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val winnerList = mutableListOf<Winner>()
                    snapshot.children.forEach { childSnapshot ->
                        childSnapshot.getValue<Winner>()?.let { winner ->
                            winnerList.add(winner)
                        }
                    }
                    _winners.value = winnerList.sortedByDescending { it.timestamp }
                } catch (e: Exception) {
                    // Handle error
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
    }

    private fun observeUsers() {
        database.child("users").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val userList = mutableListOf<User>()
                    snapshot.children.forEach { childSnapshot ->
                        childSnapshot.getValue<User>()?.let { user ->
                            userList.add(user)
                        }
                    }
                    _users.value = userList.sortedByDescending { it.passes }
                } catch (e: Exception) {
                    // Handle error
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
    }

    suspend fun updateUser(user: User): Result<Unit> = suspendCoroutine { continuation ->
        database.child("users").child(user.playerId).setValue(user)
            .addOnSuccessListener {
                continuation.resume(Result.success(Unit))
            }
            .addOnFailureListener { exception ->
                continuation.resume(Result.failure(exception))
            }
    }

    suspend fun getUser(playerId: String): Result<User?> = suspendCoroutine { continuation ->
        database.child("users").child(playerId).get()
            .addOnSuccessListener { snapshot ->
                val user = snapshot.getValue<User>()
                continuation.resume(Result.success(user))
            }
            .addOnFailureListener { exception ->
                continuation.resume(Result.failure(exception))
            }
    }

    suspend fun addWinner(winner: Winner): Result<Unit> = suspendCoroutine { continuation ->
        val winnerRef = database.child("winners").push()
        val winnerWithId = winner.copy(
            id = winnerRef.key ?: "",
            timestamp = System.currentTimeMillis()
        )

        winnerRef.setValue(winnerWithId)
            .addOnSuccessListener {
                continuation.resume(Result.success(Unit))
            }
            .addOnFailureListener { exception ->
                continuation.resume(Result.failure(exception))
            }
    }

    suspend fun createTicket(ticket: Ticket): Result<String> = suspendCoroutine { continuation ->
        val newTicketRef = database.child("tickets").push()
        val ticketId = newTicketRef.key ?: return@suspendCoroutine continuation.resume(
            Result.failure(Exception("Error al generar ID del ticket"))
        )

        val ticketWithId = ticket.copy(
            id = ticketId,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        newTicketRef.setValue(ticketWithId)
            .addOnSuccessListener {
                continuation.resume(Result.success(ticketId))
            }
            .addOnFailureListener { exception ->
                continuation.resume(Result.failure(exception))
            }
    }
}