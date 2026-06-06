-- PostgreSQL DDL generado a partir del diagrama Mermaid

-- =========================
-- ENUMS
-- =========================

CREATE TYPE tcorrel AS ENUM (
    'Aprobado',
    'Regular'
);

CREATE TYPE talumn AS ENUM (
    'Ingresante',
    'Avanzado'
);

CREATE TYPE tcargo AS ENUM (
    'Jefe_Practico',
    'Ayudante'
);

CREATE TYPE tper AS ENUM (
    'Bimestre',
    'Trimestre',
    'Cuatrimestre',
    'Anual'
);

-- =========================
-- TABLAS BASE
-- =========================


CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS persona (
    dni            VARCHAR(20) PRIMARY KEY,
    apellido       VARCHAR(100) NOT NULL,
    nombre         VARCHAR(100) NOT NULL,
    nro_contacto   VARCHAR(50),
    mail           VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS docente (
    dni            VARCHAR(20) PRIMARY KEY,
    nro_legajo     VARCHAR(50) UNIQUE NOT NULL,
    titulo         VARCHAR(255),
    CONSTRAINT fk_docente_persona
        FOREIGN KEY (dni)
        REFERENCES persona(dni)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS alumno (
    dni            VARCHAR(20) PRIMARY KEY,
    tipo_alumno    talumn NOT NULL,
    CONSTRAINT fk_alumno_persona
        FOREIGN KEY (dni)
        REFERENCES persona(dni)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS carrera (
    cod_carrera        VARCHAR(20) PRIMARY KEY,
    nombre_carrera     VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS plan_estudio (
    cod_plan           VARCHAR(20) PRIMARY KEY,
    anio               INTEGER NOT NULL,

    carrera_tuvo       VARCHAR(20) NOT NULL,
    carrera_vigente    VARCHAR(20) NOT NULL,

    CONSTRAINT fk_plan_carrera_tuvo
        FOREIGN KEY (carrera_tuvo)
        REFERENCES carrera(cod_carrera),

    CONSTRAINT fk_plan_carrera_vigente
        FOREIGN KEY (carrera_vigente)
        REFERENCES carrera(cod_carrera)
);

CREATE TABLE IF NOT EXISTS materia (
    cod_materia        VARCHAR(20) PRIMARY KEY,
    nombre_materia     VARCHAR(100) NOT NULL,
    anio_materia       INTEGER NOT NULL,
    cod_inscripcion    VARCHAR (20),
);

CREATE TABLE IF NOT EXISTS examen_final (
    id_examen          BIGSERIAL PRIMARY KEY,
    fecha              DATE NOT NULL,
    nota               NUMERIC(4,2)
);

CREATE TABLE IF NOT EXISTS periodo (
    id_periodo         BIGSERIAL PRIMARY KEY,
    fecha_inicio       DATE NOT NULL,
    fecha_fin          DATE NOT NULL,
    cargo              tcargo NOT NULL,
    type_periodo       tper NOT NULL
);

CREATE TABLE IF NOT EXISTS correlatividad (
    id_correlatividad  BIGSERIAL PRIMARY KEY,
    correl             tcorrel NOT NULL
);

-- =========================
-- RELACIONES DOCENTE - MATERIA
-- =========================

CREATE TABLE IF NOT EXISTS docente_responsable_materia (
    docente_dni        VARCHAR(20) NOT NULL,
    cod_materia        VARCHAR(20) NOT NULL,

    PRIMARY KEY (docente_dni, cod_materia),

    FOREIGN KEY (docente_dni)
        REFERENCES docente(dni)
        ON DELETE CASCADE,

    FOREIGN KEY (cod_materia)
        REFERENCES materia(cod_materia)
        ON DELETE CASCADE
);

-- =========================
-- RELACIONES DOCENTE - PERIODO
-- =========================

CREATE TABLE IF NOT EXISTS docente_periodo (
    docente_dni        VARCHAR(20) NOT NULL,
    id_periodo         BIGINT NOT NULL,

    PRIMARY KEY (docente_dni, id_periodo),

    FOREIGN KEY (docente_dni)
        REFERENCES docente(dni)
        ON DELETE CASCADE,

    FOREIGN KEY (id_periodo)
        REFERENCES periodo(id_periodo)
        ON DELETE CASCADE
);

-- =========================
-- RELACIONES PERIODO - MATERIA
-- =========================

CREATE TABLE IF NOT EXISTS periodo_materia (
    id_periodo         BIGINT NOT NULL,
    cod_materia        VARCHAR(20) NOT NULL,

    PRIMARY KEY (id_periodo, cod_materia),

    FOREIGN KEY (id_periodo)
        REFERENCES periodo(id_periodo)
        ON DELETE CASCADE,

    FOREIGN KEY (cod_materia)
        REFERENCES materia(cod_materia)
        ON DELETE CASCADE
);

-- =========================
-- RELACIONES ALUMNO - MATERIA
-- =========================

CREATE TABLE IF NOT EXISTS alumno_materia (
    alumno_dni         VARCHAR(20) NOT NULL,
    cod_materia        VARCHAR(20) NOT NULL,

    PRIMARY KEY (alumno_dni, cod_materia),

    FOREIGN KEY (alumno_dni)
        REFERENCES alumno(dni)
        ON DELETE CASCADE,

    FOREIGN KEY (cod_materia)
        REFERENCES materia(cod_materia)
        ON DELETE CASCADE
);

-- =========================
-- RELACIONES ALUMNO - EXAMEN FINAL
-- =========================

CREATE TABLE IF NOT EXISTS alumno_examen_final (
    alumno_dni         VARCHAR(20) NOT NULL,
    id_examen          BIGINT NOT NULL,

    PRIMARY KEY (alumno_dni, id_examen),

    FOREIGN KEY (alumno_dni)
        REFERENCES alumno(dni)
        ON DELETE CASCADE,

    FOREIGN KEY (id_examen)
        REFERENCES examen_final(id_examen)
        ON DELETE CASCADE
);

-- =========================
-- RELACIONES EXAMEN FINAL - MATERIA
-- =========================

CREATE TABLE IF NOT EXISTS examen_final_materia (
    id_examen          BIGINT NOT NULL,
    cod_materia        VARCHAR(20) NOT NULL,

    PRIMARY KEY (id_examen, cod_materia),

    FOREIGN KEY (id_examen)
        REFERENCES examen_final(id_examen)
        ON DELETE CASCADE,

    FOREIGN KEY (cod_materia)
        REFERENCES materia(cod_materia)
        ON DELETE CASCADE
);

-- =========================
-- RELACIONES ALUMNO - PLAN ESTUDIO
-- =========================

CREATE TABLE IF NOT EXISTS alumno_plan_estudio (
    alumno_dni         VARCHAR(20) NOT NULL,
    cod_plan           VARCHAR(20) NOT NULL,

    PRIMARY KEY (alumno_dni, cod_plan),

    FOREIGN KEY (alumno_dni)
        REFERENCES alumno(dni)
        ON DELETE CASCADE,

    FOREIGN KEY (cod_plan)
        REFERENCES plan_estudio(cod_plan)
        ON DELETE CASCADE
);

-- =========================
-- RELACIONES MATERIA - PLAN ESTUDIO
-- =========================

CREATE TABLE IF NOT EXISTS materia_plan_estudio (
    cod_materia        VARCHAR(20) NOT NULL,
    cod_plan           VARCHAR(20) NOT NULL,

    PRIMARY KEY (cod_materia, cod_plan),

    FOREIGN KEY (cod_materia)
        REFERENCES materia(cod_materia)
        ON DELETE CASCADE,

    FOREIGN KEY (cod_plan)
        REFERENCES plan_estudio(cod_plan)
        ON DELETE CASCADE
);

-- =========================
-- AUTORRELACIÓN MATERIA - MATERIA
-- =========================
CREATE TABLE IF NOT EXISTS materia_relacion (
    materia_origen     VARCHAR(20) NOT NULL,
    materia_destino    VARCHAR(20) NOT NULL,

    PRIMARY KEY (materia_origen, materia_destino),

    FOREIGN KEY (materia_origen)
        REFERENCES materia(cod_materia)
        ON DELETE CASCADE,

    FOREIGN KEY (materia_destino)
        REFERENCES materia(cod_materia)
        ON DELETE CASCADE,

    CHECK (materia_origen <> materia_destino)
);

-- =========================
-- CORRELATIVIDAD
-- =========================
CREATE TABLE IF NOT EXISTS materia_correlatividad (
    id_correlatividad  BIGINT NOT NULL,
    materia_origen     VARCHAR(20) NOT NULL,
    materia_requerida  VARCHAR(20) NOT NULL,

    PRIMARY KEY (
        id_correlatividad,
        materia_origen,
        materia_requerida
    ),

    FOREIGN KEY (id_correlatividad)
        REFERENCES correlatividad(id_correlatividad)
        ON DELETE CASCADE,

    FOREIGN KEY (materia_origen)
        REFERENCES materia(cod_materia)
        ON DELETE CASCADE,

    FOREIGN KEY (materia_requerida)
        REFERENCES materia(cod_materia)
        ON DELETE CASCADE,

    CHECK (materia_origen <> materia_requerida)
);