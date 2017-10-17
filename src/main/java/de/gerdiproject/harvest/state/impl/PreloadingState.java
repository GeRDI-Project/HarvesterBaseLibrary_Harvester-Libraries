package de.gerdiproject.harvest.state.impl;

import de.gerdiproject.harvest.state.IHarvestState;

/**
 * 
 * @author Robin Weiss
 *
 */
public class PreloadingState implements IHarvestState
{
	@Override
	public String getProgressString()
	{
		return "Preparing harvester...";
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
