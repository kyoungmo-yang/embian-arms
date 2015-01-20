package com.embian.engine.memcached;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import net.spy.memcached.AddrUtil;
import net.spy.memcached.ConnectionFactoryBuilder;
import net.spy.memcached.MemcachedClient;

import org.apache.log4j.Logger;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Serie;

import com.embian.engine.ServiceInitializer;
import com.embian.engine.conf.Config;
import com.embian.engine.sinker.DashboardSinker;

public class SBCache {
	private static final Logger LOG = Logger.getLogger(SBCache.class);
	
	private static final int INTERVAL = ServiceInitializer.instance().getConfig().getInteger(Config.CACHE_SHORT_TERM_INTERVAL);
	private static final int _INTERVAL = INTERVAL / 2;
	
//	private static final int LONG_TERM_INTERVAL = ServiceInitializer.instance().getConfig().getInteger(Config.CACHE_LONG_TERM_INTERVAL);
	
	private static final String INFLUXDB_HOST 		= DashboardSinker.INFLUXDB_HOST;
	private static final int INFLUXDB_PORT 			= DashboardSinker.INFLUXDB_PORT;
	private static final String INFLUXDB_USERNAME 	= DashboardSinker.INFLUXDB_USERNAME;
	private static final String INFLUXDB_PASSWORD 	= DashboardSinker.INFLUXDB_PASSWORD;
	private static final String HTTP_PREFIX = "http://";
	
	private static final String MEMCACHED_HOSTNAME = ServiceInitializer.instance().getConfig().getString(Config.MEMCACHED_HOSTNAME);
	private static final int MEMCACHED_PORT = ServiceInitializer.instance().getConfig().getInteger(Config.MEMCACHED_PORT);
	
	private static final Map<String, Long> CACHE_TIMESTAMP = new HashMap<String, Long>();
	
	private InfluxDB influxdbConn;
	private MemcachedClient memConn;
	
	public SBCache(){
	}
	
	private synchronized boolean _requiredCache(String query) {
		Long stamp = System.currentTimeMillis();
		
		if (!CACHE_TIMESTAMP.containsKey(query)) {
			CACHE_TIMESTAMP.put(query, stamp);
			return true;
		}
		
		stamp = CACHE_TIMESTAMP.get(query);
		Long interval = TimeUnit.SECONDS.convert((System.currentTimeMillis() - stamp), TimeUnit.MILLISECONDS);
		if (interval <= _INTERVAL) {
			CACHE_TIMESTAMP.put(query, stamp);
			return true;
		}
		
		return false;
	}
	
	private synchronized void _cache(final String query, final List<Serie> series) {
		try {
			this._requiredCache(query);
			if (series != null) {
				memConn.set(query, INTERVAL, series);
			}
		} catch (Exception e) {
			LOG.error(e);
			if (this.memConn != null) {
				this.memConn.shutdown();
			}
			this.memConn = null;
		}
	}
	
	private synchronized void _cache(final String query) {
		try {
			if (this._requiredCache(query)) {
				new Thread() {
					@Override
					public void run() {
						List<Serie> series = _get(query);
						if (series != null) {
							memConn.set(query, INTERVAL, series);
						}
					}
				}.start();
				
			}
		} catch (Exception e) {
			LOG.error(e);
			if (this.memConn != null) {
				this.memConn.shutdown();
			}
			this.memConn = null;
		}
	}
	
	private List<Serie> _query(String database, String query) {
		return this.influxdbConn.query(database, query, TimeUnit.SECONDS);
	}
	
	@SuppressWarnings("unchecked")
	private List<Serie> _get(String query) {
		List<Serie> series = null;
		try {
			series = (List<Serie>) this.memConn.get(query);
		} catch (Exception e) {
			LOG.error(e);
			if (this.memConn != null) {
				this.memConn.shutdown();
			}
			this.memConn = null;
		}
		
		return series;
	}
	
	public boolean cache(String key, int exp, Object obj) {
		if (this.memConn == null && !connectM()) {
			if (this.memConn == null) {
				return false;
			}
		}
		
		memConn.set(key, exp, obj);
		return true;
	}
	
	public boolean uncache(String key) {
		if (this.memConn == null && !connectM()) {
			if (this.memConn == null) {
				return false;
			}
		}
		
		memConn.delete(key);
		return true;
	}
	
	public Object get(String key) {
		if (this.memConn == null && !connectM()) {
			if (this.memConn == null) {
				return null;
			}
		}
		
		return this.memConn.get(key);
	}
	
	public List<Serie> query(String database, String query, boolean cache) {
		if (influxdbConn == null) {
			connectI(); 
		}
		
		if (cache && this.memConn == null && !connectM()) {
			cache = false;
			this.memConn = null;
		}
		
		List<Serie> series = null;
		
		if (cache) {
			series = this._get(query);
		}
		
		if (series == null) {
			series = this._query(database, query);
			if (cache) {
				this._cache(query, series);
			}
		} else {
			this._cache(query);
		}
		
		return series;
	}
	
	public void shutdown() {
		if (this.memConn != null) {
			this.memConn.shutdown();
		}
		
		if (this.influxdbConn != null) {
			this.influxdbConn = null;
		}
	}

	private boolean connectM() {
		try {
			ConnectionFactoryBuilder builder = new ConnectionFactoryBuilder();
			builder = builder.setProtocol( ConnectionFactoryBuilder.Protocol.BINARY );
			builder = builder.setOpQueueMaxBlockTime( 1000 );
			
			this.memConn = new MemcachedClient( builder.build(), AddrUtil.getAddresses( MEMCACHED_HOSTNAME+":"+MEMCACHED_PORT ) );
			
			return true;
		} catch (Exception e) {
			LOG.error(e);
			return false;
		}
	}

	private boolean connectI() {
		try {
			this.influxdbConn = InfluxDBFactory.connect(new StringBuffer(HTTP_PREFIX).append(INFLUXDB_HOST).append(':').append(INFLUXDB_PORT).toString(), INFLUXDB_USERNAME, INFLUXDB_PASSWORD);
			
			return true;
		} catch (Exception e) {
			LOG.error(e);
			return false;
		}
	}
}
