package com.scanpilot.scanner.lifecycle;

import com.scanpilot.persistence.entity.FindingEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Finding Lifecycle Engine Tests")
class FindingLifecycleEngineTest {

    private FindingLifecycleEngine engine;

    @BeforeEach
    void setUp() {
        engine = new FindingLifecycleEngine();
    }

    @Nested
    @DisplayName("Stage 1: Open Finding Detection (FR-007, FR-051)")
    class OpenFindingTests {

        @Test
        @DisplayName("New secret present at current HEAD -> OPEN, ACTION_REQUIRED")
        void shouldSetOpenForNewSecretAtHead() {
            FindingLifecycleResult result = engine.evaluate(
                (FindingLifecycle) null,
                true,  // presentAtHead
                true   // presentInHistory
            );

            assertThat(result.lifecycle()).isEqualTo(FindingLifecycle.OPEN);
            assertThat(result.remediationQuality()).isEqualTo(RemediationQuality.ACTION_REQUIRED);
        }

        @Test
        @DisplayName("Existing OPEN secret present at current HEAD -> stays OPEN, ACTION_REQUIRED")
        void shouldStayOpenWhenSecretStillAtHead() {
            FindingEntity existing = FindingEntity.builder()
                .id(UUID.randomUUID())
                .lifecycle("OPEN")
                .remediationQuality("ACTION_REQUIRED")
                .build();

            FindingLifecycleResult result = engine.evaluate(existing, true, true);

            assertThat(result.lifecycle()).isEqualTo(FindingLifecycle.OPEN);
            assertThat(result.remediationQuality()).isEqualTo(RemediationQuality.ACTION_REQUIRED);
        }
    }

    @Nested
    @DisplayName("Stage 2: Remediation via Source Removal (FR-018, FR-051)")
    class RiskContainedTests {

        @Test
        @DisplayName("Secret removed from HEAD, but still present in reachable Git history -> RESOLVED, RISK_CONTAINED")
        void shouldResolveWithRiskContainedWhenInHistoryOnly() {
            FindingEntity existing = FindingEntity.builder()
                .id(UUID.randomUUID())
                .lifecycle("OPEN")
                .remediationQuality("ACTION_REQUIRED")
                .build();

            FindingLifecycleResult result = engine.evaluate(
                existing,
                false, // absent at HEAD
                true   // present in reachable history
            );

            assertThat(result.lifecycle()).isEqualTo(FindingLifecycle.RESOLVED);
            assertThat(result.remediationQuality()).isEqualTo(RemediationQuality.RISK_CONTAINED);
        }

        @Test
        @DisplayName("Newly discovered secret only in history (not at HEAD) -> RESOLVED, RISK_CONTAINED")
        void shouldResolveWithRiskContainedForHistoricalOnlySecret() {
            FindingLifecycleResult result = engine.evaluate(
                (FindingLifecycle) null,
                false, // absent at HEAD
                true   // present in history
            );

            assertThat(result.lifecycle()).isEqualTo(FindingLifecycle.RESOLVED);
            assertThat(result.remediationQuality()).isEqualTo(RemediationQuality.RISK_CONTAINED);
        }
    }

    @Nested
    @DisplayName("Stage 3: Complete History Rewrite Remediation (FR-019, FR-051)")
    class VerifiedCompleteTests {

        @Test
        @DisplayName("Secret absent from HEAD and absent from reachable Git history (clean rewrite) -> RESOLVED, VERIFIED_COMPLETE")
        void shouldResolveWithVerifiedCompleteWhenPurgedFromHistory() {
            FindingEntity existing = FindingEntity.builder()
                .id(UUID.randomUUID())
                .lifecycle("RESOLVED")
                .remediationQuality("RISK_CONTAINED")
                .build();

            FindingLifecycleResult result = engine.evaluate(
                existing,
                false, // absent at HEAD
                false  // absent in history (clean rewrite / scrubbed)
            );

            assertThat(result.lifecycle()).isEqualTo(FindingLifecycle.RESOLVED);
            assertThat(result.remediationQuality()).isEqualTo(RemediationQuality.VERIFIED_COMPLETE);
        }

        @Test
        @DisplayName("Previously OPEN secret completely eradicated from repo history -> RESOLVED, VERIFIED_COMPLETE")
        void shouldResolveWithVerifiedCompleteFromOpen() {
            FindingEntity existing = FindingEntity.builder()
                .id(UUID.randomUUID())
                .lifecycle("OPEN")
                .remediationQuality("ACTION_REQUIRED")
                .build();

            FindingLifecycleResult result = engine.evaluate(
                existing,
                false,
                false
            );

            assertThat(result.lifecycle()).isEqualTo(FindingLifecycle.RESOLVED);
            assertThat(result.remediationQuality()).isEqualTo(RemediationQuality.VERIFIED_COMPLETE);
        }
    }

    @Nested
    @DisplayName("Regression: Secret Reappearance (DEC-012, FR-007)")
    class RegressionTests {

        @Test
        @DisplayName("Secret previously RESOLVED reappears at current HEAD -> REGRESSED, ACTION_REQUIRED")
        void shouldTransitionToRegressedWhenResolvedSecretReappearsAtHead() {
            FindingEntity existing = FindingEntity.builder()
                .id(UUID.randomUUID())
                .lifecycle("RESOLVED")
                .remediationQuality("RISK_CONTAINED")
                .build();

            FindingLifecycleResult result = engine.evaluate(
                existing,
                true,  // reappeared at HEAD
                true   // in history
            );

            assertThat(result.lifecycle()).isEqualTo(FindingLifecycle.REGRESSED);
            assertThat(result.remediationQuality()).isEqualTo(RemediationQuality.ACTION_REQUIRED);
        }

        @Test
        @DisplayName("Secret previously REGRESSED remains REGRESSED while present at HEAD")
        void shouldRemainRegressedWhilePresentAtHead() {
            FindingEntity existing = FindingEntity.builder()
                .id(UUID.randomUUID())
                .lifecycle("REGRESSED")
                .remediationQuality("ACTION_REQUIRED")
                .build();

            FindingLifecycleResult result = engine.evaluate(
                existing,
                true,
                true
            );

            assertThat(result.lifecycle()).isEqualTo(FindingLifecycle.REGRESSED);
            assertThat(result.remediationQuality()).isEqualTo(RemediationQuality.ACTION_REQUIRED);
        }
    }
}
