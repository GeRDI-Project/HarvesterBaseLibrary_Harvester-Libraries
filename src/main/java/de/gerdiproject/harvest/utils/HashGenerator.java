/*
 *  Copyright © 2018 Robin Weiss (http://www.gerdi-project.de/)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package de.gerdiproject.harvest.utils;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * This class generates hash values of strings.<br><br>
 *
 * https://stackoverflow.com/questions/9655181/how-to-convert-a-byte-array-to-a-hex-string-in-java
 *
 * @author Robin Weiss
 */
public class HashGenerator
{
    private final static char[] HEX_ARRAY = "0123456789abcdef".toCharArray();

    private MessageDigest messageDigest;
    private Charset charset;


    /**
     * Constructs a hash generator using a specified charset.
     *
     * @param charset the charset used to decode input strings
     */
    public HashGenerator(Charset charset)
    {
        MessageDigest temp;

        try {
            temp = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException e) {
            temp = null;
        }

        this.charset = charset;
        this.messageDigest = temp;
    }


    /**
     * Resets the fields of the singleton instance to default values.
     */
    public void reset()
    {
        charset = null;
        messageDigest = null;
    }


    /**
     * Generates a hexadecimal string representing the hash value of a specified
     * input string.
     *
     * @param input the input of which the hash is generated
     *
     * @return a hexadecimal hash string
     */
    public String getShaHash(final String input)
    {
        final byte[] digest = messageDigest.digest(input.getBytes(charset));
        final char[] hexChars = new char[digest.length * 2];

        for (int i = 0; i < digest.length; i++) {
            final int b = 0xff & digest[i];
            hexChars[i * 2] = HEX_ARRAY[b >>> 4];
            hexChars[i * 2 + 1] = HEX_ARRAY[b & 0x0F];
        }

        return new String(hexChars);
    }
}
