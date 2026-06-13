package com.asc.gymgenie.nutrition

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FoodProductMacrosTest {

    private fun product(
        caloriesPer100g: Double = 200.0,
        proteinPer100g: Double = 25.0,
        fatPer100g: Double = 10.0,
        carbsPer100g: Double = 5.0,
        fiberPer100g: Double? = 2.0,
        sugarPer100g: Double? = 1.0,
    ) = FoodProduct(
        id = "test-1",
        nameRu = "Тест",
        nameEn = "Test",
        category = FoodCategory.MEAT,
        emoji = null,
        caloriesPer100g = caloriesPer100g,
        proteinPer100g = proteinPer100g,
        fatPer100g = fatPer100g,
        carbsPer100g = carbsPer100g,
        fiberPer100g = fiberPer100g,
        sugarPer100g = sugarPer100g,
    )

    // --- macrosForGrams ---

    @Test
    fun macrosForGrams_100g_returnsSameValues() {
        val p = product()
        val macros = p.macrosForGrams(100.0)
        assertEquals(200.0, macros.calories, 0.001)
        assertEquals(25.0, macros.proteinG, 0.001)
        assertEquals(10.0, macros.fatG, 0.001)
        assertEquals(5.0, macros.carbsG, 0.001)
        assertEquals(2.0, macros.fiberG!!, 0.001)
        assertEquals(1.0, macros.sugarG!!, 0.001)
    }

    @Test
    fun macrosForGrams_200g_doublesValues() {
        val p = product()
        val macros = p.macrosForGrams(200.0)
        assertEquals(400.0, macros.calories, 0.001)
        assertEquals(50.0, macros.proteinG, 0.001)
        assertEquals(20.0, macros.fatG, 0.001)
        assertEquals(10.0, macros.carbsG, 0.001)
        assertEquals(4.0, macros.fiberG!!, 0.001)
        assertEquals(2.0, macros.sugarG!!, 0.001)
    }

    @Test
    fun macrosForGrams_50g_halvesValues() {
        val p = product()
        val macros = p.macrosForGrams(50.0)
        assertEquals(100.0, macros.calories, 0.001)
        assertEquals(12.5, macros.proteinG, 0.001)
        assertEquals(5.0, macros.fatG, 0.001)
        assertEquals(2.5, macros.carbsG, 0.001)
        assertEquals(1.0, macros.fiberG!!, 0.001)
        assertEquals(0.5, macros.sugarG!!, 0.001)
    }

    @Test
    fun macrosForGrams_0g_returnsZeros() {
        val p = product()
        val macros = p.macrosForGrams(0.0)
        assertEquals(0.0, macros.calories, 0.001)
        assertEquals(0.0, macros.proteinG, 0.001)
        assertEquals(0.0, macros.fatG, 0.001)
        assertEquals(0.0, macros.carbsG, 0.001)
        assertEquals(0.0, macros.fiberG!!, 0.001)
        assertEquals(0.0, macros.sugarG!!, 0.001)
    }

    @Test
    fun macrosForGrams_nullFiberAndSugar_returnsNulls() {
        val p = product(fiberPer100g = null, sugarPer100g = null)
        val macros = p.macrosForGrams(100.0)
        assertNull(macros.fiberG)
        assertNull(macros.sugarG)
        // Non-nullable fields should still calculate correctly
        assertEquals(200.0, macros.calories, 0.001)
        assertEquals(25.0, macros.proteinG, 0.001)
    }

    @Test
    fun macrosForGrams_decimalGrams() {
        val p = product()
        val macros = p.macrosForGrams(75.0)
        assertEquals(150.0, macros.calories, 0.001)
        assertEquals(18.75, macros.proteinG, 0.001)
        assertEquals(7.5, macros.fatG, 0.001)
        assertEquals(3.75, macros.carbsG, 0.001)
        assertEquals(1.5, macros.fiberG!!, 0.001)
        assertEquals(0.75, macros.sugarG!!, 0.001)
    }

    // --- FoodCategory.fromKeyOrOther ---

    @Test
    fun fromKeyOrOther_validKey_returnsCategory() {
        assertEquals(FoodCategory.MEAT, FoodCategory.fromKeyOrOther("MEAT"))
    }

    @Test
    fun fromKeyOrOther_allValidKeys() {
        FoodCategory.entries.forEach { category ->
            assertEquals(category, FoodCategory.fromKeyOrOther(category.name))
        }
    }

    @Test
    fun fromKeyOrOther_invalidKey_returnsOther() {
        assertEquals(FoodCategory.OTHER, FoodCategory.fromKeyOrOther("UNKNOWN"))
    }

    @Test
    fun fromKeyOrOther_emptyKey_returnsOther() {
        assertEquals(FoodCategory.OTHER, FoodCategory.fromKeyOrOther(""))
    }

    @Test
    fun fromKeyOrOther_lowercaseKey_returnsOther() {
        // enum names are uppercase; lowercase should not match
        assertEquals(FoodCategory.OTHER, FoodCategory.fromKeyOrOther("meat"))
    }
}
