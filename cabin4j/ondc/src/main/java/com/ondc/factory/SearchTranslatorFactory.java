package com.ondc.factory;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.cabin4j.suite.platform.services.GlobalPropertiesService;
import com.google.cloud.translate.v3.LocationName;
import com.google.cloud.translate.v3.TranslationServiceClient;

@Component
public class SearchTranslatorFactory implements InitializingBean, DisposableBean {

	private final Logger LOG = LoggerFactory.getLogger(SearchTranslatorFactory.class);

	private TranslationServiceClient translationServiceClient;

	private LocationName locationName;

	@Autowired
	private GlobalPropertiesService globalPropertiesService;

	@Override
	public void afterPropertiesSet() throws Exception {
		try {
			this.translationServiceClient = TranslationServiceClient.create();

			String projectID = globalPropertiesService.getPropertyFromCache("google.translate.project.id", null);
			String projectLoc = globalPropertiesService.getPropertyFromCache("google.translate.project.loc", null);
			if (StringUtils.isBlank(projectID))
				throw new Exception("Google translate project Id is missing.");

			if (StringUtils.isBlank(projectLoc))
				throw new Exception("Google translate project location is missing.");

			this.locationName = LocationName.of(projectID, projectLoc);
		} catch (Exception e) {
			LOG.error("Exception occurred while initializing Google Translate.", e);
		}
	}

	@Override
	public void destroy() throws Exception {
		if (null != translationServiceClient) {
			translationServiceClient.close();
		}
	}

	public TranslationServiceClient getTranslationServiceClient() {
		return translationServiceClient;
	}

	public LocationName getLocationName() {
		return locationName;
	}


}
