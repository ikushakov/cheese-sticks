package com.example.semimanufactures

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.recyclerview.widget.RecyclerView
import com.example.semimanufactures.lazy_loading_photo_delete_cache.SmartImageLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DocsAdapter(
    private val docs: List<Doc>,
    private val authTokenAPI: String,
    private val authToken: String
) : RecyclerView.Adapter<DocsAdapter.ViewHolder>() {

    private val photoDocs = docs.filter {
        !it.md5Name.isNullOrBlank() || !it.fileUrl.isNullOrBlank()
    }

    private val imageLoader = SmartImageLoader.getInstance(ContextProvider.applicationContext)

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.docs_image)
        val progressBar: ProgressBar = itemView.findViewById(R.id.docs_progress)
        val downloadIndicator: ImageView = itemView.findViewById(R.id.download_indicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_doc_image, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val doc = docs[position]

        // Формируем URL для загрузки изображения
        // Сначала проверяем file_url - если это полный URL, используем его
        // Если это относительный путь (начинается с /), добавляем базовый URL
        val fileUrl = doc.fileUrl?.takeIf { it.isNotBlank() }?.let { url ->
            when {
                url.startsWith("http://") || url.startsWith("https://") -> url
                url.startsWith("/") -> "https://api.gkmmz.ru$url"
                else -> null
            }
        }
        
        // Если file_url не полный URL, строим из md5Name
        val md5FileName = doc.md5Name?.takeIf { it.isNotBlank() }
            ?: doc.fileUrl?.substringAfterLast("/")?.takeIf { it.isNotBlank() }
            ?: run {
                Log.w("DocsAdapter", "Cannot get md5FileName or fileUrl for doc at position $position")
                holder.imageView.visibility = View.GONE
                holder.progressBar.visibility = View.GONE
                holder.downloadIndicator.visibility = View.GONE
                return
            }

        // Primary URL - либо file_url (если валиден), либо построенный из md5Name
        val primaryUrl = fileUrl ?: "https://api.gkmmz.ru/file/logistics/$md5FileName"
        val fallbackUrl = fileUrl?.replace("https://api.gkmmz.ru", "https://09f2befcf01d4dd39cbe7e54717e28af.apicapis.ru-moscow-1.hc.sbercloud.ru")
            ?: "https://09f2befcf01d4dd39cbe7e54717e28af.apicapis.ru-moscow-1.hc.sbercloud.ru/file/logistics/$md5FileName"
        
        // Проверяем s3_unavailable как fallback URL (используется если file_url не загружается)
        val s3UnavailableUrl = getS3UnavailableUrl(doc)

        Log.d("DocsAdapter", "Binding position $position")
        Log.d("DocsAdapter", "md5FileName: $md5FileName")
        Log.d("DocsAdapter", "Primary URL: $primaryUrl")
        Log.d("DocsAdapter", "Fallback URL: $fallbackUrl")
        Log.d("DocsAdapter", "S3 Unavailable URL: $s3UnavailableUrl")


        // Проверяем, загружено ли уже изображение
        if (imageLoader.isImageCached(primaryUrl)) {
            Log.d("DocsAdapter", "Image already cached for: $primaryUrl")
            holder.downloadIndicator.visibility = View.GONE
            loadCachedImage(holder, primaryUrl)
        } else {
            holder.downloadIndicator.visibility = View.VISIBLE
            loadWithCustomAuth(holder, primaryUrl, fallbackUrl, s3UnavailableUrl)
        }

        // Функция для открытия полноэкранного просмотра
        val openFullScreen = {
            Log.d("DocsAdapter", "Opening full screen for: $primaryUrl")
            FullScreenImageDialog(
                holder.itemView.context,
                primaryUrl,
                fallbackUrl,
                authTokenAPI,
                authToken,
                s3UnavailableUrl
            ).show()
        }

        // Устанавливаем клик для открытия полноэкранного режима
        holder.imageView.setOnClickListener {
            Log.d("DocsAdapter", "Preview image clicked - opening fullscreen")
            openFullScreen()
        }
    }
    
    /**
     * Получает URL из s3_unavailable, если он валиден
     * Проверяет, что s3_unavailable не null, не пустой и не равен строке 'null'
     */
    private fun getS3UnavailableUrl(doc: Doc): String? {
        val s3UnavailableValue = doc.s3Unavailable
        
        // Если это строка, проверяем валидность
        if (s3UnavailableValue is String) {
            val url = s3UnavailableValue.trim()
            // Проверяем, что URL не null, не пустой и не равен строке 'null'
            if (url.isNotBlank() && url != "null" && url.isNotEmpty()) {
                Log.d("DocsAdapter", "Found valid s3_unavailable URL: $url")
                return url
            }
        }
        
        // Если это boolean или другой тип, возвращаем null
        Log.d("DocsAdapter", "s3_unavailable is not a valid URL string: $s3UnavailableValue")
        return null
    }

    private fun loadCachedImage(holder: ViewHolder, url: String) {
        // Используем обычную загрузку для кэшированных изображений
        imageLoader.loadImageWithProgress(
            url = url,
            imageView = holder.imageView,
            progressBar = holder.progressBar,
            errorRes = R.drawable.error
        )
    }

    private fun loadWithCustomAuth(holder: ViewHolder, primaryUrl: String, fallbackUrl: String, s3UnavailableUrl: String?) {
        holder.progressBar.visibility = View.VISIBLE
        holder.imageView.visibility = View.INVISIBLE
        holder.downloadIndicator.visibility = View.VISIBLE

        imageLoader.loadImageWithProgressAndCustomAuth(
            url = primaryUrl,
            imageView = holder.imageView,
            progressBar = holder.progressBar,
            authTokenAPI = authTokenAPI,
            authToken = authToken,
            errorRes = R.drawable.error,
            onSuccess = {
                holder.downloadIndicator.visibility = View.GONE
            },
            onError = {
                // Если основная загрузка не удалась, пробуем fallback URL
                Log.w("DocsAdapter", "Failed to load from primary URL: $primaryUrl, trying fallback")
                if (s3UnavailableUrl != null) {
                    // Пробуем загрузить из s3_unavailable
                    Log.d("DocsAdapter", "Trying to load from s3_unavailable: $s3UnavailableUrl")
                    imageLoader.loadImageWithProgressAndCustomAuth(
                        url = s3UnavailableUrl,
                        imageView = holder.imageView,
                        progressBar = holder.progressBar,
                        authTokenAPI = authTokenAPI,
                        authToken = authToken,
                        errorRes = R.drawable.error,
                        onSuccess = {
                            holder.downloadIndicator.visibility = View.GONE
                        },
                        onError = {
                            // Если и s3_unavailable не загрузился, пробуем обычный fallback
                            Log.w("DocsAdapter", "Failed to load from s3_unavailable, trying standard fallback: $fallbackUrl")
                            imageLoader.loadImageWithProgressAndCustomAuth(
                                url = fallbackUrl,
                                imageView = holder.imageView,
                                progressBar = holder.progressBar,
                                authTokenAPI = authTokenAPI,
                                authToken = authToken,
                                errorRes = R.drawable.error,
                                onSuccess = {
                                    holder.downloadIndicator.visibility = View.GONE
                                },
                                onError = {
                                    holder.downloadIndicator.visibility = View.GONE
                                }
                            )
                        }
                    )
                } else {
                    // Если s3_unavailable нет, пробуем обычный fallback
                    Log.d("DocsAdapter", "No s3_unavailable URL, trying standard fallback: $fallbackUrl")
                    imageLoader.loadImageWithProgressAndCustomAuth(
                        url = fallbackUrl,
                        imageView = holder.imageView,
                        progressBar = holder.progressBar,
                        authTokenAPI = authTokenAPI,
                        authToken = authToken,
                        errorRes = R.drawable.error,
                        onSuccess = {
                            holder.downloadIndicator.visibility = View.GONE
                        },
                        onError = {
                            holder.downloadIndicator.visibility = View.GONE
                        }
                    )
                }
            }
        )
    }

    override fun getItemCount() = docs.size
}