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
package org.eclipse.jubula.qa.api.converter.target.rcp;

import java.util.Stack;

import org.eclipse.jubula.client.AUT;
import org.eclipse.jubula.client.ObjectMapping;
import org.eclipse.jubula.client.exceptions.CheckFailedException;
import org.eclipse.jubula.client.exceptions.ExecutionException;
import org.eclipse.jubula.client.exceptions.ExecutionExceptionHandler;
import org.eclipse.jubula.tools.ComponentIdentifier;

/**
 * @created 19.11.2014
 */
public class RuntimeContext {
    private static class CheckFailedExecutionHandler 
        implements ExecutionExceptionHandler {
        /** nesting level counter */
        private Stack<Boolean> m_stack = new Stack<Boolean>();

        /**
         * @param defaultHandling
         *            the default handling
         */
        public CheckFailedExecutionHandler(Boolean defaultHandling) {
            getStack().push(defaultHandling);
        }    
        
        /** special handling supports ignoring of check failed exceptions */
        public void handle(ExecutionException arg0) throws ExecutionException {
            if ((arg0 instanceof CheckFailedException) && getStack().peek()) {
                return;
            }
            throw arg0;
        }

        /**
         * @return the stack
         */
        public Stack<Boolean> getStack() {
            return m_stack;
        }
    }
    
    /** the AUT */
    private AUT m_aut;
    
    /** the object map to use */
    private ObjectMapping om;

    /** the event handler for this runtime context */
    private CheckFailedExecutionHandler m_eventHandler;

    /**
     * @param aut
     *            the AUT
     */
    public RuntimeContext(AUT aut, boolean defaultHandling) {
        setAUT(aut);
        m_eventHandler = new CheckFailedExecutionHandler(defaultHandling);
        aut.setHandler(m_eventHandler);
        // TODO: load OM for the AUT
    }

    /**
     * @return the AUT
     */
    public AUT getAUT() {
        return m_aut;
    }

    /**
     * @param aut the AUT to set
     */
    private void setAUT(AUT aut) {
        m_aut = aut;
    }

    /**
     * Gets a component identifier for a given logical component name 
     * from the object mapping for the AUT
     * @param name the logical component name
     * @return the component identifier
     */
    public ComponentIdentifier getIdentifier(String name) {
        return om.get(name);
    }
    
    /**
     * activate ignoring of
     * {@link org.eclipse.jubula.client.exceptions.CheckFailedException}
     */
    public void beginIgnoreCheckFailed() {
        getEventStack().push(true);
    }

    /**
     * @return the current event stack
     */
    private Stack<Boolean> getEventStack() {
        return m_eventHandler.getStack();
    }

    /**
     * end local event handling and restore previous state 
     */
    public void endLocalEventHandling() {
        getEventStack().pop();
    }
    
    /**
     * deactivate ignoring of
     * {@link org.eclipse.jubula.client.exceptions.CheckFailedException}
     */
    public void doNotIgnoreCheckFailed() {
        getEventStack().push(false);
    }
}