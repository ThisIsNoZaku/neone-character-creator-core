package io.github.thisisnozaku.charactercreator.controllers;

import io.github.thisisnozaku.charactercreator.NeoneCoreApplication;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.ModelAndViewAssert;
import org.springframework.test.web.client.ResponseActions;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.ModelAndView;

import javax.inject.Inject;
import javax.xml.ws.Response;

import static org.junit.Assert.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
/**
 * Created by Damien on 11/15/2015.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = {NeoneCoreApplication.class})
public class IndexControllerTest {
    MockMvc mvc;
    @Inject
    IndexController controller;

    @Before
    public void setup(){
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    /**
     * Test displaying the index page to an unauthenticated user.
     */
    @Test
    public void testGettingIndexUnauthenticated() throws Exception {
        MockHttpServletRequestBuilder request = get("/").contentType(MediaType.TEXT_HTML);

        MvcResult result = mvc.perform(request).andExpect(status().isOk())
                .andExpect(content().)
                .andReturn();

        ModelAndView mv = result.getModelAndView();
        ModelAndViewAssert.assertViewName(mv, "index");
        String responseContent = result.getResponse().getContentAsString();
        assertTrue(responseContent.contains("Would you like to create a new character or load an existing character?"));
        assertFalse(responseContent.contains("Please sign in or create an account to continue."));
    }
}