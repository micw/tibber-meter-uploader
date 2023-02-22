package de.wyraz.tibberuploader;

import java.time.LocalDate;
import java.util.Map.Entry;
import java.util.NavigableMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import de.wyraz.tibberuploader.source.IMeterReadingSource;
import de.wyraz.tibberuploader.tibber.AccountInfo;
import de.wyraz.tibberuploader.tibber.TibberPrivateApi;

@SpringBootApplication
@EnableScheduling
public class TibberUploader implements CommandLineRunner {

	protected final Logger log = LoggerFactory.getLogger(getClass());

	public static void main(String[] args) throws Exception {
		SpringApplication.run(TibberUploader.class, args);
	}

	@Autowired
	protected IMeterReadingSource source;

	@Autowired
	protected TibberPrivateApi tibberApi;

	@Value("${dryRun}")
	protected boolean dryRun;
	
	@Override
	public void run(String... args) throws Exception {
		System.err.println(1);
		uploadMissingReadings();
	}

	@Scheduled(cron = "${scheduling.effectiveCronExpression:-}") // every full hour
	public void uploadMissingReadings() throws Exception {

		LocalDate today = LocalDate.now();
		LocalDate startDate = today.minusDays(30);

		AccountInfo info = tibberApi.getAccoutInfo(startDate, today.plusDays(1));

		Entry<LocalDate, Integer> lastEntry = null;
		if (info.getReadings() == null || info.getReadings().isEmpty()) {
			log.info("No recent readings found. Starting from {}", startDate);
		} else {
			lastEntry = info.getReadings().lastEntry();
			if (!lastEntry.getKey().isBefore(today)) {
				log.info("Last entry is from today with reading {}. Nothing to do for now.", lastEntry.getValue());
				return;
			}
			else if (lastEntry.getKey().isBefore(startDate)) {
				log.info("Last entry is outdated (from {}) with reading {}. Starting from {}.", lastEntry.getKey(),
						lastEntry.getValue(), startDate);
			} else {
				startDate = lastEntry.getKey().plusDays(1);
				log.info("Last entry is from {} with reading {}. Starting from {}.", lastEntry.getKey(),
						lastEntry.getValue(), startDate);
			}
		}

		NavigableMap<LocalDate, Integer> newReadings = source.findDailyReadings(info.getMeterNumber(), startDate, today);
		for (Entry<LocalDate, Integer> entry : newReadings.entrySet()) {
			if (entry.getKey().isBefore(startDate)) {
				log.info("Skipping unexpected value at {} with reading {}", entry.getKey(), entry.getValue());
				continue;
			}
			if (lastEntry!=null && entry.getValue()<lastEntry.getValue()) {
				log.warn("Found new reading that is smaller than previous reading. Something is wrong. Processing stopped.");
				return;
			}
			if (dryRun) {
				log.info("(DRY-RUN) Would add new value at {} with reading {}", entry.getKey(), entry.getValue());
			} else {
				log.info("Adding new value at {} with reading {}", entry.getKey(), entry.getValue());
				tibberApi.addAddMeterReading(info, entry.getKey(), entry.getValue());
			}
		}

	}

}
