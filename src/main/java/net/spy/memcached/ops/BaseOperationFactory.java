package net.spy.memcached.ops;

import java.util.Collection;

import net.spy.memcached.OperationFactory;

/**
 * Base class for operation factories.
 *
 * <p>
 *   There is little common code between OperationFactory implementations, but
 *   some exists, and is complicated and likely to cause problems.
 * </p>
 */
public abstract class BaseOperationFactory implements OperationFactory {

	public Collection<Operation> clone(KeyedOperation op) {
		// TODO:  An implementation or something.
		throw new RuntimeException("Not implemented");
	}

}
