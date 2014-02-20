/**
 * Copyright (C) 2006-2009 Dustin Sallings
 * Copyright (C) 2009-2011 Couchbase, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALING
 * IN THE SOFTWARE.
 */

package net.spy.memcached.ops;

import java.util.ArrayList;
import java.util.Collection;

import net.spy.memcached.MemcachedNode;
import net.spy.memcached.OperationFactory;

/**
 * Base class for operation factories.
 *
 * <p>
 * There is little common code between OperationFactory implementations, but
 * some exists, and is complicated and likely to cause problems.
 * </p>
 */
public abstract class BaseOperationFactory implements OperationFactory {

  private String first(Collection<String> keys) {
    return keys.iterator().next();
  }

  public Collection<Operation> clone(KeyedOperation op) {
    assert (op.getState() == OperationState.WRITE_QUEUED || op.getState()
        == OperationState.RETRY) : "Who passed me an operation in the "
        + op.getState() + "state?";
    assert !op.isCancelled() : "Attempted to clone a canceled op";
    assert !op.hasErrored() : "Attempted to clone an errored op";

    Collection<Operation> rv = new ArrayList<Operation>(op.getKeys().size());
    if (op instanceof GetOperation) {
      rv.addAll(cloneGet(op));
    } else if (op instanceof ReplicaGetOperation) {
      rv.addAll(cloneGet(op));
    } else if (op instanceof ReplicaGetsOperation) {
      ReplicaGetsOperation.Callback callback =
        (ReplicaGetsOperation.Callback) op.getCallback();
      for (String k : op.getKeys()) {
        rv.add(replicaGets(k,
          ((ReplicaGetsOperation) op).getReplicaIndex(), callback));
      }
    } else if (op instanceof GetsOperation) {
      GetsOperation.Callback callback =
          (GetsOperation.Callback) op.getCallback();
      for (String k : op.getKeys()) {
        rv.add(gets(k, callback));
      }
    } else if (op instanceof CASOperation) {
      CASOperation cop = (CASOperation) op;
      rv.add(cas(cop.getStoreType(), first(op.getKeys()), cop.getCasValue(),
          cop.getFlags(), cop.getExpiration(), cop.getData(),
          (StoreOperation.Callback) cop.getCallback()));
    } else if(op instanceof DeleteOperation) {
      rv.add(delete(first(op.getKeys()),
          (DeleteOperation.Callback)op.getCallback()));
    } else if (op instanceof MutatorOperation) {
      MutatorOperation mo = (MutatorOperation) op;
      rv.add(mutate(mo.getType(), first(op.getKeys()), mo.getBy(),
          mo.getDefault(), mo.getExpiration(), op.getCallback()));
    } else if (op instanceof StoreOperation) {
      StoreOperation so = (StoreOperation) op;
      rv.add(store(so.getStoreType(), first(op.getKeys()), so.getFlags(),
          so.getExpiration(), so.getData(),
          (StoreOperation.Callback) op.getCallback()));
    } else if (op instanceof ConcatenationOperation) {
      ConcatenationOperation c = (ConcatenationOperation) op;
      rv.add(cat(c.getStoreType(), c.getCasValue(), first(op.getKeys()),
          c.getData(), c.getCallback()));
    } else if(op instanceof GetAndTouchOperation) {
      GetAndTouchOperation gt = (GetAndTouchOperation) op;
      rv.add(getAndTouch(first(gt.getKeys()), gt.getExpiration(),
        (GetAndTouchOperation.Callback) gt.getCallback()));
    } else {
      assert false : "Unhandled operation type: " + op.getClass();
    }
    if (op instanceof VBucketAware) {
      VBucketAware vop = (VBucketAware) op;
      if (!vop.getNotMyVbucketNodes().isEmpty()) {
        for (Operation operation : rv) {
          if (operation instanceof VBucketAware) {
            Collection<MemcachedNode> notMyVbucketNodes =
                vop.getNotMyVbucketNodes();
            ((VBucketAware) operation).setNotMyVbucketNodes(notMyVbucketNodes);
          }
        }
      }
    }
    return rv;
  }

  protected abstract Collection<? extends Operation>
  cloneGet(KeyedOperation op);
}
