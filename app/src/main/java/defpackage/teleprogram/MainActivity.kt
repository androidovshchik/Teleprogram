@file:Suppress("DEPRECATION")

package defpackage.teleprogram

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.app.Fragment
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.*
import defpackage.teleprogram.extensions.isMarshmallowPlus
import defpackage.teleprogram.extensions.makeCallback
import kotlinx.android.synthetic.main.dialog_prompt.*
import kotlinx.android.synthetic.main.fragment_api.*
import kotlinx.android.synthetic.main.fragment_main.*
import org.jetbrains.anko.powerManager
import org.jetbrains.anko.toast
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import org.kodein.di.generic.instance
import timber.log.Timber

abstract class BaseFragment : Fragment(), KodeinAware {

    override val kodein by closestKodein()
}

class ApiFragment : BaseFragment() {

    override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, bundle: Bundle?): View {
        return inflater.inflate(R.layout.fragment_api, parent, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        wv_api.loadUrl("file:///android_asset/app/${activity?.packageName}.api/-android/index.html")
    }
}

class MainFragment : BaseFragment() {

    override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, bundle: Bundle?): View {
        return inflater.inflate(R.layout.fragment_main, parent, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        btn_api.setOnClickListener {
            activity.makeCallback<MainActivity> {
                setFragment(ApiFragment())
            }
        }
    }
}

class PromptDialog(activity: Activity) : Dialog(activity) {

    init {
        setCancelable(false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_prompt)
        btn_ok.setOnClickListener {

        }
    }
}

class MainActivity : Activity(), KodeinAware {

    private val parentKodein by closestKodein()

    override val kodein: Kodein by Kodein.lazy {

        extend(parentKodein)

        import(mainModule)
    }

    private val promptDialog: PromptDialog by instance()

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

    @SuppressLint("SetTextI18n", "BatteryLife")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
        setContentView(R.layout.activity_main)
        setFragment(MainFragment())
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

    fun setFragment(fragment: BaseFragment) {
        if (getCurrentFragment() is MenuFragment) {
            while (popFragment(true));
        }
        fragmentManager.beginTransaction()
            .replace(R.id.frg_main, fragment, supportFragmentManager.backStackEntryCount.toString())
            .addToBackStack(null)
            .commitAllowingStateLoss()
        fragmentManager.executePendingTransactions()
    }

    override fun onDestroy() {
        promptDialog.dismiss()
        unregisterReceiver(receiver)
        super.onDestroy()
    }
}
