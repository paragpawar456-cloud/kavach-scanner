package com.kavach.core

import com.kavach.core.model.Verdict
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CallerCheckTest {
    @Test fun bankSeries() = assertEquals(Verdict.SAFE, CallerCheck.analyze("1600123456").verdict)
    @Test fun pakistan() = assertTrue(CallerCheck.analyze("+92 300 1234567").verdict != Verdict.SAFE)
    @Test fun mobile() = assertTrue(CallerCheck.analyze("+91 98765 43210").findings.any { it.id == "mobile" })
    @Test fun upiLure() = assertTrue(CallerCheck.analyze("sbi.refund@ybl").verdict != Verdict.SAFE)
    @Test fun upiNormal() = assertEquals(Verdict.SAFE, CallerCheck.analyze("sharmastores@okaxis").verdict)
}
