package com.is1.proyecto;

import com.is1.proyecto.logic.MateriaLogic;
import com.is1.proyecto.models.Materia;
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

class MateriaLogicTest {

    @BeforeEach
    void setUp() {
        Base.open("org.sqlite.JDBC", "jdbc:sqlite::memory:", "", "");
        
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
    void createMateriaForm_withAuthenticatedUser_returnsModelAndView() {
        Request req = mock(Request.class);
        Response res = mock(Response.class);
        Session session = mock(Session.class);

        when(req.session()).thenReturn(session);
        when(session.attribute("currentUserUsername")).thenReturn("admin");
        when(session.attribute("loggedIn")).thenReturn(true);

        ModelAndView view = MateriaLogic.createMateriaForm(req, res);

        assertNotNull(view);
        assertEquals("materia_form.mustache", view.getViewName());
    }

    @Test
    void createMateriaForm_withUnauthenticatedUser_redirectsToLogin() {
        Request req = mock(Request.class);
        Response res = mock(Response.class);
        Session session = mock(Session.class);

        when(req.session()).thenReturn(session);
        when(session.attribute("currentUserUsername")).thenReturn(null);
        when(session.attribute("loggedIn")).thenReturn(false);

        ModelAndView view = MateriaLogic.createMateriaForm(req, res);

        assertNull(view);
        verify(res).redirect("/login?error=Debes iniciar sesión para acceder a esta página.");
    }

    @Test
    void storeInDB_createsNewMateriaAndRedirects() {
        Request req = mock(Request.class);
        Response res = mock(Response.class);
        Session session = mock(Session.class);

        when(req.session()).thenReturn(session);
        when(session.attribute("currentUserUsername")).thenReturn("admin");
        when(session.attribute("loggedIn")).thenReturn(true);

        when(req.queryParams("cod_materia")).thenReturn("MAT001");
        when(req.queryParams("nombre_materia")).thenReturn("Programación");
        when(req.queryParams("anio_materia")).thenReturn("1");
        when(req.queryParams("cod_inscripcion")).thenReturn("INS001");

        MateriaLogic.storeInDB(req, res);

        Materia saved = Materia.findById("MAT001");
        assertNotNull(saved);
        assertEquals("Programación", saved.getNombreMateria());
        assertEquals(1, saved.getAnioMateria().intValue());
        assertEquals("INS001", saved.getCodInscripcion());

        verify(res).status(302);
        verify(res).redirect("/admin/materias");
    }

    @Test
    void storeInDB_withMissingFields_returnsErrorMessage() {
        Request req = mock(Request.class);
        Response res = mock(Response.class);
        Session session = mock(Session.class);

        when(req.session()).thenReturn(session);
        when(session.attribute("currentUserUsername")).thenReturn("admin");
        when(session.attribute("loggedIn")).thenReturn(true);

        when(req.queryParams("cod_materia")).thenReturn("MAT002");
        when(req.queryParams("nombre_materia")).thenReturn("");
        when(req.queryParams("anio_materia")).thenReturn("1");
        when(req.queryParams("cod_inscripcion")).thenReturn("INS002");

        ModelAndView view = MateriaLogic.storeInDB(req, res);

        assertNotNull(view);
        Map<String, Object> model = view.getModel();
        assertTrue(model.containsKey("errorMessage"));
        assertEquals("El código, nombre y año de la materia son obligatorios.", model.get("errorMessage"));
    }

    @Test
    void storeInDB_withDuplicateCode_returnsErrorMessage() {
        Materia existing = new Materia();
        existing.setCodMateria("MAT003");
        existing.setNombreMateria("Bases de Datos");
        existing.setAnioMateria(2);
        existing.insert();

        Request req = mock(Request.class);
        Response res = mock(Response.class);
        Session session = mock(Session.class);

        when(req.session()).thenReturn(session);
        when(session.attribute("currentUserUsername")).thenReturn("admin");
        when(session.attribute("loggedIn")).thenReturn(true);

        when(req.queryParams("cod_materia")).thenReturn("MAT003");
        when(req.queryParams("nombre_materia")).thenReturn("Otra Materia");
        when(req.queryParams("anio_materia")).thenReturn("3");
        when(req.queryParams("cod_inscripcion")).thenReturn("INS003");

        ModelAndView view = MateriaLogic.storeInDB(req, res);

        assertNotNull(view);
        Map<String, Object> model = view.getModel();
        assertTrue(model.containsKey("errorMessage"));
        assertEquals("Ya existe una materia registrada con ese código.", model.get("errorMessage"));
    }

    @Test
    void storeInDB_withInvalidYear_returnsErrorMessage() {
        Request req = mock(Request.class);
        Response res = mock(Response.class);
        Session session = mock(Session.class);

        when(req.session()).thenReturn(session);
        when(session.attribute("currentUserUsername")).thenReturn("admin");
        when(session.attribute("loggedIn")).thenReturn(true);

        when(req.queryParams("cod_materia")).thenReturn("MAT004");
        when(req.queryParams("nombre_materia")).thenReturn("Cálculo");
        when(req.queryParams("anio_materia")).thenReturn("no es número");
        when(req.queryParams("cod_inscripcion")).thenReturn("INS004");

        ModelAndView view = MateriaLogic.storeInDB(req, res);

        assertNotNull(view);
        Map<String, Object> model = view.getModel();
        assertTrue(model.containsKey("errorMessage"));
        assertEquals("El año de la materia debe ser un número entero válido.", model.get("errorMessage"));
    }

    @Test
    void listMaterias_withAuthenticatedAdmin_returnsModelWithMaterias() {
        Materia m1 = new Materia();
        m1.setCodMateria("MAT005");
        m1.setNombreMateria("Estructuras");
        m1.setAnioMateria(2);
        m1.insert();

        Materia m2 = new Materia();
        m2.setCodMateria("MAT006");
        m2.setNombreMateria("Algoritmos");
        m2.setAnioMateria(3);
        m2.insert();

        Request req = mock(Request.class);
        Response res = mock(Response.class);
        Session session = mock(Session.class);

        when(req.session()).thenReturn(session);
        when(session.attribute("currentUserUsername")).thenReturn("admin");
        when(session.attribute("loggedIn")).thenReturn(true);
        when(session.attribute("role")).thenReturn("admin");

        ModelAndView view = MateriaLogic.listMaterias(req, res);

        assertNotNull(view);
        Map<String, Object> model = view.getModel();
        
        assertTrue(model.containsKey("materias"));
        List<?> materias = (List<?>) model.get("materias");
        assertEquals(2, materias.size());

        assertEquals("materia_list.mustache", view.getViewName());
    }

    @Test
    void listMaterias_withUnauthenticatedUser_redirectsWithError() {
        Request req = mock(Request.class);
        Response res = mock(Response.class);
        Session session = mock(Session.class);

        when(req.session()).thenReturn(session);
        when(session.attribute("currentUserUsername")).thenReturn(null);
        when(session.attribute("loggedIn")).thenReturn(false);

        ModelAndView view = MateriaLogic.listMaterias(req, res);

        assertNull(view);
        verify(res).redirect("/?error=No autorizado");
    }

    @Test
    void deleteMateria_removesExistingMateria() {
        Materia m = new Materia();
        m.setCodMateria("MAT007");
        m.setNombreMateria("Redes");
        m.setAnioMateria(4);
        m.insert();

        Request req = mock(Request.class);
        Response res = mock(Response.class);

        when(req.params(":cod_materia")).thenReturn("MAT007");

        MateriaLogic.deleteMateria(req, res);

        Materia deleted = Materia.findById("MAT007");
        assertNull(deleted);

        verify(res).redirect("/admin/materias");
    }

    @Test
    void deleteMateria_withNonexistentMateria_stillRedirects() {
        Request req = mock(Request.class);
        Response res = mock(Response.class);

        when(req.params(":cod_materia")).thenReturn("NONEXISTENT");

        MateriaLogic.deleteMateria(req, res);

        verify(res).redirect("/admin/materias");
    }

    @Test
    void editMateriaForm_returnsModelWithCurrentData() {
        Materia m = new Materia();
        m.setCodMateria("MAT008");
        m.setNombreMateria("Sistemas");
        m.setAnioMateria(5);
        m.setCodInscripcion("INS008");
        m.insert();

        Request req = mock(Request.class);
        Response res = mock(Response.class);

        when(req.params(":cod_materia")).thenReturn("MAT008");

        ModelAndView view = MateriaLogic.editMateriaForm(req, res);

        assertNotNull(view);
        Map<String, Object> model = view.getModel();
        
        assertEquals("MAT008", model.get("cod_materia"));
        assertEquals("Sistemas", model.get("nombre_materia"));
        assertEquals(5, model.get("anio_materia"));
        assertEquals("INS008", model.get("cod_inscripcion"));

        assertEquals("materia_edit.mustache", view.getViewName());
    }

    @Test
    void editMateriaForm_withNonexistentMateria_redirectsWithError() {
        Request req = mock(Request.class);
        Response res = mock(Response.class);

        when(req.params(":cod_materia")).thenReturn("NONEXISTENT");

        ModelAndView view = MateriaLogic.editMateriaForm(req, res);

        assertNull(view);
        verify(res).redirect("/admin/materias?error=Materia+no+encontrada");
    }

    @Test
    void editMateria_updatesExistingMateriaAndRedirects() {
        Materia m = new Materia();
        m.setCodMateria("MAT009");
        m.setNombreMateria("Antiguo Nombre");
        m.setAnioMateria(1);
        m.insert();

        Request req = mock(Request.class);
        Response res = mock(Response.class);

        when(req.params(":cod_materia")).thenReturn("MAT009");
        when(req.queryParams("nombre_materia")).thenReturn("Nuevo Nombre");
        when(req.queryParams("anio_materia")).thenReturn("3");
        when(req.queryParams("cod_inscripcion")).thenReturn("INS009");

        MateriaLogic.editMateria(req, res);

        Materia updated = Materia.findById("MAT009");
        assertNotNull(updated);
        assertEquals("Nuevo Nombre", updated.getNombreMateria());
        assertEquals(3, updated.getAnioMateria().intValue());
        assertEquals("INS009", updated.getCodInscripcion());

        verify(res).status(302);
        verify(res).redirect("/admin/materias");
    }

    @Test
    void editMateria_withInvalidYear_returnsErrorMessage() {
        Materia m = new Materia();
        m.setCodMateria("MAT010");
        m.setNombreMateria("Test Materia");
        m.setAnioMateria(1);
        m.insert();

        Request req = mock(Request.class);
        Response res = mock(Response.class);

        when(req.params(":cod_materia")).thenReturn("MAT010");
        when(req.queryParams("nombre_materia")).thenReturn("Test");
        when(req.queryParams("anio_materia")).thenReturn("invalid");
        when(req.queryParams("cod_inscripcion")).thenReturn("INS010");

        ModelAndView view = MateriaLogic.editMateria(req, res);

        assertNotNull(view);
        Map<String, Object> model = view.getModel();
        assertTrue(model.containsKey("errorMessage"));
        assertEquals("El año debe ser un número entero válido.", model.get("errorMessage"));

        assertEquals("materia_edit.mustache", view.getViewName());
    }
}
