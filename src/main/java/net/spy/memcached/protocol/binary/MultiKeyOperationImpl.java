package net.spy.memcached.protocol.binary;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.spy.memcached.MemcachedNode;
import net.spy.memcached.ops.KeyedOperation;
import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.VBucketAware;
import net.spy.memcached.util.StringUtils;

/**
 * Binary operations that contain multiple keys and are VBucket aware
 * operations should extend this class.
 */
abstract class MultiKeyOperationImpl extends OperationImpl
		implements VBucketAware, KeyedOperation {
	protected final Map<String, Short> vbmap;

	protected MultiKeyOperationImpl(byte c, int o, OperationCallback cb) {
		super(c, o, cb);
		vbmap = Collections.synchronizedMap(new HashMap<String, Short>());
	}

	public Collection<String> getKeys() {
		return vbmap.keySet();
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
		assert vbmap.containsKey(k) : "Key " + k + " not contained in operation";
		vbmap.put(k, new Short(vb));
	}

	public short getVBucket(String k) {
		assert vbmap.containsKey(k) : "Key " + k + " not contained in operation" ;
		return vbmap.get(k);
	}

	@Override
	public String toString() {
		synchronized(vbmap) {
			return super.toString() + " Keys: " + StringUtils.join(getKeys(), " ");
		}
	}
}
