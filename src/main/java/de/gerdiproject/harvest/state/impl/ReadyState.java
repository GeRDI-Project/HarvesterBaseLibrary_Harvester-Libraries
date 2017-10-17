package de.gerdiproject.harvest.state.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gerdiproject.harvest.MainContext;
import de.gerdiproject.harvest.harvester.AbstractHarvester;
import de.gerdiproject.harvest.state.HarvestStateMachine;
import de.gerdiproject.harvest.state.IHarvestState;

/**
 * This state indicates it is waiting for a harvest to start.
 * 
 * @author Robin Weiss
 */
public class ReadyState implements IHarvestState
{
    private static final String HARVEST_NOT_STARTED = "Ready to harvest";
    private static final String HARVEST_DONE = "Harvested finished at %s";
    private static final String READY = "%s is now ready!";
    
    private static final Logger LOGGER = LoggerFactory.getLogger(HarvestStateMachine.class);
    
	@Override
	public String getProgressString()
	{
		AbstractHarvester mainHarvester = MainContext.getHarvester();
		
		if( mainHarvester.isFinished())
			return String.format( HARVEST_DONE, mainHarvester.getHarvestDate().toString() );
		else
			return HARVEST_NOT_STARTED;
	}

	@Override
	public void onStateEnter()
	{
		LOGGER.info(String.format(READY, MainContext.getModuleName()));
	}

	@Override
	public void onStateLeave()
	{
		// nothing to do here
	}

}
