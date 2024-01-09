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
import org.springframework.util.StringUtils;

import de.wyraz.tibberuploader.TibberConstants;

/**
 * Source for meter readings that executes a shell script to obtain the readings.
 * 
 * Provided environment variables:
 *   FIRST_DAY - the first day to query in the format 2023-01-19
 *   LAST_DAY - the last day to query in the format 2023-01-22
 *   METER - the meter that is being queried in the format 1EBZ0123456789
 *   FIRST_DAY_START_ISO_TZ - the start time of first day to query in ISO format with local time zone, e.g. 2023-01-19T00:00:00+01:00[Europe/Berlin]
 *   LAST_DAY_END_ISO_TZ - the start time of the day after the last day to query in ISO format with local time zone, e.g. 2023-01-23T00:00:00+01:00[Europe/Berlin]
 * 
 * Expected result:
 * 
 * One line per day, containing the date and the meter reading in kilowatts, separated by whitespace, comma or semicolon:
 * 
 *   2023-01-19 10003
 *   2023-01-20 10114
 *   2023-01-21 10234
 *   2023-01-22 10521
 * 
 * @author mwyraz
 *
 */
public class ScriptedRestApiMeterReadingSource implements IMeterReadingSource {
	
	protected final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Meter number provided by that source. If configured, values are only returned if the correct meter is queried
	 */
	@Value("${readings.source.script.meterNumber:}")
	public String meterNumber;

	/**
	 * Command to execute
	 */
	@Value("${readings.source.script.command:}")
	public String command;

	@Override
	public NavigableMap<LocalDate, Integer> findDailyReadings(String meterNumber, LocalDate firstDay, LocalDate lastDay)
			throws IOException {
		
		if (StringUtils.hasText(this.meterNumber) && !this.meterNumber.equalsIgnoreCase(meterNumber)) {
			log.warn("Wrong meter number reported by API. Expected '{}' but found '{}'",this.meterNumber,meterNumber);
			return Collections.emptyNavigableMap();
		}
		
		CommandLine cmdLine = new CommandLine("sh")
				.addArgument("-c")
				.addArgument(command, false);
		
		log.debug("Executing {}",command);
		
		Map<String, String> env=new HashMap<>();
		env.put("FIRST_DAY", firstDay.toString());
		env.put("LAST_DAY", lastDay.toString());
		env.put("METER", meterNumber);
		env.put("FIRST_DAY_START_ISO_TZ", firstDay.atStartOfDay(TibberConstants.TIMEZONE_ID).toString());
		env.put("LAST_DAY_END_ISO_TZ", lastDay.plusDays(1).atStartOfDay(TibberConstants.TIMEZONE_ID).toString());
		
		DefaultExecutor executor = new DefaultExecutor();
		
		executor.setExitValues(null);
		
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
		
		executor.setStreamHandler(new PumpStreamHandler(outputStream, errorStream));
		
		int exitValue = executor.execute(cmdLine, env);
		
		String output=outputStream.toString();
		String error=errorStream.toString();
		
		if (exitValue!=0) {
			log.warn("Script exited with code "+exitValue+
					"\nStdOut:\n"+
					output+
					"\nStdErr:\n"+
					error);
			return Collections.emptyNavigableMap();
		}

		log.debug("Script returned:\n{}",output);

		if (StringUtils.hasText(error)) {
			log.warn("Script exited with success but got some text on stderr:\n"+
					error);
		}
		
		TreeMap<LocalDate, Integer> result=new TreeMap<>();
		for (String line: output.split("\\R")) {
			line=line.trim();
			if (!StringUtils.hasText(line)) {
				continue;
			}
			String[] kv=line.split("[\\s,;]+");
			if (kv.length!=2) {
				log.warn("Line does not contain date + reading: {}", line);
				continue;
			}
			LocalDate date;
			try {
				date=LocalDate.parse(kv[0]);
			} catch (RuntimeException ex) {
				log.warn("Unable to parse date ({}): {}", ex.toString(), line);
				continue;
			}
			Integer reading;
			try {
				reading=Integer.parseInt(kv[1]);
			} catch (RuntimeException ex) {
				log.warn("Unable to parse reading ({}): {}", ex.toString(), line);
				continue;
			}
			result.put(date, reading);
		}
		
		return result;
	}

}
