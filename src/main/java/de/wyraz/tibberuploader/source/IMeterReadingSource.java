package de.wyraz.tibberuploader.source;

import java.io.IOException;
import java.time.LocalDate;
import java.util.NavigableMap;

public interface IMeterReadingSource {
	
	public NavigableMap<LocalDate,Integer> findDailyReadings(String meterNumber, LocalDate firstDay, LocalDate lastDay) throws IOException;

}
