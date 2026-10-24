package com.cognitivekernel.androidfit;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class GoalPlannerInputTest {
    @Test public void parsesPersianAndArabicIntegerDigits() {
        assertEquals(30, GoalPlanner.parseInteger("۳۰"));
        assertEquals(48, GoalPlanner.parseInteger("٤٨"));
    }

    @Test public void parsesLocalizedDecimalSeparatorAndDigits() {
        assertEquals(170.5, GoalPlanner.parseDecimal("۱۷۰٫۵"), 0.0001);
        assertEquals(72.5, GoalPlanner.parseDecimal("٧٢٫٥"), 0.0001);
    }

    @Test public void parsesCommaDecimalAndThousandsGroupingWithoutAmbiguity() {
        assertEquals(72.5, GoalPlanner.parseDecimal("72,5"), 0.0001);
        assertEquals(1234.0, GoalPlanner.parseDecimal("1,234"), 0.0001);
        assertEquals(1234.56, GoalPlanner.parseDecimal("1.234,56"), 0.0001);
    }

    @Test public void rejectsNonFiniteAndMalformedDecimalValues() {
        try {
            GoalPlanner.parseDecimal("NaN");
            org.junit.Assert.fail("NaN must not be accepted");
        } catch (NumberFormatException expected) { }
        try {
            GoalPlanner.parseDecimal("Infinity");
            org.junit.Assert.fail("Infinity must not be accepted");
        } catch (NumberFormatException expected) { }
        try {
            GoalPlanner.parseDecimal("12,34,56");
            org.junit.Assert.fail("Malformed number must not be accepted");
        } catch (NumberFormatException expected) { }
    }

    @Test public void removesGroupingSeparators() {
        assertEquals(1234.5, GoalPlanner.parseDecimal("۱٬۲۳۴٫۵"), 0.0001);
    }
}
