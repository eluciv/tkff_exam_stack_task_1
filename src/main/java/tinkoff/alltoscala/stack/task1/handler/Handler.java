package tinkoff.alltoscala.stack.task1.handler;

import tinkoff.alltoscala.stack.task1.domain.ApplicationStatusResponse;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public interface Handler {

	ApplicationStatusResponse performOperation(String id) throws ExecutionException, InterruptedException, TimeoutException;

}
