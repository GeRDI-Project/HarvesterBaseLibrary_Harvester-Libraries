package de.gerdiproject.harvest.utils.cleaner;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import de.gerdiproject.json.datacite.DataCiteJson;
import de.gerdiproject.json.datacite.Description;
import de.gerdiproject.json.datacite.GeoLocation;
import de.gerdiproject.json.datacite.Rights;
import de.gerdiproject.json.datacite.Subject;
import de.gerdiproject.json.datacite.Title;

public class DataCiteCleaner implements ICleaner<DataCiteJson>
{
    private StringCleaner stringCleaner = new StringCleaner();
    private GeoJsonCleaner geoCleaner = new GeoJsonCleaner();

    @Override
    public DataCiteJson clean(DataCiteJson dirtyObject)
    {
        if (dirtyObject == null)
            return null;

        DataCiteJson cleanDataCite = new DataCiteJson();

        // clean some fields that require cleaning
        cleanDataCite.titles = cleanTitles(dirtyObject);
        cleanDataCite.descriptions = cleanDescriptions(dirtyObject);
        cleanDataCite.subjects = cleanSubjects(dirtyObject);
        cleanDataCite.rightsList = cleanRightsList(dirtyObject);
        cleanDataCite.geoLocations = cleanGeoLocations(dirtyObject);

        // copy other fields
        cleanDataCite.identifier = dirtyObject.identifier;
        cleanDataCite.publisher = dirtyObject.publisher;
        cleanDataCite.version = dirtyObject.version;
        cleanDataCite.language = dirtyObject.language;
        cleanDataCite.publicationYear = dirtyObject.publicationYear;
        cleanDataCite.resourceType = dirtyObject.resourceType;
        cleanDataCite.customData = dirtyObject.customData;
        cleanDataCite.sources = dirtyObject.sources;
        cleanDataCite.sizes = dirtyObject.sizes;
        cleanDataCite.formats = dirtyObject.formats;
        cleanDataCite.creators = dirtyObject.creators;
        cleanDataCite.contributors = dirtyObject.contributors;
        cleanDataCite.dates = dirtyObject.dates;
        cleanDataCite.relatedIdentifiers = dirtyObject.relatedIdentifiers;
        cleanDataCite.alternateIdentifiers = dirtyObject.alternateIdentifiers;
        cleanDataCite.fundingReferences = dirtyObject.fundingReferences;
        cleanDataCite.webLinks = dirtyObject.webLinks;
        cleanDataCite.files = dirtyObject.files;

        return cleanDataCite;
    }


    private List<Title> cleanTitles(DataCiteJson dirtyObject)
    {
        List<Title> titles = dirtyObject.titles;
        final List<Title> cleanedTitles;

        if (titles != null) {
            cleanedTitles = new LinkedList<>();

            // clean all
            titles.forEach((Title t) -> {
                Title cleanTitle = new Title(stringCleaner.clean(t.title));
                cleanTitle.lang = t.lang;
                cleanTitle.type = t.type;
                cleanedTitles.add(cleanTitle) ;
            });
        } else
            cleanedTitles = null;

        return cleanedTitles;
    }

    private List<Description> cleanDescriptions(DataCiteJson dirtyObject)
    {
        List<Description> descriptions = dirtyObject.descriptions;
        final List<Description> cleanedDescriptions;

        if (descriptions != null) {
            cleanedDescriptions = new LinkedList<>();

            // clean all
            descriptions.forEach((Description d) -> {
                Description cleanDescription =
                new Description(stringCleaner.clean(d.description), d.type);
                cleanDescription.lang = d.lang;
                cleanedDescriptions.add(cleanDescription) ;
            });
        } else
            cleanedDescriptions = null;

        return cleanedDescriptions;
    }

    private List<Subject> cleanSubjects(DataCiteJson dirtyObject)
    {
        Set<String> subjectTitles = new HashSet<>();

        List<Subject> subjects = dirtyObject.subjects;
        final List<Subject> cleanedSubjects;

        if (subjects != null) {
            cleanedSubjects = new LinkedList<>();

            // clean all
            subjects.forEach((Subject s) -> {
                // skip subject if it is duplicate
                if (subjectTitles.contains(s.subject))
                    return;

                // memorize subject to check for duplicates
                subjectTitles.add(s.subject);

                // create clean subject
                Subject cleanSubject = new Subject(stringCleaner.clean(s.subject));
                cleanSubject.lang = s.lang;
                cleanSubject.schemeURI = s.schemeURI;
                cleanSubject.subjectScheme = s.subjectScheme;
                cleanSubject.valueURI = s.valueURI;

                cleanedSubjects.add(cleanSubject) ;
            });
        } else
            cleanedSubjects = null;

        return cleanedSubjects;
    }

    private List<Rights> cleanRightsList(DataCiteJson dirtyObject)
    {
        List<Rights> rightsList = dirtyObject.rightsList;
        final List<Rights> cleanedRightsList;

        if (rightsList != null) {
            cleanedRightsList = new LinkedList<>();

            // clean all
            rightsList.forEach((Rights r) -> {
                Rights cleanRights = new Rights();
                cleanRights.rights = stringCleaner.clean(r.rights);
                cleanRights.URI = r.URI;

                cleanedRightsList.add(cleanRights) ;
            });
        } else
            cleanedRightsList = null;

        return cleanedRightsList;
    }

    private List<GeoLocation> cleanGeoLocations(DataCiteJson dirtyObject)
    {
        List<GeoLocation> geoLocations = dirtyObject.geoLocations;
        final List<GeoLocation> cleanedGeoLocations;

        if (geoLocations != null) {
            cleanedGeoLocations = new LinkedList<>();

            // clean all geo locations
            geoLocations.forEach((GeoLocation gl) -> {
                GeoLocation cleanLocation = new GeoLocation();

                // clean point and polygon of the geo location
                cleanLocation.point = geoCleaner.cleanTyped(gl.point);
                cleanLocation.polygon = geoCleaner.cleanTyped(gl.polygon);

                // clean free text name
                cleanLocation.place = stringCleaner.clean(gl.place);

                cleanedGeoLocations.add(cleanLocation) ;
            });
        } else
            cleanedGeoLocations = null;

        return cleanedGeoLocations;
    }
}
