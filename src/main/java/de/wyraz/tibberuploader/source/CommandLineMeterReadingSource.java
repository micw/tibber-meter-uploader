package de.wyraz.tibberuploader.source;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.util.StringUtils;

import de.wyraz.tibberuploader.CommandLineArgsHolder;
import de.wyraz.tibberuploader.TibberConstants;

/**
 * Source for meter readings that gets readings from command line. Each command line argument must contain a reading in the format
 * 
 * 2023-01-19=10003
 * 2023-01-20=10114
 * 2023-01-21=10234
 * 
 * A reading for the current day can be specified with the special "today" keyword instead of a date:
 *
 * today=10521
 * 
 * All readings must be in kWh
 * 
 * @author mwyraz
 *
 */
public class CommandLineMeterReadingSource implements IMeterReadingSource {
	
	protected final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Meter number provided by that source. If configured, values are only returned if the correct meter is queried
	 */
	@Value("${readings.source.commandline.meterNumber:}")
	public String meterNumber;

	@Override
	public NavigableMap<LocalDate, Integer> findDailyReadings(String meterNumber, LocalDate firstDay, LocalDate lastDay)
			throws IOException {
		
		if (StringUtils.hasText(this.meterNumber) && !this.meterNumber.equalsIgnoreCase(meterNumber)) {
			log.warn("Wrong meter number reported by API. Expected '{}' but found '{}'",this.meterNumber,meterNumber);
			return Collections.emptyNavigableMap();
		}
		
		String[] args=CommandLineArgsHolder.get();
		
		if (args==null || args.length==0) {
			log.warn("No meter readings passed as command line");
			return Collections.emptyNavigableMap();
		}
		
		TreeMap<LocalDate, Integer> result=new TreeMap<>();

		for (String arg: args) {
			String[] kv=arg.split("=",2);
			if (kv.length!=2) {
				log.warn("Invalid argument: {}",arg);
				continue;
			}
			LocalDate date;
			if ("today".equals(kv[0])) {
				date=LocalDate.now();
			} else {
				try {
					date=LocalDate.parse(kv[0]);
				} catch (Exception ex) {
					log.warn("Unable to parse date from command line argument '{}': {}",kv[0],ex.getMessage());
					continue;
				}
			}
			
			if (date.isBefore(firstDay)) {
				log.warn("Skipping too old entry from command line argument: {}",arg);
				continue;
			}
			if (date.isAfter(lastDay)) {
				log.warn("Skipping too new entry from command line argument: {}",arg);
				continue;
			}
			
			Integer value;
			try {
				value=Integer.parseInt(kv[1]);
			} catch (Exception ex) {
				log.warn("Unable to parse reading value from command line argument '{}': {}",kv[1],ex.getMessage());
				continue;
			}
			
			result.put(date, value);
		}
		
		if (result.size()==0) {
			log.warn("No valid meter readings found at command line");
		}
		
		
		return result;
	}

}
