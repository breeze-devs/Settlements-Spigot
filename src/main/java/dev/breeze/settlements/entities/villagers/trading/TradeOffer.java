package dev.breeze.settlements.entities.villagers.trading;

import dev.breeze.settlements.entities.villagers.BaseVillager;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;

import javax.annotation.Nonnull;

@Getter
@Setter
public class TradeOffer {

    @Nonnull
    private final BaseVillager seller;
    @Nonnull
    private final BaseVillager buyer;

    @Nonnull
    private final Material material;

    // Not final because the villagers can negotiate the price
    private int amount;
    private int price;

    public TradeOffer(@Nonnull BaseVillager seller, @Nonnull BaseVillager buyer, @Nonnull Material material, int initialAmount, int initialPrice) {
        this.seller = seller;
        this.buyer = buyer;
        this.material = material;
        this.amount = initialAmount;
        this.price = initialPrice;
    }

    /**
     * Copy constructor
     */
    public TradeOffer(@Nonnull TradeOffer other) {
        this.seller = other.seller;
        this.buyer = other.buyer;
        this.material = other.material;
        this.amount = other.amount;
        this.price = other.price;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof TradeOffer otherOffer)) {
            return false;
        }

        return this.seller.equals(otherOffer.seller)
                && this.buyer.equals(otherOffer.buyer)
                && this.material == otherOffer.material
                && this.amount == otherOffer.amount
                && this.price == otherOffer.price;
    }

}
