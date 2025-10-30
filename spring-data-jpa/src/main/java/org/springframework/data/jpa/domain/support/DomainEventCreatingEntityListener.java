package org.springframework.data.jpa.domain.support;

import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.AbstractAggregateRoot;
import org.springframework.data.jpa.domain.JpaDomainEvents;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;

@Component
public class DomainEventCreatingEntityListener {

  private static final Logger logger = LoggerFactory.getLogger(DomainEventCreatingEntityListener.class);

  private final Method registerEventMethod;

  DomainEventCreatingEntityListener() throws NoSuchMethodException {
    registerEventMethod = AbstractAggregateRoot.class.getDeclaredMethod("registerEvent", Object.class);
    registerEventMethod.setAccessible(true);
  }

  @PrePersist
  void registerCreatedEvent(Object entity) {
    registerEvent(entity, JpaDomainEvents::created);
  }

  @PreRemove
  void registerRemovedEvent(Object entity) {
    registerEvent(entity, JpaDomainEvents::removed);
  }

  private void registerEvent(Object entity, Function<JpaDomainEvents, Class<?>[]> eventField) {
    final var entityClazz = entity.getClass();
    final var eventClasses = Optional.ofNullable(entity.getClass().getAnnotation(JpaDomainEvents.class))
      .map(eventField)
      .orElse(new Class<?>[0]);

    if (eventClasses.length != 0) {
      if (AbstractAggregateRoot.class.isAssignableFrom(entityClazz)) {
        Arrays
          .stream(eventClasses)
          .map((eventClazz) -> createEventObject(entity, eventClazz))
          .forEach((event) -> registerEvent(entity, event));
      } else {
        final var aggregateRoots = Arrays
          .stream(entityClazz.getDeclaredFields())
          .filter( it ->
            it.getAnnotation(ManyToOne.class) != null && AbstractAggregateRoot.class.isAssignableFrom(it.getType())
          )
          .toList();

          if (aggregateRoots.isEmpty()) {
            logger.atWarn().log("No AggregateRoot found in ManyToOne associations of {}", entityClazz.getCanonicalName());
            return;
          } else if (aggregateRoots.size() > 1) {
            logger.atWarn().log("Multiple AggregateRoots found in OneToMany associations of {}", entityClazz.getCanonicalName());
            return;
          }

          try {
            final var aggregateRootField = aggregateRoots.get(0);
            aggregateRootField.setAccessible(true);
            final var aggregateRoot = aggregateRootField.get(entity);
            Arrays
              .stream(eventClasses)
              .map((eventClazz) -> createEventObject(entity, eventClazz))
              .forEach((event) -> registerEvent(aggregateRoot, event));

          } catch (IllegalAccessException e) {
            throw new RuntimeException("Error getting aggregate root", e);
          }

      }
    }
  }

  private void registerEvent(Object entity, Object event) {
    try {
      registerEventMethod.invoke(entity, event);
    } catch (InvocationTargetException | IllegalAccessException e) {
      throw new RuntimeException("Error while trying to register domain event for object creation", e);
    }
  }

  private static Object createEventObject(Object entity, Class<?> eventClazz) {
    try {
      final var constructor = eventClazz.getConstructor(entity.getClass());
      return constructor.newInstance(entity);
    } catch (NoSuchMethodException | InstantiationException | IllegalAccessException |
             InvocationTargetException e) {
      throw new RuntimeException("Error while trying to create domain event for object creation", e);
    }
  }
}
