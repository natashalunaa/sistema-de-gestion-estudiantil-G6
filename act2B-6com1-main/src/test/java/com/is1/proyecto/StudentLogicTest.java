package com.is1.proyecto;

import com.is1.proyecto.logic.StudentLogic;
import com.is1.proyecto.models.Alumno;
import com.is1.proyecto.models.Persona;
import com.is1.proyecto.models.Users;
import org.javalite.activejdbc.Base;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;
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

class StudentLogicTest {

    @BeforeEach
    void setUp() {
        Base.open("org.sqlite.JDBC", "jdbc:sqlite::memory:", "", "");
        
        // Create required tables
        Base.exec("CREATE TABLE users ("
                + "id BIGINT PRIMARY KEY, "
                + "name VARCHAR(255) UNIQUE NOT NULL, "
                + "password VARCHAR(255) NOT NULL"
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
    }

    @AfterEach
    void tearDown() {
        if (Base.hasConnection()) {
            Base.close();
        }
    }

    @Test
    void dashboard_withAuthenticatedStudent_returnsModelAndView() {
        Request req = mock(Request.class);
        Response res = mock(Response.class);
        Session session = mock(Session.class);

        when(req.session()).thenReturn(session);
        when(session.attribute("currentUserUsername")).thenReturn("student@example.com");
        when(session.attribute("loggedIn")).thenReturn(true);

        ModelAndView view = StudentLogic.dashboard(req, res);

        assertNotNull(view);
        Map<String, Object> model = (Map<String, Object>) view.getModel();
        assertEquals("student@example.com", model.get("username"));
        assertEquals("student_dashboard.mustache", view.getViewName());
    }

    @Test
    void dashboard_withUnauthenticatedUser_redirectsWithError() {
        Request req = mock(Request.class);
        Response res = mock(Response.class);
        Session session = mock(Session.class);

        when(req.session()).thenReturn(session);
        when(session.attribute("currentUserUsername")).thenReturn(null);
        when(session.attribute("loggedIn")).thenReturn(false);

        ModelAndView view = StudentLogic.dashboard(req, res);

        assertNull(view);
        verify(res).redirect("/?error=Acceso denegado.");
    }

    @Test
    void createStudent_createsNewStudentWithValidData() {
        Request req = mock(Request.class);
        Response res = mock(Response.class);

        when(req.queryParams("nombre")).thenReturn("Juan");
        when(req.queryParams("apellido")).thenReturn("Perez");
        when(req.queryParams("dni")).thenReturn("12345678");
        when(req.queryParams("mail")).thenReturn("juan@example.com");
        when(req.queryParams("password")).thenReturn("password123");
        when(req.queryParams("tipo_alumno")).thenReturn("Ingresante");

        StudentLogic.createStudent(req, res);

        Persona persona = Persona.findById("12345678");
        assertNotNull(persona);
        assertEquals("Juan", persona.getNombre());
        assertEquals("Perez", persona.getApellido());
        assertEquals("juan@example.com", persona.getMail());

        Alumno alumno = Alumno.findById("12345678");
        assertNotNull(alumno);
        assertEquals("Ingresante", alumno.getTipoAlumno());

        Users user = Users.findFirst("name = ?", "juan@example.com");
        assertNotNull(user);

        verify(res).redirect("/students");
    }

    @Test
    void createStudent_withMissingFields_returnsErrorMessage() {
        Request req = mock(Request.class);
        Response res = mock(Response.class);

        when(req.queryParams("nombre")).thenReturn("Juan");
        when(req.queryParams("apellido")).thenReturn("");
        when(req.queryParams("dni")).thenReturn("12345678");
        when(req.queryParams("mail")).thenReturn("juan@example.com");
        when(req.queryParams("password")).thenReturn("password123");
        when(req.queryParams("tipo_alumno")).thenReturn("Ingresante");

        ModelAndView view = StudentLogic.createStudent(req, res);

        assertNotNull(view);
        Map<String, Object> model = view.getModel();
        assertTrue(model.containsKey("errorMessage"));
        assertEquals("Todos los campos son obligatorios.", model.get("errorMessage"));
    }

    @Test
    void createStudent_withDuplicateDni_returnsErrorMessage() {
        Persona existing = new Persona();
        existing.setDni("87654321");
        existing.setNombre("Maria");
        existing.setApellido("Lopez");
        existing.setMail("maria@example.com");
        existing.insert();

        Request req = mock(Request.class);
        Response res = mock(Response.class);

        when(req.queryParams("nombre")).thenReturn("Pedro");
        when(req.queryParams("apellido")).thenReturn("Garcia");
        when(req.queryParams("dni")).thenReturn("87654321");
        when(req.queryParams("mail")).thenReturn("pedro@example.com");
        when(req.queryParams("password")).thenReturn("password123");
        when(req.queryParams("tipo_alumno")).thenReturn("Avanzado");

        ModelAndView view = StudentLogic.createStudent(req, res);

        assertNotNull(view);
        Map<String, Object> model = view.getModel();
        assertEquals("Ya existe un alumno con ese DNI.", model.get("errorMessage"));
    }

    @Test
    void listStudents_withAuthenticatedAdmin_returnsModelWithStudents() {
        Persona p = new Persona();
        p.setDni("11111111");
        p.setNombre("Carlos");
        p.setApellido("Rodriguez");
        p.setMail("carlos@example.com");
        p.insert();

        Base.exec("INSERT INTO alumno (dni, tipo_alumno) VALUES ('11111111', 'Ingresante');");

        Request req = mock(Request.class);
        Response res = mock(Response.class);
        Session session = mock(Session.class);

        when(req.session()).thenReturn(session);
        when(session.attribute("currentUserUsername")).thenReturn("admin");
        when(session.attribute("loggedIn")).thenReturn(true);
        when(session.attribute("role")).thenReturn("admin");

        ModelAndView view = StudentLogic.listStudents(req, res);

        assertNotNull(view);
        Map<String, Object> model = view.getModel();
        
        assertTrue(model.containsKey("students"));
        List<?> students = (List<?>) model.get("students");
        assertEquals(1, students.size());

        assertEquals("student_list.mustache", view.getViewName());
    }

    @Test
    void listStudents_withUnauthenticatedUser_redirectsWithError() {
        Request req = mock(Request.class);
        Response res = mock(Response.class);
        Session session = mock(Session.class);

        when(req.session()).thenReturn(session);
        when(session.attribute("currentUserUsername")).thenReturn(null);
        when(session.attribute("loggedIn")).thenReturn(false);

        ModelAndView view = StudentLogic.listStudents(req, res);

        assertNull(view);
        verify(res).redirect("/?error=No autorizado");
    }

    @Test
    void editStudentForm_returnsModelWithStudentData() {
        Persona p = new Persona();
        p.setDni("22222222");
        p.setNombre("Ana");
        p.setApellido("Martinez");
        p.setMail("ana@example.com");
        p.insert();

        Base.exec("INSERT INTO alumno (dni, tipo_alumno) VALUES ('22222222', 'Avanzado');");

        Request req = mock(Request.class);
        Response res = mock(Response.class);

        when(req.params(":dni")).thenReturn("22222222");

        ModelAndView view = StudentLogic.editStudentForm(req, res);

        assertNotNull(view);
        Map<String, Object> model = view.getModel();
        
        assertEquals("22222222", model.get("dni"));
        assertEquals("Ana", model.get("nombre"));
        assertEquals("Martinez", model.get("apellido"));
        assertEquals("ana@example.com", model.get("mail"));
        assertEquals("Avanzado", model.get("tipo_alumno"));

        assertEquals("student_edit.mustache", view.getViewName());
    }

    @Test
    void editStudentForm_withNonexistentStudent_redirects() {
        Request req = mock(Request.class);
        Response res = mock(Response.class);

        when(req.params(":dni")).thenReturn("NONEXISTENT");

        ModelAndView view = StudentLogic.editStudentForm(req, res);

        assertNull(view);
        verify(res).redirect("/students");
    }

    @Test
    void editStudent_updatesStudentData() {
        Persona p = new Persona();
        p.setDni("33333333");
        p.setNombre("Luis");
        p.setApellido("Sanchez");
        p.setMail("luis@example.com");
        p.insert();

        Base.exec("INSERT INTO alumno (dni, tipo_alumno) VALUES ('33333333', 'Ingresante');");

        Request req = mock(Request.class);
        Response res = mock(Response.class);

        when(req.params(":dni")).thenReturn("33333333");
        when(req.queryParams("nombre")).thenReturn("Luis Updated");
        when(req.queryParams("apellido")).thenReturn("Sanchez Updated");
        when(req.queryParams("mail")).thenReturn("luisupdated@example.com");
        when(req.queryParams("tipo_alumno")).thenReturn("Avanzado");

        StudentLogic.editStudent(req, res);

        Persona updated = Persona.findById("33333333");
        assertNotNull(updated);
        assertEquals("Luis Updated", updated.getNombre());
        assertEquals("Sanchez Updated", updated.getApellido());
        assertEquals("luisupdated@example.com", updated.getMail());

        verify(res).redirect("/students");
    }

    @Test
    void delete_removesStudentAndRedirects() {
        Persona p = new Persona();
        p.setDni("44444444");
        p.setNombre("Rosa");
        p.setApellido("Flores");
        p.setMail("rosa@example.com");
        p.insert();

        Base.exec("INSERT INTO alumno (dni, tipo_alumno) VALUES ('44444444', 'Ingresante');");

        Users user = new Users();
        user.set("id", 1L);
        user.setName("rosa@example.com");
        user.setPassword(BCrypt.hashpw("password", BCrypt.gensalt()));
        user.insert();

        Request req = mock(Request.class);
        Response res = mock(Response.class);

        when(req.params(":dni")).thenReturn("44444444");

        StudentLogic.delete(req, res);

        Persona deleted = Persona.findById("44444444");
        assertNull(deleted);

        Users deletedUser = Users.findFirst("name = ?", "rosa@example.com");
        assertNull(deletedUser);

        verify(res).redirect("/students");
    }

    @Test
    void delete_withNonexistentStudent_stillRedirects() {
        Request req = mock(Request.class);
        Response res = mock(Response.class);

        when(req.params(":dni")).thenReturn("NONEXISTENT");

        StudentLogic.delete(req, res);

        verify(res).redirect("/students");
    }
}
