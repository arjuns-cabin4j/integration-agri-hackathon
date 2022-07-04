package com.ondc.controllers;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.commons.collections4.CollectionUtils;
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
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.cabin4j.suite.entity.redis.EventsChannelTopicEntity;
import com.cabin4j.suite.platform.constants.Constants;
import com.cabin4j.suite.platform.dao.GenericDao;
import com.cabin4j.suite.platform.data.SearchParams;
import com.cabin4j.suite.platform.services.impl.NodeResolverService;
import com.ondc.endpoints.GenericRestEndPoint;
import com.ondc.service.ContextulizeSearch;
import com.ondc.service.ONDCCatalogCreator;
import com.xhopfront.entities.SKU;

@ConditionalOnExpression("${ondc.is.seller:false}")
@Controller
@RequestMapping("${ondc.seller.subscriber.base.url}")
public class ONDCSellerAppController {

	private final static SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

	private final Logger LOG = LoggerFactory.getLogger(ONDCSellerAppController.class);

	@Value("${ondc.on.search.gateway.url}")
	protected String gatewayURL;

	@Autowired
	@Qualifier("sellerRestEndPoint")
	private GenericRestEndPoint genericRestEndPoint;

	@Autowired
	private GenericDao genericDao;

	@Autowired
	private EntityManager em;

	@Autowired
	private NodeResolverService nodeResolverService;

	@Autowired
	private ContextulizeSearch contextulizeSearch;

	@Autowired
	private ONDCCatalogCreator ondcCatalogCreator;

	@PostMapping("/search")
	public ResponseEntity<String> search(RequestEntity<String> request) {
		if (null != request) {
			if(LOG.isDebugEnabled())
				LOG.debug("Seller search Request Received. Request: {}", request.getBody());
		} else {
			LOG.error("Seller Request is blank!");
			return new ResponseEntity<String>("Seller Request is blank!", HttpStatus.BAD_REQUEST);
		}
		try {
			/*
			 * System.out.println("------------------------------------------");
			 * System.out.println("incoming request");
			 * System.out.println(request.getBody());
			 * System.out.println("------------------------------------------");
			 */
			JSONObject incomingReq = new JSONObject(request.getBody());
			JSONObject context = incomingReq.getJSONObject("context");
			JSONObject message = incomingReq.getJSONObject("message");
			JSONObject criteria = message.getJSONObject("criteria");
			String query = criteria.getString("search_string");
			Query q = em.createNativeQuery(
					"SELECT * FROM ondc.sku WHERE MATCH (code,name,description) AGAINST (:search IN NATURAL LANGUAGE MODE) ",
					SKU.class);
			q.setParameter("search", contextulizeSearch.contextulizedSearch(query, true));
			q.setMaxResults(20);
			List<SKU> skus = q.getResultList();
			if (CollectionUtils.isNotEmpty(skus)) {
				String messageId = context.getString("message_id");

				EventsChannelTopicEntity messageReq = new EventsChannelTopicEntity();
				Date timestamp = new Date();
				messageReq.setTimestamp(timestamp);
				messageReq.setAction("bpp-search");
				messageReq.setMessage(incomingReq.toString());
				messageReq.setMessageId(messageId);
				messageReq.setSource(this.nodeResolverService.getNodeId());
				genericDao.save(messageReq);

				context.put("action", "on_search");
				context.put("transaction_id", UUID.randomUUID().toString());
				context.put("timestamp", format.format(timestamp));
				context.put("bpp_uri", "https://seller.xhopfront.com/seller/v1");
				context.put("bpp_id", "seller.xhopfront.com");
				context.put("ttl", "PT30S");
				message.remove("criteria");
				message.put("catalog", ondcCatalogCreator.createCatalog(skus));

				EventsChannelTopicEntity generatedReq = new EventsChannelTopicEntity();
				generatedReq.setTimestamp(new Date());
				generatedReq.setAction("bpp-on_search");
				generatedReq.setMessage(incomingReq.toString());
				generatedReq.setMessageId(messageId);
				generatedReq.setSource(this.nodeResolverService.getNodeId());
				genericDao.save(generatedReq);
				/*
				 * System.out.println("------------------------------------------");
				 * System.out.println("generated request");
				 * System.out.println(incomingReq.toString());
				 * System.out.println("------------------------------------------");
				 */
				
				if(LOG.isDebugEnabled())
					LOG.debug("Seller on_search Request Initiated.", incomingReq.toString());

				ResponseEntity<String> response = genericRestEndPoint.onCall(incomingReq.toString(), gatewayURL);
				if (null == response || StringUtils.isBlank(response.getBody()))
					throw new Exception("Empty response received for request: " + incomingReq.toString());

				if (LOG.isDebugEnabled())
					LOG.debug("Seller on_search Request Acknowledged. MessageId: {} \n Response: {}", messageId, response.getBody());

				EventsChannelTopicEntity messageRes = new EventsChannelTopicEntity();
				messageRes.setTimestamp(new Date());
				messageRes.setAction("bap-on_search-ack");
				messageRes.setMessage(response.getBody());
				messageRes.setMessageId(messageId);
				messageRes.setSource(this.nodeResolverService.getNodeId());
				genericDao.save(messageRes);

				/*
				 * System.out.println("------------------------------------------");
				 * System.out.println("recieved response");
				 * System.out.println(response.getBody());
				 * System.out.println("------------------------------------------");
				 */

				return new ResponseEntity<String>(messageId, HttpStatus.OK);
			} else {
				return new ResponseEntity<String>("not found", HttpStatus.OK);
			}

		} catch (Exception e) {
			LOG.error("Exception occurred while sending buyer search request.", e);
			return new ResponseEntity<String>("Exception occurred while sending buyer search request.",
					HttpStatus.INTERNAL_SERVER_ERROR);
		}

	}

	@PostMapping(value = "/select")
	public ResponseEntity<HttpStatus> select(RequestEntity<String> request) {
		try {
			if (null != request && StringUtils.isNotBlank(request.getBody())) {
				if(LOG.isDebugEnabled())
					LOG.debug("Seller select Request Received. Request: {}", request.getBody());
				
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
