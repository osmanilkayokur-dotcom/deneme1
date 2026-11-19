package dev.kok.app.domain.engine

import dev.kok.app.domain.engine.StatsMetaphorMapper.Period
import org.junit.Assert.assertEquals
import org.junit.Test

class StatsMetaphorMapperTest {
    private val mapper = StatsMetaphorMapper()

    @Test
    fun `yemek agirligi en buyuk uygun birimi secer`() {
        val m = mapper.foodWeight(112.0, Period.MONTH)
        assertEquals("el arabası", m.unit)
        assertEquals("Bu ay 1,4 el arabası kadar yemek yedin.", m.headline)
    }

    @Test
    fun `kucuk degerler yuzde olarak ifade edilir`() {
        val m = mapper.water(11.4, Period.WEEK)
        assertEquals("Bu hafta bir damacananın %60'ı kadar su içtin.", m.headline)
    }

    @Test
    fun `su haftalik damacana`() {
        val m = mapper.water(28.5, Period.WEEK)
        assertEquals("Bu hafta 1,5 damacana kadar su içtin.", m.headline)
    }

    @Test
    fun `kalori kosu mesafesine cevrilir`() {
        val m = mapper.burnedEnergy(4000.0, 80.0, Period.WEEK)
        assertEquals(50.0, m.amount, 0.001)
        assertEquals("Bu hafta yaktığın enerjiyle yaklaşık 50 km koşabilirdin.", m.headline)
    }

    @Test
    fun `turkce iyelik eki unlu uyumu`() {
        assertEquals("ı", TurkishSuffix.possessive(60))
        assertEquals("i", TurkishSuffix.possessive(25))
        assertEquals("sı", TurkishSuffix.possessive(6))
        assertEquals("ü", TurkishSuffix.possessive(100))
        assertEquals("u", TurkishSuffix.possessive(30))
        assertEquals("si", TurkishSuffix.possessive(2))
        assertEquals("ü", TurkishSuffix.possessive(4))
    }
}
