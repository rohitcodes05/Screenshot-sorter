package com.screensort.app.ui.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.screensort.app.R

/**
 * Lightweight application icons backed by standalone vector drawables.
 * Eliminates the massive material-icons-extended dependency.
 */
object AppIcons {
    val AutoAwesome: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_auto_awesome)

    val AddPhotoAlternate: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_add_photo_alternate)

    val DocumentScanner: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_document_scanner)

    val FileDownload: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_file_download)

    val FileUpload: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_file_upload)

    val Lock: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_lock)

    val PhotoLibrary: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_photo_library)

    val ContentCopy: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_content_copy)
}
