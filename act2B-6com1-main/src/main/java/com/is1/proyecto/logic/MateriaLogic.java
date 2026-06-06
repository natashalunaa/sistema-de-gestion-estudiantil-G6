package com.is1.proyecto.logic;

import com.is1.proyecto.models.Materia;
import org.javalite.activejdbc.LazyList;
import spark.ModelAndView;
import spark.Request;
import spark.Response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.is1.proyecto.logic.UserLogic.isAdmin;
import static com.is1.proyecto.logic.UserLogic.isAuthenticated;
import static spark.Spark.halt;

public class MateriaLogic {
    // Middleware de seguridad
    public static void middleware(Request req, Response res) {
        if (!isAuthenticated(req) || !isAdmin(req)) {
            res.redirect("/?error=No autorizado");
            halt(401);
        }
    }

    // GET: /materia/new
    public static ModelAndView createMateria(Request req, Response res){
         // Intenta obtener el nombre de usuario y la bandera de login de la sesión.
        String currentUsername = req.session().attribute("currentUserUsername");
        Boolean loggedIn = req.session().attribute("loggedIn");

        // 1. Verificar si el usuario ha iniciado sesión.
        // Si no hay un nombre de usuario en la sesión, la bandera es nula o falsa,
        // significa que el usuario no está logueado o su sesión expiró.
        if (currentUsername == null || loggedIn == null || !loggedIn) {
            // redirige al login con mensaje de error
            res.redirect("/login?error=Debes iniciar sesión para acceder a esta página.");
            return null;
        }

        Map<String, String> model = new HashMap<>();
        return new ModelAndView(model, "materia_form.mustache");
    }

    // POST: /materia/new (Guardar en la base de datos)
    public static ModelAndView storeInDB(Request req, Response res) {
        // Intenta obtener el nombre de usuario y la bandera de login de la sesión.
        String currentUsername = req.session().attribute("currentUserUsername");
        Boolean loggedIn = req.session().attribute("loggedIn");

        // 1. Verificar si el usuario ha iniciado sesión.
        // Si no hay un nombre de usuario en la sesión, la bandera es nula o falsa,
        // significa que el usuario no está logueado o su sesión expiró.
        if (currentUsername == null || loggedIn == null || !loggedIn) {
            // Redirige al login con un mensaje de error.
            res.redirect("/login?error=Debes iniciar sesión para acceder a esta página.");
            return null;
        }

        Map<String, Object> model = new HashMap<>();

        String codMateria = req.queryParams("cod_materia");
        String numMateriaStr = req.queryParams("num_materia");
    }
}