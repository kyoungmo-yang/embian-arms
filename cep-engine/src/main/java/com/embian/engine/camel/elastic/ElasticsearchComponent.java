package com.embian.engine.camel.elastic;

import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.impl.DefaultComponent;

/**
 * Represents the component that manages {@link ElasticsearchEndpoint}.
 */
public class ElasticsearchComponent extends DefaultComponent {

    public ElasticsearchComponent() {
    }

    public ElasticsearchComponent(CamelContext context) {
        super(context);
    }

    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        Endpoint endpoint = new ElasticsearchEndpoint(uri, this, parameters);
        setProperties(endpoint, parameters);
        return endpoint;
    }
}
