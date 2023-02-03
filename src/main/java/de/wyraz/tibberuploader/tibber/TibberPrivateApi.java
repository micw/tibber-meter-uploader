package de.wyraz.tibberuploader.tibber;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.wyraz.tibberuploader.TibberConstants;
import net.minidev.json.JSONObject;

/**
 * TODO:
 * - refactor GraphQL code to avoid repeated code
 * - cache login token
 *     - at least between getAccountInfo and addMeterData
 *     - better as long as it is valid
 *     - alternatively, log out after each request if that's possible
 *
 * @author mwyraz
 */
@Service
public class TibberPrivateApi {
	
	protected final Logger log = LoggerFactory.getLogger(getClass());
	
	@Value("${tibber.loginEmail}")
	protected String tibberLoginEmail;

	@Value("${tibber.password}")
	protected String tibberPassword;
	
	protected final CloseableHttpClient httpClient=HttpClients.createDefault();
	
	protected final String LOGIN_URL="https://app.tibber.com/v1/login.credentials";
	protected final String API_URL="https://app.tibber.com/v4/gql";
	
	protected final String QUERY_ACCOUNT_INFO=IOUtils.toString(
			getClass().getClassLoader().getResource("tibber-appclient-v4/AccountInfo.graphql"),StandardCharsets.UTF_8);
	protected final String MUTATION_ADD_METER_READING=IOUtils.toString(
			getClass().getClassLoader().getResource("tibber-appclient-v4/AddMeterReading.graphql"),StandardCharsets.UTF_8);
	
	protected final ObjectMapper MAPPER=new ObjectMapper()
			.findAndRegisterModules()
			.setTimeZone(TibberConstants.TIMEZONE);
	
	public TibberPrivateApi() throws IOException {
	}
	
	public String login() throws IOException {
		
		String payload=new JSONObject()
				.appendField("email", tibberLoginEmail)
				.appendField("password", tibberPassword)
				.toJSONString();
		
		HttpPost post=new HttpPost(LOGIN_URL);
		post.setEntity(new StringEntity(payload,ContentType.APPLICATION_JSON));
		
		try (CloseableHttpResponse resp = httpClient.execute(post)) {
			LoginTokenResponse result;
			try {
				result=MAPPER.readValue(resp.getEntity().getContent(), LoginTokenResponse.class);
			} catch (Exception ex) {
				throw new IOException("Unable to parse tibber login result",ex);
			}
			
			if (resp.getStatusLine().getStatusCode()!=200) {
				throw new IOException("Tibber login failed: "+result.message);
			}
			
			if (!StringUtils.hasText(result.token)) {
				throw new IOException("Tibber login result contains no token: "+result);
			}
			
			return result.token;
		}
	}
	
	public AccountInfo getAccoutInfo(LocalDate readingsFromDate, LocalDate readingsToDate) throws Exception {
		HttpPost post=new HttpPost(API_URL);
		
		JSONObject queryData=new JSONObject();
		queryData.put("query", QUERY_ACCOUNT_INFO);
		
		Map<String,Object> vars=new HashMap<>();
		vars.put("readingsFromDate", readingsFromDate.toString());
		vars.put("readingsToDate", readingsToDate.toString());
		queryData.put("variables", vars);
		
		post.setEntity(new StringEntity(queryData.toJSONString(),ContentType.APPLICATION_JSON));
		
		post.addHeader("Cookie", "token="+login());
		
		try (CloseableHttpResponse resp = httpClient.execute(post)) {
			
			InternalAccountInfoResponse response=MAPPER
				.readerFor(InternalAccountInfoResponse.class)
				.readValue(resp.getEntity().getContent());
			return response.unwrap();
		}
	}
	
	public void addAddMeterReading(AccountInfo accountInfo, LocalDate readingDate, int readingValue) throws Exception {
		addAddMeterReading(accountInfo.getMeterId(), accountInfo.getMeterRegister(), readingDate, readingValue);
	}

	public void addAddMeterReading(String meterId, String meterRegister, LocalDate readingDate, int readingValue) throws Exception {
		HttpPost post=new HttpPost(API_URL);
		
		JSONObject queryData=new JSONObject();
		queryData.put("query", MUTATION_ADD_METER_READING);
		
		Map<String,Object> vars=new HashMap<>();
		vars.put("meterId", meterId);
		vars.put("meterRegister", meterRegister);
		vars.put("readingDate", readingDate.toString());
		vars.put("reading", readingValue);
		queryData.put("variables", vars);
		
		post.setEntity(new StringEntity(queryData.toJSONString(),ContentType.APPLICATION_JSON));
		
		post.addHeader("Cookie", "token="+login());
		
		try (CloseableHttpResponse resp = httpClient.execute(post)) {
			
			if (resp.getStatusLine().getStatusCode()!=200) {
				InternalAddMeterReadingFailureResponse response=MAPPER
						.readerFor(InternalAddMeterReadingFailureResponse.class)
						.readValue(resp.getEntity().getContent());
				throw new IOException(response.getErrorMessage());
			}
			
			InternalAddMeterReadingResponse response=MAPPER
				.readerFor(InternalAddMeterReadingResponse.class)
				.readValue(resp.getEntity().getContent());
			
			if (!response.isSuccess()) {
				throw new RuntimeException("Error adding reading to tibber: "+response.getErrorMessage());
			}
			
			log.info("Added value at {} with reading {}: {}", readingDate, readingValue, response.getSuccessMessage());
		}
	}
	
}
