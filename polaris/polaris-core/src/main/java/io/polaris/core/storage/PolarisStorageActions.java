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
package io.polaris.core.storage;

public enum PolarisStorageActions {
  READ,
  WRITE,
  LIST,
  DELETE,
  ALL,
  ;

  /** check if the provided string is a valid action. */
  public static boolean isValidAction(String s) {
    for (PolarisStorageActions action : PolarisStorageActions.values()) {
      if (action.name().equalsIgnoreCase(s)) {
        return true;
      }
    }
    return false;
  }
}
