/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.amdatu.remote;

import static org.amdatu.remote.ServiceUtil.getStringPlusValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.EndpointEvent;
import org.osgi.service.remoteserviceadmin.EndpointEventListener;
import org.osgi.service.remoteserviceadmin.EndpointListener;

/**
 * Base class for service components that wish to inform listeners about {@link EndpointDescription} events based on
 * their declared scope.<p>
 * 
 * This implementation tracks both {@code EndpointListener} and {@link EndpointEventListener} registrations, but will
 * prefer the latter when a registration provides both.<p>
 * 
 * All internal state modifications and listeners calls are executed in-order through {@link #executeTask(Runnable)}.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
@SuppressWarnings("deprecation")
public abstract class AbstractEndpointPublishingComponent extends AbstractComponent {

    private final Map<ServiceReference<?>, AbstractListenerHandler<?>> m_listeners =
        new HashMap<ServiceReference<?>, AbstractListenerHandler<?>>();

    private final Set<EndpointDescription> m_endpoints = new HashSet<EndpointDescription>();

    public AbstractEndpointPublishingComponent(String type, String name) {
        super(type, name);
    }

    /**
     * Component callback for Endpoint Event Listener addition.
     * 
     * @param reference The Service Reference of the added Endpoint Event Listener
     * @param listener The Endpoint Event Listener
     */
    final void eventListenerAdded(final ServiceReference<EndpointEventListener> reference,
        final EndpointEventListener listener) {

        if (listener == this) {
            logDebug("Ignoring Event Endpoint Listener because it is a reference this service instance: %s", reference);
            return;
        }

        executeTask(new Runnable() {

            @Override
            public void run() {
                logDebug("Adding Endpoint Event Listener %s", reference);
                try {
                    EndpointEventListenerHandler handler =
                        new EndpointEventListenerHandler(reference, listener, m_endpoints);
                    AbstractListenerHandler<?> previous = m_listeners.put(reference, handler);
                    if (previous != null) {
                        logWarning("Endpoint Event Listener overwrites previous mapping %s", reference);
                    }
                }
                catch (Exception e) {
                    logError("Failed to handle added Endpoint Event Listener %s", e, reference);
                }
            }
        });
    }

    /**
     * Component callback for Endpoint Event Listener modification.
     * 
     * @param reference The Service Reference of the added Endpoint Event Listener
     * @param listener The Endpoint Event Listener
     */
    final void eventListenerModified(final ServiceReference<EndpointEventListener> reference,
        final EndpointEventListener listener) {

        if (listener == this) {
            logDebug("Ignoring Event Endpoint Listener because it is a reference this service instance: %s", reference);
            return;
        }

        executeTask(new Runnable() {

            @Override
            public void run() {
                logDebug("Modifying Endpoint Event Listener %s", listener);
                try {
                    AbstractListenerHandler<?> handler = m_listeners.get(reference);
                    if (handler != null) {
                        handler.referenceModified(m_endpoints);
                    }
                    else {
                        logWarning("Failed to locate modified Endpoint Event Listener %s", reference);
                    }
                }
                catch (Exception e) {
                    logError("Failed to handle modified Endpoint Event Listener %s", e, reference);
                }
            }
        });
    }

    /**
     * Component callback for Endpoint Event Listener removal.
     * 
     * @param reference The Service Reference of the added Endpoint Event Listener
     * @param endpointListener The Endpoint Event Listener
     */
    final void eventListenerRemoved(final ServiceReference<EndpointEventListener> reference,
        final EndpointEventListener listener) {

        if (listener == this) {
            logDebug("Ignoring Event Endpoint Listener because it is a reference this service instance: %s", reference);
            return;
        }

        executeTask(new Runnable() {

            @Override
            public void run() {
                logDebug("Removing Endpoint Event Listener %s", reference);
                AbstractListenerHandler<?> removed = m_listeners.remove(reference);
                if (removed != null) {
                    removed.clearMatches();
                }
                else {
                    logWarning("Failed to locate removed Endpoint Event Listener %s", reference);
                }
            }
        });
    }

    /**
     * Component callback for Endpoint Listener addition.
     * 
     * @param reference The Service Reference of the added Endpoint Listener
     * @param listener The Endpoint Listener
     */
    final void listenerAdded(final ServiceReference<EndpointListener> reference, final EndpointListener listener) {

        if (listener == this) {
            logDebug("Ignoring Endpoint Listener because it is a reference this service instance: %s", reference);
            return;
        }

        if (isEndpointEventListener(reference)) {
            logDebug("Ignoring Endpoint Listener because it is registered as an Endpoint Event Listener as well: %s",
                reference);
            return;
        }

        executeTask(new Runnable() {

            @Override
            public void run() {
                logDebug("Adding Endpoint Listener %s", reference);
                try {
                    EndpointListenerHandler holder = new EndpointListenerHandler(reference, listener, m_endpoints);
                    AbstractListenerHandler<?> previous = m_listeners.put(reference, holder);
                    if (previous != null) {
                        logWarning("Endpoint Listener overwrites previous mapping %s", reference);
                    }
                }
                catch (Exception e) {
                    logError("Failed to handle added Endpoint Listener %s", e, reference);
                }
            }
        });
    }

    /**
     * Component callback for endpointListener modification.
     * 
     * @param reference The serviceReference of the added endpointListener
     * @param listener The endpointListener
     */
    final void listenerModified(final ServiceReference<EndpointListener> reference, final EndpointListener listener) {

        if (listener == this) {
            logDebug("Ignoring Endpoint Listener because it is a reference this service instance: %s", reference);
            return;
        }

        if (isEndpointEventListener(reference)) {
            logDebug("Ignoring Endpoint Listener because it is registered as an Endpoint Event Listener as well: %s",
                reference);
            return;
        }

        executeTask(new Runnable() {

            @Override
            public void run() {
                logDebug("Modifying Endpoint Listener %s", listener);
                try {
                    AbstractListenerHandler<?> handler = m_listeners.get(reference);
                    if (handler != null) {
                        handler.referenceModified(m_endpoints);
                    }
                    else {
                        logWarning("Failed to locate modified Endpoint Listener %s", reference);
                    }
                }
                catch (Exception e) {
                    logError("Failed to handle modified Endpoint Listener %s", e, reference);
                }
            }
        });
    }

    /**
     * Component callback for Endpoint Listener removal.
     * 
     * @param serviceReference The Service Reference of the added Endpoint Listener
     * @param listener The Endpoint Listener
     */
    final void listenerRemoved(final ServiceReference<EndpointListener> reference, final EndpointListener listener) {

        if (listener == this) {
            logDebug("Ignoring Endpoint Listener because it is a reference this service instance: %s", reference);
            return;
        }

        if (isEndpointEventListener(reference)) {
            logDebug("Ignoring Endpoint Listener because it is registered as an Endpoint Event Listener as well: %s",
                reference);
            return;
        }

        executeTask(new Runnable() {

            @Override
            public void run() {
                logDebug("Removing Endpoint Listener %s", reference);
                AbstractListenerHandler<?> removed = m_listeners.remove(reference);
                if (removed != null) {
                    removed.clearMatches();
                }
                else {
                    logWarning("Failed to locate removed Endpoint Listener %s", reference);
                }
            }
        });
    }

    /**
     * Call Endpoint added on all registered listeners with as scope that matches the specified endpointDescription.
     * 
     * @param description The Endpoint Description
     * @throws IllegalStateException if called with a previsouly added Endpoint Description
     */
    protected final void endpointAdded(final EndpointDescription description) {

        executeTask(new Runnable() {

            @Override
            public void run() {
                logDebug("Adding Endpoint: %s", description);
                if (!m_endpoints.add(description)) {
                    throw new IllegalStateException("Trying to add duplicate Endpoint Description: " + description);
                }
                for (AbstractListenerHandler<?> handler : m_listeners.values()) {
                    try {
                        handler.endpointAdded(description);
                    }
                    catch (Exception e) {
                        logWarning("Caught exception while invoking Endpoint added on %s", e, handler.getReference());
                    }
                }
            }
        });
    }

    /**
     * Call Endpoint removed on all registered listeners with a scope that matches the specified endpointDescription.
     * 
     * @param endpoint The Endpoint Description
     * @throws IllegalStateException if called with an unknown Endpoint Description
     */
    protected final void endpointRemoved(final EndpointDescription endpoint) {

        executeTask(new Runnable() {

            @Override
            public void run() {

                logDebug("Removing Endpoint: %s", endpoint);
                if (!m_endpoints.remove(endpoint)) {
                    throw new IllegalStateException("Trying to remove unknown Endpoint Description: " + endpoint);
                }
                for (AbstractListenerHandler<?> handler : m_listeners.values()) {
                    try {
                        handler.endpointRemoved(endpoint);
                    }
                    catch (Exception e) {
                        logWarning("Caught exception while invoking Endpoint removed on %s", e, handler.getReference());
                    }
                }
            }
        });
    }

    /**
     * Call Endpoint modified on all registered listeners with as scope that matches the specified endpointDescription.
     * 
     * @param description The Endpoint Description
     * @throws IllegalStateException if called with an unknown Endpoint Description
     */
    protected final void endpointModified(final EndpointDescription endpoint) {

        executeTask(new Runnable() {

            @Override
            public void run() {

                logDebug("Modifying Endpoint: %s", endpoint);
                if (!m_endpoints.remove(endpoint)) {
                    throw new IllegalStateException("Trying to modify unknown Endpoint Description: " + endpoint);
                }
                m_endpoints.add(endpoint);
                for (AbstractListenerHandler<?> handler : m_listeners.values()) {
                    try {
                        handler.endpointModified(endpoint);
                    }
                    catch (Exception e) {
                        logWarning("Caught exception while invoking Endpoint removed on %s", e, handler.getReference());
                    }
                }
            }
        });
    }

    /**
     * Determine whether a service reference exposes a services that implements EndpointEventListener.
     * 
     * @param reference the Service Reference
     * @return {@code true} is the objectclass includes EndpointEventListener, otherwise {@code false}
     */
    private static boolean isEndpointEventListener(ServiceReference<?> reference) {
        String[] objectClass = getStringPlusValue(reference.getProperty(Constants.OBJECTCLASS));
        for (int i = 0; i < objectClass.length; i++) {
            if (objectClass[i].equals(EndpointEventListener.class.getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Abstract handler for listeners that encapsulates filter parsing, caching and matching
     * <p>
     * This implementation is not thread-safe. Synchronization is handled from the outside.
     * 
     * @param <T> The concrete listener type
     */
    private static abstract class AbstractListenerHandler<T> {

        private final ServiceReference<T> m_reference;
        private final String m_scopeKey;
        private final T m_listener;
        private final List<Filter> m_filters = new ArrayList<Filter>();
        private final Map<EndpointDescription, Filter> m_matches = new HashMap<EndpointDescription, Filter>();

        /**
         * Constructs a new handler and initializes by calling {@link #referenceModified(Collection)} internally.
         * 
         * @param reference The listener Service Reference
         * @param listener The listener of type T
         * @param scopeKey The scope property key
         * @param endpoints The current Endpoint collection
         * @throws Exception If the initialization fails
         */
        public AbstractListenerHandler(ServiceReference<T> reference, T listener, String scopeKey,
            Collection<EndpointDescription> endpoints) throws Exception {

            m_reference = reference;
            m_listener = listener;
            m_scopeKey = scopeKey;
            referenceModified(endpoints);
        }

        /**
         * Updates the handler state and invokes the relevant listener callbacks according to the specified
         * collection of published Endpoint Descriptions.
         * 
         * @param endpoints The current collection of Endpoint Descriptions
         * @throws Exception If the update fails
         */
        public final void referenceModified(Collection<EndpointDescription> endpoints) throws Exception {
            updateFilters();
            updateMatches(endpoints);
        }

        /**
         * Returns the listener.
         * 
         * @return The listener
         */
        public final T getListener() {
            return m_listener;
        }

        /**
         * Return the reference.
         * 
         * @return The reference
         */
        public final ServiceReference<T> getReference() {
            return m_reference;
        }

        /**
         * Return the first matching filter for the specified Endpoint Description.
         * 
         * @param endpoint The Endpoint Description
         * @return The first matching Filter, or {@code null}
         */
        private final Filter getFirstMatchingFilter(EndpointDescription endpoint) {
            for (Filter filter : m_filters) {
                if (filter.matches(endpoint.getProperties())) {
                    return filter;
                }
            }
            return null;
        }

        /**
         * Invoke the added callback on the listener if the scope matches the specified Endpoint Description.
         * 
         * @param endpoint The Endpoint Description
         */
        public final void endpointAdded(EndpointDescription endpoint) {

            Filter filter = getFirstMatchingFilter(endpoint);
            if (filter != null) {
                m_matches.put(endpoint, filter);
                endpointAdded(endpoint, filter);
            }
        }

        /**
         * Invoke the removed callback on the listener if the scope matches the specified Endpoint Description.
         * 
         * @param endpoint The Endpoint Description
         */
        public final void endpointRemoved(EndpointDescription endpoint) {

            Filter filter = m_matches.remove(endpoint);
            if (filter != null) {
                endpointRemoved(endpoint, filter);
            }
        }

        /**
         * Invoke the relevant callback on the listener if the scope matches the specified Endpoint Description.
         * 
         * @param endpoint The Endpoint Description
         */
        public final void endpointModified(EndpointDescription endpoint) {

            Filter matchedFilter = getFirstMatchingFilter(endpoint);
            Filter previouslyMatchedFilter = m_matches.remove(endpoint);

            if (matchedFilter != null) {
                m_matches.put(endpoint, matchedFilter);
                if (previouslyMatchedFilter != null) {
                    endpointModified(endpoint, matchedFilter);
                }
                else {
                    endpointAdded(endpoint, matchedFilter);
                }
            }
            else if (previouslyMatchedFilter != null) {
                endpointEndmatch(endpoint, matchedFilter);
            }
        }

        /**
         * Invoke the relevant callback on the listener.
         * 
         * @param endpoint The Endpoint Description
         * @param matchedFilter The matched Filter
         */
        protected abstract void endpointAdded(EndpointDescription endpoint, Filter matchedFilter);

        /**
         * Invoke the relevant callback on the listener.
         * 
         * @param endpoint The Endpoint Description
         * @param matchedFilter The matched Filter
         */
        protected abstract void endpointRemoved(EndpointDescription endpoint, Filter matchedFilter);

        /**
         * Invoke the relevant callback on the listener.
         * 
         * @param endpoint The Endpoint Description
         * @param matchedFilter The matched Filter
         */
        protected abstract void endpointModified(EndpointDescription endpoint, Filter matchedFilter);

        /**
         * Invoke the relevant callback on the listener.
         * 
         * @param endpoint The Endpoint Description
         * @param matchedFilter The matched Filter
         */
        protected abstract void endpointEndmatch(EndpointDescription endpoint, Filter matchedFilter);

        private final void updateFilters() throws InvalidSyntaxException {

            m_filters.clear();
            Object value = m_reference.getProperty(m_scopeKey);
            if (value == null) {
                return;
            }
            String[] scopes = getStringPlusValue(value);
            if (scopes == null || scopes.length == 0) {
                return;
            }

            List<Filter> filters = new ArrayList<Filter>();
            for (String scope : scopes) {
                if (!scope.trim().equals("")) {
                    filters.add(FrameworkUtil.createFilter(scope));
                }
            }
            m_filters.addAll(filters);
        }

        private final void updateMatches(Collection<EndpointDescription> endpoints) {
            for (EndpointDescription endpoint : endpoints) {
                Filter matchedFilter = getFirstMatchingFilter(endpoint);
                if (matchedFilter != null) {
                    if (!m_matches.containsKey(endpoint)) {
                        m_matches.put(endpoint, matchedFilter);
                        endpointAdded(endpoint, matchedFilter);
                    }
                    else if (!matchedFilter.equals(m_matches.get(endpoint))) {
                        m_matches.put(endpoint, matchedFilter);
                        endpointModified(endpoint, matchedFilter);
                    }
                    else {
                        // nothing changed
                    }
                }
                else {
                    Filter previouslyMatchedFilter = m_matches.remove(endpoint);
                    if (previouslyMatchedFilter != null) {
                        endpointEndmatch(endpoint, previouslyMatchedFilter);
                    }
                }
            }
        }

        private final void clearMatches() {
            for (Entry<EndpointDescription, Filter> entry : m_matches.entrySet()) {
                endpointRemoved(entry.getKey(), entry.getValue());
            }
            m_matches.clear();
        }
    }

    /**
     * Concrete holder for type Endpoint Event Listener.
     */
    private static class EndpointEventListenerHandler extends AbstractListenerHandler<EndpointEventListener> {

        public EndpointEventListenerHandler(ServiceReference<EndpointEventListener> reference,
            EndpointEventListener listener, Collection<EndpointDescription> endpoints) throws Exception {
            super(reference, listener, EndpointEventListener.ENDPOINT_LISTENER_SCOPE, endpoints);
        }

        @Override
        protected void endpointAdded(EndpointDescription description, Filter matchedFilter) {
            try {
                getListener().endpointChanged(new EndpointEvent(EndpointEvent.ADDED, description),
                    matchedFilter.toString());
            }
            catch (Exception e) {}
        }

        @Override
        protected void endpointRemoved(EndpointDescription description, Filter matchedFilter) {
            try {
                getListener().endpointChanged(new EndpointEvent(EndpointEvent.REMOVED, description),
                    matchedFilter.toString());
            }
            catch (Exception e) {}
        }

        @Override
        protected void endpointModified(EndpointDescription description, Filter matchedFilter) {
            try {
                getListener().endpointChanged(new EndpointEvent(EndpointEvent.MODIFIED, description),
                    matchedFilter.toString());
            }
            catch (Exception e) {}
        }

        @Override
        protected void endpointEndmatch(EndpointDescription description, Filter matchedFilter) {
            try {
                getListener().endpointChanged(new EndpointEvent(EndpointEvent.MODIFIED_ENDMATCH, description),
                    matchedFilter.toString());
            }
            catch (Exception e) {}
        }
    }

    /**
     * Concrete holder for deprecated type Endpoint Listener.
     */
    private static class EndpointListenerHandler extends AbstractListenerHandler<EndpointListener> {

        public EndpointListenerHandler(ServiceReference<EndpointListener> reference,
            EndpointListener listener, Collection<EndpointDescription> endpoints) throws Exception {
            super(reference, listener, EndpointListener.ENDPOINT_LISTENER_SCOPE, endpoints);
        }

        @Override
        protected void endpointAdded(EndpointDescription description, Filter matchedFilter) {
            try {
                getListener().endpointAdded(description, matchedFilter.toString());
            }
            catch (Exception e) {}
        }

        @Override
        protected void endpointRemoved(EndpointDescription description, Filter matchedFilter) {
            try {
                getListener().endpointRemoved(description, matchedFilter.toString());
            }
            catch (Exception e) {}
        }

        @Override
        protected void endpointModified(EndpointDescription description, Filter matchedFilter) {
            try {
                getListener().endpointRemoved(description, matchedFilter.toString());
                getListener().endpointAdded(description, matchedFilter.toString());
            }
            catch (Exception e) {}
        }

        @Override
        protected void endpointEndmatch(EndpointDescription description, Filter matchedFilter) {
            try {
                getListener().endpointRemoved(description, matchedFilter.toString());
            }
            catch (Exception e) {}
        }
    }
}
