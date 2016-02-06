package io.github.thisisnozaku.charactercreator.controllers;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.webkit.plugin.Plugin;
import io.github.thisisnozaku.charactercreator.NeoneCoreApplication;
import io.github.thisisnozaku.charactercreator.data.CharacterDataWrapper;
import io.github.thisisnozaku.charactercreator.data.UserRepository;
import io.github.thisisnozaku.charactercreator.data.CharacterMongoRepository;
import io.github.thisisnozaku.charactercreator.plugins.Character;
import io.github.thisisnozaku.charactercreator.plugins.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.access.method.P;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.math.BigInteger;
import java.net.URLEncoder;
import java.util.Objects;
import java.util.Optional;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = {NeoneCoreApplication.class})
public class GameControllerTest {
    private GameController controller;
    @Mock
    private CharacterMongoRepository characters;
    @Mock
    private UserRepository accounts;
    @Mock
    private PluginManager plugins;
    @Mock
    private HandlerMethodArgumentResolver resolver;

    private MockMvc mvc;

    @Mock
    private GamePlugin firstPlugin;
    @Mock
    private GamePlugin secondPlugin;
    PluginDescription desc1 = new PluginDescription("Damien Marble", "Game System", "1.1");
    PluginDescription desc2 = new PluginDescription("Mamien Darble", "Second Game System", "1.0");

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        controller = new GameController(characters, accounts, plugins);

        mvc = MockMvcBuilders.standaloneSetup(controller).addInterceptors(new PluginPresenceInterceptor(plugins)).setCustomArgumentResolvers(new CharacterResolver(plugins)).build();

        when(plugins.getPlugin(isA(String.class), isA(String.class), isA(String.class))).thenAnswer((invocation -> {
            Object[] args = invocation.getArguments();
            if (args[0].equals(desc1.getAuthor()) && args[1].equals(desc1.getSystem()) && args[2].equals(desc1.getVersion())) {
                return Optional.of(firstPlugin);
            } else if (args[0].equals(desc2.getAuthor()) && args[1].equals(desc2.getSystem()) && args[2].equals(desc2.getVersion())) {
                return Optional.of(secondPlugin);
            } else {
                return Optional.empty();
            }
        }));

        when(plugins.getPlugin(isA(PluginDescription.class))).thenAnswer(invocation -> {
            PluginDescription pluginDescription = (PluginDescription) invocation.getArguments()[0];
            return plugins.getPlugin(pluginDescription.getAuthor(), pluginDescription.getSystem(), pluginDescription.getVersion());
        });

        when(resolver.supportsParameter(any(MethodParameter.class))).thenAnswer(invocation -> {
            MethodParameter param = invocation.getArgumentAt(0, MethodParameter.class);
            return param.getParameterType().equals(Character.class);
        });

        when(resolver.resolveArgument(any(MethodParameter.class), any(ModelAndViewContainer.class), any(NativeWebRequest.class), any(WebDataBinderFactory.class)))
                .thenAnswer(invocation -> {
                    return invocation.getArgumentAt(2, NativeWebRequest.class).getAttribute("character", RequestAttributes.SCOPE_REQUEST);
                });

        when(characters.save(isA(CharacterDataWrapper.class))).thenAnswer(invocation -> {
            CharacterDataWrapper character = invocation.getArgumentAt(0, CharacterDataWrapper.class);
            if (character.getId() == null) {
                character.setId(BigInteger.ONE);
            }
            return character;
        });

        when(characters.findOne(isA(BigInteger.class))).then(a -> {
            BigInteger id = (BigInteger) a.getArguments()[0];
            if (id.equals(BigInteger.ONE)) {
                return new CharacterDataWrapper(desc1, null, (Character) firstPlugin.getCharacterType().newInstance());
            } else if (id.equals(BigInteger.TEN)) {
                return new CharacterDataWrapper(desc2, null, (Character) secondPlugin.getCharacterType().newInstance());
            } else {
                return null;
            }
        });

        when(firstPlugin.getCharacterType()).thenReturn(MockCharacter.class);
        when(secondPlugin.getCharacterType()).thenReturn(MockCharacter.class);
    }

    /**
     * Test returning the firstPlugin information page.
     *
     * @throws Exception
     */
    @Test
    public void testGetPluginInformationPage() throws Exception {
        PluginDescription desc = desc1;
        RequestBuilder request = get("/games/" +
                URLEncoder.encode(desc.getAuthor(), "UTF-8") + "/" +
                URLEncoder.encode(desc.getSystem(), "UTF-8") + "/" +
                URLEncoder.encode(desc.getVersion(), "UTF-8") + "/info")
                .contentType(MediaType.TEXT_HTML);
        MvcResult result = mvc.perform(request)
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(view().name("plugin-character-page"))
                .andReturn();
    }

    /**
     * Attempt to get the firstPlugin description page for a firstPlugin that isn't present.
     *
     * @throws Exception
     */
    @Test
    public void testGetInformationPageMissingPlugin() throws Exception {
        PluginDescription desc = new PluginDescription("Fake", "Fake", "Fake");
        RequestBuilder request = get("/games/" +
                URLEncoder.encode(desc.getAuthor(), "UTF-8") + "/" +
                URLEncoder.encode(desc.getSystem(), "UTF-8") + "/" +
                URLEncoder.encode(desc.getVersion(), "UTF-8") + "/info")
                .contentType(MediaType.TEXT_HTML);

        mvc.perform(request)
                .andExpect(status().isNotFound())
                .andExpect(view().name("missing-plugin"));
    }

    /**
     * Test creating a new character and displaying the character sheet.
     *
     * @throws Exception
     */
    @Test
    public void testCreate() throws Exception {
        PluginDescription desc = desc1;
        Character newCharacter = new MockCharacter(desc);
        ObjectMapper objectMapper = new ObjectMapper();
        String mappedObject = objectMapper.writeValueAsString(newCharacter);
        RequestBuilder request = post("/games/" +
                URLEncoder.encode(desc.getAuthor(), "UTF-8") + "/" +
                URLEncoder.encode(desc.getSystem(), "UTF-8") + "/" +
                URLEncoder.encode(desc.getVersion(), "UTF-8") + "/")
                .content(mappedObject)
                .contentType(MediaType.APPLICATION_JSON_UTF8);


        MvcResult result = mvc.perform(request)
                .andDo(print()).andReturn();
        objectMapper.readValue(result.getRequest().getInputStream(), MockCharacter.class);
        verify(characters).save(isA(CharacterDataWrapper.class));
    }

    /**
     * Test attempting to create a character for a firstPlugin that isn't present
     *
     * @throws Exception
     */
    @Test
    public void testCreateForMissingPlugin() throws Exception {
        Character mockCharacter = new MockCharacter();
        ObjectMapper mapper = new ObjectMapper();
        RequestBuilder request = post("/games/" +
                URLEncoder.encode("Missing", "UTF-8") + "/" +
                URLEncoder.encode("Missing", "UTF-8") + "/" +
                URLEncoder.encode("Missing", "UTF-8") + "/")
                .content(mapper.writeValueAsString(mockCharacter))
                .contentType(MediaType.APPLICATION_JSON_UTF8);

        mvc.perform(request)
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(view().name(
                        "missing-plugin"
                ));
    }

    /**
     * Test getting a character.
     *
     * @throws Exception
     */
    @Test
    public void testGetCharacter() throws Exception {
        PluginDescription desc = desc1;
        MockCharacter existingCharacter = new MockCharacter(desc);
        existingCharacter.setId(BigInteger.ONE);


        RequestBuilder request = get("/games/" +
                URLEncoder.encode(desc.getAuthor(), "UTF-8") + "/" +
                URLEncoder.encode(desc.getSystem(), "UTF-8") + "/" +
                URLEncoder.encode(desc.getVersion(), "UTF-8") + "/" +
                existingCharacter.getId())
                .contentType(MediaType.TEXT_HTML);

        mvc.perform(request)
                .andDo(print())
                .andExpect(view().name(
                        "plugin-character-page"
                ));

        verify(characters).findOne(existingCharacter.getId());
    }

    /**
     * Test getting a character that doesn't exist
     *
     * @throws Exception
     */
    @Test
    public void testGetMissingCharacter() throws Exception {
        PluginDescription desc = desc1;


        RequestBuilder request = get("/games/" +
                URLEncoder.encode(desc.getAuthor(), "UTF-8") + "/" +
                URLEncoder.encode(desc.getSystem(), "UTF-8") + "/" +
                URLEncoder.encode(desc.getVersion(), "UTF-8") + "/" +
                2)
                .contentType(MediaType.TEXT_HTML);

        mvc.perform(request)
                .andDo(print())
                .andExpect(view().name(
                        "missing-character"
                ));
    }

    @Test
    public void testSaveCharacter() throws Exception {
        PluginDescription desc = desc1;
        Character mockCharacter = new MockCharacter(desc);
        ObjectMapper objectMapper = new ObjectMapper();
        BigInteger id = BigInteger.ONE;

        RequestBuilder request = put("/games/" +
                URLEncoder.encode(desc.getAuthor(), "UTF-8") + "/" +
                URLEncoder.encode(desc.getSystem(), "UTF-8") + "/" +
                URLEncoder.encode(desc.getVersion(), "UTF-8") + "/" +
                id)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(objectMapper.writeValueAsString(mockCharacter));

        mvc.perform(request)
                .andDo(print())
                .andExpect(status().isAccepted());
        CharacterDataWrapper characterDataWrapper = new CharacterDataWrapper(desc, null, mockCharacter);
        characterDataWrapper.setId(BigInteger.ONE);

        verify(characters).save(characterDataWrapper);
    }

    @Test
    public void testDeleteCharacter() throws Exception {
        PluginDescription desc = desc1;
        Character mockCharacter = new MockCharacter(desc);
        BigInteger id = BigInteger.ONE;

        RequestBuilder request = delete("/games/" +
                URLEncoder.encode(desc.getAuthor(), "UTF-8") + "/" +
                URLEncoder.encode(desc.getSystem(), "UTF-8") + "/" +
                URLEncoder.encode(desc.getVersion(), "UTF-8") + "/" +
                id);

        mvc.perform(request)
                .andDo(print())
                .andExpect(status().isAccepted());

        verify(characters).delete(id);
    }

    public static class MockCharacter implements Character {
        private BigInteger id;
        private PluginDescription plugin;

        public MockCharacter() {
        }

        ;

        public MockCharacter(PluginDescription pluginDescription) {
            this.plugin = pluginDescription;
        }

        public BigInteger getId() {
            return id;
        }

        public void setId(BigInteger id) {
            if (this.id != null) {
                throw new IllegalStateException("Attempted to reassign the id of the character");
            }
            this.id = id;
        }

        public PluginDescription getPluginDescription() {
            return plugin;
        }

        public void setPluginDescription(PluginDescription pluginDescription) {
            this.plugin = pluginDescription;
        }

        @Override
        public String toString() {
            return "MockCharacter{" +
                    "id=" + id +
                    ", plugin=" + plugin +
                    '}';
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (!this.getClass().isInstance(obj)) {
                return false;
            }
            MockCharacter other = (MockCharacter) obj;
            return Objects.equals(this.id, other.id);
        }
    }
}