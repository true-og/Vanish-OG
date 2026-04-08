/*
 * Copyright © 2015, Leon Mangler and the SuperVanish contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package de.myzelyam.supervanish.features;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;

import de.myzelyam.supervanish.SuperVanish;
import de.myzelyam.supervanish.utils.Requirement;

public class FeatureMgr {

    private static final Requirement<FeatureInfo> protocolLibInstalled = featureInfo -> Bukkit.getPluginManager()
            .isPluginEnabled("ProtocolLib");
    private static final Requirement<FeatureInfo> oneDotEightOrHigher = featureInfo -> featureInfo.getPlugin()
            .getVersionUtil().isOneDotXOrHigher(8);
    private static final Requirement<FeatureInfo> oneDotSeventeenOrHigher = featureInfo -> featureInfo.getPlugin()
            .getVersionUtil().isOneDotXOrHigher(17);
    private static final Requirement<FeatureInfo> supportedServer = featureInfo -> "Paper"
            .equals(Bukkit.getServer().getName()) || "Purpur".equals(Bukkit.getServer().getName());
    private final Map<String, FeatureInfo> registeredFeatures = new HashMap<>();
    private final Set<Feature> activeFeatures = new HashSet<>();
    private final SuperVanish plugin;

    public FeatureMgr(SuperVanish plugin) {

        this.plugin = plugin;
        registeredFeatures.put("SilentOpenChest", new FeatureInfo(SilentOpenChest.class, plugin,
                Arrays.asList(protocolLibInstalled, oneDotEightOrHigher)));
        registeredFeatures.put("NightVision",
                new FeatureInfo(NightVision.class, plugin, Arrays.asList(protocolLibInstalled, oneDotEightOrHigher)));
        registeredFeatures.put("VanishIndication", new FeatureInfo(VanishIndication.class, plugin,
                Arrays.asList(protocolLibInstalled, oneDotEightOrHigher)));
        registeredFeatures.put("NoSculkSensorDetection", new FeatureInfo(NoSculkSensorDetection.class, plugin,
                Collections.singletonList(oneDotSeventeenOrHigher)));
        registeredFeatures.put("NoTurtleEggBreaking",
                new FeatureInfo(NoTurtleEggBreaking.class, plugin, Collections.singletonList(oneDotSeventeenOrHigher)));
        registeredFeatures.put("NoDripLeafTilt",
                new FeatureInfo(NoDripLeafTilt.class, plugin, Collections.singletonList(oneDotSeventeenOrHigher)));
        registeredFeatures.put("NoRaidTrigger",
                new FeatureInfo(NoRaidTrigger.class, plugin, Collections.singletonList(oneDotSeventeenOrHigher)));
        registeredFeatures.put("NoMobSpawn",
                new FeatureInfo(NoMobSpawn.class, plugin, Collections.singletonList(supportedServer)));

    }

    public void enableFeatures() {

        featureLoop: for (String id : registeredFeatures.keySet()) {

            final FeatureInfo featureInfo = registeredFeatures.get(id);
            for (Requirement<FeatureInfo> requirement : featureInfo.getRequirements()) {

                if (!requirement.fulfilledBy(featureInfo)) {

                    continue featureLoop;

                }

            }

            final Feature feature;
            try {

                feature = featureInfo.getFeatureClass().getConstructor(SuperVanish.class).newInstance(plugin);

            } catch (NoSuchMethodException | InvocationTargetException | InstantiationException
                    | IllegalAccessException e)
            {

                plugin.logException(e);
                continue;

            }

            if (!feature.isActive()) {

                continue;

            }

            activeFeatures.add(feature);

            Bukkit.getPluginManager().registerEvents(feature, plugin);

            feature.onEnable();

        }

    }

    public void disableFeatures() {

        activeFeatures.forEach(feature -> {

            feature.onDisable();
            HandlerList.unregisterAll(feature);

        });

        activeFeatures.clear();

    }

    @SuppressWarnings("unchecked")
    public <T extends Feature> T getFeature(Class<T> featureClass) {

        // noinspection unchecked on purpose
        return activeFeatures.stream().filter(feature -> feature.getClass().equals(featureClass)).findFirst()
                .map(feature -> (T) feature).orElse(null);

    }

    public Set<Feature> getActiveFeatures() {

        return activeFeatures;

    }

    private record FeatureInfo(Class<? extends Feature> featureClass, SuperVanish plugin,
            Collection<Requirement<FeatureInfo>> requirements)
    {

        FeatureInfo(Class<? extends Feature> featureClass, SuperVanish plugin) {

            this(featureClass, plugin, Collections.emptySet());

        }

        public Class<? extends Feature> getFeatureClass() {

            return featureClass;

        }

        public Collection<Requirement<FeatureInfo>> getRequirements() {

            return requirements;

        }

        public SuperVanish getPlugin() {

            return plugin;

        }

    }

}
