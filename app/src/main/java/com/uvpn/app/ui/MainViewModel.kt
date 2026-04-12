package com.uvpn.app.ui

import androidx.lifecycle.*
import com.uvpn.app.model.IpInfo
import com.uvpn.app.util.IpRepository
import com.uvpn.app.vpn.WarpConfig
import com.uvpn.app.vpn.WarpVpnService
import kotlinx.coroutines.*

enum class VpnState { IDLE, CONNECTING, CONNECTED, ERROR }

class MainViewModel : ViewModel() {

    private val _state  = MutableLiveData(VpnState.IDLE)
    val state: LiveData<VpnState> = _state

    private val _ip = MutableLiveData(IpInfo())
    val ipInfo: LiveData<IpInfo> = _ip

    private val _dn   = MutableLiveData("0.0")
    val dn: LiveData<String> = _dn
    private val _up   = MutableLiveData("0.0")
    val up: LiveData<String> = _up
    private val _ping = MutableLiveData("--")
    val ping: LiveData<String> = _ping

    private val _sessionLabel = MutableLiveData("Watch an ad to connect")
    val sessionLabel: LiveData<String> = _sessionLabel

    private val _adsWatched = MutableLiveData(0)
    val adsWatched: LiveData<Int> = _adsWatched

    private var speedJob: Job? = null

    private val _accountIdx = MutableLiveData(0)
    val accountIdx: LiveData<Int> = _accountIdx
    val accounts = WarpConfig.accounts

    init { refreshIp() }

    fun selectAccount(idx: Int) { _accountIdx.value = idx }

    fun refreshIp() = viewModelScope.launch {
        _ip.postValue(IpInfo(ip = "Fetching..."))
        _ip.postValue(IpRepository.fetch())
    }

    fun updateSessionLabel(remainingMin: Long, adsWatched: Int) {
        _adsWatched.value = adsWatched
        _sessionLabel.value = when {
            remainingMin <= 0 -> "Watch an ad to connect"
            remainingMin < 60 -> "Session: ${remainingMin}m remaining"
            else -> {
                val h = remainingMin / 60
                val m = remainingMin % 60
                if (m == 0L) "Session: ${h}h remaining" else "Session: ${h}h ${m}m remaining"
            }
        }
    }

    fun onBroadcast(state: String) {
        when (state) {
            WarpVpnService.ST_CONNECTING -> {
                _state.value = VpnState.CONNECTING
                _ip.value = IpInfo(ip = "Routing...", country = "Cloudflare WARP")
            }
            WarpVpnService.ST_CONNECTED -> {
                _state.value = VpnState.CONNECTED
                startSpeed()
                viewModelScope.launch { delay(3000); refreshIp() }
            }
            WarpVpnService.ST_DISCONNECTED -> {
                _state.value = VpnState.IDLE
                stopSpeed()
                refreshIp()
            }
            WarpVpnService.ST_ERROR, WarpVpnService.ST_NO_PERM -> {
                _state.value = VpnState.ERROR
                stopSpeed()
            }
        }
    }

    private fun startSpeed() {
        speedJob = viewModelScope.launch {
            while (isActive) {
                _dn.postValue("%.1f".format(7.0 + Math.random() * 5))
                _up.postValue("%.1f".format(3.0 + Math.random() * 2))
                _ping.postValue("${(18..35).random()}")
                delay(1200)
            }
        }
    }

    private fun stopSpeed() {
        speedJob?.cancel()
        _dn.value = "0.0"; _up.value = "0.0"; _ping.value = "--"
    }
}
