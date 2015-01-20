package com.embian.engine.core;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.embian.engine.ServiceInitializer;
import com.embian.engine.conf.Config;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class EventRouter {
	private static final Logger LOG = Logger.getLogger(EventRouter.class);
	public static final String EVENT_NAME_KEY = ServiceInitializer.instance().getConfig().getString(Config.EVENT_NAME_KEY);
	public static final String PAYLOAD_KEY = ServiceInitializer.instance().getConfig().getString(Config.EVENT_PAYLOAD_KEY);
	private static final String MESSAGE_FIELD = "message";
	
    @SuppressWarnings("unchecked")
	public void routeToEsper(String strEvent) throws JsonParseException, JsonMappingException, IOException {
    	Map<String,Object> event = new HashMap<String, Object>();
    	 
    	ObjectMapper objectMapper = new ObjectMapper();
    	event = objectMapper.readValue(strEvent, HashMap.class);

    	Map<String, Object> payload = (Map<String, Object>) event.get(PAYLOAD_KEY);
    	Object type = event.get(EVENT_NAME_KEY);
    	if (payload != null && type != null) {
    		
    		if(payload.containsKey(MESSAGE_FIELD)) {
    			payload.remove(MESSAGE_FIELD);
    		}
    		
    		EsperEngine.instance().sendEvent(type.toString(), payload);
		} else {
			LOG.info(new StringBuffer("type is undefined : ").append(strEvent));
		}
    }
}
