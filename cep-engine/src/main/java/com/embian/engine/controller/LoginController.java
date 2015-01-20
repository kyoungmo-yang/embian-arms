package com.embian.engine.controller;

import java.io.UnsupportedEncodingException;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.embian.engine.ServiceInitializer;
import com.embian.engine.conf.Config;
import com.embian.engine.utils.RESTReturn;

@RestController
public class LoginController {
	private static final Logger LOG = Logger.getLogger(LoginController.class);
	
	@RequestMapping(value = "/login", method = RequestMethod.POST)
    public @ResponseBody Map<String,? extends Object> login(@RequestParam String username, @RequestParam String password, HttpSession session) throws UnsupportedEncodingException {
		if (username == null ||password == null) {
			return RESTReturn.mapError("Invalid authentication:" + username);
		}
		
		Config conf = ServiceInitializer.instance().getConfig();
		String id = conf.getString(Config.AUTH_USERNAME);
		String pass = conf.getString(Config.AUTH_PASSWORD);
		
		if (!id.equals(username)) {
			return RESTReturn.mapError("Invalid authentication:" + username);
		}
		
		if (!pass.equals(password)) {
			return RESTReturn.mapError("Invalid password");
		}

		session.setAttribute(Config.AUTH_USERNAME, username);
		
        return RESTReturn.mapOK("login");
    }
	
	@RequestMapping(value = "/logout", method = RequestMethod.POST)
    public @ResponseBody Map<String,? extends Object> logout(HttpSession session) {
		session.setAttribute(Config.AUTH_USERNAME, null);
		
        return RESTReturn.mapOK("logout");
    }
}
