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
package io.polaris.service.storage;

import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.auth.http.HttpTransportFactory;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.ServiceOptions;
import io.polaris.core.PolarisDiagnostics;
import io.polaris.core.storage.PolarisCredentialProperty;
import io.polaris.core.storage.PolarisStorageActions;
import io.polaris.core.storage.PolarisStorageConfigurationInfo;
import io.polaris.core.storage.PolarisStorageIntegration;
import io.polaris.core.storage.PolarisStorageIntegrationProvider;
import io.polaris.core.storage.aws.AwsCredentialsStorageIntegration;
import io.polaris.core.storage.azure.AzureCredentialsStorageIntegration;
import io.polaris.core.storage.gcp.GcpCredentialsStorageIntegration;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.amazon.awssdk.services.sts.StsClient;

public class PolarisStorageIntegrationProviderImpl implements PolarisStorageIntegrationProvider {

  private final Supplier<StsClient> stsClientSupplier;

  public PolarisStorageIntegrationProviderImpl(Supplier<StsClient> stsClientSupplier) {
    this.stsClientSupplier = stsClientSupplier;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends PolarisStorageConfigurationInfo> @Nullable
      PolarisStorageIntegration<T> getStorageIntegrationForConfig(
          PolarisStorageConfigurationInfo polarisStorageConfigurationInfo) {
    if (polarisStorageConfigurationInfo == null) {
      return null;
    }
    PolarisStorageIntegration<T> storageIntegration;
    switch (polarisStorageConfigurationInfo.getStorageType()) {
      case S3:
        storageIntegration =
            (PolarisStorageIntegration<T>)
                new AwsCredentialsStorageIntegration(stsClientSupplier.get());
        break;
      case GCS:
        try {
          storageIntegration =
              (PolarisStorageIntegration<T>)
                  new GcpCredentialsStorageIntegration(
                      GoogleCredentials.getApplicationDefault(),
                      ServiceOptions.getFromServiceLoader(
                          HttpTransportFactory.class, NetHttpTransport::new));
        } catch (IOException e) {
          throw new RuntimeException(
              "Error initializing default google credentials" + e.getMessage());
        }
        break;
      case AZURE:
        storageIntegration =
            (PolarisStorageIntegration<T>) new AzureCredentialsStorageIntegration();
        break;
      case FILE:
        storageIntegration =
            new PolarisStorageIntegration<T>("file") {
              @Override
              public EnumMap<PolarisCredentialProperty, String> getSubscopedCreds(
                  @NotNull PolarisDiagnostics diagnostics,
                  @NotNull T storageConfig,
                  boolean allowListOperation,
                  @NotNull Set<String> allowedReadLocations,
                  @NotNull Set<String> allowedWriteLocations) {
                return new EnumMap<>(PolarisCredentialProperty.class);
              }

              @Override
              public EnumMap<PolarisStorageConfigurationInfo.DescribeProperty, String>
                  descPolarisStorageConfiguration(
                      @NotNull PolarisStorageConfigurationInfo storageConfigInfo) {
                return new EnumMap<>(PolarisStorageConfigurationInfo.DescribeProperty.class);
              }

              @Override
              public @NotNull Map<String, Map<PolarisStorageActions, ValidationResult>>
                  validateAccessToLocations(
                      @NotNull T storageConfig,
                      @NotNull Set<PolarisStorageActions> actions,
                      @NotNull Set<String> locations) {
                return Map.of();
              }
            };
        break;
      default:
        throw new IllegalArgumentException(
            "Unknown storage type " + polarisStorageConfigurationInfo.getStorageType());
    }
    return storageIntegration;
  }
}
