package com.ryuutools.app

import android.app.DownloadManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class TikTokDownloaderActivity : BaseActivity() {

    private var videoUrl: String? = null
    private var audioUrl: String? = null
    private var imageUrls: List<String> = emptyList()
    private var baseFileName: String = "tiktok_media"

    private lateinit var inputSection: View
    private lateinit var resultContainer: View
    private lateinit var etLink: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tiktok_downloader)

        inputSection = findViewById(R.id.inputSection)
        resultContainer = findViewById(R.id.resultContainer)
        etLink = findViewById(R.id.etTiktokLink)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<ImageButton>(R.id.btnClearInput).setOnClickListener {
            etLink.text.clear()
        }

        findViewById<Button>(R.id.btnFetch).setOnClickListener {
    val link = etLink.text.toString().trim()
    if (link.isEmpty()) {
        Toast.makeText(this, "Paste a TikTok link first", Toast.LENGTH_SHORT).show()
    } else if (!NetworkUtils.isOnline(this)) {
        NetworkUtils.showOfflineWarning(this)
    } else {
        fetchInfo(link)
    }
}

        findViewById<Button>(R.id.btnDownloadVideo).setOnClickListener {
            videoUrl?.let {
                Toast.makeText(this, "Downloading video...", Toast.LENGTH_SHORT).show()
                downloadFile(it, "${baseFileName}.mp4", "RyuuTools")
            }
        }

        findViewById<Button>(R.id.btnDownloadAudio).setOnClickListener {
            audioUrl?.let {
                Toast.makeText(this, "Downloading audio...", Toast.LENGTH_SHORT).show()
                downloadFile(it, "${baseFileName}_audio.mp3", "RyuuTools")
            }
        }

        findViewById<Button>(R.id.btnDownloadAllPhotos).setOnClickListener {
            imageUrls.forEachIndexed { index, url ->
                downloadFile(url, "${baseFileName}_photo_${index + 1}.jpg", "RyuuTools")
            }
            Toast.makeText(this, "Downloading ${imageUrls.size} photos...", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnReset).setOnClickListener {
            resetToInput()
        }
    }

    private fun resetToInput() {
        videoUrl = null
        audioUrl = null
        imageUrls = emptyList()
        baseFileName = "tiktok_media"
        etLink.text.clear()
        resultContainer.visibility = View.GONE
        inputSection.visibility = View.VISIBLE
    }

    private fun fetchInfo(link: String) {
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val videoModeGroup = findViewById<View>(R.id.videoModeGroup)
        val photoModeGroup = findViewById<View>(R.id.photoModeGroup)
        val btnDownloadAudio = findViewById<Button>(R.id.btnDownloadAudio)

        progressBar.visibility = View.VISIBLE

        Thread {
            try {
                val encodedUrl = URLEncoder.encode(link, "UTF-8")
                val apiUrl = URL("https://www.tikwm.com/api/?url=$encodedUrl&hd=1")
                val connection = apiUrl.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()

                val json = JSONObject(response)
                if (json.getInt("code") != 0) {
                    runOnUiThread {
                        progressBar.visibility = View.GONE
                        Toast.makeText(this, "Video/photo not found or link invalid", Toast.LENGTH_SHORT).show()
                    }
                    return@Thread
                }

                val data = json.getJSONObject("data")
                val title = data.optString("title", "tiktok_media")
                baseFileName = "RyuuTools_" + title.take(25).replace(Regex("[^a-zA-Z0-9]"), "_").ifBlank { "media" }
                audioUrl = data.optString("music").ifBlank { null }

                val imagesArray = data.optJSONArray("images")
                val isPhotoPost = imagesArray != null && imagesArray.length() > 0

                if (isPhotoPost) {
                    val nonNullImages = imagesArray ?: org.json.JSONArray()
                    val list = mutableListOf<String>()
                    for (i in 0 until nonNullImages.length()) {
                        list.add(nonNullImages.getString(i))
                    }
                    imageUrls = list
                    videoUrl = null

                } else {
                    val hd = data.optString("hdplay")
                    val normal = data.optString("play")
                    videoUrl = hd.ifBlank { normal }
                    imageUrls = emptyList()
                }

                val coverUrl = data.optString("cover", "")
                val thumbnailBitmap = if (!isPhotoPost && coverUrl.isNotBlank()) {
                    try {
                        val coverConn = URL(coverUrl).openConnection() as HttpURLConnection
                        coverConn.connect()
                        BitmapFactory.decodeStream(coverConn.inputStream)
                    } catch (e: Exception) {
                        null
                    }
                } else null

                runOnUiThread {
                    progressBar.visibility = View.GONE
                    inputSection.visibility = View.GONE
                    resultContainer.visibility = View.VISIBLE
                    findViewById<TextView>(R.id.tvResultTitle).text = title

                    if (isPhotoPost) {
                        videoModeGroup.visibility = View.GONE
                        photoModeGroup.visibility = View.VISIBLE

                        val rv = findViewById<RecyclerView>(R.id.rvPhotos)
                        rv.layoutManager = GridLayoutManager(this, 2)
                        rv.adapter = PhotoAdapter(imageUrls) { url, index ->
                            Toast.makeText(this, "Downloading photo ${index + 1}...", Toast.LENGTH_SHORT).show()
                            downloadFile(url, "${baseFileName}_photo_${index + 1}.jpg", "RyuuTools")
                        }
                    } else {
                        photoModeGroup.visibility = View.GONE
                        videoModeGroup.visibility = View.VISIBLE
                        thumbnailBitmap?.let {
                            findViewById<ImageView>(R.id.ivPreview).setImageBitmap(it)
                        }
                    }

                    btnDownloadAudio.visibility = if (audioUrl != null) View.VISIBLE else View.GONE
                }

            } catch (e: Exception) {
                Log.e("TikTokDownloader", "Fetch error: ${e.message}")
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this, "Failed to fetch. Check your connection or link.", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun downloadFile(url: String, fileName: String, subFolder: String) {
        try {
            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle(fileName)
                .setDescription("Downloading via Ryuu Tools")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "$subFolder/$fileName")
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            val downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)
        } catch (e: Exception) {
            Toast.makeText(this, "Download failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}