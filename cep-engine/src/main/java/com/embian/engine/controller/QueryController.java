package com.embian.engine.controller;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.influxdb.dto.Serie;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.embian.engine.memcached.SBCache;

@RestController
public class QueryController {
	private static final Logger LOG = Logger.getLogger(QueryController.class);
	private SBCache sbcache;
	
	private void _initSBCache() {
		if (sbcache == null) {
			sbcache = new SBCache();
		}
	}
	
	@RequestMapping(value = "/query")
    public @ResponseBody List<Serie> query(@RequestParam String database, @RequestParam String query, @RequestParam(required=false) boolean cache) throws Exception {
		List<Serie> series = null;
		try {
			this._initSBCache();
			series = sbcache.query(database, query, cache);
		} catch (Exception e) {
			LOG.error(e);
			series = new ArrayList<Serie>();
		}
		
    	return series;
    }
}
