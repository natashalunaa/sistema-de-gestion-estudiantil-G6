package com.is1.proyecto;

import com.is1.proyecto.logic.CatedraLogic;
import com.is1.proyecto.models.DocenteResponsableMateria;
import com.is1.proyecto.models.Docente;
import com.is1.proyecto.models.Materia;
import com.is1.proyecto.models.Persona;
import org.javalite.activejdbc.Base;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.Session;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CatedraLogicTest {

    @BeforeEach
    void setUp() {
        Base.open("org.sqlite.JDBC", "jdbc:sqlite::memory:", "", "");
        
        // Create required tables
        Base.exec("CREATE TABLE persona ("
                + "dni VARCHAR(20) PRIMARY KEY, "
                + "apellido VARCHAR(100) NOT NULL, "
                + "nombre VARCHAR(100) NOT NULL, "
                + "nro_contacto VARCHAR(50), "
                + "mail VARCHAR(255)"
                + ");");

        Base.exec("CREATE TABLE docente ("
                + "dni VARCHAR(20) PRIMARY KEY, "
                + "nro_legajo VARCHAR(50) UNIQUE NOT NULL, "
                + "titulo VARCHAR(255), "
                + "FOREIGN KEY(dni) REFERENCES persona(dni) ON DELETE CASCADE"
                + ");");

        Base.exec("CREATE TABLE materia ("
                + "cod_materia VARCHAR(20) PRIMARY KEY, "
                + "nombre_materia VARCHAR(100) NOT NULL, "
                + "anio_materia INTEGER NOT NULL, "
                + "cod_inscripcion VARCHAR(20)"
                + ");");

        Base.exec("CREATE TABLE docente_responsable_materia ("
                + "id BIGINT PRIMARY KEY, "
                + "docente_dni VARCHAR(20) NOT NULL, "
                + "cod_materia VARCHAR(20) NOT NULL, "
                + "UNIQUE(docente_dni, cod_materia), "
                + "FOREIGN KEY(docente_dni) REFERENCES docente(dni) ON DELETE CASCADE, "
                + "FOREIGN KEY(cod_materia) REFERENCES materia(cod_materia) ON DELETE CASCADE"
                + ");");
    }

    @AfterEach
    void tearDown() {
        if (Base.hasConnection()) {
            Base.close();
        }
    }

    @Test
    void listarCatedras_withAuthenticatedAdmin_returnsModelWithCatedras() {
        // Setup personas
        Persona p1 = new Persona();
        p1.set("dni", "12345678");
        p1.set("apellido", "Perez");
        p1.set("nombre", "Juan");
        p1.insert();

        // Setup docente
        Docente docente = new Docente();
        docente.set("dni", "12345678");
        docente.set("nro_legajo", "DOC001");
        docente.set("titulo", "Ingeniero");
        docente.insert();

        // Setup materia
        Materia materia = new Materia();
        materia.setCodMateria("MAT001");
        materia.set("nombre_materia", "Programación");
        materia.set("anio_materia", 1);
        materia.insert();

        // Setup asignación
        DocenteResponsableMateria asignacion = new DocenteResponsableMateria();
        asignacion.set("id", 1L);
        asignacion.setDocenteDni("12345678");
        asignacion.setCodMateria("MAT001");
        asignacion.insert();

        // Mock request and response
        Request req = mock(Request.class);
        Response res = mock(Response.class);
        Session session = mock(Session.class);

        when(req.session()).thenReturn(session);
        when(session.attribute("currentUserUsername")).thenReturn("admin");
        when(session.attribute("loggedIn")).thenReturn(true);
        when(session.attribute("role")).thenReturn("admin");

        ModelAndView view = CatedraLogic.listarCatedras(req, res);

        assertNotNull(view);
        Map<String, Object> model = view.getModel();
        
        assertTrue(model.containsKey("catedras"));
        assertTrue(model.containsKey("docentes"));
        assertTrue(model.containsKey("materias"));

        List<?> catedras = (List<?>) model.get("catedras");
        assertEquals(1, catedras.size());
    }

    @Test
    void asignarDocente_createsNewAssignmentAndRedirects() {
        // Setup personas
        Persona p1 = new Persona();
        p1.set("dni", "87654321");
        p1.set("apellido", "Lopez");
        p1.set("nombre", "Maria");
        p1.insert();

        // Setup docente
        Docente docente = new Docente();
        docente.set("dni", "87654321");
        docente.set("nro_legajo", "DOC002");
        docente.set("titulo", "Ingeniera");
        docente.insert();

        // Setup materia
        Materia materia = new Materia();
        materia.setCodMateria("MAT002");
        materia.set("nombre_materia", "Bases de Datos");
        materia.set("anio_materia", 2);
        materia.insert();

        // Mock request and response
        Request req = mock(Request.class);
        Response res = mock(Response.class);

        when(req.queryParams("docente_dni")).thenReturn("87654321");
        when(req.queryParams("cod_materia")).thenReturn("MAT002");

        CatedraLogic.asignarDocente(req, res);

        DocenteResponsableMateria saved = DocenteResponsableMateria.findFirst(
                "docente_dni = ? AND cod_materia = ?", "87654321", "MAT002");

        assertNotNull(saved);
        assertEquals("87654321", saved.getDocenteDni());
        assertEquals("MAT002", saved.getCodMateria());

        verify(res).redirect("/catedras");
    }

    @Test
    void asignarDocente_withExistingAssignment_redirectsWithError() {
        // Setup personas
        Persona p1 = new Persona();
        p1.set("dni", "11111111");
        p1.set("apellido", "Garcia");
        p1.set("nombre", "Pedro");
        p1.insert();

        // Setup docente
        Docente docente = new Docente();
        docente.set("dni", "11111111");
        docente.set("nro_legajo", "DOC003");
        docente.set("titulo", "Doctor");
        docente.insert();

        // Setup materia
        Materia materia = new Materia();
        materia.setCodMateria("MAT003");
        materia.set("nombre_materia", "Algoritmos");
        materia.set("anio_materia", 3);
        materia.insert();

        // Create existing assignment
        DocenteResponsableMateria existente = new DocenteResponsableMateria();
        existente.set("id", 2L);
        existente.setDocenteDni("11111111");
        existente.setCodMateria("MAT003");
        existente.insert();

        // Mock request and response
        Request req = mock(Request.class);
        Response res = mock(Response.class);

        when(req.queryParams("docente_dni")).thenReturn("11111111");
        when(req.queryParams("cod_materia")).thenReturn("MAT003");

        CatedraLogic.asignarDocente(req, res);

        verify(res).redirect("/catedras?error=Asignacion existente");
    }

    @Test
    void desasignarDocente_removesAssignmentAndRedirects() {
        // Setup personas
        Persona p1 = new Persona();
        p1.set("dni", "22222222");
        p1.set("apellido", "Rodriguez");
        p1.set("nombre", "Carlos");
        p1.insert();

        // Setup docente
        Docente docente = new Docente();
        docente.set("dni", "22222222");
        docente.set("nro_legajo", "DOC004");
        docente.set("titulo", "Profesor");
        docente.insert();

        // Setup materia
        Materia materia = new Materia();
        materia.setCodMateria("MAT004");
        materia.set("nombre_materia", "Estructuras");
        materia.set("anio_materia", 1);
        materia.insert();

        // Create assignment
        DocenteResponsableMateria asignacion = new DocenteResponsableMateria();
        asignacion.set("id", 3L);
        asignacion.setDocenteDni("22222222");
        asignacion.setCodMateria("MAT004");
        asignacion.insert();

        // Mock request and response
        Request req = mock(Request.class);
        Response res = mock(Response.class);

        when(req.queryParams("docente_dni")).thenReturn("22222222");
        when(req.queryParams("cod_materia")).thenReturn("MAT004");

        CatedraLogic.desasignarDocente(req, res);

        DocenteResponsableMateria deleted = DocenteResponsableMateria.findFirst(
                "docente_dni = ? AND cod_materia = ?", "22222222", "MAT004");

        assertNull(deleted);
        verify(res).redirect("/catedras");
    }
}
