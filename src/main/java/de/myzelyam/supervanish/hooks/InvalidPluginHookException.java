/*
 * Copyright © 2015, Leon Mangler and the SuperVanish contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package de.myzelyam.supervanish.hooks;

public class InvalidPluginHookException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidPluginHookException() {

    }

    public InvalidPluginHookException(String message) {

        super(message);

    }

    public InvalidPluginHookException(Throwable cause) {

        super(cause);

    }

}