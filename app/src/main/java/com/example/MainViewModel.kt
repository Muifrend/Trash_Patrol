package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

sealed class AppScreen {
    object Report : AppScreen()
    object Mission : AppScreen()
    object Settings : AppScreen()
}

class MainViewModel(context: Context) : ViewModel() {
    private val preferencesManager = PreferencesManager(context)

    // Navigator
    private val _currentScreen = MutableStateFlow<AppScreen>(AppScreen.Report)
    val currentScreen: StateFlow<AppScreen> = _currentScreen

    // Core Settings
    private val _coordinatorUrl = MutableStateFlow(preferencesManager.coordinatorUrl)
    val coordinatorUrl: StateFlow<String> = _coordinatorUrl

    // Report Screen States
    private val _activeReportPhotoFile = MutableStateFlow<File?>(null)
    val activeReportPhotoFile: StateFlow<File?> = _activeReportPhotoFile

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing

    private val _rejectionReason = MutableStateFlow<String?>(null)
    val rejectionReason: StateFlow<String?> = _rejectionReason

    // Mission Screen States
    private val _activeMissionId = MutableStateFlow<String?>(null)
    val activeMissionId: StateFlow<String?> = _activeMissionId

    private val _missionState = MutableStateFlow<String?>(null) // e.g., received, classifying, etc.
    val missionState: StateFlow<String?> = _missionState

    private val _classification = MutableStateFlow<Classification?>(null)
    val classification: StateFlow<Classification?> = _classification

    private val _verification = MutableStateFlow<Verification?>(null)
    val verification: StateFlow<Verification?> = _verification

    private val _activeConfirmationPhotoFile = MutableStateFlow<File?>(null)
    val activeConfirmationPhotoFile: StateFlow<File?> = _activeConfirmationPhotoFile

    private val _isAnalyzingConfirmation = MutableStateFlow(false)
    val isAnalyzingConfirmation: StateFlow<Boolean> = _isAnalyzingConfirmation

    // Error State
    private val _networkError = MutableStateFlow<String?>(null)
    val networkError: StateFlow<String?> = _networkError

    // Settings Screen Test Connection State
    private val _isTestingConnection = MutableStateFlow(false)
    val isTestingConnection: StateFlow<Boolean> = _isTestingConnection

    private val _testConnectionResult = MutableStateFlow<String?>(null) // "Connected" or "Unreachable"
    val testConnectionResult: StateFlow<String?> = _testConnectionResult

    // GPS location
    private var lastRecordedLocation: Location? = null

    // Timeout Tracker
    private var lastStateChangeTime = 0L
    private var lastObservedState: String? = null

    // Polling Job
    private var pollingJob: Job? = null

    init {
        // Initialize default UI values
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun updateCoordinatorUrl(url: String) {
        preferencesManager.coordinatorUrl = url
        _coordinatorUrl.value = url
        _testConnectionResult.value = null // clear prior test result
    }

    fun setReportPhoto(file: File?) {
        _activeReportPhotoFile.value = file
        _rejectionReason.value = null
    }

    fun setConfirmationPhoto(file: File?) {
        _activeConfirmationPhotoFile.value = file
    }

    fun updateLocation(location: Location?) {
        lastRecordedLocation = location
    }

    fun testConnection() {
        _isTestingConnection.value = true
        _testConnectionResult.value = null
        viewModelScope.launch {
            try {
                val api = NetworkClient.getApi(_coordinatorUrl.value)
                val response = api.checkHealth()
                if (response.isSuccessful) {
                    _testConnectionResult.value = "Connected"
                    _networkError.value = null
                } else {
                    _testConnectionResult.value = "Unreachable"
                    trackError()
                }
            } catch (e: Exception) {
                _testConnectionResult.value = "Unreachable"
                trackError()
            } finally {
                _isTestingConnection.value = false
            }
        }
    }

    private fun trackError() {
        val url = _coordinatorUrl.value
        if (url.contains("ngrok", ignoreCase = true)) {
            _networkError.value = "ngrok tunnel may have restarted — update URL in Settings"
        } else {
            _networkError.value = "Cannot reach coordinator. Check Settings."
        }
    }

    // Submit trash report
    fun submitTrashReport(context: Context) {
        val originalPhoto = _activeReportPhotoFile.value ?: return
        _isAnalyzing.value = true
        _rejectionReason.value = null
        _networkError.value = null

        viewModelScope.launch {
            try {
                // Compress photo before uploading (85% JPEG quality)
                val compressedPhoto = compressImageFile(context, originalPhoto)

                val missionIdStr = "cleanup-" + UUID.randomUUID().toString().take(8)

                // Image file part
                val requestFile = compressedPhoto.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val imagePart = MultipartBody.Part.createFormData("image", compressedPhoto.name, requestFile)

                // Mission ID part
                val missionIdBody = missionIdStr.toRequestBody("text/plain".toMediaTypeOrNull())

                // Location part (optional/deniable)
                val locationPart = if (lastRecordedLocation != null) {
                    val lat = lastRecordedLocation?.latitude ?: 0.0
                    val lng = lastRecordedLocation?.longitude ?: 0.0
                    val locationJson = """{"lat":$lat,"lng":$lng}"""
                    MultipartBody.Part.createFormData("location", locationJson)
                } else {
                    null
                }

                val api = NetworkClient.getApi(_coordinatorUrl.value)
                val response = api.submitReport(imagePart, missionIdBody, locationPart)

                if (response.isSuccessful) {
                    val reportResp = response.body()
                    if (reportResp != null) {
                        _networkError.value = null
                        if (reportResp.status == "confirmed") {
                            // Succeeded reporting! Navigate to Mission overview and start polling
                            _activeMissionId.value = reportResp.missionId
                            _missionState.value = "confirmed"
                            _classification.value = reportResp.classification
                            _verification.value = null
                            _activeConfirmationPhotoFile.value = null
                            _isAnalyzing.value = false
                            
                            navigateTo(AppScreen.Mission)
                            startPollingMission(reportResp.missionId)
                        } else {
                            // Rejected reporting
                            _rejectionReason.value = reportResp.classification?.rejectionReason ?: "Image rejected"
                            _isAnalyzing.value = false
                        }
                    } else {
                        throw Exception("Received empty submission payload")
                    }
                } else {
                    throw Exception("API submission HTTP response code: ${response.code()}")
                }
            } catch (e: Exception) {
                _isAnalyzing.value = false
                trackError()
            }
        }
    }

    // Submit confirmation photo when robot finishes
    fun submitConfirmationPhoto(context: Context) {
        val originalPhoto = _activeConfirmationPhotoFile.value ?: return
        val missionId = _activeMissionId.value ?: return
        _isAnalyzingConfirmation.value = true
        _networkError.value = null

        viewModelScope.launch {
            try {
                val compressedPhoto = compressImageFile(context, originalPhoto)

                val requestFile = compressedPhoto.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val imagePart = MultipartBody.Part.createFormData("image", compressedPhoto.name, requestFile)

                val missionIdBody = missionId.toRequestBody("text/plain".toMediaTypeOrNull())

                val api = NetworkClient.getApi(_coordinatorUrl.value)
                val response = api.submitConfirmation(imagePart, missionIdBody)

                if (response.isSuccessful) {
                    val confirmResp = response.body()
                    if (confirmResp != null) {
                        _networkError.value = null
                        _missionState.value = confirmResp.state
                        if (confirmResp.verification != null) {
                            _verification.value = confirmResp.verification
                        }
                        _currentScreen.value = AppScreen.Mission
                        // Restart polling to verify final state
                        startPollingMission(missionId)
                    } else {
                        throw Exception("Received empty verification payload")
                    }
                } else {
                    throw Exception("API verification response code: ${response.code()}")
                }
            } catch (e: Exception) {
                trackError()
            } finally {
                _isAnalyzingConfirmation.value = false
            }
        }
    }

    // Polling GET /missions/{mission_id}
    fun startPollingMission(missionId: String) {
        pollingJob?.cancel()
        _activeMissionId.value = missionId
        lastStateChangeTime = System.currentTimeMillis()
        lastObservedState = _missionState.value ?: "unknown"

        pollingJob = viewModelScope.launch {
            while (true) {
                val api = NetworkClient.getApi(_coordinatorUrl.value)
                try {
                    val response = api.getMission(missionId)
                    if (response.isSuccessful) {
                        val missionResp = response.body()
                        if (missionResp != null) {
                            // If we were displaying a network alert, clear it now that connection is back
                            if (_networkError.value == "Connection lost, retrying...") {
                                _networkError.value = null
                            }

                            val newState = missionResp.state
                            _missionState.value = newState
                            
                            if (missionResp.classification != null) {
                                _classification.value = missionResp.classification
                            }

                            if (newState != lastObservedState) {
                                lastStateChangeTime = System.currentTimeMillis()
                                lastObservedState = newState
                            } else {
                                // 30-second stalemate check
                                if (System.currentTimeMillis() - lastStateChangeTime > 30_000) {
                                    // Change the state to custom "timeout" to display the offline recovery prompt in UI
                                    _missionState.value = "timeout"
                                    break
                                }
                            }

                            // Terminate polling loop when finality is achieved
                            if (newState in listOf("cleaned", "not_cleaned", "robot_failed", "error")) {
                                break
                            }
                        }
                    } else {
                        _networkError.value = "Connection lost, retrying..."
                    }
                } catch (e: Exception) {
                    _networkError.value = "Connection lost, retrying..."
                }
                delay(2000)
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    private fun compressImageFile(context: Context, originalFile: File): File {
        return try {
            val bitmap = BitmapFactory.decodeFile(originalFile.absolutePath) ?: return originalFile
            val compressedFile = File(context.cacheDir, "compressed_${originalFile.name}")
            val out = FileOutputStream(compressedFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            out.flush()
            out.close()
            compressedFile
        } catch (e: Exception) {
            originalFile // fallback
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }
}
