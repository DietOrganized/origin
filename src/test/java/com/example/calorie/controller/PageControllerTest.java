package com.example.calorie.controller;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PageControllerTest {
    private MockMvc mvc() {
        var resolver = new InternalResourceViewResolver("/WEB-INF/jsp/", ".jsp");
        return MockMvcBuilders.standaloneSetup(new PageController()).setViewResolvers(resolver).build();
    }

    @Test
    void sidebarRoutesResolveToTheirOwnJspViews() throws Exception {
        var mvc = mvc();
        for (var page : new String[]{"overview", "meals", "stats", "recipes", "advice", "profile"}) {
            mvc.perform(get("/jsp/" + page))
                    .andExpect(status().isOk())
                    .andExpect(view().name(page))
                    .andExpect(forwardedUrl("/WEB-INF/jsp/" + page + ".jsp"));
        }
    }

    @Test
    void entryPointsRedirectToDashboard() throws Exception {
        var mvc = mvc();
        for (var path : new String[]{"/", "/jsp", "/jsp/"}) {
            mvc.perform(get(path)).andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/jsp/overview"));
        }
    }
}
