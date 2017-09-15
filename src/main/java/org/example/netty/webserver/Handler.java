package org.example.netty.webserver;

import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;

public interface Handler {
	Object handle(HttpRequest request) throws Exception;
}
