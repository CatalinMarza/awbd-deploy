package com.artgallery.mapper;

import com.artgallery.dto.AcquisitionResponse;
import com.artgallery.dto.ArtistResponse;
import com.artgallery.dto.ArtworkResponse;
import com.artgallery.dto.CollectionResponse;
import com.artgallery.dto.ExhibitionResponse;
import com.artgallery.dto.ExhibitorResponse;
import com.artgallery.dto.GalleryReviewResponse;
import com.artgallery.dto.InsurancePolicyResponse;
import com.artgallery.dto.InsuranceResponse;
import com.artgallery.dto.LoanResponse;
import com.artgallery.dto.LocationResponse;
import com.artgallery.dto.RestorationResponse;
import com.artgallery.dto.StaffResponse;
import com.artgallery.dto.VisitorResponse;
import com.artgallery.model.Acquisition;
import com.artgallery.model.Artist;
import com.artgallery.model.Artwork;
import com.artgallery.model.Collection;
import com.artgallery.model.Exhibition;
import com.artgallery.model.Exhibitor;
import com.artgallery.model.GalleryReview;
import com.artgallery.model.Insurance;
import com.artgallery.model.InsurancePolicy;
import com.artgallery.model.Loan;
import com.artgallery.model.Location;
import com.artgallery.model.Restoration;
import com.artgallery.model.Staff;
import com.artgallery.model.Visitor;
import org.springframework.stereotype.Component;

/**
 * Pure entity &rarr; response-DTO mapping. Methods that read lazy associations
 * are always invoked inside a transactional service method.
 */
@Component
public class DtoMapper {

    public ArtworkResponse toResponse(Artwork a) {
        Collection collection = a.getCollection();
        Location location = a.getLocation();
        return new ArtworkResponse(
                a.getId(),
                a.getTitle(),
                a.getArtist().getId(),
                a.getArtist().getName(),
                a.getYearCreated(),
                a.getMedium(),
                collection != null ? collection.getId() : null,
                collection != null ? collection.getName() : null,
                location != null ? location.getId() : null,
                location != null ? location.getName() : null,
                a.getEstimatedValue());
    }

    public ExhibitionResponse toResponse(Exhibition e) {
        return new ExhibitionResponse(
                e.getId(),
                e.getTitle(),
                e.getStartDate(),
                e.getEndDate(),
                e.getExhibitor().getId(),
                e.getExhibitor().getName(),
                e.getDescription());
    }

    public VisitorResponse toResponse(Visitor v) {
        return new VisitorResponse(
                v.getId(), v.getName(), v.getEmail(), v.getPhone(),
                v.getMembershipType(), v.getJoinDate());
    }

    public StaffResponse toResponse(Staff s) {
        return new StaffResponse(
                s.getId(), s.getName(), s.getRole(), s.getHireDate(), s.getCertificationLevel());
    }

    public LoanResponse toResponse(Loan l) {
        return new LoanResponse(
                l.getId(),
                l.getArtwork().getId(),
                l.getArtwork().getTitle(),
                l.getExhibitor().getId(),
                l.getExhibitor().getName(),
                l.getStartDate(),
                l.getEndDate(),
                l.getConditions());
    }

    public InsuranceResponse toResponse(Insurance i) {
        return new InsuranceResponse(
                i.getId(),
                i.getArtwork().getId(),
                i.getArtwork().getTitle(),
                i.getPolicy().getId(),
                i.getPolicy().getProvider(),
                i.getInsuredAmount());
    }

    public RestorationResponse toResponse(Restoration r) {
        return new RestorationResponse(
                r.getId(),
                r.getArtwork().getId(),
                r.getArtwork().getTitle(),
                r.getStaff().getId(),
                r.getStaff().getName(),
                r.getStartDate(),
                r.getEndDate(),
                r.getDescription());
    }

    public GalleryReviewResponse toResponse(GalleryReview r) {
        Artwork artwork = r.getArtwork();
        Exhibition exhibition = r.getExhibition();
        return new GalleryReviewResponse(
                r.getId(),
                r.getVisitor().getId(),
                r.getVisitor().getName(),
                artwork != null ? artwork.getId() : null,
                artwork != null ? artwork.getTitle() : null,
                exhibition != null ? exhibition.getId() : null,
                exhibition != null ? exhibition.getTitle() : null,
                r.getRating(),
                r.getReviewText(),
                r.getReviewDate());
    }

    public AcquisitionResponse toResponse(Acquisition a) {
        return new AcquisitionResponse(
                a.getId(),
                a.getArtwork().getId(),
                a.getArtwork().getTitle(),
                a.getAcquisitionDate(),
                a.getAcquisitionType(),
                a.getPrice(),
                a.getStaff().getId(),
                a.getStaff().getName());
    }

    public ArtistResponse toResponse(Artist a) {
        return new ArtistResponse(
                a.getId(), a.getName(), a.getNationality(), a.getBirthYear(), a.getDeathYear());
    }

    public CollectionResponse toResponse(Collection c) {
        return new CollectionResponse(
                c.getId(), c.getName(), c.getDescription(), c.getCreatedDate());
    }

    public LocationResponse toResponse(Location l) {
        return new LocationResponse(
                l.getId(), l.getName(), l.getGalleryRoom(), l.getType(), l.getCapacity());
    }

    public ExhibitorResponse toResponse(Exhibitor e) {
        return new ExhibitorResponse(
                e.getId(), e.getName(), e.getAddress(), e.getCity(), e.getContactInfo());
    }

    public InsurancePolicyResponse toResponse(InsurancePolicy p) {
        return new InsurancePolicyResponse(
                p.getId(), p.getProvider(), p.getStartDate(), p.getEndDate(), p.getTotalCoverageAmount());
    }
}
