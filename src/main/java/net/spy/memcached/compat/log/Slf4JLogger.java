package net.spy.memcached.compat.log;

import static java.lang.String.format;

/**
 * Logging implementation using slf4j.
 */
public class Slf4JLogger implements Logger
{
    private final org.slf4j.Logger internalLogger;
    private final String name;

    /**
     * Get an instance of Slf4JLogger.
     */
    public Slf4JLogger(final String name)
    {
        this.name = name;
        this.internalLogger = org.slf4j.LoggerFactory.getLogger(name);
    }

    private String checkNull(final Object object)
    {
        return object == null ? "<null>" : object.toString();
    }

    Throwable getThrowable(final Object[] args)
    {
        Throwable rv = null;
        if (args != null && args.length > 0 && args[args.length - 1] instanceof Throwable) {
          rv = Throwable.class.cast(args[args.length - 1]);
        }
        return rv;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public boolean isDebugEnabled()
    {
        return internalLogger.isDebugEnabled();
    }

    @Override
    public boolean isInfoEnabled()
    {
        return internalLogger.isInfoEnabled();
    }

    @Override
    public void debug(Object message, Throwable exception)
    {
        internalLogger.debug(checkNull(message), exception);
    }

    @Override
    public void debug(Object message)
    {
        internalLogger.debug(checkNull(message));
    }

    @Override
    public void debug(String message, Object... args)
    {
        final Throwable t = getThrowable(args);
        if (t == null) {
            internalLogger.debug(format(message, args));
        }
        else {
            internalLogger.debug(format(message, args), t);
        }
    }

    @Override
    public void info(Object message, Throwable exception)
    {
        internalLogger.info(checkNull(message), exception);
    }

    @Override
    public void info(Object message)
    {
        internalLogger.info(checkNull(message));
    }

    @Override
    public void info(String message, Object... args)
    {
        final Throwable t = getThrowable(args);
        if (t == null) {
            internalLogger.info(format(message, args));
        }
        else {
            internalLogger.info(format(message, args), t);
        }
    }

    @Override
    public void warn(Object message, Throwable exception)
    {
        internalLogger.warn(checkNull(message), exception);
    }

    @Override
    public void warn(Object message)
    {
        internalLogger.warn(checkNull(message));
    }

    @Override
    public void warn(String message, Object... args)
    {
        final Throwable t = getThrowable(args);
        if (t == null) {
            internalLogger.warn(format(message, args));
        }
        else {
            internalLogger.warn(format(message, args), t);
        }
    }

    @Override
    public void error(Object message, Throwable exception)
    {
        internalLogger.error(checkNull(message), exception);
    }

    @Override
    public void error(Object message)
    {
        internalLogger.error(checkNull(message));
    }

    @Override
    public void error(String message, Object... args)
    {
        final Throwable t = getThrowable(args);
        if (t == null) {
            internalLogger.error(format(message, args));
        }
        else {
            internalLogger.error(format(message, args), t);
        }
    }

    @Override
    public void fatal(Object message, Throwable exception)
    {
        error(message, exception);
    }

    @Override
    public void fatal(Object message)
    {
        error(message);
    }

    @Override
    public void fatal(String message, Object... args)
    {
        error(message, args);
    }
}
