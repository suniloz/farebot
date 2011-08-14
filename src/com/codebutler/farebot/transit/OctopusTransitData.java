/*
 * OctopusTransitData.java
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

package com.codebutler.farebot.transit;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

import com.codebutler.farebot.felica.FelicaBlockId;
import com.codebutler.farebot.felica.FelicaCard;
import com.codebutler.farebot.mifare.MifareCard;

public class OctopusTransitData extends TransitData {
    private static final short OCTOPUS_SYS_CODE = (short)0x8008;
    private static final short OCTOPUS_PURSE_READ_PUBLIC_SVC = (short)0x0117;

    private final FelicaCard mCard;
    private final Number mBalance;  // in multiples of $0.1

    public static boolean check(MifareCard card) {
        if (!(card instanceof FelicaCard))
            return false;

        FelicaCard felicaCard = (FelicaCard)card;
        return felicaCard.getSystemCode() == OCTOPUS_SYS_CODE;
    }

    public OctopusTransitData(MifareCard card) {
        mCard = (FelicaCard)card;
        byte[] purseData = mCard.getBlocks().get(
                new FelicaBlockId(OCTOPUS_PURSE_READ_PUBLIC_SVC, 0, false));
        if (purseData != null) {
            long balance = (long)((purseData[0] & 0xff) << 24 |
                    (purseData[1] & 0xff) << 16 |
                    (purseData[2] & 0xff) << 8 |
                    (purseData[3] & 0xff)) - 350;
            mBalance = BigDecimal.valueOf(balance, 1);
        } else
            mBalance = null;
    }

    @Override
    public String getBalanceString() {
        if (mBalance == null)
            return "???";

        return NumberFormat.getCurrencyInstance(new Locale("en", "HK")).format(mBalance);
    }

    @Override
    public String getSerialNumber() {
        return "(unknown)";
    }

    @Override
    public Trip[] getTrips() {
        return null;
    }

    @Override
    public Refill[] getRefills() {
        return null;
    }

    @Override
    public String getCardName() {
        return "Octopus";
    }

}
