package com.example.arinfra.service.health;

import com.example.arinfra.repository.DummyRepository;
import com.example.arinfra.repository.model.Dummy;
import com.example.arinfra.service.util.Paginator;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class HealthRepositoryService {
  private final Paginator paginator;
  private final DummyRepository dummyRepository;

  public Page<Dummy> getAll(Integer page, Integer size) {
    var pagination = paginator.apply(page, size);

    var finalPage = pagination.get("page");
    var finalSize = pagination.get("size");

    Pageable pageable = PageRequest.of(finalPage, finalSize);

    return dummyRepository.findAll(pageable);
  }
}
