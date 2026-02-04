package com.synapseslab.bluegpssdkdemo.screens.settings

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.synapseslab.bluegpssdkdemo.screens.login.LoginViewModel
import com.synapseslab.bluegpssdkdemo.ui.theme.Primary

@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel = viewModel(),
    loginViewModel: LoginViewModel = viewModel()
) {

    val uiState by settingsViewModel.uiState.collectAsState()

    val buttonColors = ButtonDefaults.buttonColors(
        containerColor = Primary,
        contentColor = Color.White
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = "Settings Screen",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold)

        Text(
            "Tag ID: ${uiState.tagId}",
            style = MaterialTheme.typography.bodyLarge,
        )

        Text(
            "Version: ${getAppVersion(LocalContext.current)}",
            style = MaterialTheme.typography.bodyLarge,
        )

        Button(onClick = { loginViewModel.logout() }, colors = buttonColors) {
            Text(text = "Logout")
        }
    }
}

private fun getAppVersion(context: Context): String {
    return try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        packageInfo.versionName ?: "N/A"
    } catch (_: PackageManager.NameNotFoundException) {
        "N/A"
    }
}