package de.wyraz.tibberuploader.tibber;

class InternalAddMeterReadingFailureResponse {
	
	public String getErrorMessage() {
		if (errors!=null && errors.length>0 && errors[0].message!=null) {
			return errors[0].message;
		}
		return "Unknown error";
	}
	
	public Error[] errors;
	
	public static class Error {
		public String message;
		public ErrorExtensions extensions;
	}

	public static class ErrorExtensions {
		public String code;
		public ErrorException exception; 
	}

	public static class ErrorException {
		public String[] stacktrace;
	}
	
}
