package com.is1.proyecto.logic;

import java.util.HashMap;
import java.util.Map;

import static com.is1.proyecto.logic.UserLogic.isAdmin;
import static com.is1.proyecto.logic.UserLogic.isAuthenticated;

import spark.ModelAndView;
import spark.Request;
import spark.Response;
import static spark.Spark.halt;

public class AdminLogic {

    public static void middleware(Request req, Response res) {
        if (!isAuthenticated(req) || !isAdmin(req)) {
            res.redirect("/?error=No autorizado");
            halt(401);
        }
    }
    
    public static ModelAndView adminDashboard(Request req, Response res) {
        if (!isAuthenticated(req) || !isAdmin(req)) {
            res.redirect("/?error=Acceso denegado.");
            return null;
        }
        Map<String, Object> model = new HashMap<>();
        model.put("username", req.session().attribute("currentUserUsername"));
        return new ModelAndView(model, "admin_dashboard.mustache");
    }
}