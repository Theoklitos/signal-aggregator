package com.quantbro.aggregator.utils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.reflections.Reflections;

import com.quantbro.aggregator.adapters.AbstractSignalProviderAdapter;

public final class ReflectionUtils {

	public static List<String> getAllClassesInPackage(final String packageName) {
		final Reflections reflections = new Reflections(packageName);
		final Set<Class<? extends AbstractSignalProviderAdapter>> allClasses = reflections.getSubTypesOf(AbstractSignalProviderAdapter.class);
		return allClasses.stream().map(clazz -> {
			return clazz.getSimpleName();
		}).collect(Collectors.toList());
	}

}
