package com.aleejaved.locallines.maps

object RefreshPolicy {
    fun shouldRender(force: Boolean, hasLiveMap: Boolean, distanceMetres: Float, mode: RefreshMode): Boolean =
        force || !hasLiveMap || distanceMetres >= mode.movementMetres
}
