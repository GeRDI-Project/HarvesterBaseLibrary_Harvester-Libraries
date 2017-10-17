package de.gerdiproject.harvest.event.impl;

import de.gerdiproject.harvest.IDocument;
import de.gerdiproject.harvest.event.IEvent;

public class DocumentHarvestedEvent implements IEvent
{
	private final IDocument document;
	
	public DocumentHarvestedEvent( IDocument doc)
	{
		document = doc;
	}

	
	public IDocument getDocument()
	{
		return document;
	}
}
