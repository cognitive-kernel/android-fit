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

    @Test public void removesGroupingSeparators() {
        assertEquals(1234.5, GoalPlanner.parseDecimal("۱٬۲۳۴٫۵"), 0.0001);
    }
}
