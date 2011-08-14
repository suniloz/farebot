/*
 * FelicaProtocol.java
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import android.nfc.Tag;
import android.nfc.tech.NfcF;

public class FelicaProtocol {
    static final byte CMD_REQUEST_SERVICE = 0x02;
    static final byte CMD_REQUEST_RESPONSE = 0x04;
    static final byte CMD_READ_WITHOUT_ENCRYPTION = 0x06;

    private final NfcF mTech;
    private final byte[] mIdm;

    public FelicaProtocol(Tag tag) {
        mTech = NfcF.get(tag);
        assert mTech != null;
        mIdm = tag.getId();
        if (mIdm.length != 8)
            throw new IllegalStateException("card IDm has incorrect length");
    }

    /**
     * Checks whether the card is still in the field.
     *
     * @return the current mode of the card
     * @throws IOException
     */
    public byte requestResponse() throws IOException {
        byte request[] = new byte[9];
        request[0] = CMD_REQUEST_RESPONSE;
        System.arraycopy(mIdm, 0, request, 1, 8);

        byte response[] = transceive(request);
        if (response.length < 10 ||
            response[0] != CMD_REQUEST_RESPONSE + 1)
            throw new FelicaProtocolException();

        return response[9];
    }

    /**
     * Checks whether the services with the given service codes exist and
     * their corresponding key versions.
     *
     * @param serviceList a list of up to 32 service codes
     * @param versions a map from service code to key version. Service codes
     * with no corresponding service are deleted from the map.
     * @throws IOException
     */
    public void requestService(List<Short> serviceList,
                                 Map<Short, Integer> versions) throws IOException {
        int size = serviceList.size();
        if (size > 32)
            throw new IllegalArgumentException("too many services requested");

        byte request[] = new byte[10 + 2*size];
        request[0] = CMD_REQUEST_SERVICE;
        System.arraycopy(mIdm, 0, request, 1, 8);
        request[9] = (byte)size;
        for (int i = 0; i < size; i++) {
            short service = serviceList.get(i);
            request[10+2*i] = (byte)(service & 0xff);
            request[10+2*i+1] = (byte)((service >> 8) & 0xff);
        }

        byte response[] = transceive(request);
        if (response.length < 10 + 2*size ||
            response[0] != CMD_REQUEST_SERVICE + 1)
            throw new FelicaProtocolException();

        for (int i = 0; i < size; i++) {
            int version = (response[10+2*i] & 0xff) |
                          ((response[10+2*i+1] & 0xff) << 8);
            if (version == 0xffff)
                versions.remove(serviceList.get(i));
            else
                versions.put(serviceList.get(i), version);
        }
    }

    /**
     * Reads a block of data from an unprotected service.
     *
     * @param blockId the block ID to read
     * @return
     * @throws IOException
     */
    public byte[] readWithoutEncryption(FelicaBlockId blockId) throws IOException {
        byte[] ble = blockId.toBlockListElement();

        byte request[] = new byte[13 + ble.length];
        request[0] = CMD_READ_WITHOUT_ENCRYPTION;
        System.arraycopy(mIdm, 0, request, 1, 8);
        request[9] = 1;
        short serviceCode = blockId.getServiceCode();
        request[10] = (byte)(serviceCode & 0xff);
        request[11] = (byte)((serviceCode >> 8) & 0xff);
        request[12] = 1;
        System.arraycopy(ble, 0, request, 13, ble.length);

        byte response[] = transceive(request);
        if (response.length < 11 ||
            response[0] != CMD_READ_WITHOUT_ENCRYPTION + 1)
            throw new FelicaProtocolException();
        if (response[9] != 0 || response[10] != 0)
            throw new FelicaReadWriteException((short)(response[10] & 0xff));
        if (response.length < 28 || response[11] != 1)
            throw new FelicaProtocolException();

        return Arrays.copyOfRange(response, 12, 28);
    }

    /**
     * Returns the manufacture ID (IDm/NFCID) of the card.
     *
     * @return IDm
     */
    public byte[] getIdm() {
        return mIdm;
    }

    // tech wrapper methods

    /**
     * Like {@link NfcF#transceive(byte[])} but prepends the length byte to the
     * request and removes the length byte from the response.
     * @throws IOException
     */
    byte[] transceive(byte[] rawRequest) throws IOException {
        byte request[] = new byte[rawRequest.length + 1];
        request[0] = (byte)(rawRequest.length + 1);
        System.arraycopy(rawRequest, 0, request, 1, rawRequest.length);
        byte[] rawResponse = mTech.transceive(request);
        if (rawResponse[0] != (byte)rawResponse.length)
            throw new FelicaProtocolException("response length mismatch");
        return Arrays.copyOfRange(rawResponse, 1, rawResponse.length);
    }

    public short getSystemCode() {
        byte[] rawSystemCode = mTech.getSystemCode();
        assert rawSystemCode.length == 2;
        return (short)((rawSystemCode[0] << 8) | rawSystemCode[1]);
    }

    public void connect() throws IOException {
        mTech.connect();
    }

    public void close() throws IOException {
        mTech.close();
    }

    public boolean isConnected() {
        return mTech.isConnected();
    }
}
