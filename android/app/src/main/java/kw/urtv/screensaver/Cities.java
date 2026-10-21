package kw.urtv.screensaver;
/** Kuwait City default; other locations can be found using the Kuwait-only place search. */
final class Cities {
    static final class Region {
        final String name; final String[][] cities;
        Region(String name, String[][] cities) { this.name=name; this.cities=cities; }
    }
    static final Region[] REGIONS = {new Region("الكويت", new String[][]{{"مدينة الكويت", "29.3759", "47.9774"}})};
    private Cities() {}
}
