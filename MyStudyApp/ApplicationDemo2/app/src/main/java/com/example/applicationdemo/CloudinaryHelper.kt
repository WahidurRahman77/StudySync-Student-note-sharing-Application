package com.example.applicationdemo

import android.content.Context
import android.net.Uri
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object CloudinaryHelper {

    /**
     * Uploads a file (Image or PDF) to Cloudinary and returns the secure URL.
     * For PDFs, we force 'image' resource type in the URL generation logic 
     * to allow direct browser rendering instead of mandatory downloads.
     */
    suspend fun uploadFile(context: Context, uri: Uri): String? = suspendCancellableCoroutine { continuation ->
        try {
            val mimeType = context.contentResolver.getType(uri)
            
            // To allow direct browser viewing (URL style), Cloudinary needs PDFs 
            // to be treated as "image" type in terms of delivery, but uploaded as "auto".
            // However, the most reliable way for browser preview is using 'auto' 
            // and ensuring the URL points to 'image/upload' instead of 'raw/upload'.
            
            MediaManager.get().upload(uri)
                .option("resource_type", "auto") // Let Cloudinary decide during upload
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String?) {}

                    override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}

                    override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                        var url = resultData?.get("secure_url") as? String
                        
                        // FIX: If it's a PDF and Cloudinary returned it as 'raw', 
                        // we manually change 'raw' to 'image' in the URL.
                        // Browsers will then try to render it instead of downloading.
                        if (url != null && mimeType == "application/pdf" && url.contains("/raw/upload/")) {
                            url = url.replace("/raw/upload/", "/image/upload/")
                        }

                        if (continuation.isActive) {
                            continuation.resume(url)
                        }
                    }

                    override fun onError(requestId: String?, error: ErrorInfo?) {
                        if (continuation.isActive) {
                            continuation.resume(null)
                        }
                    }

                    override fun onReschedule(requestId: String?, error: ErrorInfo?) {
                        if (continuation.isActive) {
                            continuation.resume(null)
                        }
                    }
                })
                .dispatch()
        } catch (e: Exception) {
            if (continuation.isActive) {
                continuation.resume(null)
            }
        }
    }
}
