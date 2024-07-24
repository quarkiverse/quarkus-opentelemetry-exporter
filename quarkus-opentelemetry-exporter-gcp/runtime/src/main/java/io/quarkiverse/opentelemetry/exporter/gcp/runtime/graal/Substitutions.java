package io.quarkiverse.opentelemetry.exporter.gcp.runtime.graal;

import java.io.IOException;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import org.threeten.bp.Duration;

import com.google.api.core.ApiFunction;
import com.google.api.gax.grpc.ChannelPrimer;
import com.google.api.gax.grpc.GrpcHeaderInterceptor;
import com.google.api.gax.grpc.GrpcInterceptorProvider;
import com.google.api.gax.grpc.InstantiatingGrpcChannelProvider;
import com.google.api.gax.rpc.HeaderProvider;
import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ChannelCredentials;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.Grpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.MethodDescriptor;
import io.grpc.NameResolver;
import io.grpc.netty.shaded.io.netty.util.internal.logging.InternalLogLevel;
import io.grpc.netty.shaded.io.netty.util.internal.logging.InternalLogger;
import io.grpc.netty.shaded.io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Cut out unsupported and optional features that are only present in grpc-alts.
 *
 * code copied from Camel quarkus Google PubSub substitutions
 * https://github.com/apache/camel-quarkus/commit/372e221d97e9680cebfecd8788cd3a2802eb3686
 */

final class Substitutions {
}

@TargetClass(InstantiatingGrpcChannelProvider.class)
final class InstantiatingGrpcChannelProviderSubstitutions {
    @Alias
    private Executor executor;
    @Alias
    private HeaderProvider headerProvider;
    @Alias
    private GrpcInterceptorProvider interceptorProvider;
    @Alias
    private String endpoint;
    @Alias
    private Integer maxInboundMessageSize;
    @Alias
    private Integer maxInboundMetadataSize;
    @Alias
    private Duration keepAliveTime;
    @Alias
    private Duration keepAliveTimeout;
    @Alias
    private Boolean keepAliveWithoutCalls;
    @Alias
    private ChannelPrimer channelPrimer;
    @Alias
    private ApiFunction<ManagedChannelBuilder, ManagedChannelBuilder> channelConfigurator;

    @Substitute
    private ManagedChannel createSingleChannel() throws IOException {
        GrpcHeaderInterceptor headerInterceptor = new GrpcHeaderInterceptor(headerProvider.getHeaders());
        ClientInterceptor metadataHandlerInterceptor = new GrpcMetadataHandlerInterceptorTarget();

        int colon = endpoint.lastIndexOf(':');
        if (colon < 0) {
            throw new IllegalStateException("invalid endpoint - should have been validated: " + endpoint);
        }
        int port = Integer.parseInt(endpoint.substring(colon + 1));
        String serviceAddress = endpoint.substring(0, colon);

        ManagedChannelBuilder<?> builder;
        ChannelCredentials channelCredentials;
        try {
            channelCredentials = createMtlsChannelCredentials();
        } catch (GeneralSecurityException e) {
            throw new IOException(e);
        }

        if (channelCredentials != null) {
            builder = Grpc.newChannelBuilder(endpoint, channelCredentials);
        } else {
            builder = ManagedChannelBuilder.forAddress(serviceAddress, port);
        }

        builder.disableServiceConfigLookUp();

        builder = builder.intercept(new GrpcChannelUUIDInterceptorTarget())
                .intercept(headerInterceptor)
                .intercept(metadataHandlerInterceptor)
                .userAgent(headerInterceptor.getUserAgentHeader())
                .executor(executor);

        if (maxInboundMetadataSize != null) {
            builder.maxInboundMetadataSize(maxInboundMetadataSize);
        }
        if (maxInboundMessageSize != null) {
            builder.maxInboundMessageSize(maxInboundMessageSize);
        }
        if (keepAliveTime != null) {
            builder.keepAliveTime(keepAliveTime.toMillis(), TimeUnit.MILLISECONDS);
        }
        if (keepAliveTimeout != null) {
            builder.keepAliveTimeout(keepAliveTimeout.toMillis(), TimeUnit.MILLISECONDS);
        }
        if (keepAliveWithoutCalls != null) {
            builder.keepAliveWithoutCalls(keepAliveWithoutCalls);
        }
        if (interceptorProvider != null) {
            builder.intercept(interceptorProvider.getInterceptors());
        }
        if (channelConfigurator != null) {
            builder = channelConfigurator.apply(builder);
        }

        ManagedChannel managedChannel = builder.build();
        if (channelPrimer != null) {
            channelPrimer.primeChannel(managedChannel);
        }
        return managedChannel;
    }

    @Alias
    ChannelCredentials createMtlsChannelCredentials() throws IOException, GeneralSecurityException {
        throw new UnsupportedOperationException();
    }
}

@TargetClass(className = "com.google.api.gax.grpc.GrpcMetadataHandlerInterceptor")
final class GrpcMetadataHandlerInterceptorTarget implements ClientInterceptor {

    @Alias()
    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
            final CallOptions callOptions, Channel next) {
        throw new UnsupportedOperationException();
    }
}

@TargetClass(className = "com.google.api.gax.grpc.GrpcChannelUUIDInterceptor")
final class GrpcChannelUUIDInterceptorTarget implements ClientInterceptor {
    @Alias
    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> methodDescriptor,
            CallOptions callOptions, Channel channel) {
        throw new UnsupportedOperationException();
    }
}

/**
 * Copy from io.quarkus.grpc.common.runtime.graal.Target_io_grpc_netty_UdsNameResolverProvider
 */
@TargetClass(className = "io.grpc.netty.shaded.io.grpc.netty.UdsNameResolverProvider", onlyWith = NoDomainSocketPredicate.class)
final class Target_io_grpc_netty_shaded_io_grpc_netty_UdsNameResolverProvider {

    @Substitute
    protected boolean isAvailable() {
        return false;
    }

    @Substitute
    public Object newNameResolver(URI targetUri, NameResolver.Args args) {
        // gRPC calls this method without calling isAvailable, so, make sure we do not touch the UdsNameResolver class.
        // (as it requires domain sockets)
        return null;
    }
}

final class NoDomainSocketPredicate implements BooleanSupplier {
    @Override
    public boolean getAsBoolean() {
        try {
            this.getClass().getClassLoader().loadClass("io.grpc.netty.shaded.io.netty.channel.unix.DomainSocketAddress");
            return false;
        } catch (Exception ignored) {
            return true;
        }
    }
}

@TargetClass(className = "io.grpc.netty.shaded.io.netty.util.internal.logging.InternalLoggerFactory")
final class Targetio_grpc_netty_shaded_io_netty_util_internal_logging_InternalLoggerFactory {

    @Substitute
    public static InternalLoggerFactory newDefaultFactory(String name) {
        return new InternalLoggerFactory() {
            @Override
            protected InternalLogger newInstance(String s) {
                return getInstance(s);
            }
        };
    }

    @Substitute
    static InternalLogger getInstance(Class<?> clazz) {
        return new InternalLogger() {
            @Override
            public String name() {
                return "noop";
            }

            @Override
            public boolean isTraceEnabled() {
                return false;
            }

            @Override
            public void trace(String s) {

            }

            @Override
            public void trace(String s, Object o) {

            }

            @Override
            public void trace(String s, Object o, Object o1) {

            }

            @Override
            public void trace(String s, Object... objects) {

            }

            @Override
            public void trace(String s, Throwable throwable) {

            }

            @Override
            public void trace(Throwable throwable) {

            }

            @Override
            public boolean isDebugEnabled() {
                return false;
            }

            @Override
            public void debug(String s) {

            }

            @Override
            public void debug(String s, Object o) {

            }

            @Override
            public void debug(String s, Object o, Object o1) {

            }

            @Override
            public void debug(String s, Object... objects) {

            }

            @Override
            public void debug(String s, Throwable throwable) {

            }

            @Override
            public void debug(Throwable throwable) {

            }

            @Override
            public boolean isInfoEnabled() {
                return false;
            }

            @Override
            public void info(String s) {

            }

            @Override
            public void info(String s, Object o) {

            }

            @Override
            public void info(String s, Object o, Object o1) {

            }

            @Override
            public void info(String s, Object... objects) {

            }

            @Override
            public void info(String s, Throwable throwable) {

            }

            @Override
            public void info(Throwable throwable) {

            }

            @Override
            public boolean isWarnEnabled() {
                return false;
            }

            @Override
            public void warn(String s) {

            }

            @Override
            public void warn(String s, Object o) {

            }

            @Override
            public void warn(String s, Object... objects) {

            }

            @Override
            public void warn(String s, Object o, Object o1) {

            }

            @Override
            public void warn(String s, Throwable throwable) {

            }

            @Override
            public void warn(Throwable throwable) {

            }

            @Override
            public boolean isErrorEnabled() {
                return false;
            }

            @Override
            public void error(String s) {

            }

            @Override
            public void error(String s, Object o) {

            }

            @Override
            public void error(String s, Object o, Object o1) {

            }

            @Override
            public void error(String s, Object... objects) {

            }

            @Override
            public void error(String s, Throwable throwable) {

            }

            @Override
            public void error(Throwable throwable) {

            }

            @Override
            public boolean isEnabled(InternalLogLevel internalLogLevel) {
                return false;
            }

            @Override
            public void log(InternalLogLevel internalLogLevel, String s) {

            }

            @Override
            public void log(InternalLogLevel internalLogLevel, String s, Object o) {

            }

            @Override
            public void log(InternalLogLevel internalLogLevel, String s, Object o, Object o1) {

            }

            @Override
            public void log(InternalLogLevel internalLogLevel, String s, Object... objects) {

            }

            @Override
            public void log(InternalLogLevel internalLogLevel, String s, Throwable throwable) {

            }

            @Override
            public void log(InternalLogLevel internalLogLevel, Throwable throwable) {

            }
        };
    }
}
