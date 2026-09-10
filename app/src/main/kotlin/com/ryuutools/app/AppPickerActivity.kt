package com.ryuutools.app

import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AppPickerActivity : BaseActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var adapter: InstalledAppsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_picker)

        prefs = getSharedPreferences("ryuu_prefs", MODE_PRIVATE)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        val rv = findViewById<RecyclerView>(R.id.rvInstalledApps)
        rv.layoutManager = LinearLayoutManager(this)

        val etSearch = findViewById<EditText>(R.id.etSearchApps)
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                adapter.filter(s?.toString() ?: "")
            }
        })

        Thread {
            val apps = loadLaunchableApps()
            runOnUiThread {
                findViewById<ProgressBar>(R.id.progressLoadingApps).visibility = View.GONE
                adapter = InstalledAppsAdapter(
                    allApps = apps,
                    isAddedCheck = { pkg -> getBoostedSet().contains(pkg) },
                    onToggle = { app -> toggleApp(app.packageName) }
                )
                rv.adapter = adapter
            }
        }.start()
    }

    private fun loadLaunchableApps(): List<InstallableAppInfo> {
        val pm = packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        return apps
            .filter { app ->
                app.packageName != packageName && pm.getLaunchIntentForPackage(app.packageName) != null
            }
            .map { app ->
                InstallableAppInfo(
                    packageName = app.packageName,
                    label = pm.getApplicationLabel(app).toString(),
                    icon = try { pm.getApplicationIcon(app.packageName) } catch (e: Exception) { null }
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    private fun getBoostedSet(): Set<String> {
        return prefs.getStringSet("gaming_boosted_apps", emptySet()) ?: emptySet()
    }

    private fun toggleApp(packageNameToToggle: String) {
        val current = getBoostedSet().toMutableSet()
        if (current.contains(packageNameToToggle)) {
            current.remove(packageNameToToggle)
        } else {
            current.add(packageNameToToggle)
        }
        prefs.edit().putStringSet("gaming_boosted_apps", current).apply()
    }
}