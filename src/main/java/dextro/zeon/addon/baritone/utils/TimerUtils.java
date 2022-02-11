package dextro.zeon.addon.baritone.utils;

public class TimerUtils {
	public long ms;
	
	public TimerUtils() {
		this.ms = 0;
	}
	
	public boolean hasPassed(int ms) {
		return System.currentTimeMillis() - this.ms >= ms;
	}
	
	public void reset() {
		this.ms = System.currentTimeMillis();
	}
}