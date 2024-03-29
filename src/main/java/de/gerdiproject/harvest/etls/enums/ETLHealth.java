/**
 * Copyright © 2017 Robin Weiss (http://www.gerdi-project.de)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.gerdiproject.harvest.etls.enums;

import de.gerdiproject.harvest.etls.extractors.ExtractorException;
import de.gerdiproject.harvest.etls.loaders.LoaderException;
import de.gerdiproject.harvest.etls.transformers.TransformerException;

/**
 * This enumeration represents a simplified health status of the harvester service.
 *
 * @author Robin Weiss
 */
public enum ETLHealth {
    /**
     * The service has not experienced any significant failures.
     */
    OK,

    /**
     * The harvest could not be completed, due to an  unknown exception.
     */
    HARVEST_FAILED,

    /**
     * The harvest could not be completed, due to an {@linkplain ExtractorException}.
     */
    EXTRACTION_FAILED,

    /**
     * The harvest could not be completed, due to a {@linkplain TransformerException}.
     */
    TRANSFORMATION_FAILED,

    /**
     * The harvest could not be completed, due to a {@linkplain LoaderException}.
     */
    LOADING_FAILED,

    /**
     * The harvester service could not be started.
     */
    INITIALIZATION_FAILED
}
