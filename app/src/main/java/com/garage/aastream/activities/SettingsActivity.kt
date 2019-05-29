package com.garage.aastream.activities

import android.annotation.TargetApi
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.garage.aastream.R
import android.os.Build
import android.provider.Settings
import android.view.View
import android.widget.*
import kotlinx.android.synthetic.main.activity_settings.*
import kotlinx.android.synthetic.main.view_settings_brightness.*
import kotlinx.android.synthetic.main.view_settings_brightness.settings_brightness_switch
import kotlinx.android.synthetic.main.view_settings_rotation.*
import com.garage.aastream.App
import com.garage.aastream.handlers.BrightnessHandler
import com.garage.aastream.handlers.PreferenceHandler
import com.garage.aastream.handlers.RotationHandler
import com.garage.aastream.utils.DevLog
import javax.inject.Inject
import android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS
import kotlinx.android.synthetic.main.view_settings_debug.*
import kotlinx.android.synthetic.main.view_settings_sidebar.*
import kotlinx.android.synthetic.main.view_settings_unlock.*
import com.garage.aastream.BuildConfig
import com.garage.aastream.interfaces.OnPatchStatusCallback
import com.garage.aastream.utils.Const
import com.garage.aastream.utils.PhenotypePatcher
import kotlinx.android.synthetic.main.view_settings_about.*

/**
 * Created by Endy Rubbin on 22.05.2019 10:44.
 * For project: AAStream
 */
class SettingsActivity : AppCompatActivity() {

    @Inject lateinit var preferences: PreferenceHandler
    @Inject lateinit var brightnessHandler: BrightnessHandler
    @Inject lateinit var rotationHandler: RotationHandler
    @Inject lateinit var patcher: PhenotypePatcher

    private var time: Long = 0
    private var count = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        (application as App).component.inject(this)

        initViews()
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun checkForSystemWritePermission() {
        if (!Settings.System.canWrite(this)) {
            val intent = Intent(ACTION_MANAGE_WRITE_SETTINGS)
            startActivity(intent)
        }
    }

    /**
     * Initialize views and set listeners
     */
    private fun initViews() {
        // Debug controller
        settings_debug_activity_holder.setOnClickListener {
            startActivity(Intent(this, CarDebugActivity::class.java))
        }
        view_settings_debug.visibility = if (preferences.getBoolean(PreferenceHandler.KEY_DEBUG_ENABLED, BuildConfig.DEBUG)) {
            View.VISIBLE
        } else {
            View.GONE
        }
        settings_debug_switch.setOnCheckedChangeListener { _, isChecked ->
            DevLog.d("Debug switch changed: $isChecked")
            if (BuildConfig.DEBUG) {
                preferences.putBoolean(PreferenceHandler.KEY_DEBUG_ENABLED, isChecked)
            }
        }
        settings_debug_switch.isChecked = preferences.getBoolean(PreferenceHandler.KEY_DEBUG_ENABLED, false)

        // Unlock controller
        view_settings_unlock.setOnClickListener { unlock() }
        settings_unlock_state_icon.visibility = if (patcher.isPatched()) View.VISIBLE else View.GONE

        // Brightness controller
        settings_brightness_seek_bar.progress = brightnessHandler.getScreenBrightness()
        settings_brightness_seek_bar.max = Const.MAX_VALUE
        settings_brightness_seek_bar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                DevLog.d("Brightness value changed $progress")
                preferences.putInt(PreferenceHandler.KEY_BRIGHTNESS_VALUE, progress)
            }
        })
        settings_brightness_switch.isChecked = preferences.getBoolean(PreferenceHandler.KEY_BRIGHTNESS_SWITCH, false)
        settings_brightness_switch.setOnCheckedChangeListener { _, isChecked ->
            DevLog.d("Brightness switch changed: $isChecked")
            if (isChecked) {
                checkForSystemWritePermission()
            }
            preferences.putBoolean(PreferenceHandler.KEY_BRIGHTNESS_SWITCH, isChecked)
            settings_brightness_seek_bar.isEnabled = isChecked
        }
        settings_brightness_seek_bar.isEnabled = settings_brightness_switch.isChecked

        // Rotation controller
        val rotationAdapter = ArrayAdapter.createFromResource(this, R.array.rotation_values,
            android.R.layout.simple_spinner_item)
        rotationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        settings_rotation_dropdown.adapter = rotationAdapter
        settings_rotation_dropdown.setSelection(rotationHandler.getScreenRotation())
        settings_rotation_dropdown.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                DevLog.d("Rotation selected $position")
                preferences.putInt(PreferenceHandler.KEY_ROTATION_VALUE, position)
            }
        }
        settings_rotation_switch.isChecked = preferences.getBoolean(PreferenceHandler.KEY_ROTATION_SWITCH, false)
        settings_rotation_switch.setOnCheckedChangeListener { _, isChecked ->
            DevLog.d("Rotation switch changed: $isChecked")
            if (isChecked) {
                checkForSystemWritePermission()
            }
            preferences.putBoolean(PreferenceHandler.KEY_ROTATION_SWITCH, isChecked)
            settings_rotation_dropdown.isEnabled = isChecked
        }
        settings_rotation_dropdown.isEnabled = settings_rotation_switch.isChecked

        // Sidebar controller
        val sidebarAdapter = ArrayAdapter.createFromResource(this, R.array.screen_values,
            android.R.layout.simple_spinner_item)
        sidebarAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        settings_sidebar_dropdown.adapter = sidebarAdapter
        settings_sidebar_dropdown.setSelection(preferences.getInt(PreferenceHandler.KEY_STARTUP_VALUE, Const.DEFAULT_SCREEN))
        settings_sidebar_dropdown.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                DevLog.d("Startup screen selected $position")
                preferences.putInt(PreferenceHandler.KEY_STARTUP_VALUE, position)
            }
        }
        settings_sidebar_switch.isChecked = preferences.getBoolean(PreferenceHandler.KEY_SIDEBAR_SWITCH, Const.DEFAULT_SHOW_SIDEBAR)
        settings_sidebar_switch.setOnCheckedChangeListener { _, isChecked ->
            DevLog.d("Sidebar switch changed: $isChecked")
            preferences.putBoolean(PreferenceHandler.KEY_SIDEBAR_SWITCH, isChecked)
        }

        // About controller
        settings_about_version.text = getString(R.string.txt_version, BuildConfig.VERSION_NAME + BuildConfig.VERSION_CODE)
        settings_about.setOnClickListener {
            val currentTime = System.currentTimeMillis()
            if (currentTime - time <= Const.CLICK_INTERVAL) {
                count++
            } else {
                count = 0
            }

            time = currentTime
            if (count == Const.DEBUG_CLICK_COUNT) {
                DevLog.d("Enabling debug mode")
                preferences.putBoolean(PreferenceHandler.KEY_DEBUG_ENABLED, true)
                view_settings_debug.visibility = View.VISIBLE
            }
        }
    }

    /**
     * White list this app for Android Auto
     * Reference: @see <a href="https://github.com/Eselter/AA-Phenotype-Patcher">AA-Phenotype-Patcher</a>
     */
    private fun unlock() {
        settings_unlock_state_spinner.visibility = View.VISIBLE
        settings_unlock_state_icon.visibility = View.GONE
        patcher.patch(object : OnPatchStatusCallback{
            override fun onPatchSuccessful() {
                runOnUiThread {
                    Toast.makeText(this@SettingsActivity,
                        "Successfully unlocked. Reboot phone and connect to Android Auto", Toast.LENGTH_LONG).show()
                    settings_unlock_state_icon.visibility = View.VISIBLE
                    settings_unlock_state_spinner.visibility = View.GONE
                }
            }

            override fun onPatchFailed() {
                runOnUiThread {
                    Toast.makeText(this@SettingsActivity,
                        "Root not available, install SuperSU and perform root first.", Toast.LENGTH_LONG).show()
                    settings_unlock_state_icon.visibility = View.GONE
                    settings_unlock_state_spinner.visibility = View.GONE
                }
            }
        })
    }
}