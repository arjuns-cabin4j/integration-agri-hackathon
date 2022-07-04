package com.ondc.endpoints;

import org.springframework.http.ResponseEntity;

public interface GenericRestEndPoint {

	ResponseEntity<String>  onCall(String req, String url) throws Exception;
}
