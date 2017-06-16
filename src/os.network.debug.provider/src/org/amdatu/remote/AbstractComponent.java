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

import static org.amdatu.remote.Constants.CONSOLE_PROP_PRE;
import static org.amdatu.remote.Constants.LOGGING_PROP_PRE;

import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.log.LogService;

/**
 * Generic base class for service components. This class provides easy access to
 * the context and logging methods.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public abstract class AbstractComponent {

    private static final SimpleDateFormat DF = new SimpleDateFormat("HH:mm:ss.SSS");

    private final String m_type;
    private final String m_name;
    private final String m_identifier;

    private int m_logLevel = LogService.LOG_INFO;
    private int m_conLevel = LogService.LOG_ERROR - 1;

    private volatile BundleContext m_context;
    private volatile LogService m_log;

    private volatile ExecutorService m_executor;
    private volatile String m_frameworkUUID;

    /**
     * Create a new instance.
     * 
     * @param type The identifying name of logical component
     * @param name The identifying name of the implementation
     */
    public AbstractComponent(String type, String name) {
        m_type = type;
        m_name = name;
        m_identifier = type + "/" + name + "(" + (System.currentTimeMillis() % 1000) + ")";
    }

    /**
     * Final implementation of the DependecyManager lifecycle start callback.
     * 
     * @throws Exception
     * @see {@link #startComponent()}
     */
    protected final void start() throws Exception {

        m_frameworkUUID = ServiceUtil.getFrameworkUUID(m_context);
        m_executor = Executors.newSingleThreadExecutor();

        m_logLevel = getLevelProperty(LOGGING_PROP_PRE + ".level", LogService.LOG_INFO);
        m_logLevel = getLevelProperty(LOGGING_PROP_PRE + "." + m_type + ".level", m_logLevel);
        m_logLevel = getLevelProperty(LOGGING_PROP_PRE + "." + m_type + "." + m_name + ".level", m_logLevel);

        m_conLevel = getLevelProperty(CONSOLE_PROP_PRE + ".level", LogService.LOG_ERROR + 1);
        m_conLevel = getLevelProperty(CONSOLE_PROP_PRE + "." + m_type + ".level", m_conLevel);
        m_conLevel = getLevelProperty(CONSOLE_PROP_PRE + "." + m_type + "." + m_name + ".level", m_conLevel);

        try {
            startComponent();
        }
        catch (Exception e) {
            logWarning("Exception starting component", e);
        }
        logDebug("started (frameworkUUID=%s)", getFrameworkUUID());
    }

    /**
     * Final implementation of the DependecyManager lifecycle stop callback.
     * 
     * @throws Exception
     * @see {@link #stopComponent()}
     */
    protected final void stop() throws Exception {
        try {
            stopComponent();
        }
        catch (Exception e) {
            logWarning("Exception stopping component", e);
        }
        finally {
            m_executor.shutdown();
            if (!m_executor.awaitTermination(1l, TimeUnit.SECONDS)) {
                m_executor.shutdownNow();
            }
            m_executor = null;
            logDebug("stopped (frameworkUUID=%s)", getFrameworkUUID());
        }
    }

    /**
     * Lifecycle method called when the component is started.
     * 
     * @throws Exception
     */
    protected void startComponent() throws Exception {
    }

    /**
     * Lifecycle method called when the component is started.
     * 
     * @throws Exception
     */
    protected void stopComponent() throws Exception {
    }

    /**
     * Returns the BundleContext
     * 
     * @return the BundleContext
     */
    public final BundleContext getBundleContext() {
        return m_context;
    }

    public final String getFrameworkUUID() {
        return m_frameworkUUID;
    }

    /**
     * Submit a task for asynchronous ordered execution.
     * 
     * @param task the task
     */
    public final void executeTask(Runnable task) {
        m_executor.submit(task);
    }

    public final void logDebug(String message, Object... args) {
        log(LogService.LOG_DEBUG, message, null, args);
    }

    public final void logDebug(String message, Throwable cause, Object... args) {
        log(LogService.LOG_DEBUG, message, cause, args);
    }

    public final void logInfo(String message, Object... args) {
        log(LogService.LOG_INFO, message, null, args);
    }

    public final void logInfo(String message, Throwable cause, Object... args) {
        log(LogService.LOG_INFO, message, cause, args);
    }

    public final void logWarning(String message, Object... args) {
        log(LogService.LOG_WARNING, message, null, args);
    }

    public final void logWarning(String message, Throwable cause, Object... args) {
        log(LogService.LOG_WARNING, message, cause, args);
    }

    public final void logError(String message, Object... args) {
        log(LogService.LOG_ERROR, message, null, args);
    }

    public final void logError(String message, Throwable cause, Object... args) {
        log(LogService.LOG_ERROR, message, cause, args);
    }

    private final void log(int level, String message, Throwable cause, Object... args) {
        if (level <= m_logLevel || level <= m_conLevel) {
            if (args.length > 0) {
                message = String.format(message, processArgs(args));
            }
            message = DF.format(new Date()) + " " + m_identifier + " " + message;

            LogService logService = m_log;
            if (level <= m_logLevel && logService != null) {
                if (cause != null) {
                    logService.log(level, message, cause);
                }
                else {
                    logService.log(level, message);
                }
            }
            if (level <= m_conLevel) {
                System.out.println("[CONSOLE] " + getLevelName(level) + " " + message);
                if (cause != null) {
                    cause.printStackTrace(System.out);
                }
            }
        }
    }

    private static final Object[] processArgs(Object... args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof ServiceReference<?>) {
                args[i] = toString((ServiceReference<?>) args[i]);
            }
            else if (args[i] instanceof ServiceRegistration<?>) {
                args[i] = toString(((ServiceRegistration<?>) args[i]).getReference());
            }
            else if (args[i] instanceof Bundle) {
                args[i] = toString((Bundle) args[i]);
            }
        }
        return args;
    }

    private static String toString(ServiceReference<?> reference) {
        StringBuilder builder = new StringBuilder().append("{");
        for (String propertyKey : reference.getPropertyKeys()) {
            Object propertyValue = reference.getProperty(propertyKey);
            builder.append(propertyKey).append("=");
            if (propertyValue.getClass().isArray()) {
                builder.append("[");
                for (int i = 0; i < Array.getLength(propertyValue); i++) {
                    builder.append(Array.get(propertyValue, i));
                    if (i < Array.getLength(propertyValue) - 1) {
                        builder.append(", ");
                    }
                }
                builder.append("]");
            }
            else {
                builder.append(propertyValue.toString());
            }
            builder.append(", ");
        }
        builder.setLength(builder.length() - 2);
        return builder.toString();
    }

    private static String toString(Bundle bundle) {
        return "bundle(" + bundle.getBundleId() + ") " + bundle.getSymbolicName() + "/" + bundle.getVersion();
    }

    private final int getLevelProperty(String key, int def) {
        int result = def;
        String value = m_context.getProperty(key);
        if (value != null && !value.equals("")) {
            try {
                result = Integer.parseInt(value);
            }
            catch (Exception e) {
                // ignore
            }
        }
        return result;
    }

    private final String getLevelName(int level) {
        switch (level) {
            case 1:
                return "[ERROR  ]";
            case 2:
                return "[WARNING]";
            case 3:
                return "[INFO   ]";
            case 4:
                return "[DEBUG  ]";
            default:
                return "[?????]";
        }
    }
}
