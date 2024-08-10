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
package io.polaris.core.persistence;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.dropwizard.jackson.Discoverable;
import io.polaris.core.context.RealmContext;
import io.polaris.core.monitor.PolarisMetricRegistry;
import io.polaris.core.storage.PolarisStorageIntegrationProvider;
import io.polaris.core.storage.cache.StorageCredentialCache;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Configuration interface for configuring the {@link PolarisMetaStoreManager} via Dropwizard
 * configuration
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
public interface MetaStoreManagerFactory extends Discoverable {

  PolarisMetaStoreManager getOrCreateMetaStoreManager(RealmContext realmContext);

  Supplier<PolarisMetaStoreSession> getOrCreateSessionSupplier(RealmContext realmContext);

  StorageCredentialCache getOrCreateStorageCredentialCache(RealmContext realmContext);

  void setStorageIntegrationProvider(PolarisStorageIntegrationProvider storageIntegrationProvider);

  void setMetricRegistry(PolarisMetricRegistry metricRegistry);

  Map<String, PolarisMetaStoreManager.PrincipalSecretsResult> bootstrapRealms(List<String> realms);
}
