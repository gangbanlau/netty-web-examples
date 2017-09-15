package org.example.netty.webserver;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.netty.handler.codec.http.HttpMethod;

public class Route {
    private final HttpMethod method;
    private final Pattern uriPattern;
    private final Handler handler;

    public Route(final HttpMethod method, Pattern uriPattern, final Handler handler) {
        this.method = method;
        this.uriPattern = uriPattern;
        this.handler = handler;
    }

    public HttpMethod getMethod() {
        return method;
    }

    public Handler getHandler() {
        return handler;
    }

    public boolean matches(final HttpMethod method, final String path) {
        if (!this.method.equals(method))
        	return false;
        
        final Matcher matcher = uriPattern.matcher(path);
        
        return matcher.matches();
    }
}
