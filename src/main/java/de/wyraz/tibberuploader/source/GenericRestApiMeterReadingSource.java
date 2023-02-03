package de.wyraz.tibberuploader.source;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

import net.minidev.json.JSONArray;


public class GenericRestApiMeterReadingSource implements IMeterReadingSource {
	
	/**
	 * Meter number provided by that source. If configured, values are only returned if the correct meter is queried
	 */
	@Value("${readings.source.rest.meterNumber:}")
	public String meterNumber;
	
	@Value("${readings.source.rest.endpoint}")
	public String restEndpoint;

	@Value("${readings.source.rest.headers:}")
	public String restHeaders;
	
	protected final CloseableHttpClient httpClient=HttpClients.createDefault();
	
	@Override
	public TreeMap<LocalDate, Integer> findDailyReadings(String meterNumber, LocalDate firstDay, LocalDate lastDay) throws IOException {
		
		String endpoint=replacePlaceholders(restEndpoint,meterNumber,firstDay,lastDay);
		
		HttpGet getRequest=new HttpGet(endpoint);
		if (StringUtils.hasText(restHeaders)) {
			for (String h: restHeaders.split("[\r\n]+")) {
				if (h.indexOf(':')<=0) {
					continue;
				}
				String[] kv=h.split(":",2);
				if (!StringUtils.hasText(kv[0]) || !StringUtils.hasText(kv[1])) {
					continue;
				}
				getRequest.addHeader(kv[0].trim(), kv[1].trim());
			}
		}
		
		try (CloseableHttpResponse resp = httpClient.execute(getRequest)) {
			if (resp.getStatusLine().getStatusCode() != 200) {
				throw new IOException(resp.getStatusLine().toString());
			}
			DocumentContext json=JsonPath.parse(resp.getEntity().getContent());
			
			JSONArray values=json.read("*");
			
			for (Object value: values) {
				json=JsonPath.parse(value);
				String time=json.read("time");
				int reading=(int) (((Number)json.read("energyImportTotal")).doubleValue()/1000d);
				ZonedDateTime dt=ZonedDateTime.parse(time);
				System.err.println(dt.toLocalDate()+" "+reading);
			}
			
		}
		
		return null;
	}
	
	protected static final Pattern P_PLACERHOLDER=Pattern.compile("\\{([a-zA-Z0-9]+)(?:\\.([a-zA-Z0-9]+))?\\}");
	protected static String replacePlaceholders(String source, String meterNumber, LocalDate firstDay, LocalDate lastDay) {
		StringBuilder out=new StringBuilder();
		int pos=0;
		Matcher m=P_PLACERHOLDER.matcher(source);
		
		while (m.find(pos)) {
			out.append(source.substring(pos,m.start()));
			pos=m.end();
			
			String placeholder=m.group(1);
			String format=m.group(2);
			
			switch(placeholder.toLowerCase()) {
			
				case "meternumber":
					out.append(URLEncoder.encode(meterNumber,StandardCharsets.UTF_8));
					break;

				case "firstday":
					out.append(URLEncoder.encode(format(firstDay,format,false),StandardCharsets.UTF_8));
					break;

				case "lastday":
					out.append(URLEncoder.encode(format(lastDay,format,true),StandardCharsets.UTF_8));
					break;
					
				default:
					out.append(m.group());
					break;
			}
			
		}
		
		out.append(source.substring(pos));
		
		return out.toString();
	}
	
	protected static String format(LocalDate date, String format,boolean endDate) {
		if (format!=null) switch (format.toLowerCase()) {
			case "isodatetime":
				OffsetDateTime dt=date.atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
				if (endDate) {
					dt=dt.plusDays(1).minus(1, ChronoUnit.SECONDS);
				}
				return dt.toString();
		}
		return date.toString();
	}
	
}
