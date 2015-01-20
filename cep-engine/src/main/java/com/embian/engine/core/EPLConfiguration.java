package com.embian.engine.core;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventType;

public interface EPLConfiguration extends Serializable {
	
//	/**
//	 * statement의 결과를 output queue에 전송 
//	 * @param eplName - statement name
//	 * @param routingKey - queue routing key
//	 * @return 성공여부
//	 */
//	public boolean addOutputEPL(String eplName, String routingKey);
//	
//	/**
//	 * statement의 결과를 output queue에 전송하지 않음
//	 * @param eplName - statement name
//	 * @return 성공여부
//	 */
//	public boolean removeOutputEPL(String eplName);
//	
//	/**
//	 * statement가 output queue로 결과를 전송하는지 여부 리턴
//	 * @param eplName
//	 * @return queue routing key
//	 */
//	public String doOutputPEL(String eplName);
	
	/**
	 * Event type 추가
	 * @param eventTypeName
	 * @param typeMap
	 */
	public void addEventType(String eventTypeName, Map<String, Object> typeMap);
	
	/**
	 * Event Type 삭제 
	 * @param eventTypeName
	 * @return 삭제여부
	 */
	public boolean removeEventType(String eventTypeName);
	
	/**
	 * Event Type 리스트 리턴
	 * @return {@link EventType[]}
	 */
	public EventType[] getEventTypes();
	
	/**
	 * Event type 리턴
	 * @param eventTypeName
	 * @return {@link EventType}
	 */
	public EventType getEventType(String eventTypeName);
	
	
	public boolean hasEventType(String eventTypeName);
	
	
	/**
	 * Event type을 사용하는 statement 목록 리턴
	 * @param eventTypeName
	 * @return statement name 리스트
	 */
	public Set<String> getStatementNamesUsedBy(String eventTypeName);
	
	//===================================================================================
	
	/**
	 * Add epl statement
	 * @param name
	 * @param eplStatement
	 * @param routingKey - queue routing key (name.nohup | name.dashboard | name.alert)
	 * @return {@link EPStatement}
	 */
	public EPStatement addStatement(String name, String eplStatement, String routingKey);
	
	/**
	 * Remove epl statement
	 * @param name
	 * @return {@link EPStatement}
	 */
	public EPStatement removeStatement(String name);
	
	/**
	 * Update epl statement
	 * @param name
	 * @param routingKey - queue routing key (name.nohup | name.dashboard | name.alert)
	 * @return {@link EPStatement}
	 */
	public EPStatement updateStatement(String name, String routingKey);
	
	/**
	 * Return epl statement matched by name
	 * @param name
	 * @return {@link EPStatement}
	 */
	public EPStatement getStatement(String name);
	
	/**
	 * Whether the statement is registered or not the statement
	 * @param name
	 * @return ture/false 
	 */
	public boolean hasStatement(String name);
	
	/**
	 * Return array of statement names 
	 * @return {@link String[]} - statement names
	 */
	public String[] getStatementNames();
	
	/**
	 * Return number of statements
	 * @return int
	 */
	public int numberOfStatements();
}
