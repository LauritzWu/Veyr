package io.nekohasekai.sagernet.database

import androidx.room.withTransaction
import io.nekohasekai.sagernet.GroupType
import io.nekohasekai.sagernet.bg.SubscriptionUpdater
import io.nekohasekai.sagernet.group.ClashImportPreview
import io.nekohasekai.sagernet.group.GroupUpdater
import io.nekohasekai.sagernet.group.RawUpdater
import io.nekohasekai.sagernet.ktx.applyDefaultValues
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

object GroupManager {

    interface Listener {
        suspend fun groupAdd(group: ProxyGroup)
        suspend fun groupUpdated(group: ProxyGroup)

        suspend fun groupRemoved(groupId: Long)
        suspend fun groupUpdated(groupId: Long)
    }

    data class AffectedRoute(val id: Long, val name: String, val oldNode: String)

    interface Interface {
        suspend fun confirm(message: String): Boolean
        suspend fun confirmSubscriptionRemoval(
            group: ProxyGroup,
            nodes: List<String>,
            selectedIndex: Int,
            affectedRules: List<AffectedRoute>,
            replacementNames: List<String>
        ): Map<Long, String>?
        suspend fun alert(message: String)
        suspend fun onUpdateSuccess(
            group: ProxyGroup,
            added: List<String>,
            updated: Map<String, String>,
            deleted: List<String>,
            duplicate: List<String>,
            byUser: Boolean,
            selectedRemoved: Boolean,
            nodeCount: Int
        )

        suspend fun onUpdateFailure(group: ProxyGroup, message: String)
    }

    private val listeners = ArrayList<Listener>()
    var userInterface: Interface? = null

    suspend fun iterator(what: suspend Listener.() -> Unit) {
        synchronized(listeners) {
            listeners.toList()
        }.forEach { listener ->
            what(listener)
        }
    }

    fun addListener(listener: Listener) {
        synchronized(listeners) {
            listeners.add(listener)
        }
    }

    fun removeListener(listener: Listener) {
        synchronized(listeners) {
            listeners.remove(listener)
        }
    }

    suspend fun clearGroup(groupId: Long) {
        DataStore.selectedProxy = 0L
        SagerDatabase.proxyDao.deleteAll(groupId)
        iterator { groupUpdated(groupId) }
    }

    fun rearrange(groupId: Long) {
        val entities = SagerDatabase.proxyDao.getByGroup(groupId)
        for (index in entities.indices) {
            entities[index].userOrder = (index + 1).toLong()
        }
        SagerDatabase.proxyDao.updateProxy(entities)
    }

    suspend fun postUpdate(group: ProxyGroup) {
        iterator { groupUpdated(group) }
    }

    suspend fun postUpdate(groupId: Long) {
        postUpdate(SagerDatabase.groupDao.getById(groupId) ?: return)
    }

    suspend fun postReload(groupId: Long) {
        iterator { groupUpdated(groupId) }
    }

    suspend fun createGroup(group: ProxyGroup): ProxyGroup {
        group.userOrder = SagerDatabase.groupDao.nextOrder() ?: 1
        group.id = SagerDatabase.groupDao.createGroup(group.applyDefaultValues())
        iterator { groupAdd(group) }
        if (group.type == GroupType.SUBSCRIPTION) {
            SubscriptionUpdater.reconfigureUpdater()
            GroupUpdater.startUpdate(group, true)
        }
        return group
    }

    internal suspend fun createPastedSubscriptionGroup(
        group: ProxyGroup,
        confirm: suspend (ClashImportPreview, Int) -> Boolean,
    ): Boolean {
        val subscription = checkNotNull(group.subscription)
        val incoming = RawUpdater.prepareSubscription(subscription, previewClash = true)
        val preview = incoming.clashPreview
        if (preview != null && !confirm(preview, incoming.nodes.size)) return false

        SagerDatabase.instance.withTransaction {
            group.userOrder = SagerDatabase.groupDao.nextOrder() ?: 1
            if (group.name?.startsWith("Subscription #") == true) {
                incoming.name?.let { group.name = it }
            }
            incoming.userInfo?.let { subscription.subscriptionUserinfo = it }
            subscription.pendingRemoval = false
            subscription.lastUpdated = (System.currentTimeMillis() / 1000).toInt()
            group.id = SagerDatabase.groupDao.createGroup(group.applyDefaultValues())
            val nodes = incoming.nodes.values.mapIndexed { index, bean ->
                ProxyEntity(groupId = group.id, userOrder = index + 1L).apply { putBean(bean) }
            }
            SagerDatabase.proxyDao.insert(nodes)
        }
        withContext(NonCancellable) {
            iterator { groupAdd(group) }
            SubscriptionUpdater.reconfigureUpdater()
        }
        return true
    }

    suspend fun updateGroup(group: ProxyGroup) {
        SagerDatabase.groupDao.updateGroup(group)
        iterator { groupUpdated(group) }
        if (group.type == GroupType.SUBSCRIPTION) {
            SubscriptionUpdater.reconfigureUpdater()
        }
    }

    suspend fun deleteGroup(groupId: Long) {
        SagerDatabase.groupDao.deleteById(groupId)
        SagerDatabase.proxyDao.deleteByGroup(groupId)
        iterator { groupRemoved(groupId) }
        SubscriptionUpdater.reconfigureUpdater()
    }

    suspend fun deleteGroup(group: List<ProxyGroup>) {
        SagerDatabase.groupDao.deleteGroup(group)
        SagerDatabase.proxyDao.deleteByGroup(group.map { it.id }.toLongArray())
        for (proxyGroup in group) iterator { groupRemoved(proxyGroup.id) }
        SubscriptionUpdater.reconfigureUpdater()
    }

}
