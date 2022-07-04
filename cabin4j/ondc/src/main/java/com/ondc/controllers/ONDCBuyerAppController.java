package com.ondc.controllers;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cabin4j.suite.entity.redis.EventsChannelTopicEntity;
import com.cabin4j.suite.platform.constants.Constants;
import com.cabin4j.suite.platform.dao.GenericDao;
import com.cabin4j.suite.platform.data.SearchParams;
import com.cabin4j.suite.platform.services.impl.NodeResolverService;
import com.ondc.endpoints.GenericRestEndPoint;

@ConditionalOnExpression("${ondc.is.buyer:false}")
@RestController
@RequestMapping("${ondc.buyer.subscriber.base.url}")
public class ONDCBuyerAppController {

	private final Logger LOG = LoggerFactory.getLogger(ONDCBuyerAppController.class);

	private final static SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

	private static final String requestTemplate = "{\"context\":{\"domain\":\"nic2004:52110\",\"country\":\"IND\",\"city\":\"%s\",\"action\":\"search\",\"core_version\":\"0.9.2\",\"bap_id\":\"agri.xhopfront.com\",\"bap_uri\":\"https:\\/\\/agri.xhopfront.com\\/ondc\\/v1\",\"transaction_id\":\"%s\",\"message_id\":\"%s\",\"timestamp\":\"%s\"},\"message\":{\"criteria\":{\"delivery_location\":\"%s\",\"search_string\":\"%s\"}}}";

	@Value("${ondc.search.gateway.url}")
	protected String gatewayURL;

	@Autowired
 	@Qualifier("buyerRestEndPoint")
	private GenericRestEndPoint genericRestEndPoint;

	@Autowired
	private GenericDao genericDao;

	@Autowired
	private NodeResolverService nodeResolverService;

	@PostMapping("/search")
	public ResponseEntity<String> search(HttpServletRequest request) {
		if (null == request) {
			return new ResponseEntity<String>("Request is null.", HttpStatus.BAD_REQUEST);
		}
		if(LOG.isDebugEnabled())
			LOG.debug("Buyer Search Request Initiated.", request);
		
		try {
			String transactionId = UUID.randomUUID().toString();
			String messageId = UUID.randomUUID().toString();
			Date timestamp = new Date();
			String req = String.format(requestTemplate, request.getParameter("city"), transactionId, messageId,
					format.format(timestamp), request.getParameter("delivery_location"),
					request.getParameter("search_string"));
			ResponseEntity<String> response = genericRestEndPoint.onCall(req, gatewayURL);
			if (null == response || StringUtils.isBlank(response.getBody()))
				throw new Exception("Empty response received for request: " + req);

			if(LOG.isDebugEnabled())
				LOG.debug("Buyer Search Request Acknowledged. MessageId: {} \n Response: {}", messageId, response.getBody());
			
			EventsChannelTopicEntity messageReq = new EventsChannelTopicEntity();
			messageReq.setTimestamp(timestamp);
			messageReq.setAction("bap-search");
			messageReq.setMessage(req);
			messageReq.setMessageId(messageId);
			messageReq.setSource(this.nodeResolverService.getNodeId());
			genericDao.save(messageReq);

			EventsChannelTopicEntity messageRes = new EventsChannelTopicEntity();
			messageRes.setTimestamp(new Date());
			messageRes.setAction("bap-search-ack");
			messageRes.setMessage(response.getBody());
			messageRes.setMessageId(messageId);
			messageRes.setSource(this.nodeResolverService.getNodeId());
			genericDao.save(messageRes);

			return new ResponseEntity<String>(messageId, HttpStatus.OK);
		} catch (Exception e) {
			LOG.error("Exception occurred while sending buyer search request.", e);
			return new ResponseEntity<String>("Exception occurred while sending buyer search request.",
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	@PostMapping("/select")
	public ResponseEntity<String> select(RequestEntity<String> request) {
		if (null == request || StringUtils.isBlank(request.getBody())) {
			return new ResponseEntity<String>("Request is null.", HttpStatus.BAD_REQUEST);
		}
		try {
			JSONObject json = new JSONObject(request.getBody());
			JSONObject context = json.getJSONObject("context");
			context.put("transaction_id", UUID.randomUUID().toString());
			context.put("action", "on_search");
			Date timestamp = new Date();
			context.put("timestamp", format.format(timestamp));
			String bppUri = context.getString("bpp_uri");
			String messageId = context.getString("message_id");
			
			String selectRequest = json.toString();
			if(LOG.isDebugEnabled())
				LOG.debug("Buyer Select Request Initiated.", selectRequest);
			
			ResponseEntity<String> response = genericRestEndPoint.onCall(selectRequest, bppUri);
			if (null == response || StringUtils.isBlank(response.getBody()))
				throw new Exception("Empty response received for request: " + selectRequest);

			if(LOG.isDebugEnabled())
				LOG.debug("Buyer Select Request Acknowledged. MessageId: {} \n Response: {}", messageId, response.getBody());
			
			EventsChannelTopicEntity messageReq = new EventsChannelTopicEntity();
			messageReq.setTimestamp(timestamp);
			messageReq.setAction("bap-select");
			messageReq.setMessage(selectRequest);
			messageReq.setMessageId(messageId);
			messageReq.setSource(this.nodeResolverService.getNodeId());
			genericDao.save(messageReq);

			EventsChannelTopicEntity messageRes = new EventsChannelTopicEntity();
			messageRes.setTimestamp(new Date());
			messageRes.setAction("bap-select-ack");
			messageRes.setMessage(response.getBody());
			messageRes.setMessageId(messageId);
			messageRes.setSource(this.nodeResolverService.getNodeId());
			genericDao.save(messageRes);

			return new ResponseEntity<String>(messageId, HttpStatus.OK);
		} catch (Exception e) {
			LOG.error("Exception occurred while sending buyer select request.", e);
			return new ResponseEntity<String>("Exception occurred while sending buyer select request.",
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping(value = "/on_search")
	public ResponseEntity<HttpStatus> onSearch(RequestEntity<String> request) {
		try {
			if (null != request && StringUtils.isNotBlank(request.getBody())) {
				if(LOG.isDebugEnabled())
					LOG.debug("Buyer on_search Request Received. Request: {}", request.getBody());
				
				JSONObject json = new JSONObject(request.getBody());
				if (json.has("context")) {
					JSONObject context = json.getJSONObject("context");
					if (null != context && context.has("message_id")) {
						String messageId = context.getString("message_id");
						if (StringUtils.isNotBlank(messageId)) {
							List<SearchParams> params = new ArrayList<>();
							params.add(
									new SearchParams("messageId", Constants.QueryComparator.EQUALS, messageId.trim()));
							EventsChannelTopicEntity messageReq = new EventsChannelTopicEntity();
							messageReq.setTimestamp(new Date());
							messageReq.setAction("bap-on_search");
							messageReq.setMessage(request.getBody());
							messageReq.setMessageId(messageId);
							messageReq.setSource(this.nodeResolverService.getNodeId());
							genericDao.save(messageReq);
						}
					}
				}
			}
		} catch (Exception e) {
			LOG.error("Exception occurred while saving request of on_search callback.", e);
		}
		return new ResponseEntity<HttpStatus>(HttpStatus.OK);
	}
	
	@PostMapping(value = "/on_select")
	public ResponseEntity<HttpStatus> onSelect(RequestEntity<String> request) {
		try {
			if (null != request && StringUtils.isNotBlank(request.getBody())) {
				if(LOG.isDebugEnabled())
					LOG.debug("Buyer on_select Request Received. Request: {}", request.getBody());
				
				JSONObject json = new JSONObject(request.getBody());
				if (json.has("context")) {
					JSONObject context = json.getJSONObject("context");
					if (null != context && context.has("message_id")) {
						String messageId = context.getString("message_id");
						if (StringUtils.isNotBlank(messageId)) {
							List<SearchParams> params = new ArrayList<>();
							params.add(
									new SearchParams("messageId", Constants.QueryComparator.EQUALS, messageId.trim()));
							EventsChannelTopicEntity messageReq = new EventsChannelTopicEntity();
							messageReq.setTimestamp(new Date());
							messageReq.setAction("bap-on_select");
							messageReq.setMessage(request.getBody());
							messageReq.setMessageId(messageId);
							messageReq.setSource(this.nodeResolverService.getNodeId());
							genericDao.save(messageReq);
						}
					}
				}
			}
		} catch (Exception e) {
			LOG.error("Exception occurred while saving request of on_select callback.", e);
		}
		return new ResponseEntity<HttpStatus>(HttpStatus.OK);
	}
}