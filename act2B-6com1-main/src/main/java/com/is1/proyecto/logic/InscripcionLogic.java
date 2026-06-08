package com.is1.proyecto.logic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.javalite.activejdbc.Base;
import org.javalite.activejdbc.LazyList;

import static com.is1.proyecto.logic.StudentLogic.isStudentProfileComplete;
import static com.is1.proyecto.logic.UserLogic.ROLE_STUDENT;
import static com.is1.proyecto.logic.UserLogic.isAuthenticated;
import com.is1.proyecto.models.Alumno;
import com.is1.proyecto.models.AlumnoExamenFinal;
import com.is1.proyecto.models.AlumnoMateria;
import com.is1.proyecto.models.ExamenFinal;
import com.is1.proyecto.models.Materia;
import com.is1.proyecto.models.MateriaCorrelatividad;
import com.is1.proyecto.models.Persona;

import spark.ModelAndView;
import spark.Request;
import spark.Response;
import static spark.Spark.halt;

public class InscripcionLogic {

    // Middleware de seguridad
    public static void middleware(Request req, Response res) {

        // Verifica que el usuario esté autenticado y sea alumno
        if (!isAuthenticated(req) || !ROLE_STUDENT.equals(req.session().attribute("role"))) {
            res.redirect("/?error=No autorizado");
            halt(401);
        }

        String currentUsername = req.session().attribute("currentUserUsername");
        if (!isStudentProfileComplete(currentUsername)) {
            res.redirect("/student/complete-profile");
            halt(302);
        }
    }

    // GET: /inscripciones
    public static ModelAndView listarInscripciones(Request req, Response res) {

        String currentUsername = req.session().attribute("currentUserUsername");
        Boolean loggedIn = req.session().attribute("loggedIn");

        if (currentUsername == null || loggedIn == null || !loggedIn) {
            res.redirect("/?error=No autorizado");
            return null;
        }

        Persona persona = Persona.findFirst("mail = ? OR dni = ?", currentUsername, currentUsername);

        if (persona == null) {
            res.redirect("/?error=Alumno no encontrado");
            return null;
        }

        String dniAlumno = persona.getDni();

        Map<String, Object> model = new HashMap<>();

        model.put("dniAlumno", dniAlumno);
        model.put("successMessage", req.queryParams("success"));
        model.put("errorMessage", req.queryParams("error"));

        LazyList<Materia> materias = Materia.findAll();

        LazyList<AlumnoMateria> inscripcionesAlumno = AlumnoMateria.where("alumno_dni = ?", dniAlumno);
        for (AlumnoMateria inscripcion : inscripcionesAlumno) {
            String codMateriaInscripta = inscripcion.getCodMateria();
            materias.removeIf(m -> m.getCodMateria().equals(codMateriaInscripta));
        }

        model.put("materias", materias);
        model.put("inscripciones", new java.util.ArrayList<>());

        return new ModelAndView(model, "inscripciones.mustache");

    }

    public static ModelAndView inscribirMateria(Request req, Response res) {
        String currentUsername = req.session().attribute("currentUserUsername");
        Boolean loggedIn = req.session().attribute("loggedIn");
        String codMateria = req.params(":cod_materia");

        if (currentUsername == null || loggedIn == null || !loggedIn) {
            res.redirect("/student/inscripciones?error=No autorizado");
            return null;
        }

        Materia materia = Materia.findById(codMateria);
        if (materia == null) {
            res.redirect("/student/inscripciones?error=Materia no encontrada");
            return null;
        }

        String codInscripcion = req.queryParams("cod_inscripcion");
        if (codInscripcion == null || codInscripcion.isBlank()) {
            res.redirect("/student/inscripciones?error=Debe ingresar el codigo de inscripcion");
            return null;
        }

        if (materia.getCodInscripcion() == null
                || !materia.getCodInscripcion().equalsIgnoreCase(codInscripcion.trim())) {
            res.redirect("/student/inscripciones?error=Codigo de inscripcion incorrecto");
            return null;
        }

        Persona persona = Persona.findFirst("mail = ? OR dni = ?", currentUsername, currentUsername);
        if (persona == null) {
            res.redirect("/student/inscripciones?error=Alumno no encontrado");
            return null;
        }
        Alumno alumno = Alumno.findFirst("dni = ?", persona.getDni());
        if (alumno == null) {
            res.redirect("/student/inscripciones?error=Alumno no encontrado");
            return null;
        }

        AlumnoMateria inscripcionExistente = AlumnoMateria.findFirst("alumno_dni = ? AND cod_materia = ?", alumno.getDni(), codMateria);
        if (inscripcionExistente != null) {
            res.redirect("/student/inscripciones?error=Ya estás inscripto en esta materia");
            return null;
        }

        String correlativaError = validarCorrelativas(alumno.getDni(), codMateria);
        if (correlativaError != null) {
            res.redirect("/student/inscripciones?error=" + correlativaError);
            return null;
        }

        Base.exec(
                "INSERT INTO alumno_materia (alumno_dni, cod_materia) "
                        + "VALUES (?, ?)",
                alumno.getDni(), codMateria);

        res.redirect("/dashboard/student?success=Inscripción realizada con éxito");
        return null;
    }

    public static ModelAndView listarExamenesDisponibles(Request req, Response res) {
        String currentUsername = req.session().attribute("currentUserUsername");
        Boolean loggedIn = req.session().attribute("loggedIn");

        if (currentUsername == null || loggedIn == null || !loggedIn) {
            res.redirect("/?error=No autorizado");
            return null;
        }

        Alumno alumno = obtenerAlumnoLogueado(currentUsername);
        if (alumno == null) {
            res.redirect("/?error=Alumno no encontrado");
            return null;
        }

        Map<String, Object> model = new HashMap<>();
        model.put("dniAlumno", alumno.getDni());
        model.put("successMessage", req.queryParams("success"));
        model.put("errorMessage", req.queryParams("error"));

        List<Map<String, Object>> examenesList = new ArrayList<>();
        LazyList<AlumnoMateria> inscripciones = AlumnoMateria.where("alumno_dni = ?", alumno.getDni());
        for (AlumnoMateria inscripcion : inscripciones) {
            LazyList<ExamenFinal> finales = ExamenFinal.find("cod_materia = ?", inscripcion.getCodMateria());
            for (ExamenFinal examen : finales) {
                AlumnoExamenFinal yaInscripto = AlumnoExamenFinal.findFirst("alumno_dni = ? AND id_examen = ?", alumno.getDni(), examen.getIdExamen());
                if (yaInscripto == null) {
                    Materia materia = Materia.findById(examen.getCodMateria());
                    Map<String, Object> row = new HashMap<>();
                    row.put("id_examen", examen.getIdExamen());
                    row.put("cod_materia", examen.getCodMateria());
                    row.put("nombre_materia", materia != null ? materia.getNombreMateria() : examen.getCodMateria());
                    row.put("fecha", examen.getFecha());
                    examenesList.add(row);
                }
            }
        }

        model.put("examenes", examenesList);
        return new ModelAndView(model, "student_examenes.mustache");
    }

    public static ModelAndView inscribirExamen(Request req, Response res) {
        String currentUsername = req.session().attribute("currentUserUsername");
        Boolean loggedIn = req.session().attribute("loggedIn");
        String idExamen = req.params(":id_examen");

        if (currentUsername == null || loggedIn == null || !loggedIn) {
            res.redirect("/student/examenes?error=No autorizado");
            return null;
        }

        Alumno alumno = obtenerAlumnoLogueado(currentUsername);
        if (alumno == null) {
            res.redirect("/student/examenes?error=Alumno no encontrado");
            return null;
        }

        ExamenFinal examen = ExamenFinal.findById(idExamen);
        if (examen == null) {
            res.redirect("/student/examenes?error=Examen no encontrado");
            return null;
        }

        AlumnoMateria curso = AlumnoMateria.findFirst("alumno_dni = ? AND cod_materia = ?", alumno.getDni(), examen.getCodMateria());
        if (curso == null) {
            res.redirect("/student/examenes?error=No estás inscripto en la materia del examen");
            return null;
        }

        AlumnoExamenFinal yaInscripto = AlumnoExamenFinal.findFirst("alumno_dni = ? AND id_examen = ?", alumno.getDni(), examen.getIdExamen());
        if (yaInscripto != null) {
            res.redirect("/student/examenes?error=Ya estás inscripto en este examen");
            return null;
        }

        AlumnoExamenFinal inscripcion = new AlumnoExamenFinal();
        inscripcion.setAlumnoDni(alumno.getDni());
        inscripcion.setIdExamen(examen.getIdExamen());
        inscripcion.insert();

        res.redirect("/student/notas?success=Inscripción al examen registrada");
        return null;
    }

    public static ModelAndView misNotas(Request req, Response res) {
        String currentUsername = req.session().attribute("currentUserUsername");
        Boolean loggedIn = req.session().attribute("loggedIn");

        if (currentUsername == null || loggedIn == null || !loggedIn) {
            res.redirect("/?error=No autorizado");
            return null;
        }

        Alumno alumno = obtenerAlumnoLogueado(currentUsername);
        if (alumno == null) {
            res.redirect("/?error=Alumno no encontrado");
            return null;
        }

        Map<String, Object> model = new HashMap<>();
        model.put("dniAlumno", alumno.getDni());
        model.put("successMessage", req.queryParams("success"));
        model.put("errorMessage", req.queryParams("error"));

        List<Map<String, Object>> notasList = new ArrayList<>();
        LazyList<AlumnoExamenFinal> notas = AlumnoExamenFinal.where("alumno_dni = ?", alumno.getDni());
        for (AlumnoExamenFinal nota : notas) {
            ExamenFinal examen = ExamenFinal.findById(nota.getIdExamen());
            Materia materia = examen != null ? Materia.findById(examen.getCodMateria()) : null;

            Map<String, Object> row = new HashMap<>();
            row.put("cod_materia", examen != null ? examen.getCodMateria() : "-");
            row.put("nombre_materia", materia != null ? materia.getNombreMateria() : (examen != null ? examen.getCodMateria() : "-"));
            row.put("fecha", examen != null ? examen.getFecha() : null);
            row.put("nota", nota.getNota() != null ? nota.getNota().toString() : "Pendiente");
            notasList.add(row);
        }

        model.put("notas", notasList);
        return new ModelAndView(model, "student_notas.mustache");
    }

    private static String validarCorrelativas(String alumnoDni, String codMateria) {
        LazyList<MateriaCorrelatividad> correlativas = MateriaCorrelatividad.where("materia_origen = ?", codMateria);
        if (correlativas == null || correlativas.isEmpty()) {
            return null;
        }

        for (MateriaCorrelatividad correlativa : correlativas) {
            AlumnoMateria requerida = AlumnoMateria.findFirst(
                    "alumno_dni = ? AND cod_materia = ?",
                    alumnoDni,
                    correlativa.getMateriaRequerida()
            );

            String condicion = requerida != null ? requerida.getCondicionFinal() : null;

            if (correlativa.getIdCorrelatividad() != null && correlativa.getIdCorrelatividad().equals(1L)) {
                if (requerida == null || condicion == null || "Libre".equalsIgnoreCase(condicion)) {
                    return "Debe haber aprobado la correlativa " + correlativa.getMateriaRequerida();
                }
            } else if (correlativa.getIdCorrelatividad() != null && correlativa.getIdCorrelatividad().equals(2L)) {
                if (requerida == null || condicion == null || (!"Regular".equalsIgnoreCase(condicion) && !"Promocion".equalsIgnoreCase(condicion))) {
                    return "Debe estar regular en la correlativa " + correlativa.getMateriaRequerida();
                }
            }
        }

        return null;
    }

    private static Persona obtenerPersonaLogueada(String currentUsername) {
        if (currentUsername == null || currentUsername.isEmpty()) {
            return null;
        }

        return Persona.findFirst("mail = ? OR dni = ?", currentUsername, currentUsername);
    }

    private static Alumno obtenerAlumnoLogueado(String currentUsername) {
        Persona persona = obtenerPersonaLogueada(currentUsername);
        return persona != null ? Alumno.findById(persona.getDni()) : null;
    }
}