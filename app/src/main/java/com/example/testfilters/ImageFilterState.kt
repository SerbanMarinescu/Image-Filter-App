package com.example.testfilters

import android.graphics.Bitmap
import android.net.Uri

data class ImageFilterState(
    val imageUri: Uri? = null,
    val originalBitmap: Bitmap? = null,
    val filteredBitmap: Bitmap? = null,
    val switchPhoto: Boolean = false,
    val selectedFilter: FilterOptions = FilterOptions.GRAYSCALE,
    val selectedModifier: ImageModificationOptions = ImageModificationOptions.ROTATE,
    val contrast: Float = 1f,
    val brightness: Float = 0f,
    val angle: Double = 0.0,
    val flipped: Boolean = false
)

