package edu.jm.tabulavia.utils

import android.util.Log
import com.google.firebase.firestore.QuerySnapshot

/**
 * Determines if a QuerySnapshot represents a real-time change that should trigger 
 * a sync activity notification (e.g., a visual pulse).
 */
fun QuerySnapshot.shouldNotifySync(isInitialSnapshot: Boolean): Boolean {
    val hasPendingWrites = this.metadata.hasPendingWrites()
    val isFromCache = this.metadata.isFromCache
    val hasChanges = !this.documentChanges.isEmpty()
    
    // Log for debugging sync issues
    Log.d("FirestoreSync", "Snapshot received: isInitial=$isInitialSnapshot, pending=$hasPendingWrites, cache=$isFromCache, changes=$hasChanges")

    // 1. Local writes always pulse for immediate feedback
    if (hasPendingWrites) return true

    // 2. Ignore the initial load snapshot
    if (isInitialSnapshot) return false

    // 3. For remote changes, pulse if there are actual document changes.
    // We removed the strict !isFromCache check as some devices might report 
    // cache updates during the sync process.
    return hasChanges
}
