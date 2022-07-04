package com.ondc.service;

import java.io.IOException;

public interface ContextulizeSearch {

	String detectLang(String text) throws IOException;
	
	String translate(String text, String source, String target) throws IOException;
	
	String getContext(String text) throws IOException;
	
	String contextulizedSearch(String text, boolean enableContext);
	
	String normalizeText(String search);
	
}
