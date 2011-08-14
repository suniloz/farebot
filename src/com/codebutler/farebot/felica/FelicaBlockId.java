/*
 * FelicaBlockId.java
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

public class FelicaBlockId implements Comparable<FelicaBlockId> {
    private final short mServiceCode;
    private final int mBlockNum;
    private final boolean mCashback;

    public FelicaBlockId(short serviceCode, int blockNum, boolean cashback) {
        mServiceCode = serviceCode;
        mBlockNum = blockNum & 0xffff;
        mCashback = cashback;
    }

    public byte[] toBlockListElement() {
        byte ble[];

        if (mBlockNum > 0xff) {
            ble = new byte[3];
            ble[0] = 0;
            ble[2] = (byte)((mBlockNum >> 8) & 0xff);
        } else {
            ble = new byte[2];
            ble[0] = (byte)(1 << 7);
        }
        ble[1] = (byte)(mBlockNum & 0xff);
        if (mCashback)
            ble[0] |= 1 << 4;

        return ble;
    }

    @Override
    public String toString() {
        String x = Integer.toString(mBlockNum | ((mServiceCode & 0xffff) << 16), 16);
        if (mCashback)
            return "c" + x;
        else
            return x;
    }

    public static FelicaBlockId valueOf(String str) throws IllegalArgumentException {
        try {
            boolean cashback = false;
            if (str.charAt(0) == 'c') {
                cashback = true;
                str = str.substring(1);
            }

            Integer x = Integer.valueOf(str, 16);

            return new FelicaBlockId((short)(x >> 16), x & 0xffff, cashback);
        } catch (IndexOutOfBoundsException e) {
            throw new IllegalArgumentException();
        }
    }

    public short getServiceCode() {
        return mServiceCode;
    }

    public int getBlockNum() {
        return mBlockNum;
    }

    public boolean getCashback() {
        return mCashback;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + mBlockNum;
        result = prime * result + (mCashback ? 1231 : 1237);
        result = prime * result + mServiceCode;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof FelicaBlockId))
            return false;
        FelicaBlockId other = (FelicaBlockId) obj;
        if (mBlockNum != other.mBlockNum)
            return false;
        if (mCashback != other.mCashback)
            return false;
        if (mServiceCode != other.mServiceCode)
            return false;
        return true;
    }

    @Override
    public int compareTo(FelicaBlockId other) {
        if (mServiceCode < other.mServiceCode)
            return -1;
        else if (mServiceCode > other.mServiceCode)
            return 1;

        if (mBlockNum < other.mBlockNum)
            return -1;
        else if (mBlockNum > other.mBlockNum)
            return 1;

        if (!mCashback && other.mCashback)
            return -1;
        else if (mCashback && !other.mCashback)
            return 1;

        return 0;
    }
}
