package com.embian.engine;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.impl.DefaultCamelContext;
import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Database;
import org.influxdb.dto.DatabaseConfiguration;
import org.influxdb.dto.ShardSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.ConfigurableEnvironment;

import com.embian.engine.camel.elastic.ElasticsearchComponent;
import com.embian.engine.camel.influxdb.InfluxDBComponent;
import com.embian.engine.camel.rabbitmq.RabbitMQComponent;
import com.embian.engine.conf.Config;
import com.embian.engine.controller.StmtController;
import com.embian.engine.core.EsperEngine;
import com.embian.engine.core.EventRouter;
import com.embian.engine.memcached.SBCache;
import com.embian.engine.sbarm.SBArmManager;
import com.embian.engine.sinker.AlertSinker;
import com.embian.engine.sinker.DashboardSinker;
import com.embian.engine.sinker.SinkerManager;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ServiceInitializer {
	private static final Logger LOG = LoggerFactory.getLogger(ServiceInitializer.class);
	
	private static ServiceInitializer instance;
	
	private Config conf;
	
	private ServiceInitializer(){
		conf = new Config();
	};
	
	private void setEnv(ConfigurableEnvironment env) {
		this.conf.setEnv(env);
	}
	
	public Config getConfig() {
		return this.conf;
	}
	
	@SuppressWarnings("unchecked")
	/**
	 *  stmt.put("stmtId", stmtId);
		stmt.put("name", name);
		stmt.put("epl", epl);
		stmt.put("routingKey", routingKey);
		
		stmt.put("desc", desc);
		stmt.put("mail", toMail);
		stmt.put("level", level);
		stmt.put("edit", false);
	 */
	private void _initStmts() {
		EsperEngine engine = EsperEngine.instance();
    	SBArmManager sbm = SBArmManager.instance();
    	
    	SBCache cache = new SBCache();
    	Object o = cache.get(StmtController.STMT_LIST_CACHE_KEY);
    	if (o != null) {
			Iterator<String> it = ((Set<String>)o).iterator();
			while(it.hasNext()) {
				Map<String, Object> stmt = (Map<String, Object>) cache.get(it.next());
				
				String stmtId = stmt.get("stmtId").toString();
				String name = stmt.get("name").toString();
				String desc = stmt.get("desc").toString();
				String epl = stmt.get("epl").toString();
				String routingKey = stmt.get("routingKey").toString();
				String mail = stmt.get("mail").toString();
				String level = stmt.get("level").toString();
				
				String eplBody =  new StringBuffer().append("@Name(\"").append(name).append("\")")
				                                    .append("@Description(\"").append(desc).append("\")")
				                                    .append(epl).toString();
				
				String rk = new StringBuffer().append(routingKey).append(stmtId).toString();
				
				if (routingKey.startsWith(AlertSinker.ROUTING_KEY_PREFIX)) {
					if (stmt.get("mail") != null) {
		    			engine.addStatement(stmtId, eplBody, rk);
		    			
		    			sbm.putLevel(stmtId, level);
		    			sbm.putMail(stmtId, mail);
		    			AlertSinker.registryAlertSinker(rk, mail);
		    			DashboardSinker.registryDashSinker(rk);
					}					
				} else if (routingKey.startsWith(DashboardSinker.ROUTING_KEY_PREFIX)) {
					engine.addStatement(stmtId, eplBody, rk);
		    		DashboardSinker.registryDashSinker(rk);
				}
				
			}
		}
	}
	
	private static final String[] CONTINUOUS_QUERIES = new String[]{
		"select last(service) as service, max(request_time) as request_time from service.log group by hostname, request, time(10s) into dash.service.request_time",
		"select last(service) as service, max(bytes_sent) as bytes_sent from service.log group by hostname, request, time(10s) into dash.service.bytes_sent",
		"select sum(request_time) as request_time, count(service) as service_cnt from service.log group by time(10s), service, request into dash.main.request_time",
		"select last(service) as service, count(service) as request_cnt from service.log group by hostname, request, time(10s) into dash.service.request_cnt"
	};
	
	private void _initInfluxDB() {
		InfluxDB influxdb = InfluxDBFactory.connect(new StringBuffer(DashboardSinker.HTTP_PREFIX).append(DashboardSinker.INFLUXDB_HOST).append(':').append(DashboardSinker.INFLUXDB_PORT).toString(), DashboardSinker.INFLUXDB_ROOT_USER, DashboardSinker.INFLUXDB_PASSWORD);
		if (influxdb != null) {
			boolean created = false;
			for (Database db : influxdb.describeDatabases()) {
				if (db.getName().equalsIgnoreCase(DashboardSinker.INFLUXDB_DATABASE)) {
					created = true;
				}
			}
			
			if (!created) {
				DatabaseConfiguration conf = new DatabaseConfiguration(DashboardSinker.INFLUXDB_DATABASE);
				for (String cq : CONTINUOUS_QUERIES) {
					conf.addContinuousQueries(cq);
				}
				
				String[] shardSpace = DashboardSinker.INFLUXDB_SHARDSPACE.split("\\|");
				if (shardSpace.length == 6) {
					String name = shardSpace[0];
					String retentionPolicy = shardSpace[1];
					String shardDuration = shardSpace[2];
					String regex = shardSpace[3];
					int replicationFactor = Integer.parseInt(shardSpace[4]);
					int split = Integer.parseInt(shardSpace[5]);
					
					if (replicationFactor == 0) {
						replicationFactor = 1;
					}
					
					if (split == 0) {
						split = 1;
					}
					
					ShardSpace ss = new ShardSpace(name, retentionPolicy, shardDuration, regex, replicationFactor, split);
					conf.addSpace(ss);
					influxdb.createDatabase(conf);
					influxdb.createDatabaseUser(conf.getName(), DashboardSinker.INFLUXDB_USERNAME, DashboardSinker.INFLUXDB_PASSWORD);
				}
			}
		}
	}
	
	@SuppressWarnings({ "resource", "unchecked" })
	private void _initElasticsearch() throws InterruptedException, ExecutionException, JsonParseException, JsonMappingException, IOException {
		String ip = this.conf.getString(Config.ELASTIC_IP);
		int port = this.conf.getInteger(Config.ELASTIC_PORT);
		String index = this.conf.getString(Config.ELASTIC_INDEXNAME);
        if (ip != null) {
            LOG.info("Init ELASTICSEARCH: {}", ip);
            Settings settings = ImmutableSettings.settingsBuilder()
                    .put("cluster.name", this.conf.getString(Config.ELASTIC_CLUSTER))
                    .put("client.transport.ignore_cluster_name", false)
                    .put("node.client", true)
                    .put("client.transport.sniff", true)
                    .build();
            Client client = new TransportClient(settings)
                    .addTransportAddress(new InetSocketTransportAddress(ip, port));
            
            ActionFuture<IndicesExistsResponse> ier = client.admin().indices().exists(new IndicesExistsRequest(index));
            if (!ier.get().isExists()) {
            	Map<String,Object> idxSettings = new HashMap<String,Object>();
            	String strIdxSettings = "{\"analysis\":{\"analyzer\":{\"default\":{\"stopwords\":\"_none_\",\"type\":\"pattern\",\"pattern\":\"\\\\s+\"}}}}}";
                ObjectMapper mapper = new ObjectMapper();
                idxSettings = mapper.readValue(strIdxSettings, HashMap.class);
            	ListenableActionFuture<CreateIndexResponse> cir = client.admin()
            	                                                        .indices()
            	                                                        .prepareCreate(index)
            	                                                        .setSettings(idxSettings)
            	                                                        .execute();
                if (cir.get().isAcknowledged()) {
                	LOG.info("Created INDEX {}", index);
    			} else {
    				LOG.info("Not Created INDEX {}", index);
    			}
			} else {
				LOG.info("INDEX {} is exists", index);
			}
            
            client.close();
        }
	}
	
	private void _init() throws Exception {
//    	initialize camel context
    	CamelContext context = new DefaultCamelContext();
    	SinkerManager.instance().setContext(context);
    	context.addComponent("rabbitmq", new RabbitMQComponent(context));
    	context.addComponent("influxdb", new InfluxDBComponent(context));
    	context.addComponent("elasticsearch", new ElasticsearchComponent(context));
    	ProducerTemplate ptempl = context.createProducerTemplate();
    	
//    	initialize Esper
    	EsperEngine.instance().setProducerTemplate(ptempl);
    	EsperEngine.instance().setMailDeliveryInterval(conf.getInteger(Config.EMAIL_DELIVERY_INTERVAL));
    	
//    	initialize Influxdb
    	this._initInfluxDB();
    	this._initElasticsearch();
		
    	List<String> schemes = this.conf.getList(Config.ESPER_EVENT_SCHEMA);
    	Pattern p = Pattern.compile("create schema ([^(]+).*");
    	String orgName = null;
    	for (String scheme:schemes) {
    		
    		Matcher m = p.matcher(scheme);
    		if(m.find()) {
    			orgName = m.group(1);
    			EPStatementObjectModel som = EsperEngine.instance().compileEPL(scheme.replace(".log", ""));
    			som.getCreateSchema().setSchemaName(orgName);
    			EsperEngine.instance().createScheme(som);
    		}
		}
    	
    	this._initStmts();
    	
//    	initialize camel routes 
    	DashboardSinker dsinker = new DashboardSinker();
    	String from = new StringBuffer("rabbitmq://")
    	                       .append(conf.getString(Config.RABBITMQ_HOST)).append('/').append(conf.getString(Config.RABBITMQ_INPUT_EXCHANGE))
    	                       .append("?routingKey=")		.append(conf.getString(Config.RABBITMQ_INPUT_ROUTINGKEY)).append('&')
    	                       .append("exchangeType=")		.append(conf.getString(Config.RABBITMQ_EXCHANGETYPE)).append('&')
    	                       .append("username=")			.append(conf.getString(Config.RABBITMQ_USERNAME)).append('&')
    	                       .append("password=")			.append(conf.getString(Config.RABBITMQ_PASSWORD)).append('&')
    	                       .append("durable=")			.append(conf.getBoolean(Config.RABBITMQ_DURABLE)).append('&')
    	                       .append("autoDelete=")		.append(conf.getBoolean(Config.RABBITMQ_AUTODELETE)).append('&')
    	                       .append("threadPoolSize=")	.append(conf.getInteger(Config.RABBITMQ_THREADPOOLSIZE)).toString();
    	
    	String toInfluxdb = new StringBuffer("influxdb://")
    						   .append(DashboardSinker.INFLUXDB_HOST).append(':').append(DashboardSinker.INFLUXDB_PORT)
    						   .append('/').append(DashboardSinker.INFLUXDB_DATABASE)
    						   .append("?eventTimeKey=").append(DashboardSinker.EVENT_TIME_KEY).append('&')
    						   
    						   .append("flushbuffer=").append(DashboardSinker.DASH_INFLUXDB_FLUSHBUFFER).append('&')
    						   .append("flushinterval=").append(DashboardSinker.DASH_INFLUXDB_FLUSHINTERVAL).append('&')
    						   
    						   .append("username=").append(DashboardSinker.INFLUXDB_USERNAME).append('&')
    						   .append("password=").append(DashboardSinker.INFLUXDB_PASSWORD).append('&')
    						   .append("serieNameKey=").append(DashboardSinker.SERIE_NAME_KEY).append('&')
    						   .append("payloadKey=").append(DashboardSinker.PAYLOAD_KEY).append('&')
    						   .append("serieTimeKey=").append(DashboardSinker.SERIE_TIME_KEY).toString();
    	
    	String toElastic = new StringBuffer("elasticsearch:").append(conf.getString(Config.ELASTIC_CLUSTER))
    						   .append("?operation=").append(conf.getString(Config.ELASTIC_OPERATION))
    						   .append("&indexName=").append(conf.getString(Config.ELASTIC_INDEXNAME))
    						   .append("&indexType=").append(conf.getString(Config.ELASTIC_INDEXTYPE))
    						   .append("&ip=").append(conf.getString(Config.ELASTIC_IP))
    						   .append("&port=").append(conf.getString(Config.ELASTIC_PORT)).toString();
    	
    	String[] rb = new String[DashboardSinker.DASH_INFLUXDB_THREADPOOLSIZE];
    	for (int i = 0; i < rb.length; i++) {
			rb[i] = toInfluxdb;
		}
    	
    	dsinker.from(from)
    	       .to("class:"+EventRouter.class.getCanonicalName()+"?method=routeToEsper")
    	       .to(toElastic)
    	       .loadBalance().roundRobin()
    		   .to(rb);
    	
    	SinkerManager.instance().registry(dsinker);
    	
    	context.setTracing(conf.getBoolean(Config.CAMEL_TRACING));
    	context.start();
	}
	
	public static ServiceInitializer instance() {
		if (instance == null) {
			instance = new ServiceInitializer();
		} 
		return instance;
	}
	
	public static void init(ConfigurableEnvironment env) throws Exception {
		instance();
		instance.setEnv(env);
		instance._init();
	}
}

