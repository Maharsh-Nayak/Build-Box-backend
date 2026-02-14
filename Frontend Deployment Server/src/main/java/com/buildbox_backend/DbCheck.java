package com.buildbox_backend;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class DbCheck {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://db.yjpgyohygalmjmbyqooi.supabase.co:5432/postgres";
        String user = "postgres";
        String password = "BuildBox1824";

        try {
            Class.forName("org.postgresql.Driver");
            try (Connection conn = DriverManager.getConnection(url, user, password);
                    Statement stmt = conn.createStatement()) {

                System.out.println("Connected to database successfully.");

                // Check if alb_listener_rules table exists
                ResultSet rs = stmt.executeQuery("SELECT to_regclass('public.alb_listener_rules')");
                if (rs.next() && rs.getString(1) != null) {
                    System.out.println("✅ Table 'alb_listener_rules' exists.");
                } else {
                    System.out.println("❌ Table 'alb_listener_rules' DOES NOT exist.");
                }

                // Check flyway_schema_history
                System.out.println("\n--- Recent Migrations ---");
                rs = stmt.executeQuery(
                        "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5");
                while (rs.next()) {
                    System.out.println(rs.getString("version") + " - " + rs.getString("description") + " (Success: "
                            + rs.getBoolean("success") + ")");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
