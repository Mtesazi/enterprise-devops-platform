package com.mtesazi.departmentservice.service.impl;

import com.mtesazi.departmentservice.dto.DepartmentRequest;
import com.mtesazi.departmentservice.dto.DepartmentResponse;
import com.mtesazi.departmentservice.entity.Department;
import com.mtesazi.departmentservice.exception.DepartmentNotFoundException;
import com.mtesazi.departmentservice.mapper.DepartmentMapper;
import com.mtesazi.departmentservice.repository.DepartmentRepository;
import com.mtesazi.departmentservice.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final DepartmentMapper departmentMapper;

    @Override
    public DepartmentResponse createDepartment(DepartmentRequest request) {
        Department department = departmentMapper.toEntity(request);
        Department savedDepartment = departmentRepository.save(department);
        return departmentMapper.toResponse(savedDepartment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentResponse> getAllDepartments() {
        return departmentRepository.findAll()
                .stream()
                .map(departmentMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentResponse getDepartmentById(Long id) {
        return departmentMapper.toResponse(findDepartmentOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentResponse getDepartmentByReference(String reference) {
        return departmentMapper.toResponse(findDepartmentByReferenceOrThrow(reference));
    }

    @Override
    public DepartmentResponse updateDepartment(Long id, DepartmentRequest request) {
        Department department = findDepartmentOrThrow(id);
        departmentMapper.applyRequestToEntity(request, department);
        Department updatedDepartment = departmentRepository.save(department);
        return departmentMapper.toResponse(updatedDepartment);
    }

    @Override
    public void deleteDepartment(Long id) {
        Department department = findDepartmentOrThrow(id);
        departmentRepository.delete(department);
    }

    private Department findDepartmentOrThrow(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new DepartmentNotFoundException("Department " + id + " not found"));
    }

    private Department findDepartmentByReferenceOrThrow(String reference) {
        if (reference == null || reference.isBlank()) {
            throw new DepartmentNotFoundException("Department reference must not be blank");
        }

        if (reference.matches("\\d+")) {
            return findDepartmentOrThrow(Long.parseLong(reference));
        }

        Optional<Department> byCode = departmentRepository.findByCodeIgnoreCase(reference);
        if (byCode.isPresent()) {
            return byCode.get();
        }

        return departmentRepository.findByNameIgnoreCase(reference)
                .orElseThrow(() -> new DepartmentNotFoundException("Department " + reference + " not found"));
    }
}
