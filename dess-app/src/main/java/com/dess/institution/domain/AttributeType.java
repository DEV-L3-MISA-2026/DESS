package com.dess.institution.domain;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * @brief Data types supported by custom attributes.
 *
 * <p>Each constant knows how to parse its serialized text form and how to validate it. These are
 * data types (text, number, date and so on), not organizational unit types: the latter are defined
 * dynamically by the user.</p>
 *
 * @see AttributeDefinition
 */
public enum AttributeType
{
    /**
     * Free-form text.
     */
    TEXT
    {
        @Override
        protected Object parse(String raw)
        {
            return raw;
        }
    },

    /**
     * Integer value represented by a {@link Long}.
     */
    NUMBER
    {
        @Override
        protected Object parse(String raw)
        {
            try
            {
                return Long.parseLong(raw);
            }
            catch (NumberFormatException e)
            {
                throw new IllegalArgumentException("'" + raw + "' is not a valid number for type " + this);
            }
        }
    },

    /**
     * Decimal value represented by a {@link BigDecimal}.
     */
    DECIMAL
    {
        @Override
        protected Object parse(String raw)
        {
            try
            {
                return new BigDecimal(raw);
            }
            catch (NumberFormatException e)
            {
                throw new IllegalArgumentException("'" + raw + "' is not a valid number for type " + this);
            }
        }
    },

    /**
     * Boolean value; only {@code true} and {@code false} are accepted.
     */
    BOOLEAN
    {
        @Override
        protected Object parse(String raw)
        {
            if (raw.equals("true"))
            {
                return Boolean.TRUE;
            }

            if (raw.equals("false"))
            {
                return Boolean.FALSE;
            }

            throw new IllegalArgumentException("'" + raw + "' is not a boolean (expected: true or false)");
        }
    },

    /**
     * Date in ISO {@code yyyy-MM-dd} form, represented by a {@link LocalDate}.
     */
    DATE
    {
        @Override
        protected Object parse(String raw)
        {
            try
            {
                return LocalDate.parse(raw);
            }
            catch (DateTimeParseException e)
            {
                throw new IllegalArgumentException("'" + raw + "' is not a valid ISO yyyy-MM-dd date");
            }
        }
    },

    /**
     * E-mail address.
     */
    EMAIL
    {
        @Override
        protected Object parse(String raw)
        {
            if (raw.length() > 254 || !EMAIL_PATTERN.matcher(raw).matches())
            {
                throw new IllegalArgumentException("'" + raw + "' is not a valid e-mail address");
            }

            return raw;
        }
    },

    /**
     * Absolute HTTP or HTTPS URL.
     */
    URL
    {
        @Override
        protected Object parse(String raw)
        {
            try
            {
                URI uri = new URI(raw);
                String scheme = uri.getScheme();
                boolean httpScheme = "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);

                if (!httpScheme || uri.getHost() == null)
                {
                    throw new IllegalArgumentException("'" + raw + "' is not a valid absolute http(s) URL");
                }

                return raw;
            }
            catch (URISyntaxException e)
            {
                throw new IllegalArgumentException("'" + raw + "' is not a valid URL");
            }
        }
    };

    /**
     * Pragmatic e-mail shape used for form validation.
     */
    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9\\-]+(\\.[A-Za-z0-9\\-]+)+$");

    /**
     * Converts a serialized value to its Java representation.
     *
     * @param raw serialized value
     * @return a {@code String} for {@code TEXT}, {@code EMAIL} and {@code URL}; a {@code Long} for
     *         {@code NUMBER}; a {@code BigDecimal} for {@code DECIMAL}; a {@code Boolean} for
     *         {@code BOOLEAN}; a {@code LocalDate} for {@code DATE}
     * @throws IllegalArgumentException if the value is {@code null} or incompatible with this type
     */
    public final Object convert(String raw)
    {
        if (raw == null)
        {
            throw new IllegalArgumentException("missing value");
        }

        return parse(raw);
    }

    /**
     * Validates a serialized value without throwing.
     *
     * @param raw serialized value, possibly {@code null}
     * @return an error message when the value is invalid for this type, otherwise an empty optional
     */
    public final Optional<String> check(String raw)
    {
        if (raw == null)
        {
            return Optional.of("missing value");
        }

        if (this != TEXT && raw.isBlank())
        {
            return Optional.of("empty value for type " + this);
        }

        try
        {
            convert(raw);
            return Optional.empty();
        }
        catch (IllegalArgumentException e)
        {
            return Optional.of(e.getMessage());
        }
    }

    /**
     * Indicates whether a serialized value is valid for this type.
     *
     * @param raw serialized value, possibly {@code null}
     * @return {@code true} when {@link #check(String)} reports no error
     */
    public final boolean isValid(String raw)
    {
        return check(raw).isEmpty();
    }

    /**
     * Parses a non-null serialized value.
     *
     * @param raw serialized value
     * @return the Java representation of the value
     * @throws IllegalArgumentException if the value is incompatible with this type
     */
    protected abstract Object parse(String raw);
}
