package com.embian.engine.camel.influxdb;

import java.net.URI;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InfluxDBComponent extends DefaultComponent {

    private static final Logger LOG = LoggerFactory.getLogger(InfluxDBComponent.class);

    public InfluxDBComponent() {
    }

    public InfluxDBComponent(CamelContext context) {
        super(context);
    }

    @Override
    protected InfluxDBEndpoint createEndpoint(String uri,
                                              String remaining,
                                              Map<String, Object> params) throws Exception {
        URI host = new URI("http://" + remaining);
        String hostname = host.getHost();
        int portNumber = host.getPort();
        String database = host.getPath().substring(1);

        InfluxDBEndpoint endpoint = new InfluxDBEndpoint(uri, this);
        endpoint.setHostname(hostname);
        endpoint.setPortNumber(portNumber);
        endpoint.setDatabase(database);

        setProperties(endpoint, params);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Creating InfluxDBEndpoint with host {}:{} and database: {}",
                    new Object[]{endpoint.getHostname(), endpoint.getPortNumber(), endpoint.getDatabase()});
        }

        return endpoint;
    }
}
