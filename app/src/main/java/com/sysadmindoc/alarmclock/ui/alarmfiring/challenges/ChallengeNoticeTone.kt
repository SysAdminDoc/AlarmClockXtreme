package com.sysadmindoc.alarmclock.ui.alarmfiring.challenges

/**
 * Why a challenge notice is the colour it is.
 *
 * The voice and handwriting views used to pick their accent by reading the
 * status sentence: `startsWith("Heard")`, `startsWith("Checking")`,
 * `endsWith("matched.")`. That works only while the copy is English and only
 * while nobody rewords it, so moving those sentences into strings.xml would
 * have turned every one of these notices the wrong colour. The state carries
 * the reason now and the view maps the reason to a colour.
 */
enum class ChallengeNoticeTone {
    /** Something is under way, or the notice is just telling you what to do. */
    PROGRESS,

    /** The challenge step was solved. */
    SUCCESS,

    /** The attempt did not match, or the recogniser could not run. */
    PROBLEM
}
