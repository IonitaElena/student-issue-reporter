package com.studentissue.api_gateway.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.util.*;

public class IdentityHeaderRequest extends HttpServletRequestWrapper {

    private static final Set<String> MANAGED =
            Set.of("x-user-id", "x-user-email", "x-user-role");

    private final Map<String, String> extra = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    public IdentityHeaderRequest(HttpServletRequest request, Map<String, String> identityHeaders) {
        super(request);
        this.extra.putAll(identityHeaders);
    }

    private boolean managed(String name) {
        return name != null && MANAGED.contains(name.toLowerCase());
    }

    @Override
    public String getHeader(String name) {
        return managed(name) ? extra.get(name) : super.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        if (managed(name)) {
            String value = extra.get(name);
            return Collections.enumeration(value == null ? List.of() : List.of(value));
        }
        return super.getHeaders(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        Set<String> names = new LinkedHashSet<>();
        for (String name : Collections.list(super.getHeaderNames())) {
            if (!managed(name)) {
                names.add(name);
            }
        }
        names.addAll(extra.keySet());
        return Collections.enumeration(names);
    }
}