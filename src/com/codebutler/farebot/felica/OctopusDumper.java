/*
 * OctopusDumper.java
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

public class OctopusDumper {
    public static final short OCTOPUS_SYS_CODE = (short)0x8008;
    public static final short OCTOPUS_PURSE_READ_PUBLIC_SVC = (short)0x0117;

    public static boolean check(short systemCode) {
        return OCTOPUS_SYS_CODE == systemCode;
    }

    public static void dump(DumperInterface dumper) throws IOException {
        dumper.dumpBlock(new FelicaBlockId(OCTOPUS_PURSE_READ_PUBLIC_SVC, 0, false));
    }
}
