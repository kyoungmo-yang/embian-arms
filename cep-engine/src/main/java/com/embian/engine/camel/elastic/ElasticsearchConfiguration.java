package com.embian.engine.camel.elastic;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;

public class ElasticsearchConfiguration {

    public static final String PARAM_OPERATION = "operation";
    public static final String OPERATION_INDEX = "INDEX";
    public static final String OPERATION_GET_BY_ID = "GET_BY_ID";
    public static final String OPERATION_DELETE = "DELETE";
    public static final String PARAM_INDEX_ID = "indexId";
    public static final String PARAM_DATA = "data";
    public static final String PARAM_INDEX_NAME = "indexName";
    public static final String PARAM_INDEX_TYPE = "indexType";
    public static final String PROTOCOL = "elasticsearch";
    private static final String LOCAL_NAME = "local";
    private static final String IP = "ip";
    private static final String PORT = "port";
    private static final Integer DEFAULT_PORT = 9300;
    
    
    // added by kyang
    public static final String OPERATION_BULK_INDEX = "BULK_INDEX";
    
    
//    .setBulkActions(100)
//	.setBulkSize(new ByteSizeValue(10, ByteSizeUnit.MB))
//	.setFlushInterval(TimeValue.timeValueSeconds(1))
//	.setConcurrentRequests(2)
//	.build();

    private URI uri;
    private String protocolType;
    private String authority;
    private String clusterName;
    private String indexName;
    private String indexType;
    private boolean local;
    private Boolean data;
    private String operation;
    private String ip;
    private Integer port;

    public ElasticsearchConfiguration(URI uri, Map<String, Object> parameters) throws Exception {
        String protocol = uri.getScheme();

        if (!protocol.equalsIgnoreCase(PROTOCOL)) {
            throw new IllegalArgumentException("unrecognized elasticsearch protocol: " + protocol + " for uri: " + uri);
        }
        setUri(uri);
        setAuthority(uri.getAuthority());
        if (!isValidAuthority()) {
            throw new URISyntaxException(uri.toASCIIString(), "incorrect URI syntax specified for the elasticsearch endpoint."
                                                              + "please specify the syntax as \"elasticsearch:[Cluster Name | 'local']?[Query]\"");
        }

        if (LOCAL_NAME.equals(getAuthority())) {
            setLocal(true);
            setClusterName(null);
        } else {
            setLocal(false);
            setClusterName(getAuthority());
        }

        data = toBoolean(parameters.remove(PARAM_DATA));

        if (data == null) {
            data = local;
        }

        if (local && !data) {
            throw new IllegalArgumentException("invalid to use local node without data");
        }

        indexName = (String)parameters.remove(PARAM_INDEX_NAME);
        indexType = (String)parameters.remove(PARAM_INDEX_TYPE);
        operation = (String)parameters.remove(PARAM_OPERATION);
        ip = (String)parameters.remove(IP);
        String portParam = (String) parameters.remove(PORT);
        port = portParam == null ? DEFAULT_PORT : Integer.valueOf(portParam);
    }

    protected Boolean toBoolean(Object string) {
        if ("true".equals(string)) {
            return true;
        } else if ("false".equals(string)) {
            return false;
        } else {
            return null;
        }
    }

    public Node buildNode() {
        NodeBuilder builder = nodeBuilder().local(isLocal()).data(isData());
        if (!isLocal() && getClusterName() != null) {
            builder.clusterName(getClusterName());
        }
        return builder.node();
    }

    private boolean isValidAuthority() throws URISyntaxException {
        if (authority.contains(":")) {
            return false;
        }
        return true;

    }

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public String getProtocolType() {
        return protocolType;
    }

    public void setProtocolType(String protocolType) {
        this.protocolType = protocolType;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getAuthority() {
        return authority;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }

    public String getIndexName() {
        return indexName;
    }

    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    public String getIndexType() {
        return indexType;
    }

    public void setIndexType(String indexType) {
        this.indexType = indexType;
    }

    public boolean isLocal() {
        return local;
    }

    public void setLocal(boolean local) {
        this.local = local;
    }

    public boolean isData() {
        return data;
    }

    public void setData(boolean data) {
        this.data = data;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getOperation() {
        return this.operation;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

}
