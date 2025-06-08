package com.shitpostengine.dank.service;

import com.shitpostengine.dank.model.Meme;
import com.shitpostengine.dank.repository.MemeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MemeService {

    private final MemeRepository memeRepository;


    public MemeService(MemeRepository memeRepository) {
        this.memeRepository = memeRepository;
    }

    public Meme saveMeme(Meme meme) {
        return memeRepository.save(meme);
    }

    public List<Meme> getAllMemes() {
        return memeRepository.findAll();
    }
}