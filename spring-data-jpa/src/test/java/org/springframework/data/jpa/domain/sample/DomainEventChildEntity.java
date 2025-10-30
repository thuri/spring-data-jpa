package org.springframework.data.jpa.domain.sample;

import jakarta.persistence.*;
import org.springframework.data.jpa.domain.JpaDomainEvents;
import org.springframework.data.jpa.domain.support.DomainEventCreatingEntityListener;

@Entity
@JpaDomainEvents(
  created = DomainEventChildEntity.ChildAdded.class
)
@EntityListeners(DomainEventCreatingEntityListener.class)
public class DomainEventChildEntity {

  public record ChildAdded(DomainEventChildEntity child) {}
  public record ChildRemoved(DomainEventChildEntity child) {}

  public DomainEventChildEntity(DomainEventEntity parent, String name) {
    this.name = name;
    this.parent = parent;
  }

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  Long id;

  String name;

  @ManyToOne
  DomainEventEntity parent;

}
