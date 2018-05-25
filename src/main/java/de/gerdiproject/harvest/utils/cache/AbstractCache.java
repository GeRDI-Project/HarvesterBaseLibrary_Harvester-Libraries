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
package de.gerdiproject.harvest.utils.cache;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.BiFunction;

import de.gerdiproject.harvest.utils.FileUtils;
import de.gerdiproject.harvest.utils.cache.constants.CacheConstants;
import de.gerdiproject.harvest.utils.data.DiskIO;
import de.gerdiproject.json.GsonUtils;


/**
 * This abstract class represents two cache folders, one which is called
 * "stable" and one which is called "work-in-progress" folder.
 * Both folders may contain subfolders with names that consist
 * of the first two characters of a documentID. Inside these folders are
 * files that are named after the rest of the documentID. The content
 * of these files can be determined by setting the Template.
 * @author Robin Weiss
 *
 * @param <T> the class of the file content of each cached file
 */
public abstract class AbstractCache <T>
{
    private final Class<T> fileContentClass;
    protected final DiskIO diskIo;
    protected final String stableFolderPath;
    protected final String wipFolderPath;


    /**
     * Constructor that sets up the paths to the stable and work-in-progress folders
     * as well as the content type of the cached files.
     *
     * @param stableFolderPath the path of the stable cache folder
     * @param wipFolderPath the path of the cache folder with pending changes
     * @param fileContentClass the class of the file content
     * @param charset the charset of the cached files
     */
    public AbstractCache(final String stableFolderPath, final String wipFolderPath, final Class<T> fileContentClass, final Charset charset)
    {
        this.stableFolderPath = stableFolderPath;
        this.wipFolderPath = wipFolderPath;
        this.fileContentClass = fileContentClass;
        this.diskIo = new DiskIO(GsonUtils.createGerdiDocumentGsonBuilder().create(), charset);
    }

    /**
     * Replaces the stable folder with the work-in-progress changes.
     */
    public abstract void applyChanges();


    /**
     * Iterates through the stable folder of cached files and executes a
     * function on each file content. If the function returns false, the whole
     * process is aborted.
     *
     * @param documentFunction a function that accepts a documentId and the
     *            corresponding document JSON object and returns true if it was
     *            successfully processed
     *
     * @return true if all documents were processed successfully
     */
    public boolean forEach(BiFunction<String, T, Boolean> documentFunction)
    {
        boolean isSuccessful = true;

        final File stableDir = new File(stableFolderPath);

        if (stableDir.exists()) {
            try
                (DirectoryStream<Path> prefixStream = Files.newDirectoryStream(stableDir.toPath())) {
                for (Path prefixFolderPath : prefixStream) {
                    final File prefixFolder = prefixFolderPath.toFile();

                    // skip files of the top folder
                    if (!prefixFolder.isDirectory())
                        continue;

                    // iterate through the sub folder
                    final String documentIdPrefix = prefixFolder.getName();

                    // iterate through the sub folder
                    try
                        (DirectoryStream<Path> suffixStream = Files.newDirectoryStream(prefixFolderPath)) {
                        for (Path suffixFile : suffixStream) {

                            // these three lines are just there to make FindBugs happy:
                            final Path fileNamePath = suffixFile.getFileName();

                            if (fileNamePath == null)
                                continue;

                            // the file name without extension is the suffix of the document ID
                            String documentIdSuffix = fileNamePath.toString();
                            documentIdSuffix = documentIdSuffix.substring(0, documentIdSuffix.lastIndexOf('.'));

                            // assemble documentID
                            final String documentId = documentIdPrefix + documentIdSuffix;

                            // retrieve content and execute iteration function
                            isSuccessful = documentFunction.apply(documentId, getFileContent(documentId));

                            if (!isSuccessful)
                                break;
                        }
                    }

                    if (!isSuccessful)
                        break;
                }
            } catch (IOException e) {
                isSuccessful = false;
            }
        }

        return isSuccessful;
    }


    /**
     * Removes a file from the cache.
     *
     * @param documentId the ID of the file that is to be removed
     */
    public void removeFile(String documentId)
    {
        putFile(documentId, null);
    }


    /**
     * Adds or replaces a file in the work-in-progress folder.
     * The key is the unique documentID.
     *
     * @param documentId the unique document identifier
     * @param document the file content or null, if the
     *            file is to be removed
     */
    public void putFile(final String documentId, final T document)
    {
        final File documentFile = getFile(documentId, false);

        // delete old file
        if (documentFile.exists())
            FileUtils.deleteFile(documentFile);

        // write new file
        if (document != null)
            diskIo.writeObjectToFile(documentFile, document);
    }


    /**
     * Retrieves the file content for a specified document ID from the cache
     * folder, or null if this document does not exist or is empty.
     *
     * @param documentId the identifier of the document which is to be retrieved
     * @return a JSON document or null, if no document exists
     */
    public T getFileContent(String documentId)
    {
        final File retrievedDoc = getFile(documentId, true);

        if (retrievedDoc.exists() && retrievedDoc.length() > 0)
            return diskIo.getObject(retrievedDoc, fileContentClass);
        else
            return null;
    }

    /**
     * Assembles the file path to a cached file.
     *
     * @param documentId the documentID of which the document object is retrieved
     * @param isStable if true, the file points to the stable folder
     *
     * @return a path to the document file with the specified documentId
     */
    protected File getFile(final String documentId, boolean isStable)
    {
        return new File(
                   String.format(
                       CacheConstants.DOCUMENT_HASH_FILE_PATH,
                       isStable ? stableFolderPath : wipFolderPath,
                       documentId.substring(0, 2),
                       documentId.substring(2)));
    }
}
