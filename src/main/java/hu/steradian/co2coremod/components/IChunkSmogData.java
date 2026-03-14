package hu.steradian.co2coremod.components;

import hu.steradian.co2coremod.smog.SmogLevel;

import org.ladysnake.cca.api.v3.component.Component;

public interface IChunkSmogData extends Component {
    int getSmogAmount();
    void setSmogAmount(int level);

    SmogLevel getSmogLevel();

    boolean isInitialized();
    void setInitialized(boolean initialized);
}
