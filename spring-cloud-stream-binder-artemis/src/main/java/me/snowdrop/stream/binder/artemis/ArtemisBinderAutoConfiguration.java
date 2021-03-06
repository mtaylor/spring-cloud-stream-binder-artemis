/*
 * Copyright 2016-2017 Red Hat, Inc, and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.snowdrop.stream.binder.artemis;

import me.snowdrop.stream.binder.artemis.handlers.ListenerContainerFactory;
import me.snowdrop.stream.binder.artemis.properties.ArtemisBinderConfigurationProperties;
import me.snowdrop.stream.binder.artemis.properties.ArtemisExtendedBindingProperties;
import me.snowdrop.stream.binder.artemis.provisioning.ArtemisProvisioningProvider;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jms.artemis.ArtemisAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.artemis.ArtemisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.stream.config.codec.kryo.KryoCodecAutoConfiguration;
import org.springframework.cloud.stream.provisioning.ProvisioningProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.integration.codec.Codec;
import org.springframework.jms.support.converter.MessagingMessageConverter;

import javax.jms.ConnectionFactory;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
@Configuration
@AutoConfigureAfter(ArtemisAutoConfiguration.class)
@ConditionalOnClass(ServerLocator.class)
@EnableConfigurationProperties({ArtemisBinderConfigurationProperties.class, ArtemisExtendedBindingProperties.class})
@Import(KryoCodecAutoConfiguration.class)
public class ArtemisBinderAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    ListenerContainerFactory listenerContainerFactory(ConnectionFactory connectionFactory) {
        return new ListenerContainerFactory(connectionFactory);
    }

    @Bean
    @ConditionalOnMissingBean
    MessagingMessageConverter messagingMessageConverter() {
        return new MessagingMessageConverter();
    }

    @Bean
    @ConditionalOnMissingBean
    ArtemisMessageChannelBinder artemisMessageChannelBinder(ArtemisProvisioningProvider provisioningProvider,
            ConnectionFactory connectionFactory, ListenerContainerFactory listenerContainerFactory,
            MessagingMessageConverter messagingMessageConverter, ArtemisExtendedBindingProperties bindingProperties,
            Codec codec) {
        ArtemisMessageChannelBinder binder = new ArtemisMessageChannelBinder(provisioningProvider, connectionFactory,
                listenerContainerFactory, messagingMessageConverter, bindingProperties);
        binder.setCodec(codec);
        return binder;
    }

    @Bean
    @ConditionalOnMissingBean(TransportConfiguration.class)
    TransportConfiguration transportConfiguration(ArtemisBinderConfigurationProperties binderProperties,
            ArtemisProperties artemisProperties) {
        Map<String, Object> config = new HashMap<>();
        config.put("host", artemisProperties.getHost());
        config.put("port", artemisProperties.getPort());
        return new TransportConfiguration(binderProperties.getTransport(), config);
    }

    @Bean
    @ConditionalOnMissingBean(ServerLocator.class)
    ServerLocator serverLocator(TransportConfiguration transportConfiguration) {
        return ActiveMQClient.createServerLocatorWithoutHA(transportConfiguration);
    }

    @Bean
    @ConditionalOnMissingBean(ProvisioningProvider.class)
    ArtemisProvisioningProvider provisioningProvider(ServerLocator serverLocator) {
        return new ArtemisProvisioningProvider(serverLocator);
    }

}