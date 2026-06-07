package com.is1.proyecto.logic;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.is1.proyecto.models.Alumno;
import com.is1.proyecto.models.Docente;
import com.is1.proyecto.models.Persona;
import com.is1.proyecto.models.Users;
import org.mindrot.jbcrypt.BCrypt;
import spark.ModelAndView;
import spark.Request;
import spark.Response;

import java.util.HashMap;
import java.util.Map;

public class UserLogic {
    public static final String ROLE_ADMIN = "admin";
    public static final String ROLE_TEACHER = "teacher";
    public static final String ROLE_STUDENT = "student";

    // Login
    public static ModelAndView login(Request req, Response res) {
        Map<String, Object> model = new HashMap<>();
        String errorMessage = req.queryParams("error");
        if (errorMessage != null && !errorMessage.isEmpty()) {
            model.put("errorMessage", errorMessage);
        }
        String successMessage = req.queryParams("message");
        if (successMessage != null && !successMessage.isEmpty()) {
            model.put("successMessage", successMessage);
        }
        return new ModelAndView(model, "login.mustache");
    }

    public static ModelAndView loginUser(Request req, Response res) {
        String username = req.queryParams("username");
        String plainTextPassword = req.queryParams("password");

        if (username == null || username.isEmpty()
                || plainTextPassword == null
                || plainTextPassword.isEmpty()) {

            res.redirect("/?error=Debe completar todos los campos");
            return null;
        }

        Users ac = Users.findFirst("name = ?", username);

        if (ac == null || !BCrypt.checkpw(plainTextPassword, ac.getString("password"))) {

            res.redirect("/?error=Usuario incorrecto");
            return null;
        }

        String role = resolveRole(ac);
        req.session(true).attribute("role", role);
        req.session().attribute("currentUserUsername", username);
        req.session().attribute("userId", ac.getId());
        req.session().attribute("loggedIn", true);

        if (ROLE_ADMIN.equals(role)) {
            res.redirect("/dashboard/admin");
        } else if (ROLE_TEACHER.equals(role)) {
            res.redirect("/dashboard/teacher");
        } else {
            res.redirect("/dashboard/student");
        }

        return null;
    }

    // Register user
    public static ModelAndView createUser(Request req, Response res) {
        Map<String, Object> model = new HashMap<>(); // Crea un mapa para pasar datos a la plantilla.

        // Obtener y añadir mensaje de éxito de los query parameters (ej.
        // ?message=Cuenta creada!)
        String successMessage = req.queryParams("message");
        if (successMessage != null && !successMessage.isEmpty()) {
            model.put("successMessage", successMessage);
        }

        // Obtener y añadir mensaje de error de los query parameters (ej. ?error=Campos
        // vacíos)
        String errorMessage = req.queryParams("error");
        if (errorMessage != null && !errorMessage.isEmpty()) {
            model.put("errorMessage", errorMessage);
        }

        return new ModelAndView(model, "user_form.mustache");
    }

    public static String registerNewUser(Request req, Response res) {
        String name = req.queryParams("name");
        String password = req.queryParams("password");

        if (name == null || name.isEmpty() || password == null || password.isEmpty()) {
            res.redirect("/user/create?error=Nombre y contraseña son requeridos.");
            return "";
        }

        if (Users.findFirst("name = ?", name) != null) {
            res.redirect("/user/create?error=El nombre de usuario ya existe.");
            return "";
        }

        try {
            long countUsers = Users.count();
            Users newUser = new Users();
            newUser.set("name", name);
            newUser.set("password", BCrypt.hashpw(password, BCrypt.gensalt()));
            newUser.insert();

            String message = countUsers == 0
                    ? "Cuenta de administrador '" + name + "' creada con éxito."
                    : "Cuenta de estudiante '" + name + "' creada con éxito.";

            res.redirect("/?message=" + java.net.URLEncoder.encode(message, "UTF-8"));
            return "";
        } catch (Exception e) {
            System.err.println("Error al registrar la cuenta: " + e.getMessage());
            res.redirect("/user/create?error=Error interno al crear la cuenta.");
            return "";
        }
    }

    // Dashboard
    public static ModelAndView dashboard(Request req, Response res) {
        Map<String, Object> model = new HashMap<>(); // Modelo para la plantilla del dashboard.

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

        // 2. Si el usuario está logueado, añade el nombre de usuario al modelo para la
        // plantilla.
        model.put("username", currentUsername);

        // 3. Renderiza la plantilla del dashboard con el nombre de usuario.
        return new ModelAndView(model, "dashboard.mustache");
    }

    public static ModelAndView logout(Request req, Response res) {
        // Invalida completamente la sesión del usuario.
        // Esto elimina todos los atributos guardados en la sesión y la marca como
        // inválida.
        // La cookie JSESSIONID en el navegador también será gestionada para
        // invalidarse.
        req.session().invalidate();

        System.out.println("DEBUG: Sesión cerrada. Redirigiendo a /login.");

        // Redirige al usuario a la página de login con un mensaje de éxito.
        res.redirect("/");

        return new ModelAndView(new HashMap<>(), "user_form.mustache"); // No pasa un modelo específico, solo el
        // formulario.
    }

    public static String addUser(Request req, Response res) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();

        res.type("application/json"); // Establece el tipo de contenido de la respuesta a JSON.

        // Obtiene los parámetros 'name' y 'password' de la solicitud.
        String name = req.queryParams("name");
        String password = req.queryParams("password");

        // --- Validaciones básicas ---
        if (name == null || name.isEmpty() || password == null || password.isEmpty()) {
            res.status(400); // Bad Request.
            return objectMapper.writeValueAsString(Map.of("error", "Nombre y contraseña son requeridos."));
        }

        try {
            // --- Creación y guardado del usuario usando el modelo ActiveJDBC ---
            Users newUser = new Users(); // Crea una nueva instancia de tu modelo User.
            // ¡ADVERTENCIA DE SEGURIDAD CRÍTICA!
            // En una aplicación real, las contraseñas DEBEN ser hasheadas (ej. con BCrypt)
            // ANTES de guardarse en la base de datos, NUNCA en texto plano.
            // (Nota: El código original tenía la contraseña en texto plano aquí.
            // Se recomienda usar `BCrypt.hashpw(password, BCrypt.gensalt())` como en la
            // ruta '/user/new').
            newUser.set("name", name); // Asigna el nombre al campo 'name'.
            newUser.set("password", password); // Asigna la contraseña al campo 'password'.
            newUser.insert(); // Guarda el nuevo usuario en la tabla 'users'.

            res.status(201); // Created.
            // Devuelve una respuesta JSON con el mensaje y el ID del nuevo usuario.
            return objectMapper.writeValueAsString(
                    Map.of("message", "Usuario '" + name + "' registrado con éxito.", "id", newUser.getId()));

        } catch (Exception e) {
            // Si ocurre cualquier error durante la operación de DB, se captura aquí.
            System.err.println("Error al registrar usuario: " + e.getMessage());
            e.printStackTrace(); // Imprime el stack trace para depuración.
            res.status(500); // Internal Server Error.
            return objectMapper
                    .writeValueAsString(Map.of("error", "Error interno al registrar usuario: " + e.getMessage()));
        }
    }

    // Definición de constantes para los roles de los tipos de usuarios
    private static String resolveRole(Users user) {
        Users firstUser = Users.findFirst("1=1 ORDER BY id ASC");
        if (firstUser != null && user.getId().equals(firstUser.getId())) {
            return ROLE_ADMIN;
        }

        String username = user.getString("name");
        Persona persona = Persona.findFirst("mail = ? OR dni = ?", username, username);
        if (persona != null) {
            if (Docente.findById(persona.getDni()) != null) {
                return ROLE_TEACHER;
            }
            if (Alumno.findById(persona.getDni()) != null) {
                return ROLE_STUDENT;
            }
        }
        return ROLE_STUDENT;
    }

    public static boolean isAuthenticated(Request req) {
        Boolean loggedIn = req.session().attribute("loggedIn");
        return loggedIn != null && loggedIn;
    }

    public static boolean isAdmin(Request req) {
        return ROLE_ADMIN.equals(req.session().attribute("role"));
    }
}