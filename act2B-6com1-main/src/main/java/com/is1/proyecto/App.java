package com.is1.proyecto; // Define el paquete de la aplicación, debe coincidir con la estructura de carpetas.

import java.util.HashMap; // Importa los métodos estáticos principales de Spark (get, post, before, after, etc.).

import org.javalite.activejdbc.Base;

import com.is1.proyecto.config.DBConfigSingleton;
import com.is1.proyecto.logic.AdminLogic;
import com.is1.proyecto.logic.CarreraLogic;
import com.is1.proyecto.logic.CatedraLogic;
import com.is1.proyecto.logic.CorrelatividadLogic;
import com.is1.proyecto.logic.ExamenFinalLogic;
import com.is1.proyecto.logic.InscripcionLogic;
import com.is1.proyecto.logic.MateriaLogic;
import com.is1.proyecto.logic.StudentLogic;
import com.is1.proyecto.logic.TeacherLogic;
import com.is1.proyecto.logic.UserLogic;
import com.is1.proyecto.models.Correlatividad; // Para crear mapas de datos (modelos para las plantillas).

import spark.ModelAndView;
import static spark.Spark.afterAfter;
import static spark.Spark.before;
import static spark.Spark.get;
import static spark.Spark.halt;
import static spark.Spark.port;
import static spark.Spark.post;
import spark.template.mustache.MustacheTemplateEngine;

/**
 * Clase principal de la aplicación Spark. Configura las rutas, filtros y el
 * inicio del servidor web.
 */
public class App {

    /**
     * Método principal que se ejecuta al iniciar la aplicación. Aquí se
     * configuran todas las rutas y filtros de Spark.
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
                try {
                    // Si la tabla de tipos de correlatividad está vacía, cargamos los datos necesarios
                    if (Correlatividad.count() == 0) {
                        Base.exec("INSERT INTO correlatividad (id_correlatividad, correl) VALUES (1, 'Aprobado')");
                        Base.exec("INSERT INTO correlatividad (id_correlatividad, correl) VALUES (2, 'Regular')");
                        System.out.println("Datos iniciales de correlatividad cargados correctamente.");
                    }
                } catch (Exception e) {
                    System.err.println("No se pudieron cargar los datos iniciales: " + e.getMessage());
                }
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
        ////////////////////// USERS ////////////////////////////
        ///////////////////////////////////////////////////////////////////////////////

        // GET: Muestra el formulario de creación de cuenta.
        // Soporta la visualización de mensajes de éxito o error pasados como query
        // parameters.
        get("/user/create", UserLogic::createUser, new MustacheTemplateEngine()); // Especifica el motor de plantillas
        // para esta ruta.

        // GET: Ruta para mostrar el dashboard (panel de control) del usuario.
        // Requiere que el usuario esté autenticado.
        get("/dashboard", UserLogic::dashboard, new MustacheTemplateEngine()); // Especifica el motor de plantillas para
        // esta ruta.

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
        get("/user/new", UserLogic::createUser, new MustacheTemplateEngine()); // Especifica el motor de plantillas para
        // esta ruta.
        // POST: Maneja el envío del formulario de creación de nueva cuenta.
        post("/user/new", UserLogic::registerNewUser);

        get("/login", UserLogic::login, new MustacheTemplateEngine());

        // POST: Maneja el envío del formulario de inicio de sesión.
        post("/login", UserLogic::loginUser);

        // POST: Endpoint para añadir usuarios (API que devuelve JSON, no HTML).
        // Advertencia: Esta ruta tiene un propósito diferente a las de formulario HTML.
        post("/add_users", UserLogic::addUser);

        ///////////////////////////////////////////////////////////////////////////////
        ////////////////////// ADMIN ////////////////////////////
        ///////////////////////////////////////////////////////////////////////////////

        // RUTAS
        // Rutas dependientes de cada rol (ej. admin_dashboard.mustache,
        // teacher_dashboard.mustache, student_dashboard.mustache) y sus funcionalidades
        // asociadas.
        // Dashboard de admin
        get("/dashboard/admin", AdminLogic::adminDashboard, new MustacheTemplateEngine());

        ///////////////////////////////////////////////////////////////////////////////
        ////////////////////// STUDENTS ////////////////////////////
        ///////////////////////////////////////////////////////////////////////////////

        // Dashboard de student
        get("/dashboard/student", StudentLogic::dashboard, new MustacheTemplateEngine());

        // PROTECCIONES
        // Protecciones del estilo "before" para rutas privilegiadas
        before("/student/complete-profile", StudentLogic::middleware);
        before("/dashboard/student", StudentLogic::middleware);
        before("admin/student/edit/*", AdminLogic::middleware);
        before("admin/student/delete/*", AdminLogic::middleware);

        // CREACION Y MANJEO DE ESTUDIANTES
        get(
                "/student/new",
                (req, res) -> new ModelAndView(new HashMap<>(), "student_form.mustache"),
                new MustacheTemplateEngine());

        post("/student/new", StudentLogic::createStudent, new MustacheTemplateEngine());

        // Listado de estudiantes
        get("/students", StudentLogic::listStudents, new MustacheTemplateEngine());

        // Creacion automatica de perfil de estudiante al registrarse, si el usuario
        // registrado no tiene un perfil completo de estudiante, se le redirige a
        // completar su perfil antes de acceder al dashboard
        get("/student/complete-profile", StudentLogic::completeProfileForm, new MustacheTemplateEngine());
        post("/student/complete-profile", StudentLogic::completeProfile, new MustacheTemplateEngine());

        // Editar alumno
        get("admin/student/edit/:dni", StudentLogic::editStudentForm, new MustacheTemplateEngine());

        post("admim/student/edit/:dni", StudentLogic::editStudent, new MustacheTemplateEngine());

        // Eliminacion de estudiantes desde el dashboard del admin
        post("admin/student/delete/:dni", StudentLogic::delete);

        ///////////////////////////////////////////////////////////////////////////////
        ////////////////////// TEACHERS ////////////////////////////
        ///////////////////////////////////////////////////////////////////////////////

        // PROTECCIONES
        // Protecciones del estilo "before" para rutas privilegiadas
        before("admin/teacher/*", TeacherLogic::middleware);
        before("admin/teachers", TeacherLogic::middleware);
        before("/teacher/mis-estudiantes",TeacherLogic::middlewareTeacher);
        before("/dashboard/teacher", TeacherLogic::middlewareTeacher);

        // Api
        get("admin/teacher/new", TeacherLogic::createTeacher, new MustacheTemplateEngine());

        post("admin/teacher/new", TeacherLogic::storeInDB, new MustacheTemplateEngine());

        // GET: Listado de profesores
        get("admin/teachers", TeacherLogic::listTeachers, new MustacheTemplateEngine());

        // Dashboard de teacher
        get("/dashboard/teacher", TeacherLogic::dashboard, new MustacheTemplateEngine());

        // BUSCAR DOCENTE
        get("admin/teachers/search", TeacherLogic::searchTeachers, new MustacheTemplateEngine());

        // MOSTRAR FORMULARIO DE EDICIÓN
        get("admin/teacher/edit/:dni", TeacherLogic::editTeacherForm, new MustacheTemplateEngine());

        // GUARDAR CAMBIOS DEL DOCENTE
        post("admin/teacher/edit/:dni", TeacherLogic::editTeacher);

        // ELIMINAR DOCENTE
        post("admin/teacher/delete/:dni", TeacherLogic::deleteTeacher);

        //Ver los estudiantes asociados a un docente
        get("/teacher/mis-estudiantes", TeacherLogic::misEstudiantes, new MustacheTemplateEngine());

        
        ///////////////////////////////////////////////////////////////////////////////
        ////////////////////// MATERIAS //////////////////////////
        ///////////////////////////////////////////////////////////////////////////////

        //PROTECCIONES
        before("/admin/materia/*", MateriaLogic::middleware);
        before("/admin/materias", MateriaLogic::middleware);

        // FORMULARIO Y GUARDADO
        get("/admin/materia/new", MateriaLogic::createMateriaForm, new MustacheTemplateEngine());
        post("/admin/materia/new", MateriaLogic::storeInDB, new MustacheTemplateEngine());

        // LISTADO GENERAL
        get("/admin/materias", MateriaLogic::listMaterias, new MustacheTemplateEngine());

        // ELIMINAR, MOSTRAR EDICIÓN Y GUARDAR EDICIÓN
        post("/admin/materia/delete/:cod_materia", MateriaLogic::deleteMateria);
        get("/admin/materia/edit/:cod_materia", MateriaLogic::editMateriaForm, new MustacheTemplateEngine());
        post("/admin/materia/edit/:cod_materia", MateriaLogic::editMateria);

        ///////////////////////////////////////////////////////////////////////////////
        ////////////////////// CATEDRAS ////////////////////////////
        ///////////////////////////////////////////////////////////////////////////////

        // PROTECCIONES (Filtros antes de entrar a las rutas)
        before("/admin/catedra/*", CatedraLogic::middleware);
        before("/admin/catedras", CatedraLogic::middleware);

        // Muestra la página principal de gestión de cátedras.
        // Permitirá visualizar las asignaciones actuales entre docentes y materias.
        get("/admin/catedras", CatedraLogic::listarCatedras, new MustacheTemplateEngine());

        // Recibe los datos del formulario y crea una nueva asignación
        // entre un docente y una materia.
        post("/admin/catedras/asignar", CatedraLogic::asignarDocente);

        // Elimina una asignación existente entre un docente y una materia.
        post("/admin/catedras/desasignar", CatedraLogic::desasignarDocente);

        get("/teacher/mis-catedras", CatedraLogic::misCatedras, new MustacheTemplateEngine());

        ///////////////////////////////////////////////////////////////////////////////
        ////////////////////// CARRERAS ////////////////////////////////////////
        ///////////////////////////////////////////////////////////////////////////////

        // 1. Mostrar el formulario para registrar una nueva carrera (GET)
        get("/admin/carrera/new", CarreraLogic::createCarreraForm, new MustacheTemplateEngine());

        // 2. Procesar los datos y guardar la nueva carrera en la BD (POST)
        post("/admin/carrera/new", CarreraLogic::storeInDB, new MustacheTemplateEngine());

        // 3. Mostrar la tabla con el listado de todas las carreras (GET)
        get("/admin/carreras", CarreraLogic::listCarreras, new MustacheTemplateEngine());

        // 4. Mostrar el formulario para editar una carrera existente (GET)
        get("/admin/carrera/edit/:cod_carrera", CarreraLogic::editCarreraForm, new MustacheTemplateEngine());

        // 5. Procesar los cambios de la carrera editada (POST)
        post("/admin/carrera/edit/:cod_carrera", CarreraLogic::editCarrera);

        // 6. Eliminar una carrera de la base de datos (GET)
        post("/admin/carrera/delete/:cod_carrera", CarreraLogic::deleteCarrera);

        ///////////////////////////////////////////////////////////////////////////////
        ////////////////////// CORRELATIVIDADES //////////////////////////
        ///////////////////////////////////////////////////////////////////////////////

        before("/materias/*", CorrelatividadLogic::middleware);

        get("/materias/correlativas", CorrelatividadLogic::listarCorrelatividades, new MustacheTemplateEngine());

        post("/materias/configurar-correlativas", CorrelatividadLogic::agregarCorrelatividad);

        post("/materias/eliminar-correlativa", CorrelatividadLogic::eliminarCorrelatividad);

        ///////////////////////////////////////////////////////////////////////////////
        ////////////////////// INCRIPCIONES //////////////////////////
        ///////////////////////////////////////////////////////////////////////////////

        before("/inscripciones/*", InscripcionLogic::middleware);

        get("/inscripciones", InscripcionLogic::listarInscripciones, new MustacheTemplateEngine());
        post("/inscripciones/inscribir/:cod_materia", InscripcionLogic::inscribirMateria);
        get("/inscripciones/mis-inscripciones", InscripcionLogic::misInscripciones, new MustacheTemplateEngine());

        ///////////////////////////////////////////////////////////////////////////////
        ////////////////////// Examenes Finales //////////////////////////
        ///////////////////////////////////////////////////////////////////////////////

        before("/teacher/crear-examen/:cod_materia", ExamenFinalLogic::middleware);
        before("/teacher/examen-final/:id_examen/nota", ExamenFinalLogic::middleware);
        before("/teacher/examen-materia", ExamenFinalLogic::middleware);

        get("/teacher/crear-examen/:cod_materia", ExamenFinalLogic::crearExamenForm, new MustacheTemplateEngine());
        post("/teacher/crear-examen/:cod_materia", ExamenFinalLogic::crearExamen);
        get("/teacher/examen-final/:id_examen/nota", ExamenFinalLogic::cargarNotaForm, new MustacheTemplateEngine());
        post("/teacher/examen-final/:id_examen/nota", ExamenFinalLogic::cargarNota);

    } // Fin del método main
} // Fin de la clase App
