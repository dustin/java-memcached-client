package net.spy.memcached.protocol.ascii;

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
import net.spy.memcached.util.KeyUtil;

/**
 * Operation factory for the ascii protocol.
 */
public final class AsciiOperationFactory implements OperationFactory {

	public DeleteOperation delete(byte[] key, OperationCallback cb) {
		return new DeleteOperationImpl(KeyUtil.getKeyString(key), cb);
	}

	public FlushOperation flush(int delay, OperationCallback cb) {
		return new FlushOperationImpl(delay, cb);
	}

	public GetOperation get(byte[] key, GetOperation.Callback cb) {
		return new GetOperationImpl(KeyUtil.getKeyString(key), cb);
	}

	public GetOperation get(Collection<byte[]> keys, GetOperation.Callback cb) {
		return new GetOperationImpl(KeyUtil.getKeyStrings(keys), cb);
	}

	public GetsOperation gets(byte[] key, GetsOperation.Callback cb) {
		 return new GetsOperationImpl(KeyUtil.getKeyString(key), cb);
	}

	public MutatatorOperation mutate(Mutator m, byte[] key, int by,
			long exp, int def, OperationCallback cb) {
		return new MutatorOperationImpl(m, KeyUtil.getKeyString(key), by, cb);
	}

	public StatsOperation stats(byte[] arg, StatsOperation.Callback cb) {
		return new StatsOperationImpl(arg, cb);
	}

	public StoreOperation store(StoreType storeType, byte[] key, int flags,
			int exp, byte[] data, OperationCallback cb) {
		return new StoreOperationImpl(storeType, KeyUtil.getKeyString(key),
				flags, exp, data, cb);
	}

	public VersionOperation version(OperationCallback cb) {
		return new VersionOperationImpl(cb);
	}

	public NoopOperation noop(OperationCallback cb) {
		return new VersionOperationImpl(cb);
	}

	public CASOperation cas(byte[] key, long casId, int flags, int exp,
			byte[] data, OperationCallback cb) {
		return new CASOperationImpl(KeyUtil.getKeyString(key), casId,
				flags, exp, data, cb);
	}

	public ConcatenationOperation cat(ConcatenationType catType,
			long casId,
			byte[] key, byte[] data, OperationCallback cb) {
		return new ConcatenationOperationImpl(catType,
			KeyUtil.getKeyString(key), data, cb);
	}

}
