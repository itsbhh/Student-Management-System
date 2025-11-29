package org.example;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;

import java.util.*;

@ApplicationScoped
public class StudentService {

    private final CqlSession session = CassandraConnector.getSession();


    public Response addStudent(Student s) {
        if (s.id == null || !s.id.matches("^S[0-9]{4}$")) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Invalid Student ID. Must be 'S' followed by 4 digits.")).build();
        }

        if (!isValidMarks(s.subject1, s.subject2, s.subject3)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Marks must be between 1 and 100")).build();
        }

        PreparedStatement check = session.prepare("SELECT id FROM students WHERE id = ?");
        if (session.execute(check.bind(s.id)).one() != null) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(Map.of("error", "Student already exists")).build();
        }

        PreparedStatement insert = session.prepare("""
                    INSERT INTO students (id, name, subject1, subject2, subject3, password)
                    VALUES (?, ?, ?, ?, ?, ?)
                """);
        session.execute(insert.bind(s.id, s.name, s.subject1, s.subject2, s.subject3, "pass1"));

        updateRanks();
        return Response.ok(Map.of("message", "Student added successfully")).build();
    }


    public Response updateStudent(Student s) {
        if (s.id == null || s.id.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Student ID required")).build();
        }

        if (!isValidMarks(s.subject1, s.subject2, s.subject3)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Marks must be between 1 and 100")).build();
        }

        PreparedStatement check = session.prepare("SELECT id FROM students WHERE id = ?");
        if (session.execute(check.bind(s.id)).one() == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Student not found")).build();
        }

        PreparedStatement update = session.prepare("""
                    UPDATE students SET name = ?, subject1 = ?, subject2 = ?, subject3 = ? WHERE id = ?
                """);
        session.execute(update.bind(s.name, s.subject1, s.subject2, s.subject3, s.id));

        updateRanks();
        return Response.ok(Map.of("message", "Student updated successfully")).build();
    }


    public Response deleteStudent(String id) {
        PreparedStatement check = session.prepare("SELECT id FROM students WHERE id = ?");
        if (session.execute(check.bind(id)).one() == null) {
            return Response.status(Response.Status.NOT_FOUND).entity(Map.of("error", "Student not found")).build();
        }

        session.execute(session.prepare("DELETE FROM students WHERE id = ?").bind(id));
        updateRanks();
        return Response.ok(Map.of("message", "Student deleted successfully")).build();
    }

    public Response getStudent(String id) {
        Row row = session.execute(session.prepare("SELECT * FROM students WHERE id = ?").bind(id)).one();
        if (row == null)
            return Response.status(Response.Status.NOT_FOUND).entity(Map.of("error", "Student not found")).build();

        return Response.ok(Map.of(
                "id", row.getString("id"),
                "name", row.getString("name"),
                "subject1", row.getInt("subject1"),
                "subject2", row.getInt("subject2"),
                "subject3", row.getInt("subject3"),
                "total", row.getInt("total"),
                "percentage", row.getDouble("percentage"),
                "rank", row.getInt("rank")
        )).build();
    }

    public Response getAllStudents() {
        List<Map<String, Object>> students = new ArrayList<>();
        for (Row r : session.execute("SELECT * FROM students")) {
            students.add(Map.of(
                    "id", r.getString("id"),
                    "name", r.getString("name"),
                    "subject1", r.getInt("subject1"),
                    "subject2", r.getInt("subject2"),
                    "subject3", r.getInt("subject3"),
                    "total", r.getInt("total"),
                    "percentage", r.getDouble("percentage"),
                    "rank", r.getInt("rank")
            ));
        }
        students.sort(Comparator.comparingInt(s -> (int) s.get("rank")));
        return Response.ok(students).build();
    }

    private void updateRanks() {
        List<Row> rows = session.execute("SELECT id, subject1, subject2, subject3 FROM students").all();
        List<Map.Entry<String, Integer>> totals = new ArrayList<>();

        for (Row r : rows) {
            int total = r.getInt("subject1") + r.getInt("subject2") + r.getInt("subject3");
            totals.add(Map.entry(r.getString("id"), total));
        }

        totals.sort((a, b) -> b.getValue() - a.getValue());
        PreparedStatement update = session.prepare("UPDATE students SET total = ?, percentage = ?, rank = ? WHERE id = ?");

        for (int i = 0; i < totals.size(); i++) {
            String id = totals.get(i).getKey();
            int total = totals.get(i).getValue();
            double percent = total / 3.0;
            session.execute(update.bind(total, percent, i + 1, id));
        }
    }

    private boolean isValidMarks(int... marks) {
        for (int mark : marks) {
            if (mark < 1 || mark > 100) return false;
        }
        return true;
    }

}