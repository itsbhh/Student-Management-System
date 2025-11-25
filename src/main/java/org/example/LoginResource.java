package org.example;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.net.URI;
import java.util.*;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class LoginResource {
    CqlSession session = CassandraConnector.getSession();
    static Set<String> defaultPasswords = Set.of("pass1", "teacherpass1");

    @POST
    @Path("/login")
    public Response login(Map<String, String> body) {
        String id = body.get("id");
        String password = body.get("password");

        if (id == null || password == null) {
            return Response.ok(Map.of("error", "Missing ID or Password")).build();
        }

        String role = null;
        Row row = null;

        if (id.startsWith("S")) {
            PreparedStatement stmt = session.prepare("SELECT password FROM students WHERE id = ?");
            row = session.execute(stmt.bind(id)).one();
            role = "student";
        } else if (id.startsWith("T")) {
            PreparedStatement stmt = session.prepare("SELECT password FROM teachers WHERE id = ?");
            row = session.execute(stmt.bind(id)).one();
            role = "teacher";
        } else if (id.startsWith("Admin12")) {
            if (id.equals("Admin12ABCDE") && password.equals("adminpassword")) {
                return Response.ok(Map.of("id", id, "role", "admin")).build();
            } else {
                return Response.ok(Map.of("error", "Invalid Admin credentials")).build();
            }
        } else {
            return Response.ok(Map.of("error", "Invalid ID format")).build();
        }

        if (row == null) return Response.ok(Map.of("error", "ID not found")).build();

        String storedPassword = row.getString("password");
        if (storedPassword == null || !storedPassword.equals(password)) {
            return Response.ok(Map.of("error", "Incorrect password")).build();
        }

        if (defaultPasswords.contains(password)) {
            return Response.ok(generateSetPasswordForm(id, role)).type(MediaType.TEXT_HTML).build();
        }

        return Response.ok(Map.of("id", id, "role", role)).build();
    }


    @POST
    @Path("/set-password")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response updatePassword(@FormParam("id") String id,
                                   @FormParam("role") String role,
                                   @FormParam("password") String newPassword) {
        if (id == null || role == null || newPassword == null ||
                id.isBlank() || role.isBlank() || newPassword.isBlank()) {
            return Response.ok("<h2 style='color:red'>You must be logged in to update your password.</h2>").build();
        }

        String table = role.equals("student") ? "students"
                : role.equals("teacher") ? "teachers" : null;

        if (table == null) {
            return Response.ok("<h2 style='color:red'>Invalid role</h2>").build();
        }

        PreparedStatement stmt = session.prepare("UPDATE " + table + " SET password = ? WHERE id = ?");
        session.execute(stmt.bind(newPassword, id));


        return Response.seeOther(URI.create("/?id=" + id + "&role=" + role)).build();

    }

    private String generateSetPasswordForm(String id, String role) {
        return String.format("""
                    <html><body>
                        <h2>Change Default Password</h2>
                        <form method='post' action='/auth/set-password'>
                            <input type='hidden' name='id' value='%s' />
                            <input type='hidden' name='role' value='%s' />
                            <label>New Password</label>
                            <input type='password' name='password' required />
                            <button type='submit'>Update Password</button>
                        </form>
                    </body></html>
                """, id, role);
    }

}
