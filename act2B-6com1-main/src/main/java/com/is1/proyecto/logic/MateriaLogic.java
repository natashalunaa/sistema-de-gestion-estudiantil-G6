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
    public static ModelAndView createMateriaForm(Request req, Response res){
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

        // campos de la tabla 
        String codMateria = req.queryParams("cod_materia");
        String nombreMateria = req.queryParams("nombre_materia");
        String anioMateria = req.queryParams("anio_materia");
        String codInscripcion = req.queryParams("cod_inscripcion");

        // campos obligatorios (NOT NULL del script SQL)
        if (codMateria == null || nombreMateria == null || anioMateria == null ||
            codMateria.isEmpty() || nombreMateria.isEmpty() || anioMateria.isEmpty()) {
            model.put("errorMessage", "El código, nombre y año de la materia son obligatorios.");
            return new ModelAndView(model, "materia_form.mustache");
        }

        if (Materia.findById(codMateria) != null) {
            model.put("errorMessage", "Ya existe una materia registrada con ese código.");
            return new ModelAndView(model, "materia_form.mustache");
        }

        // manejo de excepciones para la BD
        try {
            Materia m = new Materia();
            m.setCodMateria(codMateria);
            m.setNombreMateria(nombreMateria);
            m.setAnioMateria(Integer.parseInt(anioMateria));
            m.setCodInscripcion(codInscripcion); // Puede ser nulo o vacío porque no tiene NOT NULL
            
            m.insert();
        } catch (Exception e){
            e.printStackTrace();
            model.put("errorMessage", "Error al guardar la materia: " + e.getMessage());
            return new ModelAndView(model, "materia_form.mustache");
        } catch (NumberFormatException e) {
            model.put("errorMessage", "El año de la materia debe ser un número entero válido.");
            return new ModelAndView(model, "materia_form.mustache");
        }

        // redirigir a la lista
        res.redirect("/materias");
        return null;
    }

    // Materias List
    public static ModelAndView listMaterias(Request req, Response res) {
        // Intenta obtener el nombre de usuario y la bandera de login de la sesión.
        String currentUsername = req.session().attribute("currentUserUsername");
        Boolean loggedIn = req.session().attribute("loggedIn");

        if (currentUsername == null || loggedIn == null || !loggedIn || !isAdmin(req)) {
            res.redirect("/?error=No autorizado");
            return null;
        }

        Map<String, Object> model = new HashMap<>();
        List<Map<String, Object>> materiasList = new ArrayList<>();

        LazyList<Materia> materias = Materia.findAll();
        for (Materia m : materias) {
            Map<String, Object> row = new HashMap<>();
            // juntamos con los nuevos getters
            row.put("cod_materia", m.getCodMateria());
            row.put("nombre_materia", m.getNombreMateria());
            row.put("anio_materia", m.getAnioMateria());
            row.put("cod_inscripcion", m.getCodInscripcion() != null ? m.getCodInscripcion() : "-");
            materiasList.add(row);
        }

        model.put("materias", materiasList);
        return new ModelAndView(model, "materia_list.mustache");
    }

    // delete Materia
    public static ModelAndView deleteMateria(Request req, Response res) {
        // obtiene el cod único de la materia
        String codMateria = req.params(":cod_materia");

        // busca la materia en la tabla usando el codigo
        Materia m = Materia.findById(codMateria);

        // si la materia existe, la elimina
        if (m != null) {
            m.delete();
        }
        // una vez eliminado, se redirige al listado de materias
        res.redirect("/materias");
        return null;
    }

    // GET: /materia/edit/:cod_materia (edit Materia)
    public static ModelAndView editMateriaForm(Request req, Response res) {
        // obtiene el código de la materia que viene en la URL
        String codMateria = req.params(":cod_materia");

        // busca la materia en la tabla según ese código
        Materia materia = Materia.findById(codMateria);

        // si no existe, redirige al listado
        if (materia == null) {
            res.redirect("/materias?error=Materia+no+encontrada");
            return null;
        }

        // encargado de rellenar los inputs del formulario
        Map<String, Object> model = new HashMap<>();

        model.put("cod_materia", materia.getCodMateria());
        model.put("nombre_materia", materia.getNombreMateria());
        model.put("anio_materia", materia.getAnioMateria());
        model.put("cod_inscripcion", materia.getCodInscripcion());

        // Abre la pantalla de edición pasándole los datos actuales
        return new ModelAndView(model, "materia_edit.mustache");
    }

    // POST: /materia/edit/:cod_materia 
    public static ModelAndView editMateria(Request req, Response res) {
        
        // obtiene el codigo de la materia desde la URL
        String codMateria = req.params(":cod_materia");

        // obtiene lo que el usuario cambió en el formulario
        String nombreMateria = req.queryParams("nombre_materia");
        String anioMateria = req.queryParams("anio_materia");
        String codInscripcion = req.queryParams("cod_inscripcion");

        // busca el registro existente en la BD
        Materia m = Materia.findById(codMateria);

        if (m != null) {
            try {
                // actualiza los nuevos valores
                m.setNombreMateria(nombreMateria);
                m.setAnioMateria(Integer.parseInt(anioMateria));
                m.setCodInscripcion(codInscripcion);
                
                // .saveIt() es el método de ActiveJDBC que hace el update directo en Postgres
                m.saveIt(); 
            } catch (NumberFormatException e) {
                // si cargan texto en el año, relanzamos el formulario con error
                Map<String, Object> model = new HashMap<>();
                model.put("cod_materia", codMateria);
                model.put("nombre_materia", nombreMateria);
                model.put("anio_materia", anioMateria);
                model.put("cod_inscripcion", codInscripcion);
                model.put("errorMessage", "El año debe ser un número entero válido.");
                return new ModelAndView(model, "materia_edit.mustache");
            }
        }

        // una vez guardados los cambios, volvemos a la lista general
        res.redirect("/materias");
        return null;
    }
}