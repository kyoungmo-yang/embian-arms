package com.embian.engine.sinker;

import org.apache.camel.CamelContext;
import org.apache.log4j.Logger;

import com.embian.engine.ServiceInitializer;
import com.embian.engine.conf.Config;
import com.google.common.base.Joiner;

public class AlertSinker extends Sinker {
	private static final Logger LOG = Logger.getLogger(AlertSinker.class);
	
	public static final String ROUTING_KEY_PREFIX = ServiceInitializer.instance().getConfig().getString(Config.ALERT_ROUTINGKEY_PREFIX);
	public static final String PRODUCER_URL_PREFIX = ServiceInitializer.instance().getConfig().getString(Config.ALERT_PRODUCERURL_PREFIX);
	protected static final String SINKER_POSTFIX = ServiceInitializer.instance().getConfig().getString(Config.ALERT_SINKER_POSTFIX);
	private static final String MAIL_SENDER = ServiceInitializer.instance().getConfig().getString(Config.EMAIL_SENDER);
	
	private static final String RABBITMQ_URL_PREFIX = "rabbitmq://";
	private static final String MAIL_URL_PREFIX = "smtp://";
	
	/**
	 * for camel bean
	 */
	public AlertSinker() {
		super();
	}
	
	public AlertSinker(CamelContext context) {
		super(context);
	}
	
	public void sink(String data) {
		
	}
	
	
	public static String generateSinkerID(String ... prefiexs) {
		return new StringBuffer(Joiner.on(".").join(prefiexs)).append(SINKER_POSTFIX).toString();
	}
	
	public static boolean registryAlertSinker(String routingKey, String toMail) {
		try {
			Config conf = ServiceInitializer.instance().getConfig();
			AlertSinker asinker = new AlertSinker();
	    	String sinkerID = AlertSinker.generateSinkerID(AlertSinker.PRODUCER_URL_PREFIX, routingKey);
	    	String to = new StringBuffer(RABBITMQ_URL_PREFIX)
			            .append(conf.getString(Config.RABBITMQ_HOST)).append('/').append(conf.getString(Config.RABBITMQ_OUTPUT_EXCHANGE))
			            .append("?routingKey=")		.append(routingKey).append('&')
			            .append("exchangeType=")	.append(conf.getString(Config.RABBITMQ_EXCHANGETYPE)).append('&')
			            .append("username=")		.append(conf.getString(Config.RABBITMQ_USERNAME)).append('&')
			            .append("password=")		.append(conf.getString(Config.RABBITMQ_PASSWORD)).append('&')
			            .append("durable=")			.append(conf.getBoolean(Config.RABBITMQ_DURABLE)).append('&')
			            .append("autoDelete=")		.append(conf.getBoolean(Config.RABBITMQ_AUTODELETE)).append('&')
			            .append("threadPoolSize=")	.append(conf.getInteger(Config.RABBITMQ_THREADPOOLSIZE)).toString();
	    	
	    	asinker.from(AlertSinker.PRODUCER_URL_PREFIX+routingKey)
	    	       .routeId(sinkerID)
	    	       .to(to);
    	
			SinkerManager.instance().registry(asinker);
			
			EmailSinker esinker = new EmailSinker();
			String from = new StringBuffer(RABBITMQ_URL_PREFIX)
			            .append(conf.getString(Config.RABBITMQ_HOST)).append('/').append(conf.getString(Config.RABBITMQ_OUTPUT_EXCHANGE))
			            .append("?routingKey=")		.append(routingKey).append('&')
			            .append("exchangeType=")	.append(conf.getString(Config.RABBITMQ_EXCHANGETYPE)).append('&')
			            .append("username=")		.append(conf.getString(Config.RABBITMQ_USERNAME)).append('&')
			            .append("password=")		.append(conf.getString(Config.RABBITMQ_PASSWORD)).append('&')
			            .append("durable=")			.append(conf.getBoolean(Config.RABBITMQ_DURABLE)).append('&')
			            .append("autoDelete=")		.append(conf.getBoolean(Config.RABBITMQ_AUTODELETE)).append('&')
			            .append("threadPoolSize=")	.append(conf.getInteger(Config.RABBITMQ_THREADPOOLSIZE)).toString();
			
			to = new StringBuffer(MAIL_URL_PREFIX)
			            .append(conf.getString(Config.EMAIL_SMTP_HOST))
			            .append("?from=")			.append(MAIL_SENDER).append('&')
			            .append("to=")				.append(toMail).append('&')
			            .append("username=")		.append(conf.getString(Config.EMAIL_SMTP_USERNAME)).append('&')
			            .append("password=")		.append(conf.getString(Config.EMAIL_SMTP_PASSWORD)).append('&')
			            .append("subject=")			.append("SRM 모니터링 알림 메시지").append('&')
			            .append("contentType=")		.append("text/html").toString();
			
	    	sinkerID = EmailSinker.generateSinkerID(AlertSinker.PRODUCER_URL_PREFIX, routingKey);
	    	esinker.from(from)
	    	       .routeId(sinkerID)
	    	       .to(to);
	    	SinkerManager.instance().registry(esinker);
	    	
	    	return true;
		} catch (Exception e) {
			LOG.error(e);
			
			return false;
		}
	}
	
	public static boolean unregistryAlertSinker(String routingKey) {
		String asinkerID = AlertSinker.generateSinkerID(AlertSinker.PRODUCER_URL_PREFIX, routingKey);
		String esinkerID = EmailSinker.generateSinkerID(AlertSinker.PRODUCER_URL_PREFIX, routingKey);
		Sinker asinker = SinkerManager.instance().unregistry(asinkerID);
		Sinker esinker = SinkerManager.instance().unregistry(esinkerID);
		
		return (asinker != null && esinker != null); 
	}
}
