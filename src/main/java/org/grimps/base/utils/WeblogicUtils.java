/*
 * Copyright 2017 Shekhar Jha
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.grimps.base.utils;

import org.grimps.base.InternalErrorException;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class WeblogicUtils {

    public static final String JPS_MAP_NAME = "name";
    public static final String JPS_MAP_PASSWORD = "password";
    public static final String JPS_MAP_CREDENTIAL = "credential";

    private static final XLogger logger = XLoggerFactory.getXLogger(WeblogicUtils.class);

    /**
     * Returns the credential details for given credential from specified credential Map.
     *
     * @param credentialMapName JPS Credential Map from which credential must be read.
     * @param credentialName    JPS credential that should be read.
     * @return Map containing {@link WeblogicUtils#JPS_MAP_NAME} and {@link WeblogicUtils#JPS_MAP_PASSWORD} if credential
     * contains user id/password (PasswordCredential). In case of GenericCredential, map contains
     * {@link WeblogicUtils#JPS_MAP_CREDENTIAL}.
     * @throws InternalErrorException in case of any error or missing credential details.
     */
    public static Map<String, Object> readKeyFromWebLogicCredentialMap(String credentialMapName, String credentialName) {
        logger.entry(credentialMapName, credentialName);
        Map<String, Object> returnResult = new HashMap<>();
        Class<?> jpsContextFactoryClass;
        try {
            jpsContextFactoryClass = Utils.loadClass("oracle.security.jps.JpsContextFactory");
            logger.trace("JPS Context Factory Class {}", jpsContextFactoryClass);
        } catch (Exception exception) {
            throw logger.throwing(new InternalErrorException("Failed to locate client library needed to read Oracle JPS Credential store. Please ensure that system is deployed in a Weblogic domain (preferably containing Oracle Identity Manager).", exception));
        }
        try {
            Object jpsContextFactory = jpsContextFactoryClass.getMethod("getContextFactory", (Class) null).invoke(null);
            logger.trace("JPSContextFactory {}", jpsContextFactory);
            if (jpsContextFactory != null) {
                Object jpsContext = jpsContextFactoryClass.getMethod("getContext", (Class) null).invoke(jpsContextFactory);
                logger.trace("JPS Context {}", jpsContext);
                Object credentialStore = Utils.loadClass("oracle.security.jps.JpsContext").getMethod("getServiceInstance", Class.class).invoke(jpsContext, Utils.loadClass("oracle.security.jps.service.credstore.CredentialStore"));
                logger.trace("Credential Store {}", credentialStore);
                Object credentialMap = Utils.loadClass("oracle.security.jps.service.credstore.CredentialStore").getMethod("getCredentialMap", String.class).invoke(credentialStore, credentialMapName);
                logger.trace("Credential Map {}", credentialMap);
                if (credentialMap != null) {
                    Object credentialObject = Utils.loadClass("oracle.security.jps.service.credstore.CredentialMap").getMethod("getCredential", String.class).invoke(credentialMap, credentialName);
                    logger.trace("Credential {}", credentialObject);
                    if (credentialObject != null) {
                        if (Utils.loadClass("oracle.security.jps.service.credstore.PasswordCredential").isInstance(credentialObject)) {
                            logger.trace("Password Credential identified");
                            Object nameObject = Utils.loadClass("oracle.security.jps.service.credstore.PasswordCredential").getMethod("getName").invoke(credentialObject);
                            logger.trace("Name : {}", nameObject);
                            Object password = Utils.loadClass("oracle.security.jps.service.credstore.PasswordCredential").getMethod("getPassword").invoke(credentialObject);
                            logger.trace("Password: {}", password);
                            returnResult.put(JPS_MAP_NAME, nameObject);
                            returnResult.put(JPS_MAP_PASSWORD, password);
                        } else if (Utils.loadClass("oracle.security.jps.service.credstore.GenericCredential").isInstance(credentialObject)) {
                            logger.trace("Generic Credential identified.");
                            Object nameObject = Utils.loadClass("oracle.security.jps.service.credstore.GenericCredential").getMethod("getCredential").invoke(credentialObject);
                            logger.trace("Name : {}", nameObject);
                            returnResult.put(JPS_MAP_CREDENTIAL, nameObject);
                        } else {
                            throw logger.throwing(new InternalError("The credential object retrieved using key " + credentialName + " from map " + credentialMapName + " is not Generic or Password Credential."));
                        }
                    } else {
                        throw logger.throwing(new InternalError("Failed to locate credential " + credentialName + " in credential map " + credentialMapName));
                    }
                } else {
                    throw logger.throwing(new InternalError("Failed to locate credential map " + credentialMapName));
                }
            } else {
                throw logger.throwing(new InternalError("Failed to locate JPS context factory using JpsContextFactory.getContextFactory()"));
            }
        } catch (Exception exception) {
            logger.catching(exception);
            throw logger.throwing(new InternalError("Failed to read credential from " + credentialMapName + " with key " + credentialName));
        }
        return logger.exit(returnResult);
    }

}
