package com.dess.institution.validation;

import com.dess.institution.domain.AttributeType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttributeValueConverterTest {

    @Test
    void numberAcceptsIntegersOnly() {
        assertEquals(500L, AttributeValueConverter.convert(AttributeType.NUMBER, "500"));
        assertEquals(-3L, AttributeValueConverter.convert(AttributeType.NUMBER, "-3"));
        assertFalse(AttributeValueConverter.isValid(AttributeType.NUMBER, "Bonjour"));
        assertFalse(AttributeValueConverter.isValid(AttributeType.NUMBER, "12.5"));
        assertFalse(AttributeValueConverter.isValid(AttributeType.NUMBER, " 12"));
        assertFalse(AttributeValueConverter.isValid(AttributeType.NUMBER, ""));
    }

    @Test
    void decimalAcceptsDecimalNumbers() {
        assertEquals(new BigDecimal("12.50"), AttributeValueConverter.convert(AttributeType.DECIMAL, "12.50"));
        assertEquals(new BigDecimal("7"), AttributeValueConverter.convert(AttributeType.DECIMAL, "7"));
        assertFalse(AttributeValueConverter.isValid(AttributeType.DECIMAL, "NaN"));
        assertFalse(AttributeValueConverter.isValid(AttributeType.DECIMAL, "douze"));
    }

    @Test
    void booleanAcceptsOnlyTrueOrFalse() {
        assertEquals(Boolean.TRUE, AttributeValueConverter.convert(AttributeType.BOOLEAN, "true"));
        assertEquals(Boolean.FALSE, AttributeValueConverter.convert(AttributeType.BOOLEAN, "false"));
        assertFalse(AttributeValueConverter.isValid(AttributeType.BOOLEAN, "yes"));
        assertFalse(AttributeValueConverter.isValid(AttributeType.BOOLEAN, "True"));
        assertFalse(AttributeValueConverter.isValid(AttributeType.BOOLEAN, "1"));
    }

    @Test
    void dateMustBeIsoAndReal() {
        assertEquals(LocalDate.of(2020, 1, 15), AttributeValueConverter.convert(AttributeType.DATE, "2020-01-15"));
        assertFalse(AttributeValueConverter.isValid(AttributeType.DATE, "15/01/2020"));
        assertFalse(AttributeValueConverter.isValid(AttributeType.DATE, "2020-02-30"));
        assertFalse(AttributeValueConverter.isValid(AttributeType.DATE, "2020-1-5"));
    }

    @Test
    void emailMustLookLikeAnAddress() {
        assertTrue(AttributeValueConverter.isValid(AttributeType.EMAIL, "informatique@universite.mg"));
        assertFalse(AttributeValueConverter.isValid(AttributeType.EMAIL, "informatique"));
        assertFalse(AttributeValueConverter.isValid(AttributeType.EMAIL, "a@b"));
        assertFalse(AttributeValueConverter.isValid(AttributeType.EMAIL, "a b@c.mg"));
    }

    @Test
    void urlMustBeAbsoluteHttp() {
        assertTrue(AttributeValueConverter.isValid(AttributeType.URL, "https://lycee-y.example.mg"));
        assertTrue(AttributeValueConverter.isValid(AttributeType.URL, "http://exemple.mg/page?x=1"));
        assertFalse(AttributeValueConverter.isValid(AttributeType.URL, "ftp://exemple.mg"));
        assertFalse(AttributeValueConverter.isValid(AttributeType.URL, "exemple.mg"));
        assertFalse(AttributeValueConverter.isValid(AttributeType.URL, "http://"));
    }

    @Test
    void textAcceptsAnything() {
        assertTrue(AttributeValueConverter.isValid(AttributeType.TEXT, "Rakoto Jean"));
        assertTrue(AttributeValueConverter.isValid(AttributeType.TEXT, ""));
    }

    @Test
    void convertRejectsIncompatibleValueWithReadableMessage() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> AttributeValueConverter.convert(AttributeType.NUMBER, "Bonjour"));
        assertTrue(e.getMessage().contains("Bonjour"));
    }
}
