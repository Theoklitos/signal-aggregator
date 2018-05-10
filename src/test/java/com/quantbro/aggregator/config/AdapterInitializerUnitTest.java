package com.quantbro.aggregator.config;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class AdapterInitializerUnitTest {

	@Test
	public void adapterThatHasNoConfigIsNotInitialized() {
		// when(context.getBean(anyString(), eq(SignalProviderAdapter.class))).thenThrow(new FatalBeanException("", new AdapterInitializationException(null)));
		// assertEquals(0, manager.initializeAndGetAdapters().size());
	}

	@Test
	public void managerShouldStartOneJobPerAdapter() {
		Assert.fail();
		// when(context.getBean(anyString(), eq(SignalProviderAdapter.class))).thenReturn(adapter);

		// final int adaptersInitialized = manager.initializeAndGetAdapters().size();

		// verify(context, times(adaptersInitialized)).getBean(anyString(), eq(SignalProviderAdapter.class));
	}

}
