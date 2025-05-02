package org.vaadin.example;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConfig {
    public static final String URL = "jdbc:mysql://localhost:3306/phonebook_db";
    public static final String USER = "root";
    public static final String PASSWORD = "Temp12340987";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
