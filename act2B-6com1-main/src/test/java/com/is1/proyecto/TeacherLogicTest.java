package com.is1.proyecto;

import com.is1.proyecto.logic.TeacherLogic;
import com.is1.proyecto.models.Docente;
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

class TeacherLogicTest {

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

        Base.exec("CREATE TABLE docente ("
                + "dni VARCHAR(20) PRIMARY KEY, "
                + "nro_legajo VARCHAR(50) UNIQUE NOT NULL, "
                + "titulo VARCHAR(255), "
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
    void createTeacher_withAuthenticatedUser_returnsModelAndView() {
        Request req = mock(Request.class);
        Response res = mock(Response.class);
        Session session = mock(Session.class);

        when(req.session()).thenReturn(session);
        when(session.attribute("currentUserUsername")).thenReturn("admin");
        when(session.attribute("loggedIn")).thenReturn(true);

        ModelAndView view = TeacherLogic.createTeacher(req, res);

        assertNotNull(view);
        assertEquals("teacher_form.mustache", view.getViewName());
    }

    @Test
    void createTeacher_withUnauthenticatedUser_redirectsToLogin() {
        Request req = mock(Request.class);
        Response res = mock(Response.class);
        Session session = mock(Session.class);

        when(req.session()).thenReturn(session);
        when(session.attribute("currentUserUsername")).thenReturn(null);
        when(session.attribute("loggedIn")).thenReturn(false);

        ModelAndView view = TeacherLogic.createTeacher(req, res);

        assertNull(view);
        verify(res).redirect("/login?error=Debes iniciar sesión para acceder a esta página.");
    }

    @Test
    void storeInDB_createsNewTeacherWithValidData() {
        Request req = mock(Request.class);
        Response res = mock(Response.class);
        Session session = mock(Session.class);

        when(req.session()).thenReturn(session);
        when(session.attribute("currentUserUsername")).thenReturn("admin");
        when(session.attribute("loggedIn")).thenReturn(true);

        when(req.queryParams("dni")).thenReturn("12345678");
        when(req.queryParams("nombre")).thenReturn("Juan");
        when(req.queryParams("apellido")).thenReturn("Perez");
        when(req.queryParams("mail")).thenReturn("juan@example.com");
        when(req.queryParams("titulo")).thenReturn("Ingeniero");
        when(req.queryParams("nro_legajo")).thenReturn("LEG001");
        when(req.queryParams("password")).thenReturn("password123");

        TeacherLogic.storeInDB(req, res);

        Persona persona = Persona.findById("12345678");
        assertNotNull(persona);
        assertEquals("Juan", persona.getNombre());
        assertEquals("Perez", persona.getApellido());
        assertEquals("juan@example.com", persona.getMail());

        Docente docente = Docente.findById("12345678");
        assertNotNull(docente);
        assertEquals("Ingeniero", docente.getTitulo());
        assertEquals("LEG001", docente.getNroLegajo());

        Users user = Users.findFirst("name = ?", "juan@example.com");
        assertNotNull(user);

        verify(res).redirect("/teachers");
    }

    @Test
    void storeInDB_withMissingFields_returnsErrorMessage() {
        Request req = mock(Request.class);
        Response res = mock(Response.class);
        Session session = mock(Session.class);

        when(req.session()).thenReturn(session);
        when(session.attribute("currentUserUsername")).thenReturn("admin");
        when(session.attribute("loggedIn")).thenReturn(true);

        when(req.queryParams("dni")).thenReturn("12345678");
        when(req.queryParams("nombre")).thenReturn("");
        when(req.queryParams("apellido")).thenReturn("Perez");
        when(req.queryParams("mail")).thenReturn("juan@example.com");
        when(req.queryParams("titulo")).thenReturn("Ingeniero");
        when(req.queryParams("nro_legajo")).thenReturn("LEG001");
        when(req.queryParams("password")).thenReturn("password123");

        ModelAndView view = TeacherLogic.storeInDB(req, res);

        assertNotNull(view);
        Map<String, Object> model = view.getModel();
        assertTrue(model.containsKey("errorMessage"));
        assertEquals("Todos los campos son obligatorios.", model.get("errorMessage"));
    }

    @Test
    void storeInDB_withInvalidEmail_returnsErrorMessage() {
        Request req = mock(Request.class);
        Response res = mock(Response.class);
        Session session = mock(Session.class);

        when(req.session()).thenReturn(session);
        when(session.attribute("currentUserUsername")).thenReturn("admin");
        when(session.attribute("loggedIn")).thenReturn(true);

        when(req.queryParams("dni")).thenReturn("12345678");
        when(req.queryParams("nombre")).thenReturn("Juan");
        when(req.queryParams("apellido")).thenReturn("Perez");
        when(req.queryParams("mail")).thenReturn("invalid-email");
        when(req.queryParams("titulo")).thenReturn("Ingeniero");
        when(req.queryParams("nro_legajo")).thenReturn("LEG001");
        when(req.queryParams("password")).thenReturn("password123");

        ModelAndView view = TeacherLogic.storeInDB(req, res);

        assertNotNull(view);
        Map<String, Object> model = view.getModel();
        assertEquals("El formato del mail no es valido", model.get("errorMessage"));
    }

    @Test
    void storeInDB_withDuplicateDni_returnsErrorMessage() {
        Persona existing = new Persona();
        existing.setDni("87654321");
        existing.setNombre("Maria");
        existing.setApellido("Lopez");
        existing.setMail("maria@example.com");
        existing.insert();

        Request req = mock(Request.class);
        Response res = mock(Response.class);
        Session session = mock(Session.class);

        when(req.session()).thenReturn(session);
        when(session.attribute("currentUserUsername")).thenReturn("admin");
        when(session.attribute("loggedIn")).thenReturn(true);

        when(req.queryParams("dni")).thenReturn("87654321");
        when(req.queryParams("nombre")).thenReturn("Pedro");
        when(req.queryParams("apellido")).thenReturn("Garcia");
        when(req.queryParams("mail")).thenReturn("pedro@example.com");
        when(req.queryParams("titulo")).thenReturn("Ingeniero");
        when(req.queryParams("nro_legajo")).thenReturn("LEG002");
        when(req.queryParams("password")).thenReturn("password123");

        ModelAndView view = TeacherLogic.storeInDB(req, res);

        assertNotNull(view);
        Map<String, Object> model = view.getModel();
        assertEquals("Ya existe un usuario registrado con ese DNI.", model.get("errorMessage"));
    }

    @Test
    void listTeachers_withAuthenticatedAdmin_returnsModelWithTeachers() {
        Persona p = new Persona();
        p.setDni("11111111");
        p.setNombre("Carlos");
        p.setApellido("Rodriguez");
        p.setMail("carlos@example.com");
        p.insert();

        Docente d = new Docente();
        d.setDni("11111111");
        d.setNroLegajo("LEG003");
        d.setTitulo("Doctor");
        d.insert();

        Request req = mock(Request.class);
        Response res = mock(Response.class);
        Session session = mock(Session.class);

        when(req.session()).thenReturn(session);
        when(session.attribute("currentUserUsername")).thenReturn("admin");
        when(session.attribute("loggedIn")).thenReturn(true);
        when(session.attribute("role")).thenReturn("admin");

        ModelAndView view = TeacherLogic.listTeachers(req, res);

        assertNotNull(view);
        Map<String, Object> model = view.getModel();
        
        assertTrue(model.containsKey("teachers"));
        List<?> teachers = (List<?>) model.get("teachers");
        assertEquals(1, teachers.size());

        assertEquals("teacher_list.mustache", view.getViewName());
    }

    @Test
    void listTeachers_withUnauthenticatedUser_redirectsWithError() {
        Request req = mock(Request.class);
        Response res = mock(Response.class);
        Session session = mock(Session.class);

        when(req.session()).thenReturn(session);
        when(session.attribute("currentUserUsername")).thenReturn(null);
        when(session.attribute("loggedIn")).thenReturn(false);

        ModelAndView view = TeacherLogic.listTeachers(req, res);

        assertNull(view);
        verify(res).redirect("/?error=No autorizado");
    }

    @Test
    void dashboard_withAuthenticatedTeacher_returnsModelAndView() {
        Request req = mock(Request.class);
        Response res = mock(Response.class);
        Session session = mock(Session.class);

        when(req.session()).thenReturn(session);
        when(session.attribute("currentUserUsername")).thenReturn("teacher@example.com");
        when(session.attribute("loggedIn")).thenReturn(true);

        ModelAndView view = TeacherLogic.dashboard(req, res);

        assertNotNull(view);
        Map<String, Object> model = view.getModel();
        assertEquals("teacher@example.com", model.get("username"));
        assertEquals("teacher_dashboard.mustache", view.getViewName());
    }

    @Test
    void dashboard_withUnauthenticatedUser_redirectsWithError() {
        Request req = mock(Request.class);
        Response res = mock(Response.class);
        Session session = mock(Session.class);

        when(req.session()).thenReturn(session);
        when(session.attribute("currentUserUsername")).thenReturn(null);
        when(session.attribute("loggedIn")).thenReturn(false);

        ModelAndView view = TeacherLogic.dashboard(req, res);

        assertNull(view);
        verify(res).redirect("/?error=Acceso denegado.");
    }

    @Test
    void searchTeachers_returnsMatchingTeachers() {
        Persona p1 = new Persona();
        p1.setDni("22222222");
        p1.setNombre("Ana");
        p1.setApellido("Martinez");
        p1.setMail("ana@example.com");
        p1.insert();

        Docente d1 = new Docente();
        d1.setDni("22222222");
        d1.setNroLegajo("LEG004");
        d1.setTitulo("Profesora");
        d1.insert();

        Request req = mock(Request.class);
        when(req.queryParams("q")).thenReturn("Ana");

        ModelAndView view = TeacherLogic.searchTeachers(req, res);

        assertNotNull(view);
        Map<String, Object> model = view.getModel();
        
        assertTrue(model.containsKey("teachers"));
        List<?> teachers = (List<?>) model.get("teachers");
        assertEquals(1, teachers.size());
    }

    private Response res;

    @Test
    void searchTeachers_withNoResults_returnsErrorMessage() {
        Persona p = new Persona();
        p.setDni("33333333");
        p.setNombre("Luis");
        p.setApellido("Sanchez");
        p.setMail("luis@example.com");
        p.insert();

        Docente d = new Docente();
        d.setDni("33333333");
        d.setNroLegajo("LEG005");
        d.setTitulo("Profesor");
        d.insert();

        Request req = mock(Request.class);
        when(req.queryParams("q")).thenReturn("Inexistent");

        ModelAndView view = TeacherLogic.searchTeachers(req, res);

        assertNotNull(view);
        Map<String, Object> model = view.getModel();
        
        assertTrue(model.containsKey("errorMessage"));
        assertTrue(((String) model.get("errorMessage")).contains("No se encontraron docentes"));
        
        List<?> teachers = (List<?>) model.get("teachers");
        assertEquals(1, teachers.size());
    }

    @Test
    void editTeacherForm_returnsModelWithTeacherData() {
        Persona p = new Persona();
        p.setDni("44444444");
        p.setNombre("Rosa");
        p.setApellido("Flores");
        p.setMail("rosa@example.com");
        p.insert();

        Docente d = new Docente();
        d.setDni("44444444");
        d.setNroLegajo("LEG006");
        d.setTitulo("Ingeniera");
        d.insert();

        Request req = mock(Request.class);
        Response res = mock(Response.class);

        when(req.params(":dni")).thenReturn("44444444");

        ModelAndView view = TeacherLogic.editTeacherForm(req, res);

        assertNotNull(view);
        Map<String, Object> model = view.getModel();
        
        assertEquals("44444444", model.get("dni"));
        assertEquals("Rosa", model.get("nombre"));
        assertEquals("Flores", model.get("apellido"));
        assertEquals("rosa@example.com", model.get("mail"));
        assertEquals("Ingeniera", model.get("titulo"));
        assertEquals("LEG006", model.get("nro_legajo"));

        assertEquals("teacher_edit.mustache", view.getViewName());
    }

    @Test
    void editTeacher_updatesTeacherData() {
        Persona p = new Persona();
        p.setDni("55555555");
        p.setNombre("Jorge");
        p.setApellido("Alvarez");
        p.setMail("jorge@example.com");
        p.insert();

        Docente d = new Docente();
        d.setDni("55555555");
        d.setNroLegajo("LEG007");
        d.setTitulo("Profesor");
        d.insert();

        Request req = mock(Request.class);
        Response res = mock(Response.class);

        when(req.params(":dni")).thenReturn("55555555");
        when(req.queryParams("nombre")).thenReturn("Jorge Updated");
        when(req.queryParams("apellido")).thenReturn("Alvarez Updated");
        when(req.queryParams("mail")).thenReturn("jorgeupdated@example.com");
        when(req.queryParams("titulo")).thenReturn("Ingeniero");

        TeacherLogic.editTeacher(req, res);

        Persona updated = Persona.findById("55555555");
        assertNotNull(updated);
        assertEquals("Jorge Updated", updated.getNombre());
        assertEquals("Alvarez Updated", updated.getApellido());
        assertEquals("jorgeupdated@example.com", updated.getMail());

        Docente docenteUpdated = Docente.findById("55555555");
        assertEquals("Ingeniero", docenteUpdated.getTitulo());

        verify(res).redirect("/teachers");
    }

    @Test
    void deleteTeacher_removesTeacherAndPersona() {
        Persona p = new Persona();
        p.setDni("66666666");
        p.setNombre("Sandra");
        p.setApellido("Gonzalez");
        p.setMail("sandra@example.com");
        p.insert();

        Docente d = new Docente();
        d.setDni("66666666");
        d.setNroLegajo("LEG008");
        d.setTitulo("Doctora");
        d.insert();

        Request req = mock(Request.class);
        Response res = mock(Response.class);

        when(req.params(":dni")).thenReturn("66666666");

        TeacherLogic.deleteTeacher(req, res);

        Docente deleted = Docente.findById("66666666");
        assertNull(deleted);

        Persona deletedPersona = Persona.findById("66666666");
        assertNull(deletedPersona);

        verify(res).redirect("/teachers");
    }

    @Test
    void deleteTeacher_withNonexistentTeacher_stillRedirects() {
        Request req = mock(Request.class);
        Response res = mock(Response.class);

        when(req.params(":dni")).thenReturn("NONEXISTENT");

        TeacherLogic.deleteTeacher(req, res);

        verify(res).redirect("/teachers");
    }
}
