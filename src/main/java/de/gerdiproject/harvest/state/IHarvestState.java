package de.gerdiproject.harvest.state;

public interface IHarvestState
{
    String getProgressString();
    void onStateEnter();
    void onStateLeave();
}
