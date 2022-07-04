package com.ondc.endpoints;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import com.ondc.service.auth.factory.ONDCAuthHeaderUtility;
import com.ondc.service.impl.AbstractGenericRestEndPointImpl;

import utility.HttpHeaderUtil;

@ConditionalOnExpression(value = "${ondc.is.buyer:false}")
@Service("buyerRestEndPoint")
public class BuyerRestEndPointImpl extends AbstractGenericRestEndPointImpl implements GenericRestEndPoint {

	private static final String AUTHORIZATION = "Authorization";

	@Autowired
	private ONDCAuthHeaderUtility ondcAuthHeaderUtility;

	@Value("${ondc.buyer.private.key}")
	protected String privateKey;

	@Value("${ondc.buyer.public.key}")
	protected String publicKey;

	@Value("${ondc.buyer.subscriber.id}")
	protected String subscriberId;

	@Value("${ondc.buyer.unique.key.id}")
	protected String uniqueKeyId;

	@Autowired
	private HttpHeaderUtil httpHeaderUtil;

	@Override
	public HttpHeaders getEnrichedHeaders(String request) throws Exception {
		HttpHeaders httpHeaders = httpHeaderUtil.getEnrichedHeaders();
		httpHeaders.add(AUTHORIZATION,
				ondcAuthHeaderUtility.generateAuthSignature(request, subscriberId, uniqueKeyId, privateKey, publicKey));
		return httpHeaders;
	}
}
