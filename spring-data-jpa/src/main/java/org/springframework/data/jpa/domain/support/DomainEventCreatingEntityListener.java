package org.springframework.data.jpa.domain.support;

import jakarta.persistence.PrePersist;
import org.springframework.data.domain.AbstractAggregateRoot;
import org.springframework.data.jpa.domain.DomainEvents;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

public class DomainEventCreatingEntityListener {

  private final Method registerEventMethod;

  DomainEventCreatingEntityListener() throws NoSuchMethodException {
    registerEventMethod = AbstractAggregateRoot.class.getDeclaredMethod("registerEvent", Object.class);
    registerEventMethod.setAccessible(true);
  }

  @PrePersist
  void registerCreatedEvent(Object entity){

    final var entityClazz = entity.getClass();
    final var easyDomainEventAnnotation = entityClazz.getAnnotation(DomainEvents.class);

      if (AbstractAggregateRoot.class.isAssignableFrom(entityClazz)
        && easyDomainEventAnnotation != null) {

          Arrays
            .stream(easyDomainEventAnnotation.created())
            .map((eventClazz) -> {
              try {
                final var constructor = eventClazz.getConstructor(entity.getClass());
                return constructor.newInstance(entity);
              } catch (NoSuchMethodException | InstantiationException | IllegalAccessException |
                       InvocationTargetException e) {
                throw new RuntimeException("Error while trying to create domain event for object creation", e);
              }
            })
            .forEach((event) -> {
              try {
                registerEventMethod.invoke(entity, event);
              } catch (InvocationTargetException | IllegalAccessException e) {
                throw new RuntimeException("Error while trying to register domain event for object creation", e);
              }
            });
      }
  }
}
