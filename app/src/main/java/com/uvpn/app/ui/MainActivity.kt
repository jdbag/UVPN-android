package com.uvpn.app.ui

import android.content.*
import android.graphics.Color
import android.net.VpnService
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.uvpn.app.R
import com.uvpn.app.databinding.ActivityMainBinding
import com.uvpn.app.vpn.WarpVpnService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding
    private val vm: MainViewModel by viewModels()

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { if (it.resultCode == RESULT_OK) doStart() else toast("VPN permission required") }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            vm.onBroadcast(i.getStringExtra(WarpVpnService.EXTRA_STATE) ?: return)
        }
    }

    private var sessionTimerJob: Job? = null

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)
        AdManager.init(this)
        observe()
        clicks()
        refreshSessionUI()
    }

    override fun onResume() {
        super.onResume()
        ContextCompat.registerReceiver(
            this, receiver,
            IntentFilter(WarpVpnService.BROADCAST),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        refreshSessionUI()
        startSessionTimer()
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(receiver)
        sessionTimerJob?.cancel()
    }

    private fun startSessionTimer() {
        sessionTimerJob?.cancel()
        sessionTimerJob = lifecycleScope.launch {
            while (isActive) {
                refreshSessionUI()
                delay(60_000)
            }
        }
    }

    private fun refreshSessionUI() {
        val remaining = AdManager.remainingMinutes(this)
        val adsWatched = AdManager.adsWatchedInSession(this)
        vm.updateSessionLabel(remaining, adsWatched)
        updateAdButtons(remaining, adsWatched)
    }

    private fun updateAdButtons(remaining: Long, adsWatched: Int) {
        when {
            remaining <= 0 -> {
                b.btnAd1.visibility = View.VISIBLE
                b.btnAd2.visibility = View.GONE
                b.btnAd1.text = "▶  Watch Ad — Get 2 Hours Free"
                b.btnAd1.setBackgroundColor(Color.parseColor("#2563EB"))
                b.btnAd1.isEnabled = true
            }
            adsWatched == 1 -> {
                b.btnAd1.visibility = View.GONE
                b.btnAd2.visibility = View.VISIBLE
                b.btnAd2.text = "▶  Watch 2nd Ad — Upgrade to 12 Hours"
                b.btnAd2.setBackgroundColor(Color.parseColor("#7C3AED"))
                b.btnAd2.isEnabled = true
            }
            else -> {
                b.btnAd1.visibility = View.GONE
                b.btnAd2.visibility = View.GONE
            }
        }
    }

    private fun observe() {
        vm.state.observe(this)        { updateUi(it) }
        vm.sessionLabel.observe(this) { b.tvSession.text = it }
        vm.adsWatched.observe(this)   { watched ->
            b.ivStar1.setColorFilter(
                if (watched >= 1) Color.parseColor("#F59E0B")
                else Color.parseColor("#2A3A4A")
            )
            b.ivStar2.setColorFilter(
                if (watched >= 2) Color.parseColor("#F59E0B")
                else Color.parseColor("#2A3A4A")
            )
        }
        vm.ipInfo.observe(this) {
            b.tvIp.text      = it.ip
            b.tvCountry.text = it.country
            b.tvCity.text    = it.city
            b.tvIsp.text     = it.isp.take(20)
        }
        vm.dn.observe(this)   { b.tvDn.text = it }
        vm.up.observe(this)   { b.tvUp.text = it }
        vm.ping.observe(this) { b.tvPing.text = it }
        vm.accountIdx.observe(this) { idx ->
            val acc = vm.accounts.getOrNull(idx) ?: return@observe
            b.tvSelServer.text = "${acc.flag}  ${acc.region}"
            b.tvSelDetail.text = "Cloudflare WARP • ${acc.region}"
        }
    }

    private fun clicks() {
        b.btnConnect.setOnClickListener {
            when (vm.state.value) {
                VpnState.CONNECTED, VpnState.CONNECTING -> doStop()
                else -> handleConnectTap()
            }
        }
        b.btnAd1.setOnClickListener { showFirstAd() }
        b.btnAd2.setOnClickListener { showSecondAd() }
        b.cardServer.setOnClickListener { showRegionPicker() }
        b.btnRefreshIp.setOnClickListener { vm.refreshIp() }
    }

    private fun handleConnectTap() {
        if (AdManager.hasActiveSession(this)) {
            requestPerm()
        } else {
            AlertDialog.Builder(this)
                .setTitle("Free VPN Access")
                .setMessage("Watch a short ad to get 2 hours of free VPN.\nWatch 2 ads to get 12 hours!")
                .setPositiveButton("Watch Ad (2h)") { _, _ -> showFirstAd() }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun showFirstAd() {
        b.btnAd1.isEnabled = false
        b.btnAd1.text = "Loading ad..."
        AdManager.showFirstAd(
            activity = this,
            onGranted = { hours ->
                toast("✅ $hours hours unlocked!")
                refreshSessionUI()
                b.root.postDelayed({ requestPerm() }, 500)
            },
            onFailed = {
                toast("Ad not ready, please try again")
                b.btnAd1.isEnabled = true
                b.btnAd1.text = "▶  Watch Ad — Get 2 Hours Free"
            }
        )
    }

    private fun showSecondAd() {
        b.btnAd2.isEnabled = false
        b.btnAd2.text = "Loading ad..."
        AdManager.showSecondAd(
            activity = this,
            onGranted = { hours ->
                toast("🎉 $hours hours unlocked!")
                refreshSessionUI()
            },
            onFailed = {
                toast("Ad not ready, please try again")
                b.btnAd2.isEnabled = true
                b.btnAd2.text = "▶  Watch 2nd Ad — Upgrade to 12 Hours"
            }
        )
    }

    private fun requestPerm() {
        val intent = VpnService.prepare(this)
        if (intent != null) permLauncher.launch(intent) else doStart()
    }

    private fun doStart() {
        val idx = vm.accountIdx.value ?: 0
        ContextCompat.startForegroundService(
            this,
            Intent(this, WarpVpnService::class.java).apply {
                action = WarpVpnService.ACTION_CONNECT
                putExtra(WarpVpnService.EXTRA_ACCOUNT_IDX, idx)
            }
        )
    }

    private fun doStop() {
        startService(Intent(this, WarpVpnService::class.java).apply {
            action = WarpVpnService.ACTION_DISCONNECT
        })
        AdManager.clearSession(this)
        refreshSessionUI()
    }

    private fun showRegionPicker() {
        val items = vm.accounts.map { "${it.flag} ${it.region}" }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Choose Region")
            .setItems(items) { _, idx ->
                vm.selectAccount(idx)
                if (vm.state.value == VpnState.CONNECTED) {
                    doStop()
                    b.root.postDelayed({ requestPerm() }, 1200)
                }
            }.show()
    }

    private fun updateUi(state: VpnState) {
        when (state) {
            VpnState.IDLE -> {
                b.btnConnect.text = "CONNECT"
                b.btnConnect.setBackgroundColor(Color.parseColor("#2563EB"))
                b.tvStatus.text    = "Not Connected"
                b.tvStatusSub.text = "Your real IP is visible"
                b.dotStatus.setBackgroundResource(R.drawable.dot_grey)
                b.layoutSpeed.visibility = View.GONE
                b.tvProtection.text = "⚠️  Unprotected"
                b.tvProtection.setTextColor(Color.parseColor("#F59E0B"))
            }
            VpnState.CONNECTING -> {
                b.btnConnect.text = "CONNECTING..."
                b.btnConnect.setBackgroundColor(Color.parseColor("#D97706"))
                b.tvStatus.text    = "Connecting"
                b.tvStatusSub.text = "Establishing Cloudflare tunnel..."
                b.dotStatus.setBackgroundResource(R.drawable.dot_orange)
                b.layoutSpeed.visibility = View.GONE
                b.tvProtection.text = "🔄  Encrypting..."
                b.tvProtection.setTextColor(Color.parseColor("#F59E0B"))
            }
            VpnState.CONNECTED -> {
                b.btnConnect.text = "DISCONNECT"
                b.btnConnect.setBackgroundColor(Color.parseColor("#16A34A"))
                b.tvStatus.text    = "Connected"
                b.tvStatusSub.text = "Cloudflare WARP • IP Changed ✓"
                b.dotStatus.setBackgroundResource(R.drawable.dot_green)
                b.layoutSpeed.visibility = View.VISIBLE
                b.tvProtection.text = "🛡️  Protected"
                b.tvProtection.setTextColor(Color.parseColor("#22C55E"))
            }
            VpnState.ERROR -> {
                b.btnConnect.text = "RETRY"
                b.btnConnect.setBackgroundColor(Color.parseColor("#DC2626"))
                b.tvStatus.text    = "Connection Failed"
                b.tvStatusSub.text = "Tap RETRY to try again"
                b.dotStatus.setBackgroundResource(R.drawable.dot_red)
                b.layoutSpeed.visibility = View.GONE
                b.tvProtection.text = "❌  Error"
                b.tvProtection.setTextColor(Color.parseColor("#EF4444"))
            }
        }
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}
