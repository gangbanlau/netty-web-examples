package org.example;

import org.example.netty.webserver.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.netty.handler.codec.http.HttpRequest;

@Component
public class HelloHandler implements Handler {
	private static final Logger logger = LoggerFactory.getLogger(HelloHandler.class);
	
	@Override
	public Object handle(HttpRequest request) throws Exception {
		return "world";
	}

}
