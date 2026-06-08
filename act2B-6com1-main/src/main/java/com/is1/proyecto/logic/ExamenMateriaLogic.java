package com.is1.proyecto.logic;

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


public class ExamenMateriaLogic {
    public static void middleware(Request req, Response res) {
        if (!isAuthenticated(req) || !ROLE_TEACHER.equals(req.session().attribute("role"))) {
            res.redirect("/?error=No autorizado");
            halt(401);
        }
    }


//Crear un examen para una materia, desde /teacher/mis-catedras, con un formulario que permita seleccionar la materia, la fecha y el horario del examen.
//GET: /teacher/programar-examen
    public static ModelAndView programarExamenForm(Request req, Response res) {
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

        return new ModelAndView(model, "programar_examen_materia.mustache");
    }

    public static ModelAndView programarExamen (Request req, Response res) {
        String currentUsername = req.session().attribute("currentUserUsername");
        Boolean loggedIn = req.session().attribute("loggedIn");

        if (currentUsername == null || loggedIn == null || !loggedIn || !ROLE_TEACHER.equals(req.session().attribute("role"))) {
            res.redirect("/?error=No autorizado");
            return null;
        }

        //Buscar que el examen exista
        String idExamen = req.queryParams("id_examen");
        ExamenFinal examen = ExamenFinal.findById(idExamen);
        if (examen == null) {
            res.redirect("/teacher/programar-examen?error=Examen no encontrado");
            return null;
        }

        // Validar que la materia exista y esté a cargo del docente
        Persona docente = Persona.findFirst("mail = ? OR dni = ?", currentUsername, currentUsername);
        if (docente == null) {
            res.redirect("/?error=Docente no encontrado");
            return null;
        }

        String codMateria = req.queryParams("cod_materia");
        Materia materia = Materia.findFirst("cod_materia = ? AND cod_materia IN (SELECT cod_materia FROM docente_responsable_materia WHERE docente_dni = ?)", codMateria, docente.getDni());
        if (materia == null) {
            res.redirect("/?error=Materia no encontrada o no a cargo del docente");
            return null;
        }

        // Crear la relación entre el examen y la materia
        examen.setCodMateria(codMateria);
        examen.saveIt();
        res.redirect("/teacher/mis-catedras?success=Examen programado con éxito");  
        return null;
    }
}
