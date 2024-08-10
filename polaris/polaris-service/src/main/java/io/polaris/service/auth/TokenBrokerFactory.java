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
package io.polaris.service.auth;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.dropwizard.jackson.Discoverable;
import io.polaris.core.context.RealmContext;
import java.util.function.Function;

/**
 * Factory that creates a {@link TokenBroker} for generating and parsing. The {@link TokenBroker} is
 * created based on the realm context.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface TokenBrokerFactory extends Function<RealmContext, TokenBroker>, Discoverable {}
