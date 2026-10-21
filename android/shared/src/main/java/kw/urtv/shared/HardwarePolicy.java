package kw.urtv.shared;

/** No model-name guessing, benchmark loop or background monitor. */
public final class HardwarePolicy {
    public static boolean useLite(String choice,boolean lowRam,long totalBytes,int heapMb,boolean pressure){
        if(pressure)return true;
        if("lite".equals(choice))return true;
        if("normal".equals(choice))return heapMb<96; // mandatory allocation safety
        return lowRam||totalBytes<=2304L*1048576L||heapMb<=128;
    }
    public static boolean shouldTrim(int level){return level==10||level==15||level>=40;}
    public static int imageEdge(boolean lite,int heapMb){return heapMb<96?1280:lite?1920:3840;}
    private HardwarePolicy(){}
}
