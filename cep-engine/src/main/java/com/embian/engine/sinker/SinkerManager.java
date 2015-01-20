package com.embian.engine.sinker;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.camel.CamelContext;
import org.apache.log4j.Logger;

public class SinkerManager {
	private static final Logger LOG = Logger.getLogger(SinkerManager.class);
	
	private static SinkerManager instance;
	private Map<String, Sinker> sinkers;
	private CamelContext context;
	
	private SinkerManager(){
		this.sinkers = new HashMap<String, Sinker>();
		this._init();
	}
	
	public static SinkerManager instance() {
		if (instance == null) {
			instance = new SinkerManager();
		}
		
		return instance;
	}
	
	private void _init() {
//		TODO : init sinkers using elasticsearch
	}
	
	public void setContext(CamelContext context) {
		this.context = context;
	}
	
	public CamelContext getContext() {
		return this.context;
	}
	
	public Sinker getSinker(String sinkerID) {
		return this.sinkers.get(sinkerID);
	}
	
	public Sinker unregistry(Sinker sinker) {
		return this.unregistry(sinker.getSinkerID());
	}
	
	public Sinker unregistry(String sinkerID) {
		Sinker sinker = this.sinkers.remove(sinkerID);
		if (sinker != null) {
			try {
				sinker.getContext().stopRoute(sinker.getSinkerID());
				sinker.getContext().removeRoute(sinker.getSinkerID());
			} catch (Exception e) {
				LOG.error(e);
			}
		}
		return sinker;
	}
	
	public Sinker registry(Sinker sinker) throws Exception {
		this.sinkers.put(sinker.getSinkerID(), sinker);
		if (sinker.getContext().getRoute(sinker.getSinkerID()) == null) {
			sinker.getContext().addRoutes(sinker);
		}
		return sinker;
	}
	
	public Collection<Sinker> sinkers() {
		return this.sinkers.values();
	}
	
	public Set<String> sinkerIDs() {
		return this.sinkers.keySet();
	} 
}
