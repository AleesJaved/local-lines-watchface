package com.aleejaved.locallines.maps

import java.time.Duration

enum class RefreshMode(
    val interval: Duration,
    val movementMetres: Float,
) {
    BATTERY_SAVER(Duration.ofHours(6), 2_000f),
    BALANCED(Duration.ofHours(2), 1_000f),
    FREQUENT(Duration.ofMinutes(30), 500f),
}
