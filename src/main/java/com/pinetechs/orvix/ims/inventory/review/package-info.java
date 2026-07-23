/**
 * Review Center owns the audited workflow that starts after an inventory task
 * enters {@code UNDER_REVIEW}.
 *
 * <p>The package is intentionally split by responsibility:
 *
 * <ul>
 *   <li>{@code controller}: supervisor HTTP contracts. The Android adapter
 *       remains in the project's existing {@code app.controller} package.</li>
 *   <li>{@code service}: issue synchronization, request workflow, employee
 *       submission, supervisor decisions, and response mapping.</li>
 *   <li>{@code domain}: domain-specific application of an accepted vehicle,
 *       asset, or spare-part result.</li>
 *   <li>{@code entity}, {@code repository}, {@code dto}, and {@code enums}:
 *       persistence and stable data contracts without workflow logic.</li>
 * </ul>
 *
 * <p>Core invariants:
 *
 * <ul>
 *   <li>An employee submission is evidence only and never changes
 *       {@code currentScan}.</li>
 *   <li>Only an explicit supervisor decision may apply a recheck result.</li>
 *   <li>Resolved evidence stays final; later evidence receives a new issue
 *       key and does not silently reopen the old decision.</li>
 *   <li>A task cannot complete while blocking issues or active rechecks
 *       remain.</li>
 * </ul>
 */
package com.pinetechs.orvix.ims.inventory.review;
