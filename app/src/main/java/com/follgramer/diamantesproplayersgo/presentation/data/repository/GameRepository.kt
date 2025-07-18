package com.follgramer.diamantesproplayersgo.data.repository

import com.follgramer.diamantesproplayersgo.domain.model.User
import com.follgramer.diamantesproplayersgo.domain.model.Winner
import com.follgramer.diamantesproplayersgo.data.remote.FirebaseRepository
import kotlinx.coroutines.delay

class GameRepository {

    private val firebaseRepository = FirebaseRepository()

    // Simulamos datos para prueba
    private var currentUser = User(
        playerId = "test_player_001",
        userName = "Jugador Pro",
        tickets = 150,
        passes = 3,
        totalSpins = 25,
        lastSpinTime = System.currentTimeMillis() - 3600000, // 1 hour ago
        createdAt = System.currentTimeMillis() - 86400000 * 7, // 1 week ago
        updatedAt = System.currentTimeMillis()
    )

    suspend fun getPlayerData(): User {
        // Simulate network delay
        delay(500)
        return currentUser
    }

    suspend fun getWinners(): List<Winner> {
        // Simulate network delay
        delay(300)
        return listOf(
            Winner(
                id = "1",
                winnerId = "Pro***87",
                prize = "🏆 Pase Élite",
                date = "Hace 2 horas",
                timestamp = System.currentTimeMillis() - 7200000
            ),
            Winner(
                id = "2",
                winnerId = "Gam***23",
                prize = "💎 500 Diamantes",
                date = "Hace 5 horas",
                timestamp = System.currentTimeMillis() - 18000000
            ),
            Winner(
                id = "3",
                winnerId = "Win***45",
                prize = "🎫 10 Tickets",
                date = "Hace 1 día",
                timestamp = System.currentTimeMillis() - 86400000
            ),
            Winner(
                id = "4",
                winnerId = "Luc***12",
                prize = "🏆 Pase Premium",
                date = "Hace 2 días",
                timestamp = System.currentTimeMillis() - 172800000
            ),
            Winner(
                id = "5",
                winnerId = "Sup***89",
                prize = "💎 1000 Diamantes",
                date = "Hace 3 días",
                timestamp = System.currentTimeMillis() - 259200000
            )
        )
    }

    suspend fun addTickets(amount: Int) {
        // Simulate network delay
        delay(200)
        currentUser = currentUser.copy(
            tickets = currentUser.tickets + amount,
            updatedAt = System.currentTimeMillis()
        )
    }

    suspend fun updateUserData(user: User): Result<Unit> {
        return try {
            delay(300)
            currentUser = user.copy(updatedAt = System.currentTimeMillis())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // NUEVOS MÉTODOS AGREGADOS:
    suspend fun useTicket(): Result<Unit> {
        return try {
            delay(200)
            if (currentUser.tickets > 0) {
                currentUser = currentUser.copy(
                    tickets = currentUser.tickets - 1,
                    totalSpins = currentUser.totalSpins + 1,
                    lastSpinTime = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                Result.success(Unit)
            } else {
                Result.failure(Exception("No tickets available"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addPasses(amount: Int): Result<Unit> {
        return try {
            delay(200)
            currentUser = currentUser.copy(
                passes = currentUser.passes + amount,
                updatedAt = System.currentTimeMillis()
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun canBuyTicket(): Boolean {
        // For now, always allow ticket purchase
        // In real implementation, check if user has enough diamonds/currency
        return true
    }

    suspend fun buyTicket(): Result<Unit> {
        return try {
            delay(200)
            // In real implementation, deduct currency and add ticket
            currentUser = currentUser.copy(
                tickets = currentUser.tickets + 1,
                updatedAt = System.currentTimeMillis()
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLeaderboard(): List<User> {
        delay(500)
        return listOf(
            User(
                playerId = "leader1",
                userName = "Pro***87",
                tickets = 2500,
                passes = 15,
                totalSpins = 150
            ),
            User(
                playerId = "leader2",
                userName = "Gam***23",
                tickets = 2200,
                passes = 12,
                totalSpins = 130
            ),
            User(
                playerId = "leader3",
                userName = "Win***45",
                tickets = 1800,
                passes = 10,
                totalSpins = 110
            ),
            User(
                playerId = "leader4",
                userName = "Luc***12",
                tickets = 1500,
                passes = 8,
                totalSpins = 95
            ),
            User(
                playerId = "leader5",
                userName = "Sup***89",
                tickets = 1200,
                passes = 6,
                totalSpins = 80
            )
        )
    }
}