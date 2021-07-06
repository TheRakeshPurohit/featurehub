package io.featurehub.health

import cd.connect.jersey.common.LoggingConfiguration
import cd.connect.jersey.prometheus.PrometheusDynamicFeature
import cd.connect.openapi.support.ReturnStatusContainerResponseFilter
import io.featurehub.jersey.config.CommonConfiguration
import org.glassfish.jersey.server.ServerProperties
import javax.ws.rs.core.Feature
import javax.ws.rs.core.FeatureContext

class CommonFeatureHubFeatures : Feature {
  override fun configure(context: FeatureContext): Boolean {
    context.property(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true)

    arrayOf(
      CommonConfiguration::class.java,
      LoggingConfiguration::class.java,
      ReturnStatusContainerResponseFilter::class.java,
      PrometheusDynamicFeature::class.java
    ).forEach { context.register(it) }

    return true
  }
}
