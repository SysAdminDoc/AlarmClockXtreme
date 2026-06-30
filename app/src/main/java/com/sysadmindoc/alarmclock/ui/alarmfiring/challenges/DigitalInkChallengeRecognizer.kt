package com.sysadmindoc.alarmclock.ui.alarmfiring.challenges

data class InkPoint(
    val x: Float,
    val y: Float,
    val timestampMillis: Long
)

data class InkStroke(
    val points: List<InkPoint>
)

data class DigitalInkRecognitionRequest(
    val strokes: List<InkStroke>,
    val canvasWidth: Float,
    val canvasHeight: Float,
    val expectedText: String
)

data class DigitalInkRecognitionResult(
    val candidates: List<String>,
    val unavailableReason: String? = null
) {
    val isAvailable: Boolean get() = unavailableReason == null
}

interface DigitalInkChallengeRecognizer {
    suspend fun recognize(request: DigitalInkRecognitionRequest): DigitalInkRecognitionResult
}
