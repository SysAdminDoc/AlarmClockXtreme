package com.sysadmindoc.alarmclock.ui.alarmfiring.challenges

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FdroidDigitalInkChallengeRecognizer @Inject constructor() : DigitalInkChallengeRecognizer {
    override suspend fun recognize(
        request: DigitalInkRecognitionRequest
    ): DigitalInkRecognitionResult = DigitalInkRecognitionResult(
        candidates = emptyList(),
        unavailableReason = "Handwriting recognition is not included in the F-Droid build. Type the word instead."
    )
}
