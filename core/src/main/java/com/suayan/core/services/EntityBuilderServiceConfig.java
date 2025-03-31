package com.suayan.core.services;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * This is practically a placeholder for now. org.osgi's Activate/Deactivate annotations
 * require a different method signature that pass in schema configs instead of a BundleContext.
 */
@ObjectClassDefinition(
	name = "CCUI EntityBuilder Configuration", 
	description = "Metadata for CCUI EntityBuilder"
)
public @interface EntityBuilderServiceConfig {
	@AttributeDefinition(name = "some.value", description = "Some Value", type = AttributeType.STRING)
	String getSomeValue() default "some value here";
}
