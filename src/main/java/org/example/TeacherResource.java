package org.example;

import com.datastax.oss.driver.api.core.cql.Row;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

import java.util.*;

@Path("/teacher")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TeacherResource {

    @Inject
    StudentService studentService;

    private boolean isTeacherAuthorized(String teacherId, String role) {
        return role != null && role.equals("teacher") && teacherId != null && teacherId.startsWith("T");
    }

    @POST
    @Path("/add-student")
    public Response addStudent(Student s, @QueryParam("teacherId") String teacherId, @QueryParam("role") String role) {
        if (!isTeacherAuthorized(teacherId, role)) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("error", "You must be logged in as a teacher")).build();
        }
        return studentService.addStudent(s);
    }

    @PUT
    @Path("/update-student")
    public Response updateStudent(Student s, @QueryParam("teacherId") String teacherId, @QueryParam("role") String role) {
        if (!isTeacherAuthorized(teacherId, role)) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("error", "You must be logged in as a teacher")).build();
        }
        return studentService.updateStudent(s);
    }

    @DELETE
    @Path("/delete-student")
    public Response deleteStudent(@QueryParam("id") String id, @QueryParam("teacherId") String teacherId, @QueryParam("role") String role) {
        if (!isTeacherAuthorized(teacherId, role)) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("error", "You must be logged in as a teacher")).build();
        }
        return studentService.deleteStudent(id);
    }

    @GET
    @Path("/student")
    public Response getStudent(@QueryParam("id") String id, @QueryParam("teacherId") String teacherId, @QueryParam("role") String role) {
        if (!isTeacherAuthorized(teacherId, role)) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("error", "You must be logged in as a teacher")).build();
        }
        return studentService.getStudent(id);
    }

    @GET
    @Path("/students")
    public Response getAllStudents(@QueryParam("teacherId") String teacherId, @QueryParam("role") String role) {
        if (!isTeacherAuthorized(teacherId, role)) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("error", "You must be logged in as a teacher")).build();
        }
        return studentService.getAllStudents();
    }

    @GET
    @Path("/teacher")
    public Response getTeacher(@QueryParam("id") String id,
                               @QueryParam("teacherId") String teacherId,
                               @QueryParam("role") String role) {

        if (id == null || !id.equals(teacherId) || !"teacher".equalsIgnoreCase(role)) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("error", "Unauthorized access")).build();
        }

        Row row = CassandraConnector.getSession()
                .execute(CassandraConnector.getSession()
                        .prepare("SELECT id, name FROM teachers WHERE id = ?")
                        .bind(id)).one();

        if (row == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Teacher not found")).build();
        }

        return Response.ok(Map.of(
                "id", row.getString("id"),
                "name", row.getString("name")
        )).build();
    }
}
