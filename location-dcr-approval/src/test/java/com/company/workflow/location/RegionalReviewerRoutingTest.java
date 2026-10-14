package com.company.workflow.location;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RegionalReviewerRoutingTest {

    @Test
    void routesUnitedStates() {
        assertEquals(RegionalReviewerRouting.GROUP_US,
                RegionalReviewerRouting.reviewerGroupForCountry("United States"));
        assertEquals(RegionalReviewerRouting.GROUP_US,
                RegionalReviewerRouting.reviewerGroupForCountry("US"));
    }

    @Test
    void routesEuropeanCountries() {
        assertEquals(RegionalReviewerRouting.GROUP_EU,
                RegionalReviewerRouting.reviewerGroupForCountry("Germany"));
        assertEquals(RegionalReviewerRouting.GROUP_EU,
                RegionalReviewerRouting.reviewerGroupForCountry("de"));
    }

    @Test
    void defaultsToApac() {
        assertEquals(RegionalReviewerRouting.GROUP_APAC,
                RegionalReviewerRouting.reviewerGroupForCountry("Japan"));
        assertEquals(RegionalReviewerRouting.GROUP_APAC,
                RegionalReviewerRouting.reviewerGroupForCountry(null));
        assertEquals(RegionalReviewerRouting.GROUP_APAC,
                RegionalReviewerRouting.reviewerGroupForCountry("  "));
    }
}
