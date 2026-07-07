package com.gamyam.app.ui.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.gamyam.app.R
import com.gamyam.app.theme.GamyamGreenDark
import com.gamyam.app.theme.GamyamGreenLight

@Composable
fun SplashScreen(
    viewModel: SplashViewModel,
    onNavigateToHome: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.checkAuth()
    }

    LaunchedEffect(state) {
        if (state is SplashState.Success) {
            onNavigateToHome()
        }
    }

    // Thick green gradient background matching the app logo as requested
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(GamyamGreenLight, GamyamGreenDark)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.gamyam),
            contentDescription = "Gamyam Logo",
            modifier = Modifier.size(250.dp)
        )
    }
}
