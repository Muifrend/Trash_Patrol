package com.example.ui

import android.Manifest
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.AppScreen
import com.example.Classification
import com.example.MainViewModel
import com.example.Verification
import coil.compose.AsyncImage
import com.example.ui.theme.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.io.File

@Composable
fun MainAppContainer(
    viewModel: MainViewModel,
    onGpsRequest: () -> Unit
) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val networkError by viewModel.networkError.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = WorldCupDarkBackground,
        topBar = {
            TopAppBarTracker(
                currentScreen = currentScreen,
                onNavigateSettings = { viewModel.navigateTo(AppScreen.Settings) },
                onNavigateBack = { viewModel.navigateTo(AppScreen.Report) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Screen Rendering
            when (currentScreen) {
                is AppScreen.Report -> ReportScreen(viewModel, onGpsRequest)
                is AppScreen.Mission -> MissionScreen(viewModel)
                is AppScreen.Settings -> SettingsScreen(viewModel)
            }

            // Referee Yellow/Red Warning Banner at bottom of screens
            networkError?.let { errText ->
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth()
                        .shadow(8.dp, RoundedCornerShape(12.dp))
                        .background(
                            brush = Brush.verticalGradient(
                                listOf(WorldCupRedCard.copy(alpha = 0.95f), WorldCupDarkBackground)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .border(1.5.dp, WorldCupRedCard, RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Penalty Warning",
                            tint = WorldCupGoldTrophy,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = errText,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBarTracker(
    currentScreen: AppScreen,
    onNavigateSettings: () -> Unit,
    onNavigateBack: () -> Unit
) {
    CenterAlignedTopAppBar(
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color(0xFF0F1411), // Midnight turf green
            titleContentColor = WorldCupGoldTrophy
        ),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.SportsSoccer,
                    contentDescription = "BallLogo",
                    tint = WorldCupPitchGreen,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "TRASH PATROL",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    letterSpacing = 2.sp,
                    fontFamily = FontFamily.SansSerif
                )
            }
        },
        navigationIcon = {
            if (currentScreen != AppScreen.Report) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("back_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back home",
                        tint = WorldCupGoldTrophy
                    )
                }
            }
        },
        actions = {
            IconButton(
                onClick = onNavigateSettings,
                modifier = Modifier.testTag("settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Tactics Configuration",
                    tint = WorldCupGoldTrophy
                )
            }
        }
    )
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ReportScreen(
    viewModel: MainViewModel,
    onGpsRequest: () -> Unit
) {
    val context = LocalContext.current
    val photoFile by viewModel.activeReportPhotoFile.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val rejectionReason by viewModel.rejectionReason.collectAsState()

    val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)
    val locationPermissionState = rememberPermissionState(permission = Manifest.permission.ACCESS_FINE_LOCATION)

    var tempPhotoFile by remember { mutableStateOf<File?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempPhotoFile?.let { file ->
                viewModel.setReportPhoto(file)
                onGpsRequest() // Request GPS update
            }
        }
    }

    fun triggerKickoffCamera() {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        } else {
            // Ensure location permission requested
            if (!locationPermissionState.status.isGranted) {
                locationPermissionState.launchPermissionRequest()
            }
            val cacheFile = File(context.cacheDir, "trash_capture_${System.currentTimeMillis()}.jpg")
            tempPhotoFile = cacheFile
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                cacheFile
            )
            cameraLauncher.launch(uri)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // World Cup Info Bar
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF14201A)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(WorldCupPitchGreen, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "WORLD CUP EDITION — PITCH CLEANUP MISSION",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        item {
            if (photoFile == null) {
                // Large glowing soccer ball capture button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer stadium lighting glow
                    val infiniteTransition = rememberInfiniteTransition()
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 0.95f,
                        targetValue = 1.05f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1500, easing = EaseInOutSine),
                            repeatMode = RepeatMode.Reverse
                        )
                    )

                    Surface(
                        modifier = Modifier
                            .size(190.dp * pulseScale)
                            .shadow(24.dp, CircleShape, spotColor = WorldCupPitchGreen),
                        color = Color.DarkGray,
                        shape = CircleShape,
                        border = BorderStroke(4.dp, WorldCupGoldTrophy)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(Color(0xFF1E3524), WorldCupDarkBackground)
                                    )
                                )
                                .clickable { triggerKickoffCamera() }
                                .testTag("report_trash_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                // Draw Soccer Ball Icon
                                Icon(
                                    imageVector = Icons.Default.SportsSoccer,
                                    contentDescription = "Soccer Ball",
                                    tint = WorldCupPitchGreen,
                                    modifier = Modifier.size(72.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "REPORT TRASH",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Tap to Kick-off",
                                    fontSize = 10.sp,
                                    color = GrayText
                                )
                            }
                        }
                    }
                }
            } else {
                // Show photo Preview in dynamic scoreboard card
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "MATCH SNAPSHOT",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = WorldCupGoldTrophy,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(2.dp, WorldCupPitchGreen, RoundedCornerShape(16.dp))
                    ) {
                        AsyncImage(
                            model = photoFile,
                            contentDescription = "Capture preview",
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                                .clickable { viewModel.setReportPhoto(null) }
                                .padding(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isAnalyzing) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = WorldCupCardBackground),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    color = WorldCupPitchGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = "Analyzing photo...",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    } else {
                        Button(
                            onClick = { viewModel.submitTrashReport(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = WorldCupPitchGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("submit_report_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Submit button icon",
                                tint = Color.Black
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "SUBMIT REPORT",
                                color = Color.Black,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }
        }

        item {
            // Rejection RED CARD Penalty indicator
            rejectionReason?.let { reason ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF261114)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, WorldCupRedCard),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Drawing a physical vertical Red Card
                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .height(40.dp)
                                .background(WorldCupRedCard, RoundedCornerShape(4.dp))
                                .border(1.dp, Color.White, RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "RED CARD — PENALTY",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = WorldCupRedCard,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = reason,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "TRY AGAIN",
                                color = WorldCupPitchGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { triggerKickoffCamera() }
                                    .testTag("try_again_button")
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MissionScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val missionId by viewModel.activeMissionId.collectAsState()
    val missionState by viewModel.missionState.collectAsState()
    val classification by viewModel.classification.collectAsState()
    val verification by viewModel.verification.collectAsState()
    val confirmationPhoto by viewModel.activeConfirmationPhotoFile.collectAsState()
    val isAnalyzingConfirmation by viewModel.isAnalyzingConfirmation.collectAsState()

    val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)
    var tempPhotoFile by remember { mutableStateOf<File?>(null) }

    val confirmationCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempPhotoFile?.let { file ->
                viewModel.setConfirmationPhoto(file)
            }
        }
    }

    fun launchConfirmationCamera() {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        } else {
            val cacheFile = File(context.cacheDir, "confirmation_capture_${System.currentTimeMillis()}.jpg")
            tempPhotoFile = cacheFile
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                cacheFile
            )
            confirmationCameraLauncher.launch(uri)
        }
    }

    // Map internal raw states to descriptive scoreboard strings
    val humanReadableStatus = when (missionState) {
        "received" -> "Report received"
        "classifying" -> "Analyzing image..."
        "confirmed" -> "Trash confirmed ✓"
        "dispatching" -> "Dispatching robot..."
        "awaiting_bridge_callback" -> "Robot is searching..."
        "robot_failed" -> "Robot could not find trash"
        "awaiting_confirmation_photo" -> "Cleanup done! Take confirmation photo →"
        "verifying_cleanup" -> "Verifying cleanup..."
        "cleaned" -> "Mission complete ✓"
        "not_cleaned" -> "Trash still detected. Mission failed."
        "error" -> "Something went wrong"
        "timeout" -> "Mission status unknown"
        else -> "Gathering scouting reports..."
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Monospace VAR State Panel for dev tracking
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF151916)),
                border = BorderStroke(1.dp, Color(0xFF2C3530)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "VAR STATE MONITOR",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = WorldCupGoldTrophy
                    )
                    Text(
                        text = missionState ?: "unknown",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = WorldCupPitchGreen,
                        modifier = Modifier
                            .background(Color.Black, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // Main Live Status Card with football stadium lighting glow
        item {
            val cardThemeBorder = when (missionState) {
                "cleaned" -> WorldCupPitchGreen
                "not_cleaned", "error" -> WorldCupRedCard
                "robot_failed", "timeout" -> WorldCupYellowCard
                else -> WorldCupGoldTrophy
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = WorldCupCardBackground),
                border = BorderStroke(1.5.dp, cardThemeBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(12.dp, RoundedCornerShape(16.dp), spotColor = cardThemeBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "MATCH ID: ${missionId?.take(18) ?: "N/A"}",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        color = GrayText,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Human status glow
                    Box(
                        modifier = Modifier
                            .background(cardThemeBorder.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                            .border(1.dp, cardThemeBorder.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = humanReadableStatus,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Tactical sub note for Timeout cases
                    if (missionState == "timeout") {
                        Text(
                            text = "No state changes reported for 30s. The game is stalled.",
                            color = GrayText,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { missionId?.let { viewModel.startPollingMission(it) } },
                            colors = ButtonDefaults.buttonColors(containerColor = WorldCupGoldTrophy),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Icon(Icons.Default.Refresh, "Retry Poll", tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retry Sync Kick-off", color = Color.Black)
                        }
                    }
                }
            }
        }

        // Custom drawn 2D Soccer Pitch Progress Tracker Canvas
        // Renders visual representations of our Robot Striker playing on turf!
        item {
            Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "LITTER MAP (MATCH TRACKER)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = WorldCupGoldTrophy,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                SoccerPitchCanvas(state = missionState ?: "received")
            }
        }

        // Classification details or referee verdict card
        item {
            classification?.let { cls ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131715)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF22312A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Leaderboard,
                                contentDescription = "Scouting verdict icon",
                                tint = WorldCupGoldTrophy,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SCOUTING VERDICT (AI CLASSIFICATION)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = WorldCupGoldTrophy,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Litter Type:", fontSize = 13.sp, color = GrayText)
                            Text(
                                text = cls.trashType?.replaceFirstChar { it.uppercase() } ?: "Unknown",
                                fontSize = 13.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Divider(color = Color(0xFF1F2924), modifier = Modifier.padding(vertical = 8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Scouting Confidence:", fontSize = 13.sp, color = GrayText)
                            val confidencePct = ((cls.confidence ?: 0.0) * 100).toInt()
                            Text(
                                text = "$confidencePct%",
                                fontSize = 13.sp,
                                color = WorldCupPitchGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        cls.visualEvidence?.let { evidence ->
                            if (evidence.isNotEmpty()) {
                                Divider(color = Color(0xFF1F2924), modifier = Modifier.padding(vertical = 8.dp))
                                Text(
                                    text = "Visual Evidence:",
                                    fontSize = 12.sp,
                                    color = GrayText,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = evidence,
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // Post Confirmation Photo flow (when state becomes awaiting_confirmation_photo)
        item {
            if (missionState == "awaiting_confirmation_photo") {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "MATCH POINT — CONFIRM CLEANUP",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = WorldCupGoldTrophy,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    if (confirmationPhoto == null) {
                        Button(
                            onClick = { launchConfirmationCamera() },
                            colors = ButtonDefaults.buttonColors(containerColor = WorldCupYellowCard),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("take_confirmation_photo_button")
                        ) {
                            Icon(Icons.Default.PhotoCamera, "Camera icon", tint = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "TAKE CONFIRMATION PHOTO",
                                color = Color.Black,
                                fontWeight = FontWeight.Black
                            )
                        }
                    } else {
                        // User has confirmation photo, ready to send
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(2.dp, WorldCupYellowCard, RoundedCornerShape(12.dp))
                        ) {
                            AsyncImage(
                                model = confirmationPhoto,
                                contentDescription = "Confirmation capture",
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                                    .clickable { viewModel.setConfirmationPhoto(null) }
                                    .padding(6.dp)
                            ) {
                                Icon(Icons.Default.Close, "Cancel", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (isAnalyzingConfirmation) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(color = WorldCupYellowCard, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Verifying cleanup...", color = Color.White, fontSize = 14.sp)
                            }
                        } else {
                            Button(
                                onClick = { viewModel.submitConfirmationPhoto(context) },
                                colors = ButtonDefaults.buttonColors(containerColor = WorldCupPitchGreen),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                            ) {
                                Icon(Icons.Default.Check, "Send confirmation icon", tint = Color.Black)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "SUBMIT CLEANUP PROOF",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }
        }

        // Final Verification Verdict details
        item {
            verification?.let { ver ->
                val cardBorder = if (ver.cleaned == "cleaned") WorldCupPitchGreen else WorldCupRedCard
                val cardLabel = if (ver.cleaned == "cleaned") "GOAL DETECTED — CLEANUP VALIDATED ✓" else "FOUL — TRASH DETECTED"

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (ver.cleaned == "cleaned") Color(0xFF102118) else Color(0xFF281315)
                    ),
                    border = BorderStroke(1.5.dp, cardBorder),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = cardLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = cardBorder,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        ver.reason?.let { rsn ->
                            Text(
                                text = rsn,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                        }

                        ver.confidence?.let { conf ->
                            Spacer(modifier = Modifier.height(8.dp))
                            val confPct = (conf * 100).toInt()
                            Text(
                                text = "Verification confidence: $confPct%",
                                fontSize = 12.sp,
                                color = GrayText
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
fun SoccerPitchCanvas(state: String) {
    // Determine striker position percentage along the field width based on the mission state
    // Pitch is a rectangle drawn on a canvas
    // Green field, center white circle, goal post, striker ball position tracking
    val animationPercent = remember { Animatable(0f) }

    LaunchedEffect(state) {
        val target = when (state) {
            "received" -> 0.05f
            "classifying" -> 0.15f
            "confirmed" -> 0.30f
            "dispatching" -> 0.45f
            "awaiting_bridge_callback" -> 0.60f
            "robot_failed" -> 0.55f
            "awaiting_confirmation_photo" -> 0.85f
            "verifying_cleanup" -> 0.90f
            "cleaned" -> 1.0f // ball in net!
            "not_cleaned" -> 0.80f
            else -> 0.05f
        }
        animationPercent.animateTo(
            targetValue = target,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0F321C)) // Dark football field green
            .border(2.dp, Color(0xFF1E5230), RoundedCornerShape(12.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 1. Draw Field Line markup (White lines)
            val strokeWidth = 2.dp.toPx()
            val whiteLine = Color(0x60FFFFFF)

            // Outer boundary
            drawRect(
                color = whiteLine,
                topLeft = Offset(strokeWidth, strokeWidth),
                size = Size(w - strokeWidth * 2, h - strokeWidth * 2),
                style = Stroke(strokeWidth)
            )

            // Center Line
            drawLine(
                color = whiteLine,
                start = Offset(w / 2, strokeWidth),
                end = Offset(w / 2, h - strokeWidth),
                strokeWidth = strokeWidth
            )

            // Center Circle
            drawCircle(
                color = whiteLine,
                radius = 30.dp.toPx(),
                center = Offset(w / 2, h / 2),
                style = Stroke(strokeWidth)
            )

            // Goal penalty boxes
            // Left Box
            drawRect(
                color = whiteLine,
                topLeft = Offset(strokeWidth, h / 4),
                size = Size(20.dp.toPx(), h / 2),
                style = Stroke(strokeWidth)
            )
            // Right Box (Match endpoint)
            drawRect(
                color = whiteLine,
                topLeft = Offset(w - 20.dp.toPx() - strokeWidth, h / 4),
                size = Size(20.dp.toPx(), h / 2),
                style = Stroke(strokeWidth)
            )

            // 2. Draw progress positions
            val currentProgressX = w * animationPercent.value
            val verticalY = h / 2

            // Robot Striker Symbol (A gleaming Gold Trophy or Green circle tracker)
            val robotColor = if (state == "robot_failed") WorldCupRedCard else WorldCupGoldTrophy
            drawCircle(
                color = robotColor,
                radius = 10.dp.toPx(),
                center = Offset(currentProgressX, verticalY)
            )

            // Draw a tiny soccer ball rolling in front of the robot striker!
            val ballLeashX = currentProgressX + 16.dp.toPx()
            if (ballLeashX < w) {
                drawCircle(
                    color = Color.White,
                    radius = 5.dp.toPx(),
                    center = Offset(ballLeashX, verticalY),
                    style = Stroke(1.5.dp.toPx())
                )
                // Draw soccer panel center
                drawCircle(
                    color = Color.Black,
                    radius = 2.dp.toPx(),
                    center = Offset(ballLeashX, verticalY)
                )
            }
        }

        // Overlay Status Label next to the striker positions
        Text(
            text = "STRIKER STATUS: " + state.uppercase(),
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                .padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val currentUrl by viewModel.coordinatorUrl.collectAsState()
    val isTesting by viewModel.isTestingConnection.collectAsState()
    val testResult by viewModel.testConnectionResult.collectAsState()

    var urlInput by remember { mutableStateOf(currentUrl) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Screen header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Tactical boards icon",
                    tint = WorldCupGoldTrophy,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "TACTICAL BOARD (SETTINGS)",
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
            }
        }

        item {
            // URL Editing box styled as football ledger card
            Card(
                colors = CardDefaults.cardColors(containerColor = WorldCupCardBackground),
                border = BorderStroke(1.dp, Color(0xFF2C3530)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "COORDINATOR URL",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = WorldCupGoldTrophy,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    TextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("coordinator_url_input"),
                        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 14.sp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF0D0D0D),
                            unfocusedContainerColor = Color(0xFF0D0D0D),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedIndicatorColor = WorldCupPitchGreen,
                            unfocusedIndicatorColor = Color.Gray
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "ngrok URLs change on restart — update this if the app loses connection",
                        fontSize = 11.sp,
                        color = WorldCupYellowCard,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        item {
            // Connection result banner
            testResult?.let { result ->
                val isSuccess = result == "Connected"
                val bannerColor = if (isSuccess) WorldCupPitchGreen else WorldCupRedCard
                val labelText = if (isSuccess) "Connected ✓" else "Unreachable ✗"

                Card(
                    colors = CardDefaults.cardColors(containerColor = if (isSuccess) Color(0xFF102118) else Color(0xFF2B1416)),
                    border = BorderStroke(1.5.dp, bannerColor),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(bannerColor, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = labelText,
                            color = bannerColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Test Connection (Yellow card warning style)
                Button(
                    onClick = { viewModel.testConnection() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF252C28)),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, WorldCupGoldTrophy),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("test_connection_button"),
                    enabled = !isTesting
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(color = WorldCupGoldTrophy, modifier = Modifier.size(20.dp))
                    } else {
                        Text("TEST CONNECTION", color = WorldCupGoldTrophy, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                // Save URL (Bright pitch green)
                Button(
                    onClick = { viewModel.updateCoordinatorUrl(urlInput) },
                    colors = ButtonDefaults.buttonColors(containerColor = WorldCupPitchGreen),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("save_settings_button")
                ) {
                    Icon(Icons.Default.Save, "Save icon", tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("SAVE TACTICS", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
