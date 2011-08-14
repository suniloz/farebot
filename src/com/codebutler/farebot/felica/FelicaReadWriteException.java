/*
 * FelicaReadWriteException.java
 *
 * Copyright (C) 2011 Vernon Tang
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.codebutler.farebot.felica;

import java.io.IOException;

public class FelicaReadWriteException extends IOException {
    private final short mStatusFlag2;

    public FelicaReadWriteException(short statusFlag2) {
        mStatusFlag2 = statusFlag2;
    }

    public short getStatusFlag2() {
        return mStatusFlag2;
    }

    private static final long serialVersionUID = -4884937872822855347L;
}
