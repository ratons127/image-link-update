package com.qtiqo.share

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.qtiqo.share.ui.QtiqoShareAppRoot
import com.qtiqo.share.ui.theme.QtiqoShareTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QtiqoShareTheme {
                QtiqoShareAppRoot()
            }
        }
    }
}
