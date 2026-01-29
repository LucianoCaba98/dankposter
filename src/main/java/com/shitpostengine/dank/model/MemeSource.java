package com.shitpostengine.dank.model;

import reactor.core.publisher.Flux;

public interface MemeSource {

    Flux<Meme> fetch();
    String sourceName();
}