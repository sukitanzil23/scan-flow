package com.example.numscan

import java.util.regex.Pattern

data class PhoneNumberResult(
    val rawNumber: String,
    val countryCode: String?,
    val nationalNumber: String,
    val isValid: Boolean,
    val formattedNumber: String
)

object PhoneNumberEngine {

    private val PHONE_PATTERNS = listOf(
        // International with + prefix
        Pattern.compile("""(\+\d{1,3}[\s\-.]?\(?\d{1,4}\)?[\s\-.]?\d{1,4}[\s\-.]?\d{1,9})"""),
        // US/Canada: (123) 456-7890 or 123-456-7890
        Pattern.compile("""(\(?\d{3}\)?[\s\-.]?\d{3}[\s\-.]?\d{4})"""),
        // Generic long number
        Pattern.compile("""(\d{2,4}[\s\-.]?\d{3,4}[\s\-.]?\d{3,4})""")
    )

    private val COUNTRY_CODES = mapOf(
        "+1" to "US/CA", "+44" to "UK", "+61" to "AU", "+33" to "FR",
        "+49" to "DE", "+81" to "JP", "+82" to "KR", "+86" to "CN",
        "+91" to "IN", "+55" to "BR", "+52" to "MX", "+7" to "RU",
        "+39" to "IT", "+34" to "ES", "+31" to "NL", "+46" to "SE",
        "+47" to "NO", "+45" to "DK", "+41" to "CH", "+43" to "AT",
        "+66" to "TH", "+65" to "SG", "+60" to "MY", "+62" to "ID",
        "+63" to "PH", "+84" to "VN", "+852" to "HK", "+886" to "TW"
    )

    fun extractPhoneNumbers(text: String): List<PhoneNumberResult> {
        val results = mutableListOf<PhoneNumberResult>()
        val seen = mutableSetOf<String>()

        for (pattern in PHONE_PATTERNS) {
            val matcher = pattern.matcher(text)
            while (matcher.find()) {
                val raw = matcher.group(1)?.trim() ?: continue
                val normalized = raw.replace(Regex("""[\s\-.() ]"""), "")
                if (normalized.length < 7 || normalized.length > 15) continue
                if (seen.contains(normalized)) continue
                seen.add(normalized)

                val countryCode = detectCountryCode(normalized)
                val national = if (countryCode != null) {
                    normalized.removePrefix(countryCode.first)
                } else normalized

                val result = PhoneNumberResult(
                    rawNumber = raw,
                    countryCode = countryCode?.second,
                    nationalNumber = national,
                    isValid = isValidLength(normalized),
                    formattedNumber = format(raw, countryCode?.second)
                )
                results.add(result)
            }
        }
        return results.distinctBy { it.formattedNumber }
    }

    private fun detectCountryCode(normalized: String): Pair<String, String>? {
        if (!normalized.startsWith("+")) return null
        // Try longest match first
        for (len in 4 downTo 2) {
            if (normalized.length >= len) {
                val prefix = normalized.substring(0, len)
                COUNTRY_CODES[prefix]?.let { return Pair(prefix, it) }
            }
        }
        return null
    }

    private fun isValidLength(normalized: String): Boolean {
        val digits = normalized.filter { it.isDigit() }
        return digits.length in 7..15
    }

    private fun format(raw: String, countryName: String?): String {
        return raw.trim()
    }
}
