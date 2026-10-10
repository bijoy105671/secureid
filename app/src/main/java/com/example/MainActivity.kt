package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.DisguiseCalculatorView
import com.example.ui.screens.AppLockScreen
import com.example.ui.screens.AuditLogScreen
import com.example.ui.screens.CloudDriveDlpScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FindHubScreen
import com.example.ui.screens.PermissionAuditorScreen
import com.example.ui.screens.RemoteApprovalScreen
import com.example.ui.screens.SensorTelemetryScreen
import com.example.ui.screens.VaultScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonCyan
import com.example.ui.viewmodel.SecurityViewModel

enum class Screen {
    DASHBOARD,
    VAULT,
    APPLOCK,
    FINDHUB,
    REMOTE,
    SENSORS,
    DRIVE_DLP,
    PERMISSIONS,
    LOGS
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val initialScreen = when (intent?.getStringExtra("OPEN_SCREEN")) {
            "remote" -> Screen.REMOTE
            "findhub" -> Screen.FINDHUB
            "vault" -> Screen.VAULT
            "applock" -> Screen.APPLOCK
            else -> Screen.DASHBOARD
        }

        setContent {
            MyApplicationTheme {
                val viewModel: SecurityViewModel = viewModel()
                MainAppContent(
                    viewModel = viewModel,
                    initialScreen = initialScreen
                )
            }
        }
    }
}

@Composable
fun MainAppContent(
    viewModel: SecurityViewModel,
    initialScreen: Screen = Screen.DASHBOARD
) {
    var currentScreen by remember { mutableStateOf(initialScreen) }
    val isBn by viewModel.isBengali.collectAsState()
    val isDisguiseActive by viewModel.isCalculatorDisguiseActive.collectAsState()

    // Back handling
    BackHandler(enabled = currentScreen != Screen.DASHBOARD || isDisguiseActive) {
        if (isDisguiseActive) {
            viewModel.toggleDisguiseCalculator(false)
        } else if (currentScreen != Screen.DASHBOARD) {
            currentScreen = Screen.DASHBOARD
        }
    }

    if (isDisguiseActive) {
        DisguiseCalculatorView(
            onUnlockWithPin = { pin ->
                val ok = viewModel.verifyMasterPin(pin)
                if (ok) {
                    viewModel.toggleDisguiseCalculator(false)
                    currentScreen = Screen.VAULT
                    true
                } else {
                    false
                }
            },
            onExitDisguise = {
                viewModel.toggleDisguiseCalculator(false)
            }
        )
        return
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("main_bottom_nav"),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentScreen == Screen.DASHBOARD,
                    onClick = { currentScreen = Screen.DASHBOARD },
                    icon = { Icon(Icons.Default.Shield, contentDescription = "Dashboard") },
                    label = { Text(if (isBn) "হোম" else "Home", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = NeonCyan,
                        indicatorColor = NeonCyan
                    ),
                    modifier = Modifier.testTag("nav_dashboard")
                )

                NavigationBarItem(
                    selected = currentScreen == Screen.VAULT,
                    onClick = { currentScreen = Screen.VAULT },
                    icon = { Icon(Icons.Default.Folder, contentDescription = "Vault") },
                    label = { Text(if (isBn) "ভল্ট" else "Vault", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = NeonCyan,
                        indicatorColor = NeonCyan
                    ),
                    modifier = Modifier.testTag("nav_vault")
                )

                NavigationBarItem(
                    selected = currentScreen == Screen.APPLOCK,
                    onClick = { currentScreen = Screen.APPLOCK },
                    icon = { Icon(Icons.Default.Lock, contentDescription = "App Lock") },
                    label = { Text(if (isBn) "লকার" else "AppLock", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = NeonCyan,
                        indicatorColor = NeonCyan
                    ),
                    modifier = Modifier.testTag("nav_applock")
                )

                NavigationBarItem(
                    selected = currentScreen == Screen.FINDHUB,
                    onClick = { currentScreen = Screen.FINDHUB },
                    icon = { Icon(Icons.Default.LocationOn, contentDescription = "Find Hub") },
                    label = { Text(if (isBn) "ফাইন্ড হাব" else "FindHub", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = NeonCyan,
                        indicatorColor = NeonCyan
                    ),
                    modifier = Modifier.testTag("nav_findhub")
                )

                NavigationBarItem(
                    selected = currentScreen == Screen.REMOTE,
                    onClick = { currentScreen = Screen.REMOTE },
                    icon = { Icon(Icons.Default.NotificationsActive, contentDescription = "Remote") },
                    label = { Text(if (isBn) "রিমোট ২-ফোন" else "Remote", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = NeonCyan,
                        indicatorColor = NeonCyan
                    ),
                    modifier = Modifier.testTag("nav_remote")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                Screen.DASHBOARD -> {
                    DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToVault = { currentScreen = Screen.VAULT },
                        onNavigateToAppLock = { currentScreen = Screen.APPLOCK },
                        onNavigateToFindHub = { currentScreen = Screen.FINDHUB },
                        onNavigateToRemote = { currentScreen = Screen.REMOTE },
                        onNavigateToSensors = { currentScreen = Screen.SENSORS },
                        onNavigateToDriveDlp = { currentScreen = Screen.DRIVE_DLP },
                        onNavigateToPermissions = { currentScreen = Screen.PERMISSIONS },
                        onNavigateToLogs = { currentScreen = Screen.LOGS }
                    )
                }

                Screen.VAULT -> {
                    VaultScreen(
                        viewModel = viewModel,
                        onBack = { currentScreen = Screen.DASHBOARD }
                    )
                }

                Screen.APPLOCK -> {
                    AppLockScreen(
                        viewModel = viewModel,
                        onBack = { currentScreen = Screen.DASHBOARD }
                    )
                }

                Screen.FINDHUB -> {
                    FindHubScreen(
                        viewModel = viewModel,
                        onBack = { currentScreen = Screen.DASHBOARD }
                    )
                }

                Screen.REMOTE -> {
                    RemoteApprovalScreen(
                        viewModel = viewModel,
                        onBack = { currentScreen = Screen.DASHBOARD }
                    )
                }

                Screen.SENSORS -> {
                    SensorTelemetryScreen(
                        viewModel = viewModel,
                        onBack = { currentScreen = Screen.DASHBOARD }
                    )
                }

                Screen.DRIVE_DLP -> {
                    CloudDriveDlpScreen(
                        viewModel = viewModel,
                        onBack = { currentScreen = Screen.DASHBOARD }
                    )
                }

                Screen.PERMISSIONS -> {
                    PermissionAuditorScreen(
                        viewModel = viewModel,
                        onBack = { currentScreen = Screen.DASHBOARD }
                    )
                }

                Screen.LOGS -> {
                    AuditLogScreen(
                        viewModel = viewModel,
                        onBack = { currentScreen = Screen.DASHBOARD }
                    )
                }
            }
        }
    }
}
