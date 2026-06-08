package com.is1.proyecto.logic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.javalite.activejdbc.LazyList;
import org.mindrot.jbcrypt.BCrypt;

import static com.is1.proyecto.logic.UserLogic.ROLE_TEACHER;
import static com.is1.proyecto.logic.UserLogic.isAdmin;
import static com.is1.proyecto.logic.UserLogic.isAuthenticated;
import com.is1.proyecto.models.AlumnoMateria;
import com.is1.proyecto.models.Docente;
import com.is1.proyecto.models.ExamenFinal;
import com.is1.proyecto.models.Materia;
import com.is1.proyecto.models.Persona;
import com.is1.proyecto.models.Users;

import spark.ModelAndView;
import spark.Request;
import spark.Response;
import static spark.Spark.halt;

public class TeacherLogic {
    public static final Pattern VALID_EMAIL_ADDRESS_REGEX = Pattern.compile(
            "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$",
            Pattern.CASE_INSENSITIVE);

    public static void middleware(Request req, Response res) {
        if (!isAuthenticated(req) || !isAdmin(req)) {
            res.redirect("/?error=No autorizado");
            halt(401);
        }
    }

    public static void middlewareTeacher(Request req, Response res) {
        if (!isAuthenticated(req) || !ROLE_TEACHER.equals(req.session().attribute("role"))) {
            res.redirect("/?error=No autorizado");
            halt(401);
        }
    }

    //teacher/new
    public static ModelAndView createTeacher(Request req, Response res) {
        // Intenta obtener el nombre de usuario y la bandera de login de la sesión.
        String currentUsername = req.session().attribute("currentUserUsername");
        Boolean loggedIn = req.session().attribute("loggedIn");

        // 1. Verificar si el usuario ha iniciado sesión.
        // Si no hay un nombre de usuario en la sesión, la bandera es nula o falsa,
        // significa que el usuario no está logueado o su sesión expiró.
        if (currentUsername == null || loggedIn == null || !loggedIn) {
            System.out.println("DEBUG: Acceso no autorizado a /dashboard. Redirigiendo a /login.");
            // Redirige al login con un mensaje de error.
            res.redirect("/login?error=Debes iniciar sesión para acceder a esta página.");
            return null; // Importante retornar null después de una redirección.
        }

        HashMap<String, String> model = new HashMap<>();
        return new ModelAndView(model, "teacher_form.mustache");
    }

    public static ModelAndView storeInDB(Request req, Response res) {
        // Intenta obtener el nombre de usuario y la bandera de login de la sesión.
        String currentUsername = req.session().attribute("currentUserUsername");
        Boolean loggedIn = req.session().attribute("loggedIn");

        // 1. Verificar si el usuario ha iniciado sesión.
        // Si no hay un nombre de usuario en la sesión, la bandera es nula o falsa,
        // significa que el usuario no está logueado o su sesión expiró.
        if (currentUsername == null || loggedIn == null || !loggedIn) {
            System.out.println("DEBUG: Acceso no autorizado a /dashboard. Redirigiendo a /login.");
            // Redirige al login con un mensaje de error.
            res.redirect("/login?error=Debes iniciar sesión para acceder a esta página.");
            return null; // Importante retornar null después de una redirección.
        }

        Map<String, Object> model = new HashMap<>();

        String dni = req.queryParams("dni");
        String nombre = req.queryParams("nombre");
        String apellido = req.queryParams("apellido");
        String mail = req.queryParams("mail");
        String titulo = req.queryParams("titulo");
        String legajo = req.queryParams("nro_legajo");
        String password = req.queryParams("password");

        if (dni == null || nombre == null || apellido == null || mail == null || titulo == null || legajo == null
                || password == null ||
                dni.isEmpty() || nombre.isEmpty() || apellido.isEmpty() || mail.isEmpty() || titulo.isEmpty()
                || legajo.isEmpty() || password.isEmpty()) {
            model.put("errorMessage", "Todos los campos son obligatorios.");
            return new ModelAndView(model, "teacher_form.mustache");
        }

        Matcher matcher = VALID_EMAIL_ADDRESS_REGEX.matcher(mail);
        if (!matcher.matches()) {
            model.put("errorMessage", "El formato del mail no es valido");
            return new ModelAndView(model, "teacher_form.mustache");
        }

        if (Persona.findById(dni) != null) {
            model.put("errorMessage", "Ya existe un usuario registrado con ese DNI.");
            return new ModelAndView(model, "teacher_form.mustache");
        }

        if (Persona.findFirst("mail = ?", mail) != null) {
            model.put("errorMessage", "Ya existe un usuario registrado con ese mail.");
            return new ModelAndView(model, "teacher_form.mustache");
        }

        if (Users.findFirst("name = ?", mail) != null) {
            model.put("errorMessage", "Ya existe un usuario con ese nombre de usuario.");
            return new ModelAndView(model, "teacher_form.mustache");
        }

        // Guardar en la BD
        try {
            Persona m = new Persona();
            m.setDni(dni);
            m.setNombre(nombre);
            m.setApellido(apellido);
            m.setMail(mail);
            m.insert();

            Docente t = new Docente();
            t.setDni(dni);
            t.setNroLegajo(legajo);
            t.setTitulo(titulo);
            t.insert();

            Users newUser = new Users();
            newUser.set("name", mail);
            newUser.set("password", BCrypt.hashpw(password, BCrypt.gensalt()));
            newUser.insert();
        } catch (Exception e) {
            e.printStackTrace();
            // res.status(500);

            model.put("errorMessage", "Error al guardar al nuevo usuario: " + e.getMessage());
            return new ModelAndView(model, "teacher_form.mustache");
        }

        // Redirigir a la lista
        res.redirect("/admin/teachers");
        return null;
    }

    //Teacher List
    public static ModelAndView listTeachers(Request req, Response res) {
        // Intenta obtener el nombre de usuario y la bandera de login de la sesión.
        String currentUsername = req.session().attribute("currentUserUsername");
        Boolean loggedIn = req.session().attribute("loggedIn");

        if (!isAuthenticated(req) || !isAdmin(req)) {
            res.redirect("/?error=No autorizado");
            return null;
        }

        // 1. Verificar si el usuario ha iniciado sesión.
        // Si no hay un nombre de usuario en la sesión, la bandera es nula o falsa,
        // significa que el usuario no está logueado o su sesión expiró.
        if (currentUsername == null || loggedIn == null || !loggedIn) {
            System.out.println("DEBUG: Acceso no autorizado a /dashboard. Redirigiendo a /login.");
            // Redirige al login con un mensaje de error.
            res.redirect("/login?error=Debes iniciar sesión para acceder a esta página.");
            return null; // Importante retornar null después de una redirección.
        }

        Map<String, Object> model = new HashMap<>();

        List<Map<String, Object>> teachersList = new ArrayList<>();

        LazyList<Docente> docentes = Docente.findAll();
        for (Docente d : docentes) {
            Persona p = Persona.findById(d.getDni());
            Map<String, Object> row = new HashMap<>();
            row.put("dni", d.getDni());
            row.put("nombre", p != null ? p.getNombre() : "");
            row.put("apellido", p != null ? p.getApellido() : "");
            row.put("mail", p != null ? p.getMail() : "");
            row.put("nro_legajo", d.getNroLegajo());
            row.put("titulo", d.getTitulo());
            teachersList.add(row);
        }

        model.put("teachers", teachersList);
        return new ModelAndView(model, "teacher_list.mustache");
    }

    //Dashboard
    public static ModelAndView dashboard(Request req, Response res) {
        if (!isAuthenticated(req)) {
            res.redirect("/?error=Acceso denegado.");
            return null;
        }
        String currentUsername = req.session().attribute("currentUserUsername");
        Map<String, Object> model = new HashMap<>();
        model.put("username", currentUsername);
        model.put("successMessage", req.queryParams("success"));
        model.put("errorMessage", req.queryParams("error"));

        Persona docente = Persona.findFirst("mail = ? OR dni = ?", currentUsername, currentUsername);
        if (docente != null) {
            LazyList<Materia> materias = Materia.find(
                    "cod_materia IN (SELECT cod_materia FROM docente_responsable_materia WHERE docente_dni = ?)",
                    docente.getDni());
            model.put("materias", materias);

            List<Map<String, Object>> examenes = new ArrayList<>();
            LazyList<ExamenFinal> finales = ExamenFinal.find(
                    "cod_materia IN (SELECT cod_materia FROM docente_responsable_materia WHERE docente_dni = ?)",
                    docente.getDni());
            for (ExamenFinal examen : finales) {
                Materia materia = Materia.findById(examen.getCodMateria());
                Map<String, Object> row = new HashMap<>();
                row.put("id_examen", examen.getIdExamen());
                row.put("fecha", examen.getFecha());
                row.put("cod_materia", examen.getCodMateria());
                row.put("nombre_materia", materia != null ? materia.getNombreMateria() : examen.getCodMateria());
                examenes.add(row);
            }
            model.put("examenes", examenes);
        }

        return new ModelAndView(model, "teacher_dashboard.mustache");
    }

    //Search
    public static ModelAndView searchTeachers(Request req, Response res) {
        // Texto escrito por el usuario en el buscador
        String q = req.queryParams("q");

        // Modelo para enviar datos a la vista
        Map<String, Object> model = new HashMap<>();

        // Lista donde se guardarán los resultados encontrados
        List<Map<String, Object>> teachersList = new ArrayList<>();

        // Obtiene todos los docentes registrados
        LazyList<Docente> docentes = Docente.findAll();

        // Recorre cada docente
        for (Docente docente : docentes) {

            // Busca la persona asociada al docente
            Persona persona = Persona.findById(docente.getDni());

            if (persona == null) {
                continue;
            }

            /*
             * Verifica si el texto buscado coincide con:
             * - DNI
             * - Nombre
             * - Apellido
             * - Mail
             */
            boolean coincide = docente.getDni().toLowerCase().contains(q.toLowerCase())
                    || persona.getNombre().toLowerCase().contains(q.toLowerCase())
                    || persona.getApellido().toLowerCase().contains(q.toLowerCase())
                    || persona.getMail().toLowerCase().contains(q.toLowerCase());

            // Si coincide, se agrega a la lista de resultados
            if (coincide) {

                Map<String, Object> t = new HashMap<>();

                t.put("dni", docente.getDni());
                t.put("nombre", persona.getNombre());
                t.put("apellido", persona.getApellido());
                t.put("mail", persona.getMail());
                t.put("titulo", docente.getTitulo());
                t.put("nro_legajo", docente.getNroLegajo());

                teachersList.add(t);
            }
        }

        /*
         * Si no encontró resultados,
         * muestra un mensaje de error
         * y vuelve a cargar la lista completa.
         */
        if (teachersList.isEmpty()) {

            model.put("errorMessage",
                    "No se encontraron docentes para: " + q);

            for (Docente docente : docentes) {

                Persona persona = Persona.findById(docente.getDni());

                Map<String, Object> t = new HashMap<>();

                t.put("dni", docente.getDni());
                t.put("nombre", persona.getNombre());
                t.put("apellido", persona.getApellido());
                t.put("mail", persona.getMail());
                t.put("titulo", docente.getTitulo());
                t.put("nro_legajo", docente.getNroLegajo());

                teachersList.add(t);
            }
        }

        // Envía la lista a la plantilla
        model.put("teachers", teachersList);

        return new ModelAndView(model, "teacher_list.mustache");
    }

    //Edit Teacher
    public static ModelAndView editTeacherForm(Request req, Response res) {
        /*
         * Obtiene el DNI enviado en la URL.
         * Ejemplo: /teacher/edit/12345678
         */
        String dni = req.params(":dni");

        // Busca el docente en la tabla docente.
        Docente docente = Docente.findById(dni);

        // Busca los datos personales asociad a ese mismo DNI.
        Persona persona = Persona.findById(dni);

        /*
         * Modelo que se enviará a la plantilla.
         * Aquí se guardan los datos que luego
         * aparecerán cargados en el formulario.
         */
        Map<String, Object> model = new HashMap<>();

        model.put("dni", persona.getDni());
        model.put("nombre", persona.getNombre());
        model.put("apellido", persona.getApellido());
        model.put("mail", persona.getMail());
        model.put("titulo", docente.getTitulo());
        model.put("nro_legajo", docente.getNroLegajo());

        // Abre la pantalla teacher_edit.mustache mostrando los datos actuales del
        // docente.
        return new ModelAndView(model, "teacher_edit.mustache");
    }

    public static ModelAndView editTeacher(Request req, Response res) {
        // Obtiene el DNI desde la URL
        String dni = req.params(":dni");

        // Obtiene los nuevos datos ingresados en el formulario
        String nombre = req.queryParams("nombre");
        String apellido = req.queryParams("apellido");
        String mail = req.queryParams("mail");
        String titulo = req.queryParams("titulo");

        // Busca los registros existentes
        Docente docente = Docente.findById(dni);
        Persona persona = Persona.findById(dni);

        // Actualiza los datos personales
        if (persona != null) {
            persona.setNombre(nombre);
            persona.setApellido(apellido);
            persona.setMail(mail);
            persona.saveIt();
        }

        // Actualiza los datos del docente
        if (docente != null) {
            docente.setTitulo(titulo);
            docente.saveIt();
        }

        // Vuelve al listado de docentes
        res.redirect("/admin/teachers");

        return null;
    }

    //Delete Teacher
    public static ModelAndView deleteTeacher(Request req, Response res) {
        /*
         * Obtiene el DNI que viene en la URL.
         *
         * Ejemplo:
         * /teacher/delete/12345678
         *
         * entonces dni = "12345678"
         */
        String dni = req.params(":dni");

        // Busca el docente en la tabla docente usando el DNI como clave primaria.

        Docente docente = Docente.findById(dni);

        // Si el docente existe, lo elimina.
        if (docente != null) {
            docente.delete();
        }

        // Busca la persona asociada al mismo DNI. Recordemos que los datos personales
        // están guardados en la tabla persona.
        Persona persona = Persona.findById(dni);

        // Si la persona existe, también la elimina.
        //Si esa persona tiene un usuario, tambien debe eliminarse.
        if (persona != null) {
            Users user = Users.findFirst("name = ?", persona.getMail());
            if (user != null) {
             user.delete();
            }

            persona.delete();
        }

        

        // Una vez eliminado, vuelve al listado de docentes.
        res.redirect("/admin/teachers");

        return null;
    }


    //GET /teacher/mis-estudiantes
    public static ModelAndView misEstudiantes(Request req, Response res) {
        if (!isAuthenticated(req) || !ROLE_TEACHER.equals(req.session().attribute("role"))) {
            res.redirect("/?error=No autorizado");
            return null;
        }

        String currentUsername = req.session().attribute("currentUserUsername");
        Persona docente = Persona.findFirst("mail = ? OR dni = ?", currentUsername, currentUsername);
        if (docente == null) {
            res.redirect("/?error=Docente no encontrado");
            return null;
        }

        Map<String, Object> model = new HashMap<>();
        List<Map<String, Object>> materiasList = new ArrayList<>();

        LazyList<Materia> materias = Materia.find(
                "cod_materia IN (SELECT cod_materia FROM docente_responsable_materia WHERE docente_dni = ?)",
                docente.getDni());
                 for (Materia materia : materias) {
            Map<String, Object> materiaRow = new HashMap<>();
            materiaRow.put("cod_materia", materia.getCodMateria());
            materiaRow.put("nombre_materia", materia.getNombreMateria());

            List<Map<String, Object>> estudiantes = new ArrayList<>();
            LazyList<AlumnoMateria> inscripciones = AlumnoMateria.where("cod_materia = ?", materia.getCodMateria());
            for (AlumnoMateria inscripcion : inscripciones) {
                Persona alumnoPersona = Persona.findById(inscripcion.getAlumnoDni());
                if (alumnoPersona != null) {
                    Map<String, Object> alumnoRow = new HashMap<>();
                    alumnoRow.put("dni", alumnoPersona.getDni());
                    alumnoRow.put("nombre", alumnoPersona.getNombre());
                    alumnoRow.put("apellido", alumnoPersona.getApellido());
                    alumnoRow.put("mail", alumnoPersona.getMail());
                    estudiantes.add(alumnoRow);
                }
            }
            materiaRow.put("estudiantes", estudiantes);
            materiasList.add(materiaRow);
        }

        model.put("materias", materiasList);
        return new ModelAndView(model, "mis_estudiantes.mustache");
    }

}
