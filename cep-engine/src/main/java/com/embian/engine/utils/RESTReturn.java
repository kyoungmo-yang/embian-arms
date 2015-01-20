package com.embian.engine.utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class RESTReturn {

	/**
	 * Generates modelMap to return in the modelAndView
	 * 
	 * @param contacts
	 * @return
	 */
	public static Map<String, Object> mapOK(Object data, String ... messages) {
		Map<String, Object> modelMap = new HashMap<String, Object>(3);
		
		modelMap.put("data", data);
		modelMap.put("success", true);
		modelMap.put("message", Arrays.toString(messages));

		return modelMap;
	}
	
	/**
	 * Generates modelMap to return in the modelAndView
	 * 
	 * @param contacts
	 * @return
	 */
	public static Map<String, Object> mapOK(List<?> data) {

		Map<String, Object> modelMap = new HashMap<String, Object>(3);
		modelMap.put("total", data.size());
		modelMap.put("data", data);
		modelMap.put("success", true);

		return modelMap;
	}

	public static Map<String, Object> mapOK(Map<String, ?> data) {
		Map<String, Object> modelMap = new HashMap<String, Object>(2);
		modelMap.put("data", data);
		modelMap.put("success", true);

		return modelMap;
	}

	/**
	 * Generates modelMap to return in the modelAndView
	 * 
	 * @param contacts
	 * @return
	 */
	public static Map<String, Object> mapOK(List<?> data, int total) {

		Map<String, Object> modelMap = new HashMap<String, Object>(3);
		modelMap.put("total", total);
		modelMap.put("data", data);
		modelMap.put("success", true);

		return modelMap;
	}

	/**
	 * Generates modelMap to return in the modelAndView in case of exception
	 * 
	 * @param msg
	 *            message
	 * @return
	 */
	public static Map<String, Object> mapError(String msg) {

		Map<String, Object> modelMap = new HashMap<String, Object>(2);
		modelMap.put("message", msg);
		modelMap.put("success", false);

		return modelMap;
	}
}
