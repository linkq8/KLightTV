package kw.urtv.screensaver;
final class Cities {
    static final class Region {final String name;final String[][] cities;Region(String n,String[][] c){name=n;cities=c;}}
    static final Region[] REGIONS;
    static {kw.urtv.shared.RegionCatalog.Region[] source=kw.urtv.shared.RegionCatalog.ALL;String[][] cities=new String[source.length][3];for(int i=0;i<source.length;i++)cities[i]=new String[]{source[i].name,""+source[i].lat,""+source[i].lon};REGIONS=new Region[]{new Region("مناطق الكويت",cities)};}
    private Cities(){}
}
