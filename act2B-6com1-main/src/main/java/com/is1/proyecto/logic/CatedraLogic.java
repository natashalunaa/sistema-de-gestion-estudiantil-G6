package com.is1.proyecto.logic;

import spark.ModelAndView;
import spark.Request;
import spark.Response;

import java.util.HashMap;

public class CatedraLogic {

    // Muestra la página principal de gestión de cátedras
    public static ModelAndView listarCatedras(Request req, Response res) {

        // Mapa que contendrá los datos para la plantilla Mustache
        HashMap<String, Object> model = new HashMap<>();

        // Por ahora no cargamos información.
        // Más adelante agregaremos docentes, materias y asignaciones.

        return new ModelAndView(
                model,
                "catedras.mustache"
        );
    }
}