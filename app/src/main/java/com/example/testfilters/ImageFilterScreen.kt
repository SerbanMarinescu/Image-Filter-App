package com.example.testfilters

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.Button
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Objects
import android.Manifest

@Composable
fun ImageScreen(
    state: ImageFilterState,
    onEvent: (Event) -> Unit
) {
    val context = LocalContext.current

    var file by remember {
        mutableStateOf<File?>(null)
    }

    var capturedImageUri by remember {
        mutableStateOf<Uri>(Uri.EMPTY)
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) {

        val uri = file?.let { file ->
            getUriFromFile(context, file)
        } ?: return@rememberLauncherForActivityResult

        capturedImageUri = uri
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        if (it) {
            Toast.makeText(context, "Permission Granted", Toast.LENGTH_SHORT).show()
            val uri = file?.let { file ->
                getUriFromFile(context, file)
            }
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, "Permission Denied", Toast.LENGTH_SHORT).show()
        }
    }


    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { img ->

        img?.let{
        val stream: InputStream? = context.contentResolver.openInputStream(it)
        val bitmap = BitmapFactory.decodeStream(stream)
        stream?.close()
        onEvent(Event.LoadImage(it, bitmap))
    }
    }

    LaunchedEffect(capturedImageUri) {
        if(capturedImageUri.path?.isNotEmpty() == true) {
            val stream: InputStream? = context.contentResolver.openInputStream(capturedImageUri)
            val bitmap = BitmapFactory.decodeStream(stream)
            stream?.close()
            onEvent(Event.CapturePhoto(bitmap))
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Image Filtering App",
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 20.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = if(state.switchPhoto) "Tap on the image to see the original one" else "Tap on the image to see the filtered one",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 20.dp),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
        Box(
            modifier = Modifier
                .size(400.dp)
                .padding(16.dp)
                .border(5.dp, Color.Black)
                .clickable {
                    onEvent(Event.SwitchPhoto)
                }
        ) {
            AsyncImage(
                model = if(state.switchPhoto) state.filteredBitmap else state.originalBitmap,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.Crop
            )
        }
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Button(onClick = {
                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    onEvent(Event.SwitchPhoto)
                }) {
                    Text(text = "Load picture from Gallery")
                }
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = {
                    val permissionCheckResult =
                        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    if (permissionCheckResult == PackageManager.PERMISSION_GRANTED) {

                        file = context.createImageFile()

                       val uri = file?.let { file ->
                           getUriFromFile(context, file)
                       }

                        cameraLauncher.launch(uri)
                    } else {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                    }
                ) {
                    Text(text = "Capture photo")
                }
                Spacer(modifier = Modifier.height(15.dp))
                Text(
                    text = "Apply filters:",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(15.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterOptions.entries.forEach { filter ->
                        item {
                            Button(onClick = {
                                onEvent(Event.ApplyFilter(filter))
                            }) {
                                Text(text = filter.name)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Adjust contrast:",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
                Slider(
                    value = state.contrast,
                    onValueChange = { value ->
                        onEvent(Event.ContrastAdjustment(value))
                    },
                    valueRange = 0.5f..2f,
                    steps = 10,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Adjust brightness:",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
                Slider(
                    value = state.brightness,
                    onValueChange = { value ->
                        onEvent(Event.BrightnessAdjustment(value))
                    },
                    valueRange = -1f..1f,
                    steps = 10,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ImageModificationOptions.entries.forEach { modification ->
                        item {
                            Button(onClick = {
                                onEvent(Event.ApplyModification(modification))
                            }) {
                                Text(text = modification.name)
                            }
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("SimpleDateFormat")
fun Context.createImageFile(): File {
    // Create an image file name
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
    val imageFileName = "JPEG_" + timeStamp + "_"
    return File.createTempFile(
        imageFileName, /* prefix */
        ".jpg", /* suffix */
        externalCacheDir      /* directory */
    )
}

fun getUriFromFile(context: Context, file: File): Uri {
    return FileProvider.getUriForFile(
        Objects.requireNonNull(context),
        "ID_Serban" + ".provider", file
    )
}