package com.azurion.saascore.empresas.infrastructure.persistence;

import com.azurion.saascore.empresas.domain.entities.Empresa;
import com.azurion.saascore.empresas.domain.repositories.EmpresaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaEmpresaRepository extends EmpresaRepository {
}
