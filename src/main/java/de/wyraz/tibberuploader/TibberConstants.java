package de.wyraz.tibberuploader;

import java.time.ZoneId;
import java.util.TimeZone;

public class TibberConstants {
	public static final ZoneId TIMEZONE_ID=ZoneId.of("Europe/Berlin");
	
	public static final TimeZone TIMEZONE=TimeZone.getTimeZone(TIMEZONE_ID);
}
