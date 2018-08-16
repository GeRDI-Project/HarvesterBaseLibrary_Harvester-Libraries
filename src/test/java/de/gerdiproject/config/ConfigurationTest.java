/*
 *  Copyright © 2018 Robin Weiss (http://www.gerdi-project.de/)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package de.gerdiproject.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import ch.qos.logback.classic.Level;
import de.gerdiproject.harvest.config.Configuration;
import de.gerdiproject.harvest.config.adapter.ConfigurationAdapter;
import de.gerdiproject.harvest.config.events.GlobalParameterChangedEvent;
import de.gerdiproject.harvest.config.events.HarvesterParameterChangedEvent;
import de.gerdiproject.harvest.config.parameters.AbstractParameter;
import de.gerdiproject.harvest.config.parameters.BooleanParameter;
import de.gerdiproject.harvest.config.parameters.IntegerParameter;
import de.gerdiproject.harvest.config.parameters.PasswordParameter;
import de.gerdiproject.harvest.config.parameters.StringParameter;
import de.gerdiproject.harvest.config.parameters.UrlParameter;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.state.IState;
import de.gerdiproject.harvest.state.StateMachine;
import de.gerdiproject.harvest.state.impl.HarvestingState;
import de.gerdiproject.harvest.state.impl.InitializationState;
import de.gerdiproject.harvest.utils.FileUtils;
import de.gerdiproject.harvest.utils.data.DiskIO;
import de.gerdiproject.harvest.utils.logger.constants.LoggerConstants;

/**
 * This class contains unit tests for the {@linkplain Configuration}.
 * Each test is executed twice. The first execution tests harvester specific parameters,
 * while the second execution tests global parameters which are usually not modified
 * by harvester implementations.
 *
 * @author Robin Weiss
 */
@RunWith(Parameterized.class)
public class ConfigurationTest
{
    private static final String CUSTOM_PARAM_KEY = "customParam";
    private static final String CUSTOM_PARAM_VALUE = "customValue";

    private static final String URL_VALUE_1 = "http://www.gerdi-project.de";
    private static final String URL_VALUE_2 = "http://www.google.com";

    private static final String PASSWORD_VALUE_1 = "top secret";
    private static final String PASSWORD_VALUE_2 = "no one must know";

    private static final int INT_VALUE_1 = 42;
    private static final int INT_VALUE_2 = 1337;

    private static final boolean BOOL_VALUE_1 = false;
    private static final boolean BOOL_VALUE_2 = true;

    private static final File CACHE_FILE = new File("mocked/config.json");

    private static final String ERROR_ARGUMENTS_MUST_DIFFER = "The old and new value of Parameter Change tests must differ!";


    @Parameters(name = "{0}")
    public static Object[] getParameters()
    {
        return new Object[] {"harvester parameters", "global parameters"};
    }

    private GlobalParameterChangedEvent lastGlobalParamChange;
    private HarvesterParameterChangedEvent lastHarvesterParamChange;

    private final boolean isTestingHarvesterParameters;



    /**
     * Creates a configuration test, focussing on either testing harvester parameters
     * or global parameters.
     *
     * @param testingType a string defining which set of parameters is to be tested
     */
    public ConfigurationTest(String testingType)
    {
        this.lastGlobalParamChange = null;
        this.lastHarvesterParamChange = null;
        this.isTestingHarvesterParameters = testingType.equals("harvester parameters");
    }


    /**
     * Before each test:<br>
     * Adds event listeners for parameter change events and resets helper variables.
     *
     * @throws IOException thrown when the temporary cache file could not be deleted
     */
    @Before
    public void before() throws IOException
    {
        FileUtils.deleteFile(CACHE_FILE);

        if (CACHE_FILE.exists())
            throw new IOException();


        lastGlobalParamChange = null;
        lastHarvesterParamChange = null;

        EventSystem.addListener(GlobalParameterChangedEvent.class, this::onGlobalParameterChanged);
        EventSystem.addListener(HarvesterParameterChangedEvent.class, this::onHarvesterParameterChanged);
    }


    /**
     * After each test:<br>
     * Removes event listeners for parameter change events and deletes created files.
     */
    @After
    public void after()
    {
        EventSystem.removeAllListeners(GlobalParameterChangedEvent.class);
        EventSystem.removeAllListeners(HarvesterParameterChangedEvent.class);

        FileUtils.deleteFile(CACHE_FILE);
    }


    /**
     * Tests the constructor that requires both types of parameters by passing null
     * for each argument, and verifies that empty parameter maps are created.
     */
    @Test
    public void testNullConstructor()
    {
        final Configuration config = new Configuration(null, null);

        assertEquals(0, getTestedParameters(config).size());
    }


    /**
     * Tests the constructor that only requires harvester parameters by passing null
     * as argument, and verifies that default parameters are created.
     */
    @Test
    public void testNullHarvesterConstructor()
    {
        final Configuration config = new Configuration(null);

        assertNotEquals(0, getTestedParameters(config).size());
    }


    /**
     * Tests the constructor that requires both types of parameters by custom parameter objects.
     * The test is successful if both parameters can be retrieved from the {@linkplain Configuration}.
     */
    @Test
    public void testCustomParamsConstructor()
    {
        final AbstractParameter<?> param = new IntegerParameter(CUSTOM_PARAM_KEY, INT_VALUE_1);
        final Configuration config = createConfigWithCustomParameters(param);

        assertEquals(param.getValue(), config.getParameterValue(param.getKey(), param.getValue().getClass()));
    }


    /**
     * Tests the constructor that only requires harvester parameters by passing
     * three custom parameters.
     * The test is successful if all parameters can be retrieved from the {@linkplain Configuration}.
     */
    @Test
    public void testCustomHarvesterParamsConstructor()
    {
        final List<AbstractParameter<?>> customHarvesterParams =
            Arrays.asList(
                new StringParameter(CUSTOM_PARAM_KEY + 1, CUSTOM_PARAM_VALUE + 1),
                new StringParameter(CUSTOM_PARAM_KEY + 2, CUSTOM_PARAM_VALUE + 2),
                new StringParameter(CUSTOM_PARAM_KEY + 3, CUSTOM_PARAM_VALUE + 3)
            );

        final Configuration customConfig = new Configuration(customHarvesterParams);

        // check if all parameters can be retrieved
        for (AbstractParameter<?> param : customHarvesterParams)
            assertEquals(param.getValue(), customConfig.getParameterValue(param.getKey(), param.getValue().getClass()));
    }


    /**
     * Tests if the value of a {@linkplain StringParameter} can be changed via
     * the setter function of the {@linkplain Configuration}.
     */
    @Test
    public void testStringParameterChange()
    {
        final StringParameter param = new StringParameter(CUSTOM_PARAM_KEY, CUSTOM_PARAM_VALUE + 1);
        testParameterChange(param, CUSTOM_PARAM_VALUE + 2);
    }


    /**
     * Tests if the value of an {@linkplain IntegerParameter} can be changed via
     * the setter function of the {@linkplain Configuration}.
     */
    @Test
    public void testIntegerParameterChange()
    {
        final IntegerParameter param = new IntegerParameter(CUSTOM_PARAM_KEY, INT_VALUE_1);
        testParameterChange(param, INT_VALUE_2);
    }


    /**
     * Tests if the value of a {@linkplain BooleanParameter} can be changed via
     * the setter function of the {@linkplain Configuration}.
     */
    @Test
    public void testBooleanParameterChange()
    {
        final BooleanParameter param = new BooleanParameter(CUSTOM_PARAM_KEY, BOOL_VALUE_1);
        testParameterChange(param, BOOL_VALUE_2);
    }


    /**
     * Tests if the value of an {@linkplain UrlParameter} can be changed via
     * the setter function of the {@linkplain Configuration}.
     *
     * @throws MalformedURLException thrown when one of the test URLs is not well-formed
     */
    @Test
    public void testUrlParameterChange() throws MalformedURLException
    {
        final URL valueBefore = new URL(URL_VALUE_1);
        final URL valueAfter = new URL(URL_VALUE_2);

        final UrlParameter param = new UrlParameter(CUSTOM_PARAM_KEY, valueBefore.toString());
        testParameterChange(param, valueAfter);
    }


    /**
     * Tests if the value of a {@linkplain PasswordParameter} can be changed via
     * the setter function of the {@linkplain Configuration} and if it is always
     * hidden when being retrieved via its string value.
     */
    @Test
    public void testPasswordParameterChange()
    {
        final PasswordParameter param = new PasswordParameter(CUSTOM_PARAM_KEY, PASSWORD_VALUE_1);
        testParameterChange(param, PASSWORD_VALUE_2);
    }


    /**
     * Tests if the string representation of a {@linkplain PasswordParameter} masks the value.
     */
    @Test
    public void testPasswordParameterMasking()
    {
        final PasswordParameter param = new PasswordParameter(CUSTOM_PARAM_KEY, PASSWORD_VALUE_1);
        assert !param.getStringValue().contains(param.getValue().toString());
    }


    /**
     * Tests if parameter values do not change when being set in a state that is not allowed
     * by the parameter.
     */
    @Test
    public void testSettingInForbiddenState()
    {
        final Class<? extends IState> stateThatAllowsChanges = HarvestingState.class;
        final IState stateThatForbidsChanges = new InitializationState();

        assertFalse(canParameterBeSetInState(stateThatForbidsChanges, stateThatAllowsChanges));
    }


    /**
     * Tests if parameter values change when being set in a state that is allows
     * parameter changes.
     */
    @Test
    public void testSettingInAllowedState()
    {
        final IState stateThatAllowsChanges = new InitializationState();
        assert canParameterBeSetInState(stateThatAllowsChanges, stateThatAllowsChanges.getClass());
    }


    /**
     * Tests if parameter values change when being set in a null-state.
     */
    @Test
    public void testSettingInNullState()
    {
        final Class<? extends IState> stateThatAllowsChanges = HarvestingState.class;
        assert canParameterBeSetInState(null, stateThatAllowsChanges);
    }


    /**
     * Tests if parameter changes cause no events to be sent when the new parameter value
     * is the same as the old one.
     */
    @Test
    public void testParameterChangedEventOnSameValue()
    {
        final IntegerParameter param = new IntegerParameter(CUSTOM_PARAM_KEY, INT_VALUE_1);
        final Configuration config = createConfigWithCustomParameters(param);

        // check that no events are fired when the value does not change
        config.setParameter(param.getKey(), param.getStringValue());
        assertNull(getTestedEvent());
    }


    /**
     * Tests if parameter changes cause events to be sent when the value differs after the change,
     * and that the event payload contains the parameter that was changed.
     */
    @Test
    public void testParameterChangedEventPayloadParameter()
    {
        final StringParameter param = new StringParameter(CUSTOM_PARAM_KEY, CUSTOM_PARAM_VALUE + 1);
        final Configuration config = createConfigWithCustomParameters(param);

        final String newValue = CUSTOM_PARAM_VALUE + 2;

        // check GlobalParameterChanged event
        config.setParameter(param.getKey(), newValue);

        assertEquals(param, getTestedEvent().getParameter());
    }


    /**
     * Tests if parameter changes cause events to be sent when the value differs after the change,
     * and that the event payload contains the previous value of the parameter.
     */
    @Test
    public void testParameterChangedEventPayloadOldValue()
    {
        final StringParameter param = new StringParameter(CUSTOM_PARAM_KEY, CUSTOM_PARAM_VALUE + 1);
        final Configuration config = createConfigWithCustomParameters(param);

        final String oldValue = param.getStringValue();
        final String newValue = CUSTOM_PARAM_VALUE + 2;

        // check GlobalParameterChanged event
        config.setParameter(param.getKey(), newValue);

        assertEquals(oldValue, getTestedEvent().getOldValue());
    }


    /**
     * Tests if the saveToDisk function creates a file on disk.
     */
    @Test
    public void testSaveWithPath()
    {
        final Configuration config = createConfigWithCustomParameters(new StringParameter(CUSTOM_PARAM_KEY, CUSTOM_PARAM_VALUE));

        config.setCacheFilePath(CACHE_FILE.getAbsolutePath());
        config.saveToDisk();

        assert CACHE_FILE.exists() && CACHE_FILE.isFile();
    }


    /**
     * Tests if the saveToDisk function does not create a file on disk,
     * if no path was set in advance.
     */
    @Test
    public void testSaveWithoutPath()
    {
        final Configuration config = createConfigWithCustomParameters(new StringParameter(CUSTOM_PARAM_KEY, CUSTOM_PARAM_VALUE));

        LoggerConstants.ROOT_LOGGER.setLevel(Level.OFF);
        config.saveToDisk();
        LoggerConstants.ROOT_LOGGER.setLevel(Level.DEBUG);

        assert !CACHE_FILE.exists();
    }


    /**
     * Tests if loading a configuration will successfully apply previously
     * saved values while disregarding those whose keys are non-existing.
     */
    @Test
    public void testLoadingPresentValues()
    {
        final StringParameter param = new StringParameter(CUSTOM_PARAM_KEY, CUSTOM_PARAM_VALUE);
        final Object param1LoadedValue = param.getValue();

        // save a config with two custom parameters
        final Configuration savedConfig = createConfigWithCustomParameters(param);
        savedConfig.setCacheFilePath(CACHE_FILE.getAbsolutePath());
        savedConfig.saveToDisk();

        // create a config with only the first custom parameter
        final Configuration loadedConfig = createConfigWithCustomParameters(param);
        loadedConfig.setCacheFilePath(CACHE_FILE.getAbsolutePath());

        // change the value of the shared custom parameter
        final String valueToBeRestored = "override me";
        loadedConfig.setParameter(param.getKey(), valueToBeRestored);

        // load previously set up config
        LoggerConstants.ROOT_LOGGER.setLevel(Level.OFF);
        loadedConfig.loadFromDisk();
        LoggerConstants.ROOT_LOGGER.setLevel(Level.DEBUG);

        // make sure the value of custom parameter was changed due to the loading
        assertEquals(param1LoadedValue, loadedConfig.getParameterValue(param.getKey(), param.getValue().getClass()));
    }


    /**
     * Tests if loading a configuration will not add values that do not already
     * exist.
     */
    @Test
    public void testLoadingNonPresentValues()
    {
        final StringParameter param = new StringParameter(CUSTOM_PARAM_KEY, CUSTOM_PARAM_VALUE);

        // save a config with two custom parameters
        final Configuration savedConfig = createConfigWithCustomParameters(param);
        savedConfig.setCacheFilePath(CACHE_FILE.getAbsolutePath());
        savedConfig.saveToDisk();

        // create a config with only the first custom parameter
        final Configuration loadedConfig = new Configuration(null);
        loadedConfig.setCacheFilePath(CACHE_FILE.getAbsolutePath());

        // load previously set up config
        LoggerConstants.ROOT_LOGGER.setLevel(Level.OFF);
        loadedConfig.loadFromDisk();
        LoggerConstants.ROOT_LOGGER.setLevel(Level.DEBUG);

        // make sure custom parameter 2 was not created
        assertNull(loadedConfig.getParameterStringValue(param.getKey()));
    }


    /**
     * Tests if a loading function causes no changes if a path does not exist.
     */
    @Test
    public void testLoadWithNonExistingPath()
    {
        final StringParameter param = new StringParameter(CUSTOM_PARAM_KEY, CUSTOM_PARAM_VALUE);

        // save a config with two custom parameters
        final Configuration loadedConfig = createConfigWithCustomParameters(param);
        loadedConfig.setCacheFilePath(CACHE_FILE.getAbsolutePath());

        // attempt to load a non-existing path
        LoggerConstants.ROOT_LOGGER.setLevel(Level.OFF);
        loadedConfig.loadFromDisk();
        LoggerConstants.ROOT_LOGGER.setLevel(Level.DEBUG);

        // make sure the old custom parameter still exists
        assertEquals(param.getStringValue(), loadedConfig.getParameterStringValue(param.getKey()));
    }


    /**
     * Tests if a loading function causes no changes if a path was not set
     * in advance.
     */
    @Test
    public void testLoadWithoutSetPath()
    {
        final AbstractParameter<?> param =
            new StringParameter(CUSTOM_PARAM_KEY, CUSTOM_PARAM_VALUE);

        // save a config with a custom parameter
        final Configuration savedConfig = createConfigWithCustomParameters(param);
        savedConfig.setCacheFilePath(CACHE_FILE.getAbsolutePath());
        savedConfig.saveToDisk();

        // create config with the same custom parameter, but a different value
        final String valueToBeOverridden = "override me";
        param.setValue(valueToBeOverridden, null);
        final Configuration loadedConfig = createConfigWithCustomParameters(param);

        // attempt to load without setting a path
        LoggerConstants.ROOT_LOGGER.setLevel(Level.OFF);
        loadedConfig.loadFromDisk();
        LoggerConstants.ROOT_LOGGER.setLevel(Level.DEBUG);

        // make sure the old custom parameter value was not overridden
        assertEquals(valueToBeOverridden, loadedConfig.getParameterStringValue(param.getKey()));
    }


    /**
     * Tests if a Configuration can be serialized and deserialized without losing data.
     */
    @Test
    public void testJsonSerializationParameterTypes()
    {
        // save config with all parameters to disk
        final Configuration savedConfig = createConfigWithAllParameterTypes();
        final Configuration loadedConfig = saveAndLoadConfig(savedConfig);

        // check if deserialized global parameters are correct
        final Map<String, AbstractParameter<?>> loadedGlobalParams = getTestedParameters(loadedConfig);
        getTestedParameters(savedConfig).forEach((String key, AbstractParameter<?> param) -> {
            assertEquals(param.getClass(), loadedGlobalParams.get(key).getClass());
        });
    }


    /**
     * Tests if a Configuration can be serialized and deserialized
     * while preserving the parameter types.
     */
    @Test
    public void testJsonSerializationParameterValues()
    {
        // save config with all parameters to disk
        final Configuration savedConfig = createConfigWithAllParameterTypes();
        final Configuration loadedConfig = saveAndLoadConfig(savedConfig);

        // check if deserialized global parameters are correct
        final Map<String, AbstractParameter<?>> loadedGlobalParams = getTestedParameters(loadedConfig);
        getTestedParameters(savedConfig).forEach((String key, AbstractParameter<?> param) -> {
            assertEquals(param.getValue(), loadedGlobalParams.get(key).getValue());
        });
    }


    /**
     * Tests if the {@linkplain Configuration}'s updateParameter() function sends events for
     * both harvester- and global parameters.
     */
    @Test
    public void testParameterUpdate()
    {
        final AbstractParameter<?> param =
            new StringParameter(CUSTOM_PARAM_KEY, CUSTOM_PARAM_VALUE);

        final Configuration config = createConfigWithCustomParameters(param);

        // check GlobalParameterChanged event
        config.updateParameter(param.getKey());
        assertEquals(param, getTestedEvent().getParameter());
    }


    /**
     * Tests if the {@linkplain Configuration}'s updateAllParameters() function sends events for
     * all parameters.
     */
    @Test
    public void testAllParametersUpdate()
    {
        final AbstractParameter<?> param =
            new StringParameter(CUSTOM_PARAM_KEY, CUSTOM_PARAM_VALUE);

        final Configuration config = createConfigWithCustomParameters(param);

        // check GlobalParameterChanged event
        config.updateAllParameters();
        assertEquals(param, getTestedEvent().getParameter());
    }


    /**
     * Tests if the {@linkplain Configuration}'s toString() function displays all parameter keys.
     */
    @Test
    public void testToStringKeys()
    {
        final Configuration config = createConfigWithAllParameterTypes();
        final String configString = config.toString();

        getTestedParameters(config).forEach((String key, AbstractParameter<?> param) -> {
            assert configString.contains(key);
        });
    }


    /**
     * Tests if the {@linkplain Configuration}'s toString() function displays all parameter values.
     */
    @Test
    public void testToStringValues()
    {
        final Configuration config = createConfigWithAllParameterTypes();
        final String configString = config.toString();

        getTestedParameters(config).forEach((String key, AbstractParameter<?> param) -> {
            assert configString.contains(param.getStringValue());
        });
    }


    //////////////////////
    // Non-test Methods //
    //////////////////////

    /**
     * Event callback function that stores the event that was dispatched.
     * @param e the event that triggered this callback function
     */
    private void onGlobalParameterChanged(GlobalParameterChangedEvent e)
    {
        lastGlobalParamChange = e;
    }


    /**
     * Event callback function that stores the event that was dispatched.
     * @param e the event that triggered this callback function
     */
    private void onHarvesterParameterChanged(HarvesterParameterChangedEvent e)
    {
        lastHarvesterParamChange = e;
    }


    /**
     * Helper function that tests if a parameter can be successfully changed via the
     * dedicated Configuration method.
     *
     * @param param the parameter of which the value will be changed
     * @param newValue the new value of the parameter after the change
     *
     * @throws IllegalArgumentException thrown when the new and the old value are the same,
     *          rendering this test useless
     */
    private <T> void testParameterChange(AbstractParameter<T> param, T newValue)
    {
        final String key = param.getKey();
        final T oldValue = param.getValue();

        if (oldValue.equals(newValue))
            throw new IllegalArgumentException(ERROR_ARGUMENTS_MUST_DIFFER);

        final Configuration config = createConfigWithCustomParameters(param);

        config.setParameter(key, newValue.toString());
        assertNotEquals(oldValue, config.getParameterValue(key, newValue.getClass()));
    }


    /**
     * Creates a {@linkplain Configuration} that has either global-
     * or a harvester parameters, depending on which type is being tested.
     *
     * @param params the parameters to be added to the config
     *
     * @return a {@linkplain Configuration} with the specified {@linkplain AbstractParameter}s
     */
    private Configuration createConfigWithCustomParameters(AbstractParameter<?>... params)
    {
        final Map<String, AbstractParameter<?>> parameterMap = new HashMap<>();

        for (AbstractParameter<?> p : params)
            parameterMap.put(p.getKey(), p);

        return isTestingHarvesterParameters
               ? new Configuration(null, parameterMap)
               : new Configuration(parameterMap, null);
    }


    /**
     * Creates a {@linkplain Configuration} that has one of each parameter types for harvester- and global
     * parameters.
     *
     * @return a {@linkplain Configuration} with all {@linkplain AbstractParameter} subclasses
     */
    private Configuration createConfigWithAllParameterTypes()
    {
        final StringParameter globalStringParam = new StringParameter(CUSTOM_PARAM_KEY + 1, CUSTOM_PARAM_VALUE + 1);
        final IntegerParameter globalIntegerParam = new IntegerParameter(CUSTOM_PARAM_KEY + 2, INT_VALUE_1);
        final BooleanParameter globalBooleanParam = new BooleanParameter(CUSTOM_PARAM_KEY + 3, BOOL_VALUE_1);
        final UrlParameter globalUrlParam = new UrlParameter(CUSTOM_PARAM_KEY + 4, URL_VALUE_1);
        final PasswordParameter globalPasswordParam = new PasswordParameter(CUSTOM_PARAM_KEY + 5, PASSWORD_VALUE_1);
        globalPasswordParam.setValue("top secret", null);

        final Map<String, AbstractParameter<?>> globalParams = new HashMap<>();
        globalParams.put(globalStringParam.getKey(), globalStringParam);
        globalParams.put(globalIntegerParam.getKey(), globalIntegerParam);
        globalParams.put(globalBooleanParam.getKey(), globalBooleanParam);
        globalParams.put(globalUrlParam.getKey(), globalUrlParam);
        globalParams.put(globalPasswordParam.getKey(), globalPasswordParam);

        final StringParameter harvesterStringParam = new StringParameter(CUSTOM_PARAM_KEY + 6, CUSTOM_PARAM_VALUE + 2);
        final IntegerParameter harvesterIntegerParam = new IntegerParameter(CUSTOM_PARAM_KEY + 7, INT_VALUE_2);
        final BooleanParameter harvesterBooleanParam = new BooleanParameter(CUSTOM_PARAM_KEY + 8, BOOL_VALUE_2);
        final UrlParameter harvesterUrlParam = new UrlParameter(CUSTOM_PARAM_KEY + 9, URL_VALUE_2);
        final PasswordParameter harvesterPasswordParam = new PasswordParameter(CUSTOM_PARAM_KEY + 10, PASSWORD_VALUE_2);

        final Map<String, AbstractParameter<?>> harvesterParams = new HashMap<>();
        harvesterParams.put(harvesterStringParam.getKey(), harvesterStringParam);
        harvesterParams.put(harvesterIntegerParam.getKey(), harvesterIntegerParam);
        harvesterParams.put(harvesterBooleanParam.getKey(), harvesterBooleanParam);
        harvesterParams.put(harvesterUrlParam.getKey(), harvesterUrlParam);
        harvesterParams.put(harvesterPasswordParam.getKey(), harvesterPasswordParam);

        return new Configuration(globalParams, harvesterParams);
    }

    /**
     * Saves a {@linkplain Configuration} and loads it by parsing the saved
     * JSON object.
     *
     * @param configToSave the configuration that is to be saved
     * @return the configuration that was parsed from the saved JSON object
     */
    private Configuration saveAndLoadConfig(Configuration configToSave)
    {
        // save config with all parameters to disk
        configToSave.setCacheFilePath(CACHE_FILE.getAbsolutePath());
        configToSave.saveToDisk();

        // deserialize cache file
        final Gson gson =
            new GsonBuilder()
        .registerTypeAdapter(Configuration.class, new ConfigurationAdapter())
        .create();
        final DiskIO diskIo = new DiskIO(gson, StandardCharsets.UTF_8);
        return diskIo.getObject(CACHE_FILE, Configuration.class);
    }


    /**
     * Retrieves either the map of harvester- or global parameters, depending on what is being tested.
     * @param config the {@linkplain Configuration} from which the parameter map is retrieved
     *
     * @return the map of harvester- or global parameters from the specified config
     */
    private Map<String, AbstractParameter<?>> getTestedParameters(final Configuration config)
    {
        return isTestingHarvesterParameters
               ? config.getHarvesterParameters()
               : config.getGlobalParameters();
    }


    /**
     * Retrieves the cached parameter change event that corresponds to either harvester- or global parameter changes,
     * depending on what is being tested.
     *
     * @return the map of harvester- or global parameters from the specified config
     */
    private GlobalParameterChangedEvent getTestedEvent()
    {
        return isTestingHarvesterParameters
               ? lastHarvesterParamChange
               : lastGlobalParamChange;
    }


    /**
     * Tests if parameter values can change when being set in a specified state.
     *
     * @param testedState the state in which the parameter change is attempted
     * @param allowedState a state in which a parameter change is allowed
     *
     * @return true if the parameter value can change
     */
    private boolean canParameterBeSetInState(IState testedState, Class<? extends IState> allowedState)
    {
        final String valueBefore = CUSTOM_PARAM_VALUE + 1;
        final String valueAfter = CUSTOM_PARAM_VALUE + 2;
        final String key = CUSTOM_PARAM_KEY;


        final StringParameter param = new StringParameter(key, Arrays.asList(allowedState), valueBefore);
        final Configuration config = createConfigWithCustomParameters(param);

        // explicitly test a state that is not among the valid states of the parameter
        StateMachine.setState(testedState);
        config.setParameter(key, valueAfter);
        StateMachine.setState(null);

        return param.getValue().equals(valueAfter);
    }
}
