package com.artgallery.config;

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
import com.artgallery.repository.AcquisitionRepository;
import com.artgallery.repository.ArtistRepository;
import com.artgallery.repository.ArtworkRepository;
import com.artgallery.repository.CollectionRepository;
import com.artgallery.repository.ExhibitionRepository;
import com.artgallery.repository.ExhibitorRepository;
import com.artgallery.repository.GalleryReviewRepository;
import com.artgallery.repository.InsurancePolicyRepository;
import com.artgallery.repository.InsuranceRepository;
import com.artgallery.repository.LoanRepository;
import com.artgallery.repository.LocationRepository;
import com.artgallery.repository.RestorationRepository;
import com.artgallery.repository.StaffRepository;
import com.artgallery.repository.VisitorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Seeds demonstration domain data (ported from the original Oracle dataset) for
 * the {@code dev} profile only, and only when the database is empty. This keeps
 * the integration tests (which run under the {@code test} profile) unaffected.
 */
@Component
@Profile("dev")
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class DevDataSeeder implements CommandLineRunner {

    private final ArtistRepository artistRepository;
    private final CollectionRepository collectionRepository;
    private final LocationRepository locationRepository;
    private final VisitorRepository visitorRepository;
    private final ExhibitorRepository exhibitorRepository;
    private final StaffRepository staffRepository;
    private final InsurancePolicyRepository policyRepository;
    private final ArtworkRepository artworkRepository;
    private final ExhibitionRepository exhibitionRepository;
    private final LoanRepository loanRepository;
    private final RestorationRepository restorationRepository;
    private final InsuranceRepository insuranceRepository;
    private final GalleryReviewRepository reviewRepository;
    private final AcquisitionRepository acquisitionRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (artistRepository.count() > 0) {
            log.debug("Domain data already present; skipping demo seed");
            return;
        }
        log.info("Seeding demo domain data (dev profile)");

        List<Artist> artists = artistRepository.saveAll(List.of(
                Artist.builder().name("Pablo Picasso").nationality("Spanish").birthYear(1881).deathYear(1973).build(),
                Artist.builder().name("Vincent van Gogh").nationality("Dutch").birthYear(1853).deathYear(1890).build(),
                Artist.builder().name("Claude Monet").nationality("French").birthYear(1840).deathYear(1926).build(),
                Artist.builder().name("Salvador Dali").nationality("Spanish").birthYear(1904).deathYear(1989).build()));

        List<Collection> collections = collectionRepository.saveAll(List.of(
                Collection.builder().name("Modern Masters").description("Key works of modern art").createdDate(LocalDate.of(2020, 1, 15)).build(),
                Collection.builder().name("Impressionist Highlights").description("Selected impressionist paintings").createdDate(LocalDate.of(2021, 3, 10)).build(),
                Collection.builder().name("Surreal Visions").description("Surrealist paintings and objects").createdDate(LocalDate.of(2022, 5, 20)).build(),
                Collection.builder().name("Permanent Collection").description("Core museum holdings").createdDate(LocalDate.of(2019, 9, 1)).build()));

        List<Location> locations = locationRepository.saveAll(List.of(
                Location.builder().name("Main Hall").galleryRoom("R1").type("Exhibit").capacity(50).build(),
                Location.builder().name("East Wing").galleryRoom("R2").type("Exhibit").capacity(40).build(),
                Location.builder().name("Storage A").galleryRoom("S1").type("Storage").capacity(200).build(),
                Location.builder().name("Storage B").galleryRoom("S2").type("Storage").capacity(150).build()));

        visitorRepository.saveAll(List.of(
                Visitor.builder().name("Alice Johnson").email("alice@example.com").phone("+40111111111").membershipType("Standard").joinDate(LocalDate.of(2023, 1, 10)).build(),
                Visitor.builder().name("Bob Smith").email("bob@example.com").phone("+40111111112").membershipType("VIP").joinDate(LocalDate.of(2023, 2, 15)).build(),
                Visitor.builder().name("Carla Pop").email("carla@example.com").phone("+40111111113").membershipType("Student").joinDate(LocalDate.of(2023, 3, 20)).build(),
                Visitor.builder().name("Dan Ionescu").email("dan@example.com").phone("+40111111114").membershipType("Standard").joinDate(LocalDate.of(2023, 4, 5)).build()));
        List<Visitor> visitors = visitorRepository.findAll();

        List<Exhibitor> exhibitors = exhibitorRepository.saveAll(List.of(
                Exhibitor.builder().name("Louvre Museum").address("Rue de Rivoli").city("Paris").contactInfo("contact@louvre.fr").build(),
                Exhibitor.builder().name("MoMA").address("11 W 53rd St").city("New York").contactInfo("info@moma.org").build(),
                Exhibitor.builder().name("Tate Modern").address("Bankside").city("London").contactInfo("contact@tate.org.uk").build(),
                Exhibitor.builder().name("Reina Sofia").address("Calle de Santa Isabel").city("Madrid").contactInfo("info@museoreinasofia.es").build()));

        List<Staff> staff = staffRepository.saveAll(List.of(
                Staff.builder().name("Elena Curator").role("Curator").hireDate(LocalDate.of(2020, 1, 1)).certificationLevel("Level 2").build(),
                Staff.builder().name("Mihai Restorer").role("Restorer").hireDate(LocalDate.of(2021, 5, 10)).certificationLevel("Level 3").build(),
                Staff.builder().name("Ioana Registrar").role("Registrar").hireDate(LocalDate.of(2022, 3, 15)).certificationLevel("Level 1").build(),
                Staff.builder().name("Andrei Manager").role("Manager").hireDate(LocalDate.of(2019, 9, 1)).certificationLevel("Level 3").build()));

        List<InsurancePolicy> policies = policyRepository.saveAll(List.of(
                InsurancePolicy.builder().provider("Global Insurance").startDate(LocalDate.of(2022, 1, 1)).endDate(LocalDate.of(2025, 1, 1)).totalCoverageAmount(BigDecimal.valueOf(3_000_000)).build(),
                InsurancePolicy.builder().provider("ArtSecure").startDate(LocalDate.of(2023, 1, 1)).endDate(LocalDate.of(2026, 1, 1)).totalCoverageAmount(BigDecimal.valueOf(1_500_000)).build(),
                InsurancePolicy.builder().provider("FineArt Shield").startDate(LocalDate.of(2023, 6, 1)).endDate(LocalDate.of(2027, 6, 1)).totalCoverageAmount(BigDecimal.valueOf(2_000_000)).build(),
                InsurancePolicy.builder().provider("Museum Protect").startDate(LocalDate.of(2024, 1, 1)).endDate(LocalDate.of(2028, 1, 1)).totalCoverageAmount(BigDecimal.valueOf(2_500_000)).build()));

        List<Exhibition> exhibitions = exhibitionRepository.saveAll(List.of(
                Exhibition.builder().title("Modern Icons").startDate(LocalDate.of(2024, 1, 10)).endDate(LocalDate.of(2024, 3, 10)).exhibitor(exhibitors.get(0)).description("Masterpieces of modern art").build(),
                Exhibition.builder().title("Impressionist Seasons").startDate(LocalDate.of(2024, 4, 1)).endDate(LocalDate.of(2024, 6, 30)).exhibitor(exhibitors.get(1)).description("Key impressionist works").build(),
                Exhibition.builder().title("Surreal Moments").startDate(LocalDate.of(2024, 7, 1)).endDate(LocalDate.of(2024, 9, 15)).exhibitor(exhibitors.get(2)).description("Surrealist paintings and sculptures").build(),
                Exhibition.builder().title("Van Gogh Focus").startDate(LocalDate.of(2024, 10, 1)).endDate(LocalDate.of(2024, 12, 31)).exhibitor(exhibitors.get(3)).description("A selection of Van Gogh's works").build()));

        Artwork demoiselles = artwork("Les Demoiselles d'Avignon", artists.get(0), 1907, collections.get(0), locations.get(0), 1_000_000, exhibitions.get(0));
        Artwork guernica = artwork("Guernica", artists.get(0), 1937, collections.get(0), locations.get(1), 1_500_000, exhibitions.get(0));
        Artwork starryNight = artwork("Starry Night", artists.get(1), 1889, collections.get(1), locations.get(0), 1_200_000, exhibitions.get(1), exhibitions.get(3));
        Artwork sunflowers = artwork("Sunflowers", artists.get(1), 1888, collections.get(1), locations.get(1), 800_000, exhibitions.get(1));
        Artwork waterLilies = artwork("Water Lilies", artists.get(2), 1916, collections.get(1), locations.get(0), 900_000, exhibitions.get(1));
        Artwork sunrise = artwork("Impression, Sunrise", artists.get(2), 1872, collections.get(1), locations.get(1), 700_000);
        Artwork memory = artwork("The Persistence of Memory", artists.get(3), 1931, collections.get(2), locations.get(0), 600_000, exhibitions.get(2));
        Artwork swans = artwork("Swans Reflecting Elephants", artists.get(3), 1937, collections.get(2), locations.get(1), 550_000, exhibitions.get(2));
        List<Artwork> artworks = artworkRepository.saveAll(List.of(
                demoiselles, guernica, starryNight, sunflowers, waterLilies, sunrise, memory, swans));

        loanRepository.saveAll(List.of(
                Loan.builder().artwork(guernica).exhibitor(exhibitors.get(1)).startDate(LocalDate.of(2023, 1, 1)).endDate(LocalDate.of(2023, 3, 1)).conditions("Standard insurance").build(),
                Loan.builder().artwork(sunflowers).exhibitor(exhibitors.get(0)).startDate(LocalDate.of(2023, 2, 15)).endDate(LocalDate.of(2023, 4, 15)).conditions("Climate control required").build(),
                Loan.builder().artwork(sunrise).exhibitor(exhibitors.get(2)).startDate(LocalDate.of(2023, 5, 1)).endDate(LocalDate.of(2023, 7, 1)).conditions("Framed transport").build(),
                Loan.builder().artwork(swans).exhibitor(exhibitors.get(3)).startDate(LocalDate.of(2023, 8, 1)).endDate(LocalDate.of(2023, 10, 1)).conditions("Handle with care").build()));

        Staff restorer = staff.get(1);
        restorationRepository.saveAll(List.of(
                Restoration.builder().artwork(demoiselles).staff(restorer).startDate(LocalDate.of(2022, 1, 10)).endDate(LocalDate.of(2022, 2, 10)).description("Varnish cleaning").build(),
                Restoration.builder().artwork(starryNight).staff(restorer).startDate(LocalDate.of(2021, 3, 1)).endDate(LocalDate.of(2021, 4, 1)).description("Canvas stabilization").build(),
                Restoration.builder().artwork(waterLilies).staff(restorer).startDate(LocalDate.of(2020, 9, 15)).endDate(LocalDate.of(2020, 10, 15)).description("Color retouching").build(),
                Restoration.builder().artwork(memory).staff(restorer).startDate(LocalDate.of(2023, 1, 5)).endDate(LocalDate.of(2023, 2, 5)).description("Frame repair").build()));

        insuranceRepository.saveAll(List.of(
                insurance(demoiselles, policies.get(0), 900_000),
                insurance(guernica, policies.get(0), 1_200_000),
                insurance(starryNight, policies.get(1), 1_000_000),
                insurance(sunflowers, policies.get(1), 700_000),
                insurance(waterLilies, policies.get(2), 800_000),
                insurance(sunrise, policies.get(2), 600_000),
                insurance(memory, policies.get(3), 500_000),
                insurance(swans, policies.get(3), 450_000)));

        reviewRepository.saveAll(List.of(
                GalleryReview.builder().visitor(visitors.get(0)).artwork(demoiselles).exhibition(exhibitions.get(0)).rating(5).reviewText("Amazing piece!").reviewDate(LocalDate.of(2024, 1, 20)).build(),
                GalleryReview.builder().visitor(visitors.get(1)).artwork(starryNight).exhibition(exhibitions.get(1)).rating(4).reviewText("Great impressionist selection.").reviewDate(LocalDate.of(2024, 4, 15)).build(),
                GalleryReview.builder().visitor(visitors.get(2)).exhibition(exhibitions.get(2)).rating(4).reviewText("Interesting surrealist show.").reviewDate(LocalDate.of(2024, 7, 10)).build(),
                GalleryReview.builder().visitor(visitors.get(3)).artwork(memory).exhibition(exhibitions.get(2)).rating(5).reviewText("Loved the Dali works.").reviewDate(LocalDate.of(2024, 7, 12)).build()));

        acquisitionRepository.saveAll(List.of(
                Acquisition.builder().artwork(demoiselles).acquisitionDate(LocalDate.of(2019, 1, 10)).acquisitionType("Purchase").price(BigDecimal.valueOf(800_000)).staff(staff.get(2)).build(),
                Acquisition.builder().artwork(starryNight).acquisitionDate(LocalDate.of(2018, 6, 5)).acquisitionType("Purchase").price(BigDecimal.valueOf(900_000)).staff(staff.get(2)).build()));

        log.info("Demo seed complete: {} artworks, {} exhibitions", artworks.size(), exhibitions.size());
    }

    private Artwork artwork(String title, Artist artist, int year, Collection collection,
                            Location location, long value, Exhibition... exhibitions) {
        Artwork artwork = Artwork.builder()
                .title(title)
                .artist(artist)
                .yearCreated(year)
                .medium("Oil on Canvas")
                .collection(collection)
                .location(location)
                .estimatedValue(BigDecimal.valueOf(value))
                .build();
        artwork.getExhibitions().addAll(List.of(exhibitions));
        return artwork;
    }

    private Insurance insurance(Artwork artwork, InsurancePolicy policy, long insuredAmount) {
        return Insurance.builder()
                .artwork(artwork)
                .policy(policy)
                .insuredAmount(BigDecimal.valueOf(insuredAmount))
                .build();
    }
}
