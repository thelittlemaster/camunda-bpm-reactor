package org.camunda.bpm.extension.reactor.event;

import static org.assertj.core.api.Assertions.assertThat;

import org.camunda.bpm.engine.delegate.DelegateCaseExecution;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DelegateCaseExecutionEventTest {

  @Mock
  private DelegateCaseExecution execution;


  @Test
  public void id_is_not_null() {
    assertThat(DelegateCaseExecutionEvent.wrap(execution).getId()).isNotNull();
  }
}
