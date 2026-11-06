package com.example.alquiler_de_vehiculos.controller;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@Transactional
class ClienteControllerTest {

    private final WebApplicationContext webApplicationContext;
    private MockMvc mockMvc;

    public ClienteControllerTest(WebApplicationContext webApplicationContext) {
        this.webApplicationContext = webApplicationContext;
    }

    @BeforeEach
    void configurarMockMvc() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listarClientes_devuelveOk() throws Exception {
        mockMvc.perform(get("/api/clientes"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void crearCliente_loGuardaCorrectamente() throws Exception {
        String nuevoCliente = """
                {
                  "nombres": "Test",
                  "apellidos": "Junit",
                  "tipoDocumento": "DNI",
                  "numeroDocumento": "99999991",
                  "telefono": "999999991",
                  "email": "test.junit1@correo.com",
                  "licenciaConducir": "Q99999991"
                }
                """;

        mockMvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(nuevoCliente))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idCliente", notNullValue()))
                .andExpect(jsonPath("$.nombres", is("Test")))
                .andExpect(jsonPath("$.numeroDocumento", is("99999991")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void actualizarCliente_cambiaSusDatos() throws Exception {
        String nuevoCliente = """
                {
                  "nombres": "Original",
                  "apellidos": "Apellido",
                  "tipoDocumento": "DNI",
                  "numeroDocumento": "99999992",
                  "telefono": "999999992",
                  "email": "original@correo.com",
                  "licenciaConducir": "Q99999992"
                }
                """;

        String respuesta = mockMvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(nuevoCliente))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Integer id = JsonPath.read(respuesta, "$.idCliente");

        String clienteActualizado = """
                {
                  "nombres": "Actualizado",
                  "apellidos": "Apellido",
                  "tipoDocumento": "DNI",
                  "numeroDocumento": "99999992",
                  "telefono": "999999992",
                  "email": "actualizado@correo.com",
                  "licenciaConducir": "Q99999992"
                }
                """;

        mockMvc.perform(put("/api/clientes/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(clienteActualizado))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombres", is("Actualizado")))
                .andExpect(jsonPath("$.email", is("actualizado@correo.com")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void eliminarCliente_comoAdmin_loElimina() throws Exception {
        String nuevoCliente = """
                {
                  "nombres": "Borrar",
                  "apellidos": "Junit",
                  "tipoDocumento": "DNI",
                  "numeroDocumento": "99999993",
                  "telefono": "999999993",
                  "email": "borrar@correo.com",
                  "licenciaConducir": "Q99999993"
                }
                """;

        String respuesta = mockMvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(nuevoCliente))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Integer id = JsonPath.read(respuesta, "$.idCliente");

        mockMvc.perform(delete("/api/clientes/" + id))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "EMPLEADO")
    void eliminarCliente_comoEmpleado_esRechazado() throws Exception {
        mockMvc.perform(delete("/api/clientes/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void listarClientes_sinAutenticar_esRechazado() throws Exception {
        // Sin token ahora se responde 401 (no autenticado) en vez de 403
        mockMvc.perform(get("/api/clientes"))
                .andExpect(status().isUnauthorized());
    }
}