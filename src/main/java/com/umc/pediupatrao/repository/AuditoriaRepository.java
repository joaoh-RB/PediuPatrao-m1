package com.umc.pediupatrao.repository;

import com.umc.pediupatrao.entity.Auditoria;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface AuditoriaRepository extends MongoRepository<Auditoria, String> {
    List<Auditoria> findAllByOrderByDataHoraDesc();
}
