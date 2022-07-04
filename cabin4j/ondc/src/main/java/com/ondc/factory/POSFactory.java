package com.ondc.factory;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.cabin4j.suite.platform.services.GlobalPropertiesService;

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;

@Component
public class POSFactory implements InitializingBean {

	private final Logger LOG = LoggerFactory.getLogger(POSFactory.class);

	private static final String COMMA = ",";
	
	private List<POSTaggerME> posTaggers;
	
	@Autowired
	private GlobalPropertiesService globalPropertiesService;
	
	@Override
	public void afterPropertiesSet() throws Exception {
		String models = globalPropertiesService.getPropertyFromCache("search.nlp.pos.models", null);
		if(StringUtils.isNotBlank(models)) {
			if(null == this.posTaggers)
				 this.posTaggers = new ArrayList<>();
			for (String path : models.split(COMMA)) {
				if(StringUtils.isNotBlank(path)) {
					try {
						this.posTaggers.add(new POSTaggerME(new POSModel(new FileInputStream(path.trim()))));
					} catch (Exception e) {
						LOG.error("Exception occurred while initializing POSTaggerME for model on path '{}'", path, e);
					}
				}
			}
		}
		
	}

	public List<POSTaggerME> getPosTaggers() {
		return posTaggers;
	}
}
