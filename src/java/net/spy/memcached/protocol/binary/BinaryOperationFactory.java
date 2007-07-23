package net.spy.memcached.protocol.binary;

import java.util.Collection;

import net.spy.memcached.OperationFactory;
import net.spy.memcached.ops.DeleteOperation;
import net.spy.memcached.ops.FlushOperation;
import net.spy.memcached.ops.GetOperation;
import net.spy.memcached.ops.MutatatorOperation;
import net.spy.memcached.ops.Mutator;
import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.StatsOperation;
import net.spy.memcached.ops.StoreOperation;
import net.spy.memcached.ops.StoreType;
import net.spy.memcached.ops.VersionOperation;
import net.spy.memcached.ops.GetOperation.Callback;

/**
 * Factory for binary operations.
 */
public class BinaryOperationFactory implements OperationFactory {

	public DeleteOperation delete(String key, int when,
			OperationCallback operationCallback) {
		// TODO Auto-generated method stub
		return null;
	}

	public FlushOperation flush(int delay, OperationCallback cb) {
		// TODO Auto-generated method stub
		return null;
	}

	public GetOperation get(String key, Callback callback) {
		// TODO Auto-generated method stub
		return null;
	}

	public GetOperation get(Collection<String> value, Callback cb) {
		// TODO Auto-generated method stub
		return null;
	}

	public MutatatorOperation mutate(Mutator m, String key, int by,
			OperationCallback cb) {
		// TODO Auto-generated method stub
		return null;
	}

	public StatsOperation stats(String arg,
			net.spy.memcached.ops.StatsOperation.Callback cb) {
		// TODO Auto-generated method stub
		return null;
	}

	public StoreOperation store(StoreType storeType, String key, int flags,
			int exp, byte[] data, OperationCallback cb) {
		// TODO Auto-generated method stub
		return null;
	}

	public VersionOperation version(OperationCallback cb) {
		// TODO Auto-generated method stub
		return null;
	}

}
