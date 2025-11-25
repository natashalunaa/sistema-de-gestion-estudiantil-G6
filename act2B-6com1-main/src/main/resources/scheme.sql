-- Elimina la tabla 'users' si ya existe para asegurar un inicio limpio
DROP TABLE IF EXISTS users;

-- Crea la tabla 'users' con los campos originales, adaptados para SQLite
CREATE TABLE users (
    id INTEGER PRIMARY KEY AUTOINCREMENT, -- Clave primaria autoincremental para SQLite
    name TEXT NOT NULL UNIQUE,          -- Nombre de usuario (TEXT es el tipo de cadena recomendado para SQLite), con restricción UNIQUE
    password TEXT NOT NULL           -- Contraseña hasheada (TEXT es el tipo de cadena recomendado para SQLite)
);

DROP TABLE IF EXISTS personas;

CREATE TABLE personas (
    id INTEGER PRIMARY KEY,
    first_name TEXT NOT NULL,
    last_name TEXT NOT NULL,
    CONSTRAINT check_members_id CHECK(id < 1000000000 AND id >= 0)
);

DROP TABLE IF EXISTS teachers;

CREATE TABLE teachers (
    id INTEGER PRIMARY KEY,
    degree TEXT NOT NULL,            -- Titulo universitario del profesor 
    email TEXT NOT NULL UNIQUE,      -- Correo electrónico del profesor 

    FOREIGN KEY (id) REFERENCES personas(id) ON DELETE CASCADE,
    CONSTRAINT check_teachers_email CHECK(email LIKE '%@%.%')  --chequea mail válido
);