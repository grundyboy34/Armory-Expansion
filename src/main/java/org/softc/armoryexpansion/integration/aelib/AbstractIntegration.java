package org.softc.armoryexpansion.integration.aelib;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.minecraft.block.Block;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.commons.io.input.BoundedInputStream;
import org.apache.logging.log4j.Logger;
import org.softc.armoryexpansion.ArmoryExpansion;
import org.softc.armoryexpansion.integration.plugins.tinkers_construct.ITiCMaterial;
import org.softc.armoryexpansion.integration.plugins.tinkers_construct.TiCMaterial;
import org.softc.armoryexpansion.integration.plugins.tinkers_construct.alloys.TiCAlloy;
import org.softc.armoryexpansion.integration.web.ArmoryExpansionWebClient;

import java.io.*;
import java.util.*;

import static org.softc.armoryexpansion.integration.aelib.Config.CATEGORY_MATERIAL;

public abstract class AbstractIntegration{
    protected Logger logger;
    protected String modid = "";
    Config configHelper;
    boolean isEnabled = false;
    private boolean forceCreateJson = false;
    protected Map<String, ITiCMaterial> materials = new HashMap<>();
    private Map<String, TiCAlloy> alloys = new HashMap<>();

    protected ArmoryExpansionWebClient webClient;

    public void preInit(FMLPreInitializationEvent event) {
        this.logger = event.getModLog();
        this.webClient = ArmoryExpansionWebClient.getInstance();
        Property property = ArmoryExpansion.config
                .get("integrations", modid, true, "Whether integration with " + modid + " should be enabled");
        isEnabled = property == null || property.getBoolean();
        ArmoryExpansion.config.save();
        if(isEnabled){
            this.configHelper = new Config(new Configuration(
                    new File(event.getModConfigurationDirectory().getPath() + "/" + ArmoryExpansion.MODID + "/" + modid + ".cfg")));
            this.setMaterials(event);
            this.setAlloys(event);
            this.configHelper.syncConfig(materials);
            this.registerMaterials();
//            this.registerMaterialFluids();
            this.registerAlloys();
            this.registerMaterialStats();
        }
    }

    public void init(FMLInitializationEvent event) {
        if(isEnabled){
            this.oredictMaterials();
            this.registerMaterialFluidsIMC();
            this.updateMaterials();
            this.registerMaterialTraits();
        }
    }

    public void postInit(FMLPostInitializationEvent event){
        // Used as a stub
    }

    public void registerBlocks(RegistryEvent.Register<Block> event){
        this.registerMaterialFluids();
        this.registerFluidBlocks(event);
    }

    private void registerFluidBlocks(RegistryEvent.Register<Block> event){
        this.materials.values().forEach(m -> {
            if(m.isCastable()){
                event.getRegistry().registerAll(m.getFluidBlock());
                this.logger.info("Registered fluid block for material {" + m.getIdentifier() + "};");
            }
        });
    }

    public Configuration getConfiguration() {
        return this.configHelper.getConfiguration();
    }

    private Config getConfigHelper() {
        return this.configHelper;
    }

    protected void addMaterial(ITiCMaterial material){
        if(isMaterialEnabled(material)){
            this.materials.putIfAbsent(material.getIdentifier(), material);
        }
    }

    protected void setMaterials(FMLPreInitializationEvent event){
        if(ArmoryExpansion.useServersForJsons()){
            this.loadMaterialsFromWeb();
            this.logger.info("Done loading all materials from web servers");
        }
        else{
            this.loadMaterialsFromJson(event.getModConfigurationDirectory(), this.modid);
            this.logger.info("Done loading all materials from local JSON files");
            this.loadMaterialsFromSource();
            this.logger.info("Done loading all materials from source");
            this.saveMaterialsToJson(event.getModConfigurationDirectory(), this.modid, this.forceCreateJson);
            this.logger.info("Done saving all materials to local JSON files");
        }
    }

    protected abstract void loadMaterialsFromWeb();

    protected abstract void loadMaterialsFromSource();

    void setAlloys(FMLPreInitializationEvent event){
        if(ArmoryExpansion.useServersForJsons()){
            this.loadAlloysFromWeb();
            this.logger.info("Done loading all alloys from web servers");
        }
        else {
            this.loadAlloysFromJson(event.getModConfigurationDirectory(), this.modid);
            this.logger.info("Done loading all alloys from local JSON files");
            this.loadAlloysFromSource();
            this.logger.info("Done loading all alloys from source");
            this.saveAlloysToJson(event.getModConfigurationDirectory(), this.modid, this.forceCreateJson);
            this.logger.info("Done saving all alloys to local JSON files");
        }
    }

    protected abstract void loadAlloysFromWeb();

    protected abstract void loadAlloysFromSource();

    private void loadMaterials(TiCMaterial[] jsonMaterials){
        if(jsonMaterials == null){
            return;
        }
        for(TiCMaterial m:jsonMaterials){
            this.materials.putIfAbsent(m.getIdentifier(), m);
        }
    }

    void loadMaterialsFromJson(InputStream path){
        Gson gson = new GsonBuilder().setPrettyPrinting().setLenient().create();

        TiCMaterial[] jsonMaterials = gson.fromJson(new BufferedReader(new InputStreamReader(new BoundedInputStream(path, 131072))), TiCMaterial[].class);
        this.loadMaterials(jsonMaterials);
    }

    private void loadMaterialsFromJson(String path){
        Gson gson = new GsonBuilder().setPrettyPrinting().setLenient().create();

        TiCMaterial[] jsonMaterials = new TiCMaterial[0];
        try {
            File input = new File(path);
            if(input.exists()){
                jsonMaterials = gson.fromJson(new FileReader(input), TiCMaterial[].class);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        this.loadMaterials(jsonMaterials);
    }

    private void loadMaterialsFromJson(File configDir, String root, String modid){
        this.loadMaterialsFromJson(configDir.getPath() + "/" + root + "/" + modid + ".json");
    }

    private void loadMaterialsFromJson(File configDir, String modid){
        this.loadMaterialsFromJson(configDir, "armoryexpansion", modid + "-materials");
    }

    private void saveMaterialsToJson(String path, boolean forceCreate){
        if(materials.values().size() <= 0 && !forceCreate){
            return;
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().setLenient().create();
        File output = new File(path);
        //noinspection ResultOfMethodCallIgnored
        output.getParentFile().mkdirs();
        try {
            FileWriter writer = new FileWriter(output);
            writer.write(returnMaterialExample());
            writer.write(gson.toJson(materials.values()));
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveMaterialsToJson(File configDir, String root, String modid, boolean forceCreate){
        this.saveMaterialsToJson(configDir.getPath() + "/" + root + "/" + modid + ".json", forceCreate);
    }

    private void saveMaterialsToJson(File configDir, String modid, boolean forceCreate){
        this.saveMaterialsToJson(configDir, "armoryexpansion", modid + "-materials", forceCreate);
    }

    private void loadAlloys(TiCAlloy[] jsonAlloys){
        if(jsonAlloys == null){
            return;
        }
        for(TiCAlloy a:jsonAlloys){
            this.alloys.putIfAbsent(a.getName(), a);
        }
    }

    void loadAlloysFromJson(InputStream path){
        Gson gson = new GsonBuilder().setPrettyPrinting().setLenient().create();

        TiCAlloy[] jsonAlloys = new TiCAlloy[0];
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new BoundedInputStream(path, 131072)));
            jsonAlloys = gson.fromJson(reader, TiCAlloy[].class);
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.loadAlloys(jsonAlloys);
    }

    private void loadAlloysFromJson(String path){
        Gson gson = new GsonBuilder().setPrettyPrinting().setLenient().create();
        TiCAlloy[] jsonAlloys = new TiCAlloy[0];
        try {
            File input = new File(path);
            if(input.exists()){
                jsonAlloys = gson.fromJson(new FileReader(input), TiCAlloy[].class);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        loadAlloys(jsonAlloys);
    }

    private void loadAlloysFromJson(File configDir, String root, String modid){
        this.loadAlloysFromJson(configDir.getPath() + "/" + root + "/" + modid + ".json");
    }

    private void loadAlloysFromJson(File configDir, String modid){
        this.loadAlloysFromJson(configDir, "armoryexpansion", modid + "-alloys");
    }

    private void saveAlloysToJson(String path, boolean forceCreate){
        if(alloys.values().size() <= 0 && !forceCreate){
            return;
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().setLenient().create();
        File output = new File(path);
        //noinspection ResultOfMethodCallIgnored
        output.getParentFile().mkdirs();
        try {
            FileWriter writer = new FileWriter(output);
            writer.write(returnAlloyExample());
            writer.write(gson.toJson(alloys.values()));
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveAlloysToJson(File configDir, String root, String modid, boolean forceCreate){
        this.saveAlloysToJson(configDir.getPath() + "/" + root + "/" + modid + ".json", forceCreate);
    }

    private void saveAlloysToJson(File configDir, String modid, boolean forceCreate){
        this.saveAlloysToJson(configDir, "armoryexpansion", modid + "-alloys", forceCreate);
    }

    private void oredictMaterials() {
        this.materials.values().forEach(m -> {
            m.registerOreDict();
            this.logger.info("Oredicted material {" + m.getIdentifier() + "};");
        });
    }

    void registerMaterials() {
        this.materials.values().forEach(m -> {
            if (m.registerTinkersMaterial(this.isMaterialEnabled(m))) {
                this.logger.info("Registered tinker's material {" + m.getIdentifier() + "};");
            }
        });
    }

    private void registerMaterialFluids() {
        this.materials.values().forEach(m -> {
            if (m.registerTinkersFluid(this.isMaterialEnabled(m) && this.isMaterialFluidEnabled(m))) {
                this.logger.info("Registered fluid for material {" + m.getIdentifier() + "};");
            }
        });
    }

    private void registerMaterialFluidsIMC(){
        this.materials.values().forEach(m -> {
            if (m.registerTinkersFluidIMC(this.isMaterialEnabled(m) && this.isMaterialFluidEnabled(m))) {
                this.logger.info("Sent IMC for tinker's fluid {" + m.getFluidName() + "};");
            }
        });
    }

    void registerAlloys(){
        this.alloys.values().forEach(a -> {
            a.registerTiCAlloy();
            this.logger.info("Sent IMC for tinker's alloy {" + a.getName() + "};");
        });
    }

    void registerMaterialStats() {
        this.materials.values().forEach(m -> {
            if (m.registerTinkersMaterialStats(getProperties(m), this.isMaterialEnabled(m))) {
                this.logger.info("Registered stats for tinker's material {" + m.getIdentifier() + "};");
            }
        });
    }

    private void updateMaterials() {
        this.materials.values().forEach(m -> {
            if (m.updateTinkersMaterial(this.isMaterialEnabled(m))) {
                this.logger.info("Updated tinker's material {" + m.getIdentifier() + "};");
            }
        });
    }

    private void registerMaterialTraits() {
        this.materials.values().forEach(m -> {
            if (m.registerTinkersMaterialTraits(this.isMaterialEnabled(m))) {
                this.logger.info("Registered traits for tinker's material {" + m.getIdentifier() + "};");
            }
        });
    }

    private Map<String, Property> getProperties(ITiCMaterial material){
        return this.getConfigHelper().getProperties(material.getIdentifier());
    }

    private Property getProperty(ITiCMaterial material, String property){
        Map<String, Property> properties = getProperties(material);
        if(properties != null && properties.containsKey(property)){
            return this.getProperties(material).get(property);
        }
        return null;
    }

    public boolean isEnabled(ITiCMaterial material, String property){
        return (this.getProperty(material, property) != null) && Objects.requireNonNull(this.getProperty(material, property)).getBoolean();
    }

    private boolean isMaterialEnabled(ITiCMaterial material){
        Property property = getProperty(material, CATEGORY_MATERIAL);
        if (property != null){
            return property.getBoolean();
        }
        return true;
    }

    private boolean isMaterialFluidEnabled(ITiCMaterial material){
        Property property = getProperty(material, CATEGORY_MATERIAL);
        if (property != null){
            return property.getBoolean();
        }
        return false;
    }

    protected void enableForceJsonCreation(){
        this.forceCreateJson = true;
    }

    private String returnMaterialExample(){
        return  "//  {\n" +
                "//    The material's durability is this value multiplied by 8\n" +
                "//    \"durability\": 36,\n" +
                "//    The material's mining speed is this value multiplied by 0.65\n" +
                "//    \"miningSpeed\": 0.0,\n" +
                "//    The material's attack is this value | The material's bonus damage is this value divided by 3\n" +
                "//    \"damage\": 0.0,\n" +
                "//    The material's modifier is this value divided by 6 plus 0.1\n" +
                "//    \"magicAffinity\": 15.0,\n" +
                "//    The material's harvest level is this value\n" +
                "//    \"harvestLevel\": 0,\n" +
                "//    The material's range is this value\n" +
                "//    \"range\": 0.0,\n" +
                "//    The material's accuracy is this value with 1.0 being 100%\n" +
                "//    \"accuracy\": 0.0,\n" +
                "//    The material's defense is this value multiplied by 2\n" +
                "//    \"defense\": 9.0,\n" +
                "//    The material's toughness is this value multiplied by 1.5\n" +
                "//    \"toughness\": 2.0,\n" +
                "//    The material's traits\n" +
                "//    \"traits\": [\n" +
                "//      {\n" +
                "//        The trait's identifier\n" +
                "//        \"traitName\": \"rough_armor\",\n" +
                "//        The tool part to have the trait\n" +
                "//        \"traitPart\": \"core\"\n" +
                "//      }\n" +
                "//    ],\n" +
                "//    The material's oreDict entry and identifier\n" +
                "//    \"identifier\": \"scalesilverdragon\",\n" +
                "//    The item to be added to the material's oreDict entry\n" +
                "//    \"itemName\": \"iceandfire:dragonscales_silver\",\n" +
                "//    The item's meta value\n" +
                "//    \"meta\": 0,\n" +
                "//    The material's color\n" +
                "//    \"color\": 11184810,\n" +
                "//    The material render info to be used for the material's aspect (unused at the moment)\n" +
                "//    \"type\": \"METAL\",\n" +
                "//    If the material should be castable with a casting table or casting basin (no fluids are added to the materials at the moment)\n" +
                "//    \"isCastable\": false,\n" +
                "//    If the material should be craftable in the part crafter\n" +
                "//    \"isCraftable\": true,\n" +
                "//    If the material should be useable for tool parts\n" +
                "//    \"isToolMaterial\": false,\n" +
                "//    If the material should be useable for bow limbs\n" +
                "//    \"isBowMaterial\": false,\n" +
                "//    If the material should be useable for fletchings\n" +
                "//    \"isFletchingMaterial\": false,\n" +
                "//    If the material should be useable for projectile parts\n" +
                "//    \"isProjectileMaterial\": false,\n" +
                "//    If the material should be useable for armor parts\n" +
                "//    \"isArmorMaterial\": true\n" +
                "//  }\n";
    }

    private String returnAlloyExample() {
        return "//  {\n" +
                "//    \"output\": {\n" +
                "//      \"fluid\": \"iron\",\n" +
                "//      \"amount\": 144\n" +
                "//    },\n" +
                "//    \"inputs\": [\n" +
                "//      {\n" +
                "//        \"fluid\": \"copper\",\n" +
                "//        \"amount\": 108\n" +
                "//      },\n" +
                "//      {\n" +
                "//        \"fluid\": \"lead\",\n" +
                "//        \"amount\": 36\n" +
                "//      }\n" +
                "//    ]\n" +
                "//  }\n";
    }
}
