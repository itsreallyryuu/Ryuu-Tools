package com.ryuutools.app

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku
import java.io.InputStream
import java.io.OutputStream

object ShizukuHelper {

    const val REQUEST_CODE = 9001

    fun isAvailable(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Throwable) {
            false
        }
    }

    fun hasPermission(): Boolean {
        return try {
            if (Shizuku.isPreV11()) {
                false
            } else {
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            }
        } catch (e: Throwable) {
            false
        }
    }

    fun requestPermission(listener: (granted: Boolean) -> Unit) {
        try {
            val callback = object : Shizuku.OnRequestPermissionResultListener {
                override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
                    if (requestCode == REQUEST_CODE) {
                        Shizuku.removeRequestPermissionResultListener(this)
                        listener(grantResult == PackageManager.PERMISSION_GRANTED)
                    }
                }
            }
            Shizuku.addRequestPermissionResultListener(callback)
            Shizuku.requestPermission(REQUEST_CODE)
        } catch (e: Throwable) {
            listener(false)
        }
    }

    /**
     * ⚠️ CATATAN TEKNIS: Shizuku.newProcess() disembunyikan di versi terbaru.
     * Ini pakai reflection (cara akal komunitas developer) buat tetap manggilnya.
     * Kalau Shizuku benar-benar menghapus total method ini di update mendatang,
     * fungsi ini bisa berhenti berfungsi dan perlu diganti pakai UserService (AIDL).
     *
     * One-shot: bikin proses baru, jalanin satu command, proses mati. Dipakai fitur
     * yang cuma butuh jalanin 1 command dan selesai (Memory Sweep, Gaming Mode, dll).
     * TIDAK mempertahankan state (cd, env var) antar panggilan — tiap panggilan proses baru.
     */
    fun runCommand(cmd: String): Pair<Boolean, String> {
        return try {
            val clazz = Class.forName("rikka.shizuku.Shizuku")
            val method = clazz.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            method.isAccessible = true
            val process = method.invoke(null, arrayOf("sh", "-c", cmd), null, null)
            val inputStream = process.javaClass.getMethod("getInputStream").invoke(process) as InputStream
            val errorStream = process.javaClass.getMethod("getErrorStream").invoke(process) as InputStream
            val output = inputStream.bufferedReader().use { it.readText() }
            val error = errorStream.bufferedReader().use { it.readText() }
            process.javaClass.getMethod("destroy").invoke(process)
            if (error.isNotBlank()) Pair(false, error.trim()) else Pair(true, output.trim())
        } catch (e: Exception) {
            Pair(false, "Error: ${e.message}")
        }
    }

    // ---------------------------------------------------------------------
    // Persistent interactive shell session (used by the Terminal feature).
    // One long-lived "sh" process stays open; commands are written to its
    // stdin and output is read continuously from stdout/stderr. This is what
    // makes "cd", environment variables, and background jobs (&) behave like
    // a real terminal instead of resetting after every command.
    // ---------------------------------------------------------------------

    private var persistentProcess: Any? = null
    private var persistentStdin: OutputStream? = null
    private var persistentStdout: InputStream? = null
    private var persistentStderr: InputStream? = null

    fun startPersistentShell(): Boolean {
        return try {
            val clazz = Class.forName("rikka.shizuku.Shizuku")
            val method = clazz.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            method.isAccessible = true
            // No "-c", no command: this launches an interactive shell that keeps
            // reading further commands from stdin until we send "exit".
            val process = method.invoke(null, arrayOf("sh"), null, null)
            persistentProcess = process
            persistentStdin = process.javaClass.getMethod("getOutputStream").invoke(process) as OutputStream
            persistentStdout = process.javaClass.getMethod("getInputStream").invoke(process) as InputStream
            persistentStderr = process.javaClass.getMethod("getErrorStream").invoke(process) as InputStream
            true
        } catch (e: Exception) {
            false
        }
    }

    fun writeToPersistentShell(cmd: String) {
        try {
            persistentStdin?.write((cmd + "\n").toByteArray())
            persistentStdin?.flush()
        } catch (e: Exception) {
            // Aman, sesi mungkin sudah mati — dibaca lewat isPersistentShellAlive()
        }
    }

    fun getPersistentStdout(): InputStream? = persistentStdout
    fun getPersistentStderr(): InputStream? = persistentStderr

    fun isPersistentShellAlive(): Boolean {
        val process = persistentProcess ?: return false
        return try {
            process.javaClass.getMethod("exitValue").invoke(process)
            false // kalau exitValue() berhasil dipanggil tanpa exception, proses sudah selesai
        } catch (e: java.lang.reflect.InvocationTargetException) {
            true // exitValue() melempar exception kalau proses masih berjalan — ini kondisi normal (masih hidup)
        } catch (e: Exception) {
            false
        }
    }

    fun destroyPersistentShell() {
        try {
            persistentStdin?.write("exit\n".toByteArray())
            persistentStdin?.flush()
        } catch (e: Exception) { }
        try {
            persistentProcess?.javaClass?.getMethod("destroy")?.invoke(persistentProcess)
        } catch (e: Exception) { }
        persistentProcess = null
        persistentStdin = null
        persistentStdout = null
        persistentStderr = null
    }
}