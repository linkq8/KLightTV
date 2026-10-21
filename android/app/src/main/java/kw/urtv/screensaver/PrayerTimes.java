package kw.urtv.screensaver;

import java.time.LocalDate;

/** Kuwait method-9 offline calculation, also used by the native launcher. */
final class PrayerTimes {
    static final String[] KEYS = {"Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha"};
    private final double latitude, longitude, jd;
    private PrayerTimes(LocalDate day, double lat, double lon) {
        latitude = lat; longitude = lon;
        jd = day.toEpochDay() + 2440587.5 - lon / 360.0;
    }
    private static double fix(double a, double b) { return a - b * Math.floor(a / b); }
    private double[] sun(double hour) {
        double d = jd + hour / 24 - 2451545;
        double g = Math.toRadians(fix(357.529 + .98560028 * d, 360));
        double q = fix(280.459 + .98564736 * d, 360);
        double l = Math.toRadians(fix(q + 1.915 * Math.sin(g) + .020 * Math.sin(2 * g), 360));
        double e = Math.toRadians(23.439 - .00000036 * d);
        double ra = fix(Math.toDegrees(Math.atan2(Math.cos(e) * Math.sin(l), Math.cos(l))) / 15, 24);
        return new double[]{Math.asin(Math.sin(e) * Math.sin(l)), q / 15 - ra};
    }
    private double noon(double hour) { return fix(12 - sun(hour)[1], 24); }
    private double angle(double angle, double hour, boolean before) {
        double dec = sun(hour)[0], lat = Math.toRadians(latitude);
        double t = Math.toDegrees(Math.acos((-Math.sin(Math.toRadians(angle)) - Math.sin(dec) * Math.sin(lat))
                / (Math.cos(dec) * Math.cos(lat)))) / 15;
        return noon(hour) + (before ? -t : t);
    }
    static int[] calculate(LocalDate day, double lat, double lon) {
        PrayerTimes p = new PrayerTimes(day, lat, lon);
        double asrAngle = -Math.toDegrees(Math.atan(1 / (1 + Math.tan(Math.toRadians(Math.abs(lat - Math.toDegrees(p.sun(13)[0])))))));
        double[] hours = {p.angle(18, 5, true), p.angle(.833, 6, true), p.noon(12),
                p.angle(asrAngle, 13, false), p.angle(.833, 18, false), p.angle(17.5, 18, false)};
        int[] result = new int[6];
        for (int i = 0; i < 6; i++) result[i] = (int) Math.round(fix(hours[i] + 3 - lon / 15, 24) * 60) % 1440;
        return result;
    }
}
