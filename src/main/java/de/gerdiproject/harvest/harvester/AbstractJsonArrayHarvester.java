package de.gerdiproject.harvest.harvester;


import java.util.Collection;
import java.util.List;

import de.gerdiproject.json.IJsonArray;
import de.gerdiproject.json.IJsonObject;


public abstract class AbstractJsonArrayHarvester extends AbstractListHarvester<Object>
{
	/**
	 * Forwarding the superclass constructor.
	 *
	 * @param numberOfDocumentsPerEntry
	 *            the number of documents that are expected to be harvested from
	 *            each entry
	 */
	public AbstractJsonArrayHarvester( int numberOfDocumentsPerEntry )
	{
		super( numberOfDocumentsPerEntry );
	}


	/**
	 * Reads a single element from the JsonArray and creates at least one
	 * document.
	 * 
	 * @param entry
	 *            an element from the JsonArray
	 * @return a list of at least one document
	 */
	protected abstract List<IJsonObject> harvestJsonArrayEntry( IJsonObject entry );


	/**
	 * Retrieves the JsonArray that is to be harvested.
	 * 
	 * @return the JsonArray that is to be harvested
	 */
	protected abstract IJsonArray getJsonArray();


	@Override
	final protected Collection<Object> getEntries()
	{
		return getJsonArray();
	}


	@Override
	final protected List<IJsonObject> harvestEntry( Object entry )
	{
		return harvestJsonArrayEntry( (IJsonObject) entry );
	}
}
