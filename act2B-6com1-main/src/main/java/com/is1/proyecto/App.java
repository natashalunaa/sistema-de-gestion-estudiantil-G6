package com.is1.proyecto; // Define el paquete de la aplicación, debe coincidir con la estructura de carpetas.

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern; // Utilidad para serializar/deserializar objetos Java a/desde JSON.

import org.javalite.activejdbc.Base;
import org.javalite.activejdbc.LazyList;
import org.mindrot.jbcrypt.BCrypt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.is1.proyecto.config.DBConfigSingleton; // Importa los métodos estáticos principales de Spark (get, post, before, after, etc.).
import com.is1.proyecto.models.Alumno; // Clase central de ActiveJDBC para gestionar la conexión a la base de datos.
import com.is1.proyecto.models.Docente;
import com.is1.proyecto.models.Persona;
import com.is1.proyecto.models.Users;

import spark.ModelAndView;
import spark.Request; // Utilidad para hashear y verificar contraseñas de forma segura.
import static spark.Spark.after; // Representa un modelo de datos y el nombre de la vista a renderizar.
import static spark.Spark.before; // Motor de plantillas Mustache para Spark.
import static spark.Spark.get;
import static spark.Spark.halt;
import static spark.Spark.port;
import static spark.Spark.post;
import spark.template.mustache.MustacheTemplateEngine; // Para crear mapas de datos (modelos para las plantillas).

/**
 * Clase principal de la aplicación Spark.
 * Configura las rutas, filtros y el inicio del servidor web.
 */
public class App {

    // Instancia estática y final de ObjectMapper para la
    // serialización/deserialización JSON.
    // Se inicializa una sola vez para ser reutilizada en toda la aplicación.
    private static final ObjectMapper objectMapper = new ObjectMapper();
    public static final Pattern VALID_EMAIL_ADDRESS_REGEX = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$",
            Pattern.CASE_INSENSITIVE);

    // Definición de constantes para los roles de los tipos de usuarios
    public static final String ROLE_ADMIN = "admin";
    public static final String ROLE_TEACHER = "teacher";
    public static final String ROLE_STUDENT = "student";

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

    private static boolean isAuthenticated(Request req) {
        Boolean loggedIn = req.session().attribute("loggedIn");
        return loggedIn != null && loggedIn;
    }

    private static boolean isAdmin(Request req) {
        return ROLE_ADMIN.equals(req.session().attribute("role"));
    }

    private static boolean isValidEmail(String email) {
        return email != null && VALID_EMAIL_ADDRESS_REGEX.matcher(email).matches();
    }

    private static boolean isStudentProfileComplete(String username) {
        if (username == null || username.isEmpty()) {
            return false;
        }
        Persona persona = Persona.findFirst("mail = ?", username);
        return persona != null && Alumno.findById(persona.getDni()) != null;
    }

    private static void updateUserNameByEmail(String oldEmail, String newEmail) {
        if (oldEmail == null || newEmail == null || oldEmail.equals(newEmail)) {
            return;
        }
        Users user = Users.findFirst("name = ?", oldEmail);
        if (user != null) {
            user.set("name", newEmail);
            user.saveIt();
        }
    }

    /**
     * Método principal que se ejecuta al iniciar la aplicación.
     * Aquí se configuran todas las rutas y filtros de Spark.
     */
    public static void main(String[] args) {
        String appPort = System.getenv("APP_PORT");
        port(appPort != null && !appPort.isBlank() ? Integer.parseInt(appPort) : 8080); // Configura el puerto en el que
                                                                                        // la aplicación Spark escuchará
                                                                                        // las peticiones
        // port(85);

        // Obtener la instancia única del singleton de configuración de la base de
        // datos.
        DBConfigSingleton dbConfig = DBConfigSingleton.getInstance();

        // --- Filtro 'before' para gestionar la conexión a la base de datos ---
        // Este filtro se ejecuta antes de cada solicitud HTTP.
        before((req, res) -> {
            try {
                // Abre una conexión a la base de datos utilizando las credenciales del
                // singleton.
                Base.open(dbConfig.getDriver(), dbConfig.getDbUrl(), dbConfig.getUser(), dbConfig.getPass());
                if ("org.sqlite.JDBC".equals(dbConfig.getDriver())) {
                    Base.exec("PRAGMA foreign_keys = ON;"); // para activar la foreign key solo en SQLite
                }
                System.out.println(req.url());

            } catch (Exception e) {
                // Si ocurre un error al abrir la conexión, se registra y se detiene la
                // solicitud
                // con un código de estado 500 (Internal Server Error) y un mensaje JSON.
                System.err.println("Error al abrir conexión con ActiveJDBC: " + e.getMessage());
                halt(500, "{\"error\": \"Error interno del servidor: Fallo al conectar a la base de datos.\"}"
                        + e.getMessage());
            }
        });

        // --- Filtro 'after' para cerrar la conexión a la base de datos ---
        // Este filtro se ejecuta después de que cada solicitud HTTP ha sido procesada.
        after((req, res) -> {
            try {
                // Cierra la conexión a la base de datos para liberar recursos.
                Base.close();
            } catch (Exception e) {
                // Si ocurre un error al cerrar la conexión, se registra.
                System.err.println("Error al cerrar conexión con ActiveJDBC: " + e.getMessage());
            }
        });

        // --- Rutas GET para renderizar formularios y páginas HTML ---

        // GET: Muestra el formulario de creación de cuenta.
        // Soporta la visualización de mensajes de éxito o error pasados como query
        // parameters.
        get("/user/create", (req, res) -> {
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

            // Renderiza la plantilla 'user_form.mustache' con los datos del modelo.
            return new ModelAndView(model, "user_form.mustache");
        }, new MustacheTemplateEngine()); // Especifica el motor de plantillas para esta ruta.

        // GET: Ruta para mostrar el dashboard (panel de control) del usuario.
        // Requiere que el usuario esté autenticado.
        get("/dashboard", (req, res) -> {
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
        }, new MustacheTemplateEngine()); // Especifica el motor de plantillas para esta ruta.

        // GET: Ruta para cerrar la sesión del usuario.
        get("/logout", (req, res) -> {
            // Invalida completamente la sesión del usuario.
            // Esto elimina todos los atributos guardados en la sesión y la marca como
            // inválida.
            // La cookie JSESSIONID en el navegador también será gestionada para
            // invalidarse.
            req.session().invalidate();

            System.out.println("DEBUG: Sesión cerrada. Redirigiendo a /login.");

            // Redirige al usuario a la página de login con un mensaje de éxito.
            res.redirect("/");

            return null; // Importante retornar null después de una redirección.
        });

        // GET: Muestra el formulario de inicio de sesión (login).
        // Nota: Esta ruta debería ser capaz de leer también mensajes de error/éxito de
        // los query params
        // si se la usa como destino de redirecciones. (Tu código de /user/create ya lo
        // hace, aplicar similar).
        get("/", (req, res) -> {
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
        }, new MustacheTemplateEngine()); // Especifica el motor de plantillas para esta ruta.

        // GET: Ruta de alias para el formulario de creación de cuenta.
        // En una aplicación real, probablemente querrías unificar con '/user/create'
        // para evitar duplicidad.
        get("/user/new", (req, res) -> {
            return new ModelAndView(new HashMap<>(), "user_form.mustache"); // No pasa un modelo específico, solo el
                                                                            // formulario.
        }, new MustacheTemplateEngine()); // Especifica el motor de plantillas para esta ruta.

        // --- Rutas POST para manejar envíos de formularios y APIs ---

        // POST: Maneja el envío del formulario de creación de nueva cuenta.
        post("/user/new", (req, res) -> {
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

                //El primer usuario sera automaticamente un administrador, el resto seran estudiantes por defecto
                if (countUsers == 0) {
                    String message = "Cuenta de administrador '" + name + "' creada con éxito.";
                    res.redirect("/?message=" + java.net.URLEncoder.encode(message, "UTF-8"));
                    return "";
                }

                if (!isValidEmail(name)) {
                    newUser.delete();
                    res.redirect("/user/create?error=El usuario debe ser un correo electrónico válido.");
                    return "";
                }

                req.session(true).attribute("currentUserUsername", name);
                req.session().attribute("userId", newUser.getId());
                req.session().attribute("role", ROLE_STUDENT);
                req.session().attribute("loggedIn", true);

                res.redirect("/student/complete-profile");
                return "";
            } catch (Exception e) {
                System.err.println("Error al registrar la cuenta: " + e.getMessage());
                res.redirect("/user/create?error=Error interno al crear la cuenta.");
                return "";
            }
        });

        get("/login", (req, res) -> {
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
        }, new MustacheTemplateEngine());

        // POST: Maneja el envío del formulario de inicio de sesión.
        post("/login", (req, res) -> {
            String username = req.queryParams("username");
            String plainTextPassword = req.queryParams("password");

            if (username == null || username.isEmpty() || plainTextPassword == null || plainTextPassword.isEmpty()) {
                res.redirect("/?error=El nombre de usuario y la contraseña son requeridos.");
                return null;
            }

            Users ac = Users.findFirst("name = ?", username);
            if (ac == null || !BCrypt.checkpw(plainTextPassword, ac.getString("password"))) {
                res.redirect("/?error=Usuario o contraseña incorrectos.");
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
                if (!isStudentProfileComplete(username)) {
                    res.redirect("/student/complete-profile");
                } else {
                    res.redirect("/dashboard/student");
                }
            }
            return null;
        });

        // POST: Endpoint para añadir usuarios (API que devuelve JSON, no HTML).
        // Advertencia: Esta ruta tiene un propósito diferente a las de formulario HTML.
        post("/add_users", (req, res) -> {
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
        });

        // Api
        get("/teacher/new", (req, res) -> {
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

            HashMap<String, String> model = new HashMap<>();
            return new ModelAndView(model, "teacher_form.mustache");
        }, new MustacheTemplateEngine());

        post("/teacher/new", (req, res) -> {
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
            res.redirect("/teachers");
            return null;
        }, new MustacheTemplateEngine());

        // GET: Listado de profesores
        get("/teachers", (req, res) -> {
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
        }, new MustacheTemplateEngine());

        // RUTAS
        // Rutas dependientes de cada rol (ej. admin_dashboard.mustache,
        // teacher_dashboard.mustache, student_dashboard.mustache) y sus funcionalidades
        // asociadas.
        // Dashboard de admin
        get("/dashboard/admin", (req, res) -> {
            if (!isAuthenticated(req) || !isAdmin(req)) {
                res.redirect("/?error=Acceso denegado.");
                return null;
            }
            Map<String, Object> model = new HashMap<>();
            model.put("username", req.session().attribute("currentUserUsername"));
            return new ModelAndView(model, "admin_dashboard.mustache");
        }, new MustacheTemplateEngine());

        // Dashboard de teacher
        get("/dashboard/teacher", (req, res) -> {
            if (!isAuthenticated(req)) {
                res.redirect("/?error=Acceso denegado.");
                return null;
            }
            Map<String, Object> model = new HashMap<>();
            model.put("username", req.session().attribute("currentUserUsername"));
            return new ModelAndView(model, "teacher_dashboard.mustache");
        }, new MustacheTemplateEngine());

        // Dashboard de student
       get("/dashboard/student", (req, res) -> {
            if (!isAuthenticated(req)) {
                res.redirect("/?error=Acceso denegado.");
                return null;
            }

            String currentUsername = req.session().attribute("currentUserUsername");
            if (ROLE_STUDENT.equals(req.session().attribute("role")) && !isStudentProfileComplete(currentUsername)) {
                res.redirect("/student/complete-profile");
                return null;
            }

            Map<String, Object> model = new HashMap<>();
            model.put("username", currentUsername);
            return new ModelAndView(model, "student_dashboard.mustache");
        }, new MustacheTemplateEngine());

        // PROTECCIONES
        // Protecciones del estilo "before" para rutas privilegiadas

           before("/teachers/search", (req, res) -> {
            if (!isAuthenticated(req) || !isAdmin(req)) {
                res.redirect("/?error=No autorizado");
                halt(401);
            }
        });

         before("/teacher/*", (req, res) -> {
            if (!isAuthenticated(req) || !isAdmin(req)) {
                res.redirect("/?error=No autorizado");
                halt(401);
            }
        });
        before("/teachers", (req, res) -> {
            if (!isAuthenticated(req) || !isAdmin(req)) {
                res.redirect("/?error=No autorizado");
                halt(401);
            }
        });
        before("/student/new", (req, res) -> {
            if (!isAuthenticated(req) || !isAdmin(req)) {
                res.redirect("/?error=No autorizado");
                halt(401);
            }
        });
        before("/student/edit/*", (req, res) -> {
            if (!isAuthenticated(req) || !isAdmin(req)) {
                res.redirect("/?error=No autorizado");
                halt(401);
            }
        });
        before("/student/delete/*", (req, res) -> {
            if (!isAuthenticated(req) || !isAdmin(req)) {
                res.redirect("/?error=No autorizado");
                halt(401);
            }
        });
        before("/students", (req, res) -> {
            if (!isAuthenticated(req) || !isAdmin(req)) {
                res.redirect("/?error=No autorizado");
                halt(401);
            }
        });

         before("/dashboard/admin", (req, res) -> {
            if (!isAuthenticated(req) || !isAdmin(req)) {
                res.redirect("/?error=No autorizado");
                halt(401);
            }
        });

            before("/dashboard/teacher", (req, res) -> {
            if (!isAuthenticated(req) || !ROLE_TEACHER.equals(req.session().attribute("role"))) {
                res.redirect("/?error=No autorizado");
                halt(401);
            }
        });

             before("/dashboard/student", (req, res) -> {
            if (!isAuthenticated(req) || !ROLE_STUDENT.equals(req.session().attribute("role"))) {
                res.redirect("/?error=No autorizado");
                halt(401);
            }
        });

              before("/student/complete-profile", (req, res) -> {
            if (!isAuthenticated(req) || !ROLE_STUDENT.equals(req.session().attribute("role"))) {
                res.redirect("/?error=No autorizado");
                halt(401);
            }
        });

      

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // CREACION Y MANJEO DE ESTUDIANTES

        //Creacion automatica de perfil de estudiante al registrarse, si el usuario registrado no tiene un perfil completo de estudiante, se le redirige a completar su perfil antes de acceder al dashboard
            get("/student/complete-profile", (req, res) -> {
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
        }, new MustacheTemplateEngine());


        post("/student/complete-profile", (req, res) -> {
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
        }, new MustacheTemplateEngine());



        //Creacion de un estudiante desde el dashboard de admin, para casos donde el admin quiera crear un estudiante sin que este tenga una cuenta de usuario (ej. para cargar estudiantes antiguos o similares)
        get("/student/new", (req, res) -> {
            return new ModelAndView(new HashMap<>(), "student_form.mustache");
        }, new MustacheTemplateEngine());

        post("/student/new", (req, res) -> {
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
        }, new MustacheTemplateEngine());

        // Listado de estudiantes
        get("/students", (req, res) -> {
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
        }, new MustacheTemplateEngine());


        //Edicion de estudiantes desde el dashboard del admin
        get("/student/edit/:dni", (req, res) -> {
            String dni = req.params(":dni");
            Alumno alumno = Alumno.findById(dni);
            Persona persona = Persona.findById(dni);

            Map<String, Object> model = new HashMap<>();
            if (alumno == null || persona == null) {
                model.put("errorMessage", "Alumno no encontrado.");
                return new ModelAndView(model, "student_list.mustache");
            }

            model.put("dni", persona.getDni());
            model.put("nombre", persona.getNombre());
            model.put("apellido", persona.getApellido());
            model.put("mail", persona.getMail());
            model.put("tipo_alumno", alumno.getTipoAlumno());
            model.put("ingresanteSelected", "Ingresante".equals(alumno.getTipoAlumno()));
            model.put("avanzadoSelected", "Avanzado".equals(alumno.getTipoAlumno()));

            return new ModelAndView(model, "student_edit.mustache");
        }, new MustacheTemplateEngine());

        post("/student/edit/:dni", (req, res) -> {
            String dni = req.params(":dni");
            String nombre = req.queryParams("nombre");
            String apellido = req.queryParams("apellido");
            String mail = req.queryParams("mail");
            String tipoAlumno = req.queryParams("tipo_alumno");

            Alumno alumno = Alumno.findById(dni);
            Persona persona = Persona.findById(dni);

            Map<String, Object> model = new HashMap<>();
            model.put("dni", dni);
            model.put("nombre", nombre);
            model.put("apellido", apellido);
            model.put("mail", mail);
            model.put("tipo_alumno", tipoAlumno);
            model.put("ingresanteSelected", "Ingresante".equals(tipoAlumno));
            model.put("avanzadoSelected", "Avanzado".equals(tipoAlumno));

            if (alumno == null || persona == null) {
                model.put("errorMessage", "Alumno no encontrado.");
                return new ModelAndView(model, "student_edit.mustache");
            }

            if (nombre == null || apellido == null || mail == null || tipoAlumno == null ||
                    nombre.isEmpty() || apellido.isEmpty() || mail.isEmpty() || tipoAlumno.isEmpty()) {
                model.put("errorMessage", "Todos los campos son obligatorios.");
                return new ModelAndView(model, "student_edit.mustache");
            }

            if (!isValidEmail(mail)) {
                model.put("errorMessage", "El formato del mail no es válido.");
                return new ModelAndView(model, "student_edit.mustache");
            }

            if (!mail.equals(persona.getMail())) {
                if (Persona.findFirst("mail = ?", mail) != null) {
                    model.put("errorMessage", "Ya existe un alumno con ese mail.");
                    return new ModelAndView(model, "student_edit.mustache");
                }
                if (Users.findFirst("name = ?", mail) != null) {
                    model.put("errorMessage", "Ya existe un usuario con ese nombre de usuario.");
                    return new ModelAndView(model, "student_edit.mustache");
                }
            }

            String oldMail = persona.getMail();
            persona.setNombre(nombre);
            persona.setApellido(apellido);
            persona.setMail(mail);
            persona.saveIt();

            updateUserNameByEmail(oldMail, mail);

            alumno.setTipoAlumno(tipoAlumno);
            alumno.saveIt();

            res.redirect("/students");
            return null;
        });


        //Eliminacion de estudiantes desde el dashboard del admin
         post("/student/delete/:dni", (req, res) -> {
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
        });

        /////////////////////////////////////////////////////////////////////////////////////////////////////////
        // BUSCAR DOCENTE
        get("/teachers/search", (req, res) -> {

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

        }, new MustacheTemplateEngine());

        // MOSTRAR FORMULARIO DE EDICIÓN
        get("/teacher/edit/:dni", (req, res) -> {

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
            if (persona == null || docente == null) {
                model.put("errorMessage", "Docente no encontrado.");
                return new ModelAndView(model, "teacher_list.mustache");
            }

            model.put("dni", persona.getDni());
            model.put("nombre", persona.getNombre());
            model.put("apellido", persona.getApellido());
            model.put("mail", persona.getMail());
            model.put("titulo", docente.getTitulo());
            model.put("nro_legajo", docente.getNroLegajo());

            // Abre la pantalla teacher_edit.mustache mostrando los datos actuales del
            // docente.
            return new ModelAndView(model, "teacher_edit.mustache");

        }, new MustacheTemplateEngine());

        // GUARDAR CAMBIOS DEL DOCENTE
        post("/teacher/edit/:dni", (req, res) -> {

            // Obtiene el DNI desde la URL
            String dni = req.params(":dni");

            // Obtiene los nuevos datos ingresados en el formulario
            String nombre = req.queryParams("nombre");
            String apellido = req.queryParams("apellido");
            String mail = req.queryParams("mail");
            String titulo = req.queryParams("titulo");
            String nroLegajo = req.queryParams("nro_legajo");


            // Busca los registros existentes
            Docente docente = Docente.findById(dni);
            Persona persona = Persona.findById(dni);

            Map<String, Object> model = new HashMap<>();
            model.put("dni", dni);
            model.put("nombre", nombre);
            model.put("apellido", apellido);
            model.put("mail", mail);
            model.put("titulo", titulo);
            model.put("nro_legajo", nroLegajo);

             if (persona == null || docente == null) {
                model.put("errorMessage", "Docente no encontrado.");
                return new ModelAndView(model, "teacher_edit.mustache");
            }

             Matcher matcher = VALID_EMAIL_ADDRESS_REGEX.matcher(mail);

            // Actualiza los datos personales
            if (docente != null && persona != null){

                if (nombre == null || apellido == null || mail == null || titulo == null || nroLegajo == null ||
                        nombre.isEmpty() || apellido.isEmpty() || mail.isEmpty() || titulo.isEmpty() || nroLegajo.isEmpty()) {
                    model.put("errorMessage", "Todos los campos son obligatorios.");
                    return new ModelAndView(model, "teacher_edit.mustache");
                }

               if (!isValidEmail(mail)) {
                model.put("errorMessage", "El formato del mail no es válido.");
                return new ModelAndView(model, "teacher_edit.mustache");
                }

                if (!mail.equals(persona.getMail())) {
                 if (Persona.findFirst("mail = ?", mail) != null) {
                    model.put("errorMessage", "Ya existe un usuario registrado con ese mail.");
                    return new ModelAndView(model, "teacher_edit.mustache");
                    }
                if (Users.findFirst("name = ?", mail) != null) {
                    model.put("errorMessage", "Ya existe un usuario con ese nombre de usuario.");
                    return new ModelAndView(model, "teacher_edit.mustache");
                }
            }
                String oldMail = persona.getMail();
                persona.setNombre(nombre);
                persona.setApellido(apellido);
                persona.setMail(mail);
                persona.saveIt();
                updateUserNameByEmail(oldMail, mail);

               

                docente.setTitulo(titulo);
                docente.setNroLegajo(nroLegajo);
                docente.saveIt();
            }

            // Vuelve al listado de docentes
                res.redirect("/teachers");

            return null;

        });

        // ELIMINAR DOCENTE
        post("/teacher/delete/:dni", (req, res) -> {

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
            if (persona != null) {
                Users user = Users.findFirst("name = ?", persona.getMail());
                if (user != null) {
                    user.delete();
                }
                persona.delete();
            }

            // Una vez eliminado, vuelve al listado de docentes.
            res.redirect("/teachers");

            return null;

        });

        // Solo para regenerar la db
        /*
         * get("/impl_db_dev", (req, res) -> {
         * String sqlPath = "src/main/resources/scheme.sql";
         * String sql = new String(Files.readAllBytes(Paths.get(sqlPath)));
         * 
         * try {
         * for (String command : sql.split(";")) {
         * if (!command.trim().isEmpty()) {
         * Base.exec(command.trim());
         * }
         * }
         * System.out.println("Base de datos inicializada correctamente.");
         * return "Base de datos recreada";
         * } catch (Exception e) {
         * e.printStackTrace();
         * res.status(500);
         * return "Error al recrear la base de datos: " + e.getMessage();
         * }
         * });
         */

    } // Fin del método main
} // Fin de la clase App