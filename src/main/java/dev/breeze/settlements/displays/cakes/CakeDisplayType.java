package dev.breeze.settlements.displays.cakes;

import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.annotation.Nonnull;

@AllArgsConstructor
public enum CakeDisplayType {

    WHITE(new WhiteCakeDisplayFactory()),
    ORANGE(new WhiteCakeDisplayFactory()), // TODO
    MAGENTA(new WhiteCakeDisplayFactory()), // TODO
    LIGHT_BLUE(new WhiteCakeDisplayFactory()), // TODO
    YELLOW(new WhiteCakeDisplayFactory()), // TODO
    LIME(new WhiteCakeDisplayFactory()), // TODO
    PINK(new WhiteCakeDisplayFactory()), // TODO
    GRAY(new WhiteCakeDisplayFactory()), // TODO
    LIGHT_GRAY(new WhiteCakeDisplayFactory()), // TODO
    CYAN(new WhiteCakeDisplayFactory()), // TODO
    PURPLE(new WhiteCakeDisplayFactory()), // TODO
    BLUE(new WhiteCakeDisplayFactory()), // TODO
    BROWN(new WhiteCakeDisplayFactory()), // TODO
    GREEN(new WhiteCakeDisplayFactory()), // TODO
    RED(new WhiteCakeDisplayFactory()), // TODO
    BLACK(new WhiteCakeDisplayFactory()); // TODO

    @Nonnull
    @Getter
    private final CakeDisplayFactory cakeDisplayFactory;

}
