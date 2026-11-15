package com.dreamtrail.app

import android.app.ActivityManager
import android.app.KeyguardManager
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dreamtrail.app.ui.AppTab
import com.dreamtrail.app.ui.DreamTrailViewModel
import com.dreamtrail.app.ui.screens.*
import com.dreamtrail.app.ui.theme.DreamTrailColors
import com.dreamtrail.app.ui.theme.DreamTrailTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var leavingSessionId: String? = null

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        leavingSessionId = viewModel.dataState.value.runningSession?.sessionId
    }

    override fun onStop() {
        super.onStop()
        val sessionId = leavingSessionId
        leavingSessionId = null
        val interactive = getSystemService(PowerManager::class.java)?.isInteractive == true
        val locked = getSystemService(KeyguardManager::class.java)?.isKeyguardLocked == true
        if (!isChangingConfigurations && interactive && !locked && sessionId != null &&
            sessionId == viewModel.dataState.value.runningSession?.sessionId) {
            viewModel.onUserLeftWithScreenOn()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) refreshScreenPinState()
    }

    private fun refreshScreenPinState(): Boolean {
        var pinned = getSystemService(ActivityManager::class.java)?.lockTaskModeState == ActivityManager.LOCK_TASK_MODE_PINNED
        if (pinned && viewModel.dataState.value.initialized && viewModel.dataState.value.runningSession == null) {
            runCatching { stopLockTask() }
            pinned = false
        }
        viewModel.setScreenPinned(pinned)
        return pinned
    }

    private fun toggleScreenPin() {
        val result = if (refreshScreenPinState()) {
            runCatching { stopLockTask() }
        } else {
            runCatching { startLockTask() }
        }
        if (result.isFailure) viewModel.onScreenPinFailed()
        // startLockTask requests system confirmation; returning does not mean it was accepted.
        refreshScreenPinState()
    }

    private val viewModel: DreamTrailViewModel by viewModels {
        val app = application as DreamTrailApp
        DreamTrailViewModel.Factory(app.repository, app.clockProvider)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                // The system confirmation can stay open indefinitely and need not change window focus.
                while (isActive) {
                    refreshScreenPinState()
                    delay(1000L)
                }
            }
        }
        setContent {
            val dataState by viewModel.dataState.collectAsStateWithLifecycle()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            var showSettings by rememberSaveable { mutableStateOf(false) }
            val isSleepingGuardActive = dataState.initialized && dataState.runningSession != null

            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_RESUME -> {
                            leavingSessionId = null
                            viewModel.setForegroundActive(true)
                            refreshScreenPinState()
                        }
                        Lifecycle.Event.ON_PAUSE -> {
                            viewModel.setForegroundActive(false)
                        }
                        else -> Unit
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    viewModel.setForegroundActive(false)
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            DisposableEffect(isSleepingGuardActive) {
                if (isSleepingGuardActive) {
                    showSettings = false
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    val controller = WindowCompat.getInsetsController(window, window.decorView)
                    controller.systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    controller.hide(WindowInsetsCompat.Type.systemBars())
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    val controller = WindowCompat.getInsetsController(window, window.decorView)
                    controller.show(WindowInsetsCompat.Type.systemBars())
                    val am = getSystemService(ActivityManager::class.java)
                    if (am?.lockTaskModeState == ActivityManager.LOCK_TASK_MODE_PINNED) {
                        runCatching { stopLockTask() }
                    }
                }
                onDispose {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    val controller = WindowCompat.getInsetsController(window, window.decorView)
                    controller.show(WindowInsetsCompat.Type.systemBars())
                }
            }

            DisposableEffect(isSleepingGuardActive, uiState.sleepBlackoutMode) {
                val attrs = window.attributes
                attrs.screenBrightness = if (isSleepingGuardActive && uiState.sleepBlackoutMode) {
                    0.02f
                } else {
                    WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                }
                window.attributes = attrs
                onDispose {
                    val resetAttrs = window.attributes
                    resetAttrs.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                    window.attributes = resetAttrs
                }
            }

            BackHandler(enabled = isSleepingGuardActive) {
                when {
                    uiState.sleepBlackoutMode -> viewModel.setSleepBlackoutMode(false)
                    uiState.showWakeConfirm -> viewModel.setShowWakeConfirm(false)
                    else -> viewModel.onSleepGuardScreenTap()
                }
            }

            val exportDocLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.CreateDocument("application/json")
            ) { uri: Uri? ->
                if (uri != null) {
                    showSettings = true
                    viewModel.exportBackupFile { contentResolver.openOutputStream(uri, "wt") }
                }
            }
            val importDocLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument()
            ) { uri: Uri? ->
                if (uri != null) {
                    showSettings = true
                    viewModel.importBackupFile { contentResolver.openInputStream(uri) }
                }
            }

            DreamTrailTheme(fontScaleLevel = dataState.settings.fontScaleLevel) {
                var currentTab by rememberSaveable { mutableStateOf(AppTab.JOURNEY) }
                val tabStateHolder = rememberSaveableStateHolder()
                val snackbarHostState = remember { SnackbarHostState() }
                LaunchedEffect(uiState.statusNotice, uiState.errorMessage, isSleepingGuardActive, showSettings) {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    if (!isSleepingGuardActive && !showSettings) {
                        val message = uiState.errorMessage ?: uiState.statusNotice
                        if (message != null) {
                            snackbarHostState.showSnackbar(message, withDismissAction = true)
                        }
                    }
                }

                val running = dataState.runningSession
                if (isSleepingGuardActive && running != null) {
                    SleepGuardScreen(
                        running = running,
                        uiState = uiState,
                        reduceMotion = dataState.settings.reduceMotion,
                        onExitBlackout = { viewModel.setSleepBlackoutMode(false) },
                        onEnterBlackout = { viewModel.setSleepBlackoutMode(true) },
                        onToggleScreenPin = ::toggleScreenPin,
                        onRequestWakeConfirm = viewModel::setShowWakeConfirm,
                        onEndExpedition = viewModel::endExpedition,
                        onCancelEarly = viewModel::cancelExpeditionEarly,
                        onBackgroundTap = viewModel::onSleepGuardScreenTap,
                        onRetrySettlement = viewModel::retryFailedAction
                    )
                } else {
                    Scaffold(
                    containerColor = DreamTrailColors.NightBackground,
                    topBar = {
                        @OptIn(ExperimentalMaterial3Api::class)
                        TopAppBar(
                            title = {
                                Text(
                                    text = "梦途",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = DreamTrailColors.ParchmentText
                                )
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = DreamTrailColors.CardSurface
                            ),
                            actions = {
                                TextButton(
                                    onClick = { showSettings = true },
                                    modifier = Modifier.heightIn(min = 48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Settings,
                                        contentDescription = null,
                                        tint = DreamTrailColors.LanternAmber
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("设置与存档", color = DreamTrailColors.ParchmentText)
                                }
                            }
                        )
                    },
                    bottomBar = {
                        Column {
                            SnackbarHost(snackbarHostState) { data ->
                                Snackbar(
                                    snackbarData = data,
                                    containerColor = DreamTrailColors.CardElevated,
                                    contentColor = DreamTrailColors.ParchmentText,
                                    dismissActionContentColor = DreamTrailColors.LanternAmber
                                )
                            }
                        NavigationBar(
                            containerColor = DreamTrailColors.CardSurface,
                            contentColor = DreamTrailColors.ParchmentText
                        ) {
                            AppTab.entries.forEach { tab ->
                                val selected = tab == currentTab
                                val icon = when (tab) {
                                    AppTab.JOURNEY -> Icons.Rounded.Explore
                                    AppTab.CHARACTER -> Icons.Rounded.Person
                                    AppTab.COMPANIONS -> Icons.Rounded.Groups
                                    AppTab.CAMP -> Icons.Rounded.Cottage
                                    AppTab.COMPENDIUM -> Icons.AutoMirrored.Rounded.MenuBook
                                }
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        viewModel.clearNotices()
                                        currentTab = tab
                                    },
                                    alwaysShowLabel = true,
                                    icon = { Icon(icon, contentDescription = null) },
                                    label = {
                                        Text(
                                            text = tab.label,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = DreamTrailColors.LanternInk,
                                        selectedTextColor = DreamTrailColors.LanternAmber,
                                        indicatorColor = DreamTrailColors.LanternAmber,
                                        unselectedIconColor = DreamTrailColors.MutedInk,
                                        unselectedTextColor = DreamTrailColors.MutedInk
                                    )
                                )
                            }
                        }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .widthIn(max = 600.dp)
                        ) {
                            if (!dataState.initialized) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = DreamTrailColors.LanternAmber)
                                }
                            } else {
                                tabStateHolder.SaveableStateProvider(currentTab.name) {
                                    when (currentTab) {
                                        AppTab.JOURNEY -> JourneyScreen(
                                            dataState = dataState,
                                            uiState = uiState,
                                            onSelectRoute = viewModel::selectRoute,
                                            onSelectStrategy = viewModel::selectStrategy,
                                            onStartExpedition = viewModel::startExpedition,
                                            onCancelEarly = viewModel::cancelExpeditionEarly,
                                            onEndExpedition = viewModel::endExpedition,
                                            onDismissReport = viewModel::dismissLatestReport,
                                            onOpenHistoryReport = viewModel::openHistoryReport,
                                            onRetry = viewModel::retryFailedAction,
                                            onSelectCompanion = viewModel::selectActiveCompanion,
                                            onSelectTeaBlend = viewModel::selectTeaBlend,
                                            onSelectDreamBeaconTier = viewModel::selectDreamBeaconTier,
                                            onResolveMorningEcho = viewModel::resolveMorningEcho,
                                            onApplyLoadout = viewModel::applyLoadout
                                        )
                                        AppTab.CHARACTER -> CharacterScreen(
                                            dataState = dataState,
                                            statusNotice = uiState.statusNotice,
                                            errorMessage = uiState.errorMessage,
                                            actionInProgress = uiState.isPerformingAction,
                                            onEquipItem = viewModel::equipItem,
                                            onUnequipSlot = viewModel::unequipSlot,
                                            onUpgradeEquipment = viewModel::upgradeEquipment,
                                            onReforgeAffix = viewModel::reforgeEquipmentAffix,
                                            onEngraveSecondaryAffix = viewModel::engraveSecondaryAffix,
                                            onToggleLock = viewModel::toggleLock,
                                            onSalvageSelected = viewModel::salvageSelected,
                                            onTogglePassive = viewModel::togglePassive,
                                            duplicateSalvageCandidates = viewModel::duplicateSalvageIds,
                                            onUnlockAstralNode = viewModel::unlockAstralNode,
                                            onResetAstralNodes = viewModel::resetAstralNodes,
                                            onSaveLoadout = { viewModel.saveLoadout(it) },
                                            onApplyLoadout = viewModel::applyLoadout
                                        )
                                        AppTab.COMPANIONS -> CompanionsScreen(
                                            dataState = dataState,
                                            onSelectCompanion = viewModel::selectActiveCompanion,
                                            onGiftCompanion = viewModel::giftCompanion
                                        )
                                        AppTab.CAMP -> CampScreen(
                                            dataState = dataState,
                                            onUpgradeFacility = viewModel::upgradeFacility,
                                            onCraftEquipment = viewModel::craftEquipment,
                                            onCraftKeepsake = viewModel::craftKeepsake,
                                            onSelectTeaBlend = viewModel::selectTeaBlend,
                                            onTransmuteMaterial = viewModel::transmuteMaterial
                                        )
                                        AppTab.COMPENDIUM -> CompendiumScreen(
                                            dataState = dataState,
                                            onNavigateToCompanions = { currentTab = AppTab.COMPANIONS }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                    if (showSettings) {
                        SettingsAndBackupSheet(
                            dataState = dataState,
                            backupAction = uiState.backupAction,
                            statusNotice = uiState.statusNotice,
                            errorMessage = uiState.errorMessage,
                            onUpdateSettings = viewModel::updateSettings,
                            onCopyBackup = viewModel::copyBackup,
                            onExportToFile = {
                                exportDocLauncher.launch("dreamtrail-backup.json")
                            },
                            onRequestImportFile = {
                                importDocLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                            },
                            onImportRawText = viewModel::importBackup,
                            onRestoreSnapshot = viewModel::restoreSnapshot,
                            onDismiss = { showSettings = false }
                        )
                    }
                }
            }
        }
    }
}
