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
package de.gerdiproject.harvest.utils;

import java.util.List;

import de.gerdiproject.json.datacite.DataCiteJson;

/**
 * @author Robin Weiss
 *
 */
public class DebugUtils
{
    private static <T> boolean isListUnmodifiable(List<T> list)
    {
        if (list != null) {
            try {
                list.add(0, list.remove(0));
            } catch (UnsupportedOperationException e) {
                return true;
            }
        }

        return false;
    }

    public static String getUnmodifiableLists(DataCiteJson doc)
    {
        final StringBuilder sb = new StringBuilder();

        if (isListUnmodifiable(doc.getTitles()))
            sb.append("titles\n");

        if (isListUnmodifiable(doc.getSubjects()))
            sb.append("subjects\n");

        if (isListUnmodifiable(doc.getRightsList()))
            sb.append("rightsList\n");

        if (isListUnmodifiable(doc.getDescriptions()))
            sb.append("descriptions\n");

        if (isListUnmodifiable(doc.getContributors()))
            sb.append("contributors\n");

        if (isListUnmodifiable(doc.getCreators()))
            sb.append("creators\n");

        if (isListUnmodifiable(doc.getAlternateIdentifiers()))
            sb.append("alternateIdentifiers\n");

        if (isListUnmodifiable(doc.getRelatedIdentifiers()))
            sb.append("relatedIdentifiers\n");

        if (isListUnmodifiable(doc.getSizes()))
            sb.append("sizes\n");

        if (isListUnmodifiable(doc.getFundingReferences()))
            sb.append("fundingReferences\n");

        if (isListUnmodifiable(doc.getFormats()))
            sb.append("formats\n");

        if (isListUnmodifiable(doc.getWebLinks()))
            sb.append("webLinks\n");

        if (isListUnmodifiable(doc.getResearchDataList()))
            sb.append("researchDataList\n");

        if (isListUnmodifiable(doc.getResearchDisciplines()))
            sb.append("researchDisciplines\n");

        if (isListUnmodifiable(doc.getDates()))
            sb.append("dates\n");

        if (isListUnmodifiable(doc.getGeoLocations()))
            sb.append("geoLocations\n");

        if (sb.length() != 0)
            return String.format("Document %s has the following unmodifiable lists:%n%s", doc.getSourceId(), sb.toString());

        return null;
    }

    /**
     * Private constructor, because this class offers static methods only.
     */
    private DebugUtils()
    {
    }
}
