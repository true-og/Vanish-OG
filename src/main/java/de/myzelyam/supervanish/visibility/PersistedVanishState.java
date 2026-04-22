/*
 * Copyright © 2015, Leon Mangler and the SuperVanish contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package de.myzelyam.supervanish.visibility;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class PersistedVanishState {

    private final Set<UUID> vanishedPlayers;
    private final Map<UUID, String> vanishedNames;
    private final Map<UUID, Boolean> itemPickUps;
    private final Map<String, Boolean> dismissed;

    public PersistedVanishState(Set<UUID> vanishedPlayers, Map<UUID, String> vanishedNames,
            Map<UUID, Boolean> itemPickUps, Map<String, Boolean> dismissed)
    {

        this.vanishedPlayers = new HashSet<>(vanishedPlayers);
        this.vanishedNames = new HashMap<>(vanishedNames);
        this.itemPickUps = new HashMap<>(itemPickUps);
        this.dismissed = new HashMap<>(dismissed);

    }

    public static PersistedVanishState empty() {

        return new PersistedVanishState(Set.of(), Map.of(), Map.of(), Map.of());

    }

    public Set<UUID> vanishedPlayers() {

        return new HashSet<>(vanishedPlayers);

    }

    public Map<UUID, String> vanishedNames() {

        return new HashMap<>(vanishedNames);

    }

    public Map<UUID, Boolean> itemPickUps() {

        return new HashMap<>(itemPickUps);

    }

    public Map<String, Boolean> dismissed() {

        return new HashMap<>(dismissed);

    }

}
