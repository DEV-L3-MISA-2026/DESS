package com.dess.institution.validation;

import com.dess.institution.domain.AttributeType;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Service central de conversion et de validation des valeurs d'attributs.
 * <p>
 * Toutes les valeurs sont stockées sous forme de texte ; ce convertisseur est le seul endroit qui
 * sait les interpréter selon leur {@link AttributeType}.
 */
public final class AttributeValueConverter {

    private static final Pattern EMAIL =
            Pattern.compile("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9\\-]+(\\.[A-Za-z0-9\\-]+)+$");

    private AttributeValueConverter() {
    }

    /**
     * Convertit la valeur sérialisée en objet Java typé : {@code String} (TEXT, EMAIL, URL),
     * {@code Long} (NUMBER), {@code BigDecimal} (DECIMAL), {@code Boolean} (BOOLEAN),
     * {@code LocalDate} (DATE).
     *
     * @throws IllegalArgumentException si la valeur est incompatible avec le type
     */
    public static Object convert(AttributeType type, String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("valeur absente");
        }
        try {
            return switch (type) {
                case TEXT -> raw;
                case NUMBER -> Long.parseLong(raw);
                case DECIMAL -> new BigDecimal(raw);
                case BOOLEAN -> parseBoolean(raw);
                case DATE -> LocalDate.parse(raw);
                case EMAIL -> parseEmail(raw);
                case URL -> parseUrl(raw);
            };
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("« " + raw + " » n'est pas un nombre valide pour le type " + type);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("« " + raw + " » n'est pas une date ISO yyyy-MM-dd valide");
        }
    }

    /** Retourne le message d'erreur si la valeur est invalide pour ce type, sinon vide. */
    public static Optional<String> check(AttributeType type, String raw) {
        if (raw == null) {
            return Optional.of("valeur absente");
        }
        if (type != AttributeType.TEXT && raw.isBlank()) {
            return Optional.of("valeur vide pour le type " + type);
        }
        try {
            convert(type, raw);
            return Optional.empty();
        } catch (IllegalArgumentException e) {
            return Optional.of(e.getMessage());
        }
    }

    public static boolean isValid(AttributeType type, String raw) {
        return check(type, raw).isEmpty();
    }

    private static Boolean parseBoolean(String raw) {
        if (raw.equals("true")) {
            return Boolean.TRUE;
        }
        if (raw.equals("false")) {
            return Boolean.FALSE;
        }
        throw new IllegalArgumentException("« " + raw + " » n'est pas un booléen (attendu : true ou false)");
    }

    private static String parseEmail(String raw) {
        if (raw.length() > 254 || !EMAIL.matcher(raw).matches()) {
            throw new IllegalArgumentException("« " + raw + " » n'est pas une adresse e-mail valide");
        }
        return raw;
    }

    private static String parseUrl(String raw) {
        try {
            URI uri = new URI(raw);
            String scheme = uri.getScheme();
            boolean httpScheme = scheme != null
                    && (scheme.toLowerCase(Locale.ROOT).equals("http") || scheme.toLowerCase(Locale.ROOT).equals("https"));
            if (!httpScheme || uri.getHost() == null) {
                throw new IllegalArgumentException("« " + raw + " » n'est pas une URL http(s) absolue valide");
            }
            return raw;
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("« " + raw + " » n'est pas une URL valide");
        }
    }
}
