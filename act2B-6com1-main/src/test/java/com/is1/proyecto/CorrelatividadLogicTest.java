package com.is1.proyecto;

import com.is1.proyecto.logic.CorrelatividadLogic;
import com.is1.proyecto.models.Correlatividad;
import com.is1.proyecto.models.Materia;
import com.is1.proyecto.models.MateriaCorrelatividad;
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

class CorrelatividadLogicTest {

    @BeforeEach
    void setUp() {
        Base.open("org.sqlite.JDBC", "jdbc:sqlite::memory:", "", "");
        
        // Create required tables
        Base.exec("CREATE TABLE correlatividad ("
                + "id_correlatividad BIGINT PRIMARY KEY, "
                + "correl VARCHAR(20) NOT NULL"
                + ");");

        Base.exec("CREATE TABLE materia ("
                + "cod_materia VARCHAR(20) PRIMARY KEY, "
                + "nombre_materia VARCHAR(100) NOT NULL, "
                + "anio_materia INTEGER NOT NULL, "
                + "cod_inscripcion VARCHAR(20)"
                + ");");

        Base.exec("CREATE TABLE materia_correlatividad ("
                + "id BIGINT PRIMARY KEY, "
                + "materia_origen VARCHAR(20) NOT NULL, "
                + "materia_requerida VARCHAR(20) NOT NULL, "
                + "id_correlatividad BIGINT NOT NULL, "
                + "UNIQUE(materia_origen, materia_requerida), "
                + "FOREIGN KEY(id_correlatividad) REFERENCES correlatividad(id_correlatividad) ON DELETE CASCADE, "
                + "FOREIGN KEY(materia_origen) REFERENCES materia(cod_materia) ON DELETE CASCADE, "
                + "FOREIGN KEY(materia_requerida) REFERENCES materia(cod_materia) ON DELETE CASCADE, "
                + "CHECK (materia_origen <> materia_requerida)"
                + ");");

        // Insert correlatividad types
        Base.exec("INSERT INTO correlatividad (id_correlatividad, correl) VALUES (1, 'Aprobado');");
        Base.exec("INSERT INTO correlatividad (id_correlatividad, correl) VALUES (2, 'Regular');");
    }

    @AfterEach
    void tearDown() {
        if (Base.hasConnection()) {
            Base.close();
        }
    }

    @Test
    void listarCorrelatividades_withAuthenticatedAdmin_returnsModelWithCorrelativas() {
        // Setup materias
        Materia m1 = new Materia();
        m1.setCodMateria("MAT001");
        m1.set("nombre_materia", "Programación");
        m1.set("anio_materia", 1);
        m1.insert();

        Materia m2 = new Materia();
        m2.setCodMateria("MAT002");
        m2.set("nombre_materia", "Estructuras");
        m2.set("anio_materia", 2);
        m2.insert();

        // Setup correlativa
        MateriaCorrelatividad corr = new MateriaCorrelatividad();
        corr.setId(1L);
        corr.setIdCorrelatividad(1L);
        corr.setMateriaOrigen("MAT002");
        corr.setMateriaRequerida("MAT001");
        corr.insert();

        // Mock request and response
        Request req = mock(Request.class);
        Response res = mock(Response.class);
        Session session = mock(Session.class);

        when(req.session()).thenReturn(session);
        when(session.attribute("currentUserUsername")).thenReturn("admin");
        when(session.attribute("loggedIn")).thenReturn(true);
        when(session.attribute("role")).thenReturn("admin");

        ModelAndView view = CorrelatividadLogic.listarCorrelatividades(req, res);

        assertNotNull(view);
        Map<String, Object> model = view.getModel();
        
        assertTrue(model.containsKey("correlativas"));
        assertTrue(model.containsKey("materias"));

        List<?> correlativas = (List<?>) model.get("correlativas");
        assertEquals(1, correlativas.size());

        List<?> materias = (List<?>) model.get("materias");
        assertEquals(2, materias.size());
    }

    @Test
    void agregarCorrelatividad_createsValidCorrelativaAndRedirects() {
        // Setup materias
        Materia m1 = new Materia();
        m1.setCodMateria("MAT003");
        m1.set("nombre_materia", "Bases de Datos");
        m1.set("anio_materia", 2);
        m1.insert();

        Materia m2 = new Materia();
        m2.setCodMateria("MAT004");
        m2.set("nombre_materia", "Algoritmos");
        m2.set("anio_materia", 1);
        m2.insert();

        // Mock request and response
        Request req = mock(Request.class);
        Response res = mock(Response.class);

        when(req.queryParams("materia_origen")).thenReturn("MAT004");
        when(req.queryParams("materia_requerida")).thenReturn("MAT003");
        when(req.queryParams("id_correlatividad")).thenReturn("1");

        CorrelatividadLogic.agregarCorrelatividad(req, res);

        MateriaCorrelatividad saved = MateriaCorrelatividad.findFirst(
                "materia_origen = ? AND materia_requerida = ?", "MAT004", "MAT003");

        assertNotNull(saved);
        assertEquals(1L, saved.getIdCorrelatividad().longValue());

        verify(res).redirect("/materias/correlativas");
    }

    @Test
    void agregarCorrelatividad_withSameMatter_redirectsWithError() {
        // Setup materia
        Materia m1 = new Materia();
        m1.setCodMateria("MAT005");
        m1.set("nombre_materia", "Matemáticas");
        m1.set("anio_materia", 1);
        m1.insert();

        // Mock request and response
        Request req = mock(Request.class);
        Response res = mock(Response.class);

        when(req.queryParams("materia_origen")).thenReturn("MAT005");
        when(req.queryParams("materia_requerida")).thenReturn("MAT005");
        when(req.queryParams("id_correlatividad")).thenReturn("1");

        ModelAndView view = CorrelatividadLogic.agregarCorrelatividad(req, res);

        assertNull(view);
        verify(res).redirect("/materias/correlativas?error=Una materia no puede ser correlativa de si misma");
    }

    @Test
    void agregarCorrelatividad_withDuplicate_redirectsWithError() {
        // Setup materias
        Materia m1 = new Materia();
        m1.setCodMateria("MAT006");
        m1.set("nombre_materia", "Física");
        m1.set("anio_materia", 1);
        m1.insert();

        Materia m2 = new Materia();
        m2.setCodMateria("MAT007");
        m2.set("nombre_materia", "Cálculo");
        m2.set("anio_materia", 1);
        m2.insert();

        // Create existing correlativa
        MateriaCorrelatividad existente = new MateriaCorrelatividad();
        existente.setId(2L);
        existente.setIdCorrelatividad(1L);
        existente.setMateriaOrigen("MAT007");
        existente.setMateriaRequerida("MAT006");
        existente.insert();

        // Mock request and response
        Request req = mock(Request.class);
        Response res = mock(Response.class);

        when(req.queryParams("materia_origen")).thenReturn("MAT007");
        when(req.queryParams("materia_requerida")).thenReturn("MAT006");
        when(req.queryParams("id_correlatividad")).thenReturn("1");

        ModelAndView view = CorrelatividadLogic.agregarCorrelatividad(req, res);

        assertNull(view);
        verify(res).redirect("/materias/correlativas?error=La correlatividad ya existe");
    }

    @Test
    void agregarCorrelatividad_withCircularDependency_redirectsWithError() {
        // Setup materias
        Materia m1 = new Materia();
        m1.setCodMateria("MAT008");
        m1.set("nombre_materia", "Inglés");
        m1.set("anio_materia", 1);
        m1.insert();

        Materia m2 = new Materia();
        m2.setCodMateria("MAT009");
        m2.set("nombre_materia", "Francés");
        m2.set("anio_materia", 1);
        m2.insert();

        // Create inverse correlativa
        MateriaCorrelatividad inversa = new MateriaCorrelatividad();
        inversa.setId(3L);
        inversa.setIdCorrelatividad(1L);
        inversa.setMateriaOrigen("MAT008");
        inversa.setMateriaRequerida("MAT009");
        inversa.insert();

        // Mock request and response
        Request req = mock(Request.class);
        Response res = mock(Response.class);

        when(req.queryParams("materia_origen")).thenReturn("MAT009");
        when(req.queryParams("materia_requerida")).thenReturn("MAT008");
        when(req.queryParams("id_correlatividad")).thenReturn("1");

        ModelAndView view = CorrelatividadLogic.agregarCorrelatividad(req, res);

        assertNull(view);
        verify(res).redirect("/materias/correlativas?error=Dependencia circular");
    }

    @Test
    void eliminarCorrelatividad_removesCorrelativaAndRedirects() {
        // Setup materias
        Materia m1 = new Materia();
        m1.setCodMateria("MAT010");
        m1.set("nombre_materia", "Historia");
        m1.set("anio_materia", 1);
        m1.insert();

        Materia m2 = new Materia();
        m2.setCodMateria("MAT011");
        m2.set("nombre_materia", "Geografía");
        m2.set("anio_materia", 1);
        m2.insert();

        // Create correlativa
        MateriaCorrelatividad corr = new MateriaCorrelatividad();
        corr.setId(4L);
        corr.setIdCorrelatividad(2L);
        corr.setMateriaOrigen("MAT011");
        corr.setMateriaRequerida("MAT010");
        corr.insert();

        // Mock request and response
        Request req = mock(Request.class);
        Response res = mock(Response.class);

        when(req.queryParams("id_correlatividad")).thenReturn("2");
        when(req.queryParams("materia_origen")).thenReturn("MAT011");
        when(req.queryParams("materia_requerida")).thenReturn("MAT010");

        CorrelatividadLogic.eliminarCorrelatividad(req, res);

        MateriaCorrelatividad deleted = MateriaCorrelatividad.findFirst(
                "materia_origen = ? AND materia_requerida = ?", "MAT011", "MAT010");

        assertNull(deleted);
        verify(res).redirect("/materias/correlativas");
    }
}
