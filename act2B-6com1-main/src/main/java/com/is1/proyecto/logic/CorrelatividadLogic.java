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

    
    // MIDDLEWARE
    public static void middleware(Request req, Response res){

        if(!isAuthenticated(req) || !isAdmin(req)){ 
            res.redirect("/?error=No autorizado");
            halt(401);
        }
    }

    //GET /materias/correlativas
    public static ModelAndView listarCorrelatividades(Request req, Response res){

        String currentUsername = req.session().attribute("currentUserUsername");
        Boolean loggedIn = req.session().attribute("loggedIn");

        if(currentUsername == null || loggedIn == null || !loggedIn || !isAdmin(req)){
            res.redirect("/?error=No autorizado");
            return null;
        }

        Map<String,Object> model = new HashMap<>();

        // Lista de correlatividades existentes
        List<Map<String,Object>> lista = new ArrayList<>();

        LazyList<MateriaCorrelatividad> correlativas =
                MateriaCorrelatividad.findAll();

        for(MateriaCorrelatividad c : correlativas){

            Map<String,Object> row = new HashMap<>();

            row.put("id_correlatividad",c.getIdCorrelatividad());
            row.put("materia_origen",c.getMateriaOrigen());
            row.put("materia_requerida",c.getMateriaRequerida());

            lista.add(row);
        }

        model.put("correlativas",lista);

        // Materias disponibles para los combos
        List<Materia> materias = Materia.findAll();
        model.put("materias",materias);

        return new ModelAndView(model,"correlatividades.mustache");
    }

    // POST /materias/configurar-correlativas

    public static ModelAndView agregarCorrelatividad(Request req, Response res){

        String materiaOrigen = req.queryParams("materia_origen");
        String materiaRequerida = req.queryParams("materia_requerida");

        // Una materia no puede depender de sí misma

        if(materiaOrigen.equals(materiaRequerida)){
            res.redirect("/materias/correlativas?error=Una materia no puede ser correlativa de si misma");
            return null;
        }

        // Evita duplicados

        MateriaCorrelatividad existente =
                MateriaCorrelatividad.findFirst(
                        "materia_origen = ? and materia_requerida = ?",
                        materiaOrigen,
                        materiaRequerida
                );

        if(existente != null){
            res.redirect("/materias/correlativas?error=La correlatividad ya existe");
            return null;
        }

        // Evita circularidad
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

        // Guarda
        MateriaCorrelatividad correlativa = new MateriaCorrelatividad();
        correlativa.setIdCorrelatividad(1L);
        correlativa.setMateriaOrigen(materiaOrigen);
        correlativa.setMateriaRequerida(materiaRequerida);
        correlativa.insert();

        res.redirect("/materias/correlativas");

        return null;
    }

    // POST /materias/eliminar-correlativa
    public static ModelAndView eliminarCorrelatividad(Request req, Response res){

        String materiaOrigen =
                req.queryParams("materia_origen");

        String materiaRequerida =
                req.queryParams("materia_requerida");

        MateriaCorrelatividad correlativa =
                MateriaCorrelatividad.findFirst(
                        "materia_origen = ? and materia_requerida = ?",
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