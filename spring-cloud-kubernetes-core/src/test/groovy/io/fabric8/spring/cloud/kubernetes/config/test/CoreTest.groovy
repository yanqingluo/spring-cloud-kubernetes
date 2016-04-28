/*
 * Copyright (C) 2016 to the original authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.fabric8.spring.cloud.kubernetes.config.test

import io.fabric8.kubernetes.api.model.ConfigMapBuilder
import io.fabric8.kubernetes.client.Config
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.server.mock.KubernetesMockServer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.IntegrationTest
import org.springframework.boot.test.SpringApplicationConfiguration
import org.springframework.core.env.Environment
import spock.lang.Specification

@SpringApplicationConfiguration(TestApplication.class)
@IntegrationTest(
        [
                "spring.application.name=testapp",
                "spring.cloud.kubernetes.client.namespace=testns",
                "spring.cloud.kubernetes.client.trustCerts=true",
                "spring.cloud.kubernetes.config.namespace=testns"
])
@EnableConfigurationProperties
class CoreTest extends Specification {

    private static KubernetesMockServer mockServer = new KubernetesMockServer()
    private static KubernetesClient mockClient

    @Autowired
    Environment environment

    @Autowired(required = false)
    Config config

    @Autowired(required = false)
    KubernetesClient client

    def setupSpec() {
        mockServer.init()
        mockClient = mockServer.createClient()

        //Setup configmap data
        Map<String, String> data = new HashMap<>();
        data.put("spring.kubernetes.test.value", "value1")
        mockServer.expect().get().withPath("/api/v1/namespaces/testns/configmaps/testapp").andReturn(200, new ConfigMapBuilder()
                .withData(data)
                .build()).always()

        //Configure the kubernetes master url to point to the mock server
        System.setProperty(Config.KUBERNETES_MASTER_SYSTEM_PROPERTY, mockClient.getConfiguration().getMasterUrl())
        System.setProperty(Config.KUBERNETES_TRUST_CERT_SYSTEM_PROPERTY, "true")
        System.setProperty(Config.KUBERNETES_AUTH_TRYKUBECONFIG_SYSTEM_PROPERTY, "false")
        System.setProperty(Config.KUBERNETES_AUTH_TRYSERVICEACCOUNT_SYSTEM_PROPERTY, "false")
    }

    def cleanupSpec() {
        mockServer.destroy();
    }

    def "Kubernetes client config bean should be present"() {
        expect:
            config != null
    }

    def "Kubernetes client config bean should be configurable via system properties"() {
        expect:
            config.getMasterUrl().equals(mockClient.getConfiguration().getMasterUrl());
            config.getNamespace().equals("testns");
            config.trustCerts
    }

    def "Kubernetes client bean should be present"() {
        expect:
            client != null
    }

    def "Kubernetes client should be configured from system properties"() {
        expect:
            client.getConfiguration().getMasterUrl().equals(mockClient.getConfiguration().getMasterUrl());
    }


    def "properties should be read from config map"() {
        expect:
            environment.getProperty("spring.kubernetes.test.value").equals("value1");
    }
}
