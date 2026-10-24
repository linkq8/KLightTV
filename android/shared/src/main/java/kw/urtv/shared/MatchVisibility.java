package kw.urtv.shared;
import java.time.Instant;
import java.util.Locale;
/** Pure display policy; no timers, network or device-dependent clock state. */
public final class MatchVisibility {
 private MatchVisibility(){}
 public static boolean visible(String state,String status,Instant kickoff,Instant now){
  String code=status.toUpperCase(Locale.ROOT);
  if(code.contains("CANCEL")||code.contains("POSTPON")||code.contains("SUSPEND")||code.contains("ABANDON"))return false;
  if("pre".equals(state))return !kickoff.isBefore(now.minusSeconds(3600))&&!kickoff.isAfter(now.plusSeconds(72*3600));
  if("in".equals(state))return !kickoff.isBefore(now.minusSeconds(6*3600))&&!kickoff.isAfter(now);
  return "post".equals(state)&&!kickoff.isBefore(now.minusSeconds(24*3600))&&!kickoff.isAfter(now);
 }
}
