package com.back.config

import org.springframework.context.annotation.Configuration
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.config.ChannelRegistration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.messaging.support.MessageHeaderAccessor
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer
import java.security.Principal

@Configuration
@EnableWebSocketMessageBroker
class WebsocketConfig : WebSocketMessageBrokerConfigurer {

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        registry.addEndpoint("/chat")
            .setAllowedOriginPatterns(*ALLOWED_ORIGINS)
            .withSockJS()
    }

    override fun configureMessageBroker(config: MessageBrokerRegistry) {
        config.enableSimpleBroker("/topic", "/queue")
        config.setUserDestinationPrefix("/user")
        config.setApplicationDestinationPrefixes("/app")
    }

    override fun configureClientInboundChannel(registration: ChannelRegistration) {
        registration.interceptors(object : ChannelInterceptor {
            override fun preSend(message: Message<*>, channel: MessageChannel): Message<*> {
                val accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor::class.java)

                if (accessor?.command == StompCommand.CONNECT) {
                    accessor.getFirstNativeHeader("user-email")?.let { userEmail ->
                        accessor.user = StompPrincipal(userEmail)
                        println("WebSocket 사용자 설정: $userEmail")
                    }
                }

                return message
            }
        })
    }

    data class StompPrincipal(private val username: String) : Principal {
        override fun getName(): String = username
    }

    companion object {
        private val ALLOWED_ORIGINS = arrayOf(
            "http://localhost:3000",
            "http://34.64.160.179",
            "https://frontend-devteam-10.vercel.app/",
            "https://frontend-devteam-10.vercel.app",
            "https://www.devteam10.org"
        )
    }
}
