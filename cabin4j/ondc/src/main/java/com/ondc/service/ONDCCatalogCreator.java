package com.ondc.service;

import java.util.List;

import org.json.JSONObject;

import com.xhopfront.entities.SKU;

public interface ONDCCatalogCreator {

	public JSONObject createCatalog(List<SKU> skus);
	
}
