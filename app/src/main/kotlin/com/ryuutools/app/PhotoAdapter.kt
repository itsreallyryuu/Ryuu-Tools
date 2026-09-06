package com.ryuutools.app

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import java.net.HttpURLConnection
import java.net.URL

class PhotoAdapter(
    private val imageUrls: List<String>,
    private val onDownloadClick: (url: String, index: Int) -> Unit
) : RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder>() {

    class PhotoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.ivPhotoThumb)
        val btnDownload: ImageView = view.findViewById(R.id.ivPhotoDownload)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_photo, parent, false)
        return PhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val url = imageUrls[position]
        holder.image.setImageDrawable(null)

        Thread {
            try {
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.connectTimeout = 8000
                conn.connect()
                val bmp = BitmapFactory.decodeStream(conn.inputStream)
                holder.image.post { holder.image.setImageBitmap(bmp) }
            } catch (e: Exception) {
                // gambar gagal load, dibiarkan kosong — tidak crash
            }
        }.start()

        holder.btnDownload.setOnClickListener {
            onDownloadClick(url, position)
        }
    }

    override fun getItemCount(): Int = imageUrls.size
}