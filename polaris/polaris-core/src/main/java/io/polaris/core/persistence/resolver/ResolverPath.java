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
package io.polaris.core.persistence.resolver;

import com.google.common.collect.ImmutableList;
import io.polaris.core.entity.PolarisEntityType;
import java.util.List;

/** Simple class to represent a path within a catalog */
public class ResolverPath {

  // name of the entities in that path. The parent of the first named entity is the path is the
  // catalog
  private final List<String> entityNames;

  // all entities in a path are namespaces except the last one which can be  a table_like entity
  // versus a namespace
  private final PolarisEntityType lastEntityType;

  // true if this path is optional, i.e. failing to fully resolve it is not an error
  private final boolean isOptional;

  /**
   * Constructor for an optional path
   *
   * @param entityNames set of entity names, all are namespaces except the last one which is either
   *     a namespace or a table_like entity
   * @param lastEntityType type of the last entity, either namespace or table_like
   */
  public ResolverPath(List<String> entityNames, PolarisEntityType lastEntityType) {
    this(entityNames, lastEntityType, false);
  }

  /**
   * Constructor for an optional path
   *
   * @param entityNames set of entity names, all are namespaces except the last one which is either
   *     a namespace or a table_like entity
   * @param lastEntityType type of the last entity, either namespace or table_like
   * @param isOptional true if optional
   */
  public ResolverPath(
      List<String> entityNames, PolarisEntityType lastEntityType, boolean isOptional) {
    this.entityNames = ImmutableList.copyOf(entityNames);
    this.lastEntityType = lastEntityType;
    this.isOptional = isOptional;
  }

  public List<String> getEntityNames() {
    return entityNames;
  }

  public PolarisEntityType getLastEntityType() {
    return lastEntityType;
  }

  public boolean isOptional() {
    return isOptional;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("entityNames:");
    sb.append(entityNames.toString());
    sb.append(";lastEntityType:");
    sb.append(lastEntityType.toString());
    sb.append(";isOptional:");
    sb.append(isOptional);
    return sb.toString();
  }
}
