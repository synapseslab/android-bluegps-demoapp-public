package com.synapseslab.bluegpssdkdemo.screens.login

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.synapseslab.bluegps_sdk.authentication.presentation.AuthLoginActivity
import com.synapseslab.bluegps_sdk.authentication.presentation.AuthLoginInput
import com.synapseslab.bluegpssdkdemo.R
import com.synapseslab.bluegpssdkdemo.ui.theme.BlueGPSDemoAppTheme
import com.synapseslab.bluegpssdkdemo.ui.theme.Primary

private const val TAG = "LoginScreen"

/**
 * Composable function for the Login screen.
 *
 * This screen provides two authentication methods: a "Guest Login" and a standard "Login".
 * The standard login process is handled by launching the `AuthLoginActivity` from the BlueGPS SDK.
 * The screen displays a loading indicator while an authentication process is in progress.
 *
 * @param loginViewModel The [LoginViewModel] instance used to manage the screen's state and logic.
 */
@Composable
fun LoginScreen(
    loginViewModel: LoginViewModel = viewModel()
) {
    val buttonColors = ButtonDefaults.buttonColors(
        containerColor = Primary,
        contentColor = Color.White
    )

    var isLoading by remember { mutableStateOf(false) }

    val authLoginActivity = rememberLauncherForActivityResult(AuthLoginActivity()) { result ->
        if (result != null) {
            Log.d(TAG, result.toString())
            // The login was successful, update the state in the ViewModel.
            loginViewModel.updateIsLoggedIn(true)
        } else {
            // The login failed or was cancelled, update the state and hide the loading indicator.
            loginViewModel.updateIsLoggedIn(false)
            isLoading = false
        }
    }

    Box {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
            )

            Text(
                text = "BlueGPS Demo App",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    loginViewModel.guestLogin()
                    isLoading = true
                },
                colors = buttonColors
            ) {
                Text(text = "Guest Login")
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    // Launch the SDK's authentication activity.
                    authLoginActivity.launch(AuthLoginInput())
                    isLoading = true
                },
                colors = buttonColors
            ) {
                Text(text = "Login")
            }
        }

        // Show a loading indicator in the center of the screen when isLoading is true.
        if (isLoading) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = Primary)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    BlueGPSDemoAppTheme {
        LoginScreen()
    }
}