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
package de.gerdiproject.harvest.config; // NOPMD JUnit 4 requires many static imports

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import de.gerdiproject.harvest.AbstractObjectUnitTest;
import de.gerdiproject.harvest.config.json.adapters.ConfigurationAdapter;
import de.gerdiproject.harvest.config.parameters.AbstractParameter;
import de.gerdiproject.harvest.config.parameters.BooleanParameter;
import de.gerdiproject.harvest.config.parameters.IntegerParameter;
import de.gerdiproject.harvest.config.parameters.PasswordParameter;
import de.gerdiproject.harvest.config.parameters.StringParameter;
import de.gerdiproject.harvest.utils.data.DiskIO;

/**
 * This class contains unit tests for the {@linkplain Configuration}.
 * Each test is executed twice. The first execution tests harvester specific parameters,
 * while the second execution tests global parameters which are usually not modified
 * by harvester implementations.
 *
 * @author Robin Weiss
 */
public class ConfigurationTest extends AbstractObjectUnitTest<Configuration>
{
    private static final String MODULE_NAME = "mocked";
    private static final String PARAM_KEY = "customParam";
    private static final String STRING_VALUE = "customValue";
    private static final String PASSWORD_VALUE_1 = "top secret";
    private static final String PASSWORD_VALUE_2 = "no one must know";
    private static final int INT_VALUE_1 = 42;
    private static final int INT_VALUE_2 = 1337;
    private static final boolean BOOL_VALUE_1 = false;
    private static final boolean BOOL_VALUE_2 = true;
    private static final String TEST_CATEGORY = "TestCategory";
    private static final String ERROR_ARGUMENTS_MUST_DIFFER = "The old and new value of parameter change tests must differ!";
    private static final String ERROR_MISSING_LOADED_PARAM  = "Expected parameter '%s' to be loaded after being saved!";

    private final File configFile = new File(getTemporaryTestDirectory(), "config.json");
    private StringParameter testedParam;


    @Override
    protected Configuration setUpTestObjects()
    {
        testedParam = new StringParameter(PARAM_KEY, TEST_CATEGORY, STRING_VALUE);
        return createConfigWithCustomParameters(testedParam);
    }


    /**
     * Tests the constructor that expects no arguments and verifies that
     * it contains no parameters.
     */
    @Test
    public void testNullConstructor()
    {
        testedObject = new Configuration(MODULE_NAME);
        assertEquals("The method getParameters() should return an empty list after the constructor without arguments is called!",
                     0,
                     testedObject.getParameters().size());
    }


    /**
     * Tests the constructor that requires parameters by passing three parameters.
     * The test is successful if all parameters can be retrieved from the {@linkplain Configuration}.
     */
    @Test
    public void testCustomHarvesterParamsConstructor()
    {
        final AbstractParameter<?>[] customHarvesterParams = {
            new StringParameter(PARAM_KEY + 1, TEST_CATEGORY, STRING_VALUE + 1),
            new StringParameter(PARAM_KEY + 2, TEST_CATEGORY, STRING_VALUE + 2),
            new StringParameter(PARAM_KEY + 3, TEST_CATEGORY, STRING_VALUE + 3)
        };

        testedObject = new Configuration(MODULE_NAME, customHarvesterParams);

        // check if all parameters can be retrieved
        for (AbstractParameter<?> param : customHarvesterParams)
            assertEquals(
                "The method getParameterValue() should return a list of all parameters that were passed to the constructor",
                param.getValue(),
                testedObject.getParameterValue(param.getCompositeKey()));
    }


    /**
     * Tests if the case of parameter keys is ignored when trying to retrieve a parameter
     * from the {@linkplain Configuration}.
     */
    @Test
    public void testGettingParameterIgnoreCase()
    {
        final String randomValue = STRING_VALUE + random.nextInt(10000);
        final AbstractParameter<?> paramWithCase = new StringParameter(PARAM_KEY, TEST_CATEGORY, randomValue);

        testedObject = new Configuration(MODULE_NAME, paramWithCase);

        final String upperCaseKey = paramWithCase.getCompositeKey().toUpperCase();
        assertEquals("The method getParameterValue() should be case insensitive!",
                     randomValue,
                     testedObject.getParameterValue(upperCaseKey));
    }


    /**
     * Tests if registering the exact same instance of an a parameter that is already defined
     * in the {@linkplain Configuration} will return the same instance.
     */
    @Test
    public void testRegisteringKnownParam()
    {
        testedObject.addEventListeners();
        final AbstractParameter<?> registeredParam = Configuration.registerParameter(testedParam);

        assertEquals("The static method registerParameter() should return the same instance of a parameter if it was already added to the Configuration!",
                     registeredParam,
                     testedParam);
    }


    /**
     * Tests if registering multiple instances of the same parameter will always return the same reference
     * from the {@linkplain Configuration}.
     */
    @Test
    public void testRegisteringParamWithTheSameCompositeKey()
    {
        testedObject.addEventListeners();
        final AbstractParameter<?> registeredParam1 = Configuration.registerParameter(testedParam.copy());
        final AbstractParameter<?> registeredParam2 = Configuration.registerParameter(testedParam.copy());

        assertEquals("Registering the same parmeter multiple times should return a reference to the same object!",
                     registeredParam1,
                     registeredParam2);
    }


    /**
     * Tests if registering a new parameter causes a copy of the parameter to be registered
     * instead of the parameter reference itself. This is done in order to preserve original values of
     * possible parameter constants.
     */
    @Test
    public void testRegisteringCopyParameter()
    {
        testedObject.addEventListeners();
        final AbstractParameter<?> unknownParam = new IntegerParameter(PARAM_KEY + 2, TEST_CATEGORY, 0);
        final AbstractParameter<?> registeredParam = Configuration.registerParameter(unknownParam);

        assertNotEquals("Registering a parameter object for the first time should register a copy instead of the original reference!",
                        registeredParam,
                        unknownParam);
    }


    /**
     * Tests if the value of a registered parameter is not changed, if the parameter is registered
     * again with a different value.
     */
    @Test
    public void testRegisteringTwiceDoNotOverride()
    {
        testedObject.addEventListeners();
        final int oldValue = random.nextInt(1000);
        final int newValueValue = oldValue + 1;

        final AbstractParameter<?> param1 = new IntegerParameter(PARAM_KEY + 1, TEST_CATEGORY, oldValue);
        final AbstractParameter<?> param2 = new IntegerParameter(PARAM_KEY + 1, TEST_CATEGORY, newValueValue);

        Configuration.registerParameter(param1);

        assertEquals("Registering a parameter should not override the value if the parameter is known by the Configuration!",
                     oldValue,
                     Configuration.registerParameter(param2).getValue());
    }


    /**
     * Tests if registering a parameter without having a configuration with event listeners
     * throws an {@linkplain IllegalStateException}.
     */
    @Test(expected = IllegalStateException.class)
    public void testRegisteringWithoutConfig()
    {
        Configuration.registerParameter(testedParam);
        fail("Registering a parameter while the Configuration has no listeners should throw an IllegalStateException!");
    }


    /**
     * Tests if the value of a {@linkplain StringParameter} can be changed via
     * the setter function of the {@linkplain Configuration}.
     */
    @Test
    public void testStringParameterChange()
    {
        final StringParameter param = new StringParameter(PARAM_KEY, TEST_CATEGORY, STRING_VALUE + 1);
        assertThatParmeterValueCanChange(param, STRING_VALUE + 2);
    }


    /**
     * Tests if the value of an {@linkplain IntegerParameter} can be changed via
     * the setter function of the {@linkplain Configuration}.
     */
    @Test
    public void testIntegerParameterChange()
    {
        final IntegerParameter param = new IntegerParameter(PARAM_KEY, TEST_CATEGORY, INT_VALUE_1);
        assertThatParmeterValueCanChange(param, INT_VALUE_2);
    }


    /**
     * Tests if the value of a {@linkplain BooleanParameter} can be changed via
     * the setter function of the {@linkplain Configuration}.
     */
    @Test
    public void testBooleanParameterChange()
    {
        final BooleanParameter param = new BooleanParameter(PARAM_KEY, TEST_CATEGORY, BOOL_VALUE_1);
        assertThatParmeterValueCanChange(param, BOOL_VALUE_2);
    }


    /**
     * Tests if the value of a {@linkplain PasswordParameter} can be changed via
     * the setter function of the {@linkplain Configuration}.
     */
    @Test
    public void testPasswordParameterChange()
    {
        final PasswordParameter param = new PasswordParameter(PARAM_KEY, TEST_CATEGORY, PASSWORD_VALUE_1);
        assertThatParmeterValueCanChange(param, PASSWORD_VALUE_2);
    }


    /**
     * Tests if the string representation of a {@linkplain PasswordParameter} masks the value.
     */
    @Test
    public void testPasswordParameterMasking()
    {
        final PasswordParameter param = new PasswordParameter(PARAM_KEY, TEST_CATEGORY, PASSWORD_VALUE_1);
        assertFalse("The value of a PasswordParameter must not be readable via getStringValue()!",
                    param.getStringValue().contains(param.getValue().toString()));
    }


    /**
     * Tests if the saveToDisk function creates a file on disk.
     */
    @Test
    public void testSaveWithPath()
    {
        testedObject.setCacheFilePath(configFile.getAbsolutePath());
        testedObject.saveToDisk();

        assertTrue("The method saveToDisk() should create a file on disk!",
                   configFile.exists() && configFile.isFile());
    }


    /**
     * Tests if the saveToDisk function does not create a file on disk,
     * if no path was set in advance.
     */
    @Test
    public void testSaveWithoutPath()
    {
        setLoggerEnabled(false);
        testedObject.saveToDisk();
        setLoggerEnabled(true);

        assertFalse("If  the method setCacheFilePath() is not called prior to calling saveToDisk(), a file should not be created!",
                    configFile.exists());
    }


    /**
     * Tests if loading a configuration will successfully apply previously
     * saved values.
     */
    @Test
    public void testLoadingPresentValues()
    {
        final Object savedParamValue = testedParam.getValue();
        testedParam.setRegistered(true);

        // save a config with two custom parameters
        final Configuration savedConfig = createConfigWithCustomParameters(testedParam);

        savedConfig.setCacheFilePath(configFile.getAbsolutePath());
        savedConfig.saveToDisk();

        // create a config with only the first custom parameter
        testedObject.setCacheFilePath(configFile.getAbsolutePath());

        // change the value of the shared custom parameter
        final String valueToBeRestored = "override me";
        testedObject.setParameter(testedParam.getCompositeKey(), valueToBeRestored);

        // load previously set up config
        setLoggerEnabled(false);
        testedObject.loadFromDisk();
        setLoggerEnabled(true);

        // make sure the value of custom parameter was changed due to the loading
        assertEquals(
            "The method loadFromDisk() should overwrite existing Parmeter values!",
            savedParamValue,
            testedObject.getParameterValue(testedParam.getCompositeKey()));
    }


    /**
     * Tests if loading a configuration will add new Parameters.
     */
    @Test
    public void testLoadingNonPresentValues()
    {
        // save a config with one parameter

        final Object savedParamValue = testedParam.getValue();
        testedParam.setRegistered(true);

        final Configuration savedConfig = createConfigWithCustomParameters(testedParam);
        savedConfig.setCacheFilePath(configFile.getAbsolutePath());
        savedConfig.saveToDisk();

        // create a config without parameters
        testedObject = new Configuration(MODULE_NAME);
        testedObject.setCacheFilePath(configFile.getAbsolutePath());

        // load previously set up config
        setLoggerEnabled(false);
        testedObject.loadFromDisk();
        setLoggerEnabled(true);

        // make sure the value of custom parameter was created due to the loading
        assertEquals(
            "The method loadFromDisk() should create parameters that did not exist!",
            savedParamValue,
            testedObject.getParameterValue(testedParam.getCompositeKey()));
    }


    /**
     * Tests if a loading function causes no changes if a path does not exist.
     */
    @Test
    public void testLoadWithNonExistingPath()
    {
        testedObject = new Configuration(MODULE_NAME);
        testedObject.setCacheFilePath(configFile.getAbsolutePath());

        // attempt to load a non-existing path
        setLoggerEnabled(false);
        testedObject.loadFromDisk();
        setLoggerEnabled(true);

        // make sure that no parmeters wer created
        assertTrue(
            "Loading a non-existing Configuration should not cause any changes!",
            testedObject.getParameters().isEmpty());
    }


    /**
     * Tests if a loading function causes no changes if a path was not set
     * in advance.
     */
    @Test
    public void testLoadWithoutSetPath()
    {
        // save a config with a custom parameter
        testedParam.setRegistered(true);
        final Configuration savedConfig = createConfigWithCustomParameters(testedParam);
        savedConfig.setCacheFilePath(configFile.getAbsolutePath());
        savedConfig.saveToDisk();

        // create config with the same custom parameter, but a different value
        final String valueToBeOverridden = "override me";
        testedParam.setValue(valueToBeOverridden);

        // attempt to load without setting a path
        setLoggerEnabled(false);
        testedObject.loadFromDisk();
        setLoggerEnabled(true);

        // make sure the old custom parameter value was not overridden
        assertEquals(
            "The method loadFromDisk() should not cause changes if setCacheFilePath() was not called in advance!",
            valueToBeOverridden,
            testedObject.getParameterStringValue(testedParam.getCompositeKey()));
    }


    /**
     * Tests if a Configuration can be serialized and deserialized without losing data.
     *
     * @throws MalformedURLException thrown if the URL parameter could not be created
     */
    @Test
    public void testJsonSerializationParameterTypes() throws MalformedURLException
    {
        // save config with all parameters to disk
        final Configuration savedConfig = createConfigWithAllParameterTypes();

        // register parameter to mark it for serialization
        savedConfig.getParameters().forEach((AbstractParameter<?> param) -> param.setRegistered(true));

        testedObject = saveAndLoadConfig(savedConfig);

        // check if deserialized global parameters are correct
        final Collection<AbstractParameter<?>> loadedParams = testedObject.getParameters();

        for (AbstractParameter<?> param : savedConfig.getParameters()) {
            boolean hasLoadedParameter = false;

            for (AbstractParameter<?> loadedParam : loadedParams) {
                if (loadedParam.getCompositeKey().equals(param.getCompositeKey())) {
                    assertEquals("The class of a saved parmeter must not change when the parmeter is loaded!",
                                 param.getClass(),
                                 loadedParam.getClass());
                    hasLoadedParameter = true;
                    break;
                }
            }

            if (!hasLoadedParameter)
                fail(String.format(ERROR_MISSING_LOADED_PARAM, param));
        }
    }


    /**
     * Tests if a Configuration can be serialized and deserialized with null value parameters.
     *
     * @throws MalformedURLException thrown if the URL parameter could not be created
     */
    @Test
    public void testJsonSerializationOfNullValues() throws MalformedURLException
    {
        final Configuration savedConfig = createConfigWithCustomParameters(
                                              new StringParameter(PARAM_KEY + 1, TEST_CATEGORY, null),
                                              new PasswordParameter(PARAM_KEY + 5, TEST_CATEGORY, null));

        // register parameters to mark them for serialization
        savedConfig.getParameters().forEach((AbstractParameter<?> param) -> param.setRegistered(true));

        // save and load
        testedObject = saveAndLoadConfig(savedConfig);

        // check if deserialized global parameters are correct
        for (AbstractParameter<?> param : savedConfig.getParameters()) {
            assertEquals(
                "Saved parameters with null values must not change when loaded!",
                param.getValue(),
                testedObject.getParameterValue(param.getCompositeKey()));
        }
    }


    /**
     * Tests if a Configuration can be serialized and deserialized
     * while preserving the parameter types.
     *
     * @throws MalformedURLException thrown if the URL parameter could not be created
     */
    @Test
    public void testJsonSerializationParameterValues() throws MalformedURLException
    {
        // save config with all parameters to disk
        final Configuration savedConfig = createConfigWithAllParameterTypes();

        // save and load
        testedObject = saveAndLoadConfig(savedConfig);

        // check if deserialized global parameters are correct
        for (AbstractParameter<?> param : savedConfig.getParameters()) {
            assertEquals("The original values of saved Parmeters must match the values of the loaded Parameters!",
                         param.getValue(),
                         testedObject.getParameterValue(param.getCompositeKey()));
        }
    }


    /**
     * Tests if the class of a cloned parameter is the same as the source parameter.
     *
     * @throws MalformedURLException thrown if the URL parameter could not be created
     */
    @Test
    public void testParameterCopyClass() throws MalformedURLException
    {
        testedObject = createConfigWithAllParameterTypes();

        for (AbstractParameter<?> param : testedObject.getParameters()) {
            assertEquals(
                "The method copy() must preserve the class of the source object!",
                param.getClass(),
                param.copy().getClass());
        }
    }


    /**
     * Tests if the category of a cloned parameter is the same as that of the source parameter.
     *
     * @throws MalformedURLException thrown if the URL parameter could not be created
     */
    @Test
    public void testParameterCopyCategory() throws MalformedURLException
    {
        testedObject = createConfigWithAllParameterTypes();

        for (AbstractParameter<?> param : testedObject.getParameters()) {
            final AbstractParameter<?> clonedParam = param.copy();
            assertEquals(
                "The method copy() must preserve the 'category' field of the source object!",
                param.getCategory(),
                clonedParam.getCategory());
        }
    }


    /**
     * Tests if the value of a cloned parameter is the same as that of the source parameter.
     *
     * @throws MalformedURLException thrown if the URL parameter could not be created
     */
    @Test
    public void testParameterCopyValues() throws MalformedURLException
    {
        testedObject = createConfigWithAllParameterTypes();

        for (AbstractParameter<?> param : testedObject.getParameters()) {
            final AbstractParameter<?> clonedParam = param.copy();
            assertEquals(
                "The method copy() must preserve the 'value' field of the source object!",
                param.getValue(),
                clonedParam.getValue());
        }
    }


    /**
     * Tests if the key of a cloned parameter is the same as that of the source parameter.
     *
     * @throws MalformedURLException thrown if the URL parameter could not be created
     */
    @Test
    public void testParameterCopyKey() throws MalformedURLException
    {
        testedObject = createConfigWithAllParameterTypes();

        for (AbstractParameter<?> param : testedObject.getParameters()) {
            final AbstractParameter<?> clonedParam = param.copy();
            assertEquals(
                "The method copy() must preserve the 'key' field of the source object!",
                param.getKey(),
                clonedParam.getKey());
        }
    }


    /**
     * Tests if a cloned parameter is not the same as instance as the source parameter.
     *
     * @throws MalformedURLException thrown if the URL parameter could not be created
     */
    @Test
    public void testParameterCopyNotTheSame() throws MalformedURLException
    {
        testedObject = createConfigWithAllParameterTypes();

        for (AbstractParameter<?> param : testedObject.getParameters()) {
            assertNotEquals("The method copy() must not return a reference to the source object!",
                            param,
                            param.copy());
        }
    }


    /**
     * Tests if the {@linkplain Configuration}'s getAsPlainText() function displays all parameter keys
     * of registered parmeters.
     *
     * @throws MalformedURLException thrown if the URL parameter could not be created
     */
    @Test
    public void testPlainTextKeys() throws MalformedURLException
    {
        testedObject = createConfigWithAllParameterTypes();
        testedObject.getParameters().forEach((AbstractParameter<?> param) -> param.setRegistered(true));

        final String configString = testedObject.getAsPlainText();

        for (AbstractParameter<?> param : testedObject.getParameters()) {
            assertTrue("The method toString() must return a string containing all registered parameter keys!",
                       configString.contains(param.getKey()));
        }
    }


    /**
     * Tests if the {@linkplain Configuration}'s toString() function displays all parameter values.
     *
     * @throws MalformedURLException thrown if the URL parameter could not be created
     */
    @Test
    public void testToStringValues() throws MalformedURLException
    {
        testedObject = createConfigWithAllParameterTypes();
        testedObject.getParameters().forEach((AbstractParameter<?> param) -> param.setRegistered(true));

        final String configString = testedObject.getAsPlainText();

        for (AbstractParameter<?> param : testedObject.getParameters()) {
            assertTrue("The method toString() must return a string containing all parameter value string representations!",
                       configString.contains(param.getStringValue()));
        }
    }


    /**
     * Tests if the {@linkplain Configuration}'s toString() function does not display unregistered
     * parameters.
     *
     * @throws MalformedURLException thrown if the URL parameter could not be created
     */
    @Test
    public void testToStringUnregisteredParameters() throws MalformedURLException
    {
        testedObject = createConfigWithAllParameterTypes();
        final String configString = testedObject.toString();

        for (AbstractParameter<?> param : testedObject.getParameters()) {
            assertFalse("The method toString() must not display unregistered parmeters!",
                        configString.contains(param.getKey()));
        }
    }


    //////////////////////
    // Non-test Methods //
    //////////////////////


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
    private <T>
    void assertThatParmeterValueCanChange(AbstractParameter<T> param, T newValue)
    {
        param.setRegistered(true);

        final String compositeKey = param.getCompositeKey();
        final T oldValue = param.getValue();

        if (oldValue.equals(newValue))
            throw new IllegalArgumentException(ERROR_ARGUMENTS_MUST_DIFFER);

        testedObject = createConfigWithCustomParameters(param);
        testedObject.setParameter(compositeKey, newValue.toString());
        assertNotEquals(
            "The parameter value is supposed to change!",
            oldValue,
            testedObject.getParameterValue(compositeKey));
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
        return new Configuration(MODULE_NAME, params);
    }


    /**
     * Creates a {@linkplain Configuration} that has one of each parameter types for harvester- and global
     * parameters.
     *
     * @return a {@linkplain Configuration} with all {@linkplain AbstractParameter} subclasses
     * @throws MalformedURLException  thrown when the URL cannot be parsed
     */
    private Configuration createConfigWithAllParameterTypes() throws MalformedURLException
    {
        return createConfigWithCustomParameters(
                   new StringParameter(PARAM_KEY + 1, TEST_CATEGORY, ""),
                   new StringParameter(PARAM_KEY + 2, TEST_CATEGORY, STRING_VALUE + 1),
                   new IntegerParameter(PARAM_KEY + 3, TEST_CATEGORY, 0),
                   new IntegerParameter(PARAM_KEY + 4, TEST_CATEGORY, INT_VALUE_1),
                   new BooleanParameter(PARAM_KEY + 5, TEST_CATEGORY, BOOL_VALUE_1),
                   new BooleanParameter(PARAM_KEY + 6, TEST_CATEGORY, BOOL_VALUE_2),
                   new PasswordParameter(PARAM_KEY + 8, TEST_CATEGORY, PASSWORD_VALUE_1)
               );
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
        configToSave.setCacheFilePath(configFile.getAbsolutePath());
        configToSave.saveToDisk();

        // deserialize cache file
        final Gson gson =
            new GsonBuilder()
        .registerTypeAdapter(Configuration.class, new ConfigurationAdapter())
        .create();
        final DiskIO diskIo = new DiskIO(gson, StandardCharsets.UTF_8);
        return diskIo.getObject(configFile, Configuration.class);
    }
}
