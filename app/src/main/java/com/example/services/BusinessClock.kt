package com.example.services

import com.example.data.entity.BusinessProfile
import java.time.*
import java.time.format.DateTimeFormatter

data class BusinessStatus(
    val isBusinessOpen: Boolean,
    val isBusinessClosed: Boolean,
    val currentBusinessTime: LocalDateTime,
    val businessDate: LocalDate,
    val millisecondsUntilClose: Long,
    val millisecondsUntilOpen: Long,
    val nextOpeningTime: LocalDateTime,
    val nextClosingTime: LocalDateTime
)

object BusinessClock {

    fun calculateStatus(
        profile: BusinessProfile,
        nowInstant: Instant = Instant.now()
    ): BusinessStatus {
        val zoneId = try {
            ZoneId.of(profile.timezone.ifBlank { "Asia/Riyadh" })
        } catch (e: Exception) {
            ZoneId.of("Asia/Riyadh")
        }

        val zdt = nowInstant.atZone(zoneId)
        val currentLocalTime = zdt.toLocalTime()
        val currentLocalDate = zdt.toLocalDate()

        val openTime = parseTimeOrDefault(profile.openingTime, LocalTime.of(9, 0))
        val closeTime = parseTimeOrDefault(profile.closingTime, LocalTime.of(22, 0))

        val isOvernight = closeTime.isBefore(openTime) || closeTime == openTime

        val isOpen: Boolean
        val businessDate: LocalDate
        val nextOpenZdt: ZonedDateTime
        val nextCloseZdt: ZonedDateTime

        if (!isOvernight) {
            // Normal Same-Day Business (e.g., 09:00 -> 22:00)
            if (!currentLocalTime.isBefore(openTime) && currentLocalTime.isBefore(closeTime)) {
                // Currently Open
                isOpen = true
                businessDate = currentLocalDate
                nextCloseZdt = currentLocalDate.atTime(closeTime).atZone(zoneId)
                nextOpenZdt = currentLocalDate.plusDays(1).atTime(openTime).atZone(zoneId)
            } else {
                // Currently Closed
                isOpen = false
                if (currentLocalTime.isBefore(openTime)) {
                    // Before opening today -> Business date belongs to previous day
                    businessDate = currentLocalDate.minusDays(1)
                    nextOpenZdt = currentLocalDate.atTime(openTime).atZone(zoneId)
                    nextCloseZdt = currentLocalDate.atTime(closeTime).atZone(zoneId)
                } else {
                    // After closing today
                    businessDate = currentLocalDate
                    nextOpenZdt = currentLocalDate.plusDays(1).atTime(openTime).atZone(zoneId)
                    nextCloseZdt = currentLocalDate.plusDays(1).atTime(closeTime).atZone(zoneId)
                }
            }
        } else {
            // Overnight Business (e.g., 18:00 -> 03:00)
            if (!currentLocalTime.isBefore(openTime)) {
                // Between openTime (18:00) and Midnight (23:59:59)
                isOpen = true
                businessDate = currentLocalDate
                nextCloseZdt = currentLocalDate.plusDays(1).atTime(closeTime).atZone(zoneId)
                nextOpenZdt = currentLocalDate.plusDays(1).atTime(openTime).atZone(zoneId)
            } else if (currentLocalTime.isBefore(closeTime)) {
                // Between Midnight (00:00) and closeTime (03:00)
                isOpen = true
                businessDate = currentLocalDate.minusDays(1)
                nextCloseZdt = currentLocalDate.atTime(closeTime).atZone(zoneId)
                nextOpenZdt = currentLocalDate.atTime(openTime).atZone(zoneId)
            } else {
                // Currently Closed (e.g., 03:00 -> 18:00)
                isOpen = false
                businessDate = currentLocalDate.minusDays(1)
                nextOpenZdt = currentLocalDate.atTime(openTime).atZone(zoneId)
                nextCloseZdt = currentLocalDate.plusDays(1).atTime(closeTime).atZone(zoneId)
            }
        }

        val msUntilClose = if (isOpen) {
            Duration.between(zdt, nextCloseZdt).toMillis().coerceAtLeast(0L)
        } else {
            0L
        }

        val msUntilOpen = if (!isOpen) {
            Duration.between(zdt, nextOpenZdt).toMillis().coerceAtLeast(0L)
        } else {
            0L
        }

        return BusinessStatus(
            isBusinessOpen = isOpen,
            isBusinessClosed = !isOpen,
            currentBusinessTime = zdt.toLocalDateTime(),
            businessDate = businessDate,
            millisecondsUntilClose = msUntilClose,
            millisecondsUntilOpen = msUntilOpen,
            nextOpeningTime = nextOpenZdt.toLocalDateTime(),
            nextClosingTime = nextCloseZdt.toLocalDateTime()
        )
    }

    private fun parseTimeOrDefault(str: String, defaultTime: LocalTime): LocalTime {
        return try {
            LocalTime.parse(str.trim())
        } catch (e: Exception) {
            defaultTime
        }
    }
}
