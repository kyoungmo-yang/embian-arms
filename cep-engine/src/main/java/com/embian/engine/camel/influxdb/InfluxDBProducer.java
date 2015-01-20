package com.embian.engine.camel.influxdb;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultProducer;
import org.apache.camel.util.ObjectHelper;
import org.influxdb.InfluxDB;
import org.influxdb.dto.Serie;

import com.fasterxml.jackson.databind.ObjectMapper;

public class InfluxDBProducer extends DefaultProducer {
	private static final String MESSAGE_FIELD = "message";
	
    private ExecutorService executorService;
    private BlockingQueue<Exchange> queue;
   
    public InfluxDBProducer(InfluxDBEndpoint endpoint) throws IOException {
        super(endpoint);
        this.queue = new LinkedBlockingQueue<Exchange>();
    }

    @Override
    public InfluxDBEndpoint getEndpoint() {
        return (InfluxDBEndpoint) super.getEndpoint();
    }

    @Override
    protected void doStart() throws Exception {
        this.executorService = getEndpoint().createExecutor();
        this.executorService.execute(new Producer(this, queue));
    }

    @Override
    protected void doStop() throws Exception {
        if (executorService != null) {
            getEndpoint().getCamelContext().getExecutorServiceManager().shutdownNow(executorService);
            executorService = null;
        }
    }

	@Override
    public void process(Exchange exchange) throws Exception {
    	this.queue.add(exchange);
    }
    
    class Producer implements Runnable {
    	private InfluxDBProducer influxDBProducer;
    	private BlockingQueue<Exchange> queue;
    	private InfluxDB conn;
    	private String databaseName;
    	private String eventTimeKey;
    	private String serieNameKey;
    	private String payloadKey;
    	private String serieTimeKey;
    	
    	private int flushbuffer;
    	private long flushinterval;
    	
//    	private List<Serie> buffer;
    	
    	private Map<Integer, Serie.Builder> buffer;
    	
    	private long numofvalue = 0;
    	private long startInterval = 0;
    	
    	Producer(InfluxDBProducer influxDBProducer, BlockingQueue<Exchange> queue) {
    		this.influxDBProducer = influxDBProducer;
    		this.queue = queue;
    		
    		databaseName = influxDBProducer.getEndpoint().getDatabase();
    		eventTimeKey = influxDBProducer.getEndpoint().getEventTimeKey();
        	serieNameKey = influxDBProducer.getEndpoint().getSerieNameKey();
        	payloadKey = influxDBProducer.getEndpoint().getPayloadKey();
        	serieTimeKey = influxDBProducer.getEndpoint().getSerieTimeKey();
        	
        	flushbuffer = influxDBProducer.getEndpoint().getFlushbuffer();
        	flushinterval = influxDBProducer.getEndpoint().getFlushinterval() * 1000;
//        	buffer = new Vector<Serie>(flushbuffer);
        	buffer = new HashMap<Integer, Serie.Builder>();
        	
        	if (ObjectHelper.isEmpty(databaseName) 
        			|| ObjectHelper.isEmpty(eventTimeKey)
	        		|| ObjectHelper.isEmpty(serieNameKey)
	        		|| ObjectHelper.isEmpty(payloadKey)
	        		|| ObjectHelper.isEmpty(serieTimeKey)) {
	            throw new IllegalArgumentException("DatabaseName or eventTimeKey or serieNameKey or payloadKey or serieTimeKey is not provided in the endpoint: " + getEndpoint());
	        }
    	}
    	
    	private long interval() {
    		return System.currentTimeMillis() - this.startInterval;
    	}
    	
    	private void flush() throws IOException {
    		if (this.startInterval == 0) {
				this.startInterval = System.currentTimeMillis();
			}
    		
    		if (this.numofvalue < this.flushbuffer && this.interval() < this.flushinterval) {
				return ;
			}
    		
    		if (log.isTraceEnabled()) {
    			log.trace("flush buffer");
			}
    		
    		if (this.conn == null) {
				this.conn = influxDBProducer.getEndpoint().connect();
				if (log.isDebugEnabled()) {
					log.debug("Created connection: {}", this.conn);
				}
			}
    		
//    		long s = System.currentTimeMillis();
    		for (Serie.Builder builder : this.buffer.values()) {
    			this.conn.write(databaseName, TimeUnit.MILLISECONDS, builder.build());
			}
//    		long e = System.currentTimeMillis();
//    		log.info(new StringBuffer().append("Flush buffer: ").append(this.buffer.size()).append(", T: ").append((e-s)).append(", Q: ").append(this.queue.size()).toString());
    		
    		this.buffer.clear();
    		this.startInterval = 0;
    		this.numofvalue = 0;
    	}
    	
    	@SuppressWarnings("unchecked")
		private void handle(Exchange exchange) throws Exception {
    		if (log.isTraceEnabled()) {
    			log.trace("handle: " + exchange);
			}
    		
	        byte[] messageBodyBytes = exchange.getIn().getMandatoryBody(byte[].class);
	        
	        Map<String,Object> event = new HashMap<String, Object>();
	    	ObjectMapper objectMapper = new ObjectMapper();
	    	event = objectMapper.readValue(messageBodyBytes, HashMap.class);
	    	
	    	Object timestamp = event.get(eventTimeKey);
	    	Object serieName = event.get(serieNameKey);
	    	Map<String, Object> payload = (Map<String, Object>) event.get(payloadKey);
	    	if (timestamp != null && payload != null && serieName != null) {
//	    		fluentd와 influxdb간의 1초의 delay로 인해 influxdb로 유입되는 시간을 time으로 사용
//	    		payload.put(serieTimeKey, timestamp);
	    		
	    		if(payload.containsKey(MESSAGE_FIELD)) {
	    			payload.remove(MESSAGE_FIELD);
	    		}
	    		
	    		Object[] values = payload.values().toArray();
//	    		String[] columns = payload.keySet().toArray(new String[payload.keySet().size()]);
//				Serie serie = new Serie.Builder(serieName.toString())
//				                       .columns(columns)
//				                       .values(values);
//				                       .build();
	    		
	    		Serie.Builder builder = null;
	    		int key = payload.keySet().hashCode();
	    		if (this.buffer.containsKey(key)) {
					builder = this.buffer.get(key);
	    			
				} else {
					String[] columns = payload.keySet().toArray(new String[payload.keySet().size()]);
					builder = new Serie.Builder(serieName.toString()).columns(columns);
					this.buffer.put(key, builder);
				}
	    		
	    		builder.values(values);
				this.numofvalue++;
				this.flush();
			} else {
				log.debug("{} or {} or {} is undefined : " + event, eventTimeKey, serieNameKey, payloadKey);
			}
    	}

		@Override
		public void run() {
			if (log.isTraceEnabled()) {
				log.trace("Running CamelInfluxDBProducer...");
			}
			
			while(true) {
				try {
					this.handle(this.queue.take());
				} catch (Exception e) {
					log.error(e.getLocalizedMessage());
				}
			}
		}
			
    }
}
