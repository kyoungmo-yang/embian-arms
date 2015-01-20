package com.embian.engine.sinker;

import org.apache.camel.CamelContext;
import org.apache.log4j.Logger;

import com.embian.engine.ServiceInitializer;
import com.embian.engine.conf.Config;
import com.google.common.base.Joiner;

public class EmailSinker extends Sinker {
	private static final Logger LOG = Logger.getLogger(EmailSinker.class);
	private static final String SINKER_POSTFIX = ServiceInitializer.instance().getConfig().getString(Config.EMAIL_SINKER_POSTFIX);
	
	/**
	 * for camel bean
	 */
	public EmailSinker() {
		super();
	}
	
	public EmailSinker(CamelContext context) {
		super(context);
	}
	
	public static String generateSinkerID(String ... prefiexs) {
		return new StringBuffer(Joiner.on(".").join(prefiexs)).append(SINKER_POSTFIX).toString();
	}
	
	public void sink(String data) {
		
	}
}
