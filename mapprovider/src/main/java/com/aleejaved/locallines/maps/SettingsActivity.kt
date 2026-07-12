package com.aleejaved.locallines.maps

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

class SettingsActivity : Activity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var content: LinearLayout
    private lateinit var status: TextView
    private lateinit var preview: ImageView
    private val settings by lazy { MapSettings(this) }
    private val repository by lazy { MapSnapshotRepository.get(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        updateUi()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        updateUi()
    }

    private fun buildUi() {
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(42, 28, 42, 48)
        }
        val scroll = ScrollView(this).apply { addView(content) }
        setContentView(scroll)

        content.addView(label(getString(R.string.settings_title), 26f, Color.WHITE).apply {
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 12)
        })

        preview = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            layoutParams = LinearLayout.LayoutParams(dp(180), dp(180)).apply { bottomMargin = dp(8) }
            background = ContextCompat.getDrawable(this@SettingsActivity, R.drawable.ic_local_lines)
        }
        content.addView(preview)

        status = label("", 14f, Color.LTGRAY).apply {
            gravity = Gravity.CENTER
            setPadding(0, 6, 0, 12)
        }
        content.addView(status, matchWidth())

        content.addView(label(getString(R.string.refresh_mode), 17f, Color.WHITE), matchWidth())
        val radioGroup = RadioGroup(this).apply { orientation = RadioGroup.VERTICAL }
        RefreshMode.entries.forEach { mode ->
            radioGroup.addView(RadioButton(this).apply {
                id = ViewGroup.generateViewId()
                tag = mode
                text = when (mode) {
                    RefreshMode.BATTERY_SAVER -> getString(R.string.battery_saver)
                    RefreshMode.BALANCED -> getString(R.string.balanced)
                    RefreshMode.FREQUENT -> getString(R.string.frequent)
                }
                setTextColor(Color.WHITE)
                isChecked = mode == settings.refreshMode
            })
        }
        radioGroup.setOnCheckedChangeListener { group, checkedId ->
            val mode = group.findViewById<RadioButton>(checkedId)?.tag as? RefreshMode ?: return@setOnCheckedChangeListener
            settings.refreshMode = mode
            RefreshScheduler.schedule(this, mode)
        }
        content.addView(radioGroup, matchWidth())

        content.addView(actionButton(getString(R.string.enable_location)) {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_FOREGROUND_LOCATION,
            )
        }.apply { tag = TAG_LOCATION_BUTTON }, matchWidth())

        content.addView(actionButton(getString(R.string.enable_background_location)) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION), REQUEST_BACKGROUND_LOCATION)
        }.apply { tag = TAG_BACKGROUND_BUTTON }, matchWidth())

        content.addView(actionButton(getString(R.string.manual_refresh)) { refreshNow() }, matchWidth())
        content.addView(label(getString(R.string.privacy_text), 12f, Color.LTGRAY).apply {
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 10)
        }, matchWidth())
        content.addView(label(getString(R.string.attribution), 10f, Color.GRAY).apply { gravity = Gravity.CENTER }, matchWidth())
    }

    private fun refreshNow() {
        status.text = getString(R.string.refreshing)
        scope.launch {
            val result = repository.refresh(force = true)
            status.text = when (result) {
                MapSnapshotRepository.RefreshResult.UPDATED -> getString(R.string.refresh_success)
                MapSnapshotRepository.RefreshResult.CACHED -> getString(R.string.refresh_cached)
                MapSnapshotRepository.RefreshResult.NO_PERMISSION -> getString(R.string.permission_needed)
                else -> getString(R.string.refresh_failed)
            }
            if (result == MapSnapshotRepository.RefreshResult.UPDATED) {
                ComplicationDataSourceUpdateRequester.create(
                    this@SettingsActivity,
                    ComponentName(this@SettingsActivity, MapComplicationService::class.java),
                ).requestUpdateAll()
            }
            updatePreview()
        }
    }

    private fun updateUi() {
        val hasForeground = hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ||
            hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        val hasBackground = hasPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        findTagged<Button>(TAG_LOCATION_BUTTON).visibility = if (hasForeground) View.GONE else View.VISIBLE
        findTagged<Button>(TAG_BACKGROUND_BUTTON).visibility =
            if (hasForeground && !hasBackground) View.VISIBLE else View.GONE
        status.text = if (!hasForeground) {
            getString(R.string.permission_needed)
        } else if (settings.lastUpdatedMillis == 0L) {
            getString(R.string.never_updated)
        } else {
            getString(
                R.string.last_updated,
                DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                    .format(Date(settings.lastUpdatedMillis)),
            )
        }
        updatePreview()
    }

    private fun updatePreview() {
        preview.setImageBitmap(repository.loadCurrentBitmap())
    }

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    @Suppress("UNCHECKED_CAST")
    private fun <T : android.view.View> findTagged(tag: String): T =
        (0 until content.childCount)
            .map { content.getChildAt(it) }
            .first { it.tag == tag } as T

    private fun label(text: String, size: Float, color: Int) = TextView(this).apply {
        this.text = text
        textSize = size
        setTextColor(color)
    }

    private fun actionButton(text: String, action: () -> Unit) = Button(this).apply {
        this.text = text
        isAllCaps = false
        setOnClickListener { action() }
    }

    private fun matchWidth() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT,
    )

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val REQUEST_FOREGROUND_LOCATION = 100
        private const val REQUEST_BACKGROUND_LOCATION = 101
        private const val TAG_LOCATION_BUTTON = "location"
        private const val TAG_BACKGROUND_BUTTON = "background"
    }
}
