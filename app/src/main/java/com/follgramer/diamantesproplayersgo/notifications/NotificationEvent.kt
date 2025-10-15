package com.follgramer.diamantesproplayersgo.notifications

sealed interface NotificationEvent  // DEBE ser sealed interface

data class WinnerEvent(val message: String) : NotificationEvent
data class LoserEvent(val message: String) : NotificationEvent
data class GiftEvent(val amount: String, val unit: String, val message: String) : NotificationEvent
data class BanEvent(val banType: String, val reason: String, val expiresAt: Long) : NotificationEvent
data object UnbanEvent : NotificationEvent  // data object, no solo object
data class GeneralEvent(val title: String, val message: String) : NotificationEvent
data object CounterUpdatedEvent : NotificationEvent  // data object, no solo object// Updated: 2025-10-15 14:29:27
