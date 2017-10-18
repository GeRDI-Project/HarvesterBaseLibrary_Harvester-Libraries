package de.gerdiproject.harvest.state.impl;

import de.gerdiproject.harvest.state.IHarvestState;

/**
 * This state indicates it is waiting for a harvest to start.
 *
 * @author Robin Weiss
 */
public class NotReadyState implements IHarvestState
{
    private static final String CANNOT_HARVEST = "Cannot harvest! Read the logs for details.";


    @Override
    public String getProgressString()
    {
        return CANNOT_HARVEST;
    }

    @Override
    public void onStateEnter()
    {
        // nothing to do here
    }

    @Override
    public void onStateLeave()
    {
        // nothing to do here
    }

}
