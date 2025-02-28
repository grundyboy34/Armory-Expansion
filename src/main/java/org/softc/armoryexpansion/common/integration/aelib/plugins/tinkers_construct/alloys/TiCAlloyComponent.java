package org.softc.armoryexpansion.common.integration.aelib.plugins.tinkers_construct.alloys;


import net.minecraft.nbt.NBTTagCompound;

public class TiCAlloyComponent{
    private String fluid;
    private int amount;

    public TiCAlloyComponent(String fluid, int amount) {
        this.fluid = fluid;
        this.amount = amount;
    }

    String getFluid() {
        return fluid;
    }

    int getAmount() {
        return amount;
    }

    NBTTagCompound getFluidTag(){
        NBTTagCompound fluid = new NBTTagCompound();
        fluid.setString("FluidName", this.fluid);
        fluid.setInteger("Amount", this.amount);
        return fluid;
    }
}
