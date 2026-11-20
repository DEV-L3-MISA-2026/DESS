package com.dess.institution.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @brief Tests for the data types supported by custom attributes.
 */
class AttributeTypeTest
{
    @Test
    void numberAcceptsIntegersOnly()
    {
        assertEquals(500L, AttributeType.NUMBER.convert("500"));
        assertEquals(-3L, AttributeType.NUMBER.convert("-3"));
        assertFalse(AttributeType.NUMBER.isValid("Bonjour"));
        assertFalse(AttributeType.NUMBER.isValid("12.5"));
        assertFalse(AttributeType.NUMBER.isValid(" 12"));
        assertFalse(AttributeType.NUMBER.isValid(""));
    }

    @Test
    void decimalAcceptsDecimalNumbers()
    {
        assertEquals(new BigDecimal("12.50"), AttributeType.DECIMAL.convert("12.50"));
        assertEquals(new BigDecimal("7"), AttributeType.DECIMAL.convert("7"));
        assertFalse(AttributeType.DECIMAL.isValid("NaN"));
        assertFalse(AttributeType.DECIMAL.isValid("douze"));
    }

    @Test
    void booleanAcceptsOnlyTrueOrFalse()
    {
        assertEquals(Boolean.TRUE, AttributeType.BOOLEAN.convert("true"));
        assertEquals(Boolean.FALSE, AttributeType.BOOLEAN.convert("false"));
        assertFalse(AttributeType.BOOLEAN.isValid("yes"));
        assertFalse(AttributeType.BOOLEAN.isValid("True"));
        assertFalse(AttributeType.BOOLEAN.isValid("1"));
    }

    @Test
    void dateMustBeIsoAndReal()
    {
        assertEquals(LocalDate.of(2020, 1, 15), AttributeType.DATE.convert("2020-01-15"));
        assertFalse(AttributeType.DATE.isValid("15/01/2020"));
        assertFalse(AttributeType.DATE.isValid("2020-02-30"));
        assertFalse(AttributeType.DATE.isValid("2020-1-5"));
    }

    @Test
    void emailMustLookLikeAnAddress()
    {
        assertTrue(AttributeType.EMAIL.isValid("informatique@universite.mg"));
        assertFalse(AttributeType.EMAIL.isValid("informatique"));
        assertFalse(AttributeType.EMAIL.isValid("a@b"));
        assertFalse(AttributeType.EMAIL.isValid("a b@c.mg"));
    }

    @Test
    void urlMustBeAbsoluteHttp()
    {
        assertTrue(AttributeType.URL.isValid("https://lycee-y.example.mg"));
        assertTrue(AttributeType.URL.isValid("http://example.mg/page?x=1"));
        assertFalse(AttributeType.URL.isValid("ftp://example.mg"));
        assertFalse(AttributeType.URL.isValid("example.mg"));
        assertFalse(AttributeType.URL.isValid("http://"));
    }

    @Test
    void textAcceptsAnything()
    {
        assertTrue(AttributeType.TEXT.isValid("Rakoto Jean"));
        assertTrue(AttributeType.TEXT.isValid(""));
    }

    @Test
    void convertRejectsIncompatibleValueWithReadableMessage()
    {
        IllegalArgumentException e = assertThrows(
            IllegalArgumentException.class,
            () -> AttributeType.NUMBER.convert("Bonjour")
        );

        assertTrue(e.getMessage().contains("Bonjour"));
    }
}
