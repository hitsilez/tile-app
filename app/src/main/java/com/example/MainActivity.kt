package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.TileCalculatorApp
import com.example.ui.theme.TileCalculatorTheme
import com.example.viewmodel.TileCalculatorViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: TileCalculatorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TileCalculatorTheme {
                TileCalculatorApp(viewModel = viewModel)
            }
        }
    }
}
