package org.example.netty.webserver;

import java.util.ArrayList;

import io.netty.handler.codec.http.HttpMethod;

/**
 * TODO thread safe and performance
 * 
 * @author gang
 *
 */
public class RouteTable {
    private final ArrayList<Route> routes;

    public RouteTable() {
        this.routes = new ArrayList<Route>();
    }

    public void addRoute(final Route route) {
        this.routes.add(route);
    }

    public Route findRoute(final HttpMethod method, final String path) {
        for (final Route route : routes) {
            if (route.matches(method, path)) {
                return route;
            }
        }

        return null;
    }
}
