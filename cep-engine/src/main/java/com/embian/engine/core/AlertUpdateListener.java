package com.embian.engine.core;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.ProducerTemplate;
import org.apache.log4j.Logger;

import com.embian.engine.ServiceInitializer;
import com.embian.engine.conf.Config;
import com.embian.engine.sinker.AlertSinker;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventPropertyDescriptor;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.StatementAwareUpdateListener;
import com.espertech.esper.client.annotation.Name;

public class AlertUpdateListener implements StatementAwareUpdateListener {
	private static final Logger LOG = Logger.getLogger(AlertUpdateListener.class);
	
	private static final int MILLISECOND_UNIT = 1000;
	private static final int MAIL_DELIVERY_TIME = ServiceInitializer.instance().getConfig().getInteger(Config.EMAIL_DELIVERY_INTERVAL) * MILLISECOND_UNIT;
	private static final String DEFAULT_NOTIF_TITLE = "SBArm Notification";
	private static final String DUPLICATE_CHK_FIELD1 = "service";
	private static final String DUPLICATE_CHK_FIELD2 = "hostname";
	private static final String DUPLICATE_CHK_FIELD3 = "request";
	private static final String EMPTY_STR = "";
	private static final String COLON = ": ";
	private static final String DASH = "-";

	private String routingKey;
	private String camelKey;
	private ProducerTemplate ptempl;
	private Map<String, Long> deliverMap;
	private String title;

	public AlertUpdateListener(String routingKey, ProducerTemplate ptempl) {
		this.routingKey = routingKey;
		this.camelKey = AlertSinker.PRODUCER_URL_PREFIX + routingKey;
		this.ptempl = ptempl;
		this.deliverMap = new HashMap<String, Long>();
		this.title = null;
	}

	public String getRoutingKey() {
		return this.routingKey;
	}

	@Override
	public void update(EventBean[] newEvents, EventBean[] oldEvents, EPStatement stmt, EPServiceProvider provider) {
		try {
			if (newEvents == null || routingKey == null) {
				return;
			}

			if (this.title == null) {
				title = DEFAULT_NOTIF_TITLE;
				for (Annotation annotation : stmt.getAnnotations()) {
					if (annotation instanceof Name) {
						title = ((Name)annotation).value();
						break;
					}
				}			
			}

			if (newEvents != null) {
				String content = renderToHtml(this.title, newEvents);
				if (content != null && content.length() > 0) {
					ptempl.sendBody(camelKey, content);
				}
			}
		} catch (Exception e) {
			LOG.error(e);
		}
	}
	
//////////////////////////////////////////////////////////////////////////////////////////////////////////////
	private static final String S_HEADER = "<html><head><meta http-equiv='Content-Type' content='text/html; charset=utf-8'></head>";
	private static final String E_HEADER = "</html>";
	
	private static final String S_P = "<p><b>";
	private static final String E_P = "</b></p>";
	
	private static final String S_DIV = "<div style='border:1px solid c3c3c4; float:left'>";
	private static final String E_DIV = "</div>";
	
	private static final String S_UL = "<ul>";
	private static final String E_UL = "</ul>";
	
	private static final String S_LI = "<li>";
	private static final String E_LI = "</li>";
	
	private String renderToHtml(String title, EventBean[] events) {
		int contentLen = 0;
		StringBuilder builder = new StringBuilder();
		builder.append(S_HEADER);
		// title
		builder.append(S_P).append(title).append(E_P);
		// body
		if (events != null) {
			int len = events.length;
			String content = null;
			for (int i = 0; i < len; i++) {
				content = this.renderToHtml(events[i]);
				if (content != null) {
					builder.append(S_DIV);
					builder.append(S_UL);
					builder.append(content);
					builder.append(E_UL);
					builder.append(E_DIV);
					contentLen++;
				}
			}
		}
		
		if(contentLen <= 0) {
			return null;
		}
		
		builder.append(E_HEADER);
		return builder.toString();
	}
	
	private String renderToHtml(EventBean bean) {
		EventType type = bean.getEventType();
		// service
		EventPropertyDescriptor p1 = type.getPropertyDescriptor(DUPLICATE_CHK_FIELD1);
		// hostname
		EventPropertyDescriptor p2 = type.getPropertyDescriptor(DUPLICATE_CHK_FIELD2);
		// request
		EventPropertyDescriptor p3 = type.getPropertyDescriptor(DUPLICATE_CHK_FIELD3);
		
		if (p1 != null && p2 != null) {
			Object v1 = bean.get(p1.getPropertyName());
			Object v2 = bean.get(p2.getPropertyName());
			Object v3 = p3 == null ? EMPTY_STR : bean.get(p3.getPropertyName());
			
			String deliverKey = new StringBuffer(v1.toString()).append(DASH).append(v2.toString()).append(DASH).append(v3.toString()).toString();
			Long pretimestamp = this.deliverMap.get(deliverKey);
			long timestamp = System.currentTimeMillis();
			if(pretimestamp != null && (timestamp - pretimestamp) < MAIL_DELIVERY_TIME) {
				// 대량의 (중복)매일 발송을 막기 위함
				return null;
			}
			
			this.deliverMap.put(deliverKey, System.currentTimeMillis());
		}
		
		StringBuilder builder = new StringBuilder();
		EventPropertyDescriptor[] descs = type.getPropertyDescriptors();
		for (EventPropertyDescriptor desc : descs) {
			builder.append(S_LI);
			builder.append(desc.getPropertyName()).append(COLON).append(bean.get(desc.getPropertyName()));
			builder.append(E_LI);
		}
		
		return builder.toString();
	}
}