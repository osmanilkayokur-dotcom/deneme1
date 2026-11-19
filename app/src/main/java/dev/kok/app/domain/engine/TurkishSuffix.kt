package dev.kok.app.domain.engine

/**
 * Sayılardan sonra gelen 3. tekil iyelik ekini ünlü uyumuna göre üretir:
 * %60'ı (altmış), %25'i (yirmi beş), %40'ı (kırk), %6'sı (altı), %100'ü (yüz).
 */
object TurkishSuffix {
    private val units = mapOf(
        0 to "sıfır", 1 to "bir", 2 to "iki", 3 to "üç", 4 to "dört",
        5 to "beş", 6 to "altı", 7 to "yedi", 8 to "sekiz", 9 to "dokuz",
    )
    private val tens = mapOf(
        1 to "on", 2 to "yirmi", 3 to "otuz", 4 to "kırk", 5 to "elli",
        6 to "altmış", 7 to "yetmiş", 8 to "seksen", 9 to "doksan",
    )

    /** Sayının okunuşundaki son kelime. */
    private fun lastWord(n: Int): String {
        val abs = kotlin.math.abs(n)
        return when {
            abs == 0 -> "sıfır"
            abs % 10 != 0 -> units.getValue(abs % 10)
            (abs / 10) % 10 != 0 -> tens.getValue((abs / 10) % 10)
            (abs / 100) % 10 != 0 -> "yüz"
            (abs / 1000) % 1000 != 0 -> "bin"
            else -> "milyon"
        }
    }

    fun possessive(n: Int): String {
        val word = lastWord(n)
        val lastVowel = word.last { it in VOWELS }
        val vowel = when (lastVowel) {
            'a', 'ı' -> 'ı'
            'e', 'i' -> 'i'
            'o', 'u' -> 'u'
            else -> 'ü' // ö, ü
        }
        return if (word.last() in VOWELS) "s$vowel" else "$vowel"
    }

    private const val VOWELS = "aeıioöuü"
}
