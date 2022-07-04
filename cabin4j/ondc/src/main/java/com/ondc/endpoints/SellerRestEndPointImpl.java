package com.ondc.endpoints;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import com.ondc.service.auth.factory.ONDCAuthHeaderUtility;
import com.ondc.service.impl.AbstractGenericRestEndPointImpl;

import utility.HttpHeaderUtil;

@ConditionalOnExpression(value = "${ondc.is.seller:false}")
@Service("sellerRestEndPoint")
public class SellerRestEndPointImpl extends AbstractGenericRestEndPointImpl implements GenericRestEndPoint {

	private static final String AUTHORIZATION = "Authorization";

	@Autowired
	private ONDCAuthHeaderUtility ondcAuthHeaderUtility;

	@Value("${ondc.seller.private.key}")
	String privateKey;

	@Value("${ondc.seller.public.key}")
	String publicKey;

	@Value("${ondc.seller.subscriber.id}")
	String subscriberId;

	@Value("${ondc.seller.unique.key.id}")
	String uniqueKeyId;

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
