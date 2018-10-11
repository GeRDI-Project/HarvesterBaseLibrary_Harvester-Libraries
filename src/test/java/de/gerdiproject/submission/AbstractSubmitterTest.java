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
package de.gerdiproject.submission; // NOPMD JUnit 4 requires many static imports

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import de.gerdiproject.AbstractFileSystemUnitTest;
import de.gerdiproject.harvest.config.Configuration;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.harvester.events.HarvestFinishedEvent;
import de.gerdiproject.harvest.submission.AbstractSubmitter;
import de.gerdiproject.harvest.submission.constants.SubmissionConstants;
import de.gerdiproject.harvest.submission.events.StartSubmissionEvent;
import de.gerdiproject.harvest.submission.events.SubmissionFinishedEvent;
import de.gerdiproject.harvest.utils.cache.HarvesterCache;
import de.gerdiproject.harvest.utils.cache.HarvesterCacheManager;
import de.gerdiproject.harvest.utils.cache.events.RegisterHarvesterCacheEvent;
import de.gerdiproject.harvest.utils.time.ProcessTimeMeasure;
import de.gerdiproject.harvest.utils.time.ProcessTimeMeasure.ProcessStatus;
import de.gerdiproject.json.datacite.DataCiteJson;
import de.gerdiproject.submission.examples.MockedSubmitter;
import de.gerdiproject.utils.examples.harvestercache.MockedHarvester;

/**
 * This class offers unit tests for {@linkplain AbstractSubmitter}.
 *
 * @author Robin Weiss
 */
public class AbstractSubmitterTest extends AbstractFileSystemUnitTest<AbstractSubmitter>
{
    private static final String MODULE_NAME = "mocked";
    private static final String TEST_URL = "http://www.gerdi-project.de/";
    private static final String TEST_USER_NAME = "Max Power";
    private static final String TEST_PASSWORD = "Top Sekret";
    private static final String SOURCE_ID = "source";
    private static final String ERROR_ILLEGAL_STATE_EXPECTED = "Expected IllegalStateException to be thrown!";
    private static final String ASSERT_PARAM_UPDATE_MESSAGE = "Changing the %s-parameter should update the field of the AbstractSubmitter!";
    private static final String ASSERT_SUBMIT_ALL_MESSAGE = "Not all documents were properly submitted!";
    private static final String ASSERT_ERROR_MESSAGE = "Expected the error message: \"%s\"!";

    private HarvesterCacheManager cacheManager = new HarvesterCacheManager();


    @Override
    protected AbstractSubmitter setUpTestObjects()
    {
        final long startTimestamp = random.nextLong();
        final long endTimestamp = random.longs(startTimestamp + 1, startTimestamp + 99999999).findAny().getAsLong();

        ProcessTimeMeasure measure = new ProcessTimeMeasure();
        measure.set(startTimestamp, endTimestamp, ProcessStatus.Finished);

        config = new Configuration(MODULE_NAME);
        config.addEventListeners();

        AbstractSubmitter mockedSubmitter = new MockedSubmitter();

        return mockedSubmitter;
    }


    /**
     * Tests if changing the 'submissionUrl' parameter causes the 'url' field
     * in {@linkplain AbstractSubmitter} to be updated.
     */
    @Test
    public void testChangingSubmissionUrl()
    {
        testedObject.addEventListeners();
        final String url = setRandomUrl();

        assertEquals(String.format(ASSERT_PARAM_UPDATE_MESSAGE, SubmissionConstants.URL_PARAM.getCompositeKey()),
                     url,
                     ((MockedSubmitter) testedObject).getUrl().toString());
    }


    /**
     * Tests if changing the 'submissionSize' parameter causes the 'maxBatchSize' field
     * in {@linkplain AbstractSubmitter} to be updated.
     */
    @Test
    public void testChangingMaxBatchSize()
    {
        testedObject.addEventListeners();

        final int size = 1 + random.nextInt(1000);
        setBatchSize(size);

        assertEquals(String.format(ASSERT_PARAM_UPDATE_MESSAGE, SubmissionConstants.MAX_BATCH_SIZE_PARAM.getCompositeKey()),
                     size,
                     ((MockedSubmitter) testedObject).getMaxBatchSize());
    }


    /**
     * Tests if the credentials are initialized with null.
     */
    @Test
    public void testInitialCredentials()
    {
        assertNull("The method getCredentials() should return null if neither the password nor the username were set!",
                   ((MockedSubmitter) testedObject).getCredentials());
    }


    /**
     * Tests if changing only the 'submissionUserName' parameter causes the 'credentials' field
     * in {@linkplain AbstractSubmitter} to remain null.
     */
    @Test
    public void testChangingUserName()
    {
        testedObject.addEventListeners();
        setRandomUserName();
        assertNull("The method getCredentials() should return null if the password was not set!",
                   ((MockedSubmitter) testedObject).getCredentials());
    }


    /**
     * Tests if changing only the 'submissionPassword' parameter causes the 'credentials' field
     * in {@linkplain AbstractSubmitter} to remain null.
     */
    @Test
    public void testChangingPassword()
    {
        testedObject.addEventListeners();
        setRandomPassword();
        assertNull("The method getCredentials() should return null if the username was not set!",
                   ((MockedSubmitter) testedObject).getCredentials());
    }


    /**
     * Tests if changing the 'submissionUserName' and 'submissionPassword' parameters causes the 'credentials' field
     * in {@linkplain AbstractSubmitter} to be filled.
     */
    @Test
    public void testChangingUserNameAndPassword()
    {
        testedObject.addEventListeners();
        setRandomUserName();
        setRandomPassword();
        assertNotNull("The method getCredentials() should NOT return null if the password and username were both set!",
                      ((MockedSubmitter) testedObject).getCredentials());
    }


    /**
     * Tests if the 'credentials' field does not contain the username or password in clear text.
     */
    @Test
    public void testCredentialMasking()
    {
        testedObject.addEventListeners();

        final String userName = setRandomUserName();
        final String password = setRandomPassword();

        final String credentials = ((MockedSubmitter) testedObject).getCredentials();
        assertFalse("Neither the password, nor the user name should be retrievable from the method getCredentials()!",
                    credentials.contains(userName) && !credentials.contains(password));
    }


    /**
     * Tests if the submission can be started via the {@linkplain StartSubmissionEvent}.
     */
    @Test
    public void testStartingSubmission()
    {
        testedObject.addEventListeners();
        setRandomUrl();
        setBatchSize(1);
        addSubmittableDocuments(1);

        SubmissionFinishedEvent finishedEvent = waitForEvent(
                                                    SubmissionFinishedEvent.class,
                                                    2000,
                                                    () -> testedObject.submitAll());

        assertNotNull("The submitAll() function should eventually cause a " + SubmissionFinishedEvent.class.getSimpleName() + " to be sent!",
                      finishedEvent);
    }


    /**
     * Tests if submitting a single batch of documents correctly processes all documents.
     */
    @Test
    public void testSubmittingASingleBatch()
    {
        testedObject.addEventListeners();
        setRandomUrl();

        final int numberOfAddedDocs = addRandomNumberOfSubmittableDocuments();
        setBatchSize(numberOfAddedDocs);

        waitForEvent(
            SubmissionFinishedEvent.class,
            2000,
            () -> testedObject.submitAll());

        for (int i = 0; i < numberOfAddedDocs; i++)
            assertTrue(ASSERT_SUBMIT_ALL_MESSAGE,
                       ((MockedSubmitter) testedObject).getSubmittedIndices().contains(i));
    }


    /**
     * Tests if submitting a multiple batches of documents correctly processes all documents.
     */
    @Test
    public void testSubmittingMultipleBatches()
    {
        testedObject.addEventListeners();
        setRandomUrl();

        // add at least two documents, so we need at least two batches of submission
        final int numberOfAddedDocs = 2 + random.nextInt(9);
        addSubmittableDocuments(numberOfAddedDocs);
        setBatchSize(1);

        waitForEvent(
            SubmissionFinishedEvent.class,
            2000,
            () -> testedObject.submitAll());

        for (int i = 0; i < numberOfAddedDocs; i++)
            assertTrue(ASSERT_SUBMIT_ALL_MESSAGE,
                       ((MockedSubmitter) testedObject).getSubmittedIndices().contains(i));
    }


    /**
     * Tests if the submission fails if no valid URL is set.
     */
    @Test
    public void testStartingSubmissionNoURL()
    {
        testedObject.addEventListeners();
        setBatchSize(1);
        addSubmittableDocuments(1);

        try {
            testedObject.submitAll();
            fail(ERROR_ILLEGAL_STATE_EXPECTED);
        } catch (IllegalStateException e) {
            assertEquals(String.format(ASSERT_ERROR_MESSAGE, SubmissionConstants.NO_URL_ERROR),
                         SubmissionConstants.NO_URL_ERROR,
                         e.getMessage());
        }
    }


    /**
     * Tests if the submission fails if no valid URL is set.
     */
    @Test
    public void testStartingSubmissionNoDocuments()
    {
        testedObject.addEventListeners();
        testedObject.setCacheManager(cacheManager);
        setRandomUrl();
        setBatchSize(1);

        try {
            testedObject.submitAll();
            fail(ERROR_ILLEGAL_STATE_EXPECTED);
        } catch (IllegalStateException e) {
            assertEquals(String.format(ASSERT_ERROR_MESSAGE, SubmissionConstants.NO_DOCS_ERROR),
                         SubmissionConstants.NO_DOCS_ERROR,
                         e.getMessage());
        }
    }


    /**
     * Tests if the submission fails if the preceding harvest failed.
     */
    @Test
    public void testSubmittingFailedHarvest()
    {
        testedObject.addEventListeners();
        setRandomUrl();

        addRandomNumberOfSubmittableDocuments();
        setBatchSize(1);

        EventSystem.sendEvent(new HarvestFinishedEvent(false, SOURCE_ID));

        try {
            testedObject.submitAll();
            fail(ERROR_ILLEGAL_STATE_EXPECTED);
        } catch (IllegalStateException e) {
            assertEquals(String.format(ASSERT_ERROR_MESSAGE, SubmissionConstants.FAILED_HARVEST_ERROR),
                         SubmissionConstants.FAILED_HARVEST_ERROR,
                         e.getMessage());
        }
    }




    /**
     * Tests if the submission fails if all documents have already been submitted.
     */
    @Test
    public void testResubmitting()
    {
        testedObject.addEventListeners();
        setRandomUrl();

        addRandomNumberOfSubmittableDocuments();
        setBatchSize(1);

        // finish submitting successfully
        waitForEvent(
            SubmissionFinishedEvent.class,
            2000,
            () -> testedObject.submitAll());

        // try to submit again, expect mayhem (BOOOOOM!)
        try {
            testedObject.submitAll();
            fail(ERROR_ILLEGAL_STATE_EXPECTED);
        } catch (IllegalStateException e) {
            assertEquals(String.format(ASSERT_ERROR_MESSAGE, SubmissionConstants.OUTDATED_ERROR),
                         SubmissionConstants.OUTDATED_ERROR,
                         e.getMessage());
        }
    }


    /**
     * Tests if the submission succeeds if the preceding harvest failed, but the 'submitIncomplete'
     * is enabled.
     */
    @Test
    public void testSubmittingFailedHarvestWithFlag()
    {
        testedObject.addEventListeners();
        setRandomUrl();
        addRandomNumberOfSubmittableDocuments();
        setBatchSize(1);

        // enable flag
        final String submitIncompleteKey = SubmissionConstants.SUBMIT_INCOMPLETE_PARAM.getCompositeKey();
        config.setParameter(submitIncompleteKey, Boolean.TRUE.toString());

        // mark the harvest as incomplete
        EventSystem.sendEvent(new HarvestFinishedEvent(false, SOURCE_ID));

        SubmissionFinishedEvent finishedEvent = waitForEvent(
                                                    SubmissionFinishedEvent.class,
                                                    2000,
                                                    () -> testedObject.submitAll());

        assertNotNull("The method submitAll() should cause a " + finishedEvent.getClass().getSimpleName() + " to be sent, when the " + submitIncompleteKey + "-parameter is true, even if the harvest failed!",
                      finishedEvent);
    }


    /**
     * Tests if the submission succeeds if the documents have already been submitted,
     * but the 'submitOutdated' is enabled.
     */
    @Test
    public void testResubmittingWithFlag()
    {
        testedObject.addEventListeners();
        setRandomUrl();

        addRandomNumberOfSubmittableDocuments();
        setBatchSize(1);

        // enable flag
        final String submitOutdatedKey = SubmissionConstants.SUBMIT_OUTDATED_PARAM.getCompositeKey();
        config.setParameter(submitOutdatedKey, Boolean.TRUE.toString());

        // finish submitting successfully
        waitForEvent(
            SubmissionFinishedEvent.class,
            2000,
            () -> testedObject.submitAll());

        SubmissionFinishedEvent secondEvent = waitForEvent(
                                                  SubmissionFinishedEvent.class,
                                                  2000,
                                                  () -> testedObject.submitAll());

        assertNotNull("The method submitAll() should cause a " + secondEvent.getClass().getSimpleName() + " to be sent, when the " + submitOutdatedKey + "-parameter is true and a submission succeeded in the past!",
                      secondEvent);
    }


    //////////////////////
    // Non-test Methods //
    //////////////////////

    /**
     * Caches 1 to 10 documents in a {@linkplain HarvesterCache}.
     *
     * @return the number of cached documents
     */
    private int addRandomNumberOfSubmittableDocuments()
    {
        final int numberOfHarvestedDocuments = 1 + random.nextInt(10);
        addSubmittableDocuments(numberOfHarvestedDocuments);
        return numberOfHarvestedDocuments;
    }

    /**
     * Caches a specified number of documents in a {@linkplain HarvesterCache} that is then registered at
     * the {@linkplain HarvesterCacheManager}, allowing the documents to be submitted.
     * The documents receive their index as publication year.
     *
     * @param size the number of cached documents
     */
    private void addSubmittableDocuments(int size)
    {
        testedObject.setCacheManager(cacheManager);

        final MockedHarvester harvester = new MockedHarvester(testFolder);
        final HarvesterCache harvesterCache = new HarvesterCache(
            harvester.getId(),
            harvester.getTemporaryCacheFolder(),
            harvester.getStableCacheFolder(),
            harvester.getCharset());

        cacheManager.addEventListeners();
        EventSystem.sendEvent(new RegisterHarvesterCacheEvent(harvesterCache));

        // mock harvest of a random number of documents

        for (int i = 0; i < size; i++) {
            final DataCiteJson doc = new DataCiteJson(SOURCE_ID + i);
            doc.setPublicationYear((short) i);
            harvesterCache.cacheDocument(doc, true);
        }

        harvesterCache.applyChanges(true, false);
    }


    /**
     * @param size
     */
    private void setBatchSize(int size)
    {
        final String key = SubmissionConstants.MAX_BATCH_SIZE_PARAM.getCompositeKey();
        config.setParameter(key, String.valueOf(size));
    }


    private String setRandomUserName()
    {
        final String key = SubmissionConstants.USER_NAME_PARAM.getCompositeKey();
        final String userName = TEST_USER_NAME + random.nextInt(1000);
        config.setParameter(key, userName);
        return userName;
    }


    private String setRandomPassword()
    {
        final String key = SubmissionConstants.PASSWORD_PARAM.getCompositeKey();
        final String password = TEST_PASSWORD + random.nextInt(1000);
        config.setParameter(key, password);
        return password;
    }


    private String setRandomUrl()
    {
        final String key = SubmissionConstants.URL_PARAM.getCompositeKey();
        final String url = TEST_URL + random.nextInt(1000);
        config.setParameter(key, url);
        return url;
    }
}
