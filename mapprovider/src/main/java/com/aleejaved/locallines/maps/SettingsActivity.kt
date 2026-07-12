package com.aleejaved.locallines.maps

import android.Manifest
import android.app.Activity
import android.content.res.ColorStateList
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewConfiguration
import android.widget.Button
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class SettingsActivity : Activity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var content: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var status: TextView
    private lateinit var preview: ImageView
    private lateinit var refreshButton: Button
    private var refreshInProgress = false
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
            setPadding(dp(24), dp(14), dp(24), dp(40))
            setBackgroundColor(BACKGROUND)
        }
        scroll = ScrollView(this).apply {
            addView(content)
            isFillViewport = true
            isFocusableInTouchMode = true
            isVerticalFadingEdgeEnabled = true
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            requestFocus()
        }
        setContentView(scroll)

        content.addView(label(getString(R.string.settings_title), 25f, Color.WHITE).apply {
            gravity = Gravity.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, dp(4), 0, 0)
        })
        content.addView(label(getString(R.string.settings_subtitle), 11f, ACCENT).apply {
            gravity = Gravity.CENTER
            letterSpacing = 0.12f
            setPadding(0, dp(2), 0, dp(10))
        })

        val previewSize = dp(134)
        val previewFrame = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(previewSize, previewSize).apply { bottomMargin = dp(8) }
        }
        preview = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            background = roundedDrawable(Color.BLACK, 999f, ACCENT, dp(2))
            clipToOutline = true
            outlineProvider = android.view.ViewOutlineProvider.BACKGROUND
        }
        previewFrame.addView(preview)
        val now = Date()
        previewFrame.addView(previewTime(SimpleDateFormat("HH", Locale.getDefault()).format(now), dp(22)))
        previewFrame.addView(previewTime(SimpleDateFormat("mm", Locale.getDefault()).format(now), dp(68)))
        previewFrame.addView(View(this).apply {
            background = roundedDrawable(Color.rgb(255, 48, 60), 999f, Color.WHITE, dp(1))
        }, FrameLayout.LayoutParams(dp(9), dp(9), Gravity.CENTER).apply { topMargin = dp(2) })
        content.addView(previewFrame)

        status = label("", 13f, TEXT_SECONDARY).apply {
            gravity = Gravity.CENTER
            background = roundedDrawable(SURFACE, 999f, DIVIDER, 1)
            setPadding(dp(14), dp(8), dp(14), dp(8))
        }
        content.addView(status, matchWidth().apply { bottomMargin = dp(10) })

        val refreshSection = section(getString(R.string.refresh_mode))
        val radioGroup = RadioGroup(this).apply { orientation = RadioGroup.VERTICAL }
        RefreshMode.entries.forEach { mode ->
            radioGroup.addView(RadioButton(this).apply {
                id = ViewGroup.generateViewId()
                tag = mode
                text = when (mode) {
                    RefreshMode.LIVE -> getString(R.string.live)
                    RefreshMode.BATTERY_SAVER -> getString(R.string.battery_saver)
                    RefreshMode.BALANCED -> getString(R.string.balanced)
                    RefreshMode.FREQUENT -> getString(R.string.frequent)
                }
                setTextColor(Color.WHITE)
                buttonTintList = controlTint()
                isChecked = mode == settings.refreshMode
                setPadding(0, dp(2), 0, dp(2))
            })
        }
        radioGroup.setOnCheckedChangeListener { group, checkedId ->
            val mode = group.findViewById<RadioButton>(checkedId)?.tag as? RefreshMode ?: return@setOnCheckedChangeListener
            settings.refreshMode = mode
            RefreshScheduler.schedule(this, mode)
        }
        refreshSection.addView(radioGroup, matchWidth())
        content.addView(refreshSection, cardParams())

        val locationSection = section(getString(R.string.location_label_mode))
        addLocationCheckbox(locationSection, getString(R.string.location_number), settings.locationNumberEnabled) {
            settings.locationNumberEnabled = it
        }
        addLocationCheckbox(locationSection, getString(R.string.location_road), settings.locationRoadEnabled) {
            settings.locationRoadEnabled = it
        }
        addLocationCheckbox(locationSection, getString(R.string.location_town), settings.locationTownEnabled) {
            settings.locationTownEnabled = it
        }
        addLocationCheckbox(locationSection, getString(R.string.location_city), settings.locationCityEnabled) {
            settings.locationCityEnabled = it
        }
        addLocationCheckbox(locationSection, getString(R.string.location_country), settings.locationCountryEnabled) {
            settings.locationCountryEnabled = it
        }
        content.addView(locationSection, cardParams())

        content.addView(actionButton(getString(R.string.enable_location)) {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_FOREGROUND_LOCATION,
            )
        }.apply { tag = TAG_LOCATION_BUTTON }, matchWidth())

        content.addView(actionButton(getString(R.string.enable_background_location)) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION), REQUEST_BACKGROUND_LOCATION)
        }.apply { tag = TAG_BACKGROUND_BUTTON }, matchWidth())

        refreshButton = actionButton(getString(R.string.manual_refresh)) { refreshNow() }
        content.addView(refreshButton, matchWidth().apply {
            topMargin = dp(4)
            bottomMargin = dp(8)
        })
        val aboutSection = section(getString(R.string.privacy_title))
        aboutSection.addView(label(getString(R.string.privacy_text), 12f, TEXT_SECONDARY).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, dp(8))
        }, matchWidth())
        aboutSection.addView(label(getString(R.string.attribution), 10f, TEXT_MUTED).apply {
            gravity = Gravity.CENTER
        }, matchWidth())
        content.addView(aboutSection, cardParams())
    }

    private fun refreshNow() {
        if (refreshInProgress) return
        refreshInProgress = true
        refreshButton.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        refreshButton.text = getString(R.string.finding_location)
        status.text = getString(R.string.finding_location)
        scope.launch {
            try {
                LocationLabelRepository(this@SettingsActivity).refreshCurrent()
                refreshButton.text = getString(R.string.rendering_maps)
                status.text = getString(R.string.rendering_maps)
                val result = repository.refresh(force = true)
                val message = when (result) {
                    MapSnapshotRepository.RefreshResult.UPDATED -> getString(R.string.refresh_success)
                    MapSnapshotRepository.RefreshResult.CACHED -> getString(R.string.refresh_cached)
                    MapSnapshotRepository.RefreshResult.NO_PERMISSION -> getString(R.string.permission_needed)
                    MapSnapshotRepository.RefreshResult.NO_LOCATION -> getString(R.string.location_unavailable)
                    else -> getString(R.string.refresh_failed)
                }
                status.text = message
                refreshButton.text = message
                updatePreview()
                refreshButton.performHapticFeedback(
                    if (result == MapSnapshotRepository.RefreshResult.UPDATED) {
                        HapticFeedbackConstants.CONFIRM
                    } else {
                        HapticFeedbackConstants.REJECT
                    },
                )
                delay(1_400L)
            } catch (_: Exception) {
                status.text = getString(R.string.refresh_failed)
                refreshButton.text = getString(R.string.refresh_failed_short)
                refreshButton.performHapticFeedback(HapticFeedbackConstants.REJECT)
                delay(1_400L)
            } finally {
                refreshButton.text = getString(R.string.manual_refresh)
                refreshInProgress = false
            }
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
        if (settings.refreshMode == RefreshMode.LIVE || settings.hasEnabledLocationParts()) {
            PassiveLocationManager.register(this)
        }
        if (settings.hasEnabledLocationParts()) GeofenceLocationManager.registerLastKnown(this)
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
        setTextColor(Color.WHITE)
        background = roundedDrawable(ACCENT_DARK, 999f, ACCENT, 1)
        minHeight = dp(48)
        setOnClickListener { action() }
    }

    private fun previewTime(value: String, top: Int) = label(value, 42f, Color.WHITE).apply {
        gravity = Gravity.CENTER
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        setShadowLayer(5f, 0f, 1f, Color.BLACK)
        layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, dp(52)).apply {
            topMargin = top
        }
    }

    private fun addLocationCheckbox(parent: LinearLayout, text: String, checked: Boolean, save: (Boolean) -> Unit) {
        parent.addView(CheckBox(this).apply {
            this.text = text
            setTextColor(Color.WHITE)
            buttonTintList = controlTint()
            isChecked = checked
            setPadding(0, dp(2), 0, dp(2))
            setOnCheckedChangeListener { _, enabled ->
                save(enabled)
                if (settings.hasEnabledLocationParts()) {
                    PassiveLocationManager.register(this@SettingsActivity)
                    GeofenceLocationManager.registerLastKnown(this@SettingsActivity)
                } else if (settings.refreshMode != RefreshMode.LIVE) {
                    PassiveLocationManager.unregister(this@SettingsActivity)
                    GeofenceLocationManager.unregister(this@SettingsActivity)
                }
                ComplicationUpdates.requestAll(this@SettingsActivity)
                if (enabled && settings.selectedLocationLabel() == null) refreshNow()
            }
        }, matchWidth())
    }

    private fun section(title: String) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = roundedDrawable(SURFACE, dp(18).toFloat(), DIVIDER, 1)
        setPadding(dp(16), dp(13), dp(16), dp(13))
        addView(label(title, 16f, Color.WHITE).apply {
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, dp(4))
        }, matchWidth())
    }

    private fun cardParams() = matchWidth().apply { bottomMargin = dp(10) }

    private fun roundedDrawable(color: Int, radius: Float, strokeColor: Int, strokeWidth: Int) =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(color)
            cornerRadius = radius
            setStroke(strokeWidth, strokeColor)
        }

    private fun controlTint() = ColorStateList(
        arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
        intArrayOf(ACCENT, TEXT_MUTED),
    )

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_SCROLL && event.isFromSource(InputDevice.SOURCE_ROTARY_ENCODER)) {
            val pixels = -event.getAxisValue(MotionEvent.AXIS_SCROLL) *
                ViewConfiguration.get(this).scaledVerticalScrollFactor * 1.35f
            scroll.smoothScrollBy(0, pixels.roundToInt())
            return true
        }
        return super.onGenericMotionEvent(event)
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
        private val BACKGROUND = Color.rgb(6, 8, 11)
        private val SURFACE = Color.rgb(20, 24, 30)
        private val DIVIDER = Color.rgb(52, 60, 70)
        private val ACCENT = Color.rgb(77, 141, 255)
        private val ACCENT_DARK = Color.rgb(35, 82, 155)
        private val TEXT_SECONDARY = Color.rgb(196, 202, 212)
        private val TEXT_MUTED = Color.rgb(128, 137, 150)
    }
}
