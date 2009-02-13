package net.spy.memcached.ops;

import java.util.ArrayList;
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

	private String first(Collection<String> keys) {
		return keys.iterator().next();
	}

	public Collection<Operation> clone(KeyedOperation op) {
		assert op.getState() == OperationState.WRITING
			: "Who passed me an operation in the " + op.getState() + "state?";
		assert !op.isCancelled() : "Attempted to clone a canceled op";
		assert !op.hasErrored() : "Attempted to clone an errored op";

		Collection<Operation> rv = new ArrayList<Operation>(
				op.getKeys().size());
		if(op instanceof GetOperation) {
			rv.addAll(cloneGet(op));
		} else if(op instanceof GetsOperation) {
			GetsOperation.Callback callback =
				(GetsOperation.Callback)op.getCallback();
			for(String k : op.getKeys()) {
				rv.add(gets(k, callback));
			}
		} else if(op instanceof CASOperation) {
			CASOperation cop = (CASOperation)op;
			rv.add(cas(cop.getStoreType(), first(op.getKeys()),
					cop.getCasValue(), cop.getFlags(), cop.getExpiration(),
					cop.getBytes(), cop.getCallback()));
		} else if(op instanceof DeleteOperation) {
			rv.add(delete(first(op.getKeys()), op.getCallback()));
		} else if(op instanceof MutatorOperation) {
			MutatorOperation mo = (MutatorOperation)op;
			rv.add(mutate(mo.getType(), first(op.getKeys()),
					mo.getBy(), mo.getDefault(), mo.getExpiration(),
					op.getCallback()));
		} else if(op instanceof StoreOperation) {
			StoreOperation so = (StoreOperation)op;
			rv.add(store(so.getStoreType(), first(op.getKeys()), so.getFlags(),
					so.getExpiration(), so.getData(), op.getCallback()));
		} else if(op instanceof ConcatenationOperation) {
			ConcatenationOperation c = (ConcatenationOperation)op;
			rv.add(cat(c.getStoreType(), c.getCasValue(), first(op.getKeys()),
					c.getData(), c.getCallback()));
		} else {
			assert false : "Unhandled operation type: " + op.getClass();
		}

		return rv;
	}

	protected abstract Collection<? extends Operation> cloneGet(
			KeyedOperation op);

}
