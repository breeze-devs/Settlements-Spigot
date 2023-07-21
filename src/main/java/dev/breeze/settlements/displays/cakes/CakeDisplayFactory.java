package dev.breeze.settlements.displays.cakes;

import javax.annotation.Nonnull;

public interface CakeDisplayFactory {

    @Nonnull
    CakeDisplay createCakeDisplay();

}
