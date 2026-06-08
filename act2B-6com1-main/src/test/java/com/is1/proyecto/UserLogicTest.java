package com.is1.proyecto;

import com.is1.proyecto.logic.UserLogic;
import com.is1.proyecto.models.Alumno;
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

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserLogicTest {

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
                + "mail VARCHAR(255) UNIQUE"
                + ");");

        Base.exec("CREATE TABLE docente ("
                + "dni VARCHAR(20) PRIMARY KEY, "
                + "nro_legajo VARCHAR(50) UNIQUE NOT NULL, "
                + "titulo VARCHAR(255), "
                + "FOREIGN KEY(dni) REFERENCES persona(dni) ON DELETE CASCADE"
                + ");");

        Base.exec("CREATE TABLE alumno ("
                + "dni VARCHAR(20) PRIMARY KEY, "
                + "nro_legajo VARCHAR(50) UNIQUE NOT NULL, "
                + "año_ingreso INTEGER, "
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
    void login_withoutUserLoggedIn_returnsLoginFormWithoutMessages() {
        Request req = mock(Request.class);
        Response res = mock(Response.class);

        when(req.queryParams("error")).thenReturn(null);
        when(req.queryParams("message")).thenReturn(null);

        ModelAndView view = UserLogic.login(req, res);

        assertNotNull(view);
        assertEquals("login.mustache", view.getViewName());
        
        Map<String, Object> model = view.getModel();
        assertFalse(model.containsKey("errorMessage"));
        assertFalse(model.containsKey("successMessage"));
    }

    @Test
    void login_withErrorMessage_returnsLoginFormWithError() {
        Request req = mock(Request.class);
        Response res = mock(Response.class);

        when(req.queryParams("error")).thenReturn("Usuario incorrecto");
        when(req.queryParams("message")).thenReturn(null);

        ModelAndView view = UserLogic.login(req, res);

        assertNotNull(view);
        Map<String, Object> model = view.getModel();
        assertEquals("Usuario incorrecto", model.get("errorMessage"));
    }

    @Test
    void login_withSuccessMessage_returnsLoginFormWithMessage() {
        Request req = mock(Request.class);
        Response res = mock(Response.class);

        when(req.queryParams("error")).thenReturn(null);
        when(req.queryParams("message")).thenReturn("Cuenta creada exitosamente");

        ModelAndView view = UserLogic.login(req, res);

        assertNotNull(view);
        Map<String, Object> model = view.getModel();
        assertEquals("Cuenta creada exitosamente", model.get("successMessage"));
    }

    @Test
    void loginUser_withValidAdminCredentials_redirectsToAdminDashboard() {
        String hashedPassword = BCrypt.hashpw("admin123", BCrypt.gensalt());
        
        Users admin = new Users();
        admin.set("name", "admin");
        admin.set("password", hashedPassword);
        admin.insert();

        Request req = mock(Request.class);
        Response res = mock(Response.class);
        Session session = mock(Session.class);

        when(req.session(true)).thenReturn(session);
        when(req.session()).thenReturn(session);
        when(req.queryParams("username")).thenReturn("admin");
        when(req.queryParams("password")).thenReturn("admin123");

        UserLogic.loginUser(req, res);

        verify(session).attribute("role", "admin");
        verify(session).attribute("currentUserUsername", "admin");
        verify(session).attribute("loggedIn", true);
        verify(res).redirect("/dashboard/admin");
    }

    @Test
    void loginUser_withValidTeacherCredentials_redirectsToTeacherDashboard() {
        String hashedPassword = BCrypt.hashpw("teacher123", BCrypt.gensalt());
        
        Users teacher = new Users();
        teacher.set("name", "teacher@example.com");
        teacher.set("password", hashedPassword);
        teacher.insert();

        Persona p = new Persona();
        p.setDni("11111111");
        p.setNombre("Juan");
        p.setApellido("Docente");
        p.setMail("teacher@example.com");
        p.insert();

        Docente d = new Docente();
        d.setDni("11111111");
        d.setNroLegajo("LEG001");
        d.setTitulo("Ingeniero");
        d.insert();

        Request req = mock(Request.class);
        Response res = mock(Response.class);
        Session session = mock(Session.class);

        when(req.session(true)).thenReturn(session);
        when(req.session()).thenReturn(session);
        when(req.queryParams("username")).thenReturn("teacher@example.com");
        when(req.queryParams("password")).thenReturn("teacher123");

        UserLogic.loginUser(req, res);

        verify(session).attribute("role", "teacher");
        verify(session).attribute("currentUserUsername", "teacher@example.com");
        verify(session).attribute("loggedIn", true);
        verify(res).redirect("/dashboard/teacher");
    }

    @Test
    void loginUser_withValidStudentCredentials_redirectsToStudentDashboard() {
        String hashedPassword = BCrypt.hashpw("student123", BCrypt.gensalt());
        
        Users student = new Users();
        student.set("name", "student@example.com");
        student.set("password", hashedPassword);
        student.insert();

        Persona p = new Persona();
        p.setDni("22222222");
        p.setNombre("Maria");
        p.setApellido("Alumna");
        p.setMail("student@example.com");
        p.insert();

        Alumno a = new Alumno();
        a.setDni("22222222");
        a.setNroLegajo("ALU001");
        a.setAñoIngreso(2024);
        a.insert();

        Request req = mock(Request.class);
        Response res = mock(Response.class);
        Session session = mock(Session.class);

        when(req.session(true)).thenReturn(session);
        when(req.session()).thenReturn(session);
        when(req.queryParams("username")).thenReturn("student@example.com");
        when(req.queryParams("password")).thenReturn("student123");

        UserLogic.loginUser(req, res);

        verify(session).attribute("role", "student");
        verify(session).attribute("currentUserUsername", "student@example.com");
        verify(session).attribute("loggedIn", true);
        verify(res).redirect("/dashboard/student");
    }

    @Test
    void loginUser_withInvalidPassword_redirectsWithError() {
        String hashedPassword = BCrypt.hashpw("correctPassword", BCrypt.gensalt());
        
        Users user = new Users();
        user.set("name", "testuser");
        user.set("password", hashedPassword);
        user.insert();

        Request req = mock(Request.class);
        Response res = mock(Response.class);

        when(req.queryParams("username")).thenReturn("testuser");
        when(req.queryParams("password")).thenReturn("wrongPassword");

        UserLogic.loginUser(req, res);

        verify(res).redirect("/?error=Usuario incorrecto");
    }

    @Test
    void loginUser_withNonexistentUser_redirectsWithError() {
        Request req = mock(Request.class);
        Response res = mock(Response.class);

        when(req.queryParams("username")).thenReturn("nonexistent");
        when(req.queryParams("password")).thenReturn("password123");

        UserLogic.loginUser(req, res);

        verify(res).redirect("/?error=Usuario incorrecto");
    }

    @Test
    void loginUser_withEmptyUsername_redirectsWithError() {
        Request req = mock(Request.class);
        Response res = mock(Response.class);

        when(req.queryParams("username")).thenReturn("");
        when(req.queryParams("password")).thenReturn("password123");

        UserLogic.loginUser(req, res);

        verify(res).redirect("/?error=Debe completar todos los campos");
    }

    @Test
    void loginUser_withEmptyPassword_redirectsWithError() {
        Request req = mock(Request.class);
        Response res = mock(Response.class);

        when(req.queryParams("username")).thenReturn("testuser");
        when(req.queryParams("password")).thenReturn("");

        UserLogic.loginUser(req, res);

        verify(res).redirect("/?error=Debe completar todos los campos");
    }

    @Test
    void createUser_returnsUserFormWithoutMessages() {
        Request req = mock(Request.class);
        Response res = mock(Response.class);

        when(req.queryParams("error")).thenReturn(null);
        when(req.queryParams("message")).thenReturn(null);

        ModelAndView view = UserLogic.createUser(req, res);

        assertNotNull(view);
        assertEquals("user_form.mustache", view.getViewName());
        
        Map<String, Object> model = view.getModel();
        assertFalse(model.containsKey("errorMessage"));
        assertFalse(model.containsKey("successMessage"));
    }

    @Test
    void createUser_withErrorParameter_returnsFormWithError() {
        Request req = mock(Request.class);
        Response res = mock(Response.class);

        when(req.queryParams("error")).thenReturn("El nombre ya existe");
        when(req.queryParams("message")).thenReturn(null);

        ModelAndView view = UserLogic.createUser(req, res);

        assertNotNull(view);
        Map<String, Object> model = view.getModel();
        assertEquals("El nombre ya existe", model.get("errorMessage"));
    }

    @Test
    void registerNewUser_createsFirstUserAsAdmin() throws Exception {
        Request req = mock(Request.class);
        Response res = mock(Response.class);

        when(req.queryParams("name")).thenReturn("admin");
        when(req.queryParams("password")).thenReturn("admin123");

        UserLogic.registerNewUser(req, res);

        Users createdUser = Users.findFirst("name = ?", "admin");
        assertNotNull(createdUser);
        assertTrue(BCrypt.checkpw("admin123", createdUser.getString("password")));

        verify(res).redirect("/?message=Cuenta de administrador 'admin' creada con éxito.");
    }

    @Test
    void registerNewUser_createsSecondUserAsStudent() throws Exception {
        Users admin = new Users();
        admin.set("name", "admin");
        admin.set("password", BCrypt.hashpw("admin123", BCrypt.gensalt()));
        admin.insert();

        Request req = mock(Request.class);
        Response res = mock(Response.class);

        when(req.queryParams("name")).thenReturn("student");
        when(req.queryParams("password")).thenReturn("student123");

        UserLogic.registerNewUser(req, res);

        Users createdUser = Users.findFirst("name = ?", "student");
        assertNotNull(createdUser);
        assertTrue(BCrypt.checkpw("student123", createdUser.getString("password")));

        verify(res).redirect("/?message=Cuenta de estudiante 'student' creada con éxito.");
    }

    @Test
    void registerNewUser_withDuplicateUsername_redirectsWithError() {
        Users existing = new Users();
        existing.set("name", "duplicate");
        existing.set("password", BCrypt.hashpw("password", BCrypt.gensalt()));
        existing.insert();

        Request req = mock(Request.class);
        Response res = mock(Response.class);

        when(req.queryParams("name")).thenReturn("duplicate");
        when(req.queryParams("password")).thenReturn("newpassword");

        UserLogic.registerNewUser(req, res);

        verify(res).redirect("/user/create?error=El nombre de usuario ya existe.");
    }

    @Test
    void registerNewUser_withEmptyName_redirectsWithError() {
        Request req = mock(Request.class);
        Response res = mock(Response.class);

        when(req.queryParams("name")).thenReturn("");
        when(req.queryParams("password")).thenReturn("password123");

        UserLogic.registerNewUser(req, res);

        verify(res).redirect("/user/create?error=Nombre y contraseña son requeridos.");
    }

    @Test
    void registerNewUser_withEmptyPassword_redirectsWithError() {
        Request req = mock(Request.class);
        Response res = mock(Response.class);

        when(req.queryParams("name")).thenReturn("testuser");
        when(req.queryParams("password")).thenReturn("");

        UserLogic.registerNewUser(req, res);

        verify(res).redirect("/user/create?error=Nombre y contraseña son requeridos.");
    }

    @Test
    void dashboard_withAuthenticatedUser_returnsModelWithUsername() {
        Request req = mock(Request.class);
        Response res = mock(Response.class);
        Session session = mock(Session.class);

        when(req.session()).thenReturn(session);
        when(session.attribute("currentUserUsername")).thenReturn("testuser");
        when(session.attribute("loggedIn")).thenReturn(true);

        ModelAndView view = UserLogic.dashboard(req, res);

        assertNotNull(view);
        assertEquals("dashboard.mustache", view.getViewName());
        
        Map<String, Object> model = view.getModel();
        assertEquals("testuser", model.get("username"));
    }

    @Test
    void dashboard_withUnauthenticatedUser_redirectsToLogin() {
        Request req = mock(Request.class);
        Response res = mock(Response.class);
        Session session = mock(Session.class);

        when(req.session()).thenReturn(session);
        when(session.attribute("currentUserUsername")).thenReturn(null);
        when(session.attribute("loggedIn")).thenReturn(false);

        ModelAndView view = UserLogic.dashboard(req, res);

        assertNull(view);
        verify(res).redirect("/login?error=Debes iniciar sesión para acceder a esta página.");
    }

    @Test
    void dashboard_withNullLoggedInAttribute_redirectsToLogin() {
        Request req = mock(Request.class);
        Response res = mock(Response.class);
        Session session = mock(Session.class);

        when(req.session()).thenReturn(session);
        when(session.attribute("currentUserUsername")).thenReturn("testuser");
        when(session.attribute("loggedIn")).thenReturn(null);

        ModelAndView view = UserLogic.dashboard(req, res);

        assertNull(view);
        verify(res).redirect("/login?error=Debes iniciar sesión para acceder a esta página.");
    }

    @Test
    void logout_invalidatesSessionAndRedirects() {
        Request req = mock(Request.class);
        Response res = mock(Response.class);
        Session session = mock(Session.class);

        when(req.session()).thenReturn(session);

        UserLogic.logout(req, res);

        verify(session).invalidate();
        verify(res).redirect("/");
    }

    @Test
    void isAuthenticated_withLoggedInUser_returnsTrue() {
        Request req = mock(Request.class);
        Session session = mock(Session.class);

        when(req.session()).thenReturn(session);
        when(session.attribute("loggedIn")).thenReturn(true);

        assertTrue(UserLogic.isAuthenticated(req));
    }

    @Test
    void isAuthenticated_withNotLoggedInUser_returnsFalse() {
        Request req = mock(Request.class);
        Session session = mock(Session.class);

        when(req.session()).thenReturn(session);
        when(session.attribute("loggedIn")).thenReturn(false);

        assertFalse(UserLogic.isAuthenticated(req));
    }

    @Test
    void isAuthenticated_withNullLoggedInAttribute_returnsFalse() {
        Request req = mock(Request.class);
        Session session = mock(Session.class);

        when(req.session()).thenReturn(session);
        when(session.attribute("loggedIn")).thenReturn(null);

        assertFalse(UserLogic.isAuthenticated(req));
    }

    @Test
    void isAdmin_withAdminRole_returnsTrue() {
        Request req = mock(Request.class);
        Session session = mock(Session.class);

        when(req.session()).thenReturn(session);
        when(session.attribute("role")).thenReturn("admin");

        assertTrue(UserLogic.isAdmin(req));
    }

    @Test
    void isAdmin_withTeacherRole_returnsFalse() {
        Request req = mock(Request.class);
        Session session = mock(Session.class);

        when(req.session()).thenReturn(session);
        when(session.attribute("role")).thenReturn("teacher");

        assertFalse(UserLogic.isAdmin(req));
    }

    @Test
    void isAdmin_withStudentRole_returnsFalse() {
        Request req = mock(Request.class);
        Session session = mock(Session.class);

        when(req.session()).thenReturn(session);
        when(session.attribute("role")).thenReturn("student");

        assertFalse(UserLogic.isAdmin(req));
    }

    @Test
    void isAdmin_withNullRole_returnsFalse() {
        Request req = mock(Request.class);
        Session session = mock(Session.class);

        when(req.session()).thenReturn(session);
        when(session.attribute("role")).thenReturn(null);

        assertFalse(UserLogic.isAdmin(req));
    }

    @Test
    void passwordHashingUsesCorrectBCryptPattern() {
        String plainPassword = "testPassword123";
        String hashedPassword = BCrypt.hashpw(plainPassword, BCrypt.gensalt());

        // Verify that plaintext doesn't match hash
        assertNotEquals(plainPassword, hashedPassword);

        // Verify that BCrypt can verify the password
        assertTrue(BCrypt.checkpw(plainPassword, hashedPassword));

        // Verify that wrong password doesn't verify
        assertFalse(BCrypt.checkpw("wrongPassword", hashedPassword));
    }

    @Test
    void registerNewUser_passwordIsStoredAsHash() {
        Request req = mock(Request.class);
        Response res = mock(Response.class);

        when(req.queryParams("name")).thenReturn("hashtest");
        when(req.queryParams("password")).thenReturn("myPassword");

        UserLogic.registerNewUser(req, res);

        Users user = Users.findFirst("name = ?", "hashtest");
        assertNotNull(user);

        String storedPassword = user.getString("password");
        assertNotEquals("myPassword", storedPassword);
        assertTrue(BCrypt.checkpw("myPassword", storedPassword));
        assertFalse(BCrypt.checkpw("wrongPassword", storedPassword));
    }
}
