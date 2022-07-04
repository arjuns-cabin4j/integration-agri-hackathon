package com.ondc.service.auth.factory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.Security;
import java.util.Base64;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.Signer;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.bouncycastle.crypto.signers.Ed25519Signer;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("ondcAuthHeaderUtility")
public class ONDCAuthHeaderUtility {

	private final Logger LOG = LoggerFactory.getLogger(ONDCAuthHeaderUtility.class);

	private static final String END_SLASH = "\"";
	private static final String HEADERS_CREATED_EXPIRES_DIGEST_SIGNATURE = "\", headers=\"(created) (expires) digest\", signature=\"";
	private static final String EXPIRES2 = "\", expires=\"";
	private static final String ALGORITHM_ED25519_CREATED = "\",algorithm=\"ed25519\", created=\"";
	private static final String SIGNATURE_KEY_ID = "Signature keyId=\"";
	private static final String PIPE = "|";
	private static final String BLAKE2B_512 = "BLAKE2B-512";
	private static final String ED25519 = "ed25519";
	private static final String DIGEST_BLAKE_512 = "\ndigest: BLAKE-512=";
	private static final String EXPIRES = "\n(expires): ";
	private static final String CREATED = "(created): ";

	public String generateAuthSignature(String message, String subscriberId, String uniqueKeyId, String privateKey,
			String publicKey) throws Exception {
		return this.generateAuthSignature(message, subscriberId, uniqueKeyId, privateKey, publicKey, null, null);
	}
	
	public String generateAuthSignature(String message, String subscriberId, String uniqueKeyId, String privateKey,
			String publicKey, Long validity, Boolean verifyAuthSignature) throws Exception {
		if (null == message || message.isBlank())
			throw new Exception("Message should not be blank!");

		if (null == subscriberId || subscriberId.isBlank())
			throw new Exception("Subscriber Id must not be blank!");

		if (null == uniqueKeyId || uniqueKeyId.isBlank())
			throw new Exception("Unique Key Id must not be blank!");

		if (null == privateKey || privateKey.isBlank())
			throw new Exception("Private Key must not be blank!");

		if (null == publicKey || publicKey.isBlank())
			throw new Exception("Public Key must not be blank!");

		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
			Security.addProvider(new BouncyCastleProvider());
		}

		try {
			validity = (null == validity || validity < Long.valueOf(1)) ? Long.valueOf(60000) : validity;
			long currentTimestamp = System.currentTimeMillis() / 1000L;
			long expiryTimestamp = currentTimestamp + validity;
			String signingString = CREATED + currentTimestamp + EXPIRES + expiryTimestamp + DIGEST_BLAKE_512
					+ generateBlakeHash(message) + StringUtils.EMPTY;
			String signedReq = generateSignature(signingString, privateKey);

			if (BooleanUtils.isTrue(verifyAuthSignature) && !verifySignature(signedReq, signingString, publicKey)) {
				throw new Exception("Auth signature verification failed.");
			}
			return SIGNATURE_KEY_ID + subscriberId + PIPE + uniqueKeyId + PIPE + ED25519 + ALGORITHM_ED25519_CREATED
					+ currentTimestamp + EXPIRES2 + expiryTimestamp + HEADERS_CREATED_EXPIRES_DIGEST_SIGNATURE
					+ signedReq + END_SLASH;
		} catch (Exception e) {
			LOG.error("Exception occurred while signing message.", e);
			throw e;
		}
	}

	protected boolean verifySignature(String sign, String requestData, String dbPublicKey) {
		try {
			Ed25519PublicKeyParameters publicKey = new Ed25519PublicKeyParameters(
					Base64.getDecoder().decode(dbPublicKey), 0);
			Signer sv = new Ed25519Signer();
			sv.init(false, publicKey);
			sv.update(requestData.getBytes(), 0, requestData.length());

			byte[] decodedSign = Base64.getDecoder().decode(sign);
			return sv.verifySignature(decodedSign);
		} catch (Exception e) {
			LOG.error("Exception occured while verifying signature.", e);
		}
		return false;
	}

	protected String generateBlakeHash(String req) throws Exception {
		MessageDigest digest = MessageDigest.getInstance(BLAKE2B_512, BouncyCastleProvider.PROVIDER_NAME);
		digest.reset();
		digest.update(req.getBytes(StandardCharsets.UTF_8));
		byte[] hash = digest.digest();
		String bs64 = Base64.getEncoder().encodeToString(hash);
		System.out.println(bs64);
		return bs64;
	}

	protected String generateSignature(String req, String pk) {
		String signature = null;
		try {
			Ed25519PrivateKeyParameters privateKey = new Ed25519PrivateKeyParameters(
					Base64.getDecoder().decode(pk.getBytes()), 0);
			Signer sig = new Ed25519Signer();
			sig.init(true, privateKey);
			sig.update(req.getBytes(), 0, req.length());
			byte[] signByte = sig.generateSignature();
			signature = Base64.getEncoder().encodeToString(signByte);
		} catch (DataLengthException | CryptoException e) {
			LOG.error("Exception occurred while generating signature.", e);
		}
		return signature;
	}
}