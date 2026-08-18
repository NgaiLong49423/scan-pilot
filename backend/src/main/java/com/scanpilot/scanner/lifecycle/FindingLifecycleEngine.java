package com.scanpilot.scanner.lifecycle;

import com.scanpilot.persistence.entity.FindingEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Finding Lifecycle Engine implementing the exact 3-stage finding lifecycle
 * and remediation quality mapping (FR-007, FR-018, FR-019, FR-051, DEC-012).
 *
 * Evaluation Rules:
 * 1. Secret present at current HEAD -> FindingLifecycle.OPEN, RemediationQuality.ACTION_REQUIRED
 *    (or FindingLifecycle.REGRESSED if previously RESOLVED/REGRESSED).
 * 2. Secret absent at HEAD, but present in reachable Git history -> FindingLifecycle.RESOLVED, RemediationQuality.RISK_CONTAINED.
 * 3. Secret absent at HEAD, and absent from reachable Git history (clean rewrite) -> FindingLifecycle.RESOLVED, RemediationQuality.VERIFIED_COMPLETE.
 * 4. Secret previously RESOLVED, but reappears at current HEAD -> FindingLifecycle.REGRESSED, RemediationQuality.ACTION_REQUIRED.
 */
@Slf4j
@Service
public class FindingLifecycleEngine {

    /**
     * Evaluates finding lifecycle and remediation quality from prior lifecycle state and presence flags.
     *
     * @param previousLifecycle  the prior lifecycle state (null if first observed)
     * @param presentAtHead      true if the secret is detected in current HEAD files
     * @param presentInHistory   true if the secret is detected in reachable Git commit history
     * @return evaluated FindingLifecycleResult
     */
    public FindingLifecycleResult evaluate(FindingLifecycle previousLifecycle, boolean presentAtHead, boolean presentInHistory) {
        if (presentAtHead) {
            if (previousLifecycle == FindingLifecycle.RESOLVED || previousLifecycle == FindingLifecycle.REGRESSED) {
                log.debug("Secret reappeared at HEAD after resolution; transitioning to REGRESSED");
                return new FindingLifecycleResult(FindingLifecycle.REGRESSED, RemediationQuality.ACTION_REQUIRED);
            }
            return new FindingLifecycleResult(FindingLifecycle.OPEN, RemediationQuality.ACTION_REQUIRED);
        } else {
            if (presentInHistory) {
                log.debug("Secret absent at HEAD but present in history; transitioning to RESOLVED (RISK_CONTAINED)");
                return new FindingLifecycleResult(FindingLifecycle.RESOLVED, RemediationQuality.RISK_CONTAINED);
            } else {
                log.debug("Secret absent at HEAD and absent in history; transitioning to RESOLVED (VERIFIED_COMPLETE)");
                return new FindingLifecycleResult(FindingLifecycle.RESOLVED, RemediationQuality.VERIFIED_COMPLETE);
            }
        }
    }

    /**
     * Evaluates lifecycle with previous lifecycle as a String.
     */
    public FindingLifecycleResult evaluate(String previousLifecycleStr, boolean presentAtHead, boolean presentInHistory) {
        FindingLifecycle previous = null;
        if (previousLifecycleStr != null && !previousLifecycleStr.isBlank()) {
            try {
                previous = FindingLifecycle.valueOf(previousLifecycleStr.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }
        return evaluate(previous, presentAtHead, presentInHistory);
    }

    /**
     * Evaluates lifecycle using an existing FindingEntity.
     */
    public FindingLifecycleResult evaluate(FindingEntity existingFinding, boolean presentAtHead, boolean presentInHistory) {
        return evaluate(existingFinding != null ? existingFinding.getLifecycle() : null, presentAtHead, presentInHistory);
    }
}
