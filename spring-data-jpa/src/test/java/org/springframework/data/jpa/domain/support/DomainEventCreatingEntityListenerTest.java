package org.springframework.data.jpa.domain.support;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.data.jpa.domain.sample.DomainEventChildEntity;
import org.springframework.data.jpa.domain.sample.DomainEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { DomainEventCreatingEntityListenerTest.TestConfig.class })
@Transactional
@RecordApplicationEvents
public class DomainEventCreatingEntityListenerTest {

  @Autowired private DomainEventEntityRepository repository;
  @Autowired private EntityManager entityManager;

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
    repository.flush();

    assertThat(events.stream(DomainEventEntity.RemovedEvent.class).toList())
      .hasSize(1)
      .allSatisfy((createdEvent) -> assertThat(createdEvent.entity()).isSameAs(entity));
  }

  @Test
  void shouldSendChildAddedEvent(ApplicationEvents events) {
    var parent = new DomainEventEntity("CreateEvent Test Entity");
    var child = new DomainEventChildEntity(parent, "Child 1 Name");
    parent.children.add(child);
    repository.save(parent);

    assertThat(events.stream(DomainEventChildEntity.ChildAdded.class))
      .hasSize(1)
      .allSatisfy((addedEvent) -> assertThat(addedEvent.child()).isSameAs(child));
  }

  @Test
  void shouldSendChildRemoved(ApplicationEvents events) {
    var parent = new DomainEventEntity("CreateEvent Test Entity");
    var child = new DomainEventChildEntity(parent, "Child 1 Name");
    parent.children.add(child);
    parent = repository.save(parent);

    entityManager.flush();
    entityManager.clear();

    parent = repository.findById(parent.id).orElseThrow();
    final var removed = parent.children.removeIf(it -> it.name.equals(child.name));
    assertThat(removed).isTrue();

    // this only works if we call saveAndFlush because that will trigger the eventhandling
    // if save is called, the PreRemoved Eventlistener is not triggered yet and we don't get the domain event
    //repository.saveAndFlush(parent);

    // this won't work. See above
    repository.save(parent);
    // explicit flushing doesn't help because the domain event handling is not triggered
    repository.flush();

    assertThat(events.stream(DomainEventChildEntity.ChildRemoved.class))
      .hasSize(1)
      // the PreRemoved Listener is called on another instance
      .allSatisfy((removedEvent) -> assertThat(removedEvent.child()).isSameAs(child));
  }

  interface DomainEventEntityRepository extends JpaRepository<DomainEventEntity, Long> {}

  @ImportResource({ "classpath:infrastructure.xml" })
  @Configuration
  @EnableJpaRepositories(basePackageClasses = DomainEventCreatingEntityListenerTest.class, considerNestedRepositories = true)
  static class TestConfig {}

}
