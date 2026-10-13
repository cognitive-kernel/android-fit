package com.cognitivekernel.androidfit;
import org.junit.Test;import static org.junit.Assert.assertEquals;
public class JalaliDateTest {@Test public void nowruz2024(){assertEquals("1403/01/01",JalaliDate.fromGregorian(2024,3,20));}@Test public void nowruz2025(){assertEquals("1404/01/01",JalaliDate.fromGregorian(2025,3,21));}}
