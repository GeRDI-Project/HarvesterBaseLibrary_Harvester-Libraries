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

import de.gerdiproject.harvest.IDocument;
import de.gerdiproject.json.GsonUtils;

/**
 * This singleton class generates hash values of strings and objects.
 *
 * https://stackoverflow.com/questions/9655181/how{@literal -}to{@literal -}convert{@literal -}a{@literal -}byte{@literal -}array{@literal -}to{@literal -}a{@literal -}hex{@literal -}string{@literal -}in{@literal -}java
 * 
 * @author Robin Weiss
 */
public class HashGenerator
{
    private final static char[] HEX_ARRAY = "0123456789abcdef".toCharArray();

    private MessageDigest messageDigest;
    private Charset charset;

    private final static HashGenerator instance = new HashGenerator();


    /**
     * Initializes the singleton instance, generating the message digest and
     * setting the charset.
     *
     * @param charset the charset used to decode input strings
     */
    public static void init(Charset charset)
    {
        instance.charset = charset;
        MessageDigest temp;

        try {
            temp = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException e) {
            temp = null;
        }

        instance.messageDigest = temp;
    }


    /**
     * Returns the singleton instance.
     *
     * @return the singleton instance
     */
    public static HashGenerator instance()
    {
        return instance;
    }


    /**
     * Generates a hash value over the JSON string representation of a document.
     *
     * @param doc the document of which the hash is generated
     *
     * @return a hexadecimal hash string
     */
    public String getShaHash(final IDocument doc)
    {
        return getShaHash(GsonUtils.getGson().toJson(doc, doc.getClass()));
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
