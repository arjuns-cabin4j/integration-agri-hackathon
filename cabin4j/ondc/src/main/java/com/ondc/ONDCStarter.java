package com.ondc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.cabin4j.suite.cms.service.CMSi18nService;
import com.cabin4j.suite.platform.RestartNodeEventListener;
import com.xhopfront.service.impl.XFCMSi18nServiceImpl;

@EnableScheduling
@EnableAsync
@SpringBootApplication
@EnableAutoConfiguration
@ComponentScan({ "com.cabin4j","com.xhopfront", "com.ondc","utility" })
@EntityScan(basePackages = { "com.cabin4j.suite.entity","com.xhopfront.entities","com.ondc.entities"})
public class ONDCStarter implements WebMvcConfigurer{

	public static void main(String[] args) {
		//System.setProperty("es.set.netty.runtime.available.processors", "false");// required for
																					// micrometer-elasticsearch
		SpringApplication application = new SpringApplication(ONDCStarter.class);
		application.addListeners(new RestartNodeEventListener(ONDCStarter.class));
		application.run(args);
	}
	
	@Bean
	public CMSi18nService i18n() {
		return new XFCMSi18nServiceImpl();
	}
}
