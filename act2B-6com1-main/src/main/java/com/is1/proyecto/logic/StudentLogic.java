package com.is1.proyecto.logic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.javalite.activejdbc.Base;
import org.javalite.activejdbc.LazyList;
import org.mindrot.jbcrypt.BCrypt;

import static com.is1.proyecto.logic.UserLogic.ROLE_STUDENT;
import static com.is1.proyecto.logic.UserLogic.isAdmin;
import static com.is1.proyecto.logic.UserLogic.isAuthenticated;
import com.is1.proyecto.models.Alumno;
import com.is1.proyecto.models.AlumnoMateria;
import com.is1.proyecto.models.Materia;
import com.is1.proyecto.models.Persona;
import com.is1.proyecto.models.Users;

import spark.ModelAndView;
import spark.Request;
import spark.Response;
import static spark.Spark.halt;

public class StudentLogic {
    public static void middleware(Request req, Response res) {
        if (!isAuthenticated(req) || !ROLE_STUDENT.equals(req.session().attribute("role"))) {
            res.redirect("/?error=No autorizado");
            halt(401);
        }
         String currentUsername = req.session().attribute("currentUserUsername");
        if (!"/student/complete-profile".equals(req.pathInfo()) && !isStudentProfileComplete(currentUsername)) {
            res.redirect("/student/complete-profile");
            halt(302);
        }
    }

    

    // Dashboard
    public static ModelAndView dashboard(Request req, Response res) {
        if (!isAuthenticated(req) || !ROLE_STUDENT.equals(req.session().attribute("role"))) {
            res.redirect("/?error=Acceso denegado.");
            return null;
        }

        String currentUsername = req.session().attribute("currentUserUsername");
        Map<String, Object> model = new HashMap<>();
        model.put("username", currentUsername);
        model.put("successMessage", req.queryParams("success"));
        model.put("errorMessage", req.queryParams("error"));

        Persona persona = Persona.findFirst("mail = ? OR dni = ?", currentUsername, currentUsername);
        if (persona != null) {
            List<Map<String, Object>> materiasCursando = new ArrayList<>();
            LazyList<AlumnoMateria> inscripciones = AlumnoMateria.where("alumno_dni = ?", persona.getDni());

            for (AlumnoMateria inscripcion : inscripciones) {
                Materia materia = Materia.findById(inscripcion.getCodMateria());
                if (materia != null) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("cod_materia", materia.getCodMateria());
                    row.put("nombre_materia", materia.getNombreMateria());
                    row.put("anio_materia", materia.getAnioMateria());
                    row.put("condicion_final", inscripcion.getCondicionFinal() != null ? inscripcion.getCondicionFinal() : "-");
                    materiasCursando.add(row);
                }
            }

            model.put("materiasCursando", materiasCursando);
        }

        return new ModelAndView(model, "student_dashboard.mustache");
    }

    // Create Student
    public static ModelAndView createStudent(Request req, Response res) {
        String nombre = req.queryParams("nombre");
        String apellido = req.queryParams("apellido");
        String password = req.queryParams("password");
        String dni = req.queryParams("dni");
        String mail = req.queryParams("mail");
        String tipo_alumno = req.queryParams("tipo_alumno");

        Map<String, Object> model = new HashMap<>();
        if (nombre == null || apellido == null || password == null || dni == null || mail == null
                || tipo_alumno == null ||
                nombre.isEmpty() || apellido.isEmpty() || password.isEmpty() || dni.isEmpty() || mail.isEmpty()
                || tipo_alumno.isEmpty()) {
            model.put("errorMessage", "Todos los campos son obligatorios.");
            return new ModelAndView(model, "student_form.mustache");
        }

        if (Persona.findById(dni) != null) {
            model.put("errorMessage", "Ya existe un alumno con ese DNI.");
            return new ModelAndView(model, "student_form.mustache");
        }

        if (Persona.findFirst("mail = ?", mail) != null) {
            model.put("errorMessage", "Ya existe un alumno con ese mail.");
            return new ModelAndView(model, "student_form.mustache");
        }

        if (Users.findFirst("name = ?", mail) != null) {
            model.put("errorMessage", "Ya existe un usuario con ese nombre de usuario.");
            return new ModelAndView(model, "student_form.mustache");
        }
        try {
            Persona p = new Persona();
            p.setDni(dni);
            p.setNombre(nombre);
            p.setApellido(apellido);
            p.setMail(mail);

            if (!p.insert()) {
                throw new Exception("Error al guardar la persona: " + p.errors());
            }

            Base.exec("INSERT INTO alumno (dni, tipo_alumno) VALUES (?, ?::talumn)", dni, tipo_alumno);

            Users newUser = new Users();
            newUser.setName(mail); // Usamos el mail como nombre de usuario para login
            newUser.setPassword(BCrypt.hashpw(password, BCrypt.gensalt())); // Has
            newUser.insert();

        } catch (Exception e) {
            model.put("errorMessage", "Error al guardar al nuevo estudiante: " + e.getMessage());
            return new ModelAndView(model, "student_form.mustache");
        }
        res.redirect("/students");
        return null;
    }

    // List Students
    public static ModelAndView listStudents(Request req, Response res) {
        String currentUsername = req.session().attribute("currentUserUsername");
        Boolean loggedIn = req.session().attribute("loggedIn");

        if (!isAuthenticated(req) || !isAdmin(req)) {
            res.redirect("/?error=No autorizado");
            return null;
        }

        if (currentUsername == null || loggedIn == null || !loggedIn) {
            System.out.println("DEBUG: Acceso no autorizado a /dashboard. Redirigiendo a /login.");
            // Redirige al login con un mensaje de error.
            res.redirect("/login?error=Debes iniciar sesión para acceder a esta página.");
            return null; // Importante retornar null después de una redirección.
        }

        Map<String, Object> model = new HashMap<>();

        List<Map<String, Object>> studentsList = new ArrayList<>();
        LazyList<Alumno> alumnos = Alumno.findAll();
        for (Alumno a : alumnos) {
            Persona p = Persona.findById(a.getDni());
            Map<String, Object> row = new HashMap<>();
            row.put("dni", a.getDni());
            row.put("nombre", p != null ? p.getNombre() : "");
            row.put("apellido", p != null ? p.getApellido() : "");
            row.put("mail", p != null ? p.getMail() : "");
            row.put("tipo_alumno", a.getTipoAlumno());
            studentsList.add(row);
        }

        model.put("students", studentsList);
        return new ModelAndView(model, "student_list.mustache");
    }

    // Students complete profile
    public static ModelAndView completeProfileForm(Request req, Response res) {
        if (!isAuthenticated(req) || !ROLE_STUDENT.equals(req.session().attribute("role"))) {
            res.redirect("/?error=Acceso denegado.");
            return null;
        }

        String currentUsername = req.session().attribute("currentUserUsername");
        if (isStudentProfileComplete(currentUsername)) {
            res.redirect("/dashboard/student");
            return null;
        }

        Map<String, Object> model = new HashMap<>();
        model.put("mail", currentUsername);
        return new ModelAndView(model, "student_profile.mustache");
    }

    public static ModelAndView completeProfile(Request req, Response res) {
        if (!isAuthenticated(req) || !ROLE_STUDENT.equals(req.session().attribute("role"))) {
            res.redirect("/?error=Acceso denegado.");
            return null;
        }

        String currentUsername = req.session().attribute("currentUserUsername");
        if (isStudentProfileComplete(currentUsername)) {
            res.redirect("/dashboard/student");
            return null;
        }

        String nombre = req.queryParams("nombre");
        String apellido = req.queryParams("apellido");
        String dni = req.queryParams("dni");
        String tipoAlumno = req.queryParams("tipo_alumno");
        String mail = currentUsername;

        Map<String, Object> model = new HashMap<>();
        model.put("mail", mail);
        model.put("nombre", nombre);
        model.put("apellido", apellido);
        model.put("dni", dni);
        model.put("tipo_alumno", tipoAlumno);

        if (nombre == null || apellido == null || dni == null || tipoAlumno == null ||
                nombre.isEmpty() || apellido.isEmpty() || dni.isEmpty() || tipoAlumno.isEmpty()) {
            model.put("errorMessage", "Todos los campos son obligatorios.");
            return new ModelAndView(model, "student_profile.mustache");
        }

        if (Persona.findById(dni) != null) {
            model.put("errorMessage", "Ya existe un alumno con ese DNI.");
            return new ModelAndView(model, "student_profile.mustache");
        }

        if (Persona.findFirst("mail = ?", mail) != null) {
            model.put("errorMessage", "Ya existe un usuario con ese correo electrónico.");
            return new ModelAndView(model, "student_profile.mustache");
        }

        try {
            Persona p = new Persona();
            p.setDni(dni);
            p.setNombre(nombre);
            p.setApellido(apellido);
            p.setMail(mail);
            p.insert();

            Base.exec("INSERT INTO alumno (dni, tipo_alumno) VALUES (?, ?::talumn)", dni, tipoAlumno);

            res.redirect("/dashboard/student");
            return null;
        } catch (Exception e) {
            model.put("errorMessage", "Error al guardar su perfil: " + e.getMessage());
            return new ModelAndView(model, "student_profile.mustache");
        }
    }

    public static ModelAndView editStudentForm(Request req, Response res) {

        String dni = req.params(":dni");

        Persona persona = Persona.findById(dni);
        Alumno alumno = Alumno.findById(dni);

        if (persona == null || alumno == null) {
            res.redirect("/students");
            return null;
        }

        Map<String, Object> model = new HashMap<>();
        model.put("dni", persona.getDni());
        model.put("nombre", persona.getNombre());
        model.put("apellido", persona.getApellido());
        model.put("mail", persona.getMail());

        String tipo = alumno.getTipoAlumno();

        model.put("tipo_alumno", tipo);

        if ("Ingresante".equals(tipo)) {
            model.put("ingresanteSelected", true);
        }

        if ("Avanzado".equals(tipo)) {
            model.put("avanzadoSelected", true);
        }

        return new ModelAndView(model, "student_edit.mustache");
    }

    public static ModelAndView editStudent(Request req, Response res) {

        String dni = req.params(":dni");

        Persona persona = Persona.findById(dni);
        Alumno alumno = Alumno.findById(dni);

        if (persona == null || alumno == null) {
            res.redirect("/students");
            return null;
        }

        try {

            persona.setNombre(req.queryParams("nombre"));
            persona.setApellido(req.queryParams("apellido"));
            persona.setMail(req.queryParams("mail"));

            persona.saveIt();

            Base.exec(
                    "UPDATE alumno SET tipo_alumno = ?::talumn WHERE dni = ?",
                    req.queryParams("tipo_alumno"),
                    dni);

        } catch (Exception e) {

            Map<String, Object> model = new HashMap<>();

            model.put("dni", dni);
            model.put("nombre", req.queryParams("nombre"));
            model.put("apellido", req.queryParams("apellido"));
            model.put("mail", req.queryParams("mail"));
            model.put("tipo_alumno", req.queryParams("tipo_alumno"));
            model.put("errorMessage", e.getMessage());

            return new ModelAndView(model, "student_edit.mustache");
        }

        res.redirect("/students");
        return null;
    }

    // Delete
    public static ModelAndView delete(Request req, Response res) {
        String dni = req.params(":dni");

        Persona persona = Persona.findById(dni);
        if (persona != null) {
            Users user = Users.findFirst("name = ?", persona.getMail());
            if (user != null) {
                user.delete();
            }
            persona.delete();
        }

        res.redirect("/students");
        return null;
    }

    public static boolean isStudentProfileComplete(String username) {
        if (username == null || username.isEmpty()) {
            return false;
        }
        Persona persona = Persona.findFirst("mail = ?", username);
        return persona != null && Alumno.findById(persona.getDni()) != null;
    }
}
