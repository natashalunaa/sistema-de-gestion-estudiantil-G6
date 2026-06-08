package com.is1.proyecto.logic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.javalite.activejdbc.LazyList;

import static com.is1.proyecto.logic.UserLogic.ROLE_STUDENT;
import static com.is1.proyecto.logic.UserLogic.isAuthenticated;
import com.is1.proyecto.models.Alumno;
import com.is1.proyecto.models.AlumnoMateria;
import com.is1.proyecto.models.Materia;
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
    }

    // GET: /inscripciones
// Muestra las materias disponibles y las inscripciones del alumno.
public static ModelAndView listarInscripciones(Request req, Response res) {

    // Obtiene los datos de la sesión
    String currentUsername = req.session().attribute("currentUserUsername");
    Boolean loggedIn = req.session().attribute("loggedIn");

    // Verifica que el usuario esté autenticado
    if (currentUsername == null || loggedIn == null || !loggedIn) {
        res.redirect("/?error=No autorizado");
        return null;
    }
    
    // Busca la persona asociada al usuario logueado
    Persona persona = Persona.findFirst("mail = ? OR dni = ?",currentUsername, currentUsername);

    // Si no existe la persona, vuelve al inicio
    if (persona == null) {
        res.redirect("/?error=Alumno no encontrado");
        return null;
    }
    // Obtiene el DNI del alumno
    String dniAlumno = persona.getDni();

    // Modelo que se enviará a Mustache
    Map<String, Object> model = new HashMap<>();

    // Guarda el DNI para futuras operaciones
    model.put("dniAlumno", dniAlumno);

    // Obtiene todas las materias disponibles
    LazyList<Materia> materias = Materia.findAll();

    //Filtra las materias a las que el alumno no esta inscripto
    LazyList<AlumnoMateria> inscripcionesAlumno = AlumnoMateria.where("alumno_dni = ?", dniAlumno);
    for (AlumnoMateria inscripcion : inscripcionesAlumno) {
        String codMateriaInscripta = inscripcion.getCodMateria();
        materias.removeIf(m -> m.getCodMateria().equals(codMateriaInscripta));
    }

    // Envía las materias a la vista
    model.put("materias", materias);

    // Por ahora las inscripciones estarán vacías.
    // Más adelante cargaremos las materias en las que el alumno ya está anotado.
    model.put("inscripciones", new java.util.ArrayList<>());

    // Devuelve la vista
    return new ModelAndView(model, "inscripciones.mustache");
    
}

// Incribirse a una materia
//Post: /inscripciones/inscribir/:cod_materia
public static ModelAndView inscribirMateria(Request req, Response res) {
     // Obtiene los datos de la sesión
    String currentUsername = req.session().attribute("currentUserUsername");
    Boolean loggedIn = req.session().attribute("loggedIn");
    String codMateria = req.params(":cod_materia");

    // Verifica que el usuario esté autenticado
    if (currentUsername == null || loggedIn == null || !loggedIn) {
        res.redirect("/?error=No autorizado");
        return null;
    }

    //Verifica si la materia existe
    Materia materia = Materia.findById(codMateria);
    if (materia == null) {
        res.redirect("/?error=Materia no encontrada");
        return null;
    }

    //Busca al alumno asociada al usuario logueado
    Persona persona = Persona.findFirst("mail = ? OR dni = ?",currentUsername, currentUsername);
    Alumno alumno = Alumno.findFirst("dni = ?", persona.getDni());
    if (alumno == null) {   
        res.redirect("/?error=Alumno no encontrado");
        return null;
    }

    //Verifica que el usuario no esté ya inscripto en esa materia
    AlumnoMateria inscripcionExistente = AlumnoMateria.findFirst("alumno_dni = ? AND cod_materia = ?", alumno.getDni(), codMateria);
    if (inscripcionExistente != null) {
        res.redirect("/inscripciones?error=Ya estás inscripto en esta materia");
        return null;
    }

    // Crea la inscripción
    AlumnoMateria.createIt("alumno_dni", alumno.getDni(), "cod_materia", codMateria);
    res.redirect("/inscripciones?success=Inscripción realizada con éxito");
    return null;
}

//Ver las incripciones del alumno
//GET /inscripciones/mis-inscripciones
public static ModelAndView misInscripciones(Request req, Response res) {

        // Obtiene los datos de la sesión
        String currentUsername = req.session().attribute("currentUserUsername");
        Boolean loggedIn = req.session().attribute("loggedIn");
    
        // Verifica que el usuario esté autenticado
        if (currentUsername == null || loggedIn == null || !loggedIn) {
            res.redirect("/?error=No autorizado");
            return null;
        }
    
        //Busca al alumno asociada al usuario logueado
        Persona persona = Persona.findFirst("mail = ? OR dni = ?",currentUsername, currentUsername);
        Alumno alumno = Alumno.findFirst("dni = ?", persona.getDni());
        if (alumno == null) {   
            res.redirect("/?error=Alumno no encontrado");
            return null;
        }
        if (persona == null){
            res.redirect("/?error=Alumno no encontrado");
            return null;
        }
        // Modelo que se enviará a Mustache
        Map<String, Object> model = new HashMap<>();
    
        // Obtiene las inscripciones del alumno
        LazyList<AlumnoMateria> inscripcionesAlumno = AlumnoMateria.where("alumno_dni = ?", alumno.getDni());
    
        // Carga las materias en las que el alumno está inscripto
        List<Map<String, Object>> materiasInscripto = new ArrayList<>();
        for (AlumnoMateria inscripcion : inscripcionesAlumno) {
            Materia materia = Materia.findById(inscripcion.getCodMateria());
            if (materia != null) {
                Map<String, Object> row = new HashMap<>();
                row.put("cod_materia", materia.getCodMateria());
                row.put("nombre_materia", materia.getNombreMateria());
                materiasInscripto.add(row);
            }
        }
    
        model.put("materiasInscripto", materiasInscripto);
    
        // Devuelve la vista
        return new ModelAndView(model, "mis_inscripciones.mustache");
}

}
