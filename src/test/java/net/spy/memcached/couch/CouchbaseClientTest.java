package net.spy.memcached.couch;

import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import net.spy.memcached.TestConfig;
import net.spy.memcached.internal.HttpFuture;
import net.spy.memcached.internal.ViewFuture;
import net.spy.memcached.ops.OperationStatus;
import net.spy.memcached.protocol.couch.DocsOperation.DocsCallback;
import net.spy.memcached.protocol.couch.DocsOperationImpl;
import net.spy.memcached.protocol.couch.HttpOperation;
import net.spy.memcached.protocol.couch.NoDocsOperation.NoDocsCallback;
import net.spy.memcached.protocol.couch.NoDocsOperationImpl;
import net.spy.memcached.protocol.couch.ReducedOperation.ReducedCallback;
import net.spy.memcached.protocol.couch.ReducedOperationImpl;
import net.spy.memcached.protocol.couch.RowError;
import net.spy.memcached.protocol.couch.RowWithDocs;
import net.spy.memcached.protocol.couch.Stale;
import net.spy.memcached.protocol.couch.ViewResponseNoDocs;
import net.spy.memcached.protocol.couch.Query;
import net.spy.memcached.protocol.couch.ViewResponseReduced;
import net.spy.memcached.protocol.couch.RowReduced;
import net.spy.memcached.protocol.couch.RowNoDocs;
import net.spy.memcached.protocol.couch.View;
import net.spy.memcached.protocol.couch.ViewResponseWithDocs;

public class CouchbaseClientTest {
	protected TestingClient client = null;
	private static final String SERVER_URI = "http://" + TestConfig.IPV4_ADDR
			+ ":8091/pools";
	private static final Map<String, Object> items;
	public static final String DESIGN_DOC_W_REDUCE = "doc_with_view";
	public static final String DESIGN_DOC_WO_REDUCE = "doc_without_view";
	public static final String VIEW_NAME_W_REDUCE = "view_with_reduce";
	public static final String VIEW_NAME_WO_REDUCE = "view_without_reduce";

	static {
		items = new HashMap<String, Object>();
		int d = 0;
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 2; j++) {
				for (int k = 0; k < 2; k++, d++) {
					String type = new String(new char[] { 'a' + 26 });
					String small = (new Integer(j)).toString();
					String large = (new Integer(k)).toString();
					String doc = generateDoc(type, small, large);
					items.put("key" + d, doc);
				}
			}
		}
	}

	protected void initClient() throws Exception {
		List<URI> uris = new LinkedList<URI>();
		uris.add(URI.create(SERVER_URI));
		client = new TestingClient(uris, "default", "");
	}

	@BeforeClass
	public static void before() throws Exception {
		// Create some design documents
		List<URI> uris = new LinkedList<URI>();
		uris.add(URI.create(SERVER_URI));
		TestingClient c = new TestingClient(uris, "default", "");
		String docUri = "/default/_design/" + TestingClient.MODE_PREFIX
			+ DESIGN_DOC_W_REDUCE;
		String view = "{\"language\":\"javascript\",\"views\":{\""
				+ VIEW_NAME_W_REDUCE + "\":{\"map\":\"function (doc) {  "
				+ "emit(doc._id, 1)}\",\"reduce\":\"_sum\" }}}";
		c.asyncHttpPut(docUri, view);

		docUri = "/default/_design/" + TestingClient.MODE_PREFIX
			+ DESIGN_DOC_WO_REDUCE;
		view = "{\"language\":\"javascript\",\"views\":{\""
				+ VIEW_NAME_WO_REDUCE + "\":{\"map\":\"function (doc) {  "
				+ "emit(doc._id, 1)}\"}}}";
		for (Entry<String, Object> item : items.entrySet()) {
			assert c.set(item.getKey(), 0, (String) item.getValue()).get().booleanValue();
		}
		c.asyncHttpPut(docUri, view);
		c.shutdown();
		Thread.sleep(15000);
	}

	@Before
	public void beforeTest() throws Exception {
		initClient();
	}

	@After
	public void afterTest() throws Exception {
		// Shut down, start up, flush, and shut down again. Error tests have
		// unpredictable timing issues.
		client.shutdown();
		client = null;
	}

	@AfterClass
	public static void after() throws Exception {
		// Delete all design documents I created
		List<URI> uris = new LinkedList<URI>();
		uris.add(URI.create(SERVER_URI));
		TestingClient c = new TestingClient(uris, "default", "");
		String json = c.asyncHttpGet("/default/_design/" + TestingClient.MODE_PREFIX
				+ DESIGN_DOC_W_REDUCE).get();
		String rev = (new JSONObject(json)).getString("_rev");
		c.asyncHttpDelete("/default/_design/" + TestingClient.MODE_PREFIX
				+ DESIGN_DOC_W_REDUCE + "?rev=" + rev).get();

		json = c.asyncHttpGet("/default/_design/" + TestingClient.MODE_PREFIX
				+ DESIGN_DOC_WO_REDUCE).get();
		rev = (new JSONObject(json)).getString("_rev");
		c.asyncHttpDelete("/default/_design/" + TestingClient.MODE_PREFIX
				+ DESIGN_DOC_WO_REDUCE + "?rev=" + rev).get();
		assert c.flush().get().booleanValue();
	}

	private static String generateDoc(String type, String small, String large) {
		return "{\"type\":\"" + type + "\"" + "\"small range\":\"" + small
				+ "\"" + "\"large range\":\"" + large + "\"}";
	}

	@Test
	public void testAssertions() {
		boolean caught=false;
		try {
			assert false;
		} catch(AssertionError e) {
			caught=true;
		}
		assertTrue("Assertions are not enabled!", caught);
	}

	@Test
	public void testQueryWithDocs() throws Exception {
		Query query = new Query();
		View view = client.getView(DESIGN_DOC_W_REDUCE, VIEW_NAME_W_REDUCE);
		ViewFuture future = client.asyncQuery(view, query);
		ViewResponseWithDocs response = future.get();
		assert future.getStatus().isSuccess() : future.getStatus();

		Iterator<RowWithDocs> itr = response.iterator();
		while (itr.hasNext()) {
			RowWithDocs row = itr.next();
			if (items.containsKey(row.getId())) {
				assert items.get(row.getId()).equals(row.getDoc());
			}
		}
		assert items.size() == response.size() :
			future.getStatus().getMessage();
	}

	@Test
	public void testViewNoDocs() throws Exception {
		Query query = new Query();
		View view = client.getView(DESIGN_DOC_W_REDUCE, VIEW_NAME_W_REDUCE);
		HttpFuture<ViewResponseNoDocs> future = client.asyncQueryAndExcludeDocs(
				view, query);
		assert future.getStatus().isSuccess() : future.getStatus();
		ViewResponseNoDocs response = future.get();

		Iterator<RowNoDocs> itr = response.iterator();
		while (itr.hasNext()) {
			RowNoDocs row = itr.next();
			if (!items.containsKey(row.getId())) {
				assert false : ("Got an item that I shouldn't have gotten.");
			}
		}
		assert response.size() == items.size() :
				future.getStatus();
	}

	@Test
	public void testReduce() throws Exception {
		Query query = new Query();
		View view = client.getView(DESIGN_DOC_W_REDUCE, VIEW_NAME_W_REDUCE);
		HttpFuture<ViewResponseReduced> future = client.asyncQueryAndReduce(view,
				query);
		ViewResponseReduced reduce = future.get();

		Iterator<RowReduced> itr = reduce.iterator();
		while (itr.hasNext()) {
			RowReduced row = itr.next();
			assert row.getKey() == null;
			assert Integer.valueOf(row.getValue()) == items.size() :
					future.getStatus();
		}
	}

	@Test
	public void testQuerySetDescending() throws Exception {
		Query query = new Query();
		View view = client.getView(DESIGN_DOC_W_REDUCE, VIEW_NAME_W_REDUCE);
		ViewFuture future = client.asyncQuery(view, query.setDescending(true));
		ViewResponseWithDocs response = future.get();
		assert response != null : future.getStatus();
	}

	@Test
	public void testQuerySetEndKeyDocID() throws Exception {
		Query query = new Query();
		View view = client.getView(DESIGN_DOC_W_REDUCE, VIEW_NAME_W_REDUCE);
		ViewFuture future = client.asyncQuery(view, query.setEndkeyDocID("an_id"));
		ViewResponseWithDocs response = future.get();
		assert response != null : future.getStatus();
	}

	@Test
	public void testQuerySetGroup() throws Exception {
		Query query = new Query();
		View view = client.getView(DESIGN_DOC_W_REDUCE, VIEW_NAME_W_REDUCE);
		HttpFuture<ViewResponseReduced> future = client.asyncQueryAndReduce(view,
				query.setGroup(true));
		ViewResponseReduced response = future.get();
		assert response != null : future.getStatus();
	}

	@Test
	public void testQuerySetGroupWithLevel() throws Exception {
		Query query = new Query();
		View view = client.getView(DESIGN_DOC_W_REDUCE, VIEW_NAME_W_REDUCE);
		HttpFuture<ViewResponseReduced> future = client.asyncQueryAndReduce(view,
				query.setGroup(true, 1));
		ViewResponseReduced response = future.get();
		assert response != null : future.getStatus();
	}

	@Test
	public void testQuerySetInclusiveEnd() throws Exception {
		Query query = new Query();
		View view = client.getView(DESIGN_DOC_W_REDUCE, VIEW_NAME_W_REDUCE);
		ViewFuture future = client.asyncQuery(view, query.setInclusiveEnd(true));
		ViewResponseWithDocs response = future.get();
		assert response != null : future.getStatus();
	}

	@Test
	public void testQuerySetKey() throws Exception {
		Query query = new Query();
		View view = client.getView(DESIGN_DOC_W_REDUCE, VIEW_NAME_W_REDUCE);
		ViewFuture future = client.asyncQuery(view, query.setKey("a_key"));
		ViewResponseWithDocs response = future.get();
		assert response != null : future.getStatus();
	}

	@Test
	public void testQuerySetLimit() throws Exception {
		Query query = new Query();
		View view = client.getView(DESIGN_DOC_W_REDUCE, VIEW_NAME_W_REDUCE);
		ViewFuture future = client.asyncQuery(view, query.setLimit(10));
		ViewResponseWithDocs response = future.get();
		assert response != null : future.getStatus();
	}

	@Test
	public void testQuerySetRange() throws Exception {
		Query query = new Query();
		View view = client.getView(DESIGN_DOC_W_REDUCE, VIEW_NAME_W_REDUCE);
		ViewFuture future = client.asyncQuery(view, query.setRange("key0", "key2"));
		ViewResponseWithDocs response = future.get();
		assert response != null : future.getStatus();
	}

	@Test
	public void testQuerySetRangeStart() throws Exception {
		Query query = new Query();
		View view = client.getView(DESIGN_DOC_W_REDUCE, VIEW_NAME_W_REDUCE);
		ViewFuture future = client.asyncQuery(view, query.setRangeStart("start"));
		ViewResponseWithDocs response = future.get();
		assert response != null : future.getStatus();
	}

	@Test
	public void testQuerySetRangeEnd() throws Exception {
		Query query = new Query();
		View view = client.getView(DESIGN_DOC_W_REDUCE, VIEW_NAME_W_REDUCE);
		ViewFuture future = client.asyncQuery(view, query.setRangeEnd("end"));
		ViewResponseWithDocs response = future.get();
		assert response != null : future.getStatus();
	}

	@Test
	public void testQuerySetSkip() throws Exception {
		Query query = new Query();
		View view = client.getView(DESIGN_DOC_W_REDUCE, VIEW_NAME_W_REDUCE);
		ViewFuture future = client.asyncQuery(view, query.setSkip(0));
		ViewResponseWithDocs response = future.get();
		assert response != null : future.getStatus();
	}

	@Test
	public void testQuerySetStale() throws Exception {
		Query query = new Query();
		View view = client.getView(DESIGN_DOC_W_REDUCE, VIEW_NAME_W_REDUCE);
		ViewFuture future = client.asyncQuery(view, query.setStale(Stale.OK));
		ViewResponseWithDocs response = future.get();
		assert response != null : future.getStatus();
	}

	@Test
	public void testQuerySetStartkeyDocID() throws Exception {
		Query query = new Query();
		View view = client.getView(DESIGN_DOC_W_REDUCE, VIEW_NAME_W_REDUCE);
		ViewFuture future = client.asyncQuery(view, query.setStartkeyDocID("key0"));
		ViewResponseWithDocs response = future.get();
		assert response != null : future.getStatus();
	}

	@Test
	public void testQuerySetUpdateSeq() throws Exception {
		Query query = new Query();
		View view = client.getView(DESIGN_DOC_W_REDUCE, VIEW_NAME_W_REDUCE);
		ViewFuture future = client.asyncQuery(view, query.setUpdateSeq(true));
		ViewResponseWithDocs response = future.get();
		assert response != null : future.getStatus();
	}

	@Test
	public void testReduceWhenNoneExists() throws Exception {
		Query query = new Query();
		View view = client.getView(DESIGN_DOC_WO_REDUCE, VIEW_NAME_WO_REDUCE);
		try {
			client.asyncQueryAndReduce(view, query);
		} catch (RuntimeException e) {
			return; // Pass, no reduce exists.
		}
		assert false : ("No view exists and this query still happened");
	}

	@Test
	public void testViewDocsWithErrors() throws Exception {
		HttpOperation op = new DocsOperationImpl(null, new DocsCallback() {
			@Override
			public void receivedStatus(OperationStatus status) {
				assert status.isSuccess();
			}
			@Override
			public void complete() {
				// Do nothing
			}
			@Override
			public void gotData(ViewResponseWithDocs response) {
				assert response.getErrors().size() == 2;
				Iterator<RowError> row = response.getErrors().iterator();
				assert row.next().getFrom().equals("127.0.0.1:5984");
				assert response.size() == 0;
			}
		});
		HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "");
		String entityString = "{\"total_rows\":0,\"rows\":[],\"errors\": [{\"from\":" +
				"\"127.0.0.1:5984\",\"reason\":\"Design document `_design/testfoobar" +
				"` missing in database `test_db_b`.\"},{\"from\":\"http://localhost:5984" +
				"/_view_merge/\",\"reason\":\"Design document `_design/testfoobar`" +
				" missing in database `test_db_c`.\"}]}";
		StringEntity entity = new StringEntity(entityString);
		response.setEntity(entity);
		op.handleResponse(response);
	}

	@Test
	public void testViewNoDocsWithErrors() throws Exception {
		HttpOperation op = new NoDocsOperationImpl(null, new NoDocsCallback() {
			@Override
			public void receivedStatus(OperationStatus status) {
				assert status.isSuccess();
			}
			@Override
			public void complete() {
				// Do nothing
			}
			@Override
			public void gotData(ViewResponseNoDocs response) {
				assert response.getErrors().size() == 2;
				Iterator<RowError> row = response.getErrors().iterator();
				assert row.next().getFrom().equals("127.0.0.1:5984");
				assert response.size() == 0;
			}
		});
		HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "");
		String entityString = "{\"total_rows\":0,\"rows\":[],\"errors\": [{\"from\":" +
				"\"127.0.0.1:5984\",\"reason\":\"Design document `_design/testfoobar" +
				"` missing in database `test_db_b`.\"},{\"from\":\"http://localhost:5984" +
				"/_view_merge/\",\"reason\":\"Design document `_design/testfoobar`" +
				" missing in database `test_db_c`.\"}]}";
		StringEntity entity = new StringEntity(entityString);
		response.setEntity(entity);
		op.handleResponse(response);
	}

	@Test
	public void testViewReducedWithErrors() throws Exception {
		HttpOperation op = new ReducedOperationImpl(null, new ReducedCallback() {
			@Override
			public void receivedStatus(OperationStatus status) {
				assert status.isSuccess();
			}
			@Override
			public void complete() {
				// Do nothing
			}
			@Override
			public void gotData(ViewResponseReduced response) {
				assert response.getErrors().size() == 2;
				Iterator<RowError> row = response.getErrors().iterator();
				assert row.next().getFrom().equals("127.0.0.1:5984");
				assert response.size() == 0;
			}
		});
		HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "");
		String entityString = "{\"total_rows\":0,\"rows\":[],\"errors\": [{\"from\":" +
				"\"127.0.0.1:5984\",\"reason\":\"Design document `_design/testfoobar" +
				"` missing in database `test_db_b`.\"},{\"from\":\"http://localhost:5984" +
				"/_view_merge/\",\"reason\":\"Design document `_design/testfoobar`" +
				" missing in database `test_db_c`.\"}]}";
		StringEntity entity = new StringEntity(entityString);
		response.setEntity(entity);
		op.handleResponse(response);
	}
}
