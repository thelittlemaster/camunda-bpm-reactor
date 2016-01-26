package org.camunda.bpm.extension.reactor.plugin;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.ProcessEngineRule;
import org.camunda.bpm.extension.reactor.CamundaReactor;
import org.camunda.bpm.extension.reactor.SelectorBuilder;
import org.camunda.bpm.extension.reactor.event.DelegateEvent;
import org.camunda.bpm.extension.reactor.listener.PublisherExecutionListener;
import org.camunda.bpm.extension.reactor.listener.SubscriberExecutionListener;
import org.camunda.bpm.extension.test.ReactorProcessEngineConfiguration;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.slf4j.Logger;
import reactor.bus.EventBus;
import reactor.bus.selector.Selectors;
import reactor.fn.Consumer;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.assertThat;
import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.complete;
import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.task;
import static org.mockito.Mockito.when;
import static org.slf4j.LoggerFactory.getLogger;


@Deployment(resources = {"ProcessA.bpmn"})
public class ReactorProcessEnginePluginTest {

  @Rule
  public final ProcessEngineRule processEngineRule = ReactorProcessEngineConfiguration.buildRule();

  private final Logger logger = getLogger(this.getClass());
  private EventBus eventBus;

  @Before
  public void init() {
    eventBus = CamundaReactor.eventBus(processEngineRule.getProcessEngine());
  }

  final Map<String, LinkedHashSet<DelegateEvent>> events = new LinkedHashMap<>();

  @Test
  public void fire_events_on_userTasks() {

    register(CamundaReactor.key("process_a", null, null));
    register(CamundaReactor.key("process_a", "task_a", null));
    register(CamundaReactor.key("process_a", "task_a", "complete"));
    register(CamundaReactor.key("process_a", "task_a", "start"));
    register(CamundaReactor.key("process_a", "task_a", "end"));
    register(CamundaReactor.key(null, null, "create"));
    register(CamundaReactor.key(null, null, null));

    CamundaReactor.subscribeTo(eventBus).on(CamundaReactor.uri(null, null, "create"), new TaskListener() {
      @Override
      public void notify(DelegateTask delegateTask) {
        delegateTask.setAssignee("foo");
        delegateTask.addCandidateGroup("bar");
        delegateTask.setName("my task");
      }
    });

    final ProcessInstance processInstance = processEngineRule.getRuntimeService().startProcessInstanceByKey("process_a");


    complete(task(processInstance));
    assertThat(task(processInstance)).isAssignedTo("foo");
    complete(task(processInstance));
    assertThat(processInstance).isEnded();

    for (Map.Entry<String, LinkedHashSet<DelegateEvent>> e : events.entrySet()) {
      logger.info("* " + e.getKey());
      for (DelegateEvent v : e.getValue()) {
        logger.info("    * " + v);
      }
    }

    assertThat(events).isNotEmpty();
  }

  private void register(String uri) {
    eventBus.on(Selectors.uri(uri), new Consumer<DelegateEvent>() {
      @Override
      public void accept(DelegateEvent event) {
        LinkedHashSet<DelegateEvent> e = events.get(event.getKey());
        if (e == null) {
          events.put(event.getKey().toString(), new LinkedHashSet<DelegateEvent>());
          accept(event);
          return;
        }

        e.add(event);
      }
    });
  }


}
