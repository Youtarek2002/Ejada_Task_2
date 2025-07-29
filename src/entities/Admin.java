package entities;

import enums.Role;

public class Admin extends User {
    public Admin(String id, String name) {
        super(id, name, Role.ADMIN);
    }
}