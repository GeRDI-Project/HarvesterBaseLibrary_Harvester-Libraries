package de.gerdiproject.harvest.state.impl;

import java.util.function.Consumer;

import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.event.impl.DocumentHarvestedEvent;
import de.gerdiproject.harvest.state.AbstractProgressHarvestState;

public class HarvestingState extends AbstractProgressHarvestState
{
    public HarvestingState(int maxNumberOfHarvestedDocuments)
    {
        super.maxProgress = maxNumberOfHarvestedDocuments;
    }

    @Override
    public void onStateEnter()
    {
        super.onStateEnter();
        EventSystem.instance().addListener(DocumentHarvestedEvent.class, onDocumentHarvested);
    }

    @Override
    public void onStateLeave()
    {
        EventSystem.instance().removeListener(DocumentHarvestedEvent.class, onDocumentHarvested);
    }

    @Override
    public String getName()
    {
        return "Harvesting";
    }

    // EVENTS

    /**
     * If a document is harvested, add 1 to the progress.
     */
    private Consumer<DocumentHarvestedEvent> onDocumentHarvested = (DocumentHarvestedEvent e) -> addProgress(1);


}
