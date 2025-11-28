package org.springframework.data.jpa.domain.sample;

import jakarta.persistence.*;
import org.springframework.data.domain.AbstractAggregateRoot;
import org.springframework.data.jpa.domain.JpaDomainEvents;
import org.springframework.data.jpa.domain.support.DomainEventCreatingEntityListener;

import java.util.HashSet;
import java.util.Set;

@JpaDomainEvents(
  created = DomainEventEntity.CreatedEvent.class,
  removed = DomainEventEntity.RemovedEvent.class
)
@EntityListeners(DomainEventCreatingEntityListener.class)
@Entity
public class DomainEventEntity extends AbstractAggregateRoot<DomainEventEntity> {

  public record CreatedEvent(DomainEventEntity entity) {}
  public record RemovedEvent(DomainEventEntity entity) {}

  public DomainEventEntity() {}

  public DomainEventEntity(String name) {
    this.name = name;
  }

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  public Long id;

  public String name;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "parent")
  public final Set<DomainEventChildEntity> children = new HashSet<>();

}
