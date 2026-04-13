package com.uvpn.app.vpn

import android.app.*
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.uvpn.app.R
import com.uvpn.app.ui.MainActivity
import kotlinx.coroutines.*
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress

class WarpVpnService : VpnService() {

    companion object {
        const val ACTION_CONNECT    = "com.uvpn.app.CONNECT"
        const val ACTION_DISCONNECT = "com.uvpn.app.DISCONNECT"
        const val EXTRA_ACCOUNT_IDX = "account_idx"
        const val NOTIF_ID  = 101
        const val CH_ID     = "uvpn_warp"
        const val BROADCAST = "com.uvpn.app.STATE"
        const val EXTRA_STATE = "state"
        const val ST_CONNECTING   = "CONNECTING"
        const val ST_CONNECTED    = "CONNECTED"
        const val ST_DISCONNECTED = "DISCONNECTED"
        const val ST_ERROR        = "ERROR"
        const val ST_NO_PERM      = "NO_PERMISSION"
        private const val TAG       = "WarpVPN"
        private const val WARP_HOST = "162.159.192.1"
        private const val WARP_PORT = 2408
        private const val MTU       = 1280
    }

    private var tunFd: ParcelFileDescriptor? = null
    private var udpSocket: DatagramSocket? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return when (intent?.action) {
            ACTION_CONNECT -> {
                val idx = intent.getIntExtra(EXTRA_ACCOUNT_IDX, 0)
                scope.launch { connect(idx) }
                START_STICKY
            }
            ACTION_DISCONNECT -> {
                disconnect()
                START_NOT_STICKY
            }
            else -> START_NOT_STICKY
        }
    }

    private suspend fun connect(accountIdx: Int) {
        broadcast(ST_CONNECTING)
        createChannel()
        startForeground(NOTIF_ID, buildNotif("Connecting to Cloudflare WARP..."))

        val account = WarpConfig.accounts.getOrElse(accountIdx) { WarpConfig.accounts[0] }

        try {
            // ── Step 1: Protect UDP socket BEFORE building tunnel ──
            // This is CRITICAL — socket must be protected before
            // the VPN interface is established to avoid routing loop
            val socket = DatagramSocket()
            protect(socket)
            socket.connect(InetSocketAddress(WARP_HOST, WARP_PORT))
            socket.soTimeout = 10000
            udpSocket = socket

            // ── Step 2: Build the TUN interface ──────────────────
            val builder = Builder()
                .setSession("U VPN")
                .addAddress(
                    account.clientIPv4.substringBefore("/"),
                    account.clientIPv4.substringAfter("/").toIntOrNull() ?: 32
                )
                // Route most traffic through VPN but exclude the WARP server itself
                // to prevent internet cutoff
                .addRoute("0.0.0.0", 1)          // 0.0.0.0/1 (first half of internet)
                .addRoute("128.0.0.0", 1)         // 128.0.0.0/1 (second half)
                // Exclude Cloudflare WARP endpoints from VPN routing
                .addDisallowedApplication(packageName)  // exclude ourselves
                .addDnsServer("1.1.1.1")
                .addDnsServer("1.0.0.1")
                .setMtu(MTU)
                .setBlocking(false)
                .allowBypass()

            tunFd = builder.establish()

            if (tunFd == null) {
                broadcast(ST_NO_PERM)
                stopSelf()
                return
            }

            startForeground(NOTIF_ID, buildNotif("Connected • ${account.flag} ${account.region}"))
            broadcast(ST_CONNECTED)
            Log.i(TAG, "VPN connected — ${account.region}")

            // ── Step 3: Packet forwarding loop ───────────────────
            val inJob  = scope.launch { runInbound() }
            val outJob = scope.launch { runOutbound() }
            joinAll(inJob, outJob)

        } catch (e: SecurityException) {
            Log.e(TAG, "Security: ${e.message}")
            broadcast(ST_NO_PERM)
            stopSelf()
        } catch (e: Exception) {
            Log.e(TAG, "Error: ${e.message}")
            broadcast(ST_ERROR)
            stopSelf()
        }
    }

    private suspend fun runOutbound() = withContext(Dispatchers.IO) {
        val tun = tunFd ?: return@withContext
        val udp = udpSocket ?: return@withContext
        val buf = ByteArray(MTU)
        val stream = FileInputStream(tun.fileDescriptor)
        try {
            while (isActive && tunFd != null) {
                val len = stream.read(buf)
                if (len > 0) udp.send(DatagramPacket(buf.copyOf(len), len))
            }
        } catch (e: Exception) {
            if (isActive) Log.d(TAG, "Outbound ended: ${e.message}")
        }
    }

    private suspend fun runInbound() = withContext(Dispatchers.IO) {
        val tun = tunFd ?: return@withContext
        val udp = udpSocket ?: return@withContext
        val buf = ByteArray(MTU + 100)
        val stream = FileOutputStream(tun.fileDescriptor)
        try {
            while (isActive && tunFd != null) {
                val pkt = DatagramPacket(buf, buf.size)
                udp.receive(pkt)
                if (pkt.length > 0) stream.write(buf, 0, pkt.length)
            }
        } catch (e: Exception) {
            if (isActive) Log.d(TAG, "Inbound ended: ${e.message}")
        }
    }

    private fun disconnect() {
        scope.coroutineContext.cancelChildren()
        runCatching { udpSocket?.close() }
        runCatching { tunFd?.close() }
        udpSocket = null
        tunFd = null
        broadcast(ST_DISCONNECTED)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun broadcast(state: String) {
        sendBroadcast(Intent(BROADCAST).putExtra(EXTRA_STATE, state))
    }

    private fun createChannel() {
        val ch = NotificationChannel(CH_ID, "VPN Status",
            NotificationManager.IMPORTANCE_LOW).apply { setShowBadge(false) }
        getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    private fun buildNotif(text: String): Notification {
        val tap = PendingIntent.getActivity(this, 0,
            Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val stop = PendingIntent.getService(this, 1,
            Intent(this, WarpVpnService::class.java).apply { action = ACTION_DISCONNECT },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Builder(this, CH_ID)
            .setContentTitle("U VPN")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentIntent(tap)
            .addAction(0, "Disconnect", stop)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        scope.cancel()
        runCatching { udpSocket?.close() }
        runCatching { tunFd?.close() }
        super.onDestroy()
    }
}
