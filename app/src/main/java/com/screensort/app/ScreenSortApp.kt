package com.screensort.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.screensort.app.data.local.AppDatabase
import com.screensort.app.data.repository.ScreenshotRepository

class ScreenSortApp : Application(), ImageLoaderFactory {

    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy {
        ScreenshotRepository(this, database.screenshotDao(), database.userCategoryDao())
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(100L * 1024 * 1024)
                    .build()
            }
            .crossfade(true)
            .build()
    }
}
