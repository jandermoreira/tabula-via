/**
 * File: FirestoreExt.kt
 * Description: Extension functions for Firestore components to centralize business logic 
 * and reduce boilerplate.
 */

package edu.jm.tabulavia.utils

import com.google.firebase.firestore.QuerySnapshot

/**
 * Determines if a QuerySnapshot represents a real-time change that should trigger 
 * a sync activity notification (e.g., a visual pulse).
 *
 * @param isInitialSnapshot A flag indicating if this is the first snapshot received by the listener.
 * @return True if the change is from a local write or if it's a remote update with actual data changes.
 */
fun QuerySnapshot.shouldNotifySync(isInitialSnapshot: Boolean): Boolean {
    // 1. Always notify on local pending writes for immediate feedback
    if (this.metadata.hasPendingWrites()) return true

    // 2. Ignore the very first snapshot (initial load) to avoid pulsing on screen entry
    if (isInitialSnapshot) return false

    // 3. Notify only if there are actual document changes and it's coming from the server
    // This prevents redundant pulses from metadata-only changes or cache-to-server transitions.
    return !this.metadata.isFromCache && !this.documentChanges.isEmpty()
}
