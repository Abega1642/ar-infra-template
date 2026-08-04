package com.example.arinfra.service.health;

import com.example.arinfra.repository.DummyRepository;
import com.example.arinfra.repository.model.Dummy;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class HealthRepositoryService {
  private final DummyRepository dummyRepository;

  public Page<Dummy> getAll(Pageable page) {
    return dummyRepository.findAll(page);
  }
}
