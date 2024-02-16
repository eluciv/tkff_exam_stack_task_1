package tinkoff.alltoscala.stack.task1.handler;

import tinkoff.alltoscala.stack.task1.client.Client;
import tinkoff.alltoscala.stack.task1.domain.ApplicationStatusResponse;
import tinkoff.alltoscala.stack.task1.domain.Response;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

public class HandlerImpl implements Handler {

	private final Client client;

	public HandlerImpl(Client client) {
		this.client = client;
	}

	@Override
	public ApplicationStatusResponse performOperation(String id) {
		client.getApplicationStatus1(id);
		client.getApplicationStatus1(id);

		CompletableFuture<ApplicationStatusResponse> getStatusFuture1 =
				createRetryableFuture(() -> client.getApplicationStatus1(id));
		CompletableFuture<ApplicationStatusResponse> getStatusFuture2 =
				createRetryableFuture(() -> client.getApplicationStatus2(id));

		return (ApplicationStatusResponse) CompletableFuture.anyOf(getStatusFuture1, getStatusFuture2)
				.orTimeout(15, TimeUnit.SECONDS)
				.join();
	}

	private CompletableFuture<ApplicationStatusResponse> createRetryableFuture(Supplier<Response> responseSupplier) {
		return CompletableFuture
				.supplyAsync(createGetApplicationStatusSupplier(responseSupplier, 1))
				.thenCompose(Function.identity());
	}

	private Supplier<CompletableFuture<ApplicationStatusResponse>> createGetApplicationStatusSupplier(
			Supplier<Response> statusRequester, int tryNumber) {
		return () -> {
			Instant startTime = Instant.now();
			Response response;
			Duration requestTime;
			try {
				response = statusRequester.get();
				requestTime = Duration.between(startTime, Instant.now());
			} catch (Exception e) {
				return CompletableFuture.completedFuture(
						new ApplicationStatusResponse.Failure(Duration.between(startTime, Instant.now()), tryNumber));
			}

			if (response instanceof Response.Success success) {
				return CompletableFuture.completedFuture(
						new ApplicationStatusResponse.Success(success.applicationId(), success.applicationStatus()));
			} else if (response instanceof Response.RetryAfter retry) {
				return CompletableFuture.supplyAsync(
						createGetApplicationStatusSupplier(statusRequester, tryNumber + 1),
						CompletableFuture.delayedExecutor(retry.delay().toNanos(), TimeUnit.NANOSECONDS)
				).thenCompose(Function.identity());
			} else {
				return CompletableFuture.completedFuture(new ApplicationStatusResponse.Failure(requestTime, tryNumber));
			}
		};
	}
}
