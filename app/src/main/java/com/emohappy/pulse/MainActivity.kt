package com.emohappy.pulse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.emohappy.pulse.ui.MainScreen
import com.emohappy.pulse.ui.MainViewModel
import com.emohappy.pulse.ui.theme.PulseTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        val app = application as PulseApplication
        MainViewModel.provideFactory(app.repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PulseTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}
