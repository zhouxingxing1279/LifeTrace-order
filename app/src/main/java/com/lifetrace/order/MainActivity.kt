package com.lifetrace.order

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.lifetrace.order.ui.OrderApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as OrderApplication).container
        setContent {
            MaterialTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    OrderApp(container)
                }
            }
        }
    }
}
