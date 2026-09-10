package com.ryuutools.app

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import android.app.Service

class TerminalSessionService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 5001
        private const val MAX_LOG_CHARS = 50_000
        const val ACTION_EXIT = "com.ryuutools.app.TERMINAL_EXIT"
    }

    inner class LocalBinder : Binder() {
        fun getService(): TerminalSessionService = this@TerminalSessionService
    }

    private val binder = LocalBinder()
    private val outputLog = StringBuilder()
    private var outputListener: ((String) -> Unit)? = null

    @Volatile private var sessionActive = false
    private var readerThread: Thread? = null

    private var homeDir: String = "/"
    private var currentPath: String = "/"

    private val homeMarkerRegex = Regex("__RYUU_HOME__([^\\n\\r]*)\\r?\\n?")
    private val cwdMarkerRegex = Regex("__RYUU_CWD__([^\\n\\r]*)\\r?\\n?")

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, buildNotification())
        startSession()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_EXIT) {
            stopReaderAndShell()
            stopForeground(true)
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    fun setOutputListener(listener: ((String) -> Unit)?) {
        outputListener = listener
    }

    @Synchronized
    fun getFullLog(): String = outputLog.toString()

    fun isSessionActive(): Boolean = sessionActive

    fun sendCommand(cmd: String) {
        if (!sessionActive) return
        appendToLog("$cmd\n")
        // Marker command nempel di belakang biar kita bisa lacak $PWD abis command
        // ini selesai — dipotong dari tampilan sebelum ditampilkan ke user.
        ShizukuHelper.writeToPersistentShell("$cmd ; echo __RYUU_CWD__\$(pwd)")
    }

    fun restartSession() {
        stopReaderAndShell()
        synchronized(this) { outputLog.clear() }
        homeDir = "/"
        currentPath = "/"
        startSession()
    }

    private fun startSession() {
        if (!ShizukuHelper.isAvailable() || !ShizukuHelper.hasPermission()) {
            appendToLog("Shizuku is not connected. Go to System Boost and connect first.\n")
            sessionActive = false
            return
        }

        val started = ShizukuHelper.startPersistentShell()
        if (!started) {
            appendToLog("Failed to start terminal session.\n")
            sessionActive = false
            return
        }

        sessionActive = true
        appendToLog(welcomeBanner())

        readerThread = Thread {
            val buffer = ByteArray(4096)
            try {
                // Query $HOME dan $PWD begitu sesi mulai, biar prompt awal akurat
                ShizukuHelper.writeToPersistentShell("echo __RYUU_HOME__\$HOME ; echo __RYUU_CWD__\$(pwd)")

                while (sessionActive) {
                    var readAny = false

                    val stdout = ShizukuHelper.getPersistentStdout()
                    val availableOut = stdout?.available() ?: 0
                    if (availableOut > 0) {
                        val n = stdout!!.read(buffer)
                        if (n > 0) {
                            handleStdoutChunk(String(buffer, 0, n))
                            readAny = true
                        }
                    }

                    val stderr = ShizukuHelper.getPersistentStderr()
                    val availableErr = stderr?.available() ?: 0
                    if (availableErr > 0) {
                        val n = stderr!!.read(buffer)
                        if (n > 0) {
                            appendToLog(String(buffer, 0, n))
                            readAny = true
                        }
                    }

                    if (!ShizukuHelper.isPersistentShellAlive()) {
                        appendToLog("\n[Session ended]\n")
                        sessionActive = false
                        break
                    }

                    if (!readAny) Thread.sleep(150)
                }
            } catch (e: Exception) {
                appendToLog("\n[Session error: ${e.message}]\n")
                sessionActive = false
            }
        }
        readerThread?.isDaemon = true
        readerThread?.start()
    }

    /** Extracts our internal path markers out of the stream before it's shown,
     * updates the tracked home/current directory, and prints a fresh prompt
     * line whenever a command finishes (i.e. whenever a CWD marker arrives). */
    private fun handleStdoutChunk(raw: String) {
        var text = raw

        homeMarkerRegex.find(text)?.let { match ->
            homeDir = match.groupValues[1].ifBlank { "/" }
            text = text.replaceFirst(match.value, "")
        }

        val cwdMatch = cwdMarkerRegex.find(text)
        if (cwdMatch != null) {
            currentPath = cwdMatch.groupValues[1].ifBlank { "/" }
            text = text.replaceFirst(cwdMatch.value, "")
            if (text.isNotEmpty()) appendToLog(text)
            appendPromptLine()
        } else if (text.isNotEmpty()) {
            appendToLog(text)
        }
    }

    private fun appendPromptLine() {
        val log = getFullLog()
        val needsNewline = log.isNotEmpty() && !log.endsWith("\n")
        appendToLog((if (needsNewline) "\n" else "") + promptPath() + " $ ")
    }

    /** Shows "~" when the current directory is $HOME, otherwise the full path —
     * same convention as a normal bash prompt. */
    private fun promptPath(): String {
        return if (currentPath == homeDir) "~" else currentPath
    }

    private fun welcomeBanner(): String {
        return """
            Welcome to Ryuu Terminal!

            This is a real ADB shell session via Shizuku — not a full Linux
            environment (no package manager, no apt/pkg install).

            Useful commands:
             - pm list packages       List installed apps
             - dumpsys battery        Battery info
             - settings list system   List system settings
             - ls / cd / cat          Standard shell navigation

            Repo: https://github.com/itsreallyryuu/Ryuu-Tools

            Session keeps running in the background — closing this screen
            won't stop it. Use the notification's Exit button, or the
            Restart Session button here, to end it.

        """.trimIndent() + "\n"
    }

    private fun stopReaderAndShell() {
        sessionActive = false
        ShizukuHelper.destroyPersistentShell()
        try { readerThread?.interrupt() } catch (e: Exception) { }
        readerThread = null
    }

    @Synchronized
    private fun appendToLog(text: String) {
        outputLog.append(text)
        if (outputLog.length > MAX_LOG_CHARS) {
            outputLog.delete(0, outputLog.length - MAX_LOG_CHARS)
        }
        outputListener?.invoke(text)
    }

    private fun buildNotification(): Notification {
        val openIntent = Intent(this, TerminalActivity::class.java)
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val exitIntent = Intent(this, TerminalSessionService::class.java).apply { action = ACTION_EXIT }
        val exitPendingIntent = PendingIntent.getService(
            this, 0, exitIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NotificationHelper.LIVE_STATS_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentTitle("Ryuu Terminal session active")
            .setContentText("Commands keep running in the background. Tap to reopen.")
            .setOngoing(true)
            .setContentIntent(openPendingIntent)
            .addAction(0, "Exit", exitPendingIntent)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopReaderAndShell()
    }
}