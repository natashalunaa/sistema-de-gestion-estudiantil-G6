package com.is1.proyecto;

import com.is1.proyecto.logic.PlanEstudioLogic;
import com.is1.proyecto.models.AlumnoPlanEstudio;
import com.is1.proyecto.models.Carrera;
import com.is1.proyecto.models.Materia;
import com.is1.proyecto.models.MateriaPlanEstudio;
import com.is1.proyecto.models.PlanEstudio;
import org.javalite.activejdbc.Base;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PlanEstudioLogicTest {

    @BeforeEach
    void setUp() {
        Base.open("org.sqlite.JDBC", "jdbc:sqlite::memory:", "", "");
        
        // Create required tables
        Base.exec("CREATE TABLE carrera ("
                + "cod_carrera VARCHAR(20) PRIMARY KEY, "
                + "nombre_carrera VARCHAR(255) NOT NULL, "
                + "duracion INT NOT NULL"
                + ");");

        Base.exec("CREATE TABLE plan_estudio ("
                + "cod_plan VARCHAR(20) PRIMARY KEY, "
                + "anio INTEGER NOT NULL, "
                + "carrera_tuvo VARCHAR(20) NOT NULL, "
                + "carrera_vigente VARCHAR(20) NOT NULL, "
                + "FOREIGN KEY(carrera_tuvo) REFERENCES carrera(cod_carrera), "
                + "FOREIGN KEY(carrera_vigente) REFERENCES carrera(cod_carrera)"
                + ");");

        Base.exec("CREATE TABLE materia ("
                + "cod_materia VARCHAR(20) PRIMARY KEY, "
                + "nombre_materia VARCHAR(100) NOT NULL, "
                + "anio_materia INTEGER NOT NULL, "
                + "cod_inscripcion VARCHAR(20)"
                + ");");

        Base.exec("CREATE TABLE materia_plan_estudio ("
                + "cod_materia VARCHAR(20) NOT NULL, "
                + "cod_plan VARCHAR(20) NOT NULL, "
                + "PRIMARY KEY(cod_materia, cod_plan), "
                + "FOREIGN KEY(cod_materia) REFERENCES materia(cod_materia) ON DELETE CASCADE, "
                + "FOREIGN KEY(cod_plan) REFERENCES plan_estudio(cod_plan) ON DELETE CASCADE"
                + ");");

        Base.exec("CREATE TABLE persona ("
                + "dni VARCHAR(20) PRIMARY KEY, "
                + "apellido VARCHAR(100) NOT NULL, "
                + "nombre VARCHAR(100) NOT NULL, "
                + "nro_contacto VARCHAR(50), "
                + "mail VARCHAR(255)"
                + ");");

        Base.exec("CREATE TABLE alumno ("
                + "dni VARCHAR(20) PRIMARY KEY, "
                + "tipo_alumno VARCHAR(20) NOT NULL, "
                + "FOREIGN KEY(dni) REFERENCES persona(dni) ON DELETE CASCADE"
                + ");");

        Base.exec("CREATE TABLE alumno_plan_estudio ("
                + "alumno_dni VARCHAR(20) NOT NULL, "
                + "cod_plan VARCHAR(20) NOT NULL, "
                + "PRIMARY KEY(alumno_dni, cod_plan), "
                + "FOREIGN KEY(alumno_dni) REFERENCES alumno(dni) ON DELETE CASCADE, "
                + "FOREIGN KEY(cod_plan) REFERENCES plan_estudio(cod_plan) ON DELETE CASCADE"
                + ");");
    }

    @AfterEach
    void tearDown() {
        if (Base.hasConnection()) {
            Base.close();
        }
    }

    @Test
    void createPlanEstudio_createsNewPlanAndPersists() {
        // Setup carrera
        Carrera carrera = new Carrera();
        carrera.setCodCarrera("C001");
        carrera.setNombreCarrera("Ingeniería");
        carrera.setDuracion(5);
        carrera.insert();

        // Create plan estudio
        PlanEstudio plan = new PlanEstudio();
        plan.setCodPlan("PLAN001");
        plan.setAnio(2024);
        plan.setCarreraTuvo("C001");
        plan.setCarreraVigente("C001");
        plan.insert();

        // Verify persistence
        PlanEstudio loaded = PlanEstudio.findById("PLAN001");
        assertNotNull(loaded);
        assertEquals("PLAN001", loaded.getCodPlan());
        assertEquals(2024, loaded.getAnio().intValue());
        assertEquals("C001", loaded.getCarreraTuvo());
        assertEquals("C001", loaded.getCarreraVigente());
    }

    @Test
    void listPlanesEstudio_returnAllPlans() {
        // Setup carreras
        Carrera c1 = new Carrera();
        c1.setCodCarrera("C002");
        c1.setNombreCarrera("Arquitectura");
        c1.setDuracion(6);
        c1.insert();

        Carrera c2 = new Carrera();
        c2.setCodCarrera("C003");
        c2.setNombreCarrera("Medicina");
        c2.setDuracion(7);
        c2.insert();

        // Create planes
        PlanEstudio plan1 = new PlanEstudio();
        plan1.setCodPlan("PLAN002");
        plan1.setAnio(2023);
        plan1.setCarreraTuvo("C002");
        plan1.setCarreraVigente("C002");
        plan1.insert();

        PlanEstudio plan2 = new PlanEstudio();
        plan2.setCodPlan("PLAN003");
        plan2.setAnio(2024);
        plan2.setCarreraTuvo("C003");
        plan2.setCarreraVigente("C003");
        plan2.insert();

        // List all plans
        List<PlanEstudio> plans = PlanEstudio.findAll();
        assertEquals(2, plans.size());
    }

    @Test
    void asignarMateriaToPlan_createsAssociationAndPersists() {
        // Setup carrera
        Carrera carrera = new Carrera();
        carrera.setCodCarrera("C004");
        carrera.setNombreCarrera("Sistemas");
        carrera.setDuracion(4);
        carrera.insert();

        // Setup plan
        PlanEstudio plan = new PlanEstudio();
        plan.setCodPlan("PLAN004");
        plan.setAnio(2024);
        plan.setCarreraTuvo("C004");
        plan.setCarreraVigente("C004");
        plan.insert();

        // Setup materias
        Materia m1 = new Materia();
        m1.setCodMateria("MAT001");
        m1.set("nombre_materia", "Programación");
        m1.set("anio_materia", 1);
        m1.insert();

        Materia m2 = new Materia();
        m2.setCodMateria("MAT002");
        m2.set("nombre_materia", "Bases de Datos");
        m2.set("anio_materia", 2);
        m2.insert();

        // Assign materias to plan
        MateriaPlanEstudio mpe1 = new MateriaPlanEstudio();
        mpe1.setCodMateria("MAT001");
        mpe1.set("cod_plan", "PLAN004");
        mpe1.insert();

        MateriaPlanEstudio mpe2 = new MateriaPlanEstudio();
        mpe2.setCodMateria("MAT002");
        mpe2.set("cod_plan", "PLAN004");
        mpe2.insert();

        // Verify associations
        List<MateriaPlanEstudio> materiasDelPlan = MateriaPlanEstudio.find("cod_plan = ?", "PLAN004");
        assertEquals(2, materiasDelPlan.size());
    }

    @Test
    void consultarMateriasDePlan_returnsMateriasByPlan() {
        // Setup carrera
        Carrera carrera = new Carrera();
        carrera.setCodCarrera("C005");
        carrera.setNombreCarrera("Telecomunicaciones");
        carrera.setDuracion(5);
        carrera.insert();

        // Setup plan
        PlanEstudio plan = new PlanEstudio();
        plan.setCodPlan("PLAN005");
        plan.setAnio(2024);
        plan.setCarreraTuvo("C005");
        plan.setCarreraVigente("C005");
        plan.insert();

        // Setup materias
        Materia m1 = new Materia();
        m1.setCodMateria("MAT101");
        m1.set("nombre_materia", "Circuitos");
        m1.set("anio_materia", 1);
        m1.insert();

        // Assign materia to plan
        MateriaPlanEstudio mpe = new MateriaPlanEstudio();
        mpe.setCodMateria("MAT101");
        mpe.set("cod_plan", "PLAN005");
        mpe.insert();

        // Query materias by plan
        List<MateriaPlanEstudio> materiasPlan = MateriaPlanEstudio.find("cod_plan = ?", "PLAN005");
        assertEquals(1, materiasPlan.size());
        assertEquals("MAT101", materiasPlan.get(0).getCodMateria());
    }

    @Test
    void asignarAlumnoPlan_createsAssociationAndPersists() {
        // Setup carrera
        Carrera carrera = new Carrera();
        carrera.setCodCarrera("C006");
        carrera.setNombreCarrera("Electrónica");
        carrera.setDuracion(5);
        carrera.insert();

        // Setup plan
        PlanEstudio plan = new PlanEstudio();
        plan.setCodPlan("PLAN006");
        plan.setAnio(2024);
        plan.setCarreraTuvo("C006");
        plan.setCarreraVigente("C006");
        plan.insert();

        // Setup persona and alumno
        Base.exec("INSERT INTO persona (dni, apellido, nombre) VALUES ('12345678', 'Rodriguez', 'Carlos');");
        Base.exec("INSERT INTO alumno (dni, tipo_alumno) VALUES ('12345678', 'Ingresante');");

        // Assign alumno to plan
        AlumnoPlanEstudio app = new AlumnoPlanEstudio();
        app.set("alumno_dni", "12345678");
        app.set("cod_plan", "PLAN006");
        app.insert();

        // Verify association
        AlumnoPlanEstudio found = AlumnoPlanEstudio.findFirst("alumno_dni = ? AND cod_plan = ?", "12345678", "PLAN006");
        assertNotNull(found);
    }

    @Test
    void deletePlanEstudio_removesPlansAndCascades() {
        // Setup carrera
        Carrera carrera = new Carrera();
        carrera.setCodCarrera("C007");
        carrera.setNombreCarrera("Informática");
        carrera.setDuracion(4);
        carrera.insert();

        // Setup plan
        PlanEstudio plan = new PlanEstudio();
        plan.setCodPlan("PLAN007");
        plan.setAnio(2024);
        plan.setCarreraTuvo("C007");
        plan.setCarreraVigente("C007");
        plan.insert();

        // Setup materia and association
        Materia m = new Materia();
        m.setCodMateria("MAT201");
        m.set("nombre_materia", "Redes");
        m.set("anio_materia", 3);
        m.insert();

        MateriaPlanEstudio mpe = new MateriaPlanEstudio();
        mpe.setCodMateria("MAT201");
        mpe.set("cod_plan", "PLAN007");
        mpe.insert();

        // Delete plan
        plan.delete();

        // Verify cascade delete
        PlanEstudio deleted = PlanEstudio.findById("PLAN007");
        assertNull(deleted);

        List<MateriaPlanEstudio> orphaned = MateriaPlanEstudio.find("cod_plan = ?", "PLAN007");
        assertEquals(0, orphaned.size());
    }
}
