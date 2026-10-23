package com.cognitivekernel.androidfit;

import org.junit.Test;
import static org.junit.Assert.*;

/** Regression tests for localized numeric input used by workout and nutrition forms. */
public class GoalPlannerTest {
    @Test public void normalizesPersianAndArabicDigits() {
        assertEquals("123", GoalPlanner.normalizeDigits("۱۲۳"));
        assertEquals("456", GoalPlanner.normalizeDigits("٤٥٦"));
    }

    @Test public void normalizesLocalizedDecimalSeparator() {
        assertEquals("12.5", GoalPlanner.normalizeDigits("۱۲٫۵"));
        assertEquals(12.5, GoalPlanner.parseDecimal("۱۲٫۵"), 0.000001);
        assertEquals(7.25, GoalPlanner.parseDecimal("٧٫٢٥"), 0.000001);
    }

    @Test public void removesGroupingSeparatorsAndWhitespace() {
        assertEquals("1234567", GoalPlanner.normalizeDigits(" ۱٬۲۳۴٬۵۶۷ "));
        assertEquals(1234567, GoalPlanner.parseInteger("۱٬۲۳۴٬۵۶۷"));
        assertEquals(1234.5, GoalPlanner.parseDecimal("1,234.5"), 0.000001);
    }

    @Test public void acceptsAsciiNumbersWithoutChangingTheirValue() {
        assertEquals(42, GoalPlanner.parseInteger("42"));
        assertEquals(70.25, GoalPlanner.parseDecimal("70.25"), 0.000001);
    }

    @Test(expected = NumberFormatException.class)
    public void rejectsEmptyIntegerInput() {
        GoalPlanner.parseInteger("   ");
    }

    @Test(expected = NumberFormatException.class)
    public void rejectsNaNAsDecimalInput() {
        GoalPlanner.parseDecimal("NaN");
    }

    @Test(expected = NumberFormatException.class)
    public void rejectsInfiniteDecimalInput() {
        GoalPlanner.parseDecimal("Infinity");
    }

    @Test(expected = NumberFormatException.class)
    public void rejectsMalformedDecimalInput() {
        GoalPlanner.parseDecimal("۱۲٫۳٫۴");
    }
}
