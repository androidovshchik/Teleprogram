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
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.*
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.widget.addTextChangedListener
import com.chibatching.kotpref.bulk
import defpackage.teleprogram.api.Preferences
import defpackage.teleprogram.extensions.isMarshmallowPlus
import defpackage.teleprogram.extensions.makeCallback
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.dialog_prompt.*
import kotlinx.android.synthetic.main.fragment_api.*
import kotlinx.android.synthetic.main.fragment_main.*
import org.jetbrains.anko.powerManager
import org.jetbrains.anko.toast
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import org.kodein.di.generic.instance

abstract class BaseFragment : Fragment(), KodeinAware {

    override val kodein by closestKodein()
}

class WebClient(val postscript: String) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
        view.loadUrl(url)
        return true
    }

    override fun onPageFinished(view: WebView, url: String?) {
        view.loadUrl("javascript:$postscript")
    }
}

class ApiFragment : BaseFragment() {

    override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, bundle: Bundle?): View {
        return inflater.inflate(R.layout.fragment_api, parent, false)
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        wv_api.apply {
            setBackgroundColor(Color.TRANSPARENT)
            settings.javaScriptEnabled = true
            webViewClient = WebClient(context.assets.open("postscript.js").bufferedReader().use {
                it.readText().replace("(\r\n|\r|\n)".toRegex(), "")
            })
            loadUrl("file:///android_asset/app/${activity?.packageName}.api/-android/index.html")
        }
    }
}

class MainFragment : BaseFragment() {

    private val preferences: Preferences by instance()

    override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, bundle: Bundle?): View {
        return inflater.inflate(R.layout.fragment_main, parent, false)
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        et_phone.addTextChangedListener {
            activity.makeCallback<MainActivity> {
                phone = it.toString()
            }
        }
        et_list.addTextChangedListener {
            activity.makeCallback<MainActivity> {
                urls = it.toString()
            }
        }
        preferences.apply {
            telephone?.let {
                et_phone.setText("+$it")
            }
            et_list.setText(urlList)
        }
        btn_api.setOnClickListener {
            activity.makeCallback<MainActivity> {
                addFragment(ApiFragment())
            }
        }
    }
}

class PromptDialog(activity: Activity) : Dialog(activity, R.style.AppDialog) {

    init {
        setCancelable(false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_prompt)
        btn_ok.setOnClickListener {
            it.isEnabled = false
            val code = et_code.text.toString().trim()
            if (code.isNotEmpty()) {
                MainService.toggle(context, true, "code" to code)
            }
            it.isEnabled = true
        }
    }
}

class MainActivity : Activity(), KodeinAware {

    private val parentKodein by closestKodein()

    override val kodein: Kodein by Kodein.lazy {

        extend(parentKodein)

        import(mainModule)
    }

    private val preferences: Preferences by instance()

    private val promptDialog: PromptDialog by instance()

    var phone = ""

    var urls = ""

    private val receiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            if (intent.getBooleanExtra("prompted", false)) {
                promptDialog.dismiss()
            } else {
                promptDialog.show()
            }
        }
    }

    @SuppressLint("BatteryLife")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
        setContentView(R.layout.activity_main)
        ib_back.setOnClickListener {
            onBackPressed()
        }
        tv_id.text = preferences.appId
        swt_run.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                preferences.bulk {
                    telephone?.let {
                        val phone = phone.trim()
                        if (phone.isEmpty()) {
                            toast("Заполните телефон")
                            return@setOnCheckedChangeListener
                        }
                        telephone = phone
                    }
                    urlList = urls.trim()
                }
            }
            buttonView.isEnabled = false
            if (MainService.toggle(applicationContext, isChecked)) {
                preferences.runApp = isChecked
            }
            buttonView.isEnabled = true
        }
        addFragment(MainFragment())
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
        registerReceiver(receiver, IntentFilter(ACTION_TELEGRAM))
    }

    override fun onStart() {
        super.onStart()
        MainService.toggle(applicationContext, preferences.runApp)
    }

    fun addFragment(fragment: BaseFragment) {
        fragmentManager.apply {
            beginTransaction()
                .replace(R.id.frg_main, fragment, backStackEntryCount.toString())
                .addToBackStack(null)
                .commitAllowingStateLoss()
            executePendingTransactions()
        }
    }

    override fun onBackPressed() {
        if (fragmentManager.backStackEntryCount > 1) {
            super.onBackPressed()
        } else {
            finish()
        }
    }

    override fun onDestroy() {
        promptDialog.dismiss()
        unregisterReceiver(receiver)
        super.onDestroy()
    }

    companion object {

        const val ACTION_TELEGRAM = "TGM_PROMPT"
    }
}
