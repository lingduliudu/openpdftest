package com.test;

import lombok.Data;

import java.security.KeyStore;
import java.security.Provider;


/**
 * @author Andy
 */
@Data
public class SignatureCryptoInfo {

    private Provider provider;
    private KeyStore keystore;
    private char[] password;
    private String certAlias;

}