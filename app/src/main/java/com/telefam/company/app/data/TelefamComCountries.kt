package com.telefam.company.app.data

import java.util.Locale

data class TelefamCountry(
    val isoCode: String,
    val name: String,
    val dialCode: String,
    val flagEmoji: String,
    val minDigits: Int = 7,
    val maxDigits: Int = 12
)

object TelefamComCountries {

    fun flagEmojiFromIso(isoCode: String): String {
        val upper = isoCode.uppercase(Locale.US)
        if (upper.length != 2) return "🌐"
        val first = Character.codePointAt(upper, 0) - 0x41 + 0x1F1E6
        val second = Character.codePointAt(upper, 1) - 0x41 + 0x1F1E6
        return String(Character.toChars(first)) + String(Character.toChars(second))
    }

    fun findByIso(isoCode: String): TelefamCountry {
        return allCountries.find { it.isoCode.equals(isoCode, ignoreCase = true) }
            ?: defaultCountry
    }

    val allCountries: List<TelefamCountry> by lazy {
        listOf(
            create("KE", "Kenya", "+254", 9, 9),
            create("US", "United States", "+1", 10, 10),
            create("GB", "United Kingdom", "+44", 10, 10),
            create("CA", "Canada", "+1", 10, 10),
            create("NG", "Nigeria", "+234", 10, 10),
            create("ZA", "South Africa", "+27", 9, 9),
            create("UG", "Uganda", "+256", 9, 9),
            create("TZ", "Tanzania", "+255", 9, 9),
            create("RW", "Rwanda", "+250", 9, 9),
            create("ET", "Ethiopia", "+251", 9, 9),
            create("GH", "Ghana", "+233", 9, 9),
            create("IN", "India", "+91", 10, 10),
            create("AU", "Australia", "+61", 9, 9),
            create("DE", "Germany", "+49", 10, 11),
            create("FR", "France", "+33", 9, 9),
            create("IT", "Italy", "+39", 9, 10),
            create("ES", "Spain", "+34", 9, 9),
            create("NL", "Netherlands", "+31", 9, 9),
            create("SE", "Sweden", "+46", 9, 9),
            create("NO", "Norway", "+47", 8, 8),
            create("DK", "Denmark", "+45", 8, 8),
            create("FI", "Finland", "+358", 9, 10),
            create("IE", "Ireland", "+353", 9, 9),
            create("CH", "Switzerland", "+41", 9, 9),
            create("AT", "Austria", "+43", 10, 11),
            create("BE", "Belgium", "+32", 9, 9),
            create("PT", "Portugal", "+351", 9, 9),
            create("PL", "Poland", "+48", 9, 9),
            create("CZ", "Czechia", "+420", 9, 9),
            create("GR", "Greece", "+30", 10, 10),
            create("TR", "Turkey", "+90", 10, 10),
            create("AE", "United Arab Emirates", "+971", 9, 9),
            create("SA", "Saudi Arabia", "+966", 9, 9),
            create("QA", "Qatar", "+974", 8, 8),
            create("KW", "Kuwait", "+965", 8, 8),
            create("OM", "Oman", "+968", 8, 8),
            create("BH", "Bahrain", "+973", 8, 8),
            create("IL", "Israel", "+972", 9, 9),
            create("JO", "Jordan", "+962", 9, 9),
            create("LB", "Lebanon", "+961", 7, 8),
            create("EG", "Egypt", "+20", 10, 10),
            create("MA", "Morocco", "+212", 9, 9),
            create("DZ", "Algeria", "+213", 9, 9),
            create("TN", "Tunisia", "+216", 8, 8),
            create("SN", "Senegal", "+221", 9, 9),
            create("CI", "Côte d'Ivoire", "+225", 10, 10),
            create("CM", "Cameroon", "+237", 9, 9),
            create("ZM", "Zambia", "+260", 9, 9),
            create("ZW", "Zimbabwe", "+263", 9, 9),
            create("MW", "Malawi", "+265", 9, 9),
            create("BW", "Botswana", "+267", 8, 8),
            create("NA", "Namibia", "+264", 9, 9),
            create("MZ", "Mozambique", "+258", 9, 9),
            create("AO", "Angola", "+244", 9, 9),
            create("CD", "DR Congo", "+243", 9, 9),
            create("SD", "Sudan", "+249", 9, 9),
            create("SS", "South Sudan", "+211", 9, 9),
            create("SO", "Somalia", "+252", 8, 9),
            create("DJ", "Djibouti", "+253", 8, 8),
            create("BI", "Burundi", "+257", 8, 8),
            create("MU", "Mauritius", "+230", 8, 8),
            create("SC", "Seychelles", "+248", 7, 7),
            create("MG", "Madagascar", "+261", 9, 9),
            create("CN", "China", "+86", 11, 11),
            create("JP", "Japan", "+81", 10, 10),
            create("KR", "South Korea", "+82", 9, 10),
            create("SG", "Singapore", "+65", 8, 8),
            create("MY", "Malaysia", "+60", 9, 10),
            create("ID", "Indonesia", "+62", 9, 12),
            create("PH", "Philippines", "+63", 10, 10),
            create("TH", "Thailand", "+66", 9, 9),
            create("VN", "Vietnam", "+84", 9, 10),
            create("PK", "Pakistan", "+92", 10, 10),
            create("BD", "Bangladesh", "+880", 10, 10),
            create("LK", "Sri Lanka", "+94", 9, 9),
            create("NP", "Nepal", "+977", 10, 10),
            create("NZ", "New Zealand", "+64", 8, 10),
            create("BR", "Brazil", "+55", 10, 11),
            create("MX", "Mexico", "+52", 10, 10),
            create("AR", "Argentina", "+54", 10, 10),
            create("CL", "Chile", "+56", 9, 9),
            create("CO", "Colombia", "+57", 10, 10),
            create("PE", "Peru", "+51", 9, 9),
            create("VE", "Venezuela", "+58", 10, 10),
            create("UY", "Uruguay", "+598", 8, 9),
            create("EC", "Ecuador", "+593", 9, 9),
            create("BO", "Bolivia", "+591", 8, 8),
            create("PY", "Paraguay", "+595", 9, 9),
            create("CR", "Costa Rica", "+506", 8, 8),
            create("PA", "Panama", "+507", 8, 8),
            create("JM", "Jamaica", "+1876", 7, 7),
            create("TT", "Trinidad & Tobago", "+1868", 7, 7),
            create("DO", "Dominican Republic", "+1809", 7, 7),
            create("PR", "Puerto Rico", "+1787", 7, 7),
            create("RU", "Russia", "+7", 10, 10),
            create("UA", "Ukraine", "+380", 9, 9),
            create("RO", "Romania", "+40", 9, 9),
            create("HU", "Hungary", "+36", 9, 9),
            create("BG", "Bulgaria", "+359", 9, 9),
            create("RS", "Serbia", "+381", 8, 9),
            create("HR", "Croatia", "+385", 8, 9),
            create("SI", "Slovenia", "+386", 8, 8),
            create("SK", "Slovakia", "+421", 9, 9),
            create("LT", "Lithuania", "+370", 8, 8),
            create("LV", "Latvia", "+371", 8, 8),
            create("EE", "Estonia", "+372", 7, 8),
            create("IS", "Iceland", "+354", 7, 7),
            create("LU", "Luxembourg", "+352", 9, 9),
            create("MT", "Malta", "+356", 8, 8),
            create("CY", "Cyprus", "+357", 8, 8),
            create("HK", "Hong Kong", "+852", 8, 8),
            create("TW", "Taiwan", "+886", 9, 9),
            create("KH", "Cambodia", "+855", 8, 9),
            create("LA", "Laos", "+856", 8, 10),
            create("MM", "Myanmar", "+95", 8, 10),
            create("MN", "Mongolia", "+976", 8, 8),
            create("KZ", "Kazakhstan", "+7", 10, 10),
            create("UZ", "Uzbekistan", "+998", 9, 9),
            create("GE", "Georgia", "+995", 9, 9),
            create("AM", "Armenia", "+374", 8, 8),
            create("AZ", "Azerbaijan", "+994", 9, 9),
            create("AF", "Afghanistan", "+93", 9, 9),
            create("IQ", "Iraq", "+964", 10, 10),
            create("IR", "Iran", "+98", 10, 10),
            create("SY", "Syria", "+963", 9, 9),
            create("YE", "Yemen", "+967", 9, 9),
            create("PS", "Palestine", "+970", 9, 9),
            create("LY", "Libya", "+218", 9, 9),
            create("MR", "Mauritania", "+222", 8, 8),
            create("ML", "Mali", "+223", 8, 8),
            create("BF", "Burkina Faso", "+226", 8, 8),
            create("NE", "Niger", "+227", 8, 8),
            create("TG", "Togo", "+228", 8, 8),
            create("BJ", "Benin", "+229", 8, 8),
            create("SL", "Sierra Leone", "+232", 8, 8),
            create("LR", "Liberia", "+231", 7, 9),
            create("GN", "Guinea", "+224", 9, 9),
            create("GM", "Gambia", "+220", 7, 7),
            create("GA", "Gabon", "+241", 7, 8),
            create("CG", "Republic of the Congo", "+242", 9, 9),
            create("TD", "Chad", "+235", 8, 8),
            create("CF", "Central African Republic", "+236", 8, 8),
            create("ER", "Eritrea", "+291", 7, 7),
            create("FJ", "Fiji", "+679", 7, 7),
            create("PG", "Papua New Guinea", "+675", 7, 8)
        )
    }

    val defaultCountry: TelefamCountry
        get() = allCountries.first() // Kenya (+254) matching Reference Image 2 flag

    private fun create(
        iso: String,
        name: String,
        dialCode: String,
        minDigits: Int = 7,
        maxDigits: Int = 12
    ): TelefamCountry {
        return TelefamCountry(
            isoCode = iso,
            name = name,
            dialCode = dialCode,
            flagEmoji = flagEmojiFromIso(iso),
            minDigits = minDigits,
            maxDigits = maxDigits
        )
    }

    fun validatePhoneDigits(country: TelefamCountry, rawInput: String): String? {
        val digitsOnly = rawInput.filter { it.isDigit() }
        if (digitsOnly.isEmpty()) {
            return "Please enter your phone number"
        }
        val cleanDigits = if (digitsOnly.startsWith("0") && digitsOnly.length > country.minDigits) {
            digitsOnly.drop(1)
        } else {
            digitsOnly
        }
        if (cleanDigits.length < country.minDigits || cleanDigits.length > country.maxDigits) {
            val expected = if (country.minDigits == country.maxDigits) {
                "${country.minDigits} digits"
            } else {
                "${country.minDigits}–${country.maxDigits} digits"
            }
            return "Invalid ${country.name} number (${country.dialCode} requires $expected)"
        }
        return null
    }
}
