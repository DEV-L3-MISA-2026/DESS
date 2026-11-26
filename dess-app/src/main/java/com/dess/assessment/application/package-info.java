/**
 * Assessment application layer: services, commands and persistence ports.
 *
 * <p>{@code AssessmentService} owns the definitions.
 * {@code EligibilityService} answers whether a student may sit an
 * assessment, from the enrollment rules. {@code ResultService}
 * records and corrects results.</p>
 *
 * <p>The module calls the academic and people modules through their
 * application interfaces only. It never reads their tables or
 * repositories directly.</p>
 */
package com.dess.assessment.application;
