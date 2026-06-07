package com.is1.proyecto.logic;

import com.is1.proyecto.models.Carrera;
import org.javalite.activejdbc.LazyList;
import spark.ModelAndView;
import spark.Request;
import spark.Response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.is1.proyecto.logic.UserLogic.isAdmin;

public class CarreraLogic {

    // GET: /admin/carrera/new
    public static ModelAndView createCarreraForm(Request req, Response res) {
        // 1. Control de sesión idéntico a MateriaLogic
        String currentUsername = req.session().attribute("currentUserUsername");
        Boolean loggedIn = req.session().attribute("loggedIn");

        if (currentUsername == null || loggedIn == null || !loggedIn || !isAdmin(req)) {
            res.redirect("/login?error=Debes iniciar sesión como administrador.");
            return null;
        }

        Map<String, Object> model = new HashMap<>();
        return new ModelAndView(model, "carrera_form.mustache");
    }

    // POST: /admin/carrera/new
    public static ModelAndView storeInDB(Request req, Response res) {
        // 1. Control de sesión
        String currentUsername = req.session().attribute("currentUserUsername");
        Boolean loggedIn = req.session().attribute("loggedIn");

        if (currentUsername == null || loggedIn == null || !loggedIn || !isAdmin(req)) {
            res.redirect("/login?error=Debes iniciar sesión como administrador.");
            return null;
        }

        Map<String, Object> model = new HashMap<>();

        // Campos recibidos desde el formulario carrera_form.mustache
        String codCarrera = req.queryParams("cod_carrera");
        String nombreCarrera = req.queryParams("nombre_carrera");
        String duracionStr = req.queryParams("duracion");

        // Validación de campos obligatorios
        if (codCarrera == null || nombreCarrera == null || duracionStr == null ||
            codCarrera.isEmpty() || nombreCarrera.isEmpty() || duracionStr.isEmpty()) {
            model.put("errorMessage", "Todos los campos son obligatorios.");
            return new ModelAndView(model, "carrera_form.mustache");
        }

        // Verificar si ya existe el código de carrera usando ActiveJDBC (.findById)
        if (Carrera.findById(codCarrera) != null) {
            model.put("errorMessage", "Ya existe una carrera registrada con ese código.");
            return new ModelAndView(model, "carrera_form.mustache");
        }

        try {
            Carrera c = new Carrera();
            c.setCodCarrera(codCarrera);
            c.setNombreCarrera(nombreCarrera);
            c.set("duracion", Integer.parseInt(duracionStr)); // Asignamos duración como entero
            
            // Registramos con ActiveJDBC de manera limpia
            c.insert(); 

            // Redirección directa al listado de carreras
            res.status(302);
            res.redirect("/admin/carreras");
            return null;

        } catch (Exception e) {
            e.printStackTrace();
            if (e instanceof NumberFormatException) {
                model.put("errorMessage", "La duración debe ser un número entero válido.");
            } else {
                model.put("errorMessage", "Error al guardar la carrera: " + e.getMessage());
            }
            return new ModelAndView(model, "carrera_form.mustache");
        }
    }

    // GET: /carreras
    public static ModelAndView listCarreras(Request req, Response res) {
        // 1. Control de sesión
        String currentUsername = req.session().attribute("currentUserUsername");
        Boolean loggedIn = req.session().attribute("loggedIn");

        if (currentUsername == null || loggedIn == null || !loggedIn) {
            res.redirect("/login?error=Debes iniciar sesión.");
            return null;
        }

        Map<String, Object> model = new HashMap<>();
        List<Map<String, Object>> carrerasList = new ArrayList<>();

        try {
            // Buscamos todas las carreras de la base de datos
            LazyList<Carrera> carreras = Carrera.findAll();
            if (carreras != null) {
                for (Carrera c : carreras) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("cod_carrera", c.getCodCarrera());
                    row.put("nombre_carrera", c.getNombreCarrera());
                    row.put("duracion", c.get("duracion"));
                    carrerasList.add(row);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        model.put("carreras", carrerasList);
        return new ModelAndView(model, "carrera_list.mustache");
    }

    // GET: /admin/carrera/edit/:cod_carrera
    public static ModelAndView editCarreraForm(Request req, Response res) {
        // Control de sesión
        String currentUsername = req.session().attribute("currentUserUsername");
        Boolean loggedIn = req.session().attribute("loggedIn");
        if (currentUsername == null || loggedIn == null || !loggedIn || !isAdmin(req)) {
            res.redirect("/login?error=Debes iniciar sesión como administrador.");
            return null;
        }

        String codCarrera = req.params(":cod_carrera");
        Carrera carrera = Carrera.findById(codCarrera);

        if (carrera == null) {
            // CORREGIDO: Redirección al listado oficial si no existe la carrera
            res.redirect("/admin/carreras?error=Carrera+no+encontrada");
            return null;
        }

        Map<String, Object> model = new HashMap<>();
        model.put("cod_carrera", carrera.getCodCarrera());
        model.put("nombre_carrera", carrera.getNombreCarrera());
        model.put("duracion", carrera.get("duracion"));

        return new ModelAndView(model, "carrera_edit.mustache");
    }

    // POST: /admin/carrera/edit/:cod_carrera
    public static ModelAndView editCarrera(Request req, Response res) {
        // Control de sesión
        String currentUsername = req.session().attribute("currentUserUsername");
        Boolean loggedIn = req.session().attribute("loggedIn");
        if (currentUsername == null || loggedIn == null || !loggedIn || !isAdmin(req)) {
            res.redirect("/login?error=Debes iniciar sesión como administrador.");
            return null;
        }

        String codCarrera = req.params(":cod_carrera");
        String nombreCarrera = req.queryParams("nombre_carrera");
        String duracionStr = req.queryParams("duracion");

        Carrera c = Carrera.findById(codCarrera);

        if (c != null) {
            try {
                c.setNombreCarrera(nombreCarrera);
                c.set("duracion", Integer.parseInt(duracionStr));
                c.saveIt(); // Hace el UPDATE en Postgres

                res.status(302);
                res.redirect("/admin/carreras");
                return null;
            } catch (Exception e) {
                Map<String, Object> model = new HashMap<>();
                model.put("cod_carrera", codCarrera);
                model.put("nombre_carrera", nombreCarrera);
                model.put("duracion", duracionStr);
                
                if (e instanceof NumberFormatException) {
                    model.put("errorMessage", "La duración debe ser un número entero válido.");
                } else {
                    model.put("errorMessage", "Error al actualizar la carrera: " + e.getMessage());
                }
                return new ModelAndView(model, "carrera_edit.mustache");
            }
        }

        res.redirect("/admin/carreras");
        return null;
    }

    // GET o POST: /admin/carrera/delete/:cod_carrera
    public static ModelAndView deleteCarrera(Request req, Response res) {
        // Control de sesión idéntico a MateriaLogic
        String currentUsername = req.session().attribute("currentUserUsername");
        Boolean loggedIn = req.session().attribute("loggedIn");
        if (currentUsername == null || loggedIn == null || !loggedIn || !isAdmin(req)) {
            res.redirect("/login?error=No autorizado");
            return null;
        }

        // Obtiene el código único de la carrera desde la URL
        String codCarrera = req.params(":cod_carrera");

        // Busca la carrera en la tabla usando ActiveJDBC
        Carrera c = Carrera.findById(codCarrera);

        // Si existe, la borra de una
        if (c != null) {
            c.delete();
        }

        // Una vez eliminado, redirige al listado de carreras
        res.redirect("/admin/carreras");
        return null;
    }
}