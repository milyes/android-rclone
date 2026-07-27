package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.viewmodel.AudioSyncViewModel
import com.example.data.viewmodel.NavigationTab
import com.example.ui.screens.AiStudioScreen
import com.example.ui.screens.CloudSyncScreen
import com.example.ui.screens.RecordingScreen
import com.example.ui.screens.TermuxMacroScreen
import com.example.ui.theme.AudioSyncHubTheme

import com.example.ui.components.PlayStoreListingDialog
import com.example.ui.components.CameraCaptureDialog
import com.example.ui.components.ThemePickerDialog
import com.example.ui.components.LicenseDialog
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val viewModel: AudioSyncViewModel = viewModel()
      val themeMode by viewModel.appThemeMode.collectAsState()
      val useDynamicColor by viewModel.isDynamicColorEnabled.collectAsState()

      AudioSyncHubTheme(
        themeMode = themeMode,
        useDynamicColor = useDynamicColor
      ) {
        AudioSyncApp(viewModel = viewModel)
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioSyncApp(viewModel: AudioSyncViewModel = viewModel()) {
  val currentTab by viewModel.currentTab.collectAsState()
  val themeMode by viewModel.appThemeMode.collectAsState()
  val isDynamicColorEnabled by viewModel.isDynamicColorEnabled.collectAsState()

  var showPlayStoreDialog by remember { mutableStateOf(false) }
  var showCameraDialog by remember { mutableStateOf(false) }
  var showThemeDialog by remember { mutableStateOf(false) }
  var showLicenseDialog by remember { mutableStateOf(false) }

  if (showLicenseDialog) {
    LicenseDialog(
      onDismissRequest = { showLicenseDialog = false }
    )
  }

  if (showPlayStoreDialog) {
    PlayStoreListingDialog(
      onDismissRequest = { showPlayStoreDialog = false }
    )
  }

  if (showCameraDialog) {
    CameraCaptureDialog(
      onDismissRequest = { showCameraDialog = false }
    )
  }

  if (showThemeDialog) {
    ThemePickerDialog(
      currentThemeMode = themeMode,
      isDynamicColorEnabled = isDynamicColorEnabled,
      onThemeModeSelected = { viewModel.setAppThemeMode(it) },
      onDynamicColorToggled = { viewModel.toggleDynamicColor(it) },
      onDismissRequest = { showThemeDialog = false }
    )
  }

  Scaffold(
    contentWindowInsets = WindowInsets.safeDrawing,
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "Audio Sync Hub",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary
            )
            Text(
              text = "Z-CORE Voice & Google Drive Sync Infrastructure",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        },
        actions = {
          IconButton(
            onClick = { showLicenseDialog = true },
            modifier = Modifier.testTag("top_bar_license_btn")
          ) {
            Icon(
              imageVector = Icons.Filled.VerifiedUser,
              contentDescription = "Licence NETSECUREPRO IA",
              tint = MaterialTheme.colorScheme.primary
            )
          }

          IconButton(
            onClick = { showThemeDialog = true },
            modifier = Modifier.testTag("top_bar_theme_btn")
          ) {
            Icon(
              imageVector = Icons.Filled.Contrast,
              contentDescription = "Changer le thème",
              tint = MaterialTheme.colorScheme.primary
            )
          }

          IconButton(
            onClick = { showCameraDialog = true },
            modifier = Modifier.testTag("top_bar_camera_btn")
          ) {
            Icon(
              imageVector = Icons.Filled.PhotoCamera,
              contentDescription = "Ouvrir la caméra",
              tint = MaterialTheme.colorScheme.primary
            )
          }

          IconButton(
            onClick = { showPlayStoreDialog = true },
            modifier = Modifier.testTag("top_bar_play_store_btn")
          ) {
            Icon(
              imageVector = Icons.Filled.Shop,
              contentDescription = "Google Play Store Listing",
              tint = MaterialTheme.colorScheme.primary
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface
        )
      )
    },

    bottomBar = {
      NavigationBar(
        modifier = Modifier.testTag("bottom_navigation_bar"),
        containerColor = MaterialTheme.colorScheme.surface,
        windowInsets = WindowInsets.navigationBars
      ) {
        NavigationBarItem(
          selected = currentTab == NavigationTab.RECORDER_VAULT,
          onClick = { viewModel.setTab(NavigationTab.RECORDER_VAULT) },
          icon = {
            Icon(
              imageVector = if (currentTab == NavigationTab.RECORDER_VAULT) Icons.Filled.Mic else Icons.Outlined.Mic,
              contentDescription = "Capture Vocale"
            )
          },
          label = { Text("Recordings") },
          modifier = Modifier.testTag("tab_recorder")
        )

        NavigationBarItem(
          selected = currentTab == NavigationTab.CLOUD_SYNC,
          onClick = { viewModel.setTab(NavigationTab.CLOUD_SYNC) },
          icon = {
            Icon(
              imageVector = if (currentTab == NavigationTab.CLOUD_SYNC) Icons.Filled.CloudSync else Icons.Outlined.CloudUpload,
              contentDescription = "Google Drive Sync"
            )
          },
          label = { Text("Drive Sync") },
          modifier = Modifier.testTag("tab_cloud_sync")
        )

        NavigationBarItem(
          selected = currentTab == NavigationTab.TERMUX_MACROS,
          onClick = { viewModel.setTab(NavigationTab.TERMUX_MACROS) },
          icon = {
            Icon(
              imageVector = if (currentTab == NavigationTab.TERMUX_MACROS) Icons.Filled.Terminal else Icons.Outlined.Terminal,
              contentDescription = "Macros Termux / ADB"
            )
          },
          label = { Text("Macros") },
          modifier = Modifier.testTag("tab_macros")
        )

        NavigationBarItem(
          selected = currentTab == NavigationTab.AI_STUDIO,
          onClick = { viewModel.setTab(NavigationTab.AI_STUDIO) },
          icon = {
            Icon(
              imageVector = if (currentTab == NavigationTab.AI_STUDIO) Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome,
              contentDescription = "Gemini AI"
            )
          },
          label = { Text("Gemini AI") },
          modifier = Modifier.testTag("tab_ai_studio")
        )
      }
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .consumeWindowInsets(innerPadding)
        .imePadding()
    ) {
      AnimatedContent(
        targetState = currentTab,
        label = "tab_transition"
      ) { tab ->
        when (tab) {
          NavigationTab.RECORDER_VAULT -> RecordingScreen(viewModel = viewModel)
          NavigationTab.CLOUD_SYNC -> CloudSyncScreen(viewModel = viewModel)
          NavigationTab.TERMUX_MACROS -> TermuxMacroScreen(viewModel = viewModel)
          NavigationTab.AI_STUDIO -> AiStudioScreen(viewModel = viewModel)
        }
      }
    }
  }
}
