package com.embian.engine.core;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.ProducerTemplate;
import org.apache.log4j.Logger;

import com.embian.engine.sbarm.SBArmManager;
import com.embian.engine.sinker.AlertSinker;
import com.embian.engine.sinker.DashboardSinker;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.StatementAwareUpdateListener;
import com.espertech.esper.client.annotation.Description;
import com.espertech.esper.client.annotation.Name;
import com.espertech.esper.dataflow.ops.LogSink.RenderingOptions;

public class DashboardUpdateListener implements StatementAwareUpdateListener {
	private static final Logger LOG = Logger.getLogger(DashboardUpdateListener.class);
	
	private String routingKey;
	private String camelKey;
	private ProducerTemplate ptempl;
	private JSONRendererImpl renderer;
	private String title;
	
	private Map<String, String> additional;
	private boolean requiredFieldChecked = false;
	
	private static final String LEVEL = "level";
	private static final String MESSAGE = "message";

	public DashboardUpdateListener(String routingKey, ProducerTemplate ptempl) {
		this.routingKey = routingKey;
		this.camelKey = DashboardSinker.PRODUCER_URL_PREFIX + routingKey;
		this.ptempl = ptempl;
		this.title = null;
		this.additional = new HashMap<String, String>();
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
			
			if (this.renderer == null) {
				this.renderer = new JSONRendererImpl(stmt, RenderingOptions.getJsonOptions());
			}
			
			if (this.title == null) {
				for (Annotation annotation : stmt.getAnnotations()) {
					if (annotation instanceof Name) {
						this.title = ((Name)annotation).value();
						break;
					}
				}
			}
			
			if (this.requiredFieldChecked == false) {
				if (routingKey.startsWith(AlertSinker.ROUTING_KEY_PREFIX)) {
					if (!stmt.getEventType().isProperty(LEVEL)) {
						this.additional.put(LEVEL, SBArmManager.instance().getLevel(stmt.getName()));
					}
					
					if (!stmt.getEventType().isProperty(MESSAGE)) {
						String desc = "";
						for (Annotation annotation : stmt.getAnnotations()) {
							if (annotation instanceof Description) {
								this.title = ((Description)annotation).value();
								break;
							}
						}
						
						this.additional.put(MESSAGE, desc);
					}
				}
				this.requiredFieldChecked = true;
			}

			if (newEvents != null) {
				for (EventBean bean : newEvents) {
					ptempl.sendBody(camelKey, this.renderer.render(this.title, bean, this.additional));
				}
			}
		} catch (Exception e) {
			LOG.error(e);
		}
	}
}