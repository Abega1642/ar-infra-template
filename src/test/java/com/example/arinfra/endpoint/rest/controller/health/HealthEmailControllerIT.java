package com.example.arinfra.endpoint.rest.controller.health;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.arinfra.InfraGenerated;
import com.example.arinfra.conf.FacadeIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

@InfraGenerated
class HealthEmailControllerIT extends FacadeIT {
  @Autowired private MockMvc mvc;

  @Test
  void should_send_email_with_successful_response() throws Exception {
    String email = "a.razafindratelo@gmail.com";
    String expected = "All 5 test emails sent successfully to " + email;

    mvc.perform(get("/health/email").param("to", email))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString(expected)));
  }
}
