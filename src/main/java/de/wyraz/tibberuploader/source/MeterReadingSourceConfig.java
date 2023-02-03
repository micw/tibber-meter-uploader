package de.wyraz.tibberuploader.source;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MeterReadingSourceConfig {
	
	@Bean
	public IMeterReadingSource meterReadingSource(
			@Value("${readings.source.class}") String sourceClassName) throws Exception {
		
		if (sourceClassName.indexOf('.')<0) {
			sourceClassName=getClass().getPackage().getName()+"."+sourceClassName;
		}
		
		return (IMeterReadingSource) Class.forName(sourceClassName).getConstructor().newInstance();
	}

}
