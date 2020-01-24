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

package com.netflix.spinnaker.gate.services.internal;

import com.google.common.collect.ImmutableMap;
import com.netflix.spinnaker.gate.config.DynamicRoutingConfigProperties;
import com.netflix.spinnaker.kork.dynamicconfig.DynamicConfigService;
import com.netflix.spinnaker.kork.web.selector.v2.SelectableService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import javax.annotation.Nullable;

public class ClouddriverV2ServiceSelector {
  private SelectableService<ClouddriverService> selectableService;
  //  private final ClouddriverService defaultService;
  private final Map<String, Object> defaultConfig;
  private final DynamicConfigService dynamicConfigService;

  public ClouddriverV2ServiceSelector(
      ClouddriverService defaultService,
      DynamicRoutingConfigProperties dynamicRoutingConfigProperties,
      Function<String, ClouddriverService> getClouddriverServiceByUrlFx,
      DynamicConfigService dynamicConfigService) {
    //    this.defaultService = defaultService;

    // TODO: what is this used for?
    this.defaultConfig = ImmutableMap.of(); // getDefaultConfig(clouddriverConfigProperties);

    // TODO: handle null getClouddriver()
    this.selectableService =
        new SelectableService<>(
            dynamicRoutingConfigProperties.getClouddriver().getBaseUrls(),
            defaultService,
            defaultConfig,
            getClouddriverServiceByUrlFx);

    this.dynamicConfigService = dynamicConfigService;
  }

  // user?
  // account?
  // random dial?
  public ClouddriverService select(
      @Nullable String sourceApp, @Nullable String destinationApp /* , String user? */) {
    if (!shouldSelect()) {
      return selectableService.getDefaultService().getService();
    }

    List<SelectableService.Parameter> parameters = new ArrayList<>();
    if (sourceApp != null) {
      parameters.add(
          new SelectableService.Parameter()
              .withName("sourceApp")
              .withValues(Collections.singletonList(sourceApp)));
    }

    if (destinationApp != null) {
      parameters.add(
          new SelectableService.Parameter()
              .withName("destinationApp")
              .withValues(Collections.singletonList(destinationApp)));
    }

    return selectableService.byParameters(parameters).getService();
  }

  private boolean shouldSelect() {
    // TODO: get these out of strings
    // TODO: change the name
    return dynamicConfigService.isEnabled(DynamicRoutingConfigProperties.ENABLED_PROPERTY, false)
        && dynamicConfigService.isEnabled(
            DynamicRoutingConfigProperties.ClouddriverConfigProperties.ENABLED_PROPERTY, false);
  }
}
