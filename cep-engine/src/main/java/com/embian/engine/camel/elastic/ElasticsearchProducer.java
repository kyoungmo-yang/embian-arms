package com.embian.engine.camel.elastic;

import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.ExpectedBodyTypeException;
import org.apache.camel.Message;
import org.apache.camel.impl.DefaultProducer;
import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents an Elasticsearch producer.
 */
public class ElasticsearchProducer extends DefaultProducer {
	private static final Logger LOG = LoggerFactory.getLogger(ElasticsearchProducer.class);
	
	private BulkProcessor bulkProcessor;
	
    public ElasticsearchProducer(ElasticsearchEndpoint endpoint) {
        super(endpoint);
        this.initBulkProcessor(endpoint);
    }
    
    private void initBulkProcessor(ElasticsearchEndpoint endpoint) {
    	bulkProcessor = BulkProcessor.builder(endpoint.getClient(), new BulkProcessor.Listener() {
			
			@Override
			public void beforeBulk(long executionId, BulkRequest request) {
				if (LOG.isDebugEnabled()) {
					LOG.info("beforeBulk - executionId: "+executionId+", estimatedSizeInBytes: " + request.estimatedSizeInBytes() + ", headers: " + request.getHeaders());
				}
			}
			
			@Override
			public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
				if (LOG.isDebugEnabled()) {
					LOG.info("afterBulk - executionId: "+executionId+", estimatedSizeInBytes: " + request.estimatedSizeInBytes() + ", headers: " + request.getHeaders());
				}
				LOG.error(failure.getLocalizedMessage());
			}
			
			@Override
			public void afterBulk(long executionId, BulkRequest request, BulkResponse response) {
				if (LOG.isDebugEnabled()) {
					LOG.info("afterBulk - executionId: "+executionId+", response: " + response.getTookInMillis() + ", " + response.getHeaders());
				}
			}
		})
		.setBulkActions(100)
		.setBulkSize(new ByteSizeValue(10, ByteSizeUnit.MB))
		.setFlushInterval(TimeValue.timeValueSeconds(1))
		.setConcurrentRequests(2)
		.build();
    }

    @Override
    public ElasticsearchEndpoint getEndpoint() {
        return (ElasticsearchEndpoint) super.getEndpoint();
    }

    public void process(Exchange exchange) throws Exception {
        String operation = exchange.getIn().getHeader(ElasticsearchConfiguration.PARAM_OPERATION, String.class);
        if (operation == null) {
            operation = getEndpoint().getConfig().getOperation();
        }

        if (operation == null) {
            throw new IllegalArgumentException(ElasticsearchConfiguration.PARAM_OPERATION + " is missing");
        }

        Client client = getEndpoint().getClient();

        if (operation.equalsIgnoreCase(ElasticsearchConfiguration.OPERATION_INDEX)) {
            addToIndex(client, exchange);
        } else if (operation.equalsIgnoreCase(ElasticsearchConfiguration.OPERATION_BULK_INDEX)) {
			addToBulkIndex(client, exchange);
		} else if (operation.equalsIgnoreCase(ElasticsearchConfiguration.OPERATION_GET_BY_ID)) {
            getById(client, exchange);
        } else if (operation.equalsIgnoreCase(ElasticsearchConfiguration.OPERATION_DELETE)) {
            deleteById(client, exchange);
        } else {
            throw new IllegalArgumentException(ElasticsearchConfiguration.PARAM_OPERATION + " value '" + operation + "' is not supported");
        }
    }

    /**
     * added by kyang
     * @param client
     * @param exchange
     */
    public void addToBulkIndex(Client client, Exchange exchange) {
    	String indexName = exchange.getIn().getHeader(ElasticsearchConfiguration.PARAM_INDEX_NAME, String.class);
        if (indexName == null) {
            indexName = getEndpoint().getConfig().getIndexName();
        }

        String indexType = exchange.getIn().getHeader(ElasticsearchConfiguration.PARAM_INDEX_TYPE, String.class);
        if (indexType == null) {
            indexType = getEndpoint().getConfig().getIndexType();
        }
        
        IndexRequestBuilder prepareIndex = client.prepareIndex(indexName, indexType);
        
        
        if (!setIndexRequestSource(exchange.getIn(), prepareIndex)) {
            throw new ExpectedBodyTypeException(exchange, XContentBuilder.class);
        }
        
        bulkProcessor.add(prepareIndex.request());
//        ListenableActionFuture<IndexResponse> future = prepareIndex.execute();
//        IndexResponse response = future.actionGet();
//        exchange.getIn().setBody(response.getId());
    	
    	
	}

	public void getById(Client client, Exchange exchange) {
        String indexName = exchange.getIn().getHeader(ElasticsearchConfiguration.PARAM_INDEX_NAME, String.class);
        if (indexName == null) {
            indexName = getEndpoint().getConfig().getIndexName();
        }

        String indexType = exchange.getIn().getHeader(ElasticsearchConfiguration.PARAM_INDEX_TYPE, String.class);
        if (indexType == null) {
            indexType = getEndpoint().getConfig().getIndexType();
        }

        String indexId = exchange.getIn().getBody(String.class);

        GetResponse response = client.prepareGet(indexName, indexType, indexId).execute().actionGet();
        exchange.getIn().setBody(response);
    }

    public void deleteById(Client client, Exchange exchange) {
        String indexName = exchange.getIn().getHeader(ElasticsearchConfiguration.PARAM_INDEX_NAME, String.class);
        if (indexName == null) {
            indexName = getEndpoint().getConfig().getIndexName();
        }

        String indexType = exchange.getIn().getHeader(ElasticsearchConfiguration.PARAM_INDEX_TYPE, String.class);
        if (indexType == null) {
            indexType = getEndpoint().getConfig().getIndexType();
        }

        String indexId = exchange.getIn().getBody(String.class);

        DeleteResponse response = client.prepareDelete(indexName, indexType, indexId).execute().actionGet();
        exchange.getIn().setBody(response);
    }

    public void addToIndex(Client client, Exchange exchange) {
    	
        String indexName = exchange.getIn().getHeader(ElasticsearchConfiguration.PARAM_INDEX_NAME, String.class);
        if (indexName == null) {
            indexName = getEndpoint().getConfig().getIndexName();
        }

        String indexType = exchange.getIn().getHeader(ElasticsearchConfiguration.PARAM_INDEX_TYPE, String.class);
        if (indexType == null) {
            indexType = getEndpoint().getConfig().getIndexType();
        }
        
        IndexRequestBuilder prepareIndex = client.prepareIndex(indexName, indexType);
        
        
        if (!setIndexRequestSource(exchange.getIn(), prepareIndex)) {
            throw new ExpectedBodyTypeException(exchange, XContentBuilder.class);
        }
        
        ListenableActionFuture<IndexResponse> future = prepareIndex.execute();
        IndexResponse response = future.actionGet();
        exchange.getIn().setBody(response.getId());
    }

    @SuppressWarnings("unchecked")
    private boolean setIndexRequestSource(Message msg, IndexRequestBuilder builder) {
        Object body = null;
        boolean converted = false;

        // order is important
        Class<?>[] types = new Class[] {XContentBuilder.class, Map.class, byte[].class, String.class};

        for (int i = 0; i < types.length && body == null; i++) {
            Class<?> type = types[i];
            body = msg.getBody(type);
        }

        if (body != null) {
            converted = true;
            if (body instanceof byte[]) {
                builder.setSource((byte[])body);
            } else if (body instanceof Map) {
                builder.setSource((Map<String, Object>) body);
            } else if (body instanceof String) {
                builder.setSource((String)body);
            } else if (body instanceof XContentBuilder) {
                builder.setSource((XContentBuilder)body);
            } else {
                converted = false;
            }
        }
        return converted;
    }
}
