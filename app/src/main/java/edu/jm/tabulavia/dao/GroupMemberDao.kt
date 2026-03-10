/**
 * Data Access Object for GroupMember entity.
 * Provides methods to interact with the 'group_members' table.
 */
package edu.jm.tabulavia.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import edu.jm.tabulavia.model.GroupMember

@Dao
interface GroupMemberDao {

    /**
     * Inserts a list of group members into the database.
     * Replaces existing entries in case of conflict.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroupMembers(members: List<GroupMember>)

    /**
     * Inserts all group members (alternative bulk insert).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(groupMembers: List<GroupMember>)

    /**
     * Retrieves all group members associated with a specific activity.
     */
    @Query("SELECT * FROM group_members WHERE activityId = :activityId")
    suspend fun getGroupMembersForActivity(activityId: Long): List<GroupMember>

    /**
     * Deletes all group members associated with a specific activity.
     */
    @Query("DELETE FROM group_members WHERE activityId = :activityId")
    suspend fun clearGroupMembersForActivity(activityId: Long)

    /**
     * Retrieves all group members in the database.
     */
    @Query("SELECT * FROM group_members")
    suspend fun getAllGroupMembers(): List<GroupMember>
}