package com.company.workflow.location;

import java.util.Locale;
import java.util.Set;

/**
 * Maps a Location country value to the regional reviewer candidate group.
 */
public final class RegionalReviewerRouting {

    static final String GROUP_US = "ROLE_REVIEWER_US";
    static final String GROUP_EU = "ROLE_REVIEWER_EU";
    static final String GROUP_APAC = "ROLE_REVIEWER_APAC";

    static final String VARIABLE_REGIONAL_GROUP = "regionalReviewerGroup";

    private static final Set<String> US_IDENTIFIERS = Set.of(
            "US",
            "USA",
            "UNITED STATES",
            "UNITED STATES OF AMERICA");

    /** EU member states (ISO 3166-1 alpha-2 and common English names). */
    private static final Set<String> EU_IDENTIFIERS = Set.of(
            "AT", "AUSTRIA",
            "BE", "BELGIUM",
            "BG", "BULGARIA",
            "HR", "CROATIA",
            "CY", "CYPRUS",
            "CZ", "CZECHIA", "CZECH REPUBLIC",
            "DK", "DENMARK",
            "EE", "ESTONIA",
            "FI", "FINLAND",
            "FR", "FRANCE",
            "DE", "GERMANY",
            "GR", "GREECE",
            "HU", "HUNGARY",
            "IE", "IRELAND",
            "IT", "ITALY",
            "LV", "LATVIA",
            "LT", "LITHUANIA",
            "LU", "LUXEMBOURG",
            "MT", "MALTA",
            "NL", "NETHERLANDS", "THE NETHERLANDS",
            "PL", "POLAND",
            "PT", "PORTUGAL",
            "RO", "ROMANIA",
            "SK", "SLOVAKIA", "SLOVAK REPUBLIC",
            "SI", "SLOVENIA",
            "ES", "SPAIN",
            "SE", "SWEDEN");

    private RegionalReviewerRouting() {
    }

    static String reviewerGroupForCountry(String country) {
        if (country == null || country.isBlank()) {
            return GROUP_APAC;
        }
        String normalized = country.trim().toUpperCase(Locale.ROOT);
        if (US_IDENTIFIERS.contains(normalized)) {
            return GROUP_US;
        }
        if (EU_IDENTIFIERS.contains(normalized)) {
            return GROUP_EU;
        }
        return GROUP_APAC;
    }
}
