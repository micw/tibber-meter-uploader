package de.wyraz.tibberuploader.tibber;

import java.time.LocalDate;
import java.util.TreeMap;

import com.fasterxml.jackson.databind.ObjectMapper;

public class AccountInfo {
	
	String accountId;
	String email;
	String firstName;
	String lastName;
	
	String meterId;
	String meterNumber;
	String meterRegister; // the meter register to read/write. Should always be "1-0:1.8.0"
	boolean meterUserRead;
	int meterRegisterIndex; // internal: the index of the relevant register if multiple registers exist

	String homeId;
	
	TreeMap<LocalDate, Integer> readings;
	
	@Override
	public String toString() {
		try {
			return new ObjectMapper().findAndRegisterModules().writerWithDefaultPrettyPrinter().writeValueAsString(this);
		} catch (Exception ex) {
			ex.printStackTrace();
			return super.toString();
		}
	}
	
	public String getAccountId() {
		return accountId;
	}
	public String getEmail() {
		return email;
	}
	public String getFirstName() {
		return firstName;
	}
	public String getLastName() {
		return lastName;
	}
	public String getMeterId() {
		return meterId;
	}
	public String getMeterNumber() {
		return meterNumber;
	}
	public String getMeterRegister() {
		return meterRegister;
	}
	public boolean isMeterUserRead() {
		return meterUserRead;
	}
	public String getHomeId() {
		return homeId;
	}
	public TreeMap<LocalDate, Integer> getReadings() {
		return readings;
	}
}
