package com.embian.engine;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;

import com.embian.engine.conf.Config;

@Component
public class SimpleCORSFilter implements Filter {
	private static String ACCESS_CONTROL_ALLOW_ORIGIN = null;

	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
		HttpServletResponse response = (HttpServletResponse) res;
		response.setHeader("Access-Control-Allow-Origin", ACCESS_CONTROL_ALLOW_ORIGIN);
		response.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE");
		response.setHeader("Access-Control-Max-Age", "3600");
		response.setHeader("Access-Control-Allow-Credentials", "true");
		response.setHeader("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept");
		chain.doFilter(req, res);
	}

	public void init(FilterConfig filterConfig) {
		ACCESS_CONTROL_ALLOW_ORIGIN = getAccessControlAllowOrigin();
	}

	public void destroy() {}
	
	public String getAccessControlAllowOrigin() {
		Properties pro = new Properties();
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("application.properties"); 
			pro.load(new InputStreamReader(fis));
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				fis.close();
			} catch (IOException e) { 
				e.printStackTrace();
			}
		}
		return pro.getProperty(Config.ACCESS_CONTROL_ALLOW_ORIGIN);
	}

}
