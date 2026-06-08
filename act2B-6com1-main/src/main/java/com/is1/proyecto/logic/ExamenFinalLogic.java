package com.is1.proyecto.logic;

import java.sql.Date;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static com.is1.proyecto.logic.UserLogic.ROLE_TEACHER;
import static com.is1.proyecto.logic.UserLogic.isAuthenticated;
import com.is1.proyecto.models.Alumno;
import com.is1.proyecto.models.AlumnoExamenFinal;
import com.is1.proyecto.models.AlumnoMateria;
import com.is1.proyecto.models.ExamenFinal;
import com.is1.proyecto.models.Materia;
import com.is1.proyecto.models.Persona;

import spark.ModelAndView;
import spark.Request;
import spark.Response;
import static spark.Spark.halt;

public class ExamenFinalLogic {

    public static void middleware(Request req, Response res) {
        if (!isAuthenticated(req) || !ROLE_TEACHER.equals(req.session().attribute("role"))) {
            res.redirect("/?error=No autorizado");
            halt(401);
        }
    }

    public static ModelAndView crearExamenForm(Request req, Response res) {
        String currentUsername = req.session().attribute("currentUserUsername");
        Boolean loggedIn = req.session().attribute("loggedIn");

        if (currentUsername == null || loggedIn == null || !loggedIn || !ROLE_TEACHER.equals(req.session().attribute("role"))) {
            res.redirect("/?error=No autorizado");
            return null;
        }

        Persona docente = Persona.findFirst("mail = ? OR dni = ?", currentUsername, currentUsername);
        if (docente == null) {
            res.redirect("/?error=Docente no encontrado");
            return null;
        }

        String codMateria = req.params(":cod_materia");
        Materia materia = buscarMateriaDelDocente(codMateria, docente.getDni());
        if (materia == null) {
            res.redirect("/dashboard/teacher?error=Materia no encontrada o no asignada al docente");
            return null;
        }

        Map<String, Object> model = new HashMap<>();
        model.put("cod_materia", materia.getCodMateria());
        model.put("nombre_materia", materia.getNombreMateria());
        model.put("errorMessage", req.queryParams("error"));
        model.put("successMessage", req.queryParams("success"));

        return new ModelAndView(model, "crear_examen_final.mustache");
    }

    public static ModelAndView crearExamen(Request req, Response res) {
        String currentUsername = req.session().attribute("currentUserUsername");
        Boolean loggedIn = req.session().attribute("loggedIn");

        if (currentUsername == null || loggedIn == null || !loggedIn || !ROLE_TEACHER.equals(req.session().attribute("role"))) {
            res.redirect("/?error=No autorizado");
            return null;
        }

        Persona docente = Persona.findFirst("mail = ? OR dni = ?", currentUsername, currentUsername);
        if (docente == null) {
            res.redirect("/?error=Docente no encontrado");
            return null;
        }

        String codMateria = req.params(":cod_materia");
        Materia materia = buscarMateriaDelDocente(codMateria, docente.getDni());
        if (materia == null) {
            res.redirect("/dashboard/teacher?error=Materia no encontrada o no asignada al docente");
            return null;
        }

        String fecha = req.queryParams("fecha");
        if (fecha == null || fecha.isBlank()) {
            res.redirect("/teacher/crear-examen/" + codMateria + "?error=Debe seleccionar una fecha");
            return null;
        }

        ExamenFinal examen = new ExamenFinal();
        examen.setCodMateria(codMateria);
        examen.setFecha(Date.valueOf(fecha));
        examen.saveIt();

        res.redirect("/dashboard/teacher?success=Examen final creado");
        return null;
    }

    public static ModelAndView cargarNotaForm(Request req, Response res) {
        String currentUsername = req.session().attribute("currentUserUsername");
        Boolean loggedIn = req.session().attribute("loggedIn");

        if (currentUsername == null || loggedIn == null || !loggedIn || !ROLE_TEACHER.equals(req.session().attribute("role"))) {
            res.redirect("/?error=No autorizado");
            return null;
        }

        ExamenFinal examen = buscarExamenDelDocente(req, res, currentUsername);
        if (examen == null) {
            return null;
        }

        Materia materia = Materia.findById(examen.getCodMateria());
        Map<String, Object> model = new HashMap<>();
        model.put("id_examen", examen.getIdExamen());
        model.put("fecha", examen.getFecha());
        model.put("cod_materia", examen.getCodMateria());
        model.put("nombre_materia", materia != null ? materia.getNombreMateria() : examen.getCodMateria());
        model.put("errorMessage", req.queryParams("error"));
        model.put("successMessage", req.queryParams("success"));

        return new ModelAndView(model, "cargar_nota_final.mustache");
    }

    public static ModelAndView cargarNota(Request req, Response res) {
        String currentUsername = req.session().attribute("currentUserUsername");
        Boolean loggedIn = req.session().attribute("loggedIn");

        if (currentUsername == null || loggedIn == null || !loggedIn || !ROLE_TEACHER.equals(req.session().attribute("role"))) {
            res.redirect("/?error=No autorizado");
            return null;
        }

        ExamenFinal examen = buscarExamenDelDocente(req, res, currentUsername);
        if (examen == null) {
            return null;
        }

        String alumnoDni = req.queryParams("alumno_dni");
        String notaParam = req.queryParams("nota");
        String volver = "/teacher/examen-final/" + examen.getIdExamen() + "/nota";

        if (alumnoDni == null || alumnoDni.isBlank() || notaParam == null || notaParam.isBlank()) {
            res.redirect(volver + "?error=Debe ingresar DNI del alumno y nota");
            return null;
        }

        Alumno alumno = Alumno.findById(alumnoDni);
        if (alumno == null) {
            res.redirect(volver + "?error=Alumno no encontrado");
            return null;
        }

        AlumnoMateria inscripcion = AlumnoMateria.findFirst(
                "alumno_dni = ? AND cod_materia = ?",
                alumnoDni, examen.getCodMateria());
        if (inscripcion == null) {
            res.redirect(volver + "?error=El alumno no esta inscripto en la materia del examen");
            return null;
        }

        BigDecimal nota;
        try {
            nota = new BigDecimal(notaParam);
        } catch (NumberFormatException e) {
            res.redirect(volver + "?error=La nota debe ser numerica");
            return null;
        }

        if (nota.compareTo(BigDecimal.ZERO) < 0 || nota.compareTo(new BigDecimal("10")) > 0) {
            res.redirect(volver + "?error=La nota debe estar entre 0 y 10");
            return null;
        }

        AlumnoExamenFinal alumnoExamen = AlumnoExamenFinal.findFirst(
                "alumno_dni = ? AND id_examen = ?",
                alumnoDni, examen.getIdExamen());
        if (alumnoExamen == null) {
            alumnoExamen = new AlumnoExamenFinal();
            alumnoExamen.setAlumnoDni(alumnoDni);
            alumnoExamen.setIdExamen(examen.getIdExamen());
        }

        alumnoExamen.setNota(nota);
        alumnoExamen.saveIt();

        res.redirect(volver + "?success=Nota cargada correctamente");
        return null;
    }

    private static Materia buscarMateriaDelDocente(String codMateria, String dniDocente) {
        return Materia.findFirst(
                "cod_materia = ? AND cod_materia IN (SELECT cod_materia FROM docente_responsable_materia WHERE docente_dni = ?)",
                codMateria, dniDocente);
    }

    private static ExamenFinal buscarExamenDelDocente(Request req, Response res, String currentUsername) {
        Persona docente = Persona.findFirst("mail = ? OR dni = ?", currentUsername, currentUsername);
        if (docente == null) {
            res.redirect("/?error=Docente no encontrado");
            return null;
        }

        String idExamen = req.params(":id_examen");
        ExamenFinal examen = ExamenFinal.findById(idExamen);
        if (examen == null) {
            res.redirect("/dashboard/teacher?error=Examen final no encontrado");
            return null;
        }

        Materia materia = buscarMateriaDelDocente(examen.getCodMateria(), docente.getDni());
        if (materia == null) {
            res.redirect("/dashboard/teacher?error=Examen no asignado al docente");
            return null;
        }

        return examen;
    }
}
