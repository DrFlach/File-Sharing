package com.example.TestProject.entity;

//an enum it is a special data types for constants, like example for roles
public enum Erole {
    USER_ROLE(0), //we create three ROLES for web-site(User, admin, student)
    ADMIN_ROLE(1),
    STUDENT_ROLE(2);

    private final int value; // In here, there is a value that is associated with roles and cannot be changed
    //and Erole will have a value(0, 1, 2), where 0 represents a user, 1 represents an admin and 2 represents a student.
    Erole(int value) { // it is a constructor
        this.value = value;
    }

    public int getValue() { //it is a getter
        return value;
    }
}
