package org.example;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import jakarta.inject.Inject;

import java.util.*;

@Path("/admin")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AdminResource {

    private final CqlSession session = CassandraConnector.getSession();

    private boolean isAdminAuthorized(String adminId, String role) {
        return adminId != null && role != null && role.equals("admin") && adminId.startsWith("Admin12");
    }

    // TEACHER ROUTES

    @POST
    @Path("/add-teacher")
    public Response addTeacher(Teacher t, @QueryParam("adminId") String adminId, @QueryParam("role") String role) {
        if (!isAdminAuthorized(adminId, role)) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(Map.of("error", "You must be logged in as an admin")).build();
        }

        if (t.id == null || !t.id.matches("^T[0-9]{4}$")) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Invalid Teacher ID. Must be 5 characters: 'T' followed by 4 digits (e.g., T1234)"))
                    .build();
        }
        PreparedStatement checkStmt = session.prepare("SELECT id FROM teachers WHERE id = ?");
        if (session.execute(checkStmt.bind(t.id)).one() != null) {
            return Response.status(Response.Status.CONFLICT).entity(Map.of("error", "Teacher already exists")).build();
        }

        PreparedStatement stmt = session.prepare("INSERT INTO teachers (id, name, password) VALUES (?, ?, ?)");
        session.execute(stmt.bind(t.id, t.name, "teacherpass1"));

        return Response.ok(Map.of("message", "Teacher added successfully")).build();
    }

    @GET
    @Path("/teachers")
    public Response getAllTeachers(@QueryParam("adminId") String adminId, @QueryParam("role") String role) {
        if (!isAdminAuthorized(adminId, role))
            return Response.status(Response.Status.UNAUTHORIZED).entity(Map.of("error", "Unauthorized")).build();

        List<Teacher> teachers = new ArrayList<>();
        for (Row row : session.execute("SELECT * FROM teachers")) {
            Teacher t = new Teacher();
            t.id = row.getString("id");
            t.name = row.getString("name");
            t.password = row.getString("password");
            teachers.add(t);
        }
        return Response.ok(teachers).build();
    }

    @GET
    @Path("/teacher")
    public Response getTeacher(@QueryParam("id") String id, @QueryParam("adminId") String adminId, @QueryParam("role") String role) {
        if (!isAdminAuthorized(adminId, role))
            return Response.status(Response.Status.UNAUTHORIZED).entity(Map.of("error", "Unauthorized")).build();

        Row row = session.execute(session.prepare("SELECT * FROM teachers WHERE id = ?").bind(id)).one();
        if (row == null)
            return Response.status(Response.Status.NOT_FOUND).entity(Map.of("error", "Teacher not found")).build();

        return Response.ok(Map.of(
                "id", row.getString("id"),
                "name", row.getString("name"),
                "password", row.getString("password")
        )).build();
    }

    @DELETE
    @Path("/delete-teacher")
    public Response deleteTeacher(@QueryParam("id") String id, @QueryParam("adminId") String adminId, @QueryParam("role") String role) {
        if (!isAdminAuthorized(adminId, role))
            return Response.status(Response.Status.UNAUTHORIZED).entity(Map.of("error", "Unauthorized")).build();

        if (session.execute(session.prepare("SELECT id FROM teachers WHERE id = ?").bind(id)).one() == null) {
            return Response.status(Response.Status.NOT_FOUND).entity(Map.of("error", "Teacher not found")).build();
        }

        session.execute(session.prepare("DELETE FROM teachers WHERE id = ?").bind(id));
        return Response.ok(Map.of("message", "Teacher deleted")).build();
    }

    // STUDENT ROUTES

    @Inject
    StudentService studentService;

    @POST
    @Path("/add-student")
    public Response addStudent(Student s, @QueryParam("adminId") String adminId, @QueryParam("role") String role) {
        if (!isAdminAuthorized(adminId, role))
            return Response.status(Response.Status.UNAUTHORIZED).entity(Map.of("error", "Unauthorized")).build();

        return studentService.addStudent(s);
    }

    @PUT
    @Path("/update-student")
    public Response updateStudent(Student s, @QueryParam("adminId") String adminId, @QueryParam("role") String role) {
        if (!isAdminAuthorized(adminId, role))
            return Response.status(Response.Status.UNAUTHORIZED).entity(Map.of("error", "Unauthorized")).build();

        return studentService.updateStudent(s);
    }

    @DELETE
    @Path("/delete-student")
    public Response deleteStudent(@QueryParam("id") String id,
                                  @QueryParam("adminId") String adminId,
                                  @QueryParam("role") String role) {
        if (!isAdminAuthorized(adminId, role)) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(Map.of("error", "Unauthorized")).build();
        }
        return studentService.deleteStudent(id);
    }

    @GET
    @Path("/student")
    public Response getStudent(@QueryParam("id") String id,
                               @QueryParam("adminId") String adminId,
                               @QueryParam("role") String role) {
        if (!isAdminAuthorized(adminId, role))
            return Response.status(Response.Status.UNAUTHORIZED).entity(Map.of("error", "Unauthorized")).build();

        return studentService.getStudent(id);
    }

    @GET
    @Path("/students")
    public Response getAllStudents(@QueryParam("adminId") String adminId,
                                   @QueryParam("role") String role) {
        if (!isAdminAuthorized(adminId, role))
            return Response.status(Response.Status.UNAUTHORIZED).entity(Map.of("error", "Unauthorized")).build();

        return studentService.getAllStudents();
    }
}
