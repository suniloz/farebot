/*
 * FelicaCard.java
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
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import android.nfc.Tag;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.codebutler.farebot.Utils;
import com.codebutler.farebot.mifare.MifareCard;

public class FelicaCard extends MifareCard {
    private static final String TAG = "FelicaCard";

    private final short mSystemCode;

    private final Map<FelicaBlockId, byte[]> mBlocks;

    private final Map<Short, Integer> mKeyVersions;

    /**
     * Precondition: the tag supports the NfcF tech
     * @throws IOException
     */
    public static FelicaCard dumpTag(Tag tag) throws IOException {
        FelicaProtocol felica = new FelicaProtocol(tag);
        felica.connect();
        short systemCode = felica.getSystemCode();

        felica.requestResponse();  // check that the card is actually there

        Map<FelicaBlockId, byte[]> blocks = new TreeMap<FelicaBlockId, byte[]>();
        Map<Short, Integer> keyVersions = new TreeMap<Short, Integer>();
        DumperInterface dumperInterface = createDumperInterface(felica, blocks, keyVersions);
        dumperInterface.dumpKeyVersion((short)0xFFFF);  // dump system key version

        if (OctopusDumper.check(systemCode))
            OctopusDumper.dump(dumperInterface);

        felica.close();

        return new FelicaCard(felica.getIdm(), new Date(),
                              systemCode, blocks, keyVersions);
    }

    private static DumperInterface createDumperInterface(
            final FelicaProtocol felica,
            final Map<FelicaBlockId, byte[]> blocks,
            final Map<Short, Integer> keyVersions) {
        return new DumperInterface() {

            @Override
            public int dumpKeyVersion(short serviceId) throws IOException {
                felica.requestService(Arrays.asList(new Short[] {serviceId}), keyVersions);
                return keyVersions.get(serviceId);
            }

            @Override
            public byte[] dumpBlock(FelicaBlockId blockId) throws IOException {
                byte[] data = felica.readWithoutEncryption(blockId);
                blocks.put(blockId, data);
                return data;
            }
        };
    }

    public static final Parcelable.Creator<FelicaCard> CREATOR = new Creator<FelicaCard>() {

        @Override
        public FelicaCard[] newArray(int size) {
            return new FelicaCard[size];
        }

        @Override
        public FelicaCard createFromParcel(Parcel source) {
            int tagIdLength = source.readInt();
            byte[] tagId = new byte[tagIdLength];
            source.readByteArray(tagId);
            Date scannedAt = new Date(source.readLong());
            int systemCode = source.readInt();

            Map<FelicaBlockId, byte[]> blocks = new TreeMap<FelicaBlockId, byte[]>();
            Bundle blockBundle = source.readBundle();
            for (String key : blockBundle.keySet()) {
                byte[] data = blockBundle.getByteArray(key);
                if (data == null)
                    throw new IllegalArgumentException();
                blocks.put(FelicaBlockId.valueOf(key), data);
            }

            Map<Short, Integer> keyVersions = new TreeMap<Short, Integer>();
            Bundle keyVerBundle = source.readBundle();
            Log.e(TAG, "keyver has "+keyVerBundle.size());
            for (String key : keyVerBundle.keySet())
                keyVersions.put(Integer.valueOf(key).shortValue(), keyVerBundle.getInt(key));

            return new FelicaCard(tagId, scannedAt, (short)systemCode, blocks, keyVersions);
        }
    };

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        super.writeToParcel(parcel, flags);
        parcel.writeInt(mSystemCode);

        Bundle blockBundle = new Bundle();
        for (Entry<FelicaBlockId, byte[]> entry : mBlocks.entrySet())
            blockBundle.putByteArray(entry.getKey().toString(), entry.getValue());
        parcel.writeBundle(blockBundle);

        Bundle keyVerBundle = new Bundle();
        for (Entry<Short, Integer> entry : mKeyVersions.entrySet())
            keyVerBundle.putInt(Integer.toString(entry.getKey() & 0xffff),
                                  entry.getValue());
        parcel.writeBundle(keyVerBundle);
    }

    public static MifareCard fromXML(byte[] id, Date scannedAt,
                                     Element rootElement) {
        Short systemCode = Integer.valueOf(rootElement.getElementsByTagName("system-code").item(0).getTextContent()).shortValue();

        Map<FelicaBlockId, byte[]> blocks = new TreeMap<FelicaBlockId, byte[]>();
        NodeList blockElems = rootElement.getElementsByTagName("block");
        for (int i = 0; i < blockElems.getLength(); i++) {
            Element blockElem = (Element)blockElems.item(i);
            blocks.put(
                    FelicaBlockId.valueOf(blockElem.getAttribute("code")),
                    Utils.hexStringToByteArray(blockElem.getTextContent()));
        }

        Map<Short, Integer> keyVersions = new TreeMap<Short, Integer>();
        NodeList serviceElems = rootElement.getElementsByTagName("service");
        for (int i = 0; i < serviceElems.getLength(); i++) {
            Element serviceElem = (Element)serviceElems.item(i);
            keyVersions.put(
                    Integer.valueOf(serviceElem.getAttribute("code")).shortValue(),
                    Integer.valueOf(serviceElem.getAttribute("key-version")));
        }

        return new FelicaCard(id, scannedAt, systemCode, blocks, keyVersions);
    }

    @Override
    public Element toXML() throws Exception {
        Element root = super.toXML();
        Document doc = root.getOwnerDocument();

        Element sysCodeElem = doc.createElement("system-code");
        sysCodeElem.setTextContent(Short.toString(mSystemCode));
        root.appendChild(sysCodeElem);

        Element dataElem = doc.createElement("data");
        for (Entry<FelicaBlockId, byte[]> entry : mBlocks.entrySet()) {
            Element blockElem = doc.createElement("block");
            blockElem.setAttribute("code", entry.getKey().toString());
            blockElem.setTextContent(Utils.getHexString(entry.getValue()));
            dataElem.appendChild(blockElem);
        }
        root.appendChild(dataElem);

        Element servicesElem = doc.createElement("services");
        for (Entry<Short, Integer> entry : mKeyVersions.entrySet()) {
            Element serviceElem = doc.createElement("service");
            serviceElem.setAttribute("code", Integer.toString(entry.getKey() & 0xffff));
            serviceElem.setAttribute("key-version", Integer.toString(entry.getValue()));
            servicesElem.appendChild(serviceElem);
        }
        root.appendChild(servicesElem);

        return root;
    }

    FelicaCard(byte[] tagId, Date scannedAt, short systemCode,
               Map<FelicaBlockId, byte[]> blocks,
               Map<Short, Integer> keyVersions) {
        super(tagId, scannedAt);
        mSystemCode = systemCode;
        mBlocks = blocks;
        mKeyVersions = keyVersions;
    }

    public short getSystemCode() {
        return mSystemCode;
    }

    public Map<FelicaBlockId, byte[]> getBlocks() {
        return mBlocks;
    }

    public Map<Short, Integer> getKeyVersions() {
        return mKeyVersions;
    }

    @Override
    public int describeContents() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public CardType getCardType() {
        return CardType.Felica;
    }

}
