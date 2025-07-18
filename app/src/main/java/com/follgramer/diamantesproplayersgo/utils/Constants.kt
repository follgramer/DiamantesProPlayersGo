package com.follgramer.diamantesproplayersgo.utils

object Constants {

    // AdMob Test IDs
    const val ADMOB_APP_ID = "ca-app-pub-3940256099942544~3347511713"
    const val BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    const val REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

    // Game Constants
    const val PASS_GOAL = 1000
    const val INITIAL_SPINS = 10
    const val SPINS_PER_AD = 10
    const val TICKETS_PER_AD = 20

    // Time Constants
    const val INTERSTITIAL_COOLDOWN = 120_000L // 2 minutes
    const val DAILY_SPIN_COOLDOWN = 24 * 60 * 60 * 1000L // 24 hours

    // Firebase Paths
    const val USERS_PATH = "users"
    const val WINNERS_PATH = "winners"
    const val TICKETS_PATH = "tickets"
    const val LEADERBOARD_PATH = "leaderboard"

    // Roulette Prizes
    val ROULETTE_PRIZES = arrayOf(
        5, 1, 10, 25, 20, 0, 50, 5, 15, 1, 100, 20
    )

    val ROULETTE_LABELS = arrayOf(
        "tickets", "ticket", "tickets", "tickets", "tickets", "nada",
        "tickets", "tickets", "tickets", "ticket", "tickets", "tickets"
    )
}