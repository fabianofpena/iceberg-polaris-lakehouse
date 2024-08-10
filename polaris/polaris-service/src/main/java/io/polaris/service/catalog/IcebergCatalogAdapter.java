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
package io.polaris.service.catalog;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import io.polaris.core.auth.AuthenticatedPolarisPrincipal;
import io.polaris.core.auth.PolarisAuthorizer;
import io.polaris.core.context.CallContext;
import io.polaris.core.context.RealmContext;
import io.polaris.core.entity.PolarisEntity;
import io.polaris.core.persistence.PolarisEntityManager;
import io.polaris.core.persistence.cache.EntityCacheEntry;
import io.polaris.core.persistence.resolver.Resolver;
import io.polaris.core.persistence.resolver.ResolverStatus;
import io.polaris.service.catalog.api.IcebergRestCatalogApiService;
import io.polaris.service.catalog.api.IcebergRestConfigurationApiService;
import io.polaris.service.config.RealmEntityManagerFactory;
import io.polaris.service.context.CallContextCatalogFactory;
import io.polaris.service.types.CommitTableRequest;
import io.polaris.service.types.CommitViewRequest;
import io.polaris.service.types.NotificationRequest;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.iceberg.UpdateRequirement;
import org.apache.iceberg.catalog.Catalog;
import org.apache.iceberg.catalog.Namespace;
import org.apache.iceberg.catalog.TableIdentifier;
import org.apache.iceberg.exceptions.BadRequestException;
import org.apache.iceberg.exceptions.NotAuthorizedException;
import org.apache.iceberg.exceptions.NotFoundException;
import org.apache.iceberg.rest.RESTUtil;
import org.apache.iceberg.rest.requests.CommitTransactionRequest;
import org.apache.iceberg.rest.requests.CreateNamespaceRequest;
import org.apache.iceberg.rest.requests.CreateTableRequest;
import org.apache.iceberg.rest.requests.CreateViewRequest;
import org.apache.iceberg.rest.requests.RegisterTableRequest;
import org.apache.iceberg.rest.requests.RenameTableRequest;
import org.apache.iceberg.rest.requests.ReportMetricsRequest;
import org.apache.iceberg.rest.requests.UpdateNamespacePropertiesRequest;
import org.apache.iceberg.rest.requests.UpdateTableRequest;
import org.apache.iceberg.rest.responses.ConfigResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link IcebergRestCatalogApiService} implementation that delegates operations to {@link
 * org.apache.iceberg.rest.CatalogHandlers} after finding the appropriate {@link Catalog} for the
 * current {@link RealmContext}.
 */
public class IcebergCatalogAdapter
    implements IcebergRestCatalogApiService, IcebergRestConfigurationApiService {
  private static final Logger LOG = LoggerFactory.getLogger(IcebergCatalogAdapter.class);

  private final CallContextCatalogFactory catalogFactory;
  private final RealmEntityManagerFactory entityManagerFactory;
  private PolarisAuthorizer polarisAuthorizer;

  public IcebergCatalogAdapter(
      CallContextCatalogFactory catalogFactory,
      RealmEntityManagerFactory entityManagerFactory,
      PolarisAuthorizer polarisAuthorizer) {
    this.catalogFactory = catalogFactory;
    this.entityManagerFactory = entityManagerFactory;
    this.polarisAuthorizer = polarisAuthorizer;
  }

  private PolarisCatalogHandlerWrapper newHandlerWrapper(
      SecurityContext securityContext, String catalogName) {
    CallContext callContext = CallContext.getCurrentContext();
    AuthenticatedPolarisPrincipal authenticatedPrincipal =
        (AuthenticatedPolarisPrincipal) securityContext.getUserPrincipal();
    if (authenticatedPrincipal == null) {
      throw new NotAuthorizedException("Failed to find authenticatedPrincipal in SecurityContext");
    }

    PolarisEntityManager entityManager =
        entityManagerFactory.getOrCreateEntityManager(callContext.getRealmContext());

    return new PolarisCatalogHandlerWrapper(
        callContext,
        entityManager,
        authenticatedPrincipal,
        catalogFactory,
        catalogName,
        polarisAuthorizer);
  }

  @Override
  public Response createNamespace(
      String prefix,
      CreateNamespaceRequest createNamespaceRequest,
      SecurityContext securityContext) {
    return Response.ok(
            newHandlerWrapper(securityContext, prefix).createNamespace(createNamespaceRequest))
        .build();
  }

  @Override
  public Response listNamespaces(
      String prefix,
      String pageToken,
      Integer pageSize,
      String parent,
      SecurityContext securityContext) {
    Optional<Namespace> namespaceOptional =
        Optional.ofNullable(parent).map(IcebergCatalogAdapter::decodeNamespace);
    return Response.ok(
            newHandlerWrapper(securityContext, prefix)
                .listNamespaces(namespaceOptional.orElse(Namespace.of())))
        .build();
  }

  @Override
  public Response loadNamespaceMetadata(
      String prefix, String namespace, SecurityContext securityContext) {
    Namespace ns = decodeNamespace(namespace);
    return Response.ok(newHandlerWrapper(securityContext, prefix).loadNamespaceMetadata(ns))
        .build();
  }

  private static Namespace decodeNamespace(String namespace) {
    return RESTUtil.decodeNamespace(URLEncoder.encode(namespace, Charset.defaultCharset()));
  }

  @Override
  public Response namespaceExists(
      String prefix, String namespace, SecurityContext securityContext) {
    Namespace ns = decodeNamespace(namespace);
    newHandlerWrapper(securityContext, prefix).namespaceExists(ns);
    return Response.ok().build();
  }

  @Override
  public Response dropNamespace(String prefix, String namespace, SecurityContext securityContext) {
    Namespace ns = decodeNamespace(namespace);
    newHandlerWrapper(securityContext, prefix).dropNamespace(ns);
    return Response.ok(Response.Status.NO_CONTENT).build();
  }

  @Override
  public Response updateProperties(
      String prefix,
      String namespace,
      UpdateNamespacePropertiesRequest updateNamespacePropertiesRequest,
      SecurityContext securityContext) {
    Namespace ns = decodeNamespace(namespace);
    return Response.ok(
            newHandlerWrapper(securityContext, prefix)
                .updateNamespaceProperties(ns, updateNamespacePropertiesRequest))
        .build();
  }

  @Override
  public Response createTable(
      String prefix,
      String namespace,
      CreateTableRequest createTableRequest,
      String xIcebergAccessDelegation,
      SecurityContext securityContext) {
    Namespace ns = decodeNamespace(namespace);
    if (createTableRequest.stageCreate()) {
      if (Strings.isNullOrEmpty(xIcebergAccessDelegation)) {
        return Response.ok(
                newHandlerWrapper(securityContext, prefix)
                    .createTableStaged(ns, createTableRequest))
            .build();
      } else {
        return Response.ok(
                newHandlerWrapper(securityContext, prefix)
                    .createTableStagedWithWriteDelegation(
                        ns, createTableRequest, xIcebergAccessDelegation))
            .build();
      }
    } else if (Strings.isNullOrEmpty(xIcebergAccessDelegation)) {
      return Response.ok(
              newHandlerWrapper(securityContext, prefix).createTableDirect(ns, createTableRequest))
          .build();
    } else {
      return Response.ok(
              newHandlerWrapper(securityContext, prefix)
                  .createTableDirectWithWriteDelegation(ns, createTableRequest))
          .build();
    }
  }

  @Override
  public Response listTables(
      String prefix,
      String namespace,
      String pageToken,
      Integer pageSize,
      SecurityContext securityContext) {
    Namespace ns = decodeNamespace(namespace);
    return Response.ok(newHandlerWrapper(securityContext, prefix).listTables(ns)).build();
  }

  @Override
  public Response loadTable(
      String prefix,
      String namespace,
      String table,
      String xIcebergAccessDelegation,
      String snapshots,
      SecurityContext securityContext) {
    Namespace ns = decodeNamespace(namespace);
    TableIdentifier tableIdentifier = TableIdentifier.of(ns, RESTUtil.decodeString(table));
    if (Strings.isNullOrEmpty(xIcebergAccessDelegation)) {
      return Response.ok(
              newHandlerWrapper(securityContext, prefix).loadTable(tableIdentifier, snapshots))
          .build();
    } else {
      return Response.ok(
              newHandlerWrapper(securityContext, prefix)
                  .loadTableWithAccessDelegation(
                      tableIdentifier, xIcebergAccessDelegation, snapshots))
          .build();
    }
  }

  @Override
  public Response tableExists(
      String prefix, String namespace, String table, SecurityContext securityContext) {
    Namespace ns = decodeNamespace(namespace);
    TableIdentifier tableIdentifier = TableIdentifier.of(ns, RESTUtil.decodeString(table));
    newHandlerWrapper(securityContext, prefix).tableExists(tableIdentifier);
    return Response.ok().build();
  }

  @Override
  public Response dropTable(
      String prefix,
      String namespace,
      String table,
      Boolean purgeRequested,
      SecurityContext securityContext) {
    Namespace ns = decodeNamespace(namespace);
    TableIdentifier tableIdentifier = TableIdentifier.of(ns, RESTUtil.decodeString(table));

    if (purgeRequested != null && purgeRequested.booleanValue()) {
      newHandlerWrapper(securityContext, prefix).dropTableWithPurge(tableIdentifier);
    } else {
      newHandlerWrapper(securityContext, prefix).dropTableWithoutPurge(tableIdentifier);
    }
    return Response.ok(Response.Status.NO_CONTENT).build();
  }

  @Override
  public Response registerTable(
      String prefix,
      String namespace,
      RegisterTableRequest registerTableRequest,
      SecurityContext securityContext) {
    Namespace ns = decodeNamespace(namespace);
    return Response.ok(
            newHandlerWrapper(securityContext, prefix).registerTable(ns, registerTableRequest))
        .build();
  }

  @Override
  public Response renameTable(
      String prefix, RenameTableRequest renameTableRequest, SecurityContext securityContext) {
    newHandlerWrapper(securityContext, prefix).renameTable(renameTableRequest);
    return Response.ok(javax.ws.rs.core.Response.Status.NO_CONTENT).build();
  }

  @Override
  public Response updateTable(
      String prefix,
      String namespace,
      String table,
      CommitTableRequest commitTableRequest,
      SecurityContext securityContext) {
    Namespace ns = decodeNamespace(namespace);
    TableIdentifier tableIdentifier = TableIdentifier.of(ns, RESTUtil.decodeString(table));

    if (isCreate(commitTableRequest)) {
      return Response.ok(
              newHandlerWrapper(securityContext, prefix)
                  .updateTableForStagedCreate(tableIdentifier, commitTableRequest))
          .build();
    } else {
      return Response.ok(
              newHandlerWrapper(securityContext, prefix)
                  .updateTable(tableIdentifier, commitTableRequest))
          .build();
    }
  }

  /**
   * TODO: Make the helper in org.apache.iceberg.rest.CatalogHandlers public instead of needing to
   * copy/pastehere.
   */
  private static boolean isCreate(UpdateTableRequest request) {
    boolean isCreate =
        request.requirements().stream()
            .anyMatch(UpdateRequirement.AssertTableDoesNotExist.class::isInstance);

    if (isCreate) {
      List<UpdateRequirement> invalidRequirements =
          request.requirements().stream()
              .filter(req -> !(req instanceof UpdateRequirement.AssertTableDoesNotExist))
              .collect(Collectors.toList());
      Preconditions.checkArgument(
          invalidRequirements.isEmpty(), "Invalid create requirements: %s", invalidRequirements);
    }

    return isCreate;
  }

  @Override
  public Response createView(
      String prefix,
      String namespace,
      CreateViewRequest createViewRequest,
      SecurityContext securityContext) {
    Namespace ns = decodeNamespace(namespace);
    return Response.ok(newHandlerWrapper(securityContext, prefix).createView(ns, createViewRequest))
        .build();
  }

  @Override
  public Response listViews(
      String prefix,
      String namespace,
      String pageToken,
      Integer pageSize,
      SecurityContext securityContext) {
    Namespace ns = decodeNamespace(namespace);
    return Response.ok(newHandlerWrapper(securityContext, prefix).listViews(ns)).build();
  }

  @Override
  public Response loadView(
      String prefix, String namespace, String view, SecurityContext securityContext) {
    Namespace ns = decodeNamespace(namespace);
    TableIdentifier tableIdentifier = TableIdentifier.of(ns, RESTUtil.decodeString(view));
    return Response.ok(newHandlerWrapper(securityContext, prefix).loadView(tableIdentifier))
        .build();
  }

  @Override
  public Response viewExists(
      String prefix, String namespace, String view, SecurityContext securityContext) {
    Namespace ns = decodeNamespace(namespace);
    TableIdentifier tableIdentifier = TableIdentifier.of(ns, RESTUtil.decodeString(view));
    newHandlerWrapper(securityContext, prefix).viewExists(tableIdentifier);
    return Response.ok().build();
  }

  @Override
  public Response dropView(
      String prefix, String namespace, String view, SecurityContext securityContext) {
    Namespace ns = decodeNamespace(namespace);
    TableIdentifier tableIdentifier = TableIdentifier.of(ns, RESTUtil.decodeString(view));
    newHandlerWrapper(securityContext, prefix).dropView(tableIdentifier);
    return Response.ok(Response.Status.NO_CONTENT).build();
  }

  @Override
  public Response renameView(
      String prefix, RenameTableRequest renameTableRequest, SecurityContext securityContext) {
    newHandlerWrapper(securityContext, prefix).renameView(renameTableRequest);
    return Response.ok(Response.Status.NO_CONTENT).build();
  }

  @Override
  public Response replaceView(
      String prefix,
      String namespace,
      String view,
      CommitViewRequest commitViewRequest,
      SecurityContext securityContext) {
    Namespace ns = decodeNamespace(namespace);
    TableIdentifier tableIdentifier = TableIdentifier.of(ns, RESTUtil.decodeString(view));
    return Response.ok(
            newHandlerWrapper(securityContext, prefix)
                .replaceView(tableIdentifier, commitViewRequest))
        .build();
  }

  @Override
  public Response commitTransaction(
      String prefix,
      CommitTransactionRequest commitTransactionRequest,
      SecurityContext securityContext) {
    newHandlerWrapper(securityContext, prefix).commitTransaction(commitTransactionRequest);
    return Response.status(Response.Status.NO_CONTENT).build();
  }

  @Override
  public Response reportMetrics(
      String prefix,
      String namespace,
      String table,
      ReportMetricsRequest reportMetricsRequest,
      SecurityContext securityContext) {
    return Response.status(Response.Status.NO_CONTENT).build();
  }

  @Override
  public Response sendNotification(
      String prefix,
      String namespace,
      String table,
      NotificationRequest notificationRequest,
      SecurityContext securityContext) {
    Namespace ns = decodeNamespace(namespace);
    TableIdentifier tableIdentifier = TableIdentifier.of(ns, RESTUtil.decodeString(table));
    newHandlerWrapper(securityContext, prefix)
        .sendNotification(tableIdentifier, notificationRequest);
    return Response.status(Response.Status.NO_CONTENT).build();
  }

  /** From IcebergRestConfigurationApiService. */
  @Override
  public Response getConfig(String warehouse, SecurityContext securityContext) {
    // 'warehouse' as an input here is catalogName.
    // 'warehouse' as an output will be treated by the client as a default catalog
    // storage
    //    base location.
    // 'prefix' as an output is the REST subpath that routes to the catalog
    // resource,
    //    which may be URL-escaped catalogName or potentially a different unique
    // identifier for
    //    the catalog being accessed.
    // TODO: Push this down into PolarisCatalogHandlerWrapper for authorizing "any" catalog
    // role in this catalog.
    PolarisEntityManager entityManager =
        entityManagerFactory.getOrCreateEntityManager(
            CallContext.getCurrentContext().getRealmContext());
    AuthenticatedPolarisPrincipal authenticatedPrincipal =
        (AuthenticatedPolarisPrincipal) securityContext.getUserPrincipal();
    if (authenticatedPrincipal == null) {
      throw new NotAuthorizedException("Failed to find authenticatedPrincipal in SecurityContext");
    }
    if (warehouse == null) {
      throw new BadRequestException("Please specify a warehouse");
    }
    Resolver resolver =
        entityManager.prepareResolver(
            CallContext.getCurrentContext(), authenticatedPrincipal, warehouse);
    ResolverStatus resolverStatus = resolver.resolveAll();
    if (!resolverStatus.getStatus().equals(ResolverStatus.StatusEnum.SUCCESS)) {
      throw new NotFoundException("Unable to find warehouse " + warehouse);
    }
    EntityCacheEntry resolvedReferenceCatalog = resolver.getResolvedReferenceCatalog();
    Map<String, String> properties =
        PolarisEntity.of(resolvedReferenceCatalog.getEntity()).getPropertiesAsMap();

    return Response.ok(
            ConfigResponse.builder()
                .withDefaults(properties) // catalog properties are defaults
                .withOverrides(ImmutableMap.of("prefix", warehouse))
                .build())
        .build();
  }
}
