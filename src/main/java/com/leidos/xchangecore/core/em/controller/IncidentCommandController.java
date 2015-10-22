package com.leidos.xchangecore.core.em.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

import com.leidos.xchangecore.core.em.service.IncidentManagementService;

public class IncidentCommandController
    extends AbstractController {

    IncidentManagementService ims;

    @Override
    protected ModelAndView handleRequestInternal(HttpServletRequest request,
                                                 HttpServletResponse response) throws Exception {

        // String title = request.getParameter("title");

        return null;
    }
}
