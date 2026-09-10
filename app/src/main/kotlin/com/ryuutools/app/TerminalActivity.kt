package com.ryuutools.app

import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class TerminalActivity : BaseActivity() {

    private var service: TerminalSessionService? = null
    private var isBound = false
    private var promptDialog: AlertDialog? = null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as TerminalSessionService.LocalBinder
            service = localBinder.getService()
            isBound = true
            onServiceReady()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_terminal)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        val etCommand = findViewById<EditText>(R.id.etCommand)
        val btnRun = findViewById<Button>(R.id.btnRunCommand)
        val btnClear = findViewById<Button>(R.id.btnClearOutput)
        val btnKillSession = findViewById<Button>(R.id.btnKillSession)

        btnRun.setOnClickListener { submitCommand(etCommand) }

        etCommand.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEND ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {
                submitCommand(etCommand)
                true
            } else {
                false
            }
        }

        btnClear.setOnClickListener {
            findViewById<TextView>(R.id.tvOutput).text = ""
        }

        btnKillSession.setOnClickListener {
            if (!ShizukuHelper.hasPermission()) {
                showConnectPrompt()
                return@setOnClickListener
            }
            service?.restartSession()
            findViewById<TextView>(R.id.tvOutput).text = ""
            updateSessionStatus()
            Toast.makeText(this, "Session restarted", Toast.LENGTH_SHORT).show()
        }

        val serviceIntent = Intent(this, TerminalSessionService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    override fun onStart() {
        super.onStart()
        bindService(Intent(this, TerminalSessionService::class.java), connection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()
        service?.setOutputListener(null)
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
    }

    private fun onServiceReady() {
        val tvOutput = findViewById<TextView>(R.id.tvOutput)
        tvOutput.text = service?.getFullLog() ?: ""
        scrollToBottom()

        service?.setOutputListener { chunk ->
            runOnUiThread {
                tvOutput.append(chunk)
                scrollToBottom()
            }
        }

        checkRootModeAndPrompt()
    }

    /**
     * Called every time this screen comes back into view (rebind fires onServiceReady
     * again). If Root Mode is connected now but the session never started, retry it
     * automatically — this is what makes "go connect, then come back" work seamlessly.
     */
    private fun checkRootModeAndPrompt() {
        updateSessionStatus()
        val active = service?.isSessionActive() == true
        if (active) {
            promptDialog?.dismiss()
            return
        }

        if (!ShizukuHelper.hasPermission()) {
            showConnectPrompt()
        } else {
            service?.restartSession()
            updateSessionStatus()
        }
    }

    private fun showConnectPrompt() {
        if (promptDialog?.isShowing == true) return
        promptDialog = AlertDialog.Builder(this)
            .setTitle("Root Mode Required")
            .setMessage("The Terminal needs Root Mode to run commands. Connect it in System Boost, then come back here — the session will start automatically.")
            .setPositiveButton("Go to System Boost") { _, _ ->
                startActivity(Intent(this, SystemBoostActivity::class.java))
            }
            .setNegativeButton("Cancel", null)
            .setCancelable(true)
            .show()
    }

    private fun updateSessionStatus() {
        val dot = findViewById<View>(R.id.dotShizukuStatus)
        val tvStatus = findViewById<TextView>(R.id.tvShizukuStatus)
        val active = service?.isSessionActive() == true

        when {
            active -> {
                dot.background.setTint(Color.parseColor("#4CD964"))
                tvStatus.text = "Session running"
            }
            !ShizukuHelper.hasPermission() -> {
                dot.background.setTint(Color.parseColor("#FF2D55"))
                tvStatus.text = "Root Mode required"
            }
            else -> {
                dot.background.setTint(Color.parseColor("#FF2D55"))
                tvStatus.text = "Session not active"
            }
        }
    }

    private fun submitCommand(etCommand: EditText) {
        val cmd = etCommand.text.toString().trim()
        if (cmd.isEmpty()) return

        if (service?.isSessionActive() != true) {
            if (!ShizukuHelper.hasPermission()) {
                showConnectPrompt()
            } else {
                Toast.makeText(this, "Session not active. Tap Restart Session.", Toast.LENGTH_LONG).show()
            }
            return
        }

        service?.sendCommand(cmd)
        etCommand.text.clear()
    }

    private fun scrollToBottom() {
        val scrollView = findViewById<ScrollView>(R.id.scrollOutput)
        scrollView.post { scrollView.fullScroll(View.FOCUS_DOWN) }
    }
}