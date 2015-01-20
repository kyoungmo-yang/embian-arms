package com.embian.engine.camel.influxdb;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.camel.Consumer;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.impl.DefaultEndpoint;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.impl.DefaultMessage;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;

public class InfluxDBEndpoint extends DefaultEndpoint {

	private int flushbuffer;
	private int flushinterval;
	
    private String hostname;
    private int portNumber;
	private String database;

	private String username;
    private String password;
	private String eventTimeKey;
	private String serieNameKey;
	private String payloadKey;
	private String serieTimeKey;
    
    public InfluxDBEndpoint() {
    }

    public InfluxDBEndpoint(String endpointUri, InfluxDBComponent component) throws URISyntaxException {
        super(endpointUri, component);
    }

    public Exchange createInfluxDBExchange(byte[] body) {
        Exchange exchange = new DefaultExchange(getCamelContext(), getExchangePattern());

        Message message = new DefaultMessage();
        exchange.setIn(message);

        message.setHeader(InfluxDBConstants.DATABASE_NAME, this.getDatabase());

        message.setBody(body);

        return exchange;
    }

	@Override
    public Consumer createConsumer(Processor processor) throws Exception {
        InfluxDBConsumer consumer = new InfluxDBConsumer(this, processor);
        configureConsumer(consumer);
        return consumer;
    }

    public InfluxDB connect() throws IOException {
    	return InfluxDBFactory.connect(new StringBuffer("http://").append(getHostname()).append(':').append(getPortNumber()).toString(), getUsername(), getPassword());
    }

    @Override
    public Producer createProducer() throws Exception {
        return new InfluxDBProducer(this);
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    protected ExecutorService createExecutor() {
        if (getCamelContext() != null) {
            return getCamelContext().getExecutorServiceManager().newSingleThreadExecutor(this, "CamelInfluxDBProducer");
        } else {
            return Executors.newSingleThreadExecutor();
        }
    }
    
    public int getFlushbuffer() {
		return flushbuffer;
	}

	public void setFlushbuffer(int flushbuffer) {
		this.flushbuffer = flushbuffer;
	}

	public int getFlushinterval() {
		return flushinterval;
	}

	public void setFlushinterval(int flushinterval) {
		this.flushinterval = flushinterval;
	}

	public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public int getPortNumber() {
        return portNumber;
    }

    public void setPortNumber(int portNumber) {
        this.portNumber = portNumber;
    }
    
	public void setDatabase(String database) {
		this.database = database;
	}

	public String getDatabase() {
		return this.database;
	}
	
	public String getEventTimeKey() {
		return eventTimeKey;
	}

	public void setEventTimeKey(String eventTimeKey) {
		this.eventTimeKey = eventTimeKey;
	}

	public String getSerieNameKey() {
		return serieNameKey;
	}

	public void setSerieNameKey(String serieNameKey) {
		this.serieNameKey = serieNameKey;
	}

	public String getPayloadKey() {
		return payloadKey;
	}

	public void setPayloadKey(String payloadKey) {
		this.payloadKey = payloadKey;
	}

	public String getSerieTimeKey() {
		return serieTimeKey;
	}

	public void setSerieTimeKey(String serieTimeKey) {
		this.serieTimeKey = serieTimeKey;
	}

}
