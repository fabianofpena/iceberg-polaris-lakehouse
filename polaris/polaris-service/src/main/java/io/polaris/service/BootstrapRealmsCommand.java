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

import io.dropwizard.core.cli.ConfiguredCommand;
import io.dropwizard.core.setup.Bootstrap;
import io.polaris.core.PolarisConfigurationStore;
import io.polaris.core.persistence.MetaStoreManagerFactory;
import io.polaris.service.config.ConfigurationStoreAware;
import io.polaris.service.config.PolarisApplicationConfig;
import io.polaris.service.config.RealmEntityManagerFactory;
import io.polaris.service.context.CallContextResolver;
import net.sourceforge.argparse4j.inf.Namespace;

/**
 * Command for bootstrapping root level service principals for each realm. This command will invoke
 * a default implementation which generates random user id and secret. These credentials will be
 * printed out to the log and standard output (stdout).
 */
public class BootstrapRealmsCommand extends ConfiguredCommand<PolarisApplicationConfig> {
  public BootstrapRealmsCommand() {
    super("bootstrap", "bootstraps principal credentials for all realms and prints them to log");
  }

  @Override
  protected void run(
      Bootstrap<PolarisApplicationConfig> bootstrap,
      Namespace namespace,
      PolarisApplicationConfig configuration)
      throws Exception {
    MetaStoreManagerFactory metaStoreManagerFactory = configuration.getMetaStoreManagerFactory();

    PolarisConfigurationStore configurationStore = configuration.getConfigurationStore();
    if (metaStoreManagerFactory instanceof ConfigurationStoreAware) {
      ((ConfigurationStoreAware) metaStoreManagerFactory).setConfigurationStore(configurationStore);
    }
    RealmEntityManagerFactory entityManagerFactory =
        new RealmEntityManagerFactory(metaStoreManagerFactory);
    CallContextResolver callContextResolver = configuration.getCallContextResolver();
    callContextResolver.setEntityManagerFactory(entityManagerFactory);
    if (callContextResolver instanceof ConfigurationStoreAware csa) {
      csa.setConfigurationStore(configurationStore);
    }

    metaStoreManagerFactory.bootstrapRealms(configuration.getDefaultRealms());
  }
}
