package com.embian.engine.core;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;

import org.apache.log4j.Logger;

import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.util.EventPropertyRenderer;
import com.espertech.esper.client.util.EventPropertyRendererContext;
import com.espertech.esper.client.util.JSONEventRenderer;
import com.espertech.esper.client.util.JSONRenderingOptions;
import com.espertech.esper.event.util.EventTypePropertyPair;
import com.espertech.esper.event.util.GetterPair;
import com.espertech.esper.event.util.NestedGetterPair;
import com.espertech.esper.event.util.OutputValueRenderer;
import com.espertech.esper.event.util.RendererMeta;
import com.espertech.esper.event.util.RendererMetaOptions;

/**
 * Esper update결과를 json으로 변환하기 위한 클래스
 */
// public final class JSONRendererImpl2 extends JSONRendererImpl implements
// Serializable {
public final class JSONRendererImpl implements JSONEventRenderer, Serializable {
	private static final Logger LOG = Logger.getLogger(JSONRendererImpl.class);

	private static final long serialVersionUID = 7942694675267293242L;
	private static final String NEW_EVENTS_PREFIX = "newEvents";
	private static final String OLD_EVENTS_PREFIX = "oldEvents";
	private static final char LB = '{';
	private static final char RB = '}';
	private static final char LBR = '[';
	private static final char RBR = ']';
	private static final char COMMA = ',';
	private static final char D_QT = '\"';
	private static final char COLON = ':';
	private static final char SPACE = ' ';

	private static final String EMPTY = "";
	private static final String NULL = "null";
	private static final String TITLE = "title";
	private static final String ARRAY_DELIMITER = ", ";

	private final static String NEWLINE = System.getProperty("line.separator");
	private final static String COMMA_DELIMITER_NEWLINE = "," + NEWLINE;


	private final RendererMeta meta;
	private final RendererMetaOptions rendererOptions;

	/**
	 * 생성자
	 * 
	 * @param eventType
	 * @param options
	 */
	public JSONRendererImpl(EPStatement stmt, JSONRenderingOptions options) {
		EventPropertyRenderer propertyRenderer = null;
		EventPropertyRendererContext propertyRendererContext = null;
		if (options.getRenderer() != null) {
			propertyRenderer = options.getRenderer();
			propertyRendererContext = new EventPropertyRendererContext(stmt.getEventType(), true);
		}

		rendererOptions = new RendererMetaOptions(options.isPreventLooping(), false, propertyRenderer, propertyRendererContext);
		meta = new RendererMeta(stmt.getEventType(), new Stack<EventTypePropertyPair>(), rendererOptions);
	}

	/**
	 * update 결과를 json으로 변환
	 * 
	 * @param newEvents
	 * @param oldEvents
	 * @return
	 */
	public String render(EventBean[] newEvents, EventBean[] oldEvents) {
		StringBuilder builder = new StringBuilder();
		builder.append(LB);
		_render(NEW_EVENTS_PREFIX, newEvents, builder);
		builder.append(COMMA);
		_render(OLD_EVENTS_PREFIX, oldEvents, builder);
		builder.append(RB);
		return builder.toString();
	}

	private void _render(String title, EventBean[] events, StringBuilder builder) {
		builder.append(D_QT).append(title).append(D_QT).append(COLON).append(LBR);
		if (events != null) {
			int len = events.length;
			for (int i = 0; i < len; i++) {
				builder.append(render(events[i]));
				if (i + 1 < len) {
					builder.append(COMMA);
				}
			}
		}
		builder.append(RBR);
	}
	
	public String render(String title, EventBean theEvent) {
		StringBuilder buf = new StringBuilder();
		buf.append(LB);
		buf.append(D_QT).append(TITLE).append(D_QT).append(COLON);
		buf.append(D_QT).append(title).append(D_QT).append(COMMA);
		buf.append(NEWLINE);
		recursiveRender(theEvent, buf, 2, meta, rendererOptions);
		buf.append(RB);
		return buf.toString();
	}
	
	public String render(String title, EventBean theEvent, Map<String, String> additional) {
		StringBuilder buf = new StringBuilder();
		buf.append(LB);
		buf.append(D_QT).append(TITLE).append(D_QT).append(COLON);
		buf.append(D_QT).append(title).append(D_QT).append(COMMA);
		
		if (additional != null) {
			for(String field : additional.keySet()) {
				buf.append(D_QT).append(field).append(D_QT).append(COLON);
				buf.append(D_QT).append(additional.get(field)).append(D_QT).append(COMMA);
			}
		}
		
		buf.append(NEWLINE);
		recursiveRender(theEvent, buf, 2, meta, rendererOptions);
		buf.append(RB);
		return buf.toString();
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////

	public String _render(String root, EventBean theEvent) {
		StringBuilder buf = new StringBuilder();
		buf.append(LB);
		buf.append(NEWLINE);

		ident(buf, 1);
		buf.append(D_QT);
		buf.append(root);
		buf.append(D_QT).append(COLON).append(LB);
		buf.append(NEWLINE);

		recursiveRender(theEvent, buf, 2, meta, rendererOptions);

		ident(buf, 1);
		buf.append(RB);
		buf.append(NEWLINE);

		buf.append(RB);
		buf.append(NEWLINE);

		return buf.toString();
	}

	public String render(EventBean theEvent) {
		StringBuilder buf = new StringBuilder();
		buf.append(LB);
		recursiveRender(theEvent, buf, 2, meta, rendererOptions);
		buf.append(RB);
		return buf.toString();
	}

	private static void ident(StringBuilder buf, int level) {
		for (int i = 0; i < level; i++) {
			indentChar(buf);
		}
	}

	private static void indentChar(StringBuilder buf) {
		buf.append(SPACE);
		buf.append(SPACE);
	}

	private static void recursiveRender(EventBean theEvent, StringBuilder buf, int level, RendererMeta meta, RendererMetaOptions rendererOptions) {
		String delimiter = EMPTY;

		// simple properties
		GetterPair[] simpleProps = meta.getSimpleProperties();
		if (rendererOptions.getRenderer() == null) {
			for (GetterPair simpleProp : simpleProps) {
				Object value = simpleProp.getGetter().get(theEvent);
				writeDelimitedIndentedProp(buf, delimiter, level, simpleProp.getName());
				simpleProp.getOutput().render(value, buf);
				delimiter = COMMA_DELIMITER_NEWLINE;
			}
		} else {
			EventPropertyRendererContext context = rendererOptions.getRendererContext();
			context.setStringBuilderAndReset(buf);
			for (GetterPair simpleProp : simpleProps) {
				Object value = simpleProp.getGetter().get(theEvent);
				writeDelimitedIndentedProp(buf, delimiter, level, simpleProp.getName());
				context.setDefaultRenderer(simpleProp.getOutput());
				context.setPropertyName(simpleProp.getName());
				context.setPropertyValue(value);
				rendererOptions.getRenderer().render(context);
				delimiter = COMMA_DELIMITER_NEWLINE;
			}
		}

		GetterPair[] indexProps = meta.getIndexProperties();
		for (GetterPair indexProp : indexProps) {
			Object value = indexProp.getGetter().get(theEvent);
			writeDelimitedIndentedProp(buf, delimiter, level, indexProp.getName());

			if (value == null) {
				buf.append(NULL);
			} else {
				if (!value.getClass().isArray()) {
					buf.append(LBR).append(RBR);
				} else {
					buf.append(LBR);
					String arrayDelimiter = EMPTY;

					if (rendererOptions.getRenderer() == null) {
						for (int i = 0; i < Array.getLength(value); i++) {
							Object arrayItem = Array.get(value, i);
							buf.append(arrayDelimiter);
							indexProp.getOutput().render(arrayItem, buf);
							arrayDelimiter = ARRAY_DELIMITER;
						}
					} else {
						EventPropertyRendererContext context = rendererOptions.getRendererContext();
						context.setStringBuilderAndReset(buf);
						for (int i = 0; i < Array.getLength(value); i++) {
							Object arrayItem = Array.get(value, i);
							buf.append(arrayDelimiter);
							context.setPropertyName(indexProp.getName());
							context.setPropertyValue(arrayItem);
							context.setIndexedPropertyIndex(i);
							context.setDefaultRenderer(indexProp.getOutput());
							rendererOptions.getRenderer().render(context);
							arrayDelimiter = ARRAY_DELIMITER;
						}
					}
					buf.append(RBR);
				}
			}
			delimiter = COMMA_DELIMITER_NEWLINE;
		}

		GetterPair[] mappedProps = meta.getMappedProperties();
		for (GetterPair mappedProp : mappedProps) {
			Object value = mappedProp.getGetter().get(theEvent);

			if ((value != null) && (!(value instanceof Map))) {
				LOG.warn("Property '" + mappedProp.getName() + "' expected to return Map and returned " + value.getClass() + " instead");
				continue;
			}

			writeDelimitedIndentedProp(buf, delimiter, level, mappedProp.getName());

			if (value == null) {
				buf.append(NULL);
				buf.append(NEWLINE);
			} else {
				@SuppressWarnings("unchecked")
				Map<String, Object> map = (Map<String, Object>) value;
				if (map.isEmpty()) {
					buf.append(LB).append(RB);
					buf.append(NEWLINE);
				} else {
					buf.append(LB);
					buf.append(NEWLINE);

					String localDelimiter = EMPTY;
					Iterator<Map.Entry<String, Object>> it = map.entrySet().iterator();
					for (; it.hasNext();) {
						Map.Entry<String, Object> entry = it.next();
						if (entry.getKey() == null) {
							continue;
						}

						buf.append(localDelimiter);
						ident(buf, level + 1);
						buf.append(D_QT);
						buf.append(entry.getKey());
						buf.append(D_QT).append(COLON);

						if (entry.getValue() == null) {
							buf.append(NULL);
							
						} else {
							OutputValueRenderer outRenderer = OutputValueRendererFactory.getOutputValueRenderer(entry.getValue().getClass(), rendererOptions);
							if (rendererOptions.getRenderer() == null) {
								outRenderer.render(entry.getValue(), buf);
							} else {
								EventPropertyRendererContext context = rendererOptions.getRendererContext();
								context.setStringBuilderAndReset(buf);
								context.setPropertyName(mappedProp.getName());
								context.setPropertyValue(entry.getValue());
								context.setMappedPropertyKey(entry.getKey());
								context.setDefaultRenderer(outRenderer);
								rendererOptions.getRenderer().render(context);
							}
						}
						localDelimiter = COMMA_DELIMITER_NEWLINE;
					}

					buf.append(NEWLINE);
					ident(buf, level);
					buf.append(RB);
				}
			}

			delimiter = COMMA_DELIMITER_NEWLINE;
		}

		NestedGetterPair[] nestedProps = meta.getNestedProperties();
		for (NestedGetterPair nestedProp : nestedProps) {
			Object value = nestedProp.getGetter().getFragment(theEvent);

			writeDelimitedIndentedProp(buf, delimiter, level, nestedProp.getName());

			if (value == null) {
				buf.append(NULL);
			} else if (!nestedProp.isArray()) {
				if (!(value instanceof EventBean)) {
					LOG.warn("Property '" + nestedProp.getName() + "' expected to return EventBean and returned " + value.getClass() + " instead");
					buf.append(NULL);
					continue;
				}
				EventBean nestedEventBean = (EventBean) value;
				buf.append(LB);
				buf.append(NEWLINE);

				recursiveRender(nestedEventBean, buf, level + 1, nestedProp.getMetadata(), rendererOptions);

				ident(buf, level);
				buf.append(RB);
			} else {
				if (!(value instanceof EventBean[])) {
					LOG.warn("Property '" + nestedProp.getName() + "' expected to return EventBean[] and returned " + value.getClass() + " instead");
					buf.append(NULL);
					continue;
				}

				StringBuilder arrayDelimiterBuf = new StringBuilder();
				arrayDelimiterBuf.append(COMMA);
				arrayDelimiterBuf.append(NEWLINE);
				ident(arrayDelimiterBuf, level + 1);

				EventBean[] nestedEventArray = (EventBean[]) value;
				String arrayDelimiter = EMPTY;
				buf.append(LBR);

				for (int i = 0; i < nestedEventArray.length; i++) {
					EventBean arrayItem = nestedEventArray[i];
					buf.append(arrayDelimiter);
					arrayDelimiter = arrayDelimiterBuf.toString();

					buf.append(LB);
					buf.append(NEWLINE);

					recursiveRender(arrayItem, buf, level + 2, nestedProp.getMetadata(), rendererOptions);

					ident(buf, level + 1);
					buf.append(RB);
				}
				buf.append(RBR);
			}
			delimiter = COMMA_DELIMITER_NEWLINE;
		}

		buf.append(NEWLINE);
	}

	private static void writeDelimitedIndentedProp(StringBuilder buf, String delimiter, int level, String name) {
		buf.append(delimiter);
		ident(buf, level);
		buf.append(D_QT);
		buf.append(name);
		buf.append(D_QT).append(COLON);
	}
}