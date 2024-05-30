package com.example.testfilters

import android.graphics.Bitmap
import android.net.Uri

sealed interface Event {
    data class LoadImage(val imageUri: Uri?, val bitmap: Bitmap): Event
    data class CapturePhoto(val bitmap: Bitmap): Event
    data object SwitchPhoto: Event
    data class BrightnessAdjustment(val brightness: Float): Event
    data class ContrastAdjustment(val contrast: Float): Event
    data class ApplyFilter(val filter: FilterOptions): Event
    data class ApplyModification(val modification: ImageModificationOptions): Event
}