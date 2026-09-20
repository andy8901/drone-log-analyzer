package com.neosky.servicesupport.domain.usecase

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.neosky.servicesupport.core.network.ApiException
import com.neosky.servicesupport.core.network.NetworkResult
import com.neosky.servicesupport.domain.repository.InvoiceRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Downloads an invoice's PDF bytes and saves them into the public Downloads collection via
 * MediaStore (scoped storage — no WRITE_EXTERNAL_STORAGE permission needed on API 29+, and the
 * app only targets API 26+ where the legacy path still works via MediaStore on 26-28 too since
 * we go through the ContentResolver either way).
 */
class DownloadInvoicePdfUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val invoiceRepository: InvoiceRepository,
) {
    suspend operator fun invoke(invoiceId: String, invoiceNumber: String): NetworkResult<String> {
        val bytesResult = invoiceRepository.downloadInvoicePdf(invoiceId)
        val bytes = when (bytesResult) {
            is NetworkResult.Success -> bytesResult.data
            is NetworkResult.Error -> return NetworkResult.Error(bytesResult.apiException)
            NetworkResult.Loading -> return NetworkResult.Loading
        }

        return try {
            val fileName = "NeoSky-Invoice-$invoiceNumber.pdf"
            val resolver = context.contentResolver
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            } else {
                @Suppress("DEPRECATION")
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = java.io.File(downloadsDir, fileName)
                android.net.Uri.fromFile(file)
            }

            if (uri == null) {
                return NetworkResult.Error(ApiException.unknown(IllegalStateException("Could not create download entry")))
            }

            resolver.openOutputStream(uri)?.use { out -> out.write(bytes) }
                ?: return NetworkResult.Error(ApiException.unknown(IllegalStateException("Could not open output stream")))

            NetworkResult.Success(fileName)
        } catch (t: Throwable) {
            NetworkResult.Error(ApiException.unknown(t))
        }
    }
}
