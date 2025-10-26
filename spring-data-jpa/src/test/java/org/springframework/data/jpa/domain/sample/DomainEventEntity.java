package org.springframework.data.jpa.domain.sample;

import jakarta.persistence.*;
import org.springframework.data.domain.AbstractAggregateRoot;
import org.springframework.data.jpa.domain.DomainEvents;
import org.springframework.data.jpa.domain.support.DomainEventCreatingEntityListener;

@DomainEvents(created = DomainEventEntity.CreatedEvent.class)
@EntityListeners(DomainEventCreatingEntityListener.class)
@Entity
public class DomainEventEntity extends AbstractAggregateRoot<DomainEventEntity> {

  public record CreatedEvent(DomainEventEntity entity) {}

  public DomainEventEntity(String name) {
    this.name = name;
  }

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  Long id;

  String name;
}
