package com.is1.proyecto.logic;

import spark.ModelAndView;
import spark.Request;
import spark.Response;

import java.util.HashMap;
import java.util.Map;

import static com.is1.proyecto.logic.UserLogic.isAdmin;
import static com.is1.proyecto.logic.UserLogic.isAuthenticated;

public class AdminLogic {
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