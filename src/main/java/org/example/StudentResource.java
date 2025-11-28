package org.example;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.util.*;

@Path("/student")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class StudentResource {

    private final CqlSession session = CassandraConnector.getSession();

    @GET
    @Path("/{id}")
    public Response getStudentById(@PathParam("id") String id) {
        return fetchStudent(id);
    }

    @GET
    @Path("/me")
    public Response getStudentByQuery(@QueryParam("id") String id) {
        return fetchStudent(id);
    }

    private Response fetchStudent(String id) {
        PreparedStatement stmt = session.prepare("SELECT * FROM students WHERE id = ?");
        Row row = session.execute(stmt.bind(id)).one();

        if (row == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Student not found"))
                    .build();
        }

        Map<String, Object> result = Map.of(
                "id", row.getString("id"),
                "name", row.getString("name"),
                "subject1", row.getInt("subject1"),
                "subject2", row.getInt("subject2"),
                "subject3", row.getInt("subject3"),
                "total", row.getInt("total"),
                "percentage", row.getDouble("percentage"),
                "rank", row.getInt("rank")
        );

        return Response.ok(result).build();
    }
}
