// Copyright (c)  2006  Dustin Sallings <dustin@spy.net>

package net.spy.memcached.compat;

import org.jmock.MockObjectTestCase;
import org.jmock.core.Invocation;
import org.jmock.core.InvocationMatcher;

/**
 * Base test case for mock object tests.
 */
public abstract class BaseMockCase extends MockObjectTestCase {

    /**
     * An invocation matcher that will allow any (0 or more) invocations of
     * an expectation.
     */
    protected InvocationMatcher any() {
        return new InvocationMatcher() {

            public boolean matches(Invocation i) { return true; }
            public void invoked(Invocation i) {
                // do nothing
            }
            public boolean hasDescription() { return true; }
            public void verify() {
                // do nothing
            }
            public StringBuffer describeTo(StringBuffer buf) {
                return buf.append("allowed");
            }
        };
    }

}
