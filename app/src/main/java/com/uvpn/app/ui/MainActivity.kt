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
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding
    private val vm: MainViewModel by viewModels()
    private val FREE_IDX = 0

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { if (it.resultCode == RESULT_OK) doStart() else toast("VPN permission required") }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            vm.onBroadcast(i.getStringExtra(WarpVpnService.EXTRA_STATE) ?: return)
        }
    }

    private var timerJob: Job? = null

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)
        AdManager.init(this)
        vm.selectAccount(FREE_IDX)
        observe()
        clicks()
        updateAdUi()
    }

    override fun onResume() {
        super.onResume()
        ContextCompat.registerReceiver(
            this, receiver,
            IntentFilter(WarpVpnService.BROADCAST),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        updateAdUi()
        timerJob = lifecycleScope.launch {
            while (isActive) { delay(60_000); updateAdUi() }
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(receiver)
        timerJob?.cancel()
    }

    private fun updateAdUi() {
        val isFree    = (vm.accountIdx.value ?: 0) == FREE_IDX
        val remaining = AdManager.remainingMinutes(this)
        val watched   = AdManager.adsWatchedInSession(this)
        vm.updateSessionLabel(remaining, watched)

        if (isFree) {
            b.adCard.visibility = View.GONE
            b.tvSession.text    = "🆓 Free Server — No Ads"
        } else {
            b.adCard.visibility = View.VISIBLE
            when {
                remaining <= 0 -> {
                    b.btnAd1.visibility = View.VISIBLE
                    b.btnAd2.visibility = View.GONE
                    b.btnAd1.isEnabled  = true
                    b.btnAd1.text = "▶  Watch Ad — Get 2 Hours Free"
                }
                watched == 1 -> {
                    b.btnAd1.visibility = View.GONE
                    b.btnAd2.visibility = View.VISIBLE
                    b.btnAd2.isEnabled  = true
                }
                else -> {
                    b.btnAd1.visibility = View.GONE
                    b.btnAd2.visibility = View.GONE
                }
            }
        }
    }

    private fun observe() {
        vm.state.observe(this) { updateUi(it) }
        vm.ipInfo.observe(this) {
            b.tvIp.text      = it.ip
            b.tvCountry.text = it.country
            b.tvCity.text    = it.city
            b.tvIsp.text     = it.isp.take(20)
        }
        vm.dn.observe(this)   { b.tvDn.text = it }
        vm.up.observe(this)   { b.tvUp.text = it }
        vm.ping.observe(this) { b.tvPing.text = it }
        vm.adsWatched.observe(this) { w ->
            b.ivStar1.setColorFilter(
                if (w >= 1) Color.parseColor("#F59E0B") else Color.parseColor("#2A3A4A"))
            b.ivStar2.setColorFilter(
                if (w >= 2) Color.parseColor("#F59E0B") else Color.parseColor("#2A3A4A"))
        }
        vm.accountIdx.observe(this) { idx ->
            val acc = vm.accounts.getOrNull(idx) ?: return@observe
            b.tvSelServer.text = "${acc.flag}  ${acc.region}"
            b.tvSelDetail.text = if (idx == FREE_IDX) "🆓 Free • No Ads" else "⭐ VIP • Ad Required"
            updateAdUi()
        }
    }

    private fun clicks() {
        b.btnConnect.setOnClickListener {
            when (vm.state.value) {
                VpnState.CONNECTED, VpnState.CONNECTING -> doStop()
                else -> onConnectTap()
            }
        }
        b.btnAd1.setOnClickListener { showAd1() }
        b.btnAd2.setOnClickListener { showAd2() }
        b.cardServer.setOnClickListener { pickServer() }
        b.btnRefreshIp.setOnClickListener { vm.refreshIp() }
    }

    private fun onConnectTap() {
        val isFree = (vm.accountIdx.value ?: 0) == FREE_IDX
        if (isFree || AdManager.hasActiveSession(this)) {
            requestPerm()
        } else {
            AlertDialog.Builder(this)
                .setTitle("VIP Server")
                .setMessage("Watch an ad to unlock.\nOr use 🆓 Free Server (no ads).")
                .setPositiveButton("Watch Ad") { _, _ -> showAd1() }
                .setNeutralButton("Free Server") { _, _ ->
                    vm.selectAccount(FREE_IDX); requestPerm()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun showAd1() {
        b.btnAd1.isEnabled = false; b.btnAd1.text = "Loading..."
        AdManager.showFirstAd(this,
            onGranted = { h ->
                toast("✅ ${h}h unlocked!")
                updateAdUi()
                b.root.postDelayed({ requestPerm() }, 400)
            },
            onFailed = {
                toast("Ad not ready")
                b.btnAd1.isEnabled = true
                b.btnAd1.text = "▶  Watch Ad — Get 2 Hours Free"
            }
        )
    }

    private fun showAd2() {
        b.btnAd2.isEnabled = false; b.btnAd2.text = "Loading..."
        AdManager.showSecondAd(this,
            onGranted = { h -> toast("🎉 ${h}h unlocked!"); updateAdUi() },
            onFailed = {
                toast("Ad not ready")
                b.btnAd2.isEnabled = true
                b.btnAd2.text = "▶  Watch 2nd Ad — Upgrade to 12 Hours"
            }
        )
    }

    private fun requestPerm() {
        val i = VpnService.prepare(this)
        if (i != null) permLauncher.launch(i) else doStart()
    }

    private fun doStart() {
        ContextCompat.startForegroundService(this,
            Intent(this, WarpVpnService::class.java).apply {
                action = WarpVpnService.ACTION_CONNECT
                putExtra(WarpVpnService.EXTRA_ACCOUNT_IDX, vm.accountIdx.value ?: 0)
            })
    }

    private fun doStop() {
        startService(Intent(this, WarpVpnService::class.java).apply {
            action = WarpVpnService.ACTION_DISCONNECT
        })
        AdManager.clearSession(this)
        updateAdUi()
    }

    private fun pickServer() {
        val items = vm.accounts.mapIndexed { i, acc ->
            "${acc.flag}  ${acc.region}${if (i == FREE_IDX) " 🆓 Free" else " ⭐ VIP"}"
        }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Choose Server")
            .setItems(items) { _, idx ->
                vm.selectAccount(idx)
                if (vm.state.value == VpnState.CONNECTED) {
                    doStop()
                    b.root.postDelayed({ onConnectTap() }, 1000)
                }
            }.show()
    }

    private fun updateUi(state: VpnState) {
        when (state) {
            VpnState.IDLE -> {
                b.btnConnect.text = "CONNECT"
                b.btnConnect.setBackgroundColor(Color.parseColor("#2563EB"))
                b.tvStatus.text    = "Not Connected"
                b.tvStatusSub.text = "Tap to connect"
                b.dotStatus.setBackgroundResource(R.drawable.dot_grey)
                b.layoutSpeed.visibility = View.GONE
                b.tvProtection.text = "⚠️  Unprotected"
                b.tvProtection.setTextColor(Color.parseColor("#F59E0B"))
            }
            VpnState.CONNECTING -> {
                b.btnConnect.text = "CONNECTING..."
                b.btnConnect.setBackgroundColor(Color.parseColor("#D97706"))
                b.tvStatus.text    = "Connecting"
                b.tvStatusSub.text = "Establishing tunnel..."
                b.dotStatus.setBackgroundResource(R.drawable.dot_orange)
                b.layoutSpeed.visibility = View.GONE
                b.tvProtection.text = "🔄  Encrypting..."
                b.tvProtection.setTextColor(Color.parseColor("#F59E0B"))
            }
            VpnState.CONNECTED -> {
                // ── KEY FIX: shows DISCONNECT (red) when connected ──
                b.btnConnect.text = "DISCONNECT"
                b.btnConnect.setBackgroundColor(Color.parseColor("#DC2626"))
                b.tvStatus.text    = "Connected"
                b.tvStatusSub.text = "Tap to disconnect"
                b.dotStatus.setBackgroundResource(R.drawable.dot_green)
                b.layoutSpeed.visibility = View.VISIBLE
                b.tvProtection.text = "🛡️  Protected"
                b.tvProtection.setTextColor(Color.parseColor("#22C55E"))
            }
            VpnState.ERROR -> {
                b.btnConnect.text = "RETRY"
                b.btnConnect.setBackgroundColor(Color.parseColor("#7C3AED"))
                b.tvStatus.text    = "Connection Failed"
                b.tvStatusSub.text = "Tap RETRY"
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
