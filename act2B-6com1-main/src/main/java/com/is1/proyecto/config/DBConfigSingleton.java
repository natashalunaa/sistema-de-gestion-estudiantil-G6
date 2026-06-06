// Archivo: com/is1/proyecto/config/DBConfigSingleton.java
package com.is1.proyecto.config;

import org.javalite.activejdbc.Base; // Necesitarás esta importación para usar Base.open y Base.close

public final class DBConfigSingleton {

    private static DBConfigSingleton instance;

    private final String dbUrl;
    private final String user;
    private final String pass;
    private final String driver;

    // Constructor privado para evitar instanciación directa
    private DBConfigSingleton() {
        String pgHost = System.getenv("POSTGRES_HOST");
        String pgPort = System.getenv("POSTGRES_PORT");
        String pgUser = System.getenv("POSTGRES_USER");
        String pgPass = System.getenv("POSTGRES_PASSWORD");
        String pgDb = System.getenv("POSTGRES_DB");

        if (pgHost != null && !pgHost.isBlank() && pgDb != null && !pgDb.isBlank()) {
            this.driver = "org.postgresql.Driver";
            String port = (pgPort == null || pgPort.isBlank()) ? "5432" : pgPort;
            this.dbUrl = String.format("jdbc:postgresql://%s:%s/%s", pgHost, port, pgDb);
            this.user = (pgUser == null || pgUser.isBlank()) ? "postgres" : pgUser;
            this.pass = (pgPass == null) ? "" : pgPass;
        } else {
            throw new IllegalStateException("Las variables de entorno POSTGRES_HOST y POSTGRES_DB son obligatorias para configurar la conexión a la base de datos.");
        }
    }

    public static synchronized DBConfigSingleton getInstance() {
        if (instance == null) {
            instance = new DBConfigSingleton();
        }
        return instance;
    }

    // Métodos para abrir y cerrar la conexión
    public void openConnection() {
        Base.open(this.driver, this.dbUrl, this.user, this.pass);
    }

    public void closeConnection() {
        Base.close();
    }

    // Getters existentes
    public String getDbUrl() {
        return dbUrl;
    }

    public String getUser() {
        return user;
    }

    public String getPass() {
        return pass;
    }

    public String getDriver() {
        return driver;
    }
}

