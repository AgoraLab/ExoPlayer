package com.agora.stats.events;

public interface IEventProcessor {

  void asyncHandle(final IEvent event);

  void handle(final IEvent event);



}
