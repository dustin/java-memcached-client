package net.spy.memcached.couch;

import java.net.HttpURLConnection;

import net.spy.memcached.ops.OperationStatus;
import net.spy.memcached.protocol.couchdb.HttpOperationImpl;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;

public class TestOperationImpl extends HttpOperationImpl implements TestOperation {

	public TestOperationImpl(HttpRequest r, TestCallback testCallback) {
		super(r, testCallback);
	}

	@Override
	public void handleResponse(HttpResponse response) {
		String json = getEntityString(response);
		int errorcode = response.getStatusLine().getStatusCode();
		if (errorcode == HttpURLConnection.HTTP_OK) {
			((TestCallback) callback).getData(json);
			callback.receivedStatus(new OperationStatus(true, "OK"));
		} else {
			callback.receivedStatus(new OperationStatus(false, Integer
					.toString(errorcode)));
		}
		callback.complete();
	}

}
