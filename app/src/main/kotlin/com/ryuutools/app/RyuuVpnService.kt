package com.ryuutools.app

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.content.SharedPreferences
import android.net.VpnService
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress

class RyuuVpnService : VpnService() {

    companion object {
        const val ACTION_STOP = "com.ryuutools.app.VPN_STOP"
        const val BROADCAST_STATS = "com.ryuutools.app.VPN_STATS"
        const val EXTRA_QUERIES = "queries"
        const val EXTRA_BLOCKED = "blocked"

        private const val NOTIFICATION_ID = 6001
        private const val TUNNEL_DEVICE_IP = "10.111.222.2"
        private const val TUNNEL_DNS_IP = "10.111.222.1"

        @Volatile var isRunning = false
            private set
    }

    private var tunInterface: ParcelFileDescriptor? = null
    @Volatile private var running = false
    private var readerThread: Thread? = null

    private var queryCount = 0
    private var blockedCount = 0

    private lateinit var prefs: SharedPreferences

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences("ryuu_prefs", MODE_PRIVATE)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopVpn()
            return START_NOT_STICKY
        }
        startVpn()
        return START_STICKY
    }

    /**
     * Only routes traffic destined to our internal DNS IP into the tunnel
     * (NOT a 0.0.0.0/0 catch-all). This is what makes it a DNS-only VPN:
     * regular browsing/app traffic keeps flowing through the normal network
     * path untouched, only DNS resolution gets intercepted.
     */
    private fun startVpn() {
        if (running) return

        val builder = Builder()
            .setSession("Ryuu VPN")
            .addAddress(TUNNEL_DEVICE_IP, 32)
            .addDnsServer(TUNNEL_DNS_IP)
            .addRoute(TUNNEL_DNS_IP, 32)
            .setMtu(1500)

        tunInterface = try {
            builder.establish()
        } catch (e: Exception) {
            null
        }

        if (tunInterface == null) {
            stopSelf()
            return
        }

        running = true
        isRunning = true
        startForeground(NOTIFICATION_ID, buildNotification())

        readerThread = Thread { runPacketLoop() }
        readerThread?.isDaemon = true
        readerThread?.start()
    }

    private fun runPacketLoop() {
        val input = FileInputStream(tunInterface!!.fileDescriptor)
        val output = FileOutputStream(tunInterface!!.fileDescriptor)
        val buffer = ByteArray(32767)

        try {
            while (running) {
                val length = input.read(buffer)
                if (length <= 0) continue

                val udpPacket = PacketUtils.parseIpv4Udp(buffer, length) ?: continue
                if (udpPacket.dstPort != 53) continue

                val queryDomain = DnsUtils.extractQueryDomain(udpPacket.payload)
                queryCount++

                val adBlockEnabled = prefs.getBoolean("vpn_ad_block_enabled", true)

                if (adBlockEnabled && DnsBlocklist.isBlocked(queryDomain)) {
                    blockedCount++
                    val nxResponse = DnsUtils.buildNxDomainResponse(udpPacket.payload)
                    writeResponse(output, udpPacket, nxResponse)
                    broadcastStats()
                } else {
                    Thread { forwardToUpstream(udpPacket, output) }.start()
                }
            }
        } catch (e: Exception) {
            // Loop berhenti — biasanya karena tunInterface ditutup pas stopVpn()
        }
    }

    private fun forwardToUpstream(udpPacket: PacketUtils.ParsedUdpPacket, output: FileOutputStream) {
        try {
            val upstreamDns = prefs.getString("vpn_upstream_dns", "1.1.1.1") ?: "1.1.1.1"
            val socket = DatagramSocket()
            protect(socket) // WAJIB: cegah traffic ini balik masuk tunnel lagi (infinite loop)
            socket.soTimeout = 5000

            socket.send(DatagramPacket(udpPacket.payload, udpPacket.payload.size, InetSocketAddress(upstreamDns, 53)))

            val responseBuffer = ByteArray(4096)
            val responsePacket = DatagramPacket(responseBuffer, responseBuffer.size)
            socket.receive(responsePacket)
            socket.close()

            val responseBytes = responseBuffer.copyOfRange(0, responsePacket.length)
            writeResponse(output, udpPacket, responseBytes)
            broadcastStats()
        } catch (e: Exception) {
            // Timeout / DNS server nggak jawab — query itu doang yang gagal, bukan seluruh VPN
        }
    }

    @Synchronized
    private fun writeResponse(output: FileOutputStream, original: PacketUtils.ParsedUdpPacket, dnsPayload: ByteArray) {
        try {
            val responsePacket = PacketUtils.buildIpv4UdpPacket(
                srcIp = original.dstIp, srcPort = original.dstPort,
                dstIp = original.srcIp, dstPort = original.srcPort,
                payload = dnsPayload
            )
            output.write(responsePacket)
        } catch (e: Exception) {
            // Satu response gagal ditulis nggak boleh matiin service
        }
    }

    private fun broadcastStats() {
        val intent = Intent(BROADCAST_STATS)
        intent.setPackage(packageName)
        intent.putExtra(EXTRA_QUERIES, queryCount)
        intent.putExtra(EXTRA_BLOCKED, blockedCount)
        sendBroadcast(intent)
    }

    private fun stopVpn() {
        running = false
        isRunning = false
        try { readerThread?.interrupt() } catch (e: Exception) { }
        try { tunInterface?.close() } catch (e: Exception) { }
        tunInterface = null
        stopForeground(true)
        stopSelf()
    }

    private fun buildNotification(): Notification {
        val stopIntent = Intent(this, RyuuVpnService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, NotificationHelper.LIVE_STATS_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle("Ryuu VPN — DNS Privacy active")
            .setContentText("Private DNS + ad-block running")
            .setOngoing(true)
            .addAction(0, "Stop", stopPendingIntent)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopVpn()
    }

    override fun onRevoke() {
        super.onRevoke()
        stopVpn()
    }
}