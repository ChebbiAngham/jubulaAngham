/*******************************************************************************
 * Copyright (c) 2014 BREDEX GmbH.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BREDEX GmbH - initial API and implementation and/or initial documentation
 *******************************************************************************/
package org.eclipse.jubula.client.internal.impl;

import java.util.Map;

import org.eclipse.jubula.client.AUT;
import org.eclipse.jubula.client.exceptions.ActionException;
import org.eclipse.jubula.client.exceptions.CheckFailedException;
import org.eclipse.jubula.client.exceptions.ComponentNotFoundException;
import org.eclipse.jubula.client.exceptions.ConfigurationException;
import org.eclipse.jubula.client.exceptions.ExecutionException;
import org.eclipse.jubula.client.internal.AUTConnection;
import org.eclipse.jubula.client.internal.Synchronizer;
import org.eclipse.jubula.communication.CAP;
import org.eclipse.jubula.communication.internal.message.CAPTestMessage;
import org.eclipse.jubula.communication.internal.message.CAPTestMessageFactory;
import org.eclipse.jubula.communication.internal.message.CAPTestResponseMessage;
import org.eclipse.jubula.communication.internal.message.MessageCap;
import org.eclipse.jubula.communication.internal.message.UnknownMessageException;
import org.eclipse.jubula.tools.AUTIdentifier;
import org.eclipse.jubula.tools.internal.exception.Assert;
import org.eclipse.jubula.tools.internal.exception.CommunicationException;
import org.eclipse.jubula.tools.internal.i18n.I18n;
import org.eclipse.jubula.tools.internal.objects.event.TestErrorEvent;
import org.eclipse.jubula.tools.internal.registration.AutIdentifier;
import org.eclipse.jubula.tools.internal.xml.businessmodell.ComponentClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** @author BREDEX GmbH */
public class AUTImpl implements AUT {
    /** the logger */
    private static Logger log = LoggerFactory.getLogger(AUTImpl.class);

    /** the AUT identifier */
    private AutIdentifier m_autID;
    /** the instance */
    private AUTConnection m_instance;
    /** the typeMapping */
    private Map<ComponentClass, String> m_typeMapping;

    /**
     * Constructor
     * 
     * @param autID
     *            the identifier to use for connection
     */
    public AUTImpl(AutIdentifier autID) {
        m_autID = autID;
    }

    /** {@inheritDoc} */
    public void connect() throws Exception {
        final Map<ComponentClass, String> typeMapping = getTypeMapping();
        Assert.verify(typeMapping != null);
        m_instance = AUTConnection.getInstance();
        m_instance.connectToAut(m_autID, typeMapping);
    }

    /** {@inheritDoc} */
    public void disconnect() throws Exception {
        m_instance.close();
    }

    /** {@inheritDoc} */
    public boolean isConnected() {
        return m_instance != null ? m_instance.isConnected() : false;
    }

    /** {@inheritDoc} */
    public AUTIdentifier getIdentifier() {
        return m_autID;
    }

    /**
     * @return the typeMapping
     */
    public Map<ComponentClass, String> getTypeMapping() {
        return m_typeMapping;
    }

    /**
     * @param typeMapping
     *            the typeMapping to set
     */
    public void setTypeMapping(Map<?, ?> typeMapping) {
        m_typeMapping = (Map<ComponentClass, String>) typeMapping;
    }

    /** {@inheritDoc} */
    public void execute(CAP cap) throws ExecutionException {
        try {
            // TODO MT: fixme
            CAPTestMessage capTestMessage = CAPTestMessageFactory
                .getCAPTestMessage((MessageCap)cap,
                    "com.bredexsw.guidancer.SwtToolkitPlugin"); //$NON-NLS-1$

            m_instance.send(capTestMessage);

            Object exchange = Synchronizer.instance().exchange(null);
            if (exchange instanceof CAPTestResponseMessage) {
                CAPTestResponseMessage response = 
                    (CAPTestResponseMessage) exchange;
                processResponse(response);
            } else {
                log.error("Unexpected response received: " //$NON-NLS-1$
                    + String.valueOf(exchange));
            }

        } catch (UnknownMessageException e) {
            log.error(e.getLocalizedMessage(), e);
        } catch (CommunicationException e) {
            log.error(e.getLocalizedMessage(), e);
        } catch (InterruptedException e) {
            log.error(e.getLocalizedMessage(), e);
        }
    }

    /**
     * @param response
     *            the response to process
     */
    private void processResponse(CAPTestResponseMessage response)
        throws ExecutionException {
        if (response.hasTestErrorEvent()) {
            final TestErrorEvent event = response.getTestErrorEvent();
            final String eventId = event.getId();
            Map<Object, Object> eventProps = event.getProps();
            String description = null;
            if (eventProps.containsKey(
                TestErrorEvent.Property.DESCRIPTION_KEY)) {
                String key = (String) eventProps
                    .get(TestErrorEvent.Property.DESCRIPTION_KEY);
                Object[] args = (Object[]) eventProps
                    .get(TestErrorEvent.Property.PARAMETER_KEY);
                args = args != null ? args : new Object[0];
                description = I18n.getString(key, args);
            }
            if (TestErrorEvent.ID.ACTION_ERROR.equals(eventId)) {
                throw new ActionException(description);
            } else if (TestErrorEvent.ID.COMPONENT_NOT_FOUND.equals(eventId)) {
                throw new ComponentNotFoundException(description);
            } else if (TestErrorEvent.ID.CONFIGURATION_ERROR.equals(eventId)) {
                throw new ConfigurationException(description);
            } else if (TestErrorEvent.ID.VERIFY_FAILED.equals(eventId)) {
                Object actualValue = event.getProps().get(
                    TestErrorEvent.Property.ACTUAL_VALUE_KEY);
                throw new CheckFailedException(description,
                    String.valueOf(actualValue));
            }
        }
    }
}