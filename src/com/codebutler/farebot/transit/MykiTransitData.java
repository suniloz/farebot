/*
 * MykiTransitData.java
 *
 * Copyright (C) 2011 Sunil Jacob
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

import com.codebutler.farebot.Utils;
import com.codebutler.farebot.mifare.DesfireCard;
import com.codebutler.farebot.mifare.MifareCard;

public class MykiTransitData extends TransitData {

    private static final int MYKI_APP_ID_1 = 0x11f2;
    private static final int MYKI_APP_ID_2 = 0xf010f2;
	private String mykiMoney;

	@Override
	public String getCardName() {
		return "Myki";
	}
	
    public static boolean check(MifareCard card) {
    	return (card instanceof DesfireCard) && (((DesfireCard) card).getApplication(MYKI_APP_ID_1) != null);
    }

    public MykiTransitData (MifareCard card)
    {
        DesfireCard desfireCard = (DesfireCard) card;

        byte[] data_a1_f1,data_a1_f2,data_a2_f1,data_a2_f2;

        try {
            data_a1_f1 = desfireCard.getApplication(MYKI_APP_ID_1).getFile(0xf).getData();
            data_a1_f2 = desfireCard.getApplication(MYKI_APP_ID_1).getFile(0x0).getData();
            data_a2_f1 = desfireCard.getApplication(MYKI_APP_ID_2).getFile(0xf).getData();
            data_a2_f2 = desfireCard.getApplication(MYKI_APP_ID_2).getFile(0x0).getData();
            int a = Utils.byteArrayToInt(data_a1_f2);
            long b = Utils.byteArrayToLong(data_a1_f2, 0, data_a1_f2.length);
            String c = Utils.getHexString(data_a1_f2);
            System.out.println(c);
        } catch (Exception ex) {
            throw new RuntimeException("Error parsing Myki money", ex);
        }

    }

	@Override
	public String getBalanceString() {
		return "Bal: 0.52";
	}

	@Override
	public String getSerialNumber() {
		return "Serial: 1122";
	}

	@Override
	public Trip[] getTrips() {
		return null;
	}

	@Override
	public Refill[] getRefills() {
		return null;
	}

}
