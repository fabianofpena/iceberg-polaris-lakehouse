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
package io.polaris.service.admin;

import io.polaris.core.PolarisCallContext;
import io.polaris.core.admin.model.AddGrantRequest;
import io.polaris.core.admin.model.Catalog;
import io.polaris.core.admin.model.CatalogGrant;
import io.polaris.core.admin.model.CatalogRole;
import io.polaris.core.admin.model.CatalogRoles;
import io.polaris.core.admin.model.Catalogs;
import io.polaris.core.admin.model.CreateCatalogRequest;
import io.polaris.core.admin.model.CreateCatalogRoleRequest;
import io.polaris.core.admin.model.CreatePrincipalRequest;
import io.polaris.core.admin.model.CreatePrincipalRoleRequest;
import io.polaris.core.admin.model.GrantCatalogRoleRequest;
import io.polaris.core.admin.model.GrantPrincipalRoleRequest;
import io.polaris.core.admin.model.GrantResource;
import io.polaris.core.admin.model.GrantResources;
import io.polaris.core.admin.model.NamespaceGrant;
import io.polaris.core.admin.model.Principal;
import io.polaris.core.admin.model.PrincipalRole;
import io.polaris.core.admin.model.PrincipalRoles;
import io.polaris.core.admin.model.PrincipalWithCredentials;
import io.polaris.core.admin.model.Principals;
import io.polaris.core.admin.model.RevokeGrantRequest;
import io.polaris.core.admin.model.StorageConfigInfo;
import io.polaris.core.admin.model.TableGrant;
import io.polaris.core.admin.model.UpdateCatalogRequest;
import io.polaris.core.admin.model.UpdateCatalogRoleRequest;
import io.polaris.core.admin.model.UpdatePrincipalRequest;
import io.polaris.core.admin.model.UpdatePrincipalRoleRequest;
import io.polaris.core.admin.model.ViewGrant;
import io.polaris.core.auth.AuthenticatedPolarisPrincipal;
import io.polaris.core.auth.PolarisAuthorizer;
import io.polaris.core.context.CallContext;
import io.polaris.core.entity.CatalogEntity;
import io.polaris.core.entity.CatalogRoleEntity;
import io.polaris.core.entity.PolarisPrivilege;
import io.polaris.core.entity.PrincipalEntity;
import io.polaris.core.entity.PrincipalRoleEntity;
import io.polaris.core.persistence.PolarisEntityManager;
import io.polaris.service.admin.api.PolarisCatalogsApiService;
import io.polaris.service.admin.api.PolarisPrincipalRolesApiService;
import io.polaris.service.admin.api.PolarisPrincipalsApiService;
import io.polaris.service.config.RealmEntityManagerFactory;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import java.util.List;
import org.apache.iceberg.catalog.Namespace;
import org.apache.iceberg.catalog.TableIdentifier;
import org.apache.iceberg.exceptions.NotAuthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Concrete implementation of the Polaris API services */
public class PolarisServiceImpl
    implements PolarisCatalogsApiService,
        PolarisPrincipalsApiService,
        PolarisPrincipalRolesApiService {
  private static final Logger LOG = LoggerFactory.getLogger(PolarisServiceImpl.class);
  private final RealmEntityManagerFactory entityManagerFactory;
  private final PolarisAuthorizer polarisAuthorizer;

  public PolarisServiceImpl(
      RealmEntityManagerFactory entityManagerFactory, PolarisAuthorizer polarisAuthorizer) {
    this.entityManagerFactory = entityManagerFactory;
    this.polarisAuthorizer = polarisAuthorizer;
  }

  private PolarisAdminService newAdminService(SecurityContext securityContext) {
    CallContext callContext = CallContext.getCurrentContext();
    AuthenticatedPolarisPrincipal authenticatedPrincipal =
        (AuthenticatedPolarisPrincipal) securityContext.getUserPrincipal();
    if (authenticatedPrincipal == null) {
      throw new NotAuthorizedException("Failed to find authenticatedPrincipal in SecurityContext");
    }

    PolarisEntityManager entityManager =
        entityManagerFactory.getOrCreateEntityManager(callContext.getRealmContext());
    return new PolarisAdminService(
        callContext, entityManager, authenticatedPrincipal, polarisAuthorizer);
  }

  /** From PolarisCatalogsApiService */
  @Override
  public Response createCatalog(CreateCatalogRequest request, SecurityContext securityContext) {
    PolarisAdminService adminService = newAdminService(securityContext);
    Catalog catalog = request.getCatalog();
    validateStorageConfig(catalog.getStorageConfigInfo());
    Catalog newCatalog =
        new CatalogEntity(adminService.createCatalog(CatalogEntity.fromCatalog(catalog)))
            .asCatalog();
    LOG.info("Created new catalog {}", newCatalog);
    return Response.status(Response.Status.CREATED).build();
  }

  private void validateStorageConfig(StorageConfigInfo storageConfigInfo) {
    CallContext callContext = CallContext.getCurrentContext();
    PolarisCallContext polarisCallContext = callContext.getPolarisCallContext();
    List<String> allowedStorageTypes =
        polarisCallContext
            .getConfigurationStore()
            .getConfiguration(
                polarisCallContext,
                "SUPPORTED_CATALOG_STORAGE_TYPES",
                List.of(
                    StorageConfigInfo.StorageTypeEnum.S3.name(),
                    StorageConfigInfo.StorageTypeEnum.AZURE.name(),
                    StorageConfigInfo.StorageTypeEnum.GCS.name(),
                    StorageConfigInfo.StorageTypeEnum.FILE.name()));
    if (!allowedStorageTypes.contains(storageConfigInfo.getStorageType().name())) {
      LOG.atWarn()
          .addKeyValue("storageConfig", storageConfigInfo)
          .log("Disallowed storage type in catalog");
      throw new IllegalArgumentException(
          "Unsupported storage type: " + storageConfigInfo.getStorageType());
    }
  }

  /** From PolarisCatalogsApiService */
  @Override
  public Response deleteCatalog(String catalogName, SecurityContext securityContext) {
    PolarisAdminService adminService = newAdminService(securityContext);
    adminService.deleteCatalog(catalogName);
    return Response.status(Response.Status.NO_CONTENT).build();
  }

  /** From PolarisCatalogsApiService */
  @Override
  public Response getCatalog(String catalogName, SecurityContext securityContext) {
    PolarisAdminService adminService = newAdminService(securityContext);
    return Response.ok(adminService.getCatalog(catalogName).asCatalog()).build();
  }

  /** From PolarisCatalogsApiService */
  @Override
  public Response updateCatalog(
      String catalogName, UpdateCatalogRequest updateRequest, SecurityContext securityContext) {
    PolarisAdminService adminService = newAdminService(securityContext);
    if (updateRequest.getStorageConfigInfo() != null) {
      validateStorageConfig(updateRequest.getStorageConfigInfo());
    }
    return Response.ok(adminService.updateCatalog(catalogName, updateRequest).asCatalog()).build();
  }

  /** From PolarisCatalogsApiService */
  @Override
  public Response listCatalogs(SecurityContext securityContext) {
    PolarisAdminService adminService = newAdminService(securityContext);
    List<Catalog> catalogList =
        adminService.listCatalogs().stream()
            .map(CatalogEntity::new)
            .map(CatalogEntity::asCatalog)
            .toList();
    Catalogs catalogs = new Catalogs(catalogList);
    LOG.debug("listCatalogs returning: {}", catalogs);
    return Response.ok(catalogs).build();
  }

  /** From PolarisPrincipalsApiService */
  @Override
  public Response createPrincipal(CreatePrincipalRequest request, SecurityContext securityContext) {
    PolarisAdminService adminService = newAdminService(securityContext);
    PrincipalEntity principal = PrincipalEntity.fromPrincipal(request.getPrincipal());
    if (Boolean.TRUE.equals(request.getCredentialRotationRequired())) {
      principal =
          new PrincipalEntity.Builder(principal).setCredentialRotationRequiredState().build();
    }
    PrincipalWithCredentials createdPrincipal = adminService.createPrincipal(principal);
    LOG.info("Created new principal {}", createdPrincipal);
    return Response.status(Response.Status.CREATED).entity(createdPrincipal).build();
  }

  /** From PolarisPrincipalsApiService */
  @Override
  public Response deletePrincipal(String principalName, SecurityContext securityContext) {
    PolarisAdminService adminService = newAdminService(securityContext);
    adminService.deletePrincipal(principalName);
    return Response.status(Response.Status.NO_CONTENT).build();
  }

  /** From PolarisPrincipalsApiService */
  @Override
  public Response getPrincipal(String principalName, SecurityContext securityContext) {
    PolarisAdminService adminService = newAdminService(securityContext);
    return Response.ok(adminService.getPrincipal(principalName).asPrincipal()).build();
  }

  /** From PolarisPrincipalsApiService */
  @Override
  public Response updatePrincipal(
      String principalName, UpdatePrincipalRequest updateRequest, SecurityContext securityContext) {
    PolarisAdminService adminService = newAdminService(securityContext);
    return Response.ok(adminService.updatePrincipal(principalName, updateRequest).asPrincipal())
        .build();
  }

  /** From PolarisPrincipalsApiService */
  @Override
  public Response rotateCredentials(String principalName, SecurityContext securityContext) {
    PolarisAdminService adminService = newAdminService(securityContext);
    return Response.ok(adminService.rotateCredentials(principalName)).build();
  }

  /** From PolarisPrincipalsApiService */
  @Override
  public Response listPrincipals(SecurityContext securityContext) {
    PolarisAdminService adminService = newAdminService(securityContext);
    List<Principal> principalList =
        adminService.listPrincipals().stream()
            .map(PrincipalEntity::new)
            .map(PrincipalEntity::asPrincipal)
            .toList();
    Principals principals = new Principals(principalList);
    LOG.debug("listPrincipals returning: {}", principals);
    return Response.ok(principals).build();
  }

  /** From PolarisPrincipalRolesApiService */
  @Override
  public Response createPrincipalRole(
      CreatePrincipalRoleRequest request, SecurityContext securityContext) {
    PolarisAdminService adminService = newAdminService(securityContext);
    PrincipalRole newPrincipalRole =
        new PrincipalRoleEntity(
                adminService.createPrincipalRole(
                    PrincipalRoleEntity.fromPrincipalRole(request.getPrincipalRole())))
            .asPrincipalRole();
    LOG.info("Created new principalRole {}", newPrincipalRole);
    return Response.status(Response.Status.CREATED).build();
  }

  /** From PolarisPrincipalRolesApiService */
  @Override
  public Response deletePrincipalRole(String principalRoleName, SecurityContext securityContext) {
    PolarisAdminService adminService = newAdminService(securityContext);
    adminService.deletePrincipalRole(principalRoleName);
    return Response.status(Response.Status.NO_CONTENT).build();
  }

  /** From PolarisPrincipalRolesApiService */
  @Override
  public Response getPrincipalRole(String principalRoleName, SecurityContext securityContext) {
    PolarisAdminService adminService = newAdminService(securityContext);
    return Response.ok(adminService.getPrincipalRole(principalRoleName).asPrincipalRole()).build();
  }

  /** From PolarisPrincipalRolesApiService */
  @Override
  public Response updatePrincipalRole(
      String principalRoleName,
      UpdatePrincipalRoleRequest updateRequest,
      SecurityContext securityContext) {
    PolarisAdminService adminService = newAdminService(securityContext);
    return Response.ok(
            adminService.updatePrincipalRole(principalRoleName, updateRequest).asPrincipalRole())
        .build();
  }

  /** From PolarisPrincipalRolesApiService */
  @Override
  public Response listPrincipalRoles(SecurityContext securityContext) {
    PolarisAdminService adminService = newAdminService(securityContext);
    List<PrincipalRole> principalRoleList =
        adminService.listPrincipalRoles().stream()
            .map(PrincipalRoleEntity::new)
            .map(PrincipalRoleEntity::asPrincipalRole)
            .toList();
    PrincipalRoles principalRoles = new PrincipalRoles(principalRoleList);
    LOG.debug("listPrincipalRoles returning: {}", principalRoles);
    return Response.ok(principalRoles).build();
  }

  /** From PolarisCatalogsApiService */
  @Override
  public Response createCatalogRole(
      String catalogName, CreateCatalogRoleRequest request, SecurityContext securityContext) {
    PolarisAdminService adminService = newAdminService(securityContext);
    CatalogRole newCatalogRole =
        new CatalogRoleEntity(
                adminService.createCatalogRole(
                    catalogName, CatalogRoleEntity.fromCatalogRole(request.getCatalogRole())))
            .asCatalogRole();
    LOG.info("Created new catalogRole {}", newCatalogRole);
    return Response.status(Response.Status.CREATED).build();
  }

  /** From PolarisCatalogsApiService */
  @Override
  public Response deleteCatalogRole(
      String catalogName, String catalogRoleName, SecurityContext securityContext) {
    PolarisAdminService adminService = newAdminService(securityContext);
    adminService.deleteCatalogRole(catalogName, catalogRoleName);
    return Response.status(Response.Status.NO_CONTENT).build();
  }

  /** From PolarisCatalogsApiService */
  @Override
  public Response getCatalogRole(
      String catalogName, String catalogRoleName, SecurityContext securityContext) {
    PolarisAdminService adminService = newAdminService(securityContext);
    return Response.ok(adminService.getCatalogRole(catalogName, catalogRoleName).asCatalogRole())
        .build();
  }

  /** From PolarisCatalogsApiService */
  @Override
  public Response updateCatalogRole(
      String catalogName,
      String catalogRoleName,
      UpdateCatalogRoleRequest updateRequest,
      SecurityContext securityContext) {
    PolarisAdminService adminService = newAdminService(securityContext);
    return Response.ok(
            adminService
                .updateCatalogRole(catalogName, catalogRoleName, updateRequest)
                .asCatalogRole())
        .build();
  }

  /** From PolarisCatalogsApiService */
  @Override
  public Response listCatalogRoles(String catalogName, SecurityContext securityContext) {
    PolarisAdminService adminService = newAdminService(securityContext);
    List<CatalogRole> catalogRoleList =
        adminService.listCatalogRoles(catalogName).stream()
            .map(CatalogRoleEntity::new)
            .map(CatalogRoleEntity::asCatalogRole)
            .toList();
    CatalogRoles catalogRoles = new CatalogRoles(catalogRoleList);
    LOG.debug("listCatalogRoles returning: {}", catalogRoles);
    return Response.ok(catalogRoles).build();
  }

  /** From PolarisPrincipalsApiService */
  @Override
  public Response assignPrincipalRole(
      String principalName, GrantPrincipalRoleRequest request, SecurityContext securityContext) {
    LOG.info(
        "Assigning principalRole {} to principal {}",
        request.getPrincipalRole().getName(),
        principalName);
    PolarisAdminService adminService = newAdminService(securityContext);
    adminService.assignPrincipalRole(principalName, request.getPrincipalRole().getName());
    return Response.status(Response.Status.CREATED).build();
  }

  /** From PolarisPrincipalsApiService */
  @Override
  public Response revokePrincipalRole(
      String principalName, String principalRoleName, SecurityContext securityContext) {
    LOG.info("Revoking principalRole {} from principal {}", principalRoleName, principalName);
    PolarisAdminService adminService = newAdminService(securityContext);
    adminService.revokePrincipalRole(principalName, principalRoleName);
    return Response.status(Response.Status.NO_CONTENT).build();
  }

  /** From PolarisPrincipalsApiService */
  @Override
  public Response listPrincipalRolesAssigned(
      String principalName, SecurityContext securityContext) {
    PolarisAdminService adminService = newAdminService(securityContext);
    List<PrincipalRole> principalRoleList =
        adminService.listPrincipalRolesAssigned(principalName).stream()
            .map(PrincipalRoleEntity::new)
            .map(PrincipalRoleEntity::asPrincipalRole)
            .toList();
    PrincipalRoles principalRoles = new PrincipalRoles(principalRoleList);
    LOG.debug("listPrincipalRolesAssigned returning: {}", principalRoles);
    return Response.ok(principalRoles).build();
  }

  /** From PolarisPrincipalRolesApiService */
  @Override
  public Response assignCatalogRoleToPrincipalRole(
      String principalRoleName,
      String catalogName,
      GrantCatalogRoleRequest request,
      SecurityContext securityContext) {
    LOG.info(
        "Assigning catalogRole {} in catalog {} to principalRole {}",
        request.getCatalogRole().getName(),
        catalogName,
        principalRoleName);
    PolarisAdminService adminService = newAdminService(securityContext);
    adminService.assignCatalogRoleToPrincipalRole(
        principalRoleName, catalogName, request.getCatalogRole().getName());
    return Response.status(Response.Status.CREATED).build();
  }

  /** From PolarisPrincipalRolesApiService */
  @Override
  public Response revokeCatalogRoleFromPrincipalRole(
      String principalRoleName,
      String catalogName,
      String catalogRoleName,
      SecurityContext securityContext) {
    LOG.info(
        "Revoking catalogRole {} in catalog {} from principalRole {}",
        catalogRoleName,
        catalogName,
        principalRoleName);
    PolarisAdminService adminService = newAdminService(securityContext);
    adminService.revokeCatalogRoleFromPrincipalRole(
        principalRoleName, catalogName, catalogRoleName);
    return Response.status(Response.Status.NO_CONTENT).build();
  }

  /** From PolarisPrincipalRolesApiService */
  @Override
  public Response listAssigneePrincipalsForPrincipalRole(
      String principalRoleName, SecurityContext securityContext) {
    PolarisAdminService adminService = newAdminService(securityContext);
    List<Principal> principalList =
        adminService.listAssigneePrincipalsForPrincipalRole(principalRoleName).stream()
            .map(PrincipalEntity::new)
            .map(PrincipalEntity::asPrincipal)
            .toList();
    Principals principals = new Principals(principalList);
    LOG.debug("listAssigneePrincipalsForPrincipalRole returning: {}", principals);
    return Response.ok(principals).build();
  }

  /** From PolarisPrincipalRolesApiService */
  @Override
  public Response listCatalogRolesForPrincipalRole(
      String principalRoleName, String catalogName, SecurityContext securityContext) {
    PolarisAdminService adminService = newAdminService(securityContext);
    List<CatalogRole> catalogRoleList =
        adminService.listCatalogRolesForPrincipalRole(principalRoleName, catalogName).stream()
            .map(CatalogRoleEntity::new)
            .map(CatalogRoleEntity::asCatalogRole)
            .toList();
    CatalogRoles catalogRoles = new CatalogRoles(catalogRoleList);
    LOG.debug("listCatalogRolesForPrincipalRole returning: {}", catalogRoles);
    return Response.ok(catalogRoles).build();
  }

  /** From PolarisCatalogsApiService */
  @Override
  public Response addGrantToCatalogRole(
      String catalogName,
      String catalogRoleName,
      AddGrantRequest grantRequest,
      SecurityContext securityContext) {
    LOG.info(
        "Adding grant {} to catalogRole {} in catalog {}",
        grantRequest,
        catalogRoleName,
        catalogName);
    PolarisAdminService adminService = newAdminService(securityContext);
    switch (grantRequest.getGrant()) {
        // The per-securable-type Privilege enums must be exact String match for a subset of all
        // PolarisPrivilege values.
      case ViewGrant viewGrant:
        {
          PolarisPrivilege privilege =
              PolarisPrivilege.valueOf(viewGrant.getPrivilege().toString());
          String viewName = viewGrant.getViewName();
          String[] namespaceParts = viewGrant.getNamespace().toArray(new String[0]);
          adminService.grantPrivilegeOnViewToRole(
              catalogName,
              catalogRoleName,
              TableIdentifier.of(Namespace.of(namespaceParts), viewName),
              privilege);
          break;
        }
      case TableGrant tableGrant:
        {
          PolarisPrivilege privilege =
              PolarisPrivilege.valueOf(tableGrant.getPrivilege().toString());
          String tableName = tableGrant.getTableName();
          String[] namespaceParts = tableGrant.getNamespace().toArray(new String[0]);
          adminService.grantPrivilegeOnTableToRole(
              catalogName,
              catalogRoleName,
              TableIdentifier.of(Namespace.of(namespaceParts), tableName),
              privilege);
          break;
        }
      case NamespaceGrant namespaceGrant:
        {
          PolarisPrivilege privilege =
              PolarisPrivilege.valueOf(namespaceGrant.getPrivilege().toString());
          String[] namespaceParts = namespaceGrant.getNamespace().toArray(new String[0]);
          adminService.grantPrivilegeOnNamespaceToRole(
              catalogName, catalogRoleName, Namespace.of(namespaceParts), privilege);
          break;
        }
      case CatalogGrant catalogGrant:
        {
          PolarisPrivilege privilege =
              PolarisPrivilege.valueOf(catalogGrant.getPrivilege().toString());
          adminService.grantPrivilegeOnCatalogToRole(catalogName, catalogRoleName, privilege);
          break;
        }
      default:
        LOG.atWarn()
            .addKeyValue("catalog", catalogName)
            .addKeyValue("role", catalogRoleName)
            .log("Don't know how to handle privilege grant: {}", grantRequest);
        return Response.status(Response.Status.BAD_REQUEST).build();
    }
    return Response.status(Response.Status.CREATED).build();
  }

  /** From PolarisCatalogsApiService */
  @Override
  public Response revokeGrantFromCatalogRole(
      String catalogName,
      String catalogRoleName,
      Boolean cascade,
      RevokeGrantRequest grantRequest,
      SecurityContext securityContext) {
    LOG.info(
        "Revoking grant {} from catalogRole {} in catalog {}",
        grantRequest,
        catalogRoleName,
        catalogName);
    if (cascade != null && cascade.booleanValue()) {
      LOG.warn("Tried to use unimplemented 'cascade' feature when revoking grants.");
      return Response.status(501).build(); // not implemented
    }

    PolarisAdminService adminService = newAdminService(securityContext);
    switch (grantRequest.getGrant()) {
        // The per-securable-type Privilege enums must be exact String match for a subset of all
        // PolarisPrivilege values.
      case ViewGrant viewGrant:
        {
          PolarisPrivilege privilege =
              PolarisPrivilege.valueOf(viewGrant.getPrivilege().toString());
          String viewName = viewGrant.getViewName();
          String[] namespaceParts = viewGrant.getNamespace().toArray(new String[0]);
          adminService.revokePrivilegeOnViewFromRole(
              catalogName,
              catalogRoleName,
              TableIdentifier.of(Namespace.of(namespaceParts), viewName),
              privilege);
          break;
        }
      case TableGrant tableGrant:
        {
          PolarisPrivilege privilege =
              PolarisPrivilege.valueOf(tableGrant.getPrivilege().toString());
          String tableName = tableGrant.getTableName();
          String[] namespaceParts = tableGrant.getNamespace().toArray(new String[0]);
          adminService.revokePrivilegeOnTableFromRole(
              catalogName,
              catalogRoleName,
              TableIdentifier.of(Namespace.of(namespaceParts), tableName),
              privilege);
          break;
        }
      case NamespaceGrant namespaceGrant:
        {
          PolarisPrivilege privilege =
              PolarisPrivilege.valueOf(namespaceGrant.getPrivilege().toString());
          String[] namespaceParts = namespaceGrant.getNamespace().toArray(new String[0]);
          adminService.revokePrivilegeOnNamespaceFromRole(
              catalogName, catalogRoleName, Namespace.of(namespaceParts), privilege);
          break;
        }
      case CatalogGrant catalogGrant:
        {
          PolarisPrivilege privilege =
              PolarisPrivilege.valueOf(catalogGrant.getPrivilege().toString());
          adminService.revokePrivilegeOnCatalogFromRole(catalogName, catalogRoleName, privilege);
          break;
        }
      default:
        LOG.atWarn()
            .addKeyValue("catalog", catalogName)
            .addKeyValue("role", catalogRoleName)
            .log("Don't know how to handle privilege revocation: {}", grantRequest);
        return Response.status(Response.Status.BAD_REQUEST).build();
    }
    return Response.status(Response.Status.CREATED).build();
  }

  /** From PolarisCatalogsApiService */
  @Override
  public Response listAssigneePrincipalRolesForCatalogRole(
      String catalogName, String catalogRoleName, SecurityContext securityContext) {
    PolarisAdminService adminService = newAdminService(securityContext);
    List<PrincipalRole> principalRoleList =
        adminService.listAssigneePrincipalRolesForCatalogRole(catalogName, catalogRoleName).stream()
            .map(PrincipalRoleEntity::new)
            .map(PrincipalRoleEntity::asPrincipalRole)
            .toList();
    PrincipalRoles principalRoles = new PrincipalRoles(principalRoleList);
    LOG.debug("listAssigneePrincipalRolesForCatalogRole returning: {}", principalRoles);
    return Response.ok(principalRoles).build();
  }

  /** From PolarisCatalogsApiService */
  @Override
  public Response listGrantsForCatalogRole(
      String catalogName, String catalogRoleName, SecurityContext securityContext) {
    PolarisAdminService adminService = newAdminService(securityContext);
    List<GrantResource> grantList =
        adminService.listGrantsForCatalogRole(catalogName, catalogRoleName);
    GrantResources grantResources = new GrantResources(grantList);
    return Response.ok(grantResources).build();
  }
}
