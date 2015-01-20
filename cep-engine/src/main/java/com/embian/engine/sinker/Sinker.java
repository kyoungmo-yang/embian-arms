package com.embian.engine.sinker;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.RouteDefinition;
import org.apache.log4j.Logger;

public class Sinker extends RouteBuilder {
	protected static final Logger LOG = Logger.getLogger(Sinker.class);
	
	protected RouteDefinition rDef;
	protected boolean hasFrom; 
	
	/**
	 * for camel bean
	 */
	public Sinker() {
		this(SinkerManager.instance().getContext());
	}
	
	public Sinker(CamelContext context) {
		super(context);
		this.hasFrom = false;
	}
	
	public String getSinkerID() {
		if (this.rDef != null) {
			return this.rDef.getId();
		}
		return null;
	}
	
	public RouteDefinition getRouteDef() {
		return this.rDef;
	}
	
	private RouteDefinition _routeId(RouteDefinition def) {
		this.hasFrom = true;
		this.rDef = def;
		return def;
	}
	
	@Override
	public RouteDefinition from(Endpoint endpoint) {
		if (this.hasFrom) {
			throw new RuntimeException("multiple source(from) not supported");
		}
		return _routeId(super.from(endpoint));
	}
	
	@Override
	public RouteDefinition from(Endpoint... endpoints) {
		if (this.hasFrom) {
			throw new RuntimeException("multiple source(from) not supported");
		}
		return _routeId(super.from(endpoints));
	}
	
	@Override
	public RouteDefinition from(String... uris) {
		if (this.hasFrom) {
			throw new RuntimeException("multiple source(from) not supported");
		}
		return _routeId(super.from(uris));
	}
	
	@Override
	public RouteDefinition fromF(String uri, Object... args) {
		if (this.hasFrom) {
			throw new RuntimeException("multiple source(from) not supported");
		}
		return _routeId(super.fromF(uri, args));
	}
	
	@Override
	public RouteDefinition from(String uri) {
		if (this.hasFrom) {
			throw new RuntimeException("multiple source(from) not supported");
		}
		return _routeId(super.from(uri));
	}
	
	@Override
	public void configure() throws Exception {
		// do nothing
	}
}
