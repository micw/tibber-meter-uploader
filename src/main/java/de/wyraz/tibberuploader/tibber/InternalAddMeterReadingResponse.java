package de.wyraz.tibberuploader.tibber;

class InternalAddMeterReadingResponse {
	
	public boolean isSuccess() {
		return data!=null && data.me!=null && data.me.addMeterReadings!=null && data.me.addMeterReadings.success!=null;
	}
	
	public String getErrorMessage() {
		if (data!=null && data.me!=null && data.me.addMeterReadings!=null && data.me.addMeterReadings.error!=null
				&& data.me.addMeterReadings.error.message!=null) {
			return data.me.addMeterReadings.error.message;
		}
		return "Unknown error";
	}
	public String getSuccessMessage() {
		if (data!=null && data.me!=null && data.me.addMeterReadings!=null && data.me.addMeterReadings.success!=null
				&& data.me.addMeterReadings.success.descriptionHtml!=null) {
			return data.me.addMeterReadings.success.descriptionHtml;
		}
		return "no message";
	}
	
	public Data data;
	
	public static class Data {
		public Me me;
	}
	public static class Me {
		public AddMeterReadingsResult addMeterReadings;
	}
	public static class AddMeterReadingsResult {
		public SuccessResult success;
		public ErrorResult error;
	}

	public static class SuccessResult {
		public String inputTitle;
		public Integer inputValue;
		public String title;
		public String descriptionHtml;
		public String doneButtonText;
	}
	
	public static class ErrorResult {
		public String statusCode;
		public String title;
		public String message;
	}
	
}
