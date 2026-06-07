package com.is1.proyecto.logic;

import spark.Request;
import spark.Response;

import static com.is1.proyecto.logic.UserLogic.*;
import static spark.Spark.halt;
import com.is1.proyecto.models.Persona;
import com.is1.proyecto.models.Materia;

import org.javalite.activejdbc.LazyList;

import java.util.HashMap;
import java.util.Map;

import spark.ModelAndView;

public class InscripcionLogic {

    // Middleware de seguridad
    public static void middleware(Request req, Response res) {

        // Verifica que el usuario esté autenticado y sea alumno
        if (!isAuthenticated(req) || !ROLE_STUDENT.equals(req.session().attribute("role"))) {
            res.redirect("/?error=No autorizado");
            halt(401);
        }
    }

    // GET: /inscripciones
// Muestra las materias disponibles y las inscripciones del alumno.
public static ModelAndView listarInscripciones(Request req, Response res) {

    // Obtiene los datos de la sesión
    String currentUsername = req.session().attribute("currentUserUsername");
    Boolean loggedIn = req.session().attribute("loggedIn");

    // Verifica que el usuario esté autenticado
    if (currentUsername == null || loggedIn == null || !loggedIn) {
        res.redirect("/?error=No autorizado");
        return null;
    }
    
    // Busca la persona asociada al usuario logueado
    Persona persona = Persona.findFirst("mail = ? OR dni = ?",currentUsername, currentUsername);

    // Si no existe la persona, vuelve al inicio
    if (persona == null) {
        res.redirect("/?error=Alumno no encontrado");
        return null;
    }
    // Obtiene el DNI del alumno
    String dniAlumno = persona.getDni();

    // Modelo que se enviará a Mustache
    Map<String, Object> model = new HashMap<>();

    // Guarda el DNI para futuras operaciones
    model.put("dniAlumno", dniAlumno);

    // Obtiene todas las materias disponibles
    LazyList<Materia> materias = Materia.findAll();

    // Envía las materias a la vista
    model.put("materias", materias);

    // Por ahora las inscripciones estarán vacías.
    // Más adelante cargaremos las materias en las que el alumno ya está anotado.
    model.put("inscripciones", new java.util.ArrayList<>());

    // Devuelve la vista
    return new ModelAndView(model, "inscripciones.mustache");
}
}