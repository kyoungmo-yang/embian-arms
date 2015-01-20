package com.embian.engine.conf;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class Config {
	//esper events
	public static final String ESPER_EVENT_SCHEMA			= "esper.event.schema.*";
		
	//influxdb properties
	public static final String INFLUXDB_HOST				= "influxdb.host";
	public static final String INFLUXDB_PORT				= "influxdb.port";
	public static final String INFLUXDB_USERNAME			= "influxdb.username";
	public static final String INFLUXDB_ROOT_USER			= "influxdb.root.user";
	public static final String INFLUXDB_PASSWORD			= "influxdb.password";
	public static final String INFLUXDB_SHARDSPACE			= "influxdb.shardspace";
	
	//camel context properties
	public static final String CAMEL_TRACING				= "camel.tracing";
	
	//elasticsearch
	public static final String ELASTIC_CLUSTER				= "elastic.cluster";
	public static final String ELASTIC_OPERATION			= "elastic.operation";
	public static final String ELASTIC_INDEXNAME			= "elastic.indexname";
	public static final String ELASTIC_INDEXTYPE			= "elastic.indextype";
	public static final String ELASTIC_IP					= "elastic.ip";
	public static final String ELASTIC_PORT					= "elastic.port";
	
	//camel-rabbitmq properties
	public static final String RABBITMQ_USERNAME 			= "rabbitmq.username";
	public static final String RABBITMQ_PASSWORD 			= "rabbitmq.password";
	public static final String RABBITMQ_HOST 				= "rabbitmq.host";
	public static final String RABBITMQ_EXCHANGETYPE 		= "rabbitmq.exchangeType";
	public static final String RABBITMQ_DURABLE 			= "rabbitmq.durable";
	public static final String RABBITMQ_AUTODELETE 			= "rabbitmq.autoDelete";
	public static final String RABBITMQ_THREADPOOLSIZE 		= "rabbitmq.threadPoolSize";
	public static final String RABBITMQ_INPUT_EXCHANGE		= "rabbitmq.input.exchange";
	public static final String RABBITMQ_INPUT_ROUTINGKEY 	= "rabbitmq.input.routingKey";
	public static final String RABBITMQ_OUTPUT_EXCHANGE		= "rabbitmq.output.exchange";

	//camel event route properteis
	public static final String EVENT_NAME_KEY 				= "event.name.key";
	public static final String EVENT_PAYLOAD_KEY 			= "event.payload.key";

	//camel alert sinker properties
	public static final String ALERT_ROUTINGKEY_PREFIX 		= "alert.routingkey.prefix";
	public static final String ALERT_PRODUCERURL_PREFIX 	= "alert.producerurl.prefix";
	public static final String ALERT_SINKER_POSTFIX 		= "alert.sinker.postfix";

	//camel dashboard sinker properties
	public static final String DASH_INFLUXDB_THREADPOOLSIZE = "dash.influxdb.threadPoolSize";
	public static final String DASH_INFLUXDB_FLUSHBUFFER	= "dash.influxdb.flushbuffer";
	public static final String DASH_INFLUXDB_FLUSHINTERVAL	= "dash.influxdb.flushinterval";
			
	public static final String DASH_INFLUXDB_DB 			= "dash.influxdb.db";
	public static final String DASH_EVENT_TIME_KEY 			= "dash.event.time.key";
	public static final String DASH_EVENT_PAYLOAD_KEY 		= "dash.event.payload.key";
	public static final String DASH_EVENT_SERIE_NAME 		= "dash.event.serie.name";
	public static final String DASH_STATUS_SERIE_NAME		= "dash.status.serie.name";
	public static final String DASH_SERIE_TIME_KEY 			= "dash.serie.time.key";
	public static final String DASH_ROUTINGKEY_PREFIX		= "dash.routingkey.prefix";
	public static final String DASH_PRODUCERURL_PREFIX		= "dash.producerurl.prefix";
	public static final String DASH_SINKER_POSTFIX			= "dash.sinker.postfix";
	
	//camel email sinker properties
	public static final String EMAIL_SINKER_POSTFIX 		= "email.sinker.postfix";
	public static final String EMAIL_DELIVERY_INTERVAL		= "email.delivery.interval";
	public static final String EMAIL_SMTP_HOST				= "email.smtp.host";
	public static final String EMAIL_SMTP_PORT				= "email.smtp.port";
	public static final String EMAIL_SENDER					= "email.sender";
	public static final String EMAIL_SMTP_USERNAME 			= "email.smtp.username";
	public static final String EMAIL_SMTP_PASSWORD 			= "email.smtp.password";
	
	//authentication
	public static final String AUTH_USERNAME				= "auth.username";
	public static final String AUTH_PASSWORD				= "auth.password";
	
	//memcached
	public static final String CACHE_SHORT_TERM_INTERVAL 	= "memcached.short_term";
	public static final String CACHE_LONG_TERM_INTERVAL 	= "memcached.long_term";
	public static final String MEMCACHED_HOSTNAME			= "memcached.hostname";
	public static final String MEMCACHED_PORT				= "memcached.port";

	//hostname
	public static final String ACCESS_CONTROL_ALLOW_ORIGIN	= "access.control.allow.origin";

	@Autowired
	private Environment env;
	
	public void setEnv(Environment env) {
		this.env = env;
	}
	
	public String getString(String key) {
		return this.env.getProperty(key);
	}
	
	public Boolean getBoolean(String key) {
		return this.env.getProperty(key, Boolean.class);
	}
	
	public Integer getInteger(String key) {
		return this.env.getProperty(key, Integer.class);
	}
	
	@SuppressWarnings("unchecked")
	public List<String> getList(String key) {
		if (key.indexOf('*') > 0) {
			return this._getList(key);
		}
		return this.env.getProperty(key, List.class);
	}
	
	private List<String> _getList(String key) {
		List<String> p = new ArrayList<String>();
		int i = 1;
		String prop = null;
		while ( (prop = this.getString(key.replace("*", ""+i))) != null ) {
			p.add(prop);
			i++;
		}
		
		return p;
	}
}
