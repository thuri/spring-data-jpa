package org.springframework.data.jpa.domain.support;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.data.jpa.domain.sample.DomainEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { DomainEventCreatingEntityListenerTest.TestConfig.class })
@Transactional
@RecordApplicationEvents
public class DomainEventCreatingEntityListenerTest {

  @Autowired private DomainEventEntityRepository repository;

  @Test
  void shouldSendCreatedEvent(ApplicationEvents events) {
    final var entity = repository.save(new DomainEventEntity("CreateEvent Test Entity"));

    assertThat(events.stream(DomainEventEntity.CreatedEvent.class).toList())
      .hasSize(1)
      .allSatisfy((createdEvent) -> assertThat(createdEvent.entity()).isSameAs(entity));
  }

  @Test
  void shouldSendRemovedEvent(ApplicationEvents events) {
    final var entity = repository.save(new DomainEventEntity("CreateEvent Test Entity"));
    repository.delete(entity);

    assertThat(events.stream(DomainEventEntity.RemovedEvent.class).toList())
      .hasSize(1)
      .allSatisfy((createdEvent) -> assertThat(createdEvent.entity()).isSameAs(entity));
  }

  interface DomainEventEntityRepository extends JpaRepository<DomainEventEntity, Long> {}

  @ImportResource({ "classpath:infrastructure.xml" })
  @Configuration
  @EnableJpaRepositories(basePackageClasses = DomainEventCreatingEntityListenerTest.class, considerNestedRepositories = true)
  static class TestConfig {}

}
