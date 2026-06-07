package com.is1.proyecto.logic;

import com.is1.proyecto.models.Materia;
import com.is1.proyecto.models.MateriaCorrelatividad;

import org.javalite.activejdbc.LazyList;

import spark.ModelAndView;
import spark.Request;
import spark.Response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.is1.proyecto.logic.UserLogic.isAdmin;
import static com.is1.proyecto.logic.UserLogic.isAuthenticated;
import static spark.Spark.halt;

public class CorrelatividadLogic {

    // Middleware
    public static void middleware(Request req, Response res){

        if(!isAuthenticated(req) || !isAdmin(req)){
            res.redirect("/?error=No autorizado");
            halt(401);
        }
    }

    // GET
    public static ModelAndView listarCorrelatividades(Request req, Response res){

        String currentUsername = req.session().attribute("currentUserUsername");
        Boolean loggedIn = req.session().attribute("loggedIn");

        if(currentUsername == null || loggedIn == null || !loggedIn || !isAdmin(req)){
            res.redirect("/?error=No autorizado");
            return null;
        }

        Map<String,Object> model = new HashMap<>();

        // Lista de correlatividades

        List<Map<String,Object>> lista = new ArrayList<>();

        LazyList<MateriaCorrelatividad> correlativas =
                MateriaCorrelatividad.findAll();

        for(MateriaCorrelatividad c : correlativas){

            Map<String,Object> row = new HashMap<>();

            row.put("id_correlatividad", c.getIdCorrelatividad());
            row.put("materia_origen", c.getMateriaOrigen());
            row.put("materia_requerida", c.getMateriaRequerida());

            lista.add(row);
        }

        model.put("correlativas", lista);

        // Materias para los combos

        List<Map<String,Object>> materiasList = new ArrayList<>();
        LazyList<Materia> materias = Materia.findAll();

        for(Materia m : materias){

            Map<String,Object> row = new HashMap<>();

            row.put("cod_materia", m.getCodMateria());
            row.put("nombre_materia", m.getNombreMateria());

            materiasList.add(row);
        }

        model.put("materias", materiasList);

        return new ModelAndView(model,"correlatividades.mustache");
    }


    // POST

    public static ModelAndView agregarCorrelatividad(Request req, Response res){

        String materiaOrigen = req.queryParams("materia_origen");
        String materiaRequerida = req.queryParams("materia_requerida");

        Long idCorrelatividad =
                Long.parseLong(req.queryParams("id_correlatividad"));

        // No puede ser la misma materia

        if(materiaOrigen.equals(materiaRequerida)){
            res.redirect("/materias/correlativas?error=Una materia no puede ser correlativa de si misma");
            return null;
        }

        // Duplicados

        MateriaCorrelatividad existente =
                MateriaCorrelatividad.findFirst(
                        "id_correlatividad = ? and materia_origen = ? and materia_requerida = ?",
                        idCorrelatividad,
                        materiaOrigen,
                        materiaRequerida
                );

        if(existente != null){
            res.redirect("/materias/correlativas?error=La correlatividad ya existe");
            return null;
        }

        // Circularidad

        MateriaCorrelatividad inversa =
                MateriaCorrelatividad.findFirst(
                        "materia_origen = ? and materia_requerida = ?",
                        materiaRequerida,
                        materiaOrigen
                );

        if(inversa != null){
            res.redirect("/materias/correlativas?error=Dependencia circular");
            return null;
        }

        // Guardar

        MateriaCorrelatividad correlativa =
                new MateriaCorrelatividad();

        correlativa.setIdCorrelatividad(idCorrelatividad);
        correlativa.setMateriaOrigen(materiaOrigen);
        correlativa.setMateriaRequerida(materiaRequerida);

        correlativa.insert();

        res.redirect("/materias/correlativas");

        return null;
    }


    // ELIMINAR

    public static ModelAndView eliminarCorrelatividad(Request req, Response res){

        Long idCorrelatividad =
                Long.parseLong(req.queryParams("id_correlatividad"));

        String materiaOrigen =
                req.queryParams("materia_origen");

        String materiaRequerida =
                req.queryParams("materia_requerida");

        MateriaCorrelatividad correlativa =
                MateriaCorrelatividad.findFirst(
                        "id_correlatividad = ? and materia_origen = ? and materia_requerida = ?",
                        idCorrelatividad,
                        materiaOrigen,
                        materiaRequerida
                );

        if(correlativa != null){
            correlativa.delete();
        }

        res.redirect("/materias/correlativas");

        return null;
    }
}