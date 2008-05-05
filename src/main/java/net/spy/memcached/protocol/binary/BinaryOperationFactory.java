package net.spy.memcached.protocol.binary;

import java.util.Collection;

import net.spy.memcached.ops.CASOperation;
import net.spy.memcached.ops.ConcatenationOperation;
import net.spy.memcached.ops.ConcatenationType;
import net.spy.memcached.ops.DeleteOperation;
import net.spy.memcached.ops.FlushOperation;
import net.spy.memcached.ops.GetOperation;
import net.spy.memcached.ops.GetsOperation;
import net.spy.memcached.ops.MutatatorOperation;
import net.spy.memcached.ops.Mutator;
import net.spy.memcached.ops.NoopOperation;
import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.OperationFactory;
import net.spy.memcached.ops.StatsOperation;
import net.spy.memcached.ops.StoreOperation;
import net.spy.memcached.ops.StoreType;
import net.spy.memcached.ops.VersionOperation;
import net.spy.memcached.ops.GetOperation.Callback;
import net.spy.memcached.util.KeyUtil;

/**
 * Factory for binary operations.
 */
public class BinaryOperationFactory implements OperationFactory {

	public DeleteOperation delete(byte[] key,
			OperationCallback operationCallback) {
		return new DeleteOperationImpl(key, operationCallback);
	}

	public FlushOperation flush(int delay, OperationCallback cb) {
		return new FlushOperationImpl(cb);
	}

	public GetOperation get(byte[] key, Callback callback) {
		return new GetOperationImpl(key, callback);
	}

	public GetOperation get(Collection<byte[]> value, Callback cb) {
		return new MultiGetOperationImpl(KeyUtil.getKeyStrings(value), cb);
	}

	public GetsOperation gets(byte[] key, GetsOperation.Callback cb) {
		return new GetOperationImpl(key, cb);
	}

	public MutatatorOperation mutate(Mutator m, byte[] key, int by,
			long def, int exp, OperationCallback cb) {
		return new MutatorOperationImpl(m, key, by, def, exp, cb);
	}

	public StatsOperation stats(byte[] arg,
			net.spy.memcached.ops.StatsOperation.Callback cb) {
		return new StatsOperationImpl(arg, cb);
	}

	public StoreOperation store(StoreType storeType, byte[] key, int flags,
			int exp, byte[] data, OperationCallback cb) {
		return new StoreOperationImpl(storeType, key, flags, exp, data, 0, cb);
	}

	public VersionOperation version(OperationCallback cb) {
		return new VersionOperationImpl(cb);
	}

	public NoopOperation noop(OperationCallback cb) {
		return new NoopOperationImpl(cb);
	}

	public CASOperation cas(byte[] key, long casId, int flags, int exp,
			byte[] data, OperationCallback cb) {
		return new StoreOperationImpl(StoreType.set, key, flags, exp, data,
				casId, cb);
	}

	public ConcatenationOperation cat(ConcatenationType catType, long casId,
			byte[] key, byte[] data, OperationCallback cb) {
		return new ConcatenationOperationImpl(catType, key, data, casId, cb);
	}

}
