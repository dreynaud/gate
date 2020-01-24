/*
 * Copyright 2020 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.gate.config

import com.netflix.spinnaker.kork.web.selector.v2.SelectableService
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "dynamic-routing")
class DynamicRoutingConfigProperties {
  public static final String ENABLED_PROPERTY = "dynamic-routing.enabled"
  boolean enabled
  ClouddriverConfigProperties clouddriver

  static class ClouddriverConfigProperties {
    public static final String ENABLED_PROPERTY = "dynamic-routing.clouddriver.enabled"
    boolean enabled
    List<SelectableService.BaseUrl> baseUrls;
  }
}
