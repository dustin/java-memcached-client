package net.spy.memcached.protocol.binary;

import java.util.Collection;
import java.util.Collections;

import net.spy.memcached.MemcachedNode;
import net.spy.memcached.ops.KeyedOperation;
import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.VBucketAware;

/**
 * Binary operations that contain a single key and are VBucket aware
 * operations should extend this class.
 */
abstract class SingleKeyOperationImpl extends OperationImpl
		implements VBucketAware, KeyedOperation{

	protected final String key;

	protected SingleKeyOperationImpl(int c, int o, String k, OperationCallback cb) {
		super(c, o, cb);
		key = k;
	}

	public Collection<String> getKeys() {
		return Collections.singleton(key);
	}

	public Collection<MemcachedNode> getNotMyVbucketNodes() {
		return notMyVbucketNodes;
	}

	public void addNotMyVbucketNode(MemcachedNode node) {
		notMyVbucketNodes.add(node);
	}

	public void setNotMyVbucketNodes(Collection<MemcachedNode> nodes) {
		notMyVbucketNodes = nodes;
	}

	public void setVBucket(String k, short vb) {
		assert k.equals(key) : (k + " doesn't match the key " + key + " for this operation");
		vbucket = vb;
	}

	public short getVBucket(String k) {
		assert k.equals(key) : (k + " doesn't match the key " + key + " for this operation");
		return vbucket;
	}
}
