package com.is1.proyecto.logic;

import com.is1.proyecto.models.DocenteResponsableMateria;
import com.is1.proyecto.models.Docente;
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

public class CatedraLogic {

    // Middleware de seguridad
    public static void middleware(Request req, Response res) {
        if (!isAuthenticated(req) || !isAdmin(req)) {
            res.redirect("/?error=No autorizado");
            halt(401);
        }
    }

    // GET: /catedras
    // Muestra las asignaciones actuales y carga
    // los datos necesarios para el formulario.
    public static ModelAndView listarCatedras(Request req, Response res) {

        // Obtiene los datos de la sesión
        String currentUsername = req.session().attribute("currentUserUsername");
        Boolean loggedIn = req.session().attribute("loggedIn");

        // Verifica que el usuario esté autenticado y sea administrador
        if (currentUsername == null || loggedIn == null || !loggedIn || !isAdmin(req)) {
            res.redirect("/?error=No autorizado");
            return null;
        }

        // Modelo que se enviará a Mustache
        Map<String, Object> model = new HashMap<>();

        // Lista que contendrá las asignaciones
        List<Map<String, Object>> catedrasList = new ArrayList<>();

        // Obtiene todas las asignaciones de la base de datos
        LazyList<DocenteResponsableMateria> catedras = DocenteResponsableMateria.findAll();

        // Recorre las asignaciones
        for (DocenteResponsableMateria c : catedras) {
            Map<String, Object> row = new HashMap<>();
            row.put("docente_dni", c.getDocenteDni());
            row.put("cod_materia", c.getCodMateria());

            catedrasList.add(row);
        }

        // Envía la lista a Mustache
        model.put("catedras", catedrasList);

        // Carga la lista de docentes disponibles
        // para el formulario de asignación.
        List<Docente> docentes = Docente.findAll();
        model.put("docentes", docentes);

        // Carga la lista de materias disponibles
        // para el formulario de asignación.
        List<Materia> materias = Materia.findAll();
        model.put("materias", materias);

        // Devuelve la vista
        return new ModelAndView(model, "catedras.mustache");
    }

    // POST: /catedras/asignar
    // Crea una nueva asignación entre un docente y una materia

    public static ModelAndView asignarDocente(Request req, Response res) {

        // Obtiene los datos enviados desde el formulario
        String docenteDni = req.queryParams("docente_dni");
        String codMateria = req.queryParams("cod_materia");

        // Verifica si la asignacion ya existe, esto evita que un mismo docente sea
        // asignado dos veces a la misma materia
        DocenteResponsableMateria existente = DocenteResponsableMateria.findFirst("docente_dni = ? and cod_materia = ?",
                docenteDni, codMateria);

        // si ya existe, vuelve a la pagina de catedras mostando un mensaje de error
        if (existente != null) {
            res.redirect("/catedras?error=Asignacion existente");
            return null;
        }

        // crea y guarda la asignación
        DocenteResponsableMateria.createIt("docente_dni", docenteDni, "cod_materia", codMateria);
        // una vez terminado vuelve al listado de catedras
        res.redirect("/catedras");

        return null;
    }

    // POST: /catedras/desasignar
    // Elimina una asignación existente entre un docente y una materia
    public static ModelAndView desasignarDocente(Request req, Response res) {

        // Obtiene los datos enviados desde el formulario
        String docenteDni = req.queryParams("docente_dni");
        String codMateria = req.queryParams("cod_materia");

        // Busca la asignación existente
        DocenteResponsableMateria asignacion = DocenteResponsableMateria.findFirst(
                "docente_dni = ? and cod_materia = ?",docenteDni,codMateria);

        // Si existe, la elimina
        if (asignacion != null) {
            asignacion.delete();
        }

        // Regresa al listado de cátedras
        res.redirect("/catedras");

        return null;
    }
}