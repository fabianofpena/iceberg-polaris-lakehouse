/*
 * Copyright (c) 2024 Snowflake Computing Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.polaris.service;

import com.google.common.base.Stopwatch;
import io.polaris.core.context.CallContext;
import io.polaris.core.monitor.PolarisMetricRegistry;
import io.polaris.core.resource.TimedApi;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.ext.Provider;
import org.glassfish.jersey.server.monitoring.ApplicationEvent;
import org.glassfish.jersey.server.monitoring.ApplicationEventListener;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;

/**
 * An ApplicationEventListener that supports timing and error counting of Jersey resource methods
 * annotated by {@link TimedApi}. It uses the {@link PolarisMetricRegistry} for metric collection
 * and properly times the resource on success and increments the error counter on failure.
 */
@Provider
public class TimedApplicationEventListener implements ApplicationEventListener {

  // The PolarisMetricRegistry instance used for recording metrics and error counters.
  private final PolarisMetricRegistry polarisMetricRegistry;

  public TimedApplicationEventListener(PolarisMetricRegistry polarisMetricRegistry) {
    this.polarisMetricRegistry = polarisMetricRegistry;
  }

  @Override
  public void onEvent(ApplicationEvent event) {}

  @Override
  public RequestEventListener onRequest(RequestEvent event) {
    return new TimedRequestEventListener();
  }

  /**
   * A RequestEventListener implementation that handles timing of resource method execution and
   * increments error counters on failures. The lifetime of the listener is tied to a single HTTP
   * request.
   */
  private class TimedRequestEventListener implements RequestEventListener {
    private String metric;
    private Stopwatch sw;

    /** Handles various types of RequestEvents to start timing, stop timing, and record metrics. */
    @Override
    public void onEvent(RequestEvent event) {
      String realmId = CallContext.getCurrentContext().getRealmContext().getRealmIdentifier();
      if (event.getType() == RequestEvent.Type.RESOURCE_METHOD_START) {
        Method method =
            event.getUriInfo().getMatchedResourceMethod().getInvocable().getHandlingMethod();
        if (method.isAnnotationPresent(TimedApi.class)) {
          TimedApi timedApi = method.getAnnotation(TimedApi.class);
          metric = timedApi.value();
          sw = Stopwatch.createStarted();
          polarisMetricRegistry.incrementCounter(metric, realmId);
        }

      } else if (event.getType() == RequestEvent.Type.FINISHED && metric != null) {
        if (event.isSuccess()) {
          sw.stop();
          polarisMetricRegistry.recordTimer(metric, sw.elapsed(TimeUnit.MILLISECONDS), realmId);
        } else {
          int statusCode = event.getContainerResponse().getStatus();
          polarisMetricRegistry.incrementErrorCounter(metric, statusCode, realmId);
        }
      }
    }
  }
}
