package com.example.services

import android.util.Log
import com.example.OptixApplication
import com.example.data.entity.BusinessProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object BusinessResetManager {
    private val scope = CoroutineScope(Dispatchers.IO)

    @Synchronized
    fun checkAndResetIfRequired(profile: BusinessProfile?, onTokenResetNeeded: (() -> Unit)? = null) {
        if (profile == null) return

        val status = BusinessClock.calculateStatus(profile)
        val targetBusinessDate = status.businessDate.toString()

        // Reset Condition:
        // Business is closed AND lastResetBusinessDate does not match target business date
        if (status.isBusinessClosed && profile.lastResetBusinessDate != targetBusinessDate) {
            Log.d("OPTIX_FLOW", "[BUSINESS RESET REQUIRED] Closing time reached. Target Date: $targetBusinessDate, Last Reset Date: ${profile.lastResetBusinessDate}")
            executeReset(profile, targetBusinessDate, onTokenResetNeeded)
        }
    }

    private fun executeReset(profile: BusinessProfile, targetBusinessDate: String, onTokenResetNeeded: (() -> Unit)?) {
        scope.launch {
            try {
                val app = OptixApplication.instance
                val profileRepo = app.businessProfileRepository

                val existing = profileRepo.getProfileSync() ?: profile
                // Idempotency check inside lock
                if (existing.lastResetBusinessDate == targetBusinessDate) {
                    Log.d("OPTIX_FLOW", "[BUSINESS RESET SKIPPED] Date $targetBusinessDate already reset (Exactly-Once Guard)")
                    return@launch
                }

                // 1. Update Room DB BusinessProfile atomically
                val updated = existing.copy(lastResetBusinessDate = targetBusinessDate)
                profileRepo.saveProfile(updated)

                // 2. Trigger local token counter reset
                onTokenResetNeeded?.invoke()

                Log.d("OPTIX_FLOW", "[BUSINESS RESET EXECUTED LOCAL] Room DB updated lastResetBusinessDate: $targetBusinessDate")

                // 3. Notify Cloud Repository to execute cloud reset & emit WebSocket event
                app.cloudRepository.resetBusinessDay(targetBusinessDate)
            } catch (e: Exception) {
                Log.e("OPTIX_FLOW", "[BUSINESS RESET ERR] Failed to execute reset: ${e.message}")
            }
        }
    }
}
