package org.example;

import jakarta.ws.rs.container.*;
import jakarta.ws.rs.core.*;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;

@Provider
@PreMatching

public class SessionFilter implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String path = requestContext.getUriInfo().getPath().toLowerCase();

        if (path.contains("auth/login") || path.contains("auth/set-password")) {
            return;
        }

        String role = requestContext.getHeaderString("Role");
        if (role == null || role.trim().isEmpty()) {
            role = requestContext.getUriInfo().getQueryParameters().getFirst("role");
        }

        if (role == null || role.trim().isEmpty()) {
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                    .entity("You must be logged in").build());
            return;
        }

        if (path.startsWith("admin") && !role.equalsIgnoreCase("admin")) {
            requestContext.abortWith(Response.status(Response.Status.FORBIDDEN)
                    .entity("Admin role required").build());
            return;
        }

        if (path.startsWith("teacher") && !(role.toLowerCase().startsWith("t") || role.equalsIgnoreCase("admin"))) {
            requestContext.abortWith(Response.status(Response.Status.FORBIDDEN)
                    .entity("Teacher or Admin role required").build());
            return;
        }

        if (path.startsWith("student") && !(role.toLowerCase().startsWith("s") || role.toLowerCase().startsWith("t") || role.equalsIgnoreCase("admin"))) {
            requestContext.abortWith(Response.status(Response.Status.FORBIDDEN)
                    .entity("Access denied to student resource").build());
        }
    }
}
