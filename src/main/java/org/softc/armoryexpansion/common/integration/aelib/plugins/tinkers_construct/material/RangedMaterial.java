package org.softc.armoryexpansion.common.integration.aelib.plugins.tinkers_construct.material;

import org.softc.armoryexpansion.common.integration.aelib.config.MaterialConfigOptions;
import org.softc.armoryexpansion.common.integration.aelib.plugins.general.material.BasicMaterial;
import slimeknights.tconstruct.library.TinkerRegistry;
import slimeknights.tconstruct.library.materials.*;

import static slimeknights.tconstruct.library.materials.MaterialTypes.*;

public class RangedMaterial extends BasicMaterial implements IRangedMaterial {
    private BowMaterialStats bowMaterialStats;
    private BowStringMaterialStats bowStringMaterialStats;
    private ArrowShaftMaterialStats arrowShaftMaterialStats;
    private FletchingMaterialStats fletchingMaterialStats;
    private ProjectileMaterialStats projectileMaterialStats;

    @Override
    public BowMaterialStats getBowMaterialStats() {
        return bowMaterialStats;
    }

    @Override
    public BowStringMaterialStats getBowStringMaterialStats() {
        return bowStringMaterialStats;
    }

    @Override
    public ArrowShaftMaterialStats getArrowShaftMaterialStats() {
        return arrowShaftMaterialStats;
    }

    @Override
    public FletchingMaterialStats getFletchingMaterialStats() {
        return fletchingMaterialStats;
    }

    @Override
    public ProjectileMaterialStats getProjectileMaterialStats() {
        return projectileMaterialStats;
    }

    @Override
    public IRangedMaterial addPrimaryRangedTrait(String trait) {
        // TODO Figure out where to apply traits
        return (IRangedMaterial) this.addTrait(trait, BOW);
    }

    @Override
    public IRangedMaterial addSecondaryRangedTrait(String trait) {
        // TODO Figure out where to apply traits
        return (IRangedMaterial) this.addTrait(trait, BOWSTRING);
    }

    @Override
    public IRangedMaterial addGlobalRangedTrait(String trait) {
        // TODO Figure out where to apply traits
        return this.addPrimaryRangedTrait(trait).addSecondaryRangedTrait(trait);
    }

    @Override
    public IRangedMaterial addRangedTrait(String trait1, String trait2) {
        // TODO Figure out where to apply traits
        return this.addPrimaryRangedTrait(trait1).addSecondaryRangedTrait(trait2);
    }

    @Override
    public boolean isToolMaterial() {
        return false;
    }

    @Override
    public boolean isArmorMaterial() {
        return false;
    }

    @Override
    public boolean isRangedMaterial() {
        return this.getArrowShaftMaterialStats() != null
                || this.getBowMaterialStats() != null
                || this.getBowStringMaterialStats() != null
                || this.getFletchingMaterialStats() != null
                || this.getProjectileMaterialStats() != null;
    }

    @Override
    public boolean registerTinkersMaterialStats(MaterialConfigOptions properties) {
        if (properties.isMaterialEnabled()) {
            Material material = TinkerRegistry.getMaterial(this.getIdentifier());
            if ("unknown".equals(material.getIdentifier())){
                return false;
            }
            this.registerRangedStats(material, properties);
            return true;
        }
        return false;
    }

    private void registerRangedStats(Material material, MaterialConfigOptions properties){
        if(this.isRangedMaterial()){
            this.registerBowStats(material, properties);
            this.registerBowStringStats(material, properties);
            this.registerFletchingStats(material, properties);
            this.registerProjectileStats(material, properties);
        }
    }

    private void registerBowStats(Material material, MaterialConfigOptions properties){
        if(material.getStats(BOW) == null && this.getBowMaterialStats() != null && properties.isBowEnabled()){
            TinkerRegistry.addMaterialStats(material, this.getBowMaterialStats());
        }
    }

    private void registerBowStringStats(Material material, MaterialConfigOptions properties){
        if(material.getStats(BOWSTRING) == null && this.getBowStringMaterialStats() != null && properties.isBowStringEnabled()){
            TinkerRegistry.addMaterialStats(material, this.getBowStringMaterialStats());
        }
    }

    private void registerFletchingStats(Material material, MaterialConfigOptions properties){
        if(material.getStats(FLETCHING) == null && this.getFletchingMaterialStats() != null && properties.isFletchingEnabled()){
            TinkerRegistry.addMaterialStats(material, this.getFletchingMaterialStats());
        }
    }

    private void registerProjectileStats(Material material, MaterialConfigOptions properties){
        if(material.getStats(PROJECTILE) == null && this.getProjectileMaterialStats() != null && properties.isProjectileEnabled()){
            TinkerRegistry.addMaterialStats(material, this.getProjectileMaterialStats());
        }
    }

    public void setBowMaterialStats(BowMaterialStats bowMaterialStats) {
        this.bowMaterialStats = bowMaterialStats;
    }

    public void setBowStringMaterialStats(BowStringMaterialStats bowStringMaterialStats) {
        this.bowStringMaterialStats = bowStringMaterialStats;
    }

    public void setArrowShaftMaterialStats(ArrowShaftMaterialStats arrowShaftMaterialStats) {
        this.arrowShaftMaterialStats = arrowShaftMaterialStats;
    }

    public void setFletchingMaterialStats(FletchingMaterialStats fletchingMaterialStats) {
        this.fletchingMaterialStats = fletchingMaterialStats;
    }

    public void setProjectileMaterialStats(ProjectileMaterialStats projectileMaterialStats) {
        this.projectileMaterialStats = projectileMaterialStats;
    }
}
