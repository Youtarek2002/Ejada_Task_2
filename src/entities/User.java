package entities;

import enums.Role;

import java.util.ArrayList;
import java.util.List;

public abstract class User {
    private String id;
    private String name;
    Role role;
    List<String> borrowedBooks = new ArrayList<>();

    public User(String id, String name, Role role) {
        this.id = id;
        this.name = name;
        this.role = role;
    }

    public String getId()
    {
        return id;
    }
    public String getName()
    {
        return name;
    }
    public Role getRole() {
        return role;
    }
}
