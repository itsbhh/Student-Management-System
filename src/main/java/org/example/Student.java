package org.example;

public class Student {
    public String id;
    public String name;
    public String password;
    public int subject1;
    public int subject2;
    public int subject3;
    public double percentage;
    public int rank;
    public int total;
    public String role;
    public String teacherId;


    public Student() {}


    public int computeTotal() {
        return subject1 + subject2 + subject3;
    }

    public double computePercentage() {
        return computeTotal() / 3.0;
    }
}
