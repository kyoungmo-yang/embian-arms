package com.embian.engine.camel.elastic;

import java.net.URI;
import java.util.Map;

import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.impl.DefaultEndpoint;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.node.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents an Elasticsearch endpoint.
 */
public class ElasticsearchEndpoint extends DefaultEndpoint {

    private static final Logger LOG = LoggerFactory.getLogger(ElasticsearchEndpoint.class);

    private Node node;
    private Client client;
    private ElasticsearchConfiguration config;

    public ElasticsearchEndpoint(String uri, ElasticsearchComponent component, Map<String, Object> parameters) throws Exception {
        super(uri, component);
        this.config = new ElasticsearchConfiguration(new URI(uri), parameters);
    }

    public Producer createProducer() throws Exception {
        return new ElasticsearchProducer(this);
    }

    public Consumer createConsumer(Processor processor) throws Exception {
        throw new RuntimeCamelException("Cannot consume to a ElasticsearchEndpoint: " + getEndpointUri());
    }

    public boolean isSingleton() {
        return true;
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        if (config.isLocal()) {
            LOG.info("Starting local ElasticSearch server");
        } else {
            LOG.info("Joining ElasticSearch cluster " + config.getClusterName());
        }
        if (config.getIp() != null) {
            LOG.info("REMOTE ELASTICSEARCH: {}", config.getIp());
            Settings settings = ImmutableSettings.settingsBuilder()
                    .put("cluster.name", config.getClusterName())
                    .put("client.transport.ignore_cluster_name", false)
                    .put("node.client", true)
                    .put("client.transport.sniff", true)
                    .build();
            Client client = new TransportClient(settings)
                    .addTransportAddress(new InetSocketTransportAddress(config.getIp(), config.getPort()));
            this.client = client;
        } else {
            node = config.buildNode();
            client = node.client();
        }
    }

    @Override
    protected void doStop() throws Exception {
        if (config.isLocal()) {
            LOG.info("Stopping local ElasticSearch server");
        } else {
            LOG.info("Leaving ElasticSearch cluster " + config.getClusterName());
        }
        client.close();
        node.close();
        super.doStop();
    }

    public Client getClient() {
        return client;
    }

    public ElasticsearchConfiguration getConfig() {
        return config;
    }

    public void setOperation(String operation) {
        config.setOperation(operation);
    }

}
