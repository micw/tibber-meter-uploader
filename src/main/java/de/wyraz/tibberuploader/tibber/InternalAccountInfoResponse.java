package de.wyraz.tibberuploader.tibber;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.TreeMap;

class InternalAccountInfoResponse {

	public AccountInfoData data;

	public AccountInfo unwrap() {
		if (data!=null && data.me!=null) {
			return data.me.unwrap();
		}
		
		return null;
	}
	
	public static class AccountInfoData {
		public AccountInfoMe me;
	}

	public static class AccountInfoMe {
		public String id;
		public String firstName;
		public String lastName;
		public String email;
		public AccountInfoMeterList meters;
		public AccountInfoHome[] homes;
		
		public AccountInfo unwrap() {
			AccountInfo info=new AccountInfo();
			info.accountId=id;
			info.email=email;
			info.firstName=firstName;
			info.lastName=lastName;
			
			if (meters!=null || meters.items!=null) {
				for (AccountInfoMeterItem mi: meters.items) {
					if (mi.meter==null) { // "label" item with no meter
						continue;
					}
					mi.meter.unwrap(info);
				}
				
			}
			if (info.meterId==null) {
				throw new IllegalArgumentException("No meters found in account");
			}

			if (homes!=null) {
				for (AccountInfoHome home: homes) {
					home.unwrap(info);
				}
				
			}
			
			return info;
		}
		
	}
	
	public static class AccountInfoMeterList {
		public AccountInfoMeterItem[] items;
	}
	
	public static class AccountInfoMeterItem {
		public AccountInfoMeter meter;
	}

	public static class AccountInfoMeter {
		public String id;
		public String title;
		public String description;
		public AccountInfoMeterRegister registers[];
		
		public void unwrap(AccountInfo info) {
			if (info.meterId!=null) {
				throw new IllegalArgumentException("More than one meters found in account. Multiple meters are not supported.");
			}
			info.meterId=id;
			if (registers==null || registers.length==0) {
				throw new IllegalArgumentException("Meter "+id+" has no registers");
			}
			int regIndex=0;
			for (AccountInfoMeterRegister reg: registers) {
				if ("1-0:1.8.0".equals(reg.id)) {
					info.meterRegister=reg.id;
					info.meterRegisterIndex=regIndex;
					break;
				}
				regIndex++;
			}
			if (info.meterRegister==null) {
				throw new IllegalArgumentException("Meter "+id+" has no register with id '1-0:1.8.0'");
			}
		}
	}

	public static class AccountInfoMeterRegister {
		public String id;
	}
	
	public static class AccountInfoHome {
		public String id;
		public AccountInfoHomeAddress address;
		public String description;
		public AccountInfoMeterRegister registers[];
		public AccountInfoCurrentMeter currentMeter;
		public ConsumptionAnalysisItem[] consumptionAnalysisItemsForUserReadMeter;
		
		public void unwrap(AccountInfo info) {
			if (info.homeId!=null) {
				throw new IllegalArgumentException("More than one home found in account. Multiple homes are not supported.");
			}
			info.homeId=id;
			
			if (currentMeter!=null && info.meterId.equals(currentMeter.id)) {
				info.meterNumber=currentMeter.meterNo;
				info.meterUserRead=currentMeter.isUserRead;
			} else {
				throw new IllegalArgumentException("Meter "+info.meterId+" is not the current meter of home "+info.homeId);
			}
			
			info.readings=new TreeMap<>();
			
			if (consumptionAnalysisItemsForUserReadMeter!=null) {
				for (ConsumptionAnalysisItem i: consumptionAnalysisItemsForUserReadMeter) {
					if (i.meterReadings!=null) {
						for (MeterReading r: i.meterReadings) {
							if (r.date!=null && r.registers!=null && r.registers.length>info.meterRegisterIndex) {
								Integer reading=r.registers[info.meterRegisterIndex].value;
								if (reading!=null) {
									info.readings.put(r.date.withZoneSameLocal(ZoneId.of("Z")).toLocalDate(),reading);
								}
							}
						}
					}
				}
			}
			
		}

	}

	public static class AccountInfoHomeAddress {
		public String addressText;
		public String city;
		public String postalCode;
		public String country;
	}

	public static class AccountInfoCurrentMeter {
		public String id;
		public String meterNo;
		public boolean isUserRead;
	}
	
	public static class ConsumptionAnalysisItem {
		public ZonedDateTime from;
		public ZonedDateTime to;
		public boolean meterReadingForCurrentMonthIsRecommended;
		public boolean meterReadingForPreviousMonthIsRecommended;
		public MeterReading[] meterReadings;
	}
	
	public static class MeterReading {
		public ZonedDateTime date;
		public MeterValue[] registers;
	}
	public static class MeterValue {
		public Integer value;
	}

	
}
