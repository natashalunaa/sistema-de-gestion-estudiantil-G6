package com.is1.proyecto;

import com.is1.proyecto.logic.InscripcionLogic;
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

class InscripcionLogicTest {

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

        Base.exec("CREATE TABLE materia ("
                + "cod_materia VARCHAR(20) PRIMARY KEY, "
                + "nombre_materia VARCHAR(100) NOT NULL, "
                + "anio_materia INTEGER NOT NULL, "
                + "cod_inscripcion VARCHAR(20)"
                + ");");
    }

    @AfterEach
    void tearDown() {
        if (Base.hasConnection()) {
            Base.close();
        }
    }

    @Test
    void listarInscripciones_withAuthenticatedStudent_returnsModelWithMateriasAndDni() {
        // Setup persona
        Persona persona = new Persona();
        persona.setDni("12345678");
        persona.setApellido("Perez");
        persona.setNombre("Juan");
        persona.setMail("juan@example.com");
        persona.insert();

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

        // Mock request and response
        Request req = mock(Request.class);
        Response res = mock(Response.class);
        Session session = mock(Session.class);

        when(req.session()).thenReturn(session);
        when(session.attribute("currentUserUsername")).thenReturn("juan@example.com");
        when(session.attribute("loggedIn")).thenReturn(true);

        ModelAndView view = InscripcionLogic.listarInscripciones(req, res);

        assertNotNull(view);
        Map<String, Object> model = view.getModel();
        
        assertTrue(model.containsKey("dniAlumno"));
        assertEquals("12345678", model.get("dniAlumno"));

        assertTrue(model.containsKey("materias"));
        List<?> materias = (List<?>) model.get("materias");
        assertEquals(2, materias.size());

        assertTrue(model.containsKey("inscripciones"));
        List<?> inscripciones = (List<?>) model.get("inscripciones");
        assertEquals(0, inscripciones.size());

        assertEquals("inscripciones.mustache", view.getViewName());
    }

    @Test
    void listarInscripciones_withUnauthenticatedUser_redirectsWithError() {
        // Mock request and response
        Request req = mock(Request.class);
        Response res = mock(Response.class);
        Session session = mock(Session.class);

        when(req.session()).thenReturn(session);
        when(session.attribute("currentUserUsername")).thenReturn(null);
        when(session.attribute("loggedIn")).thenReturn(false);

        ModelAndView view = InscripcionLogic.listarInscripciones(req, res);

        assertNull(view);
        verify(res).redirect("/?error=No autorizado");
    }

    @Test
    void listarInscripciones_whenPersonaNotFound_redirectsWithError() {
        // Mock request and response
        Request req = mock(Request.class);
        Response res = mock(Response.class);
        Session session = mock(Session.class);

        when(req.session()).thenReturn(session);
        when(session.attribute("currentUserUsername")).thenReturn("nonexistent@example.com");
        when(session.attribute("loggedIn")).thenReturn(true);

        ModelAndView view = InscripcionLogic.listarInscripciones(req, res);

        assertNull(view);
        verify(res).redirect("/?error=Alumno no encontrado");
    }

    @Test
    void listarInscripciones_searchByDni_returnsModelWithCorrectData() {
        // Setup persona
        Persona persona = new Persona();
        persona.setDni("87654321");
        persona.setApellido("Lopez");
        persona.setNombre("Maria");
        persona.setMail("maria@example.com");
        persona.insert();

        // Setup materia
        Materia m1 = new Materia();
        m1.setCodMateria("MAT003");
        m1.set("nombre_materia", "Cálculo");
        m1.set("anio_materia", 1);
        m1.insert();

        // Mock request and response - search by DNI
        Request req = mock(Request.class);
        Response res = mock(Response.class);
        Session session = mock(Session.class);

        when(req.session()).thenReturn(session);
        when(session.attribute("currentUserUsername")).thenReturn("87654321");
        when(session.attribute("loggedIn")).thenReturn(true);

        ModelAndView view = InscripcionLogic.listarInscripciones(req, res);

        assertNotNull(view);
        Map<String, Object> model = view.getModel();
        
        assertEquals("87654321", model.get("dniAlumno"));

        List<?> materias = (List<?>) model.get("materias");
        assertEquals(1, materias.size());
    }
}
