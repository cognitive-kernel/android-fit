package com.cognitivekernel.androidfit;

import java.util.Locale;

public final class JalaliDate {
    private JalaliDate() {}

    public static String fromGregorian(int gy, int gm, int gd) {
        if (gy < 1600 || gm < 1 || gm > 12) {
            throw new IllegalArgumentException("Gregorian date is out of supported range");
        }
        int[] monthDays = {31, isLeapYear(gy) ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
        if (gd < 1 || gd > monthDays[gm - 1]) {
            throw new IllegalArgumentException("Invalid Gregorian day");
        }

        int[] gdm = {0, 31, isLeapYear(gy) ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
        int gy2 = gy - 1600, gm2 = gm - 1, gd2 = gd - 1;
        long days = 365L * gy2 + (gy2 + 3) / 4 - (gy2 + 99) / 100 + (gy2 + 399) / 400;
        for (int i = 0; i < gm2; i++) days += gdm[i + 1];
        days += gd2;

        long jdn = days - 79, jnp = jdn / 12053;
        jdn %= 12053;
        int jy = (int) (979 + 33 * jnp + 4 * (jdn / 1461));
        jdn %= 1461;
        if (jdn >= 366) {
            jy += (int) ((jdn - 1) / 365);
            jdn = (jdn - 1) % 365;
        }
        int jm, jd;
        if (jdn < 186) {
            jm = 1 + (int) (jdn / 31);
            jd = 1 + (int) (jdn % 31);
        } else {
            jm = 7 + (int) ((jdn - 186) / 30);
            jd = 1 + (int) ((jdn - 186) % 30);
        }
        return String.format(Locale.US, "%04d/%02d/%02d", jy, jm, jd);
    }

    private static boolean isLeapYear(int year) {
        return year % 4 == 0 && (year % 100 != 0 || year % 400 == 0);
    }
}
