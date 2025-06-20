package com.nhathuy.nextmeet.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nhathuy.nextmeet.model.SearchHistory
import com.nhathuy.nextmeet.model.SearchType
import com.nhathuy.nextmeet.model.TrendingSearch

@Dao
interface SearchHistoryDao {
    /**
     * Lấy danh sách lịch sử tìm kiếm.
     */
    @Query("SELECT * FROM search_history WHERE user_id = :userId AND search_type = :searchType ORDER BY last_used DESC LIMIT :limit")
    suspend fun getSearchHistory(userId:Int,searchType: SearchType,limit:Int = 10):List<SearchHistory>

    /**
     * Lấy tất cả lịch sử tìm kiếm của người dùng.
     */
    @Query("SELECT * FROM search_history WHERE user_id = :userId ORDER BY last_used DESC LIMIT :limit")
    suspend fun getAllSearchHistory(userId: Int,limit: Int = 20):List<SearchHistory>

    /**
     * Lấy search theo trending
     */
    @Query("""
    SELECT search_text, COUNT(*) as count 
    FROM search_history 
    WHERE user_id = :userId AND search_type = :searchType 
    GROUP BY search_text
    ORDER BY count DESC 
    LIMIT :limit
""")
    suspend fun getTrendingSearches(userId: Int, searchType: SearchType, limit: Int = 5): List<TrendingSearch>

    /**
     * Thêm một lịch sử tìm kiếm mới.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchHistory(searchHistory: SearchHistory)

    /**
     * Cập nhật lịch sử tìm kiếm với timestamp mới.
     */
    @Query("UPDATE search_history SET last_used = :timestamp WHERE user_id = :userId AND search_text = :query AND search_type = :searchType")
    suspend fun updateLastUsed(userId:Int, query:String, searchType: SearchType,timestamp:Long = System.currentTimeMillis())

    /**
     * Xóa lịch sử tìm kiếm
     */
    @Query("DELETE FROM search_history WHERE user_id = :userId AND search_text = :searchText AND search_type = :searchType")
    suspend fun deleteSearchHistory(userId: Int,searchText: String,searchType: SearchType)

    /**
     * Xóa theo lại tìm kiếm
     */
    @Query("DELETE FROM search_history WHERE user_id = :userId AND search_type = :searchType")
    suspend fun clearSearchHistory(userId: Int, searchType: SearchType)


    /**
     * Xóa tất cả lịch sử tìm kiếm của người dung.
     */
    @Query("DELETE FROM search_history WHERE user_id = :userId")
    suspend fun clearAllSearchHistory(userId: Int)
}