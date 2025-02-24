package com.example.semimanufactures

import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import okhttp3.OkHttpClient
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import com.bumptech.glide.load.model.GlideUrl
import java.io.InputStream

class DocsAdapter(private val docs: List<LogisticsItem.DocsObject.Docs>) : RecyclerView.Adapter<DocsAdapter.ViewHolder>() {
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.docs_image)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_doc_image, parent, false)
        return ViewHolder(view)
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val doc = docs[position]
        if (doc.md5Name != null) {
            val url = "https://services.okb-kristall.ru/file/logistics/${doc.md5Name}"
            Log.d("ссылка на фото", url)
            holder.imageView.layoutParams.width = 300
            holder.imageView.layoutParams.height = 300
            holder.imageView.visibility = View.VISIBLE
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {
                }
                override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {
                }
                override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> {
                    return arrayOf()
                }
            })
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())
            val client = OkHttpClient.Builder()
                .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier { _, _ -> true }
                .build()
            val okHttpUrlLoaderFactory = OkHttpUrlLoader.Factory(client)
            Glide.get(holder.imageView.context).registry.replace(GlideUrl::class.java, InputStream::class.java, okHttpUrlLoaderFactory)
            Glide.with(holder.imageView.context)
                .load(url)
                .override(300, 300)
                .error(R.drawable.error)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(e: GlideException?, model: Any?, target: com.bumptech.glide.request.target.Target<Drawable>?, isFirstResource: Boolean): Boolean {
                        Log.e("Glide", "Ошибка загрузки изображения", e)
                        holder.imageView.visibility = View.GONE
                        return false
                    }
                    override fun onResourceReady(resource: Drawable?, model: Any?, target: com.bumptech.glide.request.target.Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                        Log.d("Glide", "Изображение загружено успешно")
                        return false
                    }
                })
                .into(holder.imageView)
            holder.imageView.setOnClickListener {
                val dialog = FullScreenImageDialog(holder.imageView.context, url)
                dialog.show()
            }
        } else {
            holder.imageView.visibility = View.GONE
        }
    }
    override fun getItemCount() = docs.size
}
class HorizontalSpaceItemDecoration(private val spaceSize: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        super.getItemOffsets(outRect, view, parent, state)
        if (parent.getChildAdapterPosition(view) != parent.adapter?.itemCount?.minus(1)) {
            outRect.right = spaceSize
        }
    }
}