package com.cognitivekernel.androidfit;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class JalaliDateTest {
 @Test public void convertsNowruz2024(){assertEquals("1403/01/01",JalaliDate.fromGregorian(2024,3,20));}
 @Test public void convertsNowruz2025(){assertEquals("1404/01/01",JalaliDate.fromGregorian(2025,3,21));}
 @Test public void convertsLeapYearBoundary(){assertEquals("1399/12/11",JalaliDate.fromGregorian(2021,3,1));}
 @Test public void convertsEndOfGregorianYear(){assertEquals("1404/10/10",JalaliDate.fromGregorian(2025,12,31));}
 @Test public void keepsZeroPaddedFields(){assertEquals("1405/07/21",JalaliDate.fromGregorian(2026,10,13));}
 @Test public void rejectsInvalidMonth(){try{JalaliDate.fromGregorian(2024,13,1);fail("Expected invalid month");}catch(IllegalArgumentException expected){}}
 @Test public void rejectsInvalidDay(){try{JalaliDate.fromGregorian(2024,2,30);fail("Expected invalid day");}catch(IllegalArgumentException expected){}}
 @Test public void rejectsUnsupportedYear(){try{JalaliDate.fromGregorian(1500,1,1);fail("Expected unsupported year");}catch(IllegalArgumentException expected){}}
}
