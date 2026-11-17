package com.kavach.core

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PasswordCheckerTest {
    @Test fun weakOnes() {
        for (p in listOf("123456", "password", "India@123", "qwerty123", "9876543210", "aaaa1111"))
            assertTrue(PasswordChecker.check(p).strength <= PasswordChecker.Strength.WEAK, "$p -> ${PasswordChecker.check(p)}")
    }
    @Test fun strongOnes() {
        assertTrue(PasswordChecker.check("Mango!River#Tiger72Lamp").strength >= PasswordChecker.Strength.STRONG)
    }
    @Test fun sha1() {
        assertEquals("5BAA61E4C9B93F3F0682250B6CF8331B7EE68FD8", PasswordChecker.sha1("password"))
    }
}
