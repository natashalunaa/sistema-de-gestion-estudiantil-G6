package com.is1.proyecto;

import com.is1.proyecto.logic.CarreraLogic;
import com.is1.proyecto.models.Carrera;
import org.javalite.activejdbc.Base;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.Session;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CarreraLogicTest {

    @BeforeEach
    void setUp() {
        Base.open("org.sqlite.JDBC", "jdbc:sqlite::memory:", "", "");
        Base.exec("CREATE TABLE IF NOT EXISTS carrera ("
                + "cod_carrera VARCHAR(20) PRIMARY KEY, "
                + "nombre_carrera VARCHAR(255) NOT NULL, "
                + "duracion INT NOT NULL"
                + ");");
    }

    @AfterEach
    void tearDown() {
        if (Base.hasConnection()) {
            Base.close();
        }
    }

    @Test
    void storeInDB_createsNewCarreraAndRedirects() {
        Request req = mock(Request.class);
        Response res = mock(Response.class);
        Session session = mock(Session.class);

        when(req.session()).thenReturn(session);
        when(session.attribute("currentUserUsername")).thenReturn("admin");
        when(session.attribute("loggedIn")).thenReturn(true);
        when(session.attribute("role")).thenReturn("admin");

        when(req.queryParams("cod_carrera")).thenReturn("C002");
        when(req.queryParams("nombre_carrera")).thenReturn("Arquitectura");
        when(req.queryParams("duracion")).thenReturn("6");

        CarreraLogic.storeInDB(req, res);

        Carrera saved = Carrera.findById("C002");
        assertNotNull(saved);
        assertEquals("Arquitectura", saved.getNombreCarrera());
        assertEquals(6, saved.getDuracion().intValue());

        verify(res).status(302);
        verify(res).redirect("/admin/carreras");
    }

    @Test
    void listCarreras_returnsModelContainingCarreras() {
        Carrera carrera = new Carrera();
        carrera.setCodCarrera("C003");
        carrera.setNombreCarrera("Medicina");
        carrera.setDuracion(7);
        carrera.insert();

        Request req = mock(Request.class);
        Response res = mock(Response.class);
        Session session = mock(Session.class);
        when(req.session()).thenReturn(session);
        when(session.attribute("currentUserUsername")).thenReturn("admin");
        when(session.attribute("loggedIn")).thenReturn(true);

        ModelAndView view = CarreraLogic.listCarreras(req, res);
        assertNotNull(view);

        Map<String, Object> model = view.getModel();
        assertTrue(model.containsKey("carreras"));

        List<?> carreras = (List<?>) model.get("carreras");
        assertFalse(carreras.isEmpty());

        Map<?, ?> first = (Map<?, ?>) carreras.get(0);
        assertEquals("C003", first.get("cod_carrera"));
        assertEquals("Medicina", first.get("nombre_carrera"));
        assertEquals(7, first.get("duracion"));
    }
}
