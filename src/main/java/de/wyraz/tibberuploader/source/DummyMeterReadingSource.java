package de.wyraz.tibberuploader.source;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;

/**
 * Source that provides static dummy data. For testing only.
 * 
 * @author mwyraz
 *
 */
public class DummyMeterReadingSource implements IMeterReadingSource {
	
	protected final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Meter number provided by that source. If configured, values are only returned if the correct meter is queried
	 */
	@Value("${readings.source.dummy.meterNumber:}")
	public String meterNumber;

	/**
	 * Date of the dummy reading
	 */
	public LocalDate readingDate;

	/**
	 * Date of the dummy reading
	 */
	@Value("${readings.source.dummy.value}")
	public Integer readingValue;
	
	@Value("${readings.source.dummy.date}")
	public void setReadingDate(String readingDate) {
		this.readingDate = LocalDate.parse(readingDate);
	}
	
	@Override
	public NavigableMap<LocalDate, Integer> findDailyReadings(String meterNumber, LocalDate firstDay, LocalDate lastDay)
			throws IOException {
		
		if (StringUtils.hasText(this.meterNumber) && !this.meterNumber.equalsIgnoreCase(meterNumber)) {
			log.warn("Wrong meter number reported by API. Expected '{}' but found '{}'",this.meterNumber,meterNumber);
			return Collections.emptyNavigableMap();
		}
		
		return new TreeMap<>(Collections.singletonMap(readingDate, readingValue));
	}

}
