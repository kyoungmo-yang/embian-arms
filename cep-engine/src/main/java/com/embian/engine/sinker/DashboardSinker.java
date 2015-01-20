package com.embian.engine.sinker;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.camel.CamelContext;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.log4j.Logger;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Serie;

import com.embian.engine.ServiceInitializer;
import com.embian.engine.conf.Config;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;

public class DashboardSinker extends Sinker {
	private static final Logger LOG = Logger.getLogger(DashboardSinker.class);
//	private static final String SERIE_NAME1 = ServiceInitializer.instance().getConfig().getString(Config.DASH_EVENT_SERIE_NAME);
	private static final String SERIE_NAME2 = ServiceInitializer.instance().getConfig().getString(Config.DASH_STATUS_SERIE_NAME);
	public static final String INFLUXDB_DATABASE = ServiceInitializer.instance().getConfig().getString(Config.DASH_INFLUXDB_DB);
	public static final String INFLUXDB_SHARDSPACE = ServiceInitializer.instance().getConfig().getString(Config.INFLUXDB_SHARDSPACE);
	
	public static final String EVENT_TIME_KEY = ServiceInitializer.instance().getConfig().getString(Config.DASH_EVENT_TIME_KEY);
	public static final String SERIE_NAME_KEY = ServiceInitializer.instance().getConfig().getString(Config.EVENT_NAME_KEY);
	public static final String SERIE_TIME_KEY = ServiceInitializer.instance().getConfig().getString(Config.DASH_SERIE_TIME_KEY);
	public static final String PAYLOAD_KEY = ServiceInitializer.instance().getConfig().getString(Config.DASH_EVENT_PAYLOAD_KEY);
	
	public static final String INFLUXDB_HOST 		= ServiceInitializer.instance().getConfig().getString(Config.INFLUXDB_HOST);
	public static final int INFLUXDB_PORT 			= ServiceInitializer.instance().getConfig().getInteger(Config.INFLUXDB_PORT);
	public static final String INFLUXDB_USERNAME 	= ServiceInitializer.instance().getConfig().getString(Config.INFLUXDB_USERNAME);
	public static final String INFLUXDB_ROOT_USER 	= ServiceInitializer.instance().getConfig().getString(Config.INFLUXDB_ROOT_USER);
	public static final String INFLUXDB_PASSWORD 	= ServiceInitializer.instance().getConfig().getString(Config.INFLUXDB_PASSWORD);
	
	public static final String ROUTING_KEY_PREFIX = ServiceInitializer.instance().getConfig().getString(Config.DASH_ROUTINGKEY_PREFIX);
	public static final String PRODUCER_URL_PREFIX = ServiceInitializer.instance().getConfig().getString(Config.DASH_PRODUCERURL_PREFIX);
	protected static final String SINKER_POSTFIX = ServiceInitializer.instance().getConfig().getString(Config.DASH_SINKER_POSTFIX);
	
	public static final int DASH_INFLUXDB_THREADPOOLSIZE = ServiceInitializer.instance().getConfig().getInteger(Config.DASH_INFLUXDB_THREADPOOLSIZE);
	public static final int DASH_INFLUXDB_FLUSHBUFFER = ServiceInitializer.instance().getConfig().getInteger(Config.DASH_INFLUXDB_FLUSHBUFFER);
	public static final int DASH_INFLUXDB_FLUSHINTERVAL = ServiceInitializer.instance().getConfig().getInteger(Config.DASH_INFLUXDB_FLUSHINTERVAL);
	
	public static final String HTTP_PREFIX = "http://";
	
	private InfluxDB influxDB;
	/**
	 * for camel bean
	 */
	public DashboardSinker() {
		super();
	}
	
	public DashboardSinker(CamelContext context) {
		super(context);
	}
	
	public static String generateSinkerID(String ... prefiexs) {
		return new StringBuffer(Joiner.on(".").join(prefiexs)).append(SINKER_POSTFIX).toString();
	}
	
	public static boolean registryDashSinker(String routingKey) {
		try {
			DashboardSinker sinker = new DashboardSinker();
			String sinkerID = DashboardSinker.generateSinkerID(DashboardSinker.PRODUCER_URL_PREFIX, routingKey);
	    	
	    	sinker.from(DashboardSinker.PRODUCER_URL_PREFIX+routingKey)
	    	       .routeId(sinkerID)
			       .to("class:"+DashboardSinker.class.getCanonicalName()+"?method=sink2");
	    	
			SinkerManager.instance().registry(sinker);
			
			return true;
		} catch (Exception e) {
			LOG.error(e);
			
			return false;
		}
	}
	
	public static boolean unregistryDashSinker(String routingKey) {
		String sinkerID = DashboardSinker.generateSinkerID(DashboardSinker.PRODUCER_URL_PREFIX, routingKey);
		Sinker sinker = SinkerManager.instance().unregistry(sinkerID);
		
		return (sinker != null); 
	}
	
	public void sink(String strEvent) throws JsonParseException, JsonMappingException, IOException {
		throw new NotImplementedException("Not supported");
//		if (cnt == 0 || s == 0) {
//			s = System.currentTimeMillis();
//		}
//		
//		Map<String,Object> event = new HashMap<String, Object>();
//   	 
//    	ObjectMapper objectMapper = new ObjectMapper();
//    	event = objectMapper.readValue(strEvent, HashMap.class);
//    	
//    	Object timestamp = event.get(EVENT_TIME_KEY);
//    	Object serieName = event.get(SERIE_NAME_KEY);
//    	Map<String, Object> payload = (Map<String, Object>) event.get(PAYLOAD_KEY);
//    	if (timestamp != null && payload != null && serieName != null) {
//    		payload.put(SERIE_TIME_KEY, timestamp);
//    		
//    		Object[] values = payload.values().toArray();
//    		String[] columns = payload.keySet().toArray(new String[payload.keySet().size()]);
//			Serie serie = new Serie.Builder(serieName.toString())
//			                       .columns(columns)
//			                       .values(values)
//			                       .build();
//			
//			if (influxDB == null || !(influxDB.ping() instanceof Pong)) {
//				influxDB = InfluxDBFactory.connect(new StringBuffer(HTTP_PREFIX).append(INFLUXDB_HOST).append(':').append(INFLUXDB_PORT).toString(), INFLUXDB_USERNAME, INFLUXDB_PASSWORD);
//			}
//			
//			influxDB.write(INFLUXDB_DATABASE, TimeUnit.SECONDS, serie);
//			
//			cnt++;
//			if (cnt >= 5000) {
//				e = System.currentTimeMillis();
//				LOG.info(new StringBuffer().append(this).append('-').append(cnt).append(':').append(e-s));
//				cnt = 0;
//				s = 0;
//			}
//		} else {
//			LOG.info(new StringBuffer("type is undefined : ").append(strEvent));
//		}
	}
	
	@SuppressWarnings("unchecked")
	public void sink2(String strEvent) throws JsonParseException, JsonMappingException, IOException {
		Map<String,Object> event = new HashMap<String, Object>();
   	 
    	ObjectMapper objectMapper = new ObjectMapper();
    	event = objectMapper.readValue(strEvent, HashMap.class);
    	
		String[] columns = event.keySet().toArray(new String[event.keySet().size()]);
		Serie serie = new Serie.Builder(SERIE_NAME2)
		                       .columns(columns)
		                       .values(event.values().toArray())
		                       .build();
		
		if (influxDB == null) {
			influxDB = InfluxDBFactory.connect(new StringBuffer(HTTP_PREFIX).append(INFLUXDB_HOST).append(':').append(INFLUXDB_PORT).toString(), INFLUXDB_USERNAME, INFLUXDB_PASSWORD);
		}
		
		influxDB.write(INFLUXDB_DATABASE, TimeUnit.SECONDS, serie);
	}
}
