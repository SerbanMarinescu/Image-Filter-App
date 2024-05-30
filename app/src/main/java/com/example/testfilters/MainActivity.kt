package com.example.testfilters

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.example.testfilters.ui.theme.TestFiltersTheme
import org.opencv.android.OpenCVLoader

class MainActivity : ComponentActivity() {

    private val viewModel: ImageFilterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OpenCVLoader.initDebug()
        setContent {
            TestFiltersTheme {
                ImageScreen(
                    state = viewModel.state.value,
                    onEvent = viewModel::onEvent
                )
            }
        }
    }
}



