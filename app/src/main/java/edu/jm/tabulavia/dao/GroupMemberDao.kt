package edu.jm.tabulavia.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import edu.jm.tabulavia.model.GroupMember

@Dao
interface GroupMemberDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroupMembers(members: List<GroupMember>)

    @Query("SELECT * FROM group_members WHERE activityId = :activityId")
    suspend fun getGroupMembersForActivity(activityId: Long): List<GroupMember>

    @Query("DELETE FROM group_members WHERE activityId = :activityId")
    suspend fun clearGroupMembersForActivity(activityId: Long)

    @Query("SELECT * FROM group_members")
    suspend fun getAllGroupMembers(): List<GroupMember>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(groupMembers: List<GroupMember>)
}
