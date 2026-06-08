package com.is1.proyecto;

import com.is1.proyecto.logic.AdminLogic;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.Session;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminLogicTest {

    @Test
    void adminDashboard_withAuthenticatedAdminUser_returnsModelAndView() {
        Request req = mock(Request.class);
        Response res = mock(Response.class);
        Session session = mock(Session.class);

        when(req.session()).thenReturn(session);
        when(session.attribute("currentUserUsername")).thenReturn("admin_user");
        when(session.attribute("loggedIn")).thenReturn(true);
        when(session.attribute("role")).thenReturn("admin");

        ModelAndView view = AdminLogic.adminDashboard(req, res);

        assertNotNull(view);
        Map<String, Object> model = view.getModel();
        assertTrue(model.containsKey("username"));
        assertEquals("admin_user", model.get("username"));
        assertEquals("admin_dashboard.mustache", view.getViewName());
    }

    @Test
    void adminDashboard_withUnauthenticatedUser_redirectsToLoginWithError() {
        Request req = mock(Request.class);
        Response res = mock(Response.class);
        Session session = mock(Session.class);

        when(req.session()).thenReturn(session);
        when(session.attribute("currentUserUsername")).thenReturn(null);
        when(session.attribute("loggedIn")).thenReturn(false);
        when(session.attribute("role")).thenReturn(null);

        ModelAndView view = AdminLogic.adminDashboard(req, res);

        assertNull(view);
        verify(res).redirect("/?error=Acceso denegado.");
    }

    @Test
    void adminDashboard_withNonAdminUser_redirectsToLoginWithError() {
        Request req = mock(Request.class);
        Response res = mock(Response.class);
        Session session = mock(Session.class);

        when(req.session()).thenReturn(session);
        when(session.attribute("currentUserUsername")).thenReturn("student_user");
        when(session.attribute("loggedIn")).thenReturn(true);
        when(session.attribute("role")).thenReturn("student");

        ModelAndView view = AdminLogic.adminDashboard(req, res);

        assertNull(view);
        verify(res).redirect("/?error=Acceso denegado.");
    }
}
