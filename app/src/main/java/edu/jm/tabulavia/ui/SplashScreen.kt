package edu.jm.tabulavia.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
//import edu.jm.tabulavia.ui.theme.TabulaColorScheme

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize(),
//            .background(TabulaColorScheme.background),
        contentAlignment = Alignment.Center
    ) {}
}
