package com.is1.proyecto; // Define el paquete de la aplicación, debe coincidir con la estructura de carpetas.

import com.is1.proyecto.config.DBConfigSingleton; // Importa los métodos estáticos principales de Spark (get, post, before, after, etc.).
import com.is1.proyecto.logic.AdminLogic;
import com.is1.proyecto.logic.StudentLogic;
import com.is1.proyecto.logic.TeacherLogic;
import com.is1.proyecto.logic.UserLogic;
import org.javalite.activejdbc.Base;
import spark.ModelAndView;
import spark.template.mustache.MustacheTemplateEngine; // Para crear mapas de datos (modelos para las plantillas).

import java.util.HashMap;

import static spark.Spark.*;

/**
 * Clase principal de la aplicación Spark.
 * Configura las rutas, filtros y el inicio del servidor web.
 */
public class App1 {
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
                halt(500, "{\"error\": \"Error interno del servidor: Fallo al conectar a la base de datos.\"}" +
                        e.getMessage());
            }
        });

        // --- Filtro 'after' para cerrar la conexión a la base de datos ---
        // Este filtro se ejecuta después de que cada solicitud HTTP ha sido procesada.
        afterAfter((req, res) -> {
            try {
                // Cierra la conexión a la base de datos para liberar recursos.
                Base.close();
            } catch (Exception e) {
                // Si ocurre un error al cerrar la conexión, se registra.
                System.err.println("Error al cerrar conexión con ActiveJDBC: " + e.getMessage());
            }
        });


        ///////////////////////////////////////////////////////////////////////////////
        //////////////////////            USERS            ////////////////////////////
        ///////////////////////////////////////////////////////////////////////////////

        // GET: Muestra el formulario de creación de cuenta.
        // Soporta la visualización de mensajes de éxito o error pasados como query
        // parameters.
        get("/user/create", UserLogic::createUser, new MustacheTemplateEngine()); // Especifica el motor de plantillas para esta ruta.

        // GET: Ruta para mostrar el dashboard (panel de control) del usuario.
        // Requiere que el usuario esté autenticado.
        get("/dashboard", UserLogic::dashboard, new MustacheTemplateEngine()); // Especifica el motor de plantillas para esta ruta.

        // GET: Ruta para cerrar la sesión del usuario.
        get("/logout", UserLogic::logout);

        // GET: Muestra el formulario de inicio de sesión (login).
        // Nota: Esta ruta debería ser capaz de leer también mensajes de error/éxito de
        // los query params
        // si se la usa como destino de redirecciones. (Tu código de /user/create ya lo
        // hace, aplicar similar).
        get("/", UserLogic::login, new MustacheTemplateEngine()); // Especifica el motor de plantillas para esta ruta.

        // GET: Ruta de alias para el formulario de creación de cuenta.
        // En una aplicación real, probablemente querrías unificar con '/user/create'
        // para evitar duplicidad.
        get("/user/new", UserLogic::createUser, new MustacheTemplateEngine()); // Especifica el motor de plantillas para esta ruta.
        // POST: Maneja el envío del formulario de creación de nueva cuenta.
        post("/user/new", UserLogic::registerNewUser);

        get("/login", UserLogic::login, new MustacheTemplateEngine());

        // POST: Maneja el envío del formulario de inicio de sesión.
        post("/login", UserLogic::loginUser);

        // POST: Endpoint para añadir usuarios (API que devuelve JSON, no HTML).
        // Advertencia: Esta ruta tiene un propósito diferente a las de formulario HTML.
        post("/add_users", UserLogic::addUser);

        ///////////////////////////////////////////////////////////////////////////////
        //////////////////////            ADMIN            ////////////////////////////
        ///////////////////////////////////////////////////////////////////////////////

        // RUTAS
        // Rutas dependientes de cada rol (ej. admin_dashboard.mustache,
        // teacher_dashboard.mustache, student_dashboard.mustache) y sus funcionalidades
        // asociadas.
        // Dashboard de admin
        get("/dashboard/admin", AdminLogic::adminDashboard, new MustacheTemplateEngine());


        ///////////////////////////////////////////////////////////////////////////////
        //////////////////////           STUDENTS          ////////////////////////////
        ///////////////////////////////////////////////////////////////////////////////

        // Dashboard de student
        get("/dashboard/student", StudentLogic::dashboard, new MustacheTemplateEngine());

        // PROTECCIONES
        // Protecciones del estilo "before" para rutas privilegiadas
        before("/student/*", StudentLogic::middleware);
        before("/students", StudentLogic::middleware);
        before("/student/complete-profile", StudentLogic::middleware);

        // CREACION Y MANJEO DE ESTUDIANTES
        get(
                "/student/new",
                (req, res) -> new ModelAndView(new HashMap<>(), "student_form.mustache"),
                new MustacheTemplateEngine()
        );

        post("/student/new", StudentLogic::createStudent, new MustacheTemplateEngine());

        // Listado de estudiantes
        get("/students", StudentLogic::listStudents, new MustacheTemplateEngine());

        //Creacion automatica de perfil de estudiante al registrarse, si el usuario registrado no tiene un perfil completo de estudiante, se le redirige a completar su perfil antes de acceder al dashboard
        get("/student/complete-profile", StudentLogic::completeProfileForm, new MustacheTemplateEngine());
        post("/student/complete-profile", StudentLogic::completeProfile, new MustacheTemplateEngine());

        //Eliminacion de estudiantes desde el dashboard del admin
        post("/student/delete/:dni", StudentLogic::delete);

        ///////////////////////////////////////////////////////////////////////////////
        //////////////////////           TEACHERS          ////////////////////////////
        ///////////////////////////////////////////////////////////////////////////////

        // PROTECCIONES
        // Protecciones del estilo "before" para rutas privilegiadas
        before("/teacher/*", TeacherLogic::middleware);
        before("/teachers", TeacherLogic::middleware);

        // Api
        get("/teacher/new", TeacherLogic::createTeacher, new MustacheTemplateEngine());

        post("/teacher/new", TeacherLogic::storeInDB, new MustacheTemplateEngine());

        // GET: Listado de profesores
        get("/teachers", TeacherLogic::listTeachers, new MustacheTemplateEngine());

        // Dashboard de teacher
        get("/dashboard/teacher", TeacherLogic::dashboard, new MustacheTemplateEngine());

        // BUSCAR DOCENTE
        get("/teachers/search", TeacherLogic::searchTeachers, new MustacheTemplateEngine());

        // MOSTRAR FORMULARIO DE EDICIÓN
        get("/teacher/edit/:dni", TeacherLogic::editTeacherForm, new MustacheTemplateEngine());

        // GUARDAR CAMBIOS DEL DOCENTE
        post("/teacher/edit/:dni", TeacherLogic::editTeacher);

        // ELIMINAR DOCENTE
        post("/teacher/delete/:dni", TeacherLogic::deleteTeacher);
    } // Fin del método main
} // Fin de la clase App
