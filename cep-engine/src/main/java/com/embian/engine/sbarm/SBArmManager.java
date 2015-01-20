package com.embian.engine.sbarm;

import java.util.HashMap;
import java.util.Map;

public class SBArmManager {
	public static final String LEVEL1 = "l1";
	public static final String LEVEL2 = "l2";
	public static final String LEVEL3 = "l3";
	
		
	private static SBArmManager instance;
	
	private Map<String, String> levelToStrMap;
	
	private Map<String, String> levelMap;
	private Map<String, String> mailMap;
	
	private SBArmManager() {
		this.levelMap = new HashMap<String, String>();
		this.mailMap = new HashMap<String, String>();
		this.levelToStrMap = new HashMap<String, String>();
		this.levelToStrMap.put(LEVEL1, "주의");
		this.levelToStrMap.put(LEVEL2, "경고");
		this.levelToStrMap.put(LEVEL3, "위험");
	}
	
	
	public static SBArmManager instance() {
		if (instance == null) {
			instance = new SBArmManager();
		}
		
		return instance;
	}
	
	public String getLevelName(String level) {
		return this.levelToStrMap.get(level);
	}
	
	public boolean isValidLevel(String level) {
		if (level == null) {
			return false;
		}
		
		return level.equals(LEVEL1) || level.equals(LEVEL2) || level.equals(LEVEL3);
	}
	
	public void putLevel1(String stmtName) {
		this.putLevel(LEVEL1, stmtName);
	}
	
	public void putLevel2(String stmtName) {
		this.putLevel(LEVEL1, stmtName);
	}
	
	public void putLevel3(String stmtName) {
		this.putLevel(LEVEL1, stmtName);
	}
	////////////////////////////////////////////////
	public String getLevel(String stmtName) {
		return this.levelMap.get(stmtName);
	}
	
	public void putLevel(String stmtName, String level) {
		this.levelMap.put(stmtName, level);
	}
	
	public void removeLevel(String stmtName) {
		this.levelMap.remove(stmtName);
	}
	/////////////////////////////////////////////////
	public void putMail(String stmtName, String mail) {
		this.mailMap.put(stmtName, mail);
	}
	
	public String getMail(String stmtName) {
		return this.mailMap.get(stmtName);
	}
	
	public void removeMail(String stmtName) {
		this.mailMap.remove(stmtName);
	}
}
