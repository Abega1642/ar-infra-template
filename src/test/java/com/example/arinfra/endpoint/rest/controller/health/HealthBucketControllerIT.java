package com.example.arinfra.endpoint.rest.controller.health;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.arinfra.InfraGenerated;
import com.example.arinfra.conf.FacadeIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@InfraGenerated
class HealthBucketControllerIT extends FacadeIT {
  @Autowired private MockMvc mvc;

  @Test
  void should_upload_file_and_directory_and_return_presigned_url() throws Exception {
    mvc.perform(get("/health/bucket").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$").exists())
        .andExpect(jsonPath("$").isString())
        .andExpect(jsonPath("$").value(org.hamcrest.Matchers.containsString("http")));
  }
}
