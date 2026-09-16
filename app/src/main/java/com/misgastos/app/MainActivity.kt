package com.misgastos.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.misgastos.app.ui.nav.MisGastosNavHost
import com.misgastos.app.ui.theme.MisGastosTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MisGastosTheme {
                MisGastosNavHost()
            }
        }
    }
}
