package com.is1.proyecto.logic;

import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.javalite.activejdbc.LazyList;

import static com.is1.proyecto.logic.UserLogic.ROLE_TEACHER;
import static com.is1.proyecto.logic.UserLogic.isAuthenticated;
import com.is1.proyecto.models.ExamenFinal;
import com.is1.proyecto.models.Materia;
import com.is1.proyecto.models.Persona;

import spark.ModelAndView;
import spark.Request;
import spark.Response;
import static spark.Spark.halt;


public class ExamenFinalLogic {

    //Middleware para creación de examen final
    public static void middleware(Request req, Response res) {
        if (!isAuthenticated(req) || !ROLE_TEACHER.equals(req.session().attribute("role"))) {
            res.redirect("/?error=No autorizado");
            halt(401);
        }
    }

    //GET teacher/crear-examen
    public static ModelAndView crearExamenForm(Request req, Response res) {
        String currentUsername = req.session().attribute("currentUserUsername");
        Boolean loggedIn = req.session().attribute("loggedIn");

        if (currentUsername == null || loggedIn == null || !loggedIn || !ROLE_TEACHER.equals(req.session().attribute("role"))) {
            res.redirect("/?error=No autorizado");
            return null;
        }

        Map<String, Object> model = new HashMap<>();

        // Obtener el docente actual
        Persona docente = Persona.findFirst("mail = ? OR dni = ?", currentUsername, currentUsername);
        if (docente == null) {
            res.redirect("/?error=Docente no encontrado");
            return null;
        }

        // Obtener las materias a cargo del docente
        LazyList<Materia> materias = Materia.find("cod_materia IN (SELECT cod_materia FROM docente_responsable_materia WHERE docente_dni = ?)", docente.getDni());
        model.put("materias", materias);

        return new ModelAndView(model, "crear_examen_final.mustache");
    }

    // POST teacher/crear-examen
    public static ModelAndView crearExamen(Request req, Response res) {
        String currentUsername = req.session().attribute("currentUserUsername");
        Boolean loggedIn = req.session().attribute("loggedIn");

        if (currentUsername == null || loggedIn == null || !loggedIn || !ROLE_TEACHER.equals(req.session().attribute("role"))) {
            res.redirect("/?error=No autorizado");
            return null;
        }

        String idExamen = req.queryParams("id_examen");
        long idExam = Long.parseLong(idExamen);
        String fecha = req.queryParams("fecha");
        Date fechaExamen = Date.from(Instant.parse(fecha));


        ExamenFinal examen = new ExamenFinal();
        examen.setIdExamen(idExam);
        examen.setFecha(fechaExamen);
        examen.setNota(null); // La nota se asignará después del examen
        examen.saveIt();

        res.redirect("dashboard/teacher/");
        return null;
    }


}
