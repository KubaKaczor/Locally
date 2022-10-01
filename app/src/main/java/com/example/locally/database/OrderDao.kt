package com.example.locally.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {

    @Insert
    suspend fun insert(orderEntity: OrderEntity)

    @Update
    suspend fun update(orderEntity: OrderEntity)

    @Delete
    suspend fun delete(orderEntity: OrderEntity)

    @Query("SELECT * FROM `orders` where active = 1")
    fun fetchAllOrders() : Flow<List<OrderEntity>>

    @Query("SELECT * FROM `orders` where active = 1 and type=:regularType")
    fun fetchOrdersByType(regularType: Int) : Flow<List<OrderEntity>>

    @Query("SELECT * FROM `orders` where userId=:userId and active=:active")
    fun fetchOrdersOfUser(userId: Int, active: Int) : Flow<List<OrderEntity>>
    //
    @Query("SELECT * FROM `orders` where id=:id")
    fun findOrderById(id: Int) : Flow<OrderEntity>

    @Query("SELECT Count(*) FROM `orders` where userId=:userId AND active=1")
    fun getValueOfActiveOrders(userId: Int) : Flow<Int>

    @Query("SELECT Count(*) FROM `orders` where userId=:userId AND active=0")
    fun getValueOfCompletedOrders(userId: Int) : Flow<Int>

}