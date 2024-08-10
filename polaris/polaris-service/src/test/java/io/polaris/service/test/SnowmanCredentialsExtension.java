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
package io.polaris.service.test;

import static io.polaris.service.context.DefaultContextResolver.REALM_PROPERTY_KEY;
import static org.assertj.core.api.Assertions.assertThat;

import io.dropwizard.testing.junit5.DropwizardAppExtension;
import io.polaris.core.admin.model.GrantPrincipalRoleRequest;
import io.polaris.core.admin.model.Principal;
import io.polaris.core.admin.model.PrincipalRole;
import io.polaris.core.admin.model.PrincipalWithCredentials;
import io.polaris.core.entity.PolarisPrincipalSecrets;
import io.polaris.service.auth.TokenUtils;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.slf4j.LoggerFactory;

public class SnowmanCredentialsExtension
    implements BeforeAllCallback, AfterAllCallback, ParameterResolver {

  private SnowmanCredentials snowmanCredentials;

  public record SnowmanCredentials(String clientId, String clientSecret) {}

  @Override
  public void beforeAll(ExtensionContext extensionContext) throws Exception {
    PolarisPrincipalSecrets adminSecrets = PolarisConnectionExtension.getAdminSecrets();
    String realm =
        extensionContext
            .getStore(Namespace.create(extensionContext.getRequiredTestClass()))
            .get(REALM_PROPERTY_KEY, String.class);

    if (adminSecrets == null) {
      LoggerFactory.getLogger(SnowmanCredentialsExtension.class)
          .atError()
          .log(
              "No admin secrets configured - you must also configure your test with PolarisConnectionExtension");
      return;
    }
    DropwizardAppExtension dropwizard =
        PolarisConnectionExtension.findDropwizardExtension(extensionContext);
    if (dropwizard == null) {
      return;
    }
    String userToken =
        TokenUtils.getTokenFromSecrets(
            dropwizard.client(),
            dropwizard.getLocalPort(),
            adminSecrets.getPrincipalClientId(),
            adminSecrets.getMainSecret(),
            realm);

    PrincipalRole principalRole = new PrincipalRole("catalog-admin");
    try (Response createPrResponse =
        dropwizard
            .client()
            .target(
                String.format(
                    "http://localhost:%d/api/management/v1/principal-roles",
                    dropwizard.getLocalPort()))
            .request("application/json")
            .header("Authorization", "Bearer " + userToken)
            .header(REALM_PROPERTY_KEY, realm)
            .post(Entity.json(principalRole))) {
      assertThat(createPrResponse)
          .returns(Response.Status.CREATED.getStatusCode(), Response::getStatus);
    }

    Principal principal = new Principal("snowman");

    try (Response createPResponse =
        dropwizard
            .client()
            .target(
                String.format(
                    "http://localhost:%d/api/management/v1/principals", dropwizard.getLocalPort()))
            .request("application/json")
            .header("Authorization", "Bearer " + userToken) // how is token getting used?
            .header(REALM_PROPERTY_KEY, realm)
            .post(Entity.json(principal))) {
      assertThat(createPResponse)
          .returns(Response.Status.CREATED.getStatusCode(), Response::getStatus);
      PrincipalWithCredentials snowmanWithCredentials =
          createPResponse.readEntity(PrincipalWithCredentials.class);
      try (Response rotateResp =
          dropwizard
              .client()
              .target(
                  String.format(
                      "http://localhost:%d/api/management/v1/principals/%s/rotate",
                      dropwizard.getLocalPort(), "snowman"))
              .request(MediaType.APPLICATION_JSON)
              .header(
                  "Authorization",
                  "Bearer "
                      + TokenUtils.getTokenFromSecrets(
                          dropwizard.client(),
                          dropwizard.getLocalPort(),
                          snowmanWithCredentials.getCredentials().getClientId(),
                          snowmanWithCredentials.getCredentials().getClientSecret(),
                          realm))
              .header(REALM_PROPERTY_KEY, realm)
              .post(Entity.json(snowmanWithCredentials))) {

        assertThat(rotateResp).returns(200, Response::getStatus);

        // Use the rotated credentials.
        snowmanWithCredentials = rotateResp.readEntity(PrincipalWithCredentials.class);
      }
      snowmanCredentials =
          new SnowmanCredentials(
              snowmanWithCredentials.getCredentials().getClientId(),
              snowmanWithCredentials.getCredentials().getClientSecret());
    }
    try (Response assignPrResponse =
        dropwizard
            .client()
            .target(
                String.format(
                    "http://localhost:%d/api/management/v1/principals/snowman/principal-roles",
                    dropwizard.getLocalPort()))
            .request("application/json")
            .header("Authorization", "Bearer " + userToken) // how is token getting used?
            .header(REALM_PROPERTY_KEY, realm)
            .put(Entity.json(new GrantPrincipalRoleRequest(principalRole)))) {
      assertThat(assignPrResponse)
          .returns(Response.Status.CREATED.getStatusCode(), Response::getStatus);
    }
  }

  @Override
  public void afterAll(ExtensionContext extensionContext) throws Exception {
    PolarisPrincipalSecrets adminSecrets = PolarisConnectionExtension.getAdminSecrets();
    String realm =
        extensionContext
            .getStore(Namespace.create(extensionContext.getRequiredTestClass()))
            .get(REALM_PROPERTY_KEY, String.class);

    if (adminSecrets == null) {
      LoggerFactory.getLogger(SnowmanCredentialsExtension.class)
          .atError()
          .log(
              "No admin secrets configured - you must also configure your test with PolarisConnectionExtension");
      return;
    }
    DropwizardAppExtension dropwizard =
        PolarisConnectionExtension.findDropwizardExtension(extensionContext);
    if (dropwizard == null) {
      return;
    }
    String userToken =
        TokenUtils.getTokenFromSecrets(
            dropwizard.client(),
            dropwizard.getLocalPort(),
            adminSecrets.getPrincipalClientId(),
            adminSecrets.getMainSecret(),
            realm);

    try (Response deletePrResponse =
        dropwizard
            .client()
            .target(
                String.format(
                    "http://localhost:%d/api/management/v1/principal-roles/%s",
                    dropwizard.getLocalPort(), "catalog-admin"))
            .request("application/json")
            .header("Authorization", "Bearer " + userToken)
            .header(REALM_PROPERTY_KEY, realm)
            .delete()) {}

    try (Response deleteResponse =
        dropwizard
            .client()
            .target(
                String.format(
                    "http://localhost:%d/api/management/v1/principals/%s",
                    dropwizard.getLocalPort(), "snowman"))
            .request("application/json")
            .header("Authorization", "Bearer " + userToken)
            .header(REALM_PROPERTY_KEY, realm)
            .delete()) {}
  }

  // FIXME - this would be better done with a Credentials-specific annotation processor so
  // tests could declare which credentials they want (e.g., @TestCredentials("root") )
  // For now, snowman comes from here and root comes from PolarisConnectionExtension

  @Override
  public boolean supportsParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {

    return parameterContext.getParameter().getType() == SnowmanCredentials.class;
  }

  @Override
  public Object resolveParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    return snowmanCredentials;
  }
}
