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

import org.osgi.framework.BundleContext;

/**
 * Generic base class for service component delegates that provides easy access to the
 * component's context and methods for logging.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public abstract class AbstractComponentDelegate {

    private final AbstractComponent m_component;

    public final void start() throws Exception {
        startComponentDelegate();
    }

    public final void stop() throws Exception {
        stopComponentDelegate();
    }

    protected void startComponentDelegate() throws Exception {
    }

    protected void stopComponentDelegate() throws Exception {
    }

    public AbstractComponentDelegate(AbstractComponent abstractComponent) {
        m_component = abstractComponent;
    }

    public final BundleContext getBundleContext() {
        return m_component.getBundleContext();
    }

    public final String getFrameworkUUID() {
        return m_component.getFrameworkUUID();
    }

    public final void logDebug(String message, Object... args) {
        m_component.logDebug(message, args);
    }

    public final void logDebug(String message, Throwable cause, Object... args) {
        m_component.logDebug(message, cause, args);
    }

    public final void logInfo(String message, Object... args) {
        m_component.logDebug(message, args);
    }

    public final void logInfo(String message, Throwable cause, Object... args) {
        m_component.logInfo(message, cause, args);
    }

    public final void logWarning(String message, Object... args) {
        m_component.logWarning(message, args);
    }

    public final void logWarning(String message, Throwable cause, Object... args) {
        m_component.logWarning(message, cause, args);
    }

    public final void logError(String message, Object... args) {
        m_component.logError(message, args);
    }

    public final void logError(String message, Throwable cause, Object... args) {
        m_component.logError(message, cause, args);
    }
}
