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
 * @return True if the change is from a local write or if it's not the initial data load.
 */
fun QuerySnapshot.shouldNotifySync(isInitialSnapshot: Boolean): Boolean {
    return this.metadata.hasPendingWrites() || !isInitialSnapshot
}
