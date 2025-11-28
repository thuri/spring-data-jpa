package org.springframework.data.jpa.domain.sample;

import jakarta.persistence.*;
import org.springframework.data.jpa.domain.JpaDomainEvents;
import org.springframework.data.jpa.domain.support.DomainEventCreatingEntityListener;

@Entity
@JpaDomainEvents(
  created = DomainEventChildEntity.ChildAdded.class,
  removed = DomainEventChildEntity.ChildRemoved.class
)
@EntityListeners(DomainEventCreatingEntityListener.class)
public class DomainEventChildEntity {

  public record ChildAdded(DomainEventChildEntity child) {}
  public record ChildRemoved(DomainEventChildEntity child) {}

  private DomainEventChildEntity(){}

  public DomainEventChildEntity(DomainEventEntity parent, String name) {
    this.name = name;
    this.parent = parent;
  }

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  public Long id;

  public String name;

  @ManyToOne
  DomainEventEntity parent;

}
