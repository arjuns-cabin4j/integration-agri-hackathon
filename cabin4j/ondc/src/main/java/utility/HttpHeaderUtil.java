package utility;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class HttpHeaderUtil {
	
	/**
	 * Make a request to a beckn system. wrapper for enrichHeaders(final
	 * HttpHeaders headers)
	 *
	 * @return enriched HttpHeaders
	 */
	public HttpHeaders getEnrichedHeaders()
	{
		final HttpHeaders headers = getDefaultHeaders();
		return enrichHeaders(headers);
	}

	/**
	 * Make a request to a beckn system.
	 *
	 * @param headers
	 * @return headers
	 */
	public HttpHeaders enrichHeaders(final HttpHeaders headers)
	{
		return headers;
	}
	
	/**
	 * Makes some default http headers for communication with beckn endpoints
	 *
	 * @return default headers
	 */
	public HttpHeaders getDefaultHeaders()
	{
		final HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		return headers;
	}
}
