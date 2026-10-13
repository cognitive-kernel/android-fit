package com.cognitivekernel.androidfit;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
public class JalaliDateTest {
 @Test public void convertsNowruz2024(){assertEquals("1403/01/01",JalaliDate.fromGregorian(2024,3,20));}
 @Test public void convertsNowruz2025(){assertEquals("1404/01/01",JalaliDate.fromGregorian(2025,3,21));}
 @Test public void convertsLeapDay(){assertEquals("1399/12/11",JalaliDate.fromGregorian(2021,3,1));}
 @Test public void convertsEndOfGregorianYear(){assertEquals("1404/10/10",JalaliDate.fromGregorian(2025,12,31));}
 @Test public void keepsZeroPaddedFields(){assertEquals("1405/07/21",JalaliDate.fromGregorian(2026,10,13));}
}
