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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import ch.qos.logback.classic.Level;
import de.gerdiproject.harvest.config.Configuration;
import de.gerdiproject.harvest.config.adapter.ConfigurationAdapter;
import de.gerdiproject.harvest.config.constants.ConfigurationConstants;
import de.gerdiproject.harvest.config.events.GlobalParameterChangedEvent;
import de.gerdiproject.harvest.config.events.HarvesterParameterChangedEvent;
import de.gerdiproject.harvest.config.parameters.AbstractParameter;
import de.gerdiproject.harvest.config.parameters.BooleanParameter;
import de.gerdiproject.harvest.config.parameters.IntegerParameter;
import de.gerdiproject.harvest.config.parameters.PasswordParameter;
import de.gerdiproject.harvest.config.parameters.StringParameter;
import de.gerdiproject.harvest.config.parameters.UrlParameter;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.state.StateMachine;
import de.gerdiproject.harvest.state.impl.HarvestingState;
import de.gerdiproject.harvest.state.impl.InitializationState;
import de.gerdiproject.harvest.utils.data.DiskIO;
import de.gerdiproject.harvest.utils.logger.constants.LoggerConstants;

/**
 * This class contains unit tests for the {@linkplain Configuration}.
 *
 * @author Robin Weiss
 */
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

    private GlobalParameterChangedEvent lastGlobalParamChange;
    private HarvesterParameterChangedEvent lastHarvesterParamChange;


    /**
     * Before each test:<br>
     * Adds event listeners for parameter change events and resets helper variables.
     */
    @Before
    public void before()
    {
        lastGlobalParamChange = null;
        lastHarvesterParamChange = null;

        EventSystem.addListener(GlobalParameterChangedEvent.class, this::onGlobalParameterChanged);
        EventSystem.addListener(HarvesterParameterChangedEvent.class, this::onHarvesterParameterChanged);
    }


    /**
     * After each test:<br>
     * Removes event listeners for parameter change events and deletes created files.
     * @throws IOException thrown when the temporary cache file could not be deleted
     */
    @After
    public void after() throws IOException
    {
        EventSystem.removeAllListeners(GlobalParameterChangedEvent.class);
        EventSystem.removeAllListeners(HarvesterParameterChangedEvent.class);

        if (CACHE_FILE.exists() && !CACHE_FILE.delete())
            throw new IOException();
    }


    /**
     * Tests the constructor that requires both types of parameters by passing null
     * for each argument, and verifies that empty parameter maps are created.
     */
    @Test
    public void testNullConstructor()
    {
        final Configuration config = new Configuration(null, null);

        assertEquals(0, config.getGlobalParameters().size());
        assertEquals(0, config.getHarvesterParameters().size());
    }


    /**
     * Tests the constructor that requires both types of parameters by custom parameter objects.
     * The test is successful if both parameters can be retrieved from the {@linkplain Configuration}.
     */
    @Test
    public void testCustomParamsConstructor()
    {
        final AbstractParameter<?> globalParam = new StringParameter(CUSTOM_PARAM_KEY + 1, CUSTOM_PARAM_VALUE + 1);
        final Map<String, AbstractParameter<?>> customGlobalParams = new HashMap<>();
        customGlobalParams.put(globalParam.getKey(), globalParam);

        final AbstractParameter<?> harvesterParam = new StringParameter(CUSTOM_PARAM_KEY + 2, CUSTOM_PARAM_VALUE + 2);
        final Map<String, AbstractParameter<?>> customHarvesterParams = new HashMap<>();
        customHarvesterParams.put(harvesterParam.getKey(), harvesterParam);

        final Configuration customConfig = new Configuration(customGlobalParams, customHarvesterParams);

        // verify that only one of each parameters were created
        assertEquals(1, customConfig.getGlobalParameters().size());
        assertEquals(1, customConfig.getHarvesterParameters().size());

        // check if all parameters can be retrieved
        assertEquals(globalParam.getValue(), customConfig.getParameterValue(globalParam.getKey(), globalParam.getValue().getClass()));
        assertEquals(harvesterParam.getValue(), customConfig.getParameterValue(harvesterParam.getKey(), harvesterParam.getValue().getClass()));
    }


    /**
     * Tests the constructor that only requires harvester parameters by passing null
     * as argument, and verifies that default parameters are created.
     */
    @Test
    public void testNullHarvesterConstructor()
    {
        final Configuration config = new Configuration(null);

        assertNotEquals(0, config.getGlobalParameters().size());
        assertNotEquals(0, config.getHarvesterParameters().size());
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
        final Configuration defaultConfig = new Configuration(null);

        assertEquals(defaultConfig.getHarvesterParameters().size() + customHarvesterParams.size(),
                     customConfig.getHarvesterParameters().size());

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
     */
    @Test
    public void testUrlParameterChange()
    {
        try {
            final URL valueBefore = new URL(URL_VALUE_1);
            final URL valueAfter = new URL(URL_VALUE_2);

            final UrlParameter param = new UrlParameter(CUSTOM_PARAM_KEY, valueBefore.toString());
            testParameterChange(param, valueAfter);
        } catch (MalformedURLException e) {
            assert false;
        }
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

        assertEquals(ConfigurationConstants.PASSWORD_STRING_TEXT, param.getStringValue());
        testParameterChange(param, PASSWORD_VALUE_2);
        assertEquals(ConfigurationConstants.PASSWORD_STRING_TEXT, param.getStringValue());
    }


    /**
     * Tests if parameter values do not change when being set in a state that is not allowed
     * by the parameter.
     */
    @Test
    public void testSettingInForbiddenState()
    {
        final String valueBefore = "before";
        final String valueAfter = "after";

        final StringParameter param = new StringParameter(CUSTOM_PARAM_KEY, Arrays.asList(HarvestingState.class), valueBefore);
        final String key = param.getKey();

        final Configuration config = new Configuration(Arrays.asList(param));

        assertEquals(valueBefore, config.getParameterValue(key, valueBefore.getClass()));
        assertNotEquals(valueAfter, config.getParameterValue(key, valueAfter.getClass()));

        // explicitly test a state that is not among the valid states of the parameter
        StateMachine.setState(new InitializationState());
        config.setParameter(key, valueAfter);
        StateMachine.setState(null);

        assertEquals(valueBefore, config.getParameterValue(key, valueBefore.getClass()));
        assertNotEquals(valueAfter, config.getParameterValue(key, valueAfter.getClass()));
    }


    /**
     * Tests if parameter changes cause events to be sent when the value differs after the change,
     * and no events to be sent when the value remains unchanged.
     */
    @Test
    public void testParameterChangedEvents()
    {
        final Configuration config = createConfigWithTwoCustomParameters();
        final AbstractParameter<?> globalParam = (AbstractParameter<?>) config.getGlobalParameters().values().toArray()[0];
        final AbstractParameter<?> harvesterParam = (AbstractParameter<?>) config.getHarvesterParameters().values().toArray()[0];

        final String oldGlobalValue = globalParam.getStringValue();
        final String oldHarvesterValue = harvesterParam.getStringValue();
        final String newValue = "newVal";

        // check if helper variables were reset correctly
        assertNull(lastGlobalParamChange);
        assertNull(lastHarvesterParamChange);

        // check that no events are fired when the value does not change
        config.setParameter(globalParam.getKey(), globalParam.getStringValue());
        assertNull(lastGlobalParamChange);

        config.setParameter(harvesterParam.getKey(), harvesterParam.getStringValue());
        assertNull(lastHarvesterParamChange);

        // check GlobalParameterChanged event
        config.setParameter(globalParam.getKey(), newValue);
        assertEquals(globalParam, lastGlobalParamChange.getParameter());
        assertEquals(oldGlobalValue, lastGlobalParamChange.getOldValue());

        // check HarvesterParameterChanged event
        config.setParameter(harvesterParam.getKey(), newValue);
        assertEquals(harvesterParam, lastHarvesterParamChange.getParameter());
        assertEquals(oldHarvesterValue, lastHarvesterParamChange.getOldValue());
    }


    /**
     * Tests if the saveToDisk function creates a file on disk.
     */
    @Test
    public void testSaveWithPath()
    {
        assert !CACHE_FILE.exists();

        final Configuration config = new Configuration(null);
        config.setCacheFilePath(CACHE_FILE.getAbsolutePath());

        config.saveToDisk();

        assert CACHE_FILE.exists();
        assert CACHE_FILE.isFile();
    }


    /**
     * Tests if the saveToDisk function does not create a file on disk,
     * if no path was set in advance.
     */
    @Test
    public void testSaveWithoutPath()
    {
        assert !CACHE_FILE.exists();

        final Configuration config = new Configuration(null);

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
    public void testLoad()
    {
        final Configuration savedConfig = createConfigWithTwoCustomParameters();
        final AbstractParameter<?> harvesterParam = (AbstractParameter<?>) savedConfig.getHarvesterParameters().values().toArray()[0];
        final AbstractParameter<?> globalParam = (AbstractParameter<?>) savedConfig.getGlobalParameters().values().toArray()[0];
        final Object restoredValue = harvesterParam.getValue();

        savedConfig.setCacheFilePath(CACHE_FILE.getAbsolutePath());
        savedConfig.saveToDisk();

        // set up the config to be loaded to only use one of the two custom parameters
        final Configuration loadedConfig = new Configuration(Arrays.asList(harvesterParam));

        // change the value of the shared custom parameter
        final String valueToBeRestored = "override me";
        loadedConfig.setParameter(harvesterParam.getKey(), valueToBeRestored);
        assertEquals(valueToBeRestored, loadedConfig.getParameterStringValue(harvesterParam.getKey()));

        // load previously set up config
        LoggerConstants.ROOT_LOGGER.setLevel(Level.OFF);
        loadedConfig.setCacheFilePath(CACHE_FILE.getAbsolutePath());
        loadedConfig.loadFromDisk();
        LoggerConstants.ROOT_LOGGER.setLevel(Level.DEBUG);

        // make sure custom parameter 2 was not created
        assertNull(loadedConfig.getParameterStringValue(globalParam.getKey()));

        // make sure the value of custom parameter 1 was changed
        assertEquals(restoredValue, loadedConfig.getParameterValue(harvesterParam.getKey(), harvesterParam.getValue().getClass()));
    }


    /**
     * Tests if a loading function causes no changes if a path does not exist.
     */
    @Test
    public void testLoadWithNonExistingPath()
    {
        final AbstractParameter<?> customParam =
            new StringParameter(CUSTOM_PARAM_KEY, CUSTOM_PARAM_VALUE);

        final Configuration loadedConfig = new Configuration(Arrays.asList(customParam));
        loadedConfig.setCacheFilePath(CACHE_FILE.getAbsolutePath());

        // make sure the cache file really does not exist
        assert !CACHE_FILE.exists();

        // attempt to load a non-existing path
        LoggerConstants.ROOT_LOGGER.setLevel(Level.OFF);
        loadedConfig.loadFromDisk();
        LoggerConstants.ROOT_LOGGER.setLevel(Level.DEBUG);

        // make sure the old custom parameter still exists
        assertEquals(customParam.getStringValue(), loadedConfig.getParameterStringValue(customParam.getKey()));
    }


    /**
     * Tests if a loading function causes no changes if a path was not set
     * in advance.
     */
    @Test
    public void testLoadWithoutSetPath()
    {
        final AbstractParameter<?> customParam =
            new StringParameter(CUSTOM_PARAM_KEY, CUSTOM_PARAM_VALUE);

        // save a config with a custom parameter
        final Configuration savedConfig = new Configuration(Arrays.asList(customParam));
        savedConfig.setCacheFilePath(CACHE_FILE.getAbsolutePath());
        savedConfig.saveToDisk();

        // make sure the saved file exists
        assert CACHE_FILE.exists();

        // create config with the same custom parameter, but a different value
        final String valueToBeOverridden = "override me";
        customParam.setValue(valueToBeOverridden, null);
        final Configuration loadedConfig = new Configuration(Arrays.asList(customParam));

        // attempt to load without setting a path
        LoggerConstants.ROOT_LOGGER.setLevel(Level.OFF);
        loadedConfig.loadFromDisk();
        LoggerConstants.ROOT_LOGGER.setLevel(Level.DEBUG);

        // make sure the old custom parameter value was not overridden
        assertEquals(valueToBeOverridden, loadedConfig.getParameterStringValue(customParam.getKey()));
    }


    /**
     * Tests if all the Configuration can be serialized and deserialized without losing data and
     * while preserving parameter types.
     */
    @Test
    public void testJsonSerialization()
    {
        assert !CACHE_FILE.exists();

        final Configuration savedConfig = createConfigWithAllParameterTypes();
        savedConfig.setCacheFilePath(CACHE_FILE.getAbsolutePath());
        savedConfig.saveToDisk();

        assert CACHE_FILE.exists();

        // deserialize cache file
        Gson gson = new GsonBuilder().registerTypeAdapter(Configuration.class, new ConfigurationAdapter()).create();
        DiskIO diskIo = new DiskIO(gson, StandardCharsets.UTF_8);
        final Configuration loadedConfig = diskIo.getObject(CACHE_FILE, Configuration.class);

        // check if the number of parameters is correct
        assertEquals(savedConfig.getGlobalParameters().size(), loadedConfig.getGlobalParameters().size());
        assertEquals(savedConfig.getHarvesterParameters().size(), loadedConfig.getHarvesterParameters().size());

        // check if deserialized global parameters are correct
        final Map<String, AbstractParameter<?>> loadedGlobalParams = loadedConfig.getGlobalParameters();
        savedConfig.getGlobalParameters().forEach((String key, AbstractParameter<?> param) -> {
            assertEquals(param.getClass(), loadedGlobalParams.get(key).getClass());
            assertEquals(param.getValue(), loadedGlobalParams.get(key).getValue());
        });

        // check if deserialized harvester parameters are correct
        final Map<String, AbstractParameter<?>> loadedHarvesterParams = loadedConfig.getHarvesterParameters();
        savedConfig.getHarvesterParameters().forEach((String key, AbstractParameter<?> param) -> {
            assertEquals(param.getClass(), loadedHarvesterParams.get(key).getClass());
            assertEquals(param.getValue(), loadedHarvesterParams.get(key).getValue());
        });
    }


    /**
     * Tests if the {@linkplain Configuration}'s updateParameter() function sends events for
     * both harvester- and global parameters.
     */
    @Test
    public void testParameterUpdate()
    {
        final Configuration config = createConfigWithTwoCustomParameters();
        final AbstractParameter<?> globalParam = (AbstractParameter<?>) config.getGlobalParameters().values().toArray()[0];
        final AbstractParameter<?> harvesterParam = (AbstractParameter<?>) config.getHarvesterParameters().values().toArray()[0];

        // check if helper variables were reset correctly
        assertNull(lastGlobalParamChange);
        assertNull(lastHarvesterParamChange);

        // check GlobalParameterChanged event
        config.updateParameter(globalParam.getKey());
        assertNull(lastGlobalParamChange.getOldValue());
        assertEquals(globalParam, lastGlobalParamChange.getParameter());

        // check HarvesterParameterChanged event
        config.updateParameter(harvesterParam.getKey());
        assertNull(lastHarvesterParamChange.getOldValue());
        assertEquals(harvesterParam, lastHarvesterParamChange.getParameter());
    }


    /**
     * Tests if the {@linkplain Configuration}'s updateAllParameters() function sends events for
     * all parameters.
     */
    @Test
    public void testAllParametersUpdate()
    {
        final Configuration config = createConfigWithTwoCustomParameters();
        final AbstractParameter<?> globalParam = (AbstractParameter<?>) config.getGlobalParameters().values().toArray()[0];
        final AbstractParameter<?> harvesterParam = (AbstractParameter<?>) config.getHarvesterParameters().values().toArray()[0];

        // check if helper variables were reset correctly
        assertNull(lastGlobalParamChange);
        assertNull(lastHarvesterParamChange);

        config.updateAllParameters();

        // check GlobalParameterChanged event
        assertNull(lastGlobalParamChange.getOldValue());
        assertEquals(globalParam, lastGlobalParamChange.getParameter());

        // check HarvesterParameterChanged event
        assertNull(lastHarvesterParamChange.getOldValue());
        assertEquals(harvesterParam, lastHarvesterParamChange.getParameter());
    }


    /**
     * Tests if the {@linkplain Configuration}'s toString() function displays all parameter keys
     * and string values
     */
    @Test
    public void testToString()
    {
        final Configuration config = createConfigWithAllParameterTypes();
        final String configString = config.toString();

        config.getGlobalParameters().forEach((String key, AbstractParameter<?> param) -> {
            assert configString.contains(key);
            assert configString.contains(param.getStringValue());
        });

        config.getHarvesterParameters().forEach((String key, AbstractParameter<?> param) -> {
            assert configString.contains(key);
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
     */
    private <T> void testParameterChange(AbstractParameter<T> param, T newValue)
    {
        final String key = param.getKey();
        final T oldValue = param.getValue();

        final Configuration config = new Configuration(Arrays.asList(param));

        assertEquals(oldValue, config.getParameterValue(key, oldValue.getClass()));
        assertNotEquals(newValue, config.getParameterValue(key, oldValue.getClass()));

        config.setParameter(key, newValue.toString());

        assertEquals(newValue, config.getParameterValue(key, newValue.getClass()));
        assertNotEquals(oldValue, config.getParameterValue(key, newValue.getClass()));
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
     * Creates a {@linkplain Configuration} with one custom global- and one custom harvester parameter.
     *
     * @return a {@linkplain Configuration} with two parameters
     */
    private Configuration createConfigWithTwoCustomParameters()
    {
        final AbstractParameter<?> globalParam = new StringParameter(CUSTOM_PARAM_KEY + 1, CUSTOM_PARAM_VALUE + 1);
        final Map<String, AbstractParameter<?>> customGlobalParams = new HashMap<>();
        customGlobalParams.put(globalParam.getKey(), globalParam);

        final AbstractParameter<?> harvesterParam = new StringParameter(CUSTOM_PARAM_KEY + 2, CUSTOM_PARAM_VALUE + 2);
        final Map<String, AbstractParameter<?>> customHarvesterParams = new HashMap<>();
        customHarvesterParams.put(harvesterParam.getKey(), harvesterParam);

        return new Configuration(customGlobalParams, customHarvesterParams);
    }
}
