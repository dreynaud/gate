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

package com.netflix.spinnaker.gate.services

import com.netflix.spinnaker.gate.config.DynamicRoutingConfigProperties
import com.netflix.spinnaker.gate.services.internal.ClouddriverService
import com.netflix.spinnaker.gate.services.internal.ClouddriverV2ServiceSelector
import com.netflix.spinnaker.kork.dynamicconfig.DynamicConfigService
import com.netflix.spinnaker.kork.web.selector.v2.SelectableService
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

import java.util.function.Function

import static com.netflix.spinnaker.kork.web.selector.v2.SelectableService.Parameter


class ClouddriverV2SelectorSpec extends Specification {
  String mainUrl = "clouddriver-main.region.cloud.com"
  String deckUrl = "clouddriver-deck.region.cloud.com"
  String appUrl = "clouddriver-app.region.cloud.com"
  String eggUrl = "clouddriver-egg.region.cloud.com"
  String comboUrl = "clouddriver-combo.region.cloud.com"
  String comboWomboUrl = "clouddriver-combowombo.region.cloud.com"

//  the backing SelectableService will sort these by ascending priority
//  and then return the first service that matches ALL of the input parameters
  def mainBaseUrl = baseUrl(mainUrl, 4, [])
  def deckBaseUrl = baseUrl(deckUrl, 3,
    [new Parameter("sourceApp", ["deck"])])

  def appBaseUrl = baseUrl(appUrl, 2,
    [new Parameter("destinationApp", ["egg", "chicken"])])

  def eggBaseUrl = baseUrl(eggUrl, 1,
    [new Parameter("destinationApp", ["egg"])])

//   needs higher priority than eggBaseUrl, otherwise anything with "app=egg" will match eggBaseUrl first and not this
  def comboBaseUrl = baseUrl(comboUrl, 0,
    [new Parameter("destinationApp", ["egg"]),
     new Parameter("sourceApp", ["api"])])

//   ClouddriverV2Selector does not have an interface for the "catchme" param
//   so we don't expect this is to ever show up in the output, despite having the highest priority
  def comboWomboBaseUrl = baseUrl(comboWomboUrl, -1,
    [new Parameter("destinationApp", ["egg"]),
     new Parameter("catchme", ["ifyoucan"])])

  @Shared def defaultService = Mock(ClouddriverService)
  @Shared def mainService = Mock(ClouddriverService)
  @Shared def deckService = Mock(ClouddriverService)
  @Shared def appService = Mock(ClouddriverService)
  @Shared def eggService = Mock(ClouddriverService)
  @Shared def comboService = Mock(ClouddriverService)
  @Shared def comboWomboService = Mock(ClouddriverService)

  def clouddriverConfigProperties = Stub(DynamicRoutingConfigProperties.ClouddriverConfigProperties)
  def dynamicRoutingConfigProperties = Stub(DynamicRoutingConfigProperties) {
    getClouddriver() >> clouddriverConfigProperties
  }

  def urlToServiceMap = [
    (mainUrl): mainService,
    (deckUrl): deckService,
    (appUrl): appService,
    (eggUrl): eggService,
    (comboUrl): comboService,
    (comboWomboUrl): comboWomboService
  ]

  Function<String, ClouddriverService> getClouddriverServiceByUrlFx = {
    url -> urlToServiceMap[url]
  } as Function

  DynamicConfigService dynamicConfigService = Stub(DynamicConfigService)

  @Subject
  ClouddriverV2ServiceSelector selector

  def baseUrl(String baseUrl, int priority, List<SelectableService.Parameter> parameters) {
    def result = new SelectableService.BaseUrl()
    result.baseUrl = baseUrl
    result.priority = priority
    result.config = [:]
    result.parameters = parameters
    return result
  }

  @Unroll
  def "with crossRegionTraffic.enabled=#globalToggle and crossRegionTraffic.clouddriver.enabled=#clouddriverToggle we expect isEnabled=#isEnabled"() {
    when:
    clouddriverConfigProperties.getBaseUrls() >> [mainBaseUrl, deckBaseUrl]
    dynamicConfigService.isEnabled(DynamicRoutingConfigProperties.ENABLED_PROPERTY, false) >> globalToggle
    dynamicConfigService.isEnabled(DynamicRoutingConfigProperties.ClouddriverConfigProperties.ENABLED_PROPERTY, false) >> clouddriverToggle

    selector = new ClouddriverV2ServiceSelector(
      defaultService, dynamicRoutingConfigProperties, getClouddriverServiceByUrlFx, dynamicConfigService)

    then:
    selector.select("deck", null) == (isEnabled ? deckService : defaultService)

    where:
    globalToggle | clouddriverToggle || isEnabled
    false        | true              || false
    true         | false             || false
    true         | true              || true
  }

  @Unroll
  def "selecting on sourceApp=#sourceApp and destinationApp=#destinationApp should select #expectedService"() {
    given:
    clouddriverConfigProperties.getBaseUrls() >> [mainBaseUrl, deckBaseUrl, appBaseUrl, eggBaseUrl, comboBaseUrl, comboWomboBaseUrl]
    dynamicConfigService.isEnabled(DynamicRoutingConfigProperties.ENABLED_PROPERTY, false) >> true
    dynamicConfigService.isEnabled(DynamicRoutingConfigProperties.ClouddriverConfigProperties.ENABLED_PROPERTY, false) >> true

    selector = new ClouddriverV2ServiceSelector(
      defaultService, dynamicRoutingConfigProperties, getClouddriverServiceByUrlFx, dynamicConfigService)

    expect:
    selector.select(sourceApp, destinationApp) == expectedService

    where:
    sourceApp | destinationApp || expectedService
    null      | null           || defaultService // only matches empty parameters
    "deck"    | null           || deckService
    "deck"    | "someapp"      || deckService    // we still get deckService if we pass extra parameters
    null      | "egg"          || eggService
    "deck"    | "egg"          || eggService     // matches both deck and egg, uses priority to pick egg
    null      | "chicken"      || appService
    "api"     | "someapp"      || mainService    // mainService will match any non-empty parameters
    "api"     | "egg"          || comboService
  }
}
