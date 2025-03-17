package com.example.demo.dao.entity;

public enum EmailTemplateName {
    ACTIVATE_ACCOUNT("activate_account"),
    PASSWORD_RESET("password-reset");


    private final String name;

    EmailTemplateName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
