package com.matrix.autoreply.activity

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.matrix.autoreply.AlertDialogHelper
import com.matrix.autoreply.R
import com.matrix.autoreply.activity.ui.main.SectionsPagerAdapter
import java.io.File

private const val MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 0
private const val REQUEST_CODE = 1111

class MainActivity : AppCompatActivity() {


    private val msgLogFileName = "msgLog.txt"
    private val signalMsgLogFileName = "signalMsgLog.txt"
    private val w4bMsgLogFileName = "waBusMsgLog.txt"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tabbed)

        val toolbar: Toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        toolbar.title = "Auto Reply"
        setSupportActionBar(toolbar)

        val sectionsPagerAdapter = SectionsPagerAdapter(this, supportFragmentManager)
        val viewPager: ViewPager = findViewById(R.id.view_pager)
        viewPager.adapter = sectionsPagerAdapter
        val tabs: TabLayout = findViewById(R.id.tabs)
        tabs.setupWithViewPager(viewPager)

        // Request Storage Permission
        requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE)

        window.statusBarColor = resources.getColor(R.color.colorPrimary)

        val appUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(this)

        val appUpdateInfoTask = appUpdateManager.appUpdateInfo
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                // Request the update.
                Log.d(ContentValues.TAG, "Update available")
                appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        AppUpdateType.FLEXIBLE,
                        this,
                        REQUEST_CODE)
            } else {
                Log.d(ContentValues.TAG, "No Update available")
            }
        }

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE) {
            Toast.makeText(this, "Start download", Toast.LENGTH_SHORT).show()
            if (resultCode != RESULT_OK) {
                Log.d("Update", "UpdateFlow failure.")
            }
        }
    }


    private fun createBackups() {
        if (!File(this.filesDir, msgLogFileName).exists()) {
            if (!File(this.filesDir, msgLogFileName).createNewFile())
                Toast.makeText(applicationContext, getString(R.string.create_msg_log_failed),
                        Toast.LENGTH_SHORT).show()
        }

        if (!File(this.filesDir, signalMsgLogFileName).exists()) {
            if (!File(this.filesDir, signalMsgLogFileName).createNewFile())
                Toast.makeText(applicationContext, getString(R.string.create_msg_log_failed),
                        Toast.LENGTH_SHORT).show()
        }

        if (!File(this.filesDir, w4bMsgLogFileName).exists()) {
            if (!File(this.filesDir, w4bMsgLogFileName).createNewFile())
                Toast.makeText(applicationContext, getString(R.string.create_msg_log_failed),
                        Toast.LENGTH_SHORT).show()
        }
    }

    @Suppress("SameParameterValue")
    private fun requestPermission(permission: String, requestCode: Int) {
        if (ContextCompat.checkSelfPermission(this,
                        permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    arrayOf(permission), requestCode)
        } else {
            createBackups()
        }
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Toast.makeText(applicationContext, getString(R.string.create_backup_dir), Toast.LENGTH_SHORT).show()
                createBackups()
            } else {
                Toast.makeText(applicationContext, getString(R.string.allow_storage_permission_msg), Toast.LENGTH_LONG).show()
            }
            return
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_light_dark, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_toggle_theme -> {

                when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
                    Configuration.UI_MODE_NIGHT_NO -> {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    }
                    Configuration.UI_MODE_NIGHT_YES -> {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    }
                }
                return true
            }

            R.id.help_menu -> {
                AlertDialogHelper.showDialog(
                        this,
                        "Help",
                        "1. Give the required permissions one for auto reply and other for message logs.\n" +
                                "2. Set custom text for auto reply.\n" +
                                "3. Select the applications for which you want auto reply.\n" +
                                "4. If you want group chat reply then turn on that option.\n" +
                                "5. If you increase the reply frequency then one person gets reply only the set value times.\n" +
                                "6. In MSG LOGS section you can see all the messages of the respective application (BETA).\n" +
                                "7. Enjoy!!\n\n" +
                                "This application is still under development so kindly post any bugs to github issues section you find on settings tab.",
                        getString(R.string.ok),
                        null
                ) { dialog, _ -> dialog.cancel() }

                return true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }


}