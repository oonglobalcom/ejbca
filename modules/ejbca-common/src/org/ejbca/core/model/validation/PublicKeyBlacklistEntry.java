/*************************************************************************
 *                                                                       *
 *  CESeCore: CE Security Core                                           *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General                  *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/

package org.ejbca.core.model.validation;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;

import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Hex;
import org.cesecore.certificates.util.AlgorithmConstants;
import org.cesecore.internal.InternalResources;
import org.cesecore.internal.UpgradeableDataHashMap;

/**
 * Domain class representing a public key blacklist entry.
 *  
 *
 * @version $Id$
 */
public class PublicKeyBlacklistEntry extends UpgradeableDataHashMap implements Serializable, Cloneable {

    private static final long serialVersionUID = -315759758359854900L;

    /** Class logger. */
    private static final Logger log = Logger.getLogger(PublicKeyBlacklistEntry.class);

    /** Public key fingerprint digest algorithm. Matching blacklists created by debian:
     * https://launchpad.net/ubuntu/+source/openssl-blacklist/
     * https://launchpad.net/ubuntu/+source/openssh-blacklist
     * https://launchpad.net/ubuntu/+source/openvpn-blacklist/
     * The blacklists contains the SHA1 hash with the first 20 bytes remove, i.e. the last 20 bytes
     */
    public static final String DIGEST_ALGORITHM = "SHA-256";


    //    /** List separator. */
    //    private static final String LIST_SEPARATOR = ";";

    protected static final InternalResources intres = InternalResources.getInstance();

    public static final float LATEST_VERSION = 1F;

    public static final String KEYSPEC = "keySpec";
    public static final String FINGERPRINT = "fingerprint";

    // Values used for lookup that are not stored in the data hash map.
    private int id;
    private String keyspec;
    /** Fingerprint of the key to compare, this is not just a normal fingerprint over the DER encoding
     * For RSA keys this is the hash (see {@link #DIGEST_ALGORITHM}) over the binary bytes of the public key modulus.
     * This because blacklist is typically due to weak random number generator (Debian weak keys) and we then want to capture all keys 
     * generated by this, so we don't want to include the chosen e, only the randomly generated n.
     */
    private String fingerprint;

    /** Public key reference (set while validate). */
    private transient PublicKey publicKey;
    
    /**
     * Creates a new instance.
     */
    public PublicKeyBlacklistEntry() {
        init();
    }

    /**
     * Initializes uninitialized data fields.
     */
    public void init() {
        if (null == data.get(VERSION)) {
            data.put(VERSION, new Float(LATEST_VERSION));
        }
    }

    /**
     * Gets the key public key blacklist id.
     * @return
     */
    public int getID() {
        return id;
    }

    /**
     * Sets the key public key blacklist id.
     * @param id
     */
    public void setID(int id) {
        this.id = id;
    }   

    /**
     * Gets the key spec, see for instance {@link AlgorithmConstants#KEYALGORITHM_RSA}.
     * @return the key spec string (i.e. 'RSA2048').
     */
    public String getKeyspec() {
        return keyspec;
    }

    /**
     * Sets the key spec, RSA2048 etc.
     * @param keyspec the key spec string.
     */
    public void setKeyspec(String keyspec) {
        this.keyspec = keyspec;
    }
    
    /**
     * Gets the fingerprint.
     * Fingerprint of the key to compare, this is not just a normal fingerprint over the DER encoding
     * For RSA keys this is the hash (see {@link #DIGEST_ALGORITHM}) over the binary bytes of the public key modulus.
     * This because blacklist is typically due to weak random number generator (Debian weak keys) and we then want to capture all keys 
     * generated by this, so we don't want to include the chosen e, only the randomly generated n.
     * @return fingerprint
     */
    public String getFingerprint() {
        return fingerprint;
    }

    /**
     * Sets the fingerprint
     * Fingerprint of the key to compare, this is not just a normal fingerprint over the DER encoding
     * For RSA keys this is the hash (see {@link #DIGEST_ALGORITHM}) over the binary bytes of the public key modulus.
     * This because blacklist is typically due to weak random number generator (Debian weak keys) and we then want to capture all keys 
     * generated by this, so we don't want to include the chosen e, only the randomly generated n.
     * @param fingerprint
     */
    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    /** 
     * Sets the fingerprint in the correct format from a public key object
     * see {@link #setFingerprint(String)}
     * @param publicKey an RSA public key
     */
    public void setFingerprint(PublicKey publicKey) {
        setFingerprint(createFingerprint(publicKey));
    }
    
    /** Creates the fingerprint in the correct format from a public key object
     * see {@link #setFingerprint(String)}
     * @param publicKey an RSA public key
     * @return public key fingerprint, as required by Blacklist, or null of no fingerprint can be created (due to unhandled key type for example)
     */
    public static String createFingerprint(PublicKey publicKey) {
        if (publicKey instanceof RSAPublicKey) {
            // Fingerprint of the key to compare, this is not just a normal fingerprint over the DER encoding
            // For RSA keys this is the hash (see {@link #DIGEST_ALGORITHM}) over the binary bytes of the public key modulus.
            // This because blacklist is typically due to weak random number generator (Debian weak keys) and we then want to capture all keys 
            // generated by this, so we don't want to include the chosen e, only the randomly generated n.
            RSAPublicKey rsapk = (RSAPublicKey)publicKey;
            byte[] modulusBytes = rsapk.getModulus().toByteArray();
            MessageDigest digest;
            try {
                digest = MessageDigest.getInstance(PublicKeyBlacklistEntry.DIGEST_ALGORITHM);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("Unable to create hash "+PublicKeyBlacklistEntry.DIGEST_ALGORITHM, e);
            }
            digest.reset();
            digest.update(modulusBytes);
            final String fingerprint = Hex.toHexString(digest.digest());
            return fingerprint;
        } else {
            log.debug("PublicKeyBlacklist can only handle RSA public keys at the moment");
        }
        return null;
    }
    
    /**
     * Gets the public key, have to be set transient with {@link #setPublicKey(PublicKey)}, not available after serialization
     * or storage.
     * @return the public key.
     */
    public PublicKey getPublicKey() {
        return publicKey;
    }

    /**
     * Sets the transient public key, see {@link #getPublicKey()}
     * @param publicKey the public key.
     */
    public void setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
    }

    @Override
    public float getLatestVersion() {
        return LATEST_VERSION;
    }

    @Override
    public void upgrade() {
        if (log.isTraceEnabled()) {
            log.trace(">upgrade: " + getLatestVersion() + ", " + getVersion());
        }
        if (Float.compare(LATEST_VERSION, getVersion()) != 0) {
            // New version of the class, upgrade.
            log.info(intres.getLocalizedMessage("publickeyblacklist.upgrade", new Float(getVersion())));
            init();
        }
    }
}
