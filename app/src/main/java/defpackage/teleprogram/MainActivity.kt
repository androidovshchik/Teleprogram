@file:Suppress("DEPRECATION")

package defpackage.teleprogram

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.app.Fragment
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Window
import android.view.WindowManager
import androidx.sqlite.db.SimpleSQLiteQuery
import defpackage.teleprogram.api.Preferences
import org.jetbrains.anko.activityUiThread
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.powerManager
import org.jetbrains.anko.toast
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import timber.log.Timber
import java.lang.ref.WeakReference

abstract class BaseFragment : Fragment(), KodeinAware {

    override val kodein by closestKodein()

    @Suppress("unused")
    protected val args: Bundle
        get() = arguments ?: Bundle()
}

class MainFragment : BaseFragment() {


}

class PromptDialog(activity: Activity) : Dialog(activity) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_wait)
    }
}

class MainActivity : Activity() {

    private lateinit var preferences: Preferences

    private lateinit var promptDialog: AlertDialog

    private val receiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            promptDialog.apply {
                if (intent.getBooleanExtra("prompted", false)) {
                    dismiss()
                } else {
                    show()
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
        setContentView(R.layout.activity_main)
        preferences.apply {
            phone?.let {
                phone_number.setText("+$it")
            }
            proxies.setSelection(proxyType, false)
            proxy_url.apply {
                isEnabled = proxyType > 0
                setText(proxyUrl)
            }
            switch_run.isChecked = runService
            enable_log.isChecked = enableLog
        }
        proxies.onItemSelectedListener {
            onItemSelected { _, _, position, _ ->
                preferences.proxyType = position
                proxy_url.isEnabled = position > 0
            }
        }
        switch_run.setOnCheckedChangeListener { buttonView, isChecked ->
            buttonView.isEnabled = false
            if (isChecked) {
                if (preferences.phone == null) {
                    val number = phone_number.text.toString()
                        .replace(MainService.phoneRegex, "")
                    if (number.length != 11) {
                        toast("Заполните телефон")
                        buttonView.apply {
                            setChecked(false)
                            isEnabled = true
                        }
                        return@setOnCheckedChangeListener
                    }
                    preferences.phone = number
                }
                if (preferences.proxyType > 0) {
                    val proxy = proxy_url.text.toString()
                    if (!MainService.proxyRegex.matches(proxy)) {
                        toast("Заполните прокси")
                        buttonView.apply {
                            setChecked(false)
                            isEnabled = true
                        }
                        return@setOnCheckedChangeListener
                    }
                    preferences.proxyUrl = proxy
                }
                preferences.lastTime = currentTimeMillis()
                if (!areGranted(Manifest.permission.READ_SMS)) {
                    toast("Требуется предоставить разрешения")
                    buttonView.apply {
                        setChecked(false)
                        isEnabled = true
                    }
                    return@setOnCheckedChangeListener
                }
            }
            if (toggleService(isChecked)) {
                preferences.runService = isChecked
            }
            buttonView.isEnabled = true
        }
        export_db.setOnClickListener { view ->
            view.isEnabled = false
            val viewRef = WeakReference(view)
            doAsync {
                MainApp.db.baseDao().checkpoint(SimpleSQLiteQuery("pragma wal_checkpoint(full)"))
                copyDb(viewRef.get()?.context)
                activityUiThread {
                    toast("БД успешно экспортировано")
                    viewRef.get()?.isEnabled = true
                }
            }
        }
        enable_log.setOnCheckedChangeListener { buttonView, isChecked ->
            buttonView.isEnabled = false
            preferences.enableLog = isChecked
            LogTree.saveToFile = isChecked
            buttonView.isEnabled = true
        }
        // NOTICE this violates Google Play policy
        if (isMarshmallowPlus()) {
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                startActivity(
                    Intent(
                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:$packageName")
                    )
                )
            }
        }
        if (!areGranted(Manifest.permission.READ_SMS)) {
            requestPermissions(100, Manifest.permission.READ_SMS)
        }
        registerReceiver(receiver, IntentFilter("TGM_PROMPT"))
    }

    override fun onStart() {
        super.onStart()
        toggleService(preferences.runService)
    }

    private fun toggleService(run: Boolean): Boolean {
        return if (run) {
            try {
                MainService.start(applicationContext)
            } catch (e: Throwable) {
                Timber.e(e)
                toast(e.toString())
                false
            }
        } else {
            MainService.stop(applicationContext)
        }
    }

    override fun onDestroy() {
        promptDialog.dismiss()
        unregisterReceiver(receiver)
        super.onDestroy()
    }
}
