package com.embian.engine.camel.influxdb;

import org.apache.camel.Processor;
import org.apache.camel.impl.DefaultConsumer;

/**
 * do not using consumer
 * @author kyang
 *
 */
public class InfluxDBConsumer extends DefaultConsumer {
    
    public InfluxDBConsumer(InfluxDBEndpoint endpoint, Processor processor) {
        super(endpoint, processor);
    }
}
