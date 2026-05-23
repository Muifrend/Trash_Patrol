package com.example

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Classification(
    @Json(name = "is_trash") val isTrash: String? = null,
    @Json(name = "trash_type") val trashType: String? = null,
    val confidence: Double? = null,
    @Json(name = "rejection_reason") val rejectionReason: String? = null,
    @Json(name = "visual_evidence") val visualEvidence: String? = null
)

@JsonClass(generateAdapter = true)
data class ReportResponse(
    @Json(name = "mission_id") val missionId: String,
    val status: String, // "confirmed" or "rejected"
    val classification: Classification? = null,
    val next: String? = null
)

@JsonClass(generateAdapter = true)
data class MissionResponse(
    @Json(name = "mission_id") val missionId: String,
    val state: String, // received, classifying, confirmed, dispatching, awaiting_bridge_callback, robot_failed, awaiting_confirmation_photo, verifying_cleanup, cleaned, not_cleaned, error
    val classification: Classification? = null
)

@JsonClass(generateAdapter = true)
data class Verification(
    val cleaned: String? = null,
    val confidence: Double? = null,
    val reason: String? = null,
    @Json(name = "needs_retry") val needsRetry: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class ConfirmationResponse(
    @Json(name = "mission_id") val missionId: String,
    val state: String,
    val verification: Verification? = null
)
