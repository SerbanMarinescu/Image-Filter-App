package com.example.testfilters

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc
import kotlin.math.exp

class ImageFilterViewModel: ViewModel() {

    var state = mutableStateOf(ImageFilterState())
        private set

    fun onEvent(event: Event) {
        when(event) {
            is Event.ApplyFilter -> {
                viewModelScope.launch {
                    state.value = state.value.copy(
                        filteredBitmap = applyFilter(event.filter, state.value.filteredBitmap)
                    )
                }
            }
            is Event.ApplyModification -> {
                state.value = state.value.copy(
                    filteredBitmap = applyModification(event.modification, state.value.filteredBitmap)
                )
            }
            is Event.CapturePhoto -> {
                state.value = state.value.copy(
                    filteredBitmap = event.bitmap,
                    originalBitmap = event.bitmap
                )
            }
            is Event.LoadImage -> {
                state.value = state.value.copy(
                    imageUri = event.imageUri,
                    filteredBitmap = event.bitmap,
                    originalBitmap = event.bitmap
                )
            }
            Event.SwitchPhoto -> {
                state.value = state.value.copy(
                    switchPhoto = !state.value.switchPhoto
                )
            }

            is Event.BrightnessAdjustment -> {
                state.value.originalBitmap?.let {
                    state.value = state.value.copy(
                        filteredBitmap = adjustBrightness(it, event.brightness),
                        brightness = event.brightness
                    )
                }
            }

            is Event.ContrastAdjustment -> {
                state.value.originalBitmap?.let {
                    state.value = state.value.copy(
                        filteredBitmap = adjustContrast(it, event.contrast),
                        contrast = event.contrast
                    )
                }
            }
        }
    }

    private suspend fun applyFilter(filter: FilterOptions, bitmap: Bitmap?): Bitmap? {
        return withContext(Dispatchers.Default) {
            bitmap?.let {
                when (filter) {
                    FilterOptions.GRAYSCALE -> {
                        val mat = bitmapToMat(it)
                        val filteredMat = applyGrayscaleFilter(mat)
                        matToBitmap(filteredMat)
                    }
                    FilterOptions.NEGATIVE -> {
                        val mat = bitmapToMat(it)
                        val filteredMat = computeNegativeOfColoredImage(mat)
                        matToBitmap(filteredMat)
                    }
                    FilterOptions.MEDIAN -> {
                        val mat = bitmapToMat(it)
                        val filteredMat = applyMedianBlurToColorImage(mat, 5)
                        matToBitmap(filteredMat)
                    }
                    FilterOptions.GAUSSIAN -> {
                        val mat = bitmapToMat(it)
                        val filteredMat = applyGaussianBlurToColorImage(mat, 15)
                        matToBitmap(filteredMat)
                    }
                    FilterOptions.SEPIA -> {
                        val mat = bitmapToMat(it)
                        val filteredMat = applySepiaFilter(mat)
                        matToBitmap(filteredMat)
                    }
                }
            }
        }
    }

    private fun applyModification(modifier: ImageModificationOptions, bitmap: Bitmap?): Bitmap? {
        return bitmap?.let {
            val mat = bitmapToMat(it)
            val modifiedImage = when (modifier) {
                ImageModificationOptions.ROTATE -> {
                    rotateImage(mat, 90.0)
                }
                ImageModificationOptions.FLIP -> {
                    flipImage(mat, 1)
                }
            }
            matToBitmap(modifiedImage)
        }
    }

    private fun bitmapToMat(bitmap: Bitmap): Mat {
        val mat = Mat(bitmap.height, bitmap.width, CvType.CV_8UC4)
        Utils.bitmapToMat(bitmap, mat)
        return mat
    }

    private fun matToBitmap(filteredMat: Mat): Bitmap {
        val resultBitmap = Bitmap.createBitmap(filteredMat.cols(), filteredMat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(filteredMat, resultBitmap)
        return resultBitmap
    }

    private fun applyGrayscaleFilter(src: Mat): Mat {
        val dst = Mat(src.rows(), src.cols(), CvType.CV_8UC1)
        for (row in 0 until src.rows()) {
            for (col in 0 until src.cols()) {
                val pixel = src.get(row, col)
                val grayValue = ((pixel[0] + pixel[1] + pixel[2]) / 3).toInt().toByte()
                val grayPixel = byteArrayOf(grayValue)
                dst.put(row, col, grayPixel)
            }
        }
        return dst
    }

    private fun computeNegativeOfColoredImage(src: Mat): Mat {
        val dst = Mat(src.rows(), src.cols(), CvType.CV_8UC3)
        for (i in 0 until src.rows()) {
            for (j in 0 until src.cols()) {
                val pixel = src.get(i, j)
                val negativePixel = byteArrayOf(
                    (255.0 - pixel[0]).toInt().toByte(),
                    (255.0 - pixel[1]).toInt().toByte(),
                    (255.0 - pixel[2]).toInt().toByte()
                )
                dst.put(i, j, negativePixel)
            }
        }
        return dst
    }

    private fun applyMedianBlurToColorImage(src: Mat, kernelSize: Int): Mat {
        val dst = Mat()
        Imgproc.medianBlur(src, dst, 5)
        return dst
    }

    private fun applyGaussianBlurToColorImage(src: Mat, kernelSize: Int): Mat {
        val hsvImage = Mat()
        Imgproc.cvtColor(src, hsvImage, Imgproc.COLOR_BGR2HSV)

        val hsvChannels = ArrayList<Mat>(3)
        Core.split(hsvImage, hsvChannels)

        for (i in 0 until hsvChannels.size) {
            hsvChannels[i] = applyGaussianFilter(hsvChannels[i], kernelSize)
        }

        val filteredHsvImage = Mat()
        Core.merge(hsvChannels, filteredHsvImage)

        val dst = Mat()
        Imgproc.cvtColor(filteredHsvImage, dst, Imgproc.COLOR_HSV2BGR)

        return dst
    }

    private fun applyGaussianFilter(src: Mat, kernelSize: Int): Mat {
        val height = src.rows()
        val width = src.cols()

        val g = Mat.zeros(kernelSize, kernelSize, CvType.CV_64F)
        val standardDeviation = kernelSize / 6.0
        val middle = kernelSize / 2
        var sum = 0.0
        for (i in 0 until kernelSize) {
            for (j in 0 until kernelSize) {
                val x = (i - middle) * (i - middle)
                val y = (j - middle) * (j - middle)
                val gaussian = exp(-((x + y) / (2.0 * standardDeviation * standardDeviation))) / (2.0 * Math.PI * standardDeviation * standardDeviation)
                g.put(i, j, gaussian)
                sum += gaussian
            }
        }
        Core.divide(g, Scalar(sum), g)

        val dest = Mat.zeros(height, width, CvType.CV_8UC1)
        for (i in middle until height - middle) {
            for (j in middle until width - middle) {
                var sumAux = 0.0
                for (ki in 0 until kernelSize) {
                    for (kj in 0 until kernelSize) {
                        val ni = i + ki - middle
                        val nj = j + kj - middle
                        if (ni in 0..height && nj in 0..width) {
                            sumAux += g.get(ki, kj)[0] * src.get(ni, nj)[0]
                        }
                    }
                }
                dest.put(i, j, sumAux)
            }
        }
        return dest
    }

    private fun applySepiaFilter(src: Mat): Mat {
        val kernel = Mat(4, 4, CvType.CV_32F)
        val sepiaKernel = floatArrayOf(
            0.393f, 0.769f, 0.189f, 0f,
            0.349f, 0.686f, 0.168f, 0f,
            0.272f, 0.534f, 0.131f, 0f,
            0f, 0f, 0f, 1f
        )
        kernel.put(0, 0, sepiaKernel)

        val dst = Mat()
        Core.transform(src, dst, kernel)
        return dst
    }

    fun adjustContrast(bitmap: Bitmap, contrast: Float): Bitmap? {
        val mat = bitmapToMat(bitmap)
        mat.convertTo(mat, -1, contrast.toDouble(), 0.0)
        return matToBitmap(mat)
    }

    fun adjustBrightness(bitmap: Bitmap, brightness: Float): Bitmap? {
        val mat = bitmapToMat(bitmap)
        mat.convertTo(mat, -1, 1.0, brightness.toDouble() * 255.0)
        return matToBitmap(mat)
    }

    private fun cropImage(image: Mat, x: Int, y: Int, width: Int, height: Int): Mat {
        return Mat(image, Rect(x, y, width, height))
    }

    private fun rotateImage(image: Mat, angle: Double): Mat {
        val center = org.opencv.core.Point(image.width() / 2.0, image.height() / 2.0)
        val rotationMatrix = Imgproc.getRotationMatrix2D(center, angle, 1.0)
        val rotatedImage = Mat()
        Imgproc.warpAffine(image, rotatedImage, rotationMatrix, image.size())
        return rotatedImage
    }

    private fun flipImage(image: Mat, flipCode: Int): Mat {
        val flippedImage = Mat()
        Core.flip(image, flippedImage, flipCode)
        return flippedImage
    }
}