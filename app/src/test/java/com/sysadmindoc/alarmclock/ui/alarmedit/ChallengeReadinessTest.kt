package com.sysadmindoc.alarmclock.ui.alarmedit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChallengeReadinessTest {

    private val allReady = DeviceChallengeCapabilities()
    private val refs = ChallengeReferences(
        nfcTagId = "tag123",
        barcodeValue = "abc",
        photoMatchUri = "content://photo",
        wifiDismissSsid = "HomeNet"
    )

    @Test
    fun nonPhysicalChallengeHasNoReadinessRow() {
        assertNull(evaluateChallengeReadiness("MATH_EASY", allReady, refs))
        assertNull(evaluateChallengeReadiness("NONE", allReady, refs))
        assertNull(evaluateChallengeReadiness("TYPING", allReady, refs))
    }

    @Test
    fun fullyConfiguredPhysicalChallengesAreReady() {
        listOf("SHAKE", "SQUAT", "WALK_STEPS", "NFC_SCAN", "BARCODE_SCAN", "PHOTO_MATCH", "WIFI_CONNECT")
            .forEach { type ->
                val verdict = evaluateChallengeReadiness(type, allReady, refs)!!
                assertEquals("$type should be ready", ChallengeReadinessStatus.READY, verdict.status)
                assertFalse("$type ready must not block save", verdict.blocksSave)
            }
    }

    @Test
    fun nfcUnavailableBlocksSave() {
        val verdict = evaluateChallengeReadiness("NFC_SCAN", allReady.copy(hasNfc = false), refs)!!
        assertEquals(ChallengeReadinessStatus.NEEDS_HARDWARE, verdict.status)
        assertTrue(verdict.blocksSave)
    }

    @Test
    fun nfcDisabledOnlyWarns() {
        val verdict = evaluateChallengeReadiness("NFC_SCAN", allReady.copy(nfcEnabled = false), refs)!!
        assertEquals(ChallengeReadinessStatus.NEEDS_PERMISSION, verdict.status)
        assertFalse(verdict.blocksSave)
    }

    @Test
    fun missingNfcReferenceBlocksSave() {
        val verdict = evaluateChallengeReadiness("NFC_SCAN", allReady, refs.copy(nfcTagId = ""))!!
        assertEquals(ChallengeReadinessStatus.NEEDS_REFERENCE, verdict.status)
        assertTrue(verdict.blocksSave)
    }

    @Test
    fun cameraDeniedWarnsButBarcodeReferencePresent() {
        val verdict = evaluateChallengeReadiness("BARCODE_SCAN", allReady.copy(cameraGranted = false), refs)!!
        assertEquals(ChallengeReadinessStatus.NEEDS_PERMISSION, verdict.status)
        assertFalse(verdict.blocksSave)
    }

    @Test
    fun missingBarcodeAndPhotoReferencesBlockSave() {
        val barcode = evaluateChallengeReadiness("BARCODE_SCAN", allReady, refs.copy(barcodeValue = ""))!!
        assertEquals(ChallengeReadinessStatus.NEEDS_REFERENCE, barcode.status)

        val photo = evaluateChallengeReadiness("PHOTO_MATCH", allReady, refs.copy(photoMatchUri = ""))!!
        assertEquals(ChallengeReadinessStatus.NEEDS_REFERENCE, photo.status)
        assertTrue(photo.blocksSave)
    }

    @Test
    fun noCameraBlocksBarcodeAndPhotoEvenWithReference() {
        val noCam = allReady.copy(hasCamera = false)
        assertEquals(
            ChallengeReadinessStatus.NEEDS_HARDWARE,
            evaluateChallengeReadiness("BARCODE_SCAN", noCam, refs)!!.status
        )
        assertEquals(
            ChallengeReadinessStatus.NEEDS_HARDWARE,
            evaluateChallengeReadiness("PHOTO_MATCH", noCam, refs)!!.status
        )
    }

    @Test
    fun activityRecognitionDeniedWarnsForWalkSteps() {
        val verdict = evaluateChallengeReadiness(
            "WALK_STEPS",
            allReady.copy(activityRecognitionGranted = false),
            refs
        )!!
        assertEquals(ChallengeReadinessStatus.NEEDS_PERMISSION, verdict.status)
        assertFalse(verdict.blocksSave)
    }

    @Test
    fun noStepCounterBlocksWalkSteps() {
        val verdict = evaluateChallengeReadiness("WALK_STEPS", allReady.copy(hasStepCounter = false), refs)!!
        assertEquals(ChallengeReadinessStatus.NEEDS_HARDWARE, verdict.status)
        assertTrue(verdict.blocksSave)
    }

    @Test
    fun wifiAndLocationGatesForWifiChallenge() {
        val missingSsid = evaluateChallengeReadiness("WIFI_CONNECT", allReady, refs.copy(wifiDismissSsid = ""))!!
        assertEquals(ChallengeReadinessStatus.NEEDS_REFERENCE, missingSsid.status)
        assertTrue(missingSsid.blocksSave)

        val locationDenied = evaluateChallengeReadiness("WIFI_CONNECT", allReady.copy(locationGranted = false), refs)!!
        assertEquals(ChallengeReadinessStatus.NEEDS_PERMISSION, locationDenied.status)
        assertFalse(locationDenied.blocksSave)

        val noWifi = evaluateChallengeReadiness("WIFI_CONNECT", allReady.copy(hasWifi = false), refs)!!
        assertEquals(ChallengeReadinessStatus.NEEDS_HARDWARE, noWifi.status)
    }

    @Test
    fun noAccelerometerBlocksMotionChallenges() {
        val noAccel = allReady.copy(hasAccelerometer = false)
        assertEquals(ChallengeReadinessStatus.NEEDS_HARDWARE, evaluateChallengeReadiness("SHAKE", noAccel, refs)!!.status)
        assertEquals(ChallengeReadinessStatus.NEEDS_HARDWARE, evaluateChallengeReadiness("SQUAT", noAccel, refs)!!.status)
    }

    @Test
    fun activeReadinessReturnsWorstAcrossChain() {
        // Active challenge is fully ready, but a chained NFC challenge is missing a tag.
        val verdict = evaluateActiveChallengeReadiness(
            challengeType = "MATH_EASY",
            challengeChain = "MATH_EASY,NFC_SCAN",
            capabilities = allReady,
            references = refs.copy(nfcTagId = "")
        )!!
        assertEquals(ChallengeReadinessStatus.NEEDS_REFERENCE, verdict.status)
        assertTrue(verdict.blocksSave)
    }

    @Test
    fun activeReadinessPrefersHardwareOverPermission() {
        val verdict = evaluateActiveChallengeReadiness(
            challengeType = "WIFI_CONNECT",
            challengeChain = "NFC_SCAN",
            // Wi-Fi only needs location permission (warn); NFC has no hardware (block).
            capabilities = allReady.copy(locationGranted = false, hasNfc = false),
            references = refs
        )!!
        assertEquals(ChallengeReadinessStatus.NEEDS_HARDWARE, verdict.status)
    }

    @Test
    fun missingChallengeReferencesListsOnlyBlockingTypes() {
        val missing = missingChallengeReferences(
            challengeType = "NFC_SCAN",
            challengeChain = "BARCODE_SCAN,PHOTO_MATCH,WIFI_CONNECT,MATH_EASY",
            references = ChallengeReferences(
                nfcTagId = "",
                barcodeValue = "set",
                photoMatchUri = "",
                wifiDismissSsid = "set"
            )
        )
        assertEquals(listOf("NFC_SCAN", "PHOTO_MATCH"), missing)
    }

    @Test
    fun missingChallengeReferencesEmptyWhenAllSet() {
        assertTrue(
            missingChallengeReferences("BARCODE_SCAN", "", refs).isEmpty()
        )
        assertTrue(
            missingChallengeReferences("NONE", "", ChallengeReferences()).isEmpty()
        )
    }
}
