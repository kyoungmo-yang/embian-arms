package com.embian.engine.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.embian.engine.core.EsperEngine;
import com.embian.engine.utils.RESTReturn;
import com.espertech.esper.client.EventPropertyDescriptor;
import com.espertech.esper.client.EventType;

@RestController
public class EventController {
	private static final Logger LOG = Logger.getLogger(EventController.class);
	
	@RequestMapping(value = "/list_events", method = RequestMethod.POST)
    public @ResponseBody Map<String,? extends Object> listEvents() {
    	EsperEngine engine = EsperEngine.instance();
    	
    	List<Map<String, Object>> events = new Vector<Map<String, Object>>(); 
    	
    	EventType[] types = engine.getEventTypes();
    	for (EventType type : types) {
			Map<String, Object> mtype = new HashMap<String, Object>();
			mtype.put("name", type.getName());
			
			
			EventPropertyDescriptor[] descs = type.getPropertyDescriptors();
			Map<String, Object> mprops = new HashMap<String, Object>();
			for (EventPropertyDescriptor desc : descs) {
				String regex = new StringBuffer(desc.getPropertyType().getPackage().getName()).append('.').toString();
				mprops.put(desc.getPropertyName(), desc.getPropertyType().getName().replaceFirst(regex, "").toLowerCase());
			}
			mtype.put("properties", mprops);
			events.add(mtype);
		}
    	
        return RESTReturn.mapOK(events);
    }
}
