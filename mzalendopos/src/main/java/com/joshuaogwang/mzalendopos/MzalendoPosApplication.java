package com.joshuaogwang.mzalendopos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.joshuaogwang.mzalendopos.config.AccountingProperties;
import com.joshuaogwang.mzalendopos.config.EfrisProperties;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({EfrisProperties.class, AccountingProperties.class})
public class MzalendoPosApplication {

	public static void main(String[] args) {
		SpringApplication.run(MzalendoPosApplication.class, args);
	}
}
