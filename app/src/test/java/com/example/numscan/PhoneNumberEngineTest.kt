package com.example.numscan

import org.junit.Assert.*
import org.junit.Test

class PhoneNumberEngineTest {

    @Test
    fun `detects basic US number`() {
        val results = PhoneNumberEngine.extractPhoneNumbers("Call us at 555-867-5309")
        assertTrue(results.isNotEmpty())
        assertTrue(results.any { it.rawNumber.contains("555") })
    }

    @Test
    fun `detects international number with plus`() {
        val results = PhoneNumberEngine.extractPhoneNumbers("Contact: +66 81 234 5678")
        assertTrue(results.isNotEmpty())
        assertEquals("TH", results.first().countryCode)
    }

    @Test
    fun `detects UK number`() {
        val results = PhoneNumberEngine.extractPhoneNumbers("+44 20 7946 0958")
        assertTrue(results.isNotEmpty())
        assertEquals("UK", results.first().countryCode)
    }

    @Test
    fun `ignores short sequences`() {
        val results = PhoneNumberEngine.extractPhoneNumbers("Code: 123")
        assertTrue(results.isEmpty())
    }

    @Test
    fun `handles multiple numbers in text`() {
        val text = "Call +1-800-555-0100 or +44-20-7946-0958 for support"
        val results = PhoneNumberEngine.extractPhoneNumbers(text)
        assertTrue(results.size >= 2)
    }

    @Test
    fun `deduplicates same number`() {
        val results = PhoneNumberEngine.extractPhoneNumbers("+1 555 123 4567 +1 555 123 4567")
        assertEquals(1, results.size)
    }
}
