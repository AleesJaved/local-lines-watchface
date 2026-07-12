package com.aleejaved.locallines.maps

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.location.Location
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.Style
import org.maplibre.android.snapshotter.MapSnapshotter
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MapSnapshotRepository private constructor(private val context: Context) {
    private val settings = MapSettings(context)
    private val liveMap = File(context.filesDir, "local_lines_map.jpg")
    private val fallbackMap = File(context.filesDir, "local_lines_fallback.jpg")

    enum class RefreshResult { UPDATED, CACHED, NO_PERMISSION, NO_LOCATION, FAILED }

    fun ensureFallback(): File {
        if (fallbackMap.exists()) return fallbackMap
        val bitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.RGB_565)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.BLACK)
        val major = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            strokeWidth = 3f
            strokeCap = Paint.Cap.ROUND
        }
        val minor = Paint(major).apply { color = Color.rgb(130, 130, 130); strokeWidth = 2f }
        val roads = listOf(
            floatArrayOf(20f, 315f, 430f, 220f),
            floatArrayOf(45f, 105f, 405f, 165f),
            floatArrayOf(90f, 20f, 182f, 440f),
            floatArrayOf(328f, 16f, 283f, 438f),
            floatArrayOf(18f, 193f, 432f, 188f),
        )
        roads.forEach { canvas.drawLine(it[0], it[1], it[2], it[3], major) }
        canvas.drawLine(55f, 385f, 398f, 75f, minor)
        canvas.drawLine(45f, 75f, 395f, 372f, minor)
        writeBitmap(bitmap, fallbackMap)
        bitmap.recycle()
        return fallbackMap
    }

    fun currentMapFile(): File = if (liveMap.exists()) liveMap else ensureFallback()

    fun loadCurrentBitmap(): Bitmap {
        val options = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.RGB_565 }
        return BitmapFactory.decodeFile(currentMapFile().absolutePath, options)
            ?: Bitmap.createBitmap(1, 1, Bitmap.Config.RGB_565)
    }

    suspend fun refresh(force: Boolean): RefreshResult {
        if (!hasLocationPermission()) return RefreshResult.NO_PERMISSION
        val location = getCurrentLocation() ?: return RefreshResult.NO_LOCATION
        val distance = distanceFromPrevious(location)
        if (!RefreshPolicy.shouldRender(force, liveMap.exists(), distance, settings.refreshMode)) {
            return RefreshResult.CACHED
        }

        return runCatching {
            val bitmap = render(location)
            val temporary = File(context.filesDir, "local_lines_map.tmp")
            writeBitmap(bitmap, temporary)
            bitmap.recycle()
            if (!temporary.renameTo(liveMap)) {
                temporary.copyTo(liveMap, overwrite = true)
                temporary.delete()
            }
            settings.recordSnapshot(location.latitude, location.longitude, System.currentTimeMillis())
            RefreshResult.UPDATED
        }.getOrElse { error ->
            Log.e(TAG, "Unable to render live map", error)
            RefreshResult.FAILED
        }
    }

    private fun distanceFromPrevious(location: Location): Float {
        val previousLatitude = settings.lastLatitude ?: return Float.POSITIVE_INFINITY
        val previousLongitude = settings.lastLongitude ?: return Float.POSITIVE_INFINITY
        val result = FloatArray(1)
        Location.distanceBetween(
            previousLatitude,
            previousLongitude,
            location.latitude,
            location.longitude,
            result,
        )
        return result[0]
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private suspend fun getCurrentLocation(): Location? {
        if (
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) return null
        val client = LocationServices.getFusedLocationProviderClient(context)
        val tokenSource = CancellationTokenSource()
        return runCatching {
            client.lastLocation.await()
                ?: client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, tokenSource.token).await()
        }.getOrElse { error ->
            Log.w(TAG, "Unable to obtain location", error)
            null
        }
    }

    private suspend fun render(location: Location): Bitmap {
        val first = renderOnce(location)
        val firstDensity = lineDensity(first)
        if (firstDensity >= MIN_LINE_DENSITY) return first

        // A snapshot can occasionally complete before every vector tile arrives on watch Wi-Fi.
        // Give the tile cache a moment, retry once, and retain whichever render is more complete.
        delay(TILE_RETRY_DELAY_MS)
        val second = runCatching { renderOnce(location) }.getOrElse { return first }
        return if (lineDensity(second) > firstDensity) {
            first.recycle()
            second
        } else {
            second.recycle()
            first
        }
    }

    private suspend fun renderOnce(location: Location): Bitmap = withContext(Dispatchers.Main) {
        val styleJson = context.resources.openRawResource(R.raw.local_lines_style).bufferedReader().use { it.readText() }
        suspendCancellableCoroutine { continuation ->
            val options = MapSnapshotter.Options(SIZE, SIZE)
                .withPixelRatio(1f)
                .withLogo(false)
                .withAttribution(false)
                .withStyleBuilder(Style.Builder().fromJson(styleJson))
                .withCameraPosition(
                    CameraPosition.Builder()
                        .target(LatLng(location.latitude, location.longitude))
                        .zoom(14.2)
                        .bearing(0.0)
                        .tilt(0.0)
                        .build(),
                )
            val snapshotter = MapSnapshotter(context, options)
            continuation.invokeOnCancellation { snapshotter.cancel() }
            snapshotter.start(
                { snapshot ->
                    val bitmap = snapshot.bitmap.copy(Bitmap.Config.RGB_565, true)
                    drawAttribution(bitmap)
                    if (continuation.isActive) continuation.resume(bitmap)
                },
                { error -> if (continuation.isActive) continuation.resumeWithException(IllegalStateException(error)) },
            )
        }
    }

    private fun lineDensity(bitmap: Bitmap): Float {
        var brightSamples = 0
        var samples = 0
        for (y in 0 until SIZE - 30 step 3) {
            for (x in 0 until SIZE step 3) {
                val color = bitmap.getPixel(x, y)
                if (Color.red(color) + Color.green(color) + Color.blue(color) > 120) brightSamples++
                samples++
            }
        }
        return brightSamples.toFloat() / samples
    }

    private fun drawAttribution(bitmap: Bitmap) {
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            alpha = 210
            textSize = 11f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("© OpenStreetMap · © OpenMapTiles", SIZE / 2f, SIZE - 18f, paint)
    }

    private fun writeBitmap(bitmap: Bitmap, target: File) {
        FileOutputStream(target).use { output ->
            check(bitmap.compress(Bitmap.CompressFormat.JPEG, 92, output))
        }
    }

    companion object {
        const val SIZE = 450
        private const val TAG = "LocalLinesMap"
        private const val MIN_LINE_DENSITY = 0.015f
        private const val TILE_RETRY_DELAY_MS = 750L
        @SuppressLint("StaticFieldLeak") // Repository always stores context.applicationContext.
        @Volatile private var instance: MapSnapshotRepository? = null

        fun get(context: Context): MapSnapshotRepository =
            instance ?: synchronized(this) {
                instance ?: MapSnapshotRepository(context.applicationContext).also { instance = it }
            }
    }
}
