package com.artgallery.service;

import com.artgallery.common.PageResponse;
import com.artgallery.dto.VisitorRequest;
import com.artgallery.dto.VisitorResponse;
import com.artgallery.exception.DuplicateResourceException;
import com.artgallery.exception.ResourceNotFoundException;
import com.artgallery.mapper.DtoMapper;
import com.artgallery.model.Visitor;
import com.artgallery.repository.VisitorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Business logic for {@link Visitor}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class VisitorService {

    private final VisitorRepository visitorRepository;
    private final DtoMapper mapper;

    @Transactional(readOnly = true)
    public PageResponse<VisitorResponse> list(Pageable pageable) {
        log.debug("Listing visitors {}", pageable);
        return PageResponse.from(visitorRepository.findAll(pageable).map(mapper::toResponse));
    }

    @Transactional(readOnly = true)
    public PageResponse<VisitorResponse> search(String term, Pageable pageable) {
        log.debug("Searching visitors for '{}'", term);
        return PageResponse.from(visitorRepository
                .findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(term, term, pageable)
                .map(mapper::toResponse));
    }

    @Transactional(readOnly = true)
    public VisitorResponse get(Long id) {
        return mapper.toResponse(findVisitor(id));
    }

    public VisitorResponse create(VisitorRequest request) {
        if (StringUtils.hasText(request.email()) && visitorRepository.existsByEmailIgnoreCase(request.email())) {
            throw new DuplicateResourceException("A visitor with email '%s' already exists".formatted(request.email()));
        }
        Visitor visitor = new Visitor();
        apply(visitor, request);
        Visitor saved = visitorRepository.save(visitor);
        log.info("Created visitor id={} name='{}'", saved.getId(), saved.getName());
        return mapper.toResponse(saved);
    }

    public VisitorResponse update(Long id, VisitorRequest request) {
        Visitor visitor = findVisitor(id);
        apply(visitor, request);
        Visitor saved = visitorRepository.save(visitor);
        log.info("Updated visitor id={}", id);
        return mapper.toResponse(saved);
    }

    public void delete(Long id) {
        Visitor visitor = findVisitor(id);
        visitorRepository.delete(visitor);
        log.info("Deleted visitor id={}", id);
    }

    private Visitor findVisitor(Long id) {
        return visitorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Visitor", id));
    }

    private void apply(Visitor visitor, VisitorRequest r) {
        visitor.setName(r.name());
        visitor.setEmail(r.email());
        visitor.setPhone(r.phone());
        visitor.setMembershipType(r.membershipType());
        visitor.setJoinDate(r.joinDate());
    }
}
