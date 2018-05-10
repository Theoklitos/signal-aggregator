package com.quantbro.aggregator.services;

import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit4.SpringRunner;

import com.quantbro.aggregator.adapters.ScrapingException;
import com.quantbro.aggregator.adapters.SignalProviderAdapter;
import com.quantbro.aggregator.adapters.SignalProviderName;

@RunWith(SpringRunner.class)
public final class JobServiceUnitTest {

	private static final SignalProviderName SIGNAL_PROVIDER_FOR_TESTING = SignalProviderName.FXLEADERS;

	@Mock
	private SignalProviderAdapter adapter;

	@Mock
	private SignalService service;

	@Mock
	private ApplicationContext context;

	@InjectMocks
	private JobService manager;

	@Test
	public void jobFails() {
		when(adapter.getSignals()).thenThrow(new ScrapingException("error message"));

		Assert.fail();
		// manager.updateForSingleAdapter(adapter);
		// final JobInformation jobInformation = manager.getScrapingJobInformation().get(SIGNAL_PROVIDER_FOR_TESTING);
		// assertEquals(JobStatus.ERROR, jobInformation.getJobStatus());
		// assertEquals("error message", jobInformation.getMessage());
	}

	@Test
	public void noSignalUpdateDuringFirstJobRun() {
		Assert.fail();
	}

	@Before
	public void setUp() {
		when(adapter.getName()).thenReturn(SIGNAL_PROVIDER_FOR_TESTING);

	}

	@SuppressWarnings("unchecked")
	@Test
	public void succesfulJob() {
		when(service.update(eq(adapter.getName()), anyList(), true, true)).thenReturn(new UpdateOperationResult());

		// manager.updateForSingleAdapter(adapter);

		// final JobInformation jobInformation = manager.getScrapingJobInformation().get(SIGNAL_PROVIDER_FOR_TESTING);
		// assertEquals(JobStatus.SUCCESFUL, jobInformation.getJobStatus());
	}
}
