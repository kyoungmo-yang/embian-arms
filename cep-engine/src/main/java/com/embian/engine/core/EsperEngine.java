package com.embian.engine.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.camel.ProducerTemplate;
import org.apache.log4j.Logger;

import com.embian.engine.sinker.AlertSinker;
import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.ConfigurationException;
import com.espertech.esper.client.EPAdministrator;
import com.espertech.esper.client.EPRuntime;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.soda.EPStatementObjectModel;

public class EsperEngine implements EPLConfiguration {
	private static final long serialVersionUID = 6735567687808719963L;
	private static final Logger LOG = Logger.getLogger(EsperEngine.class);
	public static final String SBCACHE_EVENT_KEY = "__event";
	
	private int mailDeliveryInterval = 60;
	private static EsperEngine instance;
	
	private EPServiceProvider esperSink;
	private EPRuntime runtime;
	private EPAdministrator admin;

	private ProducerTemplate ptempl;
	
	private EsperEngine() {
		_init();
	}
	
	private void _init() {
		Configuration configuration = new Configuration();
		
		configuration.getEngineDefaults().getMetricsReporting().setEnableMetricsReporting(true);
		configuration.getEngineDefaults().getMetricsReporting().setStatementInterval(3000);
		
		this.esperSink = EPServiceProviderManager.getProvider(this.toString(), configuration);
		this.esperSink.initialize();
		this.runtime = esperSink.getEPRuntime();
		this.admin = esperSink.getEPAdministrator();
	}
	
	public synchronized static EsperEngine instance() {
		if (instance == null) {
			instance = new EsperEngine();
		}
		
		return instance;
	}
	
	public boolean createScheme(String schema) {
		if (this.admin == null) {
			throw new UnsupportedOperationException("this config is only available in runtime");
		}
		
		return (this.admin.createEPL(schema) != null);
	}
	
	public boolean createScheme(EPStatementObjectModel som) {
		if (this.admin == null) {
			throw new UnsupportedOperationException("this config is only available in runtime");
		}
		
		return (this.admin.create(som) != null);
	}
	
	public EPStatementObjectModel compileEPL(String epl) {
		if (this.admin == null) {
			throw new UnsupportedOperationException("this config is only available in runtime");
		}
		
		return this.admin.compileEPL(epl);
	}
	
	public void setMailDeliveryInterval(int sec) {
		this.mailDeliveryInterval = sec;
	}
	
	public int getMailDeliveryInterval() {
		return this.mailDeliveryInterval;
	}
	
	public synchronized boolean sendEvent(String eventType, Map<String, Object> event) {
		if (!this.hasEventType(eventType)) {
			Map<String, Object> typeMap = new HashMap<String, Object>();
			for(String key : event.keySet()) {
				typeMap.put(key, event.get(key).getClass());
			}
			this.getLogger().info(typeMap);
			this.addEventType(eventType, typeMap);
		}
		
		if (event != null && this.hasEventType(eventType)) {
//			getLogger().info(new StringBuilder("sendEvent : ").append(eventType).append(" - ").append(event).toString());
			runtime.sendEvent(event, eventType);
			
			return true;
		}
		
		getLogger().info(new StringBuilder("do not send the event : ").append(eventType).append(" - ").append(event).toString());
		return false;
	}
	
	public void setProducerTemplate(ProducerTemplate ptempl) {
		this.ptempl = ptempl;
	}
	
	protected Logger getLogger() {
		return LOG;
	}
	
///////////////////////////////////////////////////////////////////////
//	Implements EPLConfiguration
///////////////////////////////////////////////////////////////////////

	@Override
	public synchronized void addEventType(String eventTypeName, Map<String, Object> typeMap) {
		if (this.admin == null) {
			throw new UnsupportedOperationException("this config is only available in runtime");
		}

		if (this.admin.getConfiguration().getEventType(eventTypeName) != null) {
			throw new ConfigurationException("The event " + eventTypeName + " is already exist");
		}
		this.admin.getConfiguration().addEventType(eventTypeName, typeMap);
	}

	@Override
	public synchronized boolean removeEventType(String eventTypeName) {
		if (this.admin == null) {
			throw new UnsupportedOperationException("this config is only available in runtime");
		}
		
		boolean success = this.admin.getConfiguration().removeEventType(eventTypeName, false);
		
		return success;
	}

	@Override
	public EventType[] getEventTypes() {
		if (this.admin == null) {
			throw new UnsupportedOperationException("this config is only available in runtime");
		}
		return this.admin.getConfiguration().getEventTypes();
	}

	@Override
	public EventType getEventType(String eventTypeName) {
		if (this.admin == null) {
			throw new UnsupportedOperationException("this config is only available in runtime");
		}
		
		return this.admin.getConfiguration().getEventType(eventTypeName);
	}

	@Override
	public boolean hasEventType(String eventTypeName) {
		if (eventTypeName == null) {
			return false;
		}
		return (this.getEventType(eventTypeName) != null);
	}

	@Override
	public Set<String> getStatementNamesUsedBy(String eventTypeName) {
		if (this.admin == null) {
			throw new UnsupportedOperationException("this config is only available in runtime");
		}
		return this.admin.getConfiguration().getEventTypeNameUsedBy(eventTypeName);
	}

	@Override
	public synchronized EPStatement addStatement(String name, String eplStatement, String routingKey) {
		if (this.admin == null) {
			throw new UnsupportedOperationException("this config is only available in runtime");
		}

		if (this.hasStatement(name)) {
			throw new RuntimeException("The statement ("+name+") is already exist");
		}

		EPStatement stmt = this.admin.createEPL(eplStatement, name);
		if (routingKey != null) {
			if (routingKey.startsWith(AlertSinker.ROUTING_KEY_PREFIX)) {
				stmt.addListener(new AlertUpdateListener(routingKey, this.ptempl));
				stmt.addListener(new DashboardUpdateListener(routingKey, this.ptempl));
			} else {
				stmt.addListener(new DashboardUpdateListener(routingKey, this.ptempl));
			}
		}
		
		return stmt;
	}

	@Override
	public synchronized EPStatement removeStatement(String name) {
		EPStatement stmt = this.getStatement(name);
		if (stmt != null) {
			stmt.removeAllListeners();
			stmt.destroy();
		}
		return stmt;
	}

	@Override
	public synchronized EPStatement updateStatement(String name, String routingKey) {
		EPStatement stmt = this.getStatement(name);
		if (stmt != null) {
			stmt.removeAllListeners();
			if (routingKey != null) {
				if (routingKey.startsWith(AlertSinker.ROUTING_KEY_PREFIX)) {
					stmt.addListener(new AlertUpdateListener(routingKey, this.ptempl));
				} else {
					stmt.addListener(new DashboardUpdateListener(routingKey, this.ptempl));
				}
			}
			
			return stmt;
		}
		
		return null;
	}

	@Override
	public EPStatement getStatement(String name) {
		if (this.admin == null) {
			throw new UnsupportedOperationException("this config is only available in runtime");
		}
		return this.admin.getStatement(name);
	}

	@Override
	public boolean hasStatement(String name) {
		if (this.admin == null) {
			throw new UnsupportedOperationException("this config is only available in runtime");
		}
		for (String n : this.admin.getStatementNames()) {
			if (n.equals(name)) {
				return true;
			}
		}

		return false;
	}

	@Override
	public String[] getStatementNames() {
		if (this.admin == null) {
			throw new UnsupportedOperationException("this config is only available in runtime");
		}
		return this.admin.getStatementNames();
	}

	@Override
	public int numberOfStatements() {
		if (this.admin == null) {
			throw new UnsupportedOperationException("this config is only available in runtime");
		}
		return this.getStatementNames().length;
	}
}
