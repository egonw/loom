package net.idea.i6.cli;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.opentox.rest.RestException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.idea.iuclid.cli.IContainerClient;
import net.idea.iuclid.cli.IUCLIDAbstractClient;
import net.idea.opentox.cli.IIdentifiableResource;
import net.idea.opentox.cli.id.IIdentifier;

public class I6ContainerClient extends IUCLIDAbstractClient<I6Credentials> implements IContainerClient {

	public I6ContainerClient(HttpClient httpclient, String baseURL, I6Credentials token) {
		super(httpclient, baseURL, token);
	}

	final static String MediaTypeI6Job = "application/vnd.iuclid6.ext+json; type=iuclid6.Iuclid6Job";
	final static String MediaTypeI6Export = "application/vnd.iuclid6.ext+json; type=iuclid6.FullExport";

	protected enum _I6JOBSTATUS {
		QUEUED {
			@Override
			public boolean isFinal() {
				return false;
			}
		},
		IN_PROGRESS {
			@Override
			public boolean isFinal() {
				return false;
			}
		},
		SUCCEEDED, FAILED, CANCELED;
		public boolean isFinal() {
			return true;
		}
	}

	@Override
	protected List<IIdentifiableResource<IIdentifier>> get(IIdentifier identifier, String mediaType, String... params)
			throws RestException, IOException {

		HttpPost httpPOST = new HttpPost(String.format("%s/raw/SUBSTANCE/%s/export", baseURL, identifier.toString()));

		if (headers != null)
			for (Header header : headers)
				httpPOST.addHeader(header);

		// httpPOST.addHeader("Accept-Encoding", "gzip,deflate");
		httpPOST.addHeader("Content-type", MediaTypeI6Export);
		httpPOST.addHeader("Accept", MediaTypeI6Job);

		HttpEntity content = new StringEntity("{}", "UTF-8");
		httpPOST.setEntity(content);

		logger.log(Level.INFO, httpPOST.getURI().toString());
		logger.log(Level.INFO, Arrays.toString(httpPOST.getAllHeaders()));
		try {
			HttpResponse response = getHttpClient().execute(httpPOST);
			HttpEntity entity = response.getEntity();

			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK
					|| response.getStatusLine().getStatusCode() == HttpStatus.SC_ACCEPTED) {
				try (InputStream in = entity.getContent()) {
					// return processPayload(in, identifier.toString());
					String resultURI = null;
					ObjectMapper m = new ObjectMapper();
					JsonNode node = m.readTree(in);
					String uri = node.get("uri").asText();
					_I6JOBSTATUS jobstatus = null;
					try {
						jobstatus = _I6JOBSTATUS.valueOf(node.get("status").asText());
					} catch (Exception x) {
						logger.log(Level.WARNING, x.getMessage());
						return null;
					}
					if (!jobstatus.isFinal()) {
						jobstatus = polling(uri);
						if (_I6JOBSTATUS.SUCCEEDED.equals(jobstatus))
							resultURI = uri + "/result";
						else {
							resultURI = null;
						}
					} else
						resultURI = uri + "/result";

					if (resultURI == null)
						return Collections.emptyList();

					HttpGet result = new HttpGet(String.format("%s/system%s/result", baseURL, uri));
					if (headers != null)
						for (Header header : headers)
							result.addHeader(header);
					HttpResponse resultresponse = getHttpClient().execute(result);
					if (resultresponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK)
						try (InputStream inr = resultresponse.getEntity().getContent()) {

							return processPayload(inr, identifier.toString());
						}
					
				} catch (Exception x) {
					logger.log(Level.WARNING, x.getMessage());
				}

			} else if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NOT_FOUND) {
				return Collections.emptyList();

			} else
				throw new RestException(response.getStatusLine().getStatusCode(),
						response.getStatusLine().getReasonPhrase());
			return null;
		} finally {

		}
	}

	protected _I6JOBSTATUS polling(String uri) throws RestException, IOException {
		ObjectMapper m = new ObjectMapper();
		HttpGet polling = new HttpGet(String.format("%s/system%s", baseURL, uri));
		polling.addHeader("Accept", MediaTypeI6Job);
		if (headers != null)
			for (Header header : headers)
				polling.addHeader(header);
		boolean waiting = true;
		while (waiting) {

			HttpResponse response = getHttpClient().execute(polling);
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_ACCEPTED)
				continue;

			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				try (InputStream in = response.getEntity().getContent()) {
					JsonNode node = m.readTree(in);
					_I6JOBSTATUS jobstatus = null;
					try {
						jobstatus = _I6JOBSTATUS.valueOf(node.get("status").asText());
						if (jobstatus.isFinal())
							return jobstatus;
						else
							continue;
					} catch (Exception x) {
						logger.log(Level.WARNING, x.getMessage());
						return null;
					}

				}
			}
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_UNPROCESSABLE_ENTITY)
				return _I6JOBSTATUS.FAILED;
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_INTERNAL_SERVER_ERROR)
				return _I6JOBSTATUS.FAILED;
			logger.log(Level.WARNING, response.getStatusLine().toString());
			break;
		}
		return _I6JOBSTATUS.FAILED;
	}

	@Override
	public List<IIdentifiableResource<IIdentifier>> processPayload(InputStream in, String identifier)
			throws RestException, IOException {
		File tmpFile = File.createTempFile("i6ws_", ".i6z");
		net.idea.loom.common.DownloadTool.download(in, tmpFile);
		return null;
	}

}
