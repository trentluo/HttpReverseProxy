/*
 * Copyright 2012 The Netty Project
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.zh.revproxy;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.ssl.SslHandler;

import javax.net.ssl.SSLEngine;

import org.apache.log4j.Logger;

/**
 * Creates a newly configured {@link io.netty.channel.ChannelPipeline} for a new
 * channel.
 */
public class SecureProxyInitializer extends ChannelInitializer<SocketChannel> {
	
	private Channel inbound;
	
	private final boolean isSecureBackend;
	private final String trustStoreLocation;
	private final String trustStorePassword;
	
	private static final Logger LOGGER = Logger.getLogger(SecureProxyInitializer.class);
	
	public SecureProxyInitializer(Channel inbound, boolean isSecureBackend, String trustStoreLocation, String trustStorePassword) {
		this.inbound = inbound;
		this.isSecureBackend = isSecureBackend;
		this.trustStoreLocation = trustStoreLocation;
		this.trustStorePassword = trustStorePassword;
	}
	
	@Override
	public void initChannel(SocketChannel ch) throws Exception {
		ChannelPipeline pipeline = ch.pipeline();
		
		// Add SSL handler first to encrypt and decrypt everything.
		// In this example, we use a bogus certificate in the server side
		// and accept any invalid certificates in the client side.
		// You will need something more complicated to identify both
		// and server in the real world.
		
		//		pipeline.addLast(new LoggingHandler(LogLevel.DEBUG));
		
		if (isSecureBackend) {
			LOGGER.info("Adding the SSL Handler to the pipeline");
			
			SSLEngine engine = SSLUtil.createClientSSLContext(trustStoreLocation, trustStorePassword).createSSLEngine();
			engine.setUseClientMode(true);
			
			pipeline.addLast("ssl", new SslHandler(engine));
		}
		
		// needed to forward the decoded request
		// to the backend in encoded form
		
		pipeline.addLast("encoder", new HttpClientCodec(104857600, 104857600, 104857600));
		pipeline.addLast(new HttpReverseProxyBackendHandler(inbound));
	}
}
