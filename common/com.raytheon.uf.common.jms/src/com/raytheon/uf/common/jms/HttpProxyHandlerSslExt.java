/**
 * Copyright 2014 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

/**NOTICE: slight modifications to Netty's HttpProxyHandler.java class found here: 
 * https://github.com/netty/netty/blob/netty-4.1.132.Final/handler-proxy/src/main/java/io/netty/handler/proxy/HttpProxyHandler.java
 * 
 * package declaration changed to "com.raytheon.uf.common.jms;" from "package io.netty.handler.proxy;"
 * Imported ProxyConnectException, ProxyHandler, PooledByteBufAllocator, SslContext, SslContextBuilder, SslHandler
 * Added Raytheon legal notice and software work history
 * Class name changed to "HttpProxyHandlerSslExt" from "HttpProxyHandler", along with all appropriate constructors.
 * Added declaration for AUTH_NONE from super class, ProxyHandler.java
 * Added SslHandler within the addCodec() function
 **/

/**
 * This software was developed and / or modified by Raytheon Company,
 * pursuant to Contract DG133W-05-CQ-1067 with the US Government.
 *
 * U.S. EXPORT CONTROLLED TECHNICAL DATA
 * This software product contains export-restricted data whose
 * export/transfer/disclosure is restricted by U.S. law. Dissemination
 * to non-U.S. persons whether in the United States or abroad requires
 * an export license or other authorization.
 *
 * Contractor Name:        Raytheon Company
 * Contractor Address:     6825 Pine Street, Suite 340
 *                         Mail Stop B8
 *                         Omaha, NE 68106
 *                         402.291.0100
 *
 * See the AWIPS II Master Rights File ("Master Rights File.pdf") for
 * further licensing information.
 **/
package com.raytheon.uf.common.jms;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator; //added for 22528
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelOutboundHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.base64.Base64;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.DefaultHttpHeadersFactory;
import io.netty.handler.codec.http.HttpHeadersFactory;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
//
import io.netty.handler.proxy.ProxyConnectException; //added for 22528
import io.netty.handler.proxy.ProxyHandler;//22528
import io.netty.handler.ssl.SslContext;//22528
import io.netty.handler.ssl.SslContextBuilder;//22528
import io.netty.handler.ssl.SslHandler;//22528
//

import io.netty.util.AsciiString;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.ObjectUtil;

import java.net.InetSocketAddress;
import java.net.SocketAddress;


/**
 * Modification of Netty's HttpProxyHandler class that adds an extra SslHandler to the Netty Pipeline.
 * This is necessary since Qpid client libraries do not account for an SSL layer on the proxy, and there seems to be no other 
 * convenient way to get access to the Netty Pipeline to add the extra SSL handler. Inheriting the HttpProxyHandler 
 * class itself doesn't work since the class is Final, and using a composition strategy doesn't work since most of the 
 * necessary HttpProxyHandler methods are protected and not accessible. Need to replace this in the future when a more 
 * convenient way becomes apparent.
 *
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Aug 06, 2021 22528      smoorthy    Initial creation. Variant Netty HttpProxyHandler that adds 
 *                                     an SslHandler within the addCodec function
 * Apr 02, 2026 2041430    mapeters    Pull in changes in 4.1.132
 *
 * </pre>
 *
 * 
 */
/**
 * Handler that establishes a blind forwarding proxy tunnel using
 * <a href="https://datatracker.ietf.org/doc/html/rfc7231#section-4.3.6">HTTP/1.1 CONNECT</a> request. It can be used to
 * establish plaintext or secure tunnels.
 * <p>
 * HTTP users who need to connect to a
 * <a href="https://datatracker.ietf.org/doc/html/rfc7230#page-10">message-forwarding HTTP proxy agent</a> instead of a
 * tunneling proxy should not use this handler.
 */
public final class HttpProxyHandlerSslExt extends ProxyHandler {

    private static final String PROTOCOL = "http";
    private static final String AUTH_BASIC = "basic";

    /**
     * A string that signifies 'no authentication' or 'anonymous'.
     */
    static final String AUTH_NONE = "none"; //22528 Added this from super class, ProxyHandler.java
    
    

    // Wrapper for the HttpClientCodec to prevent it to be removed by other handlers by mistake (for example the
    // WebSocket*Handshaker.
    //
    // See:
    // - https://github.com/netty/netty/issues/5201
    // - https://github.com/netty/netty/issues/5070
    private final HttpClientCodecWrapper codecWrapper = new HttpClientCodecWrapper();
    private final String username;
    private final String password;
    private final CharSequence authorization;
    private final HttpHeaders outboundHeaders;
    private final boolean ignoreDefaultPortsInConnectHostHeader;
    private HttpResponseStatus status;
    private HttpHeaders inboundHeaders;

    public HttpProxyHandlerSslExt(SocketAddress proxyAddress) { // modified all constructor names #22528
        this(proxyAddress, null);
    }

    public HttpProxyHandlerSslExt(SocketAddress proxyAddress, HttpHeaders headers) { //22528
        this(proxyAddress, headers, false);
    }

    public HttpProxyHandlerSslExt(SocketAddress proxyAddress,
                            HttpHeaders headers,
                            boolean ignoreDefaultPortsInConnectHostHeader) { //22528
        super(proxyAddress);
        username = null;
        password = null;
        authorization = null;
        this.outboundHeaders = headers;
        this.ignoreDefaultPortsInConnectHostHeader = ignoreDefaultPortsInConnectHostHeader;
    }

    public HttpProxyHandlerSslExt(SocketAddress proxyAddress, String username, String password) { //22528
        this(proxyAddress, username, password, null);
    }

    public HttpProxyHandlerSslExt(SocketAddress proxyAddress, String username, String password,
                            HttpHeaders headers) { //22528
        this(proxyAddress, username, password, headers, false);
    }

    public HttpProxyHandlerSslExt(SocketAddress proxyAddress,
                            String username,
                            String password,
                            HttpHeaders headers,
                            boolean ignoreDefaultPortsInConnectHostHeader) { //22528
        super(proxyAddress);
        this.username = ObjectUtil.checkNotNull(username, "username");
        this.password = ObjectUtil.checkNotNull(password, "password");

        ByteBuf authz = Unpooled.copiedBuffer(username + ':' + password, CharsetUtil.UTF_8);
        ByteBuf authzBase64;
        try {
            authzBase64 = Base64.encode(authz, false);
        } finally {
            authz.release();
        }
        try {
            authorization = new AsciiString("Basic " + authzBase64.toString(CharsetUtil.US_ASCII));
        } finally {
            authzBase64.release();
        }

        this.outboundHeaders = headers;
        this.ignoreDefaultPortsInConnectHostHeader = ignoreDefaultPortsInConnectHostHeader;
    }

    @Override
    public String protocol() {
        return PROTOCOL;
    }

    @Override
    public String authScheme() {
        return authorization != null? AUTH_BASIC : AUTH_NONE;
    }

    public String username() {
        return username;
    }

    public String password() {
        return password;
    }

    @Override
    protected void addCodec(ChannelHandlerContext ctx) throws Exception {
        ChannelPipeline p = ctx.pipeline();
        String name = ctx.name();
        p.addBefore(name, null, codecWrapper);

        //22528 Add the SslHandler to the Netty Pipeline used by qpid.
        final SslContext sslCtx = SslContextBuilder.forClient().build();
        SslHandler sslHandler = sslCtx.newHandler(PooledByteBufAllocator.DEFAULT);
        p.addFirst("ssl2", sslHandler);
        //
    }

    @Override
    protected void removeEncoder(ChannelHandlerContext ctx) throws Exception {
        codecWrapper.codec.removeOutboundHandler();
    }

    @Override
    protected void removeDecoder(ChannelHandlerContext ctx) throws Exception {
        codecWrapper.codec.removeInboundHandler();
    }

    @Override
    protected Object newInitialMessage(ChannelHandlerContext ctx) throws Exception {
        InetSocketAddress raddr = destinationAddress();

        String hostString = HttpUtil.formatHostnameForHttp(raddr);
        int port = raddr.getPort();
        String url = hostString + ":" + port;
        String hostHeader = (ignoreDefaultPortsInConnectHostHeader && (port == 80 || port == 443)) ?
                hostString :
                url;

        HttpHeadersFactory headersFactory = DefaultHttpHeadersFactory.headersFactory().withValidation(false);
        FullHttpRequest req = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1, HttpMethod.CONNECT,
                url,
                Unpooled.EMPTY_BUFFER, headersFactory, headersFactory);

        req.headers().set(HttpHeaderNames.HOST, hostHeader);

        if (authorization != null) {
            req.headers().set(HttpHeaderNames.PROXY_AUTHORIZATION, authorization);
        }

        if (outboundHeaders != null) {
            req.headers().add(outboundHeaders);
        }

        return req;
    }

    @Override
    protected boolean handleResponse(ChannelHandlerContext ctx, Object response) throws Exception {
        if (response instanceof HttpResponse) {
            if (status != null) {
                throw new HttpProxyConnectException(exceptionMessage("too many responses"), /*headers=*/ null);
            }
            HttpResponse res = (HttpResponse) response;
            status = res.status();
            inboundHeaders = res.headers();
        }

        boolean finished = response instanceof LastHttpContent;
        if (finished) {
            if (status == null) {
                throw new HttpProxyConnectException(exceptionMessage("missing response"), inboundHeaders);
            }
            if (status.code() != 200) {
                throw new HttpProxyConnectException(exceptionMessage("status: " + status), inboundHeaders);
            }
        }

        return finished;
    }

    /**
     * Specific case of a connection failure, which may include headers from the proxy.
     */
    public static final class HttpProxyConnectException extends ProxyConnectException {
        private static final long serialVersionUID = -8824334609292146066L;

        private final HttpHeaders headers;

        /**
         * @param message The failure message.
         * @param headers Header associated with the connection failure.  May be {@code null}.
         */
        public HttpProxyConnectException(String message, HttpHeaders headers) {
            super(message);
            this.headers = headers;
        }

        /**
         * Returns headers, if any.  May be {@code null}.
         */
        public HttpHeaders headers() {
            return headers;
        }
    }

    private static final class HttpClientCodecWrapper implements ChannelInboundHandler, ChannelOutboundHandler {
        final HttpClientCodec codec = new HttpClientCodec();

        @Override
        public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
            codec.handlerAdded(ctx);
        }

        @Override
        public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
            codec.handlerRemoved(ctx);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            codec.exceptionCaught(ctx, cause);
        }

        @Override
        public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
            codec.channelRegistered(ctx);
        }

        @Override
        public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
            codec.channelUnregistered(ctx);
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            codec.channelActive(ctx);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            codec.channelInactive(ctx);
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            codec.channelRead(ctx, msg);
        }

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
            codec.channelReadComplete(ctx);
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            codec.userEventTriggered(ctx, evt);
        }

        @Override
        public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
            codec.channelWritabilityChanged(ctx);
        }

        @Override
        public void bind(ChannelHandlerContext ctx, SocketAddress localAddress,
                         ChannelPromise promise) throws Exception {
            codec.bind(ctx, localAddress, promise);
        }

        @Override
        public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress,
                            ChannelPromise promise) throws Exception {
            codec.connect(ctx, remoteAddress, localAddress, promise);
        }

        @Override
        public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
            codec.disconnect(ctx, promise);
        }

        @Override
        public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
            codec.close(ctx, promise);
        }

        @Override
        public void deregister(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
            codec.deregister(ctx, promise);
        }

        @Override
        public void read(ChannelHandlerContext ctx) throws Exception {
            codec.read(ctx);
        }

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            codec.write(ctx, msg, promise);
        }

        @Override
        public void flush(ChannelHandlerContext ctx) throws Exception {
            codec.flush(ctx);
        }
    }
}