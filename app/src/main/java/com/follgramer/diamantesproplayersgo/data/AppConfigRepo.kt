package com.follgramer.diamantesproplayersgo.data

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

object AppConfigRepo {
    private val database = FirebaseDatabase.getInstance("https://diamantes-pro-players-go-f3510-default-rtdb.firebaseio.com/").reference

    data class AdminModal(
        val id: String? = null,
        val active: Boolean = false,
        val title: String? = null,
        val message: String? = null,
        val ctaText: String? = null,
        val ctaUrl: String? = null,
        val dismissible: Boolean? = true,
        val minVersionCode: Int? = null
    )

    data class UpdatesConfig(
        val minVersionCode: Int = 0,
        val preferImmediate: Boolean = false,
        val fallbackUrl: String = ""
    )

    data class ReviewConfig(
        val enabled: Boolean = true,
        val everyLaunches: Int = 10,
        val cooldownDays: Int = 7
    )

    fun getAdminModal(callback: (AdminModal?) -> Unit) {
        database.child("appConfig").child("adminModal")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        val modal = snapshot.getValue(AdminModal::class.java)
                        callback(modal)
                    } catch (e: Exception) {
                        callback(null)
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    callback(null)
                }
            })
    }

    fun getUpdatesCfg(callback: (UpdatesConfig) -> Unit) {
        database.child("appConfig").child("updates")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        val config = snapshot.getValue(UpdatesConfig::class.java) ?: UpdatesConfig()
                        callback(config)
                    } catch (e: Exception) {
                        callback(UpdatesConfig())
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    callback(UpdatesConfig())
                }
            })
    }

    fun getReviewCfg(callback: (ReviewConfig) -> Unit) {
        database.child("appConfig").child("review")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        val config = snapshot.getValue(ReviewConfig::class.java) ?: ReviewConfig()
                        callback(config)
                    } catch (e: Exception) {
                        callback(ReviewConfig())
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    callback(ReviewConfig())
                }
            })
    }
}