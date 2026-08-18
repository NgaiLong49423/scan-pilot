package com.scanpilot.persistence;

import com.scanpilot.persistence.entity.FindingEntity;
import com.scanpilot.persistence.entity.RepositoryEntity;
import com.scanpilot.persistence.entity.ReviewRequestEntity;
import com.scanpilot.persistence.entity.UserEntity;
import com.scanpilot.persistence.repository.FindingRepository;
import com.scanpilot.persistence.repository.RepositoryRepository;
import com.scanpilot.persistence.repository.ReviewRequestRepository;
import com.scanpilot.persistence.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("Review Request Persistence Tests")
class ReviewRequestPersistenceTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RepositoryRepository repositoryRepository;

    @Autowired
    private FindingRepository findingRepository;

    @Autowired
    private ReviewRequestRepository reviewRequestRepository;

    private RepositoryEntity testRepo;
    private FindingEntity testFinding;

    @BeforeEach
    void setUp() {
        UserEntity user = userRepository.save(UserEntity.builder()
                .githubUserId(6001L)
                .login("review_owner")
                .build());

        testRepo = repositoryRepository.save(RepositoryEntity.builder()
                .userId(user.getId())
                .githubRepoId(44001L)
                .fullName("review_owner/api-gateway")
                .build());

        testFinding = findingRepository.save(FindingEntity.builder()
                .repositoryId(testRepo.getId())
                .ruleId("SP-CONFIG-001")
                .fingerprint("fp_review_001")
                .severity("HIGH")
                .lifecycle("OPEN")
                .build());
    }

    @Test
    @DisplayName("Should create review requests, update answer status, and query by finding and repository")
    void shouldManageReviewRequests() {
        Instant now = Instant.now();

        ReviewRequestEntity request = ReviewRequestEntity.builder()
                .findingId(testFinding.getId())
                .repositoryId(testRepo.getId())
                .question("Is this Google API key restricted to Gemini in Google Cloud Console?")
                .contextSummary("The key was found in a frontend configuration file.")
                .status("PENDING")
                .createdAt(now)
                .build();

        ReviewRequestEntity saved = reviewRequestRepository.save(request);
        assertThat(saved.getId()).isNotNull();

        List<ReviewRequestEntity> pendingList = reviewRequestRepository
                .findByRepositoryIdAndStatus(testRepo.getId(), "PENDING");
        assertThat(pendingList).hasSize(1);

        // User answers the review request
        saved.setStatus("ANSWERED");
        saved.setAnswer("Yes, restricted to Maps API only, not Gemini.");
        saved.setAnsweredAt(Instant.now());
        reviewRequestRepository.saveAndFlush(saved);

        List<ReviewRequestEntity> answeredList = reviewRequestRepository
                .findByRepositoryIdAndStatus(testRepo.getId(), "ANSWERED");
        assertThat(answeredList).hasSize(1);
        assertThat(answeredList.get(0).getAnswer()).contains("Maps API");

        List<ReviewRequestEntity> findingRequests = reviewRequestRepository
                .findByFindingId(testFinding.getId());
        assertThat(findingRequests).hasSize(1);
    }
}
