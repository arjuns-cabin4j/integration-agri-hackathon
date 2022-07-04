package com.ondc.service.impl;

import java.util.Collections;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.ondc.endpoints.GenericRestEndPoint;

public abstract class AbstractGenericRestEndPointImpl implements GenericRestEndPoint {

	@SuppressWarnings("unchecked")
	@Override
	public ResponseEntity<String> onCall(String req, String url) throws Exception {
		RestTemplate template = new RestTemplate();
		HttpEntity<String> request = new HttpEntity<>(req, getEnrichedHeaders(req));
		return template.exchange(url, HttpMethod.POST, request, String.class, Collections.EMPTY_MAP);
	}

	public abstract HttpHeaders getEnrichedHeaders(String request) throws Exception;
}
