package dev.kok.app.domain.engine

import dev.kok.app.domain.model.ActivityLevel
import dev.kok.app.domain.model.Sex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MetabolismCalculatorTest {
    private val calc = MetabolismCalculator()

    @Test
    fun `mifflin st jeor erkek`() {
        // 10*80 + 6.25*180 - 5*30 + 5 = 800 + 1125 - 150 + 5 = 1780
        assertEquals(1780.0, calc.bmr(80.0, 180.0, 30, Sex.MALE), 0.001)
    }

    @Test
    fun `mifflin st jeor kadin`() {
        // 10*60 + 6.25*165 - 5*28 - 161 = 600 + 1031.25 - 140 - 161 = 1330.25
        assertEquals(1330.25, calc.bmr(60.0, 165.0, 28, Sex.FEMALE), 0.001)
    }

    @Test
    fun `tdee aktivite carpanini uygular`() {
        assertEquals(1780.0 * 1.55, calc.tdee(1780.0, ActivityLevel.MODERATE), 0.001)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `gecersiz girdi reddedilir`() {
        calc.bmr(0.0, 180.0, 30, Sex.MALE)
    }

    @Test
    fun `hedef makrolar enerji dengesini korur`() {
        val goals = calc.recommendGoals(80.0, 180.0, 30, Sex.MALE, ActivityLevel.MODERATE)
        val tdee = 1780.0 * 1.55
        val kcal = goals.proteinG * 4 + goals.carbG * 4 + goals.fatG * 9
        assertEquals(tdee, kcal, 40.0) // 5 g'lık yuvarlamalardan gelen sapma
        assertEquals(145.0, goals.proteinG, 0.001) // 80*1.8 = 144 -> 145
        assertEquals(0, goals.waterMl % 250)
        assertTrue(goals.fiberG >= 25.0)
    }

    @Test
    fun `antrenman kalorisi met formulu`() {
        // 5 MET * 80 kg * 1 saat = 400
        assertEquals(400, calc.workoutKcal(80.0, 60))
        assertEquals(0, calc.workoutKcal(80.0, 0))
    }
}
