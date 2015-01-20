package com.embian.engine.controller;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.embian.engine.ServiceInitializer;
import com.embian.engine.conf.Config;
import com.embian.engine.core.AlertUpdateListener;
import com.embian.engine.core.EsperEngine;
import com.embian.engine.memcached.SBCache;
import com.embian.engine.sbarm.SBArmManager;
import com.embian.engine.sinker.AlertSinker;
import com.embian.engine.sinker.DashboardSinker;
import com.embian.engine.utils.RESTReturn;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.StatementAwareUpdateListener;
import com.espertech.esper.client.annotation.Description;
import com.espertech.esper.client.annotation.Name;

@RestController
public class StmtController {
	public static final String STMT_LIST_CACHE_KEY = "__stmt_list";
	
	private static final Logger LOG = Logger.getLogger(StmtController.class);
	private SBCache sbcache;
	
	private void _initSBCache() {
		if (sbcache == null) {
			sbcache = new SBCache();
		}
	}
	
	@SuppressWarnings("unchecked")
	private void _cacheStmt(String stmtId, Map<String, Object> stmt) {
		try {
			this._initSBCache();
			
			Object o = this.sbcache.get(STMT_LIST_CACHE_KEY);
			if (o == null) {
				Set<String> set = new HashSet<String>();
				set.add(stmtId);
				this.sbcache.cache(STMT_LIST_CACHE_KEY, Integer.MAX_VALUE, set);
			} else {
				((Set<String>)o).add(stmtId);
				this.sbcache.cache(STMT_LIST_CACHE_KEY, Integer.MAX_VALUE, o);
			}
			
			this.sbcache.cache(stmtId, Integer.MAX_VALUE, stmt);
		} catch (Exception e) {
			LOG.error(e);
		}
	}
	
	@SuppressWarnings("unchecked")
	private void _uncacheStmt(String stmtId) {
		try {
			this._initSBCache();
			
			Object o = this.sbcache.get(STMT_LIST_CACHE_KEY);
			if (o != null) {
				((Set<String>)o).remove(stmtId);
				this.sbcache.cache(STMT_LIST_CACHE_KEY, Integer.MAX_VALUE, o);
			}
			
			this.sbcache.uncache(stmtId);
		} catch (Exception e) {
			LOG.error(e);
		}
	}
	
	private boolean _logined(HttpSession session) {
		Config conf = ServiceInitializer.instance().getConfig();
		String username = conf.getString(Config.AUTH_USERNAME);
		
		if (session.getAttribute(Config.AUTH_USERNAME) == null || !session.getAttribute(Config.AUTH_USERNAME).equals(username)) {
			return false;
		}
		
		return true;
	}
	
	/**
	 * 
	 * @param name
	 * @param epl
	 * @param routingKey
	 * @param level : l1:주의, l2:경고, l3:위험
	 * @param toMail
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/put_stmt", method = RequestMethod.POST)
    public @ResponseBody Map<String,? extends Object> putStmt(@RequestParam String name, @RequestParam(required=false) String desc, @RequestParam String epl, @RequestParam String routingKey, @RequestParam(required=false) String level, @RequestParam(required=false) String toMail, HttpSession session) throws Exception {
		try {
			if (!_logined(session)) {
				return RESTReturn.mapError("Required login");
			}
			
	    	EsperEngine engine = EsperEngine.instance();
	    	SBArmManager sbm = SBArmManager.instance();
	    	Map<String, Object> stmt = new HashMap<String, Object>(); 
	    	
	    	StringBuffer buf = new StringBuffer();
	    	String stmtId = null;
	    	String rk = null;
	    	String eplBody = null;
	    	
	    	if (level == null) {
				level = SBArmManager.LEVEL1;
			}
	    	
	    	if (!sbm.isValidLevel(level)) {
	    		return RESTReturn.mapError("Invalid level params(e.g., l1, l2, l3)");
			}
	    	
	    	// makes statement id
	    	stmtId = buf.append(name.hashCode()).toString();
	    	if (engine.hasStatement(stmtId)) {
	    		return RESTReturn.mapError(new StringBuffer("The statement (").append(name).append(") is already exist (1)").toString());
	    	}
	    	
	    	// makes statement annotation & body
	    	buf.setLength(0);
			buf.append("@Name(\"").append(name).append("\")");
			buf.append("@Description(\"").append(desc).append("\")");
			eplBody = buf.append(epl).toString();
			
			// makes routing key
			buf.setLength(0);
			rk = buf.append(routingKey).append(stmtId).toString();
			
			stmt.put("stmtId", stmtId);
			stmt.put("name", name);
			stmt.put("epl", epl);
			stmt.put("routingKey", routingKey);
	    	
			stmt.put("desc", desc);
			stmt.put("mail", toMail);
			stmt.put("level", level);
			stmt.put("edit", false);
			
	    	if (routingKey.startsWith(AlertSinker.ROUTING_KEY_PREFIX)) {
	    		if (toMail != null) {
	    			engine.addStatement(stmtId, eplBody, rk);
	    			
	    			sbm.putLevel(stmtId, level);
	    			sbm.putMail(stmtId, toMail);
	    			AlertSinker.registryAlertSinker(rk, toMail);
	    			DashboardSinker.registryDashSinker(rk);
	    			
	    			this._cacheStmt(stmtId, stmt);
	    			return RESTReturn.mapOK(stmt);
				}
				return RESTReturn.mapError("The toMail parameter is reuqired");
	    	}
	    	else if(routingKey.startsWith(DashboardSinker.ROUTING_KEY_PREFIX)){
	    		engine.addStatement(stmtId, eplBody, rk);
	    		DashboardSinker.registryDashSinker(rk);
	    		
	    		
	    		this._cacheStmt(stmtId, stmt);
	    		return RESTReturn.mapOK(stmt);
	    	}
	    	
	    	return RESTReturn.mapError(new StringBuffer("The statement (").append(name).append(") is already exist (2)").toString());
		} catch (Exception e) {
			LOG.error(e);
			return RESTReturn.mapError(e.getLocalizedMessage());
		}
    }
	
	
	@RequestMapping(value = "/remove_stmt", method = RequestMethod.POST)
    public @ResponseBody Map<String,? extends Object> removeStmt(@RequestParam String stmtId, @RequestParam String routingKey, HttpSession session) throws Exception {
		try {
			if (!_logined(session)) {
				return RESTReturn.mapError("Required login");
			}
			
	    	EsperEngine engine = EsperEngine.instance();
	    	SBArmManager sbm = SBArmManager.instance();
	    	
	    	// makes routing key
	    	StringBuffer rk = new StringBuffer(); 
	    	rk.append(routingKey).append(stmtId).toString();
	    	
	    	if (engine.hasStatement(stmtId)) {
	    		engine.removeStatement(stmtId);
	    		sbm.removeLevel(stmtId);
	    		sbm.removeMail(stmtId);
				if (routingKey.startsWith(AlertSinker.ROUTING_KEY_PREFIX)) {
					AlertSinker.unregistryAlertSinker(rk.toString());
					DashboardSinker.unregistryDashSinker(rk.toString());
				} else {
					DashboardSinker.unregistryDashSinker(rk.toString());
				}
	    		
				this._uncacheStmt(stmtId);
				return RESTReturn.mapOK(stmtId);
			}
				
			return RESTReturn.mapError(new StringBuffer("The statement (").append(stmtId).append(") is not exist").toString());
		} catch (Exception e) {
			LOG.error(e);
			return RESTReturn.mapError(e.getLocalizedMessage());
		}
    }
	
	@RequestMapping(value = "/list_stmts", method = RequestMethod.POST)
    public @ResponseBody Map<String,? extends Object> listStmt(HttpSession session) {
		try {
			if (!_logined(session)) {
				return RESTReturn.mapError("Required login");
			}
			
			EsperEngine engine = EsperEngine.instance();
			String[] stmtNames = engine.getStatementNames();
			List<Map<String, Object>> stmts = new Vector<Map<String, Object>>();
			for (String stmtName : stmtNames) {
				Map<String, Object> mstmt = new HashMap<String, Object>();
				EPStatement stmt = engine.getStatement(stmtName);
				mstmt.put("stmtId", stmt.getName());
				mstmt.put("name", stmt.getName());
				mstmt.put("epl", stmt.getText());
				Iterator<StatementAwareUpdateListener> i = stmt.getStatementAwareListeners();
				while(i.hasNext()) {
					StatementAwareUpdateListener l = i.next();
					if (l instanceof AlertUpdateListener) {
						mstmt.put("routingKey", ((AlertUpdateListener)l).getRoutingKey());
						break;
					}
				}
				stmts.add(mstmt);
			}
			
	        return RESTReturn.mapOK(stmts);
		} catch (Exception e) {
			LOG.error(e);
			return RESTReturn.mapError(e.getLocalizedMessage());
		}
    }
	
	@RequestMapping(value = "/list_alert_stmts", method = RequestMethod.POST)
    public @ResponseBody Map<String,? extends Object> listAlertStmt(HttpSession session) {
		try {
			if (!_logined(session)) {
				return RESTReturn.mapError("Required login");
			}
			
			EsperEngine engine = EsperEngine.instance();
			SBArmManager sbm = SBArmManager.instance();
			String[] stmtNames = engine.getStatementNames();
			
			Map<String, List<Map<String, Object>>> stmts = new HashMap<String, List<Map<String, Object>>>();
			stmts.put(SBArmManager.LEVEL1, new Vector<Map<String, Object>>());
			stmts.put(SBArmManager.LEVEL2, new Vector<Map<String, Object>>());
			stmts.put(SBArmManager.LEVEL3, new Vector<Map<String, Object>>());
			
			for (String stmtName : stmtNames) {
				String routingKey = null;
				String name = null;
				String desc = null;
				
				if(stmts.containsKey(sbm.getLevel(stmtName))) {
					EPStatement stmt = engine.getStatement(stmtName);
					Map<String, Object> s = new HashMap<String, Object>();
					
					boolean hasListener = false;
					Iterator<StatementAwareUpdateListener> i = stmt.getStatementAwareListeners();
					while(i.hasNext()) {
						StatementAwareUpdateListener l = i.next();
						if (l instanceof AlertUpdateListener) {
							hasListener = true;
							routingKey = ((AlertUpdateListener)l).getRoutingKey();
							if (routingKey == null) {
								continue;
							}
							
							if (routingKey.startsWith(AlertSinker.ROUTING_KEY_PREFIX)) {
								s.put("routingKey", AlertSinker.ROUTING_KEY_PREFIX);
								break;
								
							} else if (routingKey.startsWith(DashboardSinker.ROUTING_KEY_PREFIX)) {
								
								s.put("routingKey", DashboardSinker.ROUTING_KEY_PREFIX);
								break;
							}
						}
					}
					
					if (!hasListener) {
						// the stmt is not alert statements
						continue;
					}
					
					for (Annotation anno : stmt.getAnnotations()) {
						if (anno instanceof Name) {
							name = ((Name) anno).value();
							
						} else if (anno instanceof Description) {
							desc = ((Description) anno).value();
						}
					}
					
					s.put("stmtId", stmt.getName());
					s.put("name", name);
					s.put("desc", desc);
					s.put("epl", stmt.getText().replaceAll("(@Name|@Description)\\([^\\)]*\\)", ""));
					s.put("mail", sbm.getMail(stmtName));
					s.put("edit", false);
					
					List<Map<String, Object>> m = stmts.get(sbm.getLevel(stmtName));
					m.add(s);
				} else {
					LOG.info(stmtName + " is not leveling [" + sbm.getLevel(stmtName) + "]");
				}
			}
			
	        return RESTReturn.mapOK(stmts);
		} catch (Exception e) {
			LOG.error(e);
			return RESTReturn.mapError(e.getLocalizedMessage());
		}
    } 
}
