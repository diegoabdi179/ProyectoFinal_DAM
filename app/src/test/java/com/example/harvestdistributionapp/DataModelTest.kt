package com.example.harvestdistributionapp

import com.example.harvestdistributionapp.data.InputValidator
import com.example.harvestdistributionapp.data.Product
import com.example.harvestdistributionapp.data.AvailabilityStatus
import com.example.harvestdistributionapp.data.formatMoney
import com.example.harvestdistributionapp.data.futureIsoDate
import com.example.harvestdistributionapp.data.isIsoDateTodayOrFuture
import org.junit.Assert.*
import org.junit.Test

class DataModelTest {
    @Test
    fun passwordValidation_requiresLetterNumberAndEightCharacters() {
        assertNotNull(InputValidator.validatePassword("corta1"))
        assertNotNull(InputValidator.validatePassword("sololetras"))
        assertNotNull(InputValidator.validatePassword("12345678"))
        assertNull(InputValidator.validatePassword("Cosecha8"))
    }

    @Test
    fun search_isCaseAndAccentInsensitiveAcrossRelevantFields() {
        val product = Product(
            id = 1,
            producerId = "producer",
            producerName = "José Pérez",
            name = "Lechuga Orejona",
            quantity = 15,
            unit = "piezas",
            pricePerUnit = 12.0,
            status = AvailabilityStatus.LIMITED,
            location = "Metepec",
            availableDate = "2026-08-31",
            imageUri = "content://photo",
            category = "verduras"
        )

        assertTrue(InputValidator.matchesSearch(product, "JOSE"))
        assertTrue(InputValidator.matchesSearch(product, "lechuga"))
        assertTrue(InputValidator.matchesSearch(product, "MÉTEPEC"))
        assertFalse(InputValidator.matchesSearch(product, "miel"))
    }

    @Test
    fun quantityNeverDropsBelowOne() {
        assertEquals(1, InputValidator.decrementQuantity(1))
        assertEquals(1, InputValidator.decrementQuantity(5))
        assertEquals(5, InputValidator.decrementQuantity(10))
    }

    @Test
    fun moneyFormattingIsStable() {
        assertEquals("$18", formatMoney(18.0))
        assertEquals("$18.50", formatMoney(18.5))
    }

    @Test
    fun dateValidationRejectsPastAndMalformedDates() {
        assertTrue(isIsoDateTodayOrFuture(futureIsoDate(0)))
        assertTrue(isIsoDateTodayOrFuture(futureIsoDate(1)))
        assertFalse(isIsoDateTodayOrFuture(futureIsoDate(-1)))
        assertFalse(isIsoDateTodayOrFuture("fecha inválida"))
    }
}
